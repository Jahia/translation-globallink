package org.jahia.translation.globallink.job;

import org.apache.commons.collections.CollectionUtils;
import org.jahia.services.SpringContextSingleton;
import org.jahia.services.scheduler.BackgroundJob;
import org.jahia.translation.globallink.dto.GlobalLinkConfigurationDTO;
import org.jahia.translation.globallink.service.api.GlobalLinkRetrieveDocumentService;
import org.jahia.translation.globallink.service.api.GlobalLinkSubmissionService;
import org.jahia.translation.globallink.service.api.GlobalLinkTranslatedContentProcessService;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Implementation for Background translation job execution.
 *
 * @author Prince.Arora, WebItUp
 */
public class GlobalLinkTranslationJob extends BackgroundJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalLinkTranslationJob.class);

    @Override
    public void executeJahiaJob(JobExecutionContext context) throws Exception {
        LOGGER.info("Inside GBL Translation background Job Execution");
        List<GlobalLinkConfigurationDTO> configList = ((GlobalLinkSubmissionService) SpringContextSingleton
                .getBean("submissionService")).submitSiteProjects();
        if (CollectionUtils.isNotEmpty(configList)) {
            ((GlobalLinkRetrieveDocumentService) SpringContextSingleton.getBean("retrieveDocumentService"))
                    .retrieveCompletedProjects(configList);
            ((GlobalLinkTranslatedContentProcessService) SpringContextSingleton
                    .getBean("translatedContentProcessService")).processTranslatedContent(configList);
        }
    }

}
