/*
 * /*
 *  * ==========================================================================================
 *  * =                            JAHIA'S ENTERPRISE DISTRIBUTION                             =
 *  * ==========================================================================================
 *  *
 *  *                                  http://www.jahia.com
 *  *
 *  * JAHIA'S ENTERPRISE DISTRIBUTIONS LICENSING - IMPORTANT INFORMATION
 *  * ==========================================================================================
 *  *
 *  *     Copyright (C) 2002-2022 Jahia Solutions Group. All rights reserved.
 *  *
 *  *     This file is part of a Jahia's Enterprise Distribution.
 *  *
 *  *     Jahia's Enterprise Distributions must be used in accordance with the terms
 *  *     contained in the Jahia Solutions Group Terms & Conditions as well as
 *  *     the Jahia Sustainable Enterprise License (JSEL).
 *  *
 *  *     For questions regarding licensing, support, production usage...
 *  *     please contact our team at sales@jahia.com or go to http://www.jahia.com/license.
 *  *
 *  * ==========================================================================================
 *  */
package org.jahia.translation.globallink.action;

import org.apache.commons.lang.StringUtils;
import org.gs4tr.gcc.restclient.GCExchange;
import org.gs4tr.gcc.restclient.model.Connector;
import org.gs4tr.gcc.restclient.model.LanguageDirection;
import org.gs4tr.gcc.restclient.model.LocaleConfig;
import org.gs4tr.gcc.restclient.operation.ConnectorsConfig.ConnectorsConfigResponseData;
import org.jahia.bin.Action;
import org.jahia.bin.ActionResult;
import org.jahia.registries.ServicesRegistry;
import org.jahia.services.content.JCRContentUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;
import org.jahia.services.seo.urlrewrite.ResourceChecksumCalculator;
import org.jahia.translation.globallink.dto.GlobalLinkConfigurationDTO;
import org.jahia.translation.globallink.exception.GlobalLinkServiceException;
import org.jahia.translation.globallink.util.GlobalLinkUtil;
import org.jahia.translation.globallink.util.JCRUtil;
import org.json.JSONObject;
import org.osgi.service.component.annotations.Component;
import org.quartz.CronTrigger;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toSet;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.jahia.translation.globallink.common.GlobalLinkConstants.*;

/**
 * Jahia Action to process and save Global Link Site configurations.
 *
 * @author Aashish.Kocchar, WebitUp
 */
