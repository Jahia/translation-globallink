package org.jahia.translation.globallink.rules;

import com.globallink.api.GLExchange;
import org.drools.core.spi.KnowledgeHelper;
import org.jahia.services.content.JCRNodeIteratorWrapper;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.rules.AddedNodeFact;
import org.jahia.services.content.rules.DeletedNodeFact;
import org.jahia.translation.globallink.dto.GlobalLinkConfigurationDTO;
import org.jahia.translation.globallink.service.api.SiteContentService;
import org.jahia.translation.globallink.service.impl.GlobalLinkQueryServiceImpl;
import org.jahia.translation.globallink.service.impl.GlobalLinkSubmissionServiceImpl;
import org.jahia.translation.globallink.util.GlobalLinkUtil;
import org.jahia.translation.globallink.util.JCRUtil;

import javax.jcr.RepositoryException;
import java.util.List;

import static org.jahia.translation.globallink.common.GlobalLinkConstants.GBL_PROJECT_SUB_TICKET;
import static org.jahia.translation.globallink.common.GlobalLinkConstants.JCR_DEFAULT_WS;
import static org.jahia.translation.globallink.common.SubmissionStatus.STATUS_CANCELLED;
import static org.jahia.translation.globallink.common.SubmissionStatus.STATUS_READY;

/**
 * Created by rincevent on 2017-01-18.
 */
public class GlobalLinkSubmissionService {
    private GlobalLinkSubmissionServiceImpl submissionService;
    private GlobalLinkQueryServiceImpl queryService;
    private SiteContentService contentService;

    public void setSubmissionService(GlobalLinkSubmissionServiceImpl submissionService) {
        this.submissionService = submissionService;
    }

    public GlobalLinkSubmissionServiceImpl getSubmissionService() {
        return submissionService;
    }

    public void removeEmptySubmission(AddedNodeFact addedNodeFact, KnowledgeHelper drools) {
        List<GlobalLinkConfigurationDTO> globalLinkConfigurationDTOS = submissionService.submitSiteProjects();
        JCRNodeWrapper node = addedNodeFact.getNode();
        globalLinkConfigurationDTOS.forEach(config -> {
            if(node.getPath().startsWith(config.getSiteNode().getPath())) {
                JCRSessionWrapper rootSession = JCRUtil.getRootSession(JCR_DEFAULT_WS);
                JCRNodeIteratorWrapper submittedRequests = queryService.getSubmittedRequests(node.getPath(), rootSession.getWorkspace().getQueryManager());
                GLExchange glExchange = GlobalLinkUtil.getGLExchangeClient(config);
                if(glExchange !=null ) {
                    submittedRequests.forEach((request) -> {
                        try {
                            String submissionTicket = request.getPropertyAsString(GBL_PROJECT_SUB_TICKET);
                            String submissionStatus = glExchange.getSubmissionStatus(submissionTicket);
                            if (submissionStatus.equals(STATUS_READY)) {
                                glExchange.cancelSubmission(submissionTicket, "Content has been deleted on " + config.getSiteNode().getServerName());
                                contentService.updateRequestStatus(request, rootSession, STATUS_CANCELLED);
                            }
                            drools.insert(new DeletedNodeFact(addedNodeFact, request.getPath()));
                        } catch (RepositoryException e) {
                            e.printStackTrace();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                }
            }
        });
    }

    public void setQueryService(GlobalLinkQueryServiceImpl queryService) {
        this.queryService = queryService;
    }

    public GlobalLinkQueryServiceImpl getQueryService() {
        return queryService;
    }

    public void setContentService(SiteContentService contentService) {
        this.contentService = contentService;
    }
}
