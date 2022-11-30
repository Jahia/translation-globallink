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

import org.apache.commons.lang.StringUtils;
import org.jahia.registries.ServicesRegistry;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRValueWrapper;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.jahia.services.content.decorator.JCRUserNode;
import org.jahia.translation.globallink.dto.GlobalLinkConfigurationDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.jahia.translation.globallink.common.GlobalLinkConstants.*;

/**
 * Utility methods for JCR Repository
 *
 * @author Pricne.Arora, WebitUp.
 * @author Aashish.Kocchar, WebitUp.
 */
public class JCRUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(JCRUtil.class);

    /**
     * Get {@link JCRSessionWrapper} for root user
     *
     * @param workspace
     * @return
     */
    public static JCRSessionWrapper getRootSession(String workspace) {
        try {
            JCRUserNode rootUser = ServicesRegistry.getInstance().getJahiaUserManagerService().lookupRootUser();
            JCRSessionFactory sessionFactory = JCRSessionFactory.getInstance();
            sessionFactory.setCurrentUser(rootUser.getJahiaUser());
            return sessionFactory.getCurrentUserSession(workspace);
        } catch (Exception ex) {
            LOGGER.error("Exception -> ", ex);
            return null;
        }
    }

    public static GlobalLinkConfigurationDTO getSiteConfiguration(JCRSiteNode siteNode){
        List<GlobalLinkConfigurationDTO> configurationDTOS = getConfigurationList(Collections.singletonList(siteNode));
        return !configurationDTOS.isEmpty() ? configurationDTOS.get(0) : null;
    }
    /**
     * Preparing list of configurations for all the sites that have GLobal link
     * translation settings
     */
    public static List<GlobalLinkConfigurationDTO> getConfigurationList(List<JCRSiteNode> sites) {
        List<GlobalLinkConfigurationDTO> configList = new ArrayList<>();
        for (JCRSiteNode node : sites) {
            try {
                if (node.hasProperty(GBL_PROPERTY_USERNAME) && node.getProperty(GBL_PROPERTY_ENABLE).getBoolean()) {
                    GlobalLinkConfigurationDTO gblConfig = new GlobalLinkConfigurationDTO();
                    gblConfig.setUsername(node.getProperty(GBL_PROPERTY_USERNAME).getString());
                    gblConfig.setPassword(node.getProperty(GBL_PROPERTY_PASSWORD).getString());
                    gblConfig.setEnable(node.getProperty(GBL_PROPERTY_ENABLE).getBoolean());
                    gblConfig.setConnectorName(node.getProperty(GBL_PROPERTY_CONNECTOR_NAME).getString());
                    gblConfig.setSubmissionPrefix(node.getProperty(GBL_PROPERTY_PREFIX).getString());
                    gblConfig.setUrl(node.getProperty(GBL_PROPERTY_URL).getString());
                    gblConfig.setUserAgent(node.getProperty(GBL_PROPERTY_AGENT).getString());
                    gblConfig.setFileFormat(node.getProperty(GBL_PROPERTY_FORMAT).getString());
                    gblConfig.setDocumentPath(node.getPropertyAsString(GBL_PROPERTY_DOC_LOCATION));
                    if (node.hasProperty(GBL_PROPERTY_COMPONENTS)) {
                        gblConfig.setComponentList(getComponentListFromConf(node.
                                getProperty(GBL_PROPERTY_COMPONENTS).getValues()));
                    }
                    gblConfig.setSiteNode(node);
                    configList.add(gblConfig);
                }
            } catch (RepositoryException ex) {
                LOGGER.error("Error while preparing config -> ", ex);
            }
        }
        return configList;
    }

    /**
     * Get list of components configured in Global link configuration
     * pages.
     *
     * @param valueWrappers
     * @return
     */
    public static List<String> getComponentListFromConf(JCRValueWrapper[] valueWrappers) {
        List<String> componentList = new ArrayList<>();
        if (valueWrappers != null && valueWrappers.length > 0) {
            for (JCRValueWrapper jcrValueWrapper : valueWrappers) {
                try {
                    componentList.add(StringUtils.substringAfter(jcrValueWrapper.getString(), "-"));
                } catch (RepositoryException re) {
                    LOGGER.error("Error while getting component value -> ", re);
                }
            }
        }
        return componentList;
    }
}
