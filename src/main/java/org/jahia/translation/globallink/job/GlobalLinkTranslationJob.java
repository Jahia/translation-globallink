package org.jahia.translation.globallink.job;

import org.apache.commons.collections.CollectionUtils;
import org.jahia.services.SpringContextSingleton;
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

    private GlobalLinkQueryService gblQueryService;

    public void setGblQueryService(GlobalLinkQueryService gblQueryService) {
        this.gblQueryService = gblQueryService;
    }

    @Override public void executeJahiaJob(JobExecutionContext context) throws Exception {
        LOGGER.info("Inside GBL Translation background Job Execution");
        JCRSessionWrapper sessionWrapper = JCRUtil.getRootSession(JCR_DEFAULT_WS);
        List<JCRSiteNode> sites = ((GlobalLinkQueryService) SpringContextSingleton.getBean("globalLinkQueryService"))
                .getAllSites(sessionWrapper.getWorkspace().getQueryManager());
        List<GlobalLinkConfigurationDTO> configList = JCRUtil.getConfigurationList(sites);
        if (CollectionUtils.isNotEmpty(configList)) {
            ((GlobalLinkSubmissionService) SpringContextSingleton.getBean("submissionService")).submitSiteProjects(configList);
            ((GlobalLinkRetrieveDocumentService) SpringContextSingleton.getBean("retrieveDocumentService"))
                    .retrieveCompletedProjects(configList);
            ((GlobalLinkTranslatedContentProcessService) SpringContextSingleton.getBean("translatedContentProcessService"))
                    .processTranslatedContent(configList);
        }
    }

}
