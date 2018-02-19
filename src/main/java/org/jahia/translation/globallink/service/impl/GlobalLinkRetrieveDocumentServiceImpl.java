package org.jahia.translation.globallink.service.impl;

import com.globallink.api.GLExchange;
import com.globallink.api.model.Target;
import org.apache.commons.lang.StringUtils;
import org.jahia.services.content.JCRNodeIteratorWrapper;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.mail.MailService;
import org.jahia.translation.globallink.dto.GlobalLinkConfigurationDTO;
import org.jahia.translation.globallink.service.api.GlobalLinkQueryService;
import org.jahia.translation.globallink.service.api.GlobalLinkRetrieveDocumentService;
import org.jahia.translation.globallink.service.api.SiteContentService;
import org.jahia.translation.globallink.util.GlobalLinkUtil;
import org.jahia.translation.globallink.util.IOUtil;
import org.jahia.translation.globallink.util.JCRUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.io.File;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.jahia.translation.globallink.common.GlobalLinkConstants.*;
import static org.jahia.translation.globallink.common.SubmissionStatus.*;

/**
 * Document service to retrieve all translated documents from Global Link PD.
 *
 * @author Rakesh.Kumar, WebitUp.
 * @author Prince.Arora, WebItUp.
 */
public class GlobalLinkRetrieveDocumentServiceImpl implements GlobalLinkRetrieveDocumentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalLinkRetrieveDocumentServiceImpl.class);

    private JCRSessionWrapper sessionWrapper;

    private SiteContentService contentService;

    private GlobalLinkQueryService queryService;

    private MailService mailService;

    /**
     * {@inheritDoc}
     */
    @Override
    public List<GlobalLinkConfigurationDTO> retrieveCompletedProjects(List<GlobalLinkConfigurationDTO> configList) {
        try {
            LOGGER.info("====  Initializing Retrieve process  =====");
            this.sessionWrapper = JCRUtil.getRootSession(JCR_DEFAULT_WS);
            for (GlobalLinkConfigurationDTO config : configList) {
                this.retrieveDocuments(config);
            }
        } catch (Exception ex) {
            LOGGER.error("Exception while starting document retrieve process -> ", ex);
        }
        return configList;
    }

    /**
     * Retrieve completed documents from Global link PD
     *
     * @param config
     */
    private void retrieveDocuments(GlobalLinkConfigurationDTO config) {
        JCRNodeIteratorWrapper submittedRequests = this.queryService.getSubmittedRequests(config.getSiteNode().getPath(),
                this.sessionWrapper.getWorkspace().getQueryManager());
        GLExchange glExchange = GlobalLinkUtil.getGLExchangeClient(config);
        for (JCRNodeWrapper request : submittedRequests) {
            processRequestForRetrieval(request, glExchange, config);
        }
    }

    /**
     * Process a request for retrieval of all the completed targets and
     * translated documents.
     *
     * @param requestNode Noe containing the request data
     * @param glExchange
     * @param config
     */
    private void processRequestForRetrieval(JCRNodeWrapper requestNode, GLExchange glExchange,
                                            GlobalLinkConfigurationDTO config) {
        String submissionTicket = requestNode.getPropertyAsString(GBL_PROJECT_SUB_TICKET);
        if (!StringUtils.isEmpty(submissionTicket)) {
            Target[] targets = glExchange.getCompletedTargets(submissionTicket, 100);
            List<Target> completedTargets = new ArrayList<>();
            if (targets.length > 0) {
                for (Target target : targets) {

                    boolean status = processTarget(target, glExchange, config);
                    if (status) {
                        completedTargets.add(target);
                    }
                }
            } else {
                targets = glExchange.getCancelledTargets(submissionTicket, 100);
                if (targets.length > 0) {
                    for (Target target : targets) {
                        try {
                            if (requestNode.getProperty(GBL_PROJECT_UPLOAD_TICKET).getString().equals(target.getDocumentTicket())) {
                                this.contentService.updateRequestStatus(requestNode, this.sessionWrapper, STATUS_CANCELLED);
                            }
                        } catch (RepositoryException e) {
                            LOGGER.error("Error cancelling submission - ", e);
                        }
                    }
                } else {
                    try {
                        String submissionStatus = glExchange.getSubmissionStatus(submissionTicket);
                        if (submissionStatus == null) {
                            this.contentService.updateRequestStatus(requestNode, this.sessionWrapper, STATUS_DELETED);
                        } else if (submissionStatus.equals(STATUS_READY)) {
                            this.contentService.updateRequestStatus(requestNode, this.sessionWrapper, STATUS_SUBMITTED);
                        }
                    } catch (Exception e) {
                        LOGGER.error("Error retrieving translated document - ", e);
                    }
                }
            }
        }
    }

    /**
     * Process and save translated document from completed target.
     *
     * @param target
     * @param glExchange
     * @return
     */
    private boolean processTarget(Target target, GLExchange glExchange, GlobalLinkConfigurationDTO config) {
        try {
            LOGGER.info("Ticket: {}", target.getTicket());
            JCRNodeWrapper requestNode = (JCRNodeWrapper) this.queryService.
                    getSubmissionNodeByDocumentTicket(target.getDocumentTicket(),
                            this.sessionWrapper.getWorkspace().getQueryManager()).next();
            this.contentService.unLockNode(requestNode.getParent(), this.sessionWrapper);
            String docPath = "";
            if (config.getDocumentPath() != null && !config.getDocumentPath().equals("")) {
                docPath = config.getDocumentPath() + File.separator + requestNode.getPropertyAsString(GBL_PROJECT_REQUEST_ID)
                        + File.separator + TRANSLATED_PATH;
            } else {
                docPath = DOCUMENT_PATH + File.separator + requestNode.getPropertyAsString(GBL_PROJECT_REQUEST_ID)
                        + File.separator + TRANSLATED_PATH;
            }
            IOUtil.createDirectories(FileSystems.getDefault().getPath(docPath));
            String fileName = target.getTargetLocale() + "_" + StringUtils.substringAfterLast(target.getDocumentName(), "_");
            String filePath = docPath + File.separator + fileName;
            if (IOUtil.createFile(target.getData(glExchange), filePath)) {
                glExchange.sendDownloadConfirmation(target.getTicket());
            }
            this.contentService.updateRequestStatus(requestNode, this.sessionWrapper, STATUS_RETRIEVED);
            String targetStatus = target.getTargetLocale() + "_" + target.getTicket() + "_" + target.getWordCount().getTotal();
            this.contentService.addTargetTicketsInStatus(requestNode, targetStatus, this.sessionWrapper);
            return true;
        } catch (Exception ex) {
            LOGGER.error("Error retrieving translated document - ", ex);
            return false;
        }
    }

    public void setContentService(SiteContentService contentService) {
        this.contentService = contentService;
    }

    public void setQueryService(GlobalLinkQueryService queryService) {
        this.queryService = queryService;
    }
}
