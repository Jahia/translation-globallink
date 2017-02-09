package org.jahia.translation.globallink.rules;

import com.globallink.api.GLExchange;
import org.apache.commons.lang.StringUtils;
import org.drools.core.spi.KnowledgeHelper;
import org.jahia.services.content.JCRNodeIteratorWrapper;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.decorator.JCRUserNode;
import org.jahia.services.content.rules.AbstractNodeFact;
import org.jahia.services.content.rules.AddedNodeFact;
import org.jahia.services.content.rules.DeletedNodeFact;
import org.jahia.services.mail.MailService;
import org.jahia.services.usermanager.JahiaUserManagerService;
import org.jahia.translation.globallink.dto.GlobalLinkConfigurationDTO;
import org.jahia.translation.globallink.service.api.SiteContentService;
import org.jahia.translation.globallink.service.impl.GlobalLinkQueryServiceImpl;
import org.jahia.translation.globallink.service.impl.GlobalLinkSubmissionServiceImpl;
import org.jahia.translation.globallink.util.GlobalLinkUtil;
import org.jahia.translation.globallink.util.JCRUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.text.MessageFormat;
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
    private MailService mailService;
    private JahiaUserManagerService userManagerService;

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalLinkSubmissionServiceImpl.class);

    public void setSubmissionService(GlobalLinkSubmissionServiceImpl submissionService) {
        this.submissionService = submissionService;
    }

    public void removeEmptySubmission(AddedNodeFact addedNodeFact, KnowledgeHelper drools) {
        List<GlobalLinkConfigurationDTO> globalLinkConfigurationDTOS = submissionService.submitSiteProjects();
        JCRNodeWrapper node = addedNodeFact.getNode();
        globalLinkConfigurationDTOS.forEach(config -> {
            if (node.getPath().startsWith(config.getSiteNode().getPath())) {
                JCRSessionWrapper rootSession = JCRUtil.getRootSession(JCR_DEFAULT_WS);
                JCRNodeIteratorWrapper submittedRequests = queryService.getSubmittedRequests(node.getPath(), rootSession.getWorkspace().getQueryManager());
                GLExchange glExchange = GlobalLinkUtil.getGLExchangeClient(config);
                if (glExchange != null) {
                    submittedRequests.forEach((request) -> {
                        try {
                            String submissionTicket = request.getPropertyAsString(GBL_PROJECT_SUB_TICKET);
                            String submissionStatus = glExchange.getSubmissionStatus(submissionTicket);
                            if (submissionStatus.equals(STATUS_READY)) {
                                glExchange.cancelSubmission(submissionTicket, "Content has been deleted on " + config.getSiteNode().getServerName());
                                contentService.updateRequestStatus(request, rootSession, STATUS_CANCELLED);
                            }
                            drools.insert(new DeletedNodeFact(addedNodeFact, request.getPath()));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                }
            }
        });
    }

    public void sendNotification(AbstractNodeFact fact, KnowledgeHelper drools) {
        if (mailService.isEnabled()) {
            JCRNodeWrapper node = fact.getNode();
            try {
                JCRUserNode jcrUserNode = userManagerService.lookupUser(node.getCreationUser(), node.getSession());
                if (jcrUserNode.hasProperty("j:email")) {
                    MessageFormat messageFormat = new MessageFormat("Your translation submission {0} has changed status to {1}");
                    String name = node.getProperty("name").getString();
                    mailService.sendMessage(null, jcrUserNode.getProperty("j:email").getString(), null, null, "Satus Update on your translation request "+name,
                            messageFormat.format(new Object[]{
                                    name,
                                    StringUtils.substringAfterLast(node.getProperty("gblSubmitState").getString(), ".")
                            }));
                }

            } catch (RepositoryException e) {
                LOGGER.debug("Accessing property on a deleted node");
            }
        }
    }

    public void setQueryService(GlobalLinkQueryServiceImpl queryService) {
        this.queryService = queryService;
    }

    public void setContentService(SiteContentService contentService) {
        this.contentService = contentService;
    }

    public void setMailService(MailService mailService) {
        this.mailService = mailService;
    }

    public void setUserManagerService(JahiaUserManagerService userManagerService) {
        this.userManagerService = userManagerService;
    }
}
