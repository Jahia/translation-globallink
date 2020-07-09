package org.jahia.translation.globallink.service.impl;

import org.apache.commons.lang.StringUtils;
import org.gs4tr.gcc.restclient.GCExchange;
import org.gs4tr.gcc.restclient.operation.SubmissionSubmit.SubmissionSubmitResponseData;
import org.gs4tr.gcc.restclient.request.SubmissionSubmitRequest;
import org.gs4tr.gcc.restclient.request.UploadFileRequest;
import org.jahia.services.content.JCRContentUtils;
import org.jahia.services.content.JCRNodeIteratorWrapper;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.jahia.services.content.decorator.JCRUserNode;
import org.jahia.services.mail.MailServiceImpl;
import org.jahia.services.usermanager.JahiaUserManagerService;
import org.jahia.translation.globallink.dto.GlobalLinkConfigurationDTO;
import org.jahia.translation.globallink.dto.GlobalLinkProjectRequestDTO;
import org.jahia.translation.globallink.exception.GlobalLinkServiceException;
import org.jahia.translation.globallink.service.api.GlobalLinkDocumentService;
import org.jahia.translation.globallink.service.api.GlobalLinkQueryService;
import org.jahia.translation.globallink.service.api.GlobalLinkSubmissionService;
import org.jahia.translation.globallink.service.api.SiteContentService;
import org.jahia.translation.globallink.util.GlobalLinkUtil;
import org.jahia.translation.globallink.util.JCRUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Value;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.jahia.translation.globallink.common.GlobalLinkConstants.*;
import static org.jahia.translation.globallink.common.SubmissionStatus.STATUS_NO_DOCUMENT;

/**
 * Implementation for Global link translation project submission service
 *
 * @author Rakesh.Kumar, WebitUp.
 */
