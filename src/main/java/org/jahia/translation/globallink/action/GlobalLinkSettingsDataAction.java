/*
 * ==========================================================================================
 * =                            JAHIA'S ENTERPRISE DISTRIBUTION                             =
 * ==========================================================================================
 *
 *                                  http://www.jahia.com
 *
 * JAHIA'S ENTERPRISE DISTRIBUTIONS LICENSING - IMPORTANT INFORMATION
 * ==========================================================================================
 *
 *     Copyright (C) 2002-2026 Jahia Solutions Group. All rights reserved.
 *
 *     This file is part of a Jahia's Enterprise Distribution.
 *
 *     Jahia's Enterprise Distributions must be used in accordance with the terms
 *     contained in the Jahia Solutions Group Terms & Conditions as well as
 *     the Jahia Sustainable Enterprise License (JSEL).
 *
 *     For questions regarding licensing, support, production usage...
 *     please contact our team at sales@jahia.com or go to http://www.jahia.com/license.
 *
 * ==========================================================================================
 */
package org.jahia.translation.globallink.action;

import org.apache.commons.lang.StringUtils;
import org.gs4tr.gcc.restclient.GCExchange;
import org.jahia.bin.Action;
import org.jahia.bin.ActionResult;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRValueWrapper;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.jahia.services.content.nodetypes.ExtendedNodeType;
import org.jahia.services.content.nodetypes.ExtendedPropertyDefinition;
import org.jahia.services.content.nodetypes.NodeTypeRegistry;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;
import org.jahia.translation.globallink.dto.GlobalLinkConfigurationDTO;
import org.jahia.translation.globallink.util.GlobalLinkUtil;
import org.jahia.translation.globallink.util.JCRUtil;
import org.jahia.utils.LanguageCodeConverters;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.jahia.translation.globallink.common.GlobalLinkConstants.*;

/**
 * Read-only action feeding the GlobalLink settings screen of Site Settings: the site's connection settings
 * (never the password, only whether one is stored), the state of the connection to GlobalLink with its
 * language directions, the language mapping and the content types offered for translation.
 * <p>
 * Called with {@code GET /cms/render/default/<lang>/sites/<site>.globalLinkSettingsData.do?uilang=<ui language>}
 * by users holding {@code adminGlobalLinkTranslation} on the site. It changes nothing; it calls GlobalLink once
 * when the site has an active configuration, as the former JSP screen did on every display.
 */
