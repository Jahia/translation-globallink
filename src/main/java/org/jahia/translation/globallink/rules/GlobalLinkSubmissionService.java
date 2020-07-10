package org.jahia.translation.globallink.rules;

import org.apache.commons.lang.StringUtils;
import org.drools.core.spi.KnowledgeHelper;
import org.gs4tr.gcc.restclient.GCExchange;
import org.gs4tr.gcc.restclient.model.Status;
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
import static org.jahia.translation.globallink.common.SubmissionStatus.STATUS_TRANSLATE;

/**
 * Created by rincevent on 2017-01-18.
 */
public class GlobalLinkSubmissionService {
    private GlobalLinkQueryServiceImpl queryService;
    private SiteContentService contentService;
    private MailService mailService;
    private JahiaUserManagerService userManagerService;

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalLinkSubmissionService.class);

    public void removeEmptySubmission(AddedNodeFact addedNodeFact, KnowledgeHelper drools) {

        JCRSessionWrapper rootSession = JCRUtil.getRootSession(JCR_DEFAULT_WS);
        List<GlobalLinkConfigurationDTO> configList = JCRUtil
                .getConfigurationList(queryService.getAllSites(rootSession.getWorkspace().getQueryManager()));
        JCRNodeWrapper node = addedNodeFact.getNode();
        for (GlobalLinkConfigurationDTO config : configList) {

            if (node.getPath().startsWith(config.getSiteNode().getPath())) {
                JCRNodeIteratorWrapper submittedRequests = queryService
                        .getSubmittedRequests(node.getPath(), rootSession.getWorkspace().getQueryManager());
                GCExchange gcExchange = GlobalLinkUtil.getGlobalLinkClient(config);
                if (gcExchange != null) {
                    for (JCRNodeWrapper request : submittedRequests) {
                        try {
                            Long submissionId = request.getProperty(GBL_PROJECT_SUB_TICKET).getLong();
                            Status submissionStatus = gcExchange.getSubmissionStatus(submissionId);
                            if (submissionStatus.getStatusName().equals(STATUS_TRANSLATE)) {
                                gcExchange.cancelSubmission(submissionId);
                                contentService.updateRequestStatus(request, rootSession, STATUS_CANCELLED);
                            }
                            drools.insert(new DeletedNodeFact(addedNodeFact, request.getPath()));
                        } catch (Exception e) {
                            LOGGER.error("Error while releting translation", e);
                        }
                    }
                }
            }
        }
    }

    public void sendNotification(AbstractNodeFact fact) {
        if (mailService.isEnabled()) {
            JCRNodeWrapper node = fact.getNode();
            try {
                JCRUserNode jcrUserNode = userManagerService.lookupUser(node.getCreationUser(), node.getSession());
                if (jcrUserNode.hasProperty("j:email")) {
                    MessageFormat messageFormat = new MessageFormat("Your translation submission {0} has changed status to {1}");
                    String name = node.getProperty("name").getString();
                    mailService.sendMessage(null, jcrUserNode.getProperty("j:email").getString(), null, null,
                            "Satus Update on your translation request " + name, messageFormat.format(new Object[] { name,
                                    StringUtils.substringAfterLast(node.getProperty("gblSubmitState").getString(), ".") }));
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