@Component(service = Action.class, immediate = true)
public class GlobalLinkConfigAction extends Action {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalLinkConfigAction.class);

    public GlobalLinkConfigAction() {
        this.setRequiredMethods("POST");
        this.setRequireAuthenticatedUser(true);
        this.setRequiredPermission("adminGlobalLinkTranslation");
    }

    /**
     * Saves the GlobalLink configurations to the site node.
     */
    @Override public ActionResult doExecute(HttpServletRequest request, RenderContext renderContext, Resource resource,
            JCRSessionWrapper jcrSessionWrapper, Map<String, List<String>> map, URLResolver urlResolver) throws Exception {
        ActionResult actionResult = new ActionResult(SC_OK, request.getRequestURL().toString(), new JSONObject());
        try {
            this.saveGlobalLinkConfigs(request, renderContext, jcrSessionWrapper);
            actionResult.getJson().put("message", "gbl.settings.success");
        } catch (Exception ex) {
            LOGGER.error("Exception while saving GlobalLink settings!!", ex);
            actionResult.getJson().put("message", "gbl.settings.error");
        }
        return actionResult;
    }

    /**
     * Save global link settings for a site.
     *
     * @param request
     * @param renderContext
     * @param sessionWrapper
     * @throws RepositoryException
     */
    private void saveGlobalLinkConfigs(HttpServletRequest request, RenderContext renderContext, JCRSessionWrapper sessionWrapper)
            throws RepositoryException {
        JCRSiteNode siteNode = renderContext.getSite();

        initGBLNode(siteNode, sessionWrapper);

        LOGGER.info("Saving configuration for Site: {}", siteNode.getTitle());
        if (!Arrays.asList(siteNode.getMixinNodeTypes()).contains(GBL_MIXIN_TYPE)) {
            siteNode.addMixin(GBL_MIXIN_TYPE);
            sessionWrapper.save();
        }
        switch (getRequestParameter(request, GBL_PROPERTY_ENABLE)) {
            case "true":
                siteNode.setProperty(GBL_PROPERTY_ENABLE, true);
                break;
            case "false":
                siteNode.setProperty(GBL_PROPERTY_ENABLE, false);
                break;
            default:
                break;
        }
        siteNode.setProperty(GBL_PROPERTY_USERNAME, getRequestParameter(request, GBL_PROPERTY_USERNAME));
        siteNode.setProperty(GBL_PROPERTY_PASSWORD, getRequestParameter(request, GBL_PROPERTY_PASSWORD));
        siteNode.setProperty(GBL_PROPERTY_URL, getRequestParameter(request, GBL_PROPERTY_URL));
        siteNode.setProperty(GBL_PROPERTY_AGENT, getRequestParameter(request, GBL_PROPERTY_AGENT));
        siteNode.setProperty(GBL_PROPERTY_CONNECTOR_NAME, getRequestParameter(request, GBL_PROPERTY_CONNECTOR_NAME));
        siteNode.setProperty(GBL_PROPERTY_PREFIX, getRequestParameter(request, GBL_PROPERTY_PREFIX));
        siteNode.setProperty(GBL_PROPERTY_FORMAT, getRequestParameter(request, GBL_PROPERTY_FORMAT));
        siteNode.setProperty(GBL_PROPERTY_DOC_LOCATION, getRequestParameter(request, GBL_PROPERTY_DOC_LOCATION));
        if (!getRequestParameter(request, GBL_PROPERTY_INTERVAL).equals(StringUtils.EMPTY)) {
            siteNode.setProperty(GBL_PROPERTY_INTERVAL, Long.valueOf(getRequestParameter(request, GBL_PROPERTY_INTERVAL)));
            try {
                Trigger[] triggersOfJob = ServicesRegistry.getInstance().getSchedulerService().getScheduler()
                        .getTriggersOfJob("translationJob", "DEFAULT");
                for (int i = 0; i < triggersOfJob.length; i++) {
                    Trigger trigger = triggersOfJob[i];
                    if (trigger instanceof CronTrigger) {
                        CronTrigger cronTrigger = (CronTrigger) trigger;
                        CronTrigger cronTrigger1 = new CronTrigger(cronTrigger.getName(), cronTrigger.getGroup());
                        cronTrigger1.setJobName("translationJob");
                        cronTrigger1.setJobGroup("DEFAULT");
                        Long aLong = Long.valueOf(getRequestParameter(request, GBL_PROPERTY_INTERVAL));
                        if (aLong > 59l) {
                            aLong = 59l;
                        } else if (aLong < 1l) {
                            aLong = 1l;
                        }
                        cronTrigger1.setCronExpression("25 0/" + aLong + " * * * ?");
                        Date date = ServicesRegistry.getInstance().getSchedulerService().getScheduler()
                                .rescheduleJob(cronTrigger.getName(), cronTrigger.getGroup(), cronTrigger1);
                    }
                }
            } catch (SchedulerException | ParseException e) {
                LOGGER.error("Error scheduling jobs", e);
            }
        }
        siteNode.setProperty(GBL_PROPERTY_COMPONENTS, getMultiRequestparameter(request, GBL_PROPERTY_COMPONENTS));
        String[] multiRequestparameter = getMultiRequestparameter(request, "j:languageMappings");
        if (multiRequestparameter != null) {
            siteNode.setProperty("j:languageMappings", multiRequestparameter);
        }
        siteNode.setProperty("status", "OK");
        //Change by cedric to save even in case of errors
        sessionWrapper.save();
        this.getLanguageMapping(siteNode);
        sessionWrapper.save();
    }

    /**
     * Check for null pointer and return request parameter value.
     *
     * @return parameter value
     */
    private String getRequestParameter(HttpServletRequest request, String parameterName) {
        if (request.getParameter(parameterName) != null) {
            return request.getParameter(parameterName).trim();
        }
        return StringUtils.EMPTY;
    }

    /**
     * Check for null pointer and return array of string for a request parameter.
     *
     * @return parameter value
     */
    private String[] getMultiRequestparameter(HttpServletRequest request, String parameterName) {
        if (request.getParameter(parameterName) != null) {
            return request.getParameterMap().get(parameterName);
        }
        return null;
    }

    protected void getLanguageMapping(JCRSiteNode siteNode) throws RepositoryException {
        GlobalLinkConfigurationDTO configuration = JCRUtil.getSiteConfiguration(siteNode);

        if (configuration != null) {
            GCExchange gcExchange = Optional.of(configuration).map(GlobalLinkUtil::getGlobalLinkClient).orElse(null);

            if (gcExchange != null) {
                try {
                    Map<String, Set<String>> availableLanguageMapping = getAvailableLanguageMapping(gcExchange.getConnectorsConfig());

                    availableLanguageMapping.forEach((key, value) -> {
                        JCRNodeWrapper sourceNode;
                        String sourceNodeName = key + "-gblSource";
                        try {
                            if (siteNode.hasNode(sourceNodeName)) {
                                sourceNode = siteNode.getNode(sourceNodeName);
                            } else {
                                sourceNode = siteNode.addNode(sourceNodeName, GBL_PROPERTY_SOURCE_DIRECTIONS);
                            }
                            Set<String> targetLanguages = availableLanguageMapping.get(key);
                            sourceNode.setProperty("targetLanguages", targetLanguages.toArray(new String[targetLanguages.size()]));
                        } catch (RepositoryException e) {
                            throw new GlobalLinkServiceException("Error while saving language mapping", e);
                        }
                    });

                } catch (Exception e) {
                    siteNode.setProperty(GBL_PROPERTY_ENABLE, false);
                    if (e.getMessage().contains("Connector key")) {
                        StringBuilder availableConnectors = new StringBuilder();
                        for (Connector c : gcExchange.getConnectors()) {
                            if (availableConnectors.length() > 0) {
                                availableConnectors.append(", ");
                            }
                            availableConnectors.append(c.getConnectorName());
                        }

                        siteNode.setProperty("status", availableConnectors.toString());
                    } else {
                        siteNode.setProperty("status", e.getMessage());
                    }
                    siteNode.getSession().save();
                    throw e;
                }
            }
        }
    }

    private static Map<String, Set<String>> getAvailableLanguageMapping(ConnectorsConfigResponseData connectorsConfigResponseData) {
        List<LanguageDirection> languageMappingList = connectorsConfigResponseData.getLanguageDirections();
        if (languageMappingList.isEmpty()) {
            Optional<LocaleConfig> sourceLocaleConfigOptional = connectorsConfigResponseData.getSupportedLocales().stream()
                    .filter(LocaleConfig::getIsSource).findFirst();

            if (sourceLocaleConfigOptional.isPresent()) {
                LocaleConfig localeConfig = sourceLocaleConfigOptional.get();
                return Collections.singletonMap(localeConfig.getConnectorLocale(),
                        connectorsConfigResponseData.getSupportedLocales().stream().filter(locale -> !locale.getIsSource())
                                .map(LocaleConfig::getConnectorLocale).collect(toSet()));
            }
        } else {
            return languageMappingList.stream().collect(Collectors.groupingBy(LanguageDirection::getSourceLocale,
                    Collectors.mapping(LanguageDirection::getTargetLocale, Collectors.toSet())));
        }
        return Collections.emptyMap();
    }

    private JCRNodeWrapper initGBLNode(JCRSiteNode siteNode, JCRSessionWrapper sessionWrapper) {
        try {
            JCRNodeWrapper node = JCRContentUtils.getOrAddPath(sessionWrapper, siteNode, NODE_NAME_PROJECT_REQUESTS, NODE_TYPE_PROJECT_REQUESTS);
            sessionWrapper.save();

            return node;
        } catch (RepositoryException e) {
            throw new GlobalLinkServiceException(e.getMessage(), e);
        }
    }
}
