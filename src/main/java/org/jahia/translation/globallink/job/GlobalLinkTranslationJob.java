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
import org.jahia.translation.globallink.dto.GlobalLinkConfigurationDTO;
import org.jahia.translation.globallink.service.api.GlobalLinkQueryService;
import org.jahia.translation.globallink.service.api.GlobalLinkRetrieveDocumentService;
import org.jahia.translation.globallink.service.api.GlobalLinkSubmissionService;
import org.jahia.translation.globallink.service.api.GlobalLinkTranslatedContentProcessService;
import org.jahia.translation.globallink.util.JCRUtil;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.jahia.translation.globallink.common.GlobalLinkConstants.JCR_DEFAULT_WS;

/**
 * Implementation for Background translation job execution.
 *
 * @author Prince.Arora, WebItUp
 */
public class GlobalLinkTranslationJob extends BackgroundJob {

    @Override
    public void executeJahiaJob(JobExecutionContext context) throws Exception {
        JCRSessionWrapper sessionWrapper = JCRUtil.getRootSession(JCR_DEFAULT_WS);

        GlobalLinkQueryService globalLinkQueryService = BundleUtils.getOsgiService(GlobalLinkQueryService.class, null);
        GlobalLinkSubmissionService globalLinkSubmissionService = BundleUtils.getOsgiService(GlobalLinkSubmissionService.class, null);
        GlobalLinkRetrieveDocumentService globalLinkRetrieveDocumentService = BundleUtils.getOsgiService(GlobalLinkRetrieveDocumentService.class, null);
        GlobalLinkTranslatedContentProcessService globalLinkTranslatedContentProcessService = BundleUtils.getOsgiService(GlobalLinkTranslatedContentProcessService.class, null);

        List<JCRSiteNode> sites = globalLinkQueryService.getAllSites(sessionWrapper.getWorkspace().getQueryManager());
        List<GlobalLinkConfigurationDTO> configList = JCRUtil.getConfigurationList(sites);
        if (CollectionUtils.isNotEmpty(configList)) {
            globalLinkSubmissionService.submitSiteProjects(configList);
            globalLinkRetrieveDocumentService.retrieveCompletedProjects(configList);
            globalLinkTranslatedContentProcessService.processTranslatedContent(configList);
        }
    }
}
