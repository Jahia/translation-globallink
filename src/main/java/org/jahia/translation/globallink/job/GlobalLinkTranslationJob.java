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
package org.jahia.translation.globallink.job;

import org.apache.commons.collections.CollectionUtils;
import org.jahia.osgi.BundleUtils;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.jahia.services.scheduler.BackgroundJob;
import org.jahia.translation.globallink.action.GlobalLinkConfigAction;
import org.jahia.translation.globallink.dto.GlobalLinkConfigurationDTO;
import org.jahia.translation.globallink.service.api.GlobalLinkQueryService;
import org.jahia.translation.globallink.service.api.GlobalLinkRetrieveDocumentService;
import org.jahia.translation.globallink.service.api.GlobalLinkSubmissionService;
import org.jahia.translation.globallink.service.api.GlobalLinkTranslatedContentProcessService;
import org.jahia.translation.globallink.util.JCRUtil;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.util.Calendar;
import java.util.List;
import java.util.stream.Collectors;

import static org.jahia.translation.globallink.common.GlobalLinkConstants.*;

/**
 * Implementation for Background translation job execution.
 *
 * @author Prince.Arora, WebItUp
 */
public class GlobalLinkTranslationJob extends BackgroundJob {
    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalLinkConfigAction.class);

    @Override
    public void executeJahiaJob(JobExecutionContext context) {
        JCRSessionWrapper sessionWrapper = JCRUtil.getRootSession(JCR_DEFAULT_WS);

        GlobalLinkQueryService globalLinkQueryService = BundleUtils.getOsgiService(GlobalLinkQueryService.class, null);
        GlobalLinkSubmissionService globalLinkSubmissionService = BundleUtils.getOsgiService(GlobalLinkSubmissionService.class, null);
        GlobalLinkRetrieveDocumentService globalLinkRetrieveDocumentService = BundleUtils.getOsgiService(GlobalLinkRetrieveDocumentService.class, null);
        GlobalLinkTranslatedContentProcessService globalLinkTranslatedContentProcessService = BundleUtils.getOsgiService(GlobalLinkTranslatedContentProcessService.class, null);

        List<JCRSiteNode> sites = globalLinkQueryService.getAllSites(sessionWrapper.getWorkspace().getQueryManager());
        List<GlobalLinkConfigurationDTO> configList = JCRUtil.getConfigurationList(sites).stream().filter(this::isIntervalReached).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(configList)) {
            globalLinkSubmissionService.submitSiteProjects(configList);
            globalLinkRetrieveDocumentService.retrieveCompletedProjects(configList);
            globalLinkTranslatedContentProcessService.processTranslatedContent(configList);
            configList.forEach((config) -> {
                    try {
                        Calendar lastExecution = Calendar.getInstance();
                        lastExecution.set(Calendar.SECOND, 0);
                        config.getSiteNode().setProperty(GBL_PROPERTY_LAST_EXEC, lastExecution);
                        sessionWrapper.save();
                    } catch (RepositoryException e) {
                        LOGGER.error("Error while updating last execution time: ", e);
                    }
                }
            );
        }
    }

    /**
     * Check if the interval is reached for the given configuration
     * @param config {@link GlobalLinkConfigurationDTO} configuration of a site to check
     * @return true if the interval is reached, false otherwise
     */
    private boolean isIntervalReached(GlobalLinkConfigurationDTO config) {
        try {
            if (config.getSiteNode().hasProperty(GBL_PROPERTY_LAST_EXEC) && config.getSiteNode().hasProperty(GBL_PROPERTY_INTERVAL)) {
                Calendar lastExecuted = config.getSiteNode().getProperty(GBL_PROPERTY_LAST_EXEC).getDate();
                lastExecuted.add(Calendar.MINUTE,
                    Integer.parseInt((String.valueOf(config.getSiteNode().getProperty(GBL_PROPERTY_INTERVAL).getLong()))));
                return Calendar.getInstance().after(lastExecuted);
            }
            return true;
        } catch (RepositoryException re) {
            LOGGER.error("Error while checking submission interval -> ", re);
        }
        return false;
    }
}