public class GlobalLinkSubmissionServiceImpl implements GlobalLinkSubmissionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalLinkSubmissionServiceImpl.class);

    private static final String WITHOUT = " without ";
    public static final String DUE_DATE = "dueDate";

    private GlobalLinkQueryService gblQueryService;

    private GlobalLinkDocumentService documentService;

    private JCRSessionWrapper sessionWrapper;

    private SiteContentService contentService;
    private MailServiceImpl mailService;
    private JahiaUserManagerService userManagerService;

    public void setMailService(MailServiceImpl mailService) {
        this.mailService = mailService;
    }

    public void setGblQueryService(GlobalLinkQueryService gblQueryService) {
        this.gblQueryService = gblQueryService;
    }

    public void setDocumentService(GlobalLinkDocumentService documentService) {
        this.documentService = documentService;
    }

    public void setContentService(SiteContentService contentService) {
        this.contentService = contentService;
    }

    public void setUserManagerService(JahiaUserManagerService userManagerService) {
        this.userManagerService = userManagerService;
    }

    /**
     * {@inheritDoc}
     */
    @Override public void submitSiteProjects(List<GlobalLinkConfigurationDTO> configList) {
        try {
            LOGGER.info("====  Initializing submission process  =====");
            this.sessionWrapper = JCRUtil.getRootSession(JCR_DEFAULT_WS);
            for (GlobalLinkConfigurationDTO config : configList) {
                LOGGER.info("Site found with GBL Translation config - {} Sitename - {}", config.getUsername(),
                        config.getSiteNode().getName());
                this.processAllGBLTranslationProjects(config);
            }
        } catch (Exception ex) {
            LOGGER.error("Exception while starting submission process -> ", ex);
        }
    }

    /**
     * Check and process all the project available under a site.
     *
     * @param config globallink API configuration
     */
    private void processAllGBLTranslationProjects(GlobalLinkConfigurationDTO config) {
        GCExchange gcExchange = GlobalLinkUtil.getGlobalLinkClient(config);
        if (gcExchange == null && mailService.isEnabled()) {
            String to = mailService.defaultRecipient();
            String from = mailService.defaultSender();
            mailService.sendMessage(from, to, null, null,
                    "GlobalLink Translation settings are not valid or the GlobalLink Translation Server may be down!", null,
                    "GlobalLink Translation Server is not reachable for the web site: " + config.getSiteNode().getSiteKey() + ".<br> "
                            + "Configuration settings might be out of date, or the GlobalLink Translation Server may be down or your internet settings may be down.<br> "
                            + "Please check the GlobalLink Translation Server settings for the web site: " + config.getSiteNode()
                            .getSiteKey() + ".<br>" + "Please check the Jahia LOG files and outputs for further details!<br>");
        }
        JCRNodeIteratorWrapper projects = this.gblQueryService
                .getGBLRequests(config.getSiteNode(), this.sessionWrapper.getWorkspace().getQueryManager());
        for (JCRNodeWrapper project : projects)
            try {
                if (checkInterval(config) && (!project.hasProperty(GBL_SUBMISSION_STATE) || !project.hasProperty(GBL_PROJECT_REQUEST_ID))
                        && !project.hasProperty(GBL_PROJECT_ERROR)) {
                    LOGGER.info("processing project node: {}", project.getPath());
                    GlobalLinkProjectRequestDTO projectRequestDTO = buildProjectRequestDTO(project, config);

                    this.contentService.addRequestId(project, this.sessionWrapper, projectRequestDTO.getRequestId());
                    processRequestDTO(projectRequestDTO, gcExchange, config);
                    config.getSiteNode().setProperty(GBL_PROPERTY_LAST_EXEC, Calendar.getInstance());
                    this.sessionWrapper.save();
                }
            } catch (RepositoryException | GlobalLinkServiceException ex) {
                LOGGER.error("Error while collecting project info for - " + project.getPath() + " Exception -> ", ex);
            }
    }

    private GlobalLinkProjectRequestDTO buildProjectRequestDTO(JCRNodeWrapper project, GlobalLinkConfigurationDTO config)
            throws RepositoryException {
        GlobalLinkProjectRequestDTO projectRequestDTO = new GlobalLinkProjectRequestDTO();
        projectRequestDTO.setFileFormat(StringUtils.substringAfter(config.getFileFormat(), "_").toLowerCase());
        String sourceLanguage = project.getProperty(GBL_PROJECT_SOURCE_LANG).getString();
        projectRequestDTO.setSourceLanguage(sourceLanguage);
        ArrayList<String> allTargetLanguages = new ArrayList<>();
        for (Value targetLanguages : project.getProperty(GBL_PROJECT_TARGET_LANG).getValues()) {
            allTargetLanguages.add(targetLanguages.getString());
        }
        projectRequestDTO.setDesLanguages(allTargetLanguages.toArray(new String[0]));
        projectRequestDTO.setNodeWrapper(project);
        projectRequestDTO.setRequestId(UUID.randomUUID().toString());
        projectRequestDTO.setDocumentpath(config.getDocumentPath());
        if (project.hasProperty(GBL_SKIP_TRANSLATED)) {
            projectRequestDTO.setSkipTranslated(project.getProperty(GBL_SKIP_TRANSLATED).getBoolean());
        } else {
            projectRequestDTO.setSkipTranslated(false);
        }
        return projectRequestDTO;
    }

    private void processDocumentForProject(GlobalLinkProjectRequestDTO requestDTO, GlobalLinkConfigurationDTO config)
            throws RepositoryException {

        if (this.documentService.createDocumentForProject(requestDTO, requestDTO.getNodeWrapper().getParent(), requestDTO.getNodeWrapper(),
                config.getComponentList(), sessionWrapper)) {

            String documentName = GlobalLinkUtil.getSourceDocumentPath(requestDTO, requestDTO.getNodeWrapper().getParent());

            Optional.ofNullable(prepareGlobalLinkDocument(documentName, config.getFileFormat()))
                    .ifPresent(uploadFileRequest -> requestDTO.getUploadFileRequests().add(uploadFileRequest));
        }
    }

    /**
     * Process Global link submission for a given project DTO
     *
     * @param requestDTO
     * @return
     */
    private boolean submitGBLRequest(GlobalLinkProjectRequestDTO requestDTO, GlobalLinkConfigurationDTO config, GCExchange gcExchange) {
        String submissionName;
        Date dueDate;
        String[] split = requestDTO.getSourceLanguage().split("###");
        String sourceLanguage = split[1];
        List<String> targetLanguages = new LinkedList<>();
        for (String targetLanguage : requestDTO.getDesLanguages()) {
            targetLanguages.add(StringUtils.substringAfter(targetLanguage, "###"));
        }
        JCRNodeWrapper projectRootNode = requestDTO.getNodeWrapper();
        try {
            String jahiaSourceLanguage = split[0];
            JCRSiteNode siteNode = config.getSiteNode();
            if (!siteNode.getLanguages().contains(jahiaSourceLanguage)) {
                throw new GlobalLinkServiceException("no source lang matching in site");
            }
            JCRNodeWrapper parent = projectRootNode.getParent();
            String pageTitle;
            if (parent.hasNode("j:translation_" + jahiaSourceLanguage)) {
                pageTitle = parent.getNode("j:translation_" + jahiaSourceLanguage).getProperty("jcr:title").getString();
            } else {
                pageTitle = parent.getName();
            }

            if (projectRootNode.hasProperty("name")) {
                submissionName =
                        projectRootNode.getProperty("name").getString() + (requestDTO.isChildIncluded() ? " " + "with" + " " : WITHOUT)
                                + "sub pages";
            } else {
                submissionName =
                        config.getSubmissionPrefix() + " - from page " + pageTitle + (requestDTO.isChildIncluded() ? " with " : WITHOUT)
                                + " sub pages";
            }
            projectRootNode.setProperty("name", submissionName);
            projectRootNode.getSession().save();

            String pmNotes = "Translation for - " + pageTitle + (requestDTO.isChildIncluded() ? " with " : WITHOUT) + " sub pages\n"
                    + "form the web site - " + siteNode.getServerName() + "( " + siteNode.getTitle() + " )";
            if (projectRootNode.hasProperty(DUE_DATE)) {
                dueDate = projectRootNode.getProperty(DUE_DATE).getDate().getTime();
            } else {
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.DAY_OF_YEAR, 5);
                dueDate = calendar.getTime();
            }
            SubmissionSubmitRequest submissionSubmitRequest = new SubmissionSubmitRequest(submissionName, dueDate, sourceLanguage,
                    targetLanguages, uploadContentlist(requestDTO, gcExchange, parent, dueDate, submissionName));
            submissionSubmitRequest.setInstructions(pmNotes);

            SubmissionSubmitResponseData submissionSubmitResponseData = gcExchange.submitSubmission(submissionSubmitRequest);
            LOGGER.info("Submission Id: {}", submissionSubmitResponseData.getSubmissionId());
            requestDTO.setSubmissionId(submissionSubmitResponseData.getSubmissionId());

            return true;
        } catch (Exception ex) {
            LOGGER.error("Error while submitting Global link request -> ", ex);
            this.contentService.addTranslationRequestError(projectRootNode, this.sessionWrapper, ex.getMessage());
        }
        return false;
    }

    private List<String> uploadContentlist(GlobalLinkProjectRequestDTO requestDTO, GCExchange gcExchange, JCRNodeWrapper parent,
            Date dueDate, String submissionName) {
        List<UploadFileRequest> uploadFileRequests = requestDTO.getUploadFileRequests();

        List<String> contentIds = new ArrayList<>();
        uploadFileRequests.forEach(uploadFileRequest -> {
            try {
                String contentId = gcExchange.uploadContent(uploadFileRequest);
                contentIds.add(contentId);
                String id = StringUtils.substringBefore(StringUtils.substringAfterLast(uploadFileRequest.getFileName(), "___"), ".");
                if (!id.equals(parent.getIdentifier())) {
                    JCRNodeWrapper requestNode = this.sessionWrapper.getNodeByIdentifier(id).getNode(NODE_NAME_GLOBAL_LINK);
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(dueDate);
                    requestNode.setProperty(DUE_DATE, calendar);
                    requestNode.setProperty("name", submissionName);
                    this.contentService.addUploadTicketForRequest(requestNode, this.sessionWrapper, contentId);
                } else {
                    requestDTO.setUploadTicket(contentId);
                }
            } catch (Exception ex) {
                LOGGER.error("Error while uploading document -> ", ex);
                this.contentService.addTranslationRequestError(requestDTO.getNodeWrapper(), this.sessionWrapper, ex.getMessage());
            }
        });
        return contentIds;
    }

    /**
     * Process a request for submission.
     *
     * @param requestDTO
     * @param gcExchange
     * @param config
     */
    private void processRequestDTO(GlobalLinkProjectRequestDTO requestDTO, GCExchange gcExchange, GlobalLinkConfigurationDTO config) {
        try {
            processDocumentForProject(requestDTO, config);
            if (requestDTO.getNodeWrapper().hasProperty(GBL_INCLUDE_CHILD) && requestDTO.getNodeWrapper().getProperty(GBL_INCLUDE_CHILD)
                    .getBoolean()) {
                requestDTO.setChildIncluded(true);
                processChildPages(requestDTO, requestDTO.getNodeWrapper().getParent(), config);
            }
            if (!requestDTO.getUploadFileRequests().isEmpty() && this.submitGBLRequest(requestDTO, config, gcExchange)) {
                this.contentService.logProjectRequestInJcr(requestDTO, true, sessionWrapper);
            } else {
                this.contentService.updateRequestStatus(requestDTO.getNodeWrapper(), this.sessionWrapper, STATUS_NO_DOCUMENT);
            }
        } catch (RepositoryException e) {
            LOGGER.error("Error while processing project request DTO -> ", e);
        }
    }

    /**
     * Process child pages for a page node for submission documents.
     *
     * @param requestDTO
     * @param pageNode
     * @param config
     */
    private void processChildPages(GlobalLinkProjectRequestDTO requestDTO, JCRNodeWrapper pageNode, GlobalLinkConfigurationDTO config) {
        JCRContentUtils.getChildrenOfType(pageNode, NODE_TYPE_PAGE).forEach(child -> {
            try {
                JCRNodeWrapper requestNode = this.contentService.addGlobalLinkRequestNode(child, this.sessionWrapper, requestDTO);
                boolean documentForProject = this.documentService
                        .createDocumentForProject(requestDTO, child, requestNode, config.getComponentList(), sessionWrapper);
                if (documentForProject) {
                    String documentName = GlobalLinkUtil.getSourceDocumentPath(requestDTO, child);
                    Optional.ofNullable(prepareGlobalLinkDocument(documentName, config.getFileFormat()))
                            .ifPresent(uploadFileRequest -> requestDTO.getUploadFileRequests().add(uploadFileRequest));
                } else {
                    sendMailForEmptySubmission(requestDTO, requestNode, child);
                }
                if (!JCRContentUtils.getChildrenOfType(child, NODE_TYPE_PAGE).isEmpty()) {
                    processChildPages(requestDTO, child, config);
                }
            } catch (GlobalLinkServiceException se) {
                LOGGER.error("Exception while processing child page -> ", se);
            }
        });
    }

    private void sendMailForEmptySubmission(GlobalLinkProjectRequestDTO requestDTO, JCRNodeWrapper requestNode, JCRNodeWrapper page) {
        try {
            if (mailService.isEnabled()) {
                JCRNodeWrapper node = requestDTO.getNodeWrapper();
                JCRUserNode jcrUserNode = userManagerService.lookupUser(node.getCreationUser(), page.getSession());
                if (jcrUserNode.hasProperty("j:email")) {
                    MessageFormat messageFormat = new MessageFormat(
                            "Your translation submission {0} was empty for page " + page.getDisplayableName() + " and "
                                    + "so has not been submitted");
                    String name = node.getProperty("name").getString();
                    mailService.sendMessage(null, jcrUserNode.getProperty("j:email").getString(), null, null,
                            "Satus Update on your translation request " + name, messageFormat.format(new Object[] { name }));
                }
            }
            requestNode.remove();
            this.sessionWrapper.save();
        } catch (RepositoryException e) {
            LOGGER.error("Error while sending mail", e);
        }
    }

    private UploadFileRequest prepareGlobalLinkDocument(String filePath, String fileType) {
        try {
            return new UploadFileRequest(filePath, null, fileType);
        } catch (IOException ex) {
            LOGGER.error("Error while preparing global link content -> ", ex);
        }
        return null;
    }

    /**
     * Check configured interval for global link request process and
     * match last executed time.
     *
     * @param config
     * @return
     */
    private boolean checkInterval(GlobalLinkConfigurationDTO config) {
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
