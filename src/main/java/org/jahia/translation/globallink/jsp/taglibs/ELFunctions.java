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
package org.jahia.translation.globallink.jsp.taglibs;

import org.gs4tr.gcc.restclient.GCExchange;
import org.gs4tr.gcc.restclient.model.Connector;
import org.gs4tr.gcc.restclient.model.LanguageDirection;
import org.gs4tr.gcc.restclient.model.LocaleConfig;
import org.gs4tr.gcc.restclient.operation.ConnectorsConfig.ConnectorsConfigResponseData;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRValueWrapper;
import org.jahia.services.render.scripting.Script;
import org.jahia.translation.globallink.common.GlobalLinkConstants;
import org.jahia.translation.globallink.dto.GlobalLinkConfigurationDTO;
import org.jahia.translation.globallink.util.GlobalLinkUtil;
import org.jahia.translation.globallink.util.JCRUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Class for all ELFunctions for Global link translation jsp taglibs.
 *
 * @author Prince.Arora, Webitup
 */
public class ELFunctions {

    private static final Logger LOGGER = LoggerFactory.getLogger(ELFunctions.class);

    /**
     * Get list of all the components allowed in current site.
     *
     * @param nodeWrapper
     * @param locale
     * @param script
     * @return
     */
    public static Map<String, String> getComponentList(JCRNodeWrapper nodeWrapper, Locale locale, Script script,
            JCRValueWrapper[] valueWrappers) {
        try {
            return GlobalLinkUtil.filterComponentsList(
                    GlobalLinkUtil.getComponentTypes(nodeWrapper, null, GlobalLinkUtil.getExcludedComponents(valueWrappers), locale));
        } catch (Exception ex) {
            LOGGER.error("Error while fetching components list -> ", ex);
        }
        return null;
    }

    /**
     * Check a global link configuration settings and populate language
     * mapping
     *
     * @param nodeWrapper
     * @return
     * @throws RepositoryException
     */
    public static String checkAndGetProjectInfo(JCRNodeWrapper nodeWrapper) throws RepositoryException {
        String languageMappingValue = null;
        if (nodeWrapper != null) {
            GlobalLinkConfigurationDTO configuration = JCRUtil.getSiteConfiguration(nodeWrapper.getResolveSite());
            if (configuration != null) {
                GCExchange gcExchange = Optional.of(configuration).map(GlobalLinkUtil::getGlobalLinkClient).orElse(null);

                if (gcExchange != null) {
                    try {
                        languageMappingValue = getAvailableLanguageMapping(gcExchange.getConnectorsConfig());
                    } catch (Exception e) {
                        languageMappingValue = e.getLocalizedMessage();
                    }
                } else {
                    languageMappingValue = "NA";
                }
            } else {
                languageMappingValue = "NS";
            }
        }
        return languageMappingValue;
    }

    private static String getAvailableLanguageMapping(ConnectorsConfigResponseData connectorsConfigResponseData) {
        List<LanguageDirection> languageMappingList = connectorsConfigResponseData.getLanguageDirections();
        String languageDirectionValue = "";
        if (languageMappingList.isEmpty()) {
            Optional<LocaleConfig> sourceLocaleConfigOptional = connectorsConfigResponseData.getSupportedLocales().stream()
                    .filter(LocaleConfig::getIsSource).findFirst();

            if (sourceLocaleConfigOptional.isPresent()) {
                LocaleConfig localeConfig = sourceLocaleConfigOptional.get();
                languageDirectionValue = connectorsConfigResponseData.getSupportedLocales().stream().filter(locale -> !locale.getIsSource())
                        .map(languageDirection -> new StringBuilder().append(localeConfig.getConnectorLocale()).append(" -> ")
                                .append(languageDirection.getConnectorLocale())).collect(Collectors.joining(", "));
            }
        } else {
            languageDirectionValue = languageMappingList.stream()
                    .map(languageDirection -> new StringBuilder().append(languageDirection.getSourceLocale()).append(" -> ")
                            .append(languageDirection.getTargetLocale())).collect(Collectors.joining(", "));
        }
        return languageDirectionValue;
    }

    public static JCRNodeWrapper getNodeFromId(String nodeId) throws RepositoryException {
        JCRSessionWrapper jcrSessionWrapper = JCRSessionFactory.getInstance().getCurrentUserSession(GlobalLinkConstants.JCR_DEFAULT_WS);
        return jcrSessionWrapper.getNodeByUUID(nodeId).getNode("j:translation_en");
    }

    public static String displayLocale(String locale, Locale toDisplayNameWith) {
        return Locale.forLanguageTag(locale).getDisplayName(toDisplayNameWith);
    }
}
