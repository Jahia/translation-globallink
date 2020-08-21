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

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalLinkTranslationJob.class);

    @Override public void executeJahiaJob(JobExecutionContext context) throws Exception {
        LOGGER.info("Inside GBL Translation background Job Execution");
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
