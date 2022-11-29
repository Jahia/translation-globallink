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
package org.jahia.translation.globallink.util;

import com.google.common.base.Functions;
import com.google.common.collect.Ordering;
import org.apache.commons.lang.StringUtils;
import org.gs4tr.gcc.restclient.GCConfig;
import org.gs4tr.gcc.restclient.GCExchange;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRValueWrapper;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.jahia.services.content.nodetypes.ConstraintsHelper;
import org.jahia.services.content.nodetypes.ExtendedNodeType;
import org.jahia.services.content.nodetypes.ExtendedPropertyDefinition;
import org.jahia.services.content.nodetypes.NodeTypeRegistry;
import org.jahia.translation.globallink.dto.GlobalLinkConfigurationDTO;
import org.jahia.translation.globallink.dto.GlobalLinkProjectRequestDTO;
import org.jahia.utils.Patterns;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import java.io.File;
import java.nio.file.FileSystems;
import java.util.*;

import static org.jahia.translation.globallink.common.GlobalLinkConstants.DOCUMENT_PATH;
import static org.jahia.translation.globallink.common.GlobalLinkConstants.FILE_EXT_XML;

/**
 * Utility class for common methods.
 *
 * @author Prince.Arora, WebItUp.
 * @author Rakesh.Kumar, WebitUp.
 */
public class GlobalLinkUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalLinkUtil.class);

    /**
     * Get Full locale code with region code.
     *
     * @param locale
     * @return
     */
    public static String getGLLocale(String locale) {
        return locale.replace("_", "-");
    }

    public static String getJavaLocale(String locale) {
        return locale.replace("-", "_");
    }

    /**
     *
     * @param config
     * @return
     */
    public static GCExchange getGlobalLinkClient(GlobalLinkConfigurationDTO config) {
        GCConfig gcConfig = new GCConfig(config.getUrl(), config.getUsername(), config.getPassword());
        gcConfig.setUserAgent(config.getUserAgent());
        try {
            GCExchange gcExchange = new GCExchange(gcConfig);
            gcExchange.getConnectors()
                    .stream()
                    .filter(connector -> connector.getConnectorName().equals(config.getConnectorName()))
                    .findFirst()
                    .ifPresent(connector -> gcExchange.setConnectorKey(connector.getConnectorKey()));
            return gcExchange;
        } catch (Exception ex) {
            LOGGER.error("Error while generating GCExchange client: ", ex);
        }
        return null;
    }

    public static String getSourceDocumentPath(GlobalLinkProjectRequestDTO requestDTO, JCRNodeWrapper pageNode) {
        try {
            String docPath = DOCUMENT_PATH;
            if (requestDTO.getDocumentpath() != null && !requestDTO.getDocumentpath().equals("")) {
                docPath = requestDTO.getDocumentpath();
            }
            IOUtil.createDirectories(FileSystems.getDefault().getPath(docPath) + File.separator + requestDTO.getRequestId());
            return docPath + File.separator + requestDTO.getRequestId() + File.separator + pageNode.getDisplayableName() + "___" + pageNode.getIdentifier() + FILE_EXT_XML;
        } catch (Exception ex) {
            LOGGER.error("Exception while preparing source file path -> ", ex);
        }
        return null;
    }

    /**
     * Reading global link translation config property file and getting
     * list of all components that should be excluded from translation
     * components list shown in global link translation settings page.
     *
     * @return
     */
    public static List<String> getExcludedComponents(JCRValueWrapper[] valueWrappers) {
        List<String> excludeList = new ArrayList<>();
        if (valueWrappers != null && valueWrappers.length > 0) {
            for (JCRValueWrapper jcrValueWrapper : valueWrappers) {
                try {
                    excludeList.add(StringUtils.substringAfter(jcrValueWrapper.getString(), "-"));
                } catch (RepositoryException e) {
                    LOGGER.error("Cannot get string value for jcr value wrapper: {} exception {}", jcrValueWrapper, e);
                }
            }
            return excludeList;
        }
        return null;
    }

    public static Map<String, String> filterComponentsList(Map<String, String> components) {
        Map<String, String> filteredMap = new HashMap<>();
        for (Map.Entry<String, String> entry : components.entrySet()) {
            try {
                ExtendedNodeType extendedNodeType = NodeTypeRegistry.getInstance().getNodeType(entry.getKey());
                if(extendedNodeType.isNodeType("jmix:editorialContent")) {
                    Map<String, ExtendedPropertyDefinition> definitions = extendedNodeType.getPropertyDefinitionsAsMap();
                    for (Map.Entry<String, ExtendedPropertyDefinition> definitionEntry : definitions.entrySet()) {
                        ExtendedPropertyDefinition value1 = definitionEntry.getValue();
                        if (!value1.isProtected() && value1.isInternationalized() && value1.getRequiredType() == PropertyType.STRING) {
                            filteredMap.put(entry.getKey(), entry.getValue());
                            break;
                        }
                    }
                }
            } catch (NoSuchNodeTypeException e) {
                LOGGER.error("failed to check internationalized properties", e);
            }
        }
        return filteredMap;
    }

    private static boolean allowType(ExtendedNodeType t, List<String> includeTypeList,
                                     List<String> excludeTypeList) {
        if(t.getName().equals("jmix:publication")) {
            return false;
        }
        boolean include = true;
        String typeName = t.getName();

        if (excludeTypeList != null && !excludeTypeList.isEmpty()) {
            include = !excludeTypeList.contains(typeName);
            if (include) {
                for (String s : excludeTypeList) {
                    if (t.isNodeType(s)) {
                        include = false;
                        break;
                    }
                }
            }
        }

        if (!include) {
            return false;
        }

        if (includeTypeList != null && !includeTypeList.isEmpty()) {
            include = includeTypeList.contains(typeName);
            if (!include) {
                for (String s : includeTypeList) {
                    if (t.isNodeType(s)) {
                        include = true;
                        break;
                    }
                }
            }
        }

        return include;
    }

    public static Map<String, String> getComponentTypes(final JCRNodeWrapper node,
                                                        final List<String> includeTypeList, final List<String> excludeTypeList,
                                                        Locale displayLocale) throws PathNotFoundException, RepositoryException {

        if (displayLocale == null) {
            displayLocale = node.getSession().getLocale();
        }

        Map<String, String> finalComponents = new HashMap<String, String>();

        JCRSiteNode resolvedSite = node.getResolveSite();

        String[] constraints = Patterns.SPACE.split(ConstraintsHelper.getConstraints(node));

        Set<String> l = new HashSet<String>();
        l.add("system-jahia");

        if (resolvedSite != null) {
            l.addAll(resolvedSite.getInstalledModulesWithAllDependencies());
        }

        for (String aPackage : l) {
            for (ExtendedNodeType type : NodeTypeRegistry.getInstance().getNodeTypes(aPackage)) {
                if (allowType(type, includeTypeList, excludeTypeList)) {
                    for (String s : constraints) {
                        if (!finalComponents.containsKey(type.getName()) && type.isNodeType(s)) {
                            finalComponents.put(type.getName(), type.getLabel(displayLocale));
                            break;
                        }
                    }
                }
            }
        }

        SortedMap<String, String> sortedComponents = new TreeMap<String, String>(
                Ordering.from(String.CASE_INSENSITIVE_ORDER).onResultOf(Functions.forMap(finalComponents)));
        sortedComponents.putAll(finalComponents);

        return sortedComponents;
    }
}