@Component(service = Action.class, immediate = true)
public class GlobalLinkSettingsDataAction extends Action {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalLinkSettingsDataAction.class);
    private static final String SOURCE_SUFFIX = "-gblSource";
    private static final String MAPPING_SEPARATOR = "###";
    private static final String LABEL = "label";
    private static final String VALUE = "value";

    public GlobalLinkSettingsDataAction() {
        this.setRequiredMethods("GET");
        this.setRequireAuthenticatedUser(true);
        this.setRequiredPermission("adminGlobalLinkTranslation");
    }

    @Override
    public ActionResult doExecute(HttpServletRequest request, RenderContext renderContext, Resource resource,
            JCRSessionWrapper session, Map<String, List<String>> parameters, URLResolver urlResolver) throws Exception {
        JCRSiteNode site = resource.getNode().getResolveSite();
        Locale uiLocale = getUiLocale(request, renderContext);

        JSONObject json = new JSONObject();
        json.put("siteKey", site.getSiteKey());
        json.put("settings", readSettings(site));
        json.put("status", StringUtils.defaultString(site.getPropertyAsString("status")));
        json.put("connection", readConnection(site));
        json.put("siteLanguages", readSiteLanguages(site, uiLocale));
        json.put("globalLinkLocales", readGlobalLinkLocales(site, uiLocale));
        json.put("languageMappings", readLanguageMappings(site));
        json.put("components", readComponents(site, uiLocale));

        HttpServletResponse response = renderContext.getResponse();
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-store, private");
        writeBody(response, json);
        // The body is written: a returned ActionResult would make Render write its own answer
        return null;
    }

    private static void writeBody(HttpServletResponse response, JSONObject json) throws IOException {
        response.getOutputStream().write(json.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static Locale getUiLocale(HttpServletRequest request, RenderContext renderContext) {
        String uilang = request.getParameter("uilang");
        if (StringUtils.isNotBlank(uilang) && uilang.matches("[A-Za-z_-]{2,12}")) {
            return LanguageCodeConverters.languageCodeToLocale(uilang);
        }
        return renderContext.getUILocale() != null ? renderContext.getUILocale() : Locale.ENGLISH;
    }

    private static JSONObject readSettings(JCRSiteNode site) throws JSONException, RepositoryException {
        JSONObject settings = new JSONObject();
        settings.put("activated", site.hasProperty(GBL_PROPERTY_ENABLE) && site.getProperty(GBL_PROPERTY_ENABLE).getBoolean());
        settings.put("username", StringUtils.defaultString(site.getPropertyAsString(GBL_PROPERTY_USERNAME)));
        settings.put("passwordSet", StringUtils.isNotEmpty(site.getPropertyAsString(GBL_PROPERTY_PASSWORD)));
        settings.put("url", StringUtils.defaultString(site.getPropertyAsString(GBL_PROPERTY_URL)));
        settings.put("connectorName", StringUtils.defaultString(site.getPropertyAsString(GBL_PROPERTY_CONNECTOR_NAME)));
        settings.put("submissionPrefix", StringUtils.defaultString(site.getPropertyAsString(GBL_PROPERTY_PREFIX)));
        settings.put("fileFormat", StringUtils.defaultString(site.getPropertyAsString(GBL_PROPERTY_FORMAT)));
        settings.put("fileLocation", StringUtils.defaultString(site.getPropertyAsString(GBL_PROPERTY_DOC_LOCATION)));
        settings.put("interval", StringUtils.defaultString(site.getPropertyAsString(GBL_PROPERTY_INTERVAL)));
        return settings;
    }

    /**
     * {@code NOT_CONFIGURED}: no active configuration. {@code INVALID}: GlobalLink refused the client.
     * {@code ERROR}: GlobalLink answered an error ({@code message}). {@code OK}: {@code directions} lists the
     * language directions of the connector.
     */
    private static JSONObject readConnection(JCRSiteNode site) throws JSONException {
        JSONObject connection = new JSONObject();
        JSONArray directions = new JSONArray();
        connection.put("directions", directions);
        GlobalLinkConfigurationDTO configuration = JCRUtil.getSiteConfiguration(site);
        if (configuration == null) {
            connection.put("state", "NOT_CONFIGURED");
            return connection;
        }
        GCExchange exchange = GlobalLinkUtil.getGlobalLinkClient(configuration);
        if (exchange == null) {
            connection.put("state", "INVALID");
            return connection;
        }
        try {
            Map<String, Set<String>> mapping = GlobalLinkConfigAction.getAvailableLanguageMapping(exchange.getConnectorsConfig());
            for (Map.Entry<String, Set<String>> entry : mapping.entrySet()) {
                for (String target : new TreeSet<>(entry.getValue())) {
                    directions.put(new JSONObject().put("source", entry.getKey()).put("target", target));
                }
            }
            connection.put("state", "OK");
        } catch (Exception e) {
            LOGGER.warn("GlobalLink refused the connector configuration of site {}: {}", site.getSiteKey(), e.getMessage());
            connection.put("state", "ERROR");
            connection.put("message", StringUtils.defaultString(e.getLocalizedMessage()));
        }
        return connection;
    }

    private static JSONArray readSiteLanguages(JCRSiteNode site, Locale uiLocale) throws JSONException {
        JSONArray languages = new JSONArray();
        for (Locale locale : site.getLanguagesAsLocales()) {
            languages.put(new JSONObject().put(VALUE, locale.toString()).put(LABEL, locale.getDisplayName(uiLocale)));
        }
        return languages;
    }

    /** The GlobalLink locales of the connector, read from the source language nodes the last save or refresh wrote. */
    private static JSONArray readGlobalLinkLocales(JCRSiteNode site, Locale uiLocale) throws RepositoryException, JSONException {
        Set<String> codes = new TreeSet<>();
        for (JCRNodeWrapper source : site.getNodes()) {
            if (!source.isNodeType(GBL_PROPERTY_SOURCE_DIRECTIONS)) {
                continue;
            }
            codes.add(StringUtils.substringBefore(source.getName(), SOURCE_SUFFIX));
            if (source.hasProperty("targetLanguages")) {
                for (JCRValueWrapper target : source.getProperty("targetLanguages").getValues()) {
                    codes.add(target.getString());
                }
            }
        }
        JSONArray locales = new JSONArray();
        for (String code : codes) {
            String label = Locale.forLanguageTag(code).getDisplayName(uiLocale);
            locales.put(new JSONObject().put(VALUE, code).put(LABEL, StringUtils.isNotBlank(label) ? label + " (" + code + ")" : code));
        }
        return locales;
    }

    /** {@code {"<site language>": "<GlobalLink locale>"}}, from the stored {@code <site language>###<locale>} values. */
    private static JSONObject readLanguageMappings(JCRSiteNode site) throws RepositoryException, JSONException {
        JSONObject mappings = new JSONObject();
        if (site.hasProperty("j:languageMappings")) {
            for (JCRValueWrapper value : site.getProperty("j:languageMappings").getValues()) {
                String mapping = value.getString();
                if (mapping.contains(MAPPING_SEPARATOR)) {
                    mappings.put(StringUtils.substringBefore(mapping, MAPPING_SEPARATOR), StringUtils.substringAfter(mapping, MAPPING_SEPARATOR));
                }
            }
        }
        return mappings;
    }

    /**
     * The content types offered for translation. Each value is stored as {@code <label>-<node type>}, the format
     * the translation job reads. {@code editorial} holds the editorial types with an internationalized string
     * property, {@code all} every type with one, {@code selected} the stored choice.
     */
    private static JSONObject readComponents(JCRSiteNode site, Locale uiLocale) throws RepositoryException, JSONException {
        JSONArray selected = new JSONArray();
        if (site.hasProperty(GBL_PROPERTY_COMPONENTS)) {
            for (JCRValueWrapper value : site.getProperty(GBL_PROPERTY_COMPONENTS).getValues()) {
                String stored = value.getString();
                selected.put(componentOption(stored, labelOf(StringUtils.substringAfterLast(stored, "-"), uiLocale, stored)));
            }
        }
        Map<String, String> available = GlobalLinkUtil.getComponentTypes(site, null, null, uiLocale);
        JSONArray editorial = new JSONArray();
        JSONArray all = new JSONArray();
        for (Map.Entry<String, String> type : available.entrySet()) {
            String typeName = type.getKey();
            ExtendedNodeType nodeType = nodeType(typeName);
            if (nodeType == null || !hasTranslatableProperty(nodeType)) {
                continue;
            }
            JSONObject option = componentOption(type.getValue() + "-" + typeName, type.getValue() + " (" + typeName + ")");
            all.put(option);
            if (nodeType.isNodeType("jmix:editorialContent")) {
                editorial.put(option);
            }
        }
        return new JSONObject().put("selected", selected).put("editorial", editorial).put("all", all);
    }

    private static JSONObject componentOption(String value, String label) throws JSONException {
        return new JSONObject().put(VALUE, value).put(LABEL, label);
    }

    private static String labelOf(String typeName, Locale uiLocale, String fallback) {
        ExtendedNodeType type = nodeType(typeName);
        return type != null ? type.getLabel(uiLocale) + " (" + typeName + ")" : fallback;
    }

    private static ExtendedNodeType nodeType(String typeName) {
        try {
            return NodeTypeRegistry.getInstance().getNodeType(typeName);
        } catch (NoSuchNodeTypeException e) {
            return null;
        }
    }

    private static boolean hasTranslatableProperty(ExtendedNodeType type) {
        for (ExtendedPropertyDefinition definition : type.getPropertyDefinitionsAsMap().values()) {
            if (!definition.isProtected() && definition.isInternationalized() && definition.getRequiredType() == PropertyType.STRING) {
                return true;
            }
        }
        return false;
    }
}
