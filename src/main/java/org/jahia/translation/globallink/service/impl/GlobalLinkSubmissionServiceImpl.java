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
import org.jahia.services.mail.MailService;
import org.jahia.services.usermanager.JahiaUserManagerService;
import org.jahia.translation.globallink.dto.GlobalLinkConfigurationDTO;
import org.jahia.translation.globallink.dto.GlobalLinkProjectRequestDTO;
import org.jahia.translation.globallink.exception.GlobalLinkServiceException;
import org.jahia.translation.globallink.service.api.GlobalLinkDocumentService;
import org.jahia.translation.globallink.service.api.GlobalLinkMailService;
import org.jahia.translation.globallink.service.api.GlobalLinkQueryService;
import org.jahia.translation.globallink.service.api.GlobalLinkSubmissionService;
import org.jahia.translation.globallink.service.api.SiteContentService;
import org.jahia.translation.globallink.util.GlobalLinkUtil;
import org.jahia.translation.globallink.util.JCRUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Value;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;

import static org.jahia.translation.globallink.common.GlobalLinkConstants.*;
import static org.jahia.translation.globallink.common.SubmissionStatus.STATUS_NO_DOCUMENT;
import static org.jahia.translation.globallink.common.SubmissionStatus.STATUS_SUBMITTED;

/**
 * Implementation for Global link translation project submission service
 *
 * @author Rakesh.Kumar, WebitUp.
 */
@Component(service = GlobalLinkSubmissionService.class, immediate = true)
public class GlobalLinkSubmissionServiceImpl implements GlobalLinkSubmissionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalLinkSubmissionServiceImpl.class);

    private static final String WITHOUT = " without ";
    public static final String SHORT = "short";
    public static final String J_TRANSLATION_PREFIX = "j:translation_";
    public static final String J_EMAIL = "j:email";

    private GlobalLinkQueryService gblQueryService;

    private GlobalLinkDocumentService documentService;

    private JCRSessionWrapper sessionWrapper;

    private SiteContentService contentService;
    private MailService mailService;
    private JahiaUserManagerService userManagerService;
    private GlobalLinkMailService globalLinkMailService;

    @Reference
    public void setMailService(MailService mailService) {
        this.mailService = mailService;
    }

    @Reference
    public void setGblQueryService(GlobalLinkQueryService gblQueryService) {
        this.gblQueryService = gblQueryService;
    }

    @Reference
    public void setDocumentService(GlobalLinkDocumentService documentService) {
        this.documentService = documentService;
    }

    @Reference
    public void setContentService(SiteContentService contentService) {
        this.contentService = contentService;
    }

    @Reference
    public void setUserManagerService(JahiaUserManagerService userManagerService) {
        this.userManagerService = userManagerService;
    }

    @Reference
    public void setGlobalLinkMailService(GlobalLinkMailService globalLinkMailService) {
        this.globalLinkMailService = globalLinkMailService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void submitSiteProjects(List<GlobalLinkConfigurationDTO> configList) {
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
                // Remove request that match removed nodes
                if (project.getProperty(GBL_PROJECT_TARGET_NODE).getValue().getNode() == null) {
                    if (project.hasProperty(GBL_PROJECT_REQUEST_ID)) {
                        String instructions = project.hasProperty("instructions") ? project.getPropertyAsString("instructions") : "No translation request sent";
                        String submissionId = project.hasProperty(GBL_PROJECT_REQUEST_ID) ? project.getPropertyAsString(GBL_PROJECT_REQUEST_ID) : "No";
                        String state = project.getPropertyAsString(GBL_SUBMISSION_STATE);
                        LOGGER.info("Remove translation request {} [{}] with ID {} and status {} as the target has been removed", project.getName(), instructions, submissionId, state);
                    } else {
                        LOGGER.info("Remove translation request (No submission was send to translations.com)");
                    }
                    project.remove();
                    sessionWrapper.save();
                } else if (checkInterval(config) && (!project.hasProperty(GBL_SUBMISSION_STATE) || !project.hasProperty(GBL_PROJECT_REQUEST_ID))
                        && !project.hasProperty(GBL_PROJECT_ERROR)) {
                    LOGGER.info("processing project node: {}", project.getPath());
                    GlobalLinkProjectRequestDTO projectRequestDTO = buildProjectRequestDTO(project, config);

                    this.contentService.addRequestId(project, this.sessionWrapper, projectRequestDTO.getRequestId());
                    processRequestDTO(projectRequestDTO, gcExchange, config);
                    config.getSiteNode().setProperty(GBL_PROPERTY_LAST_EXEC, Calendar.getInstance());
                    this.sessionWrapper.save();
                    if (mailService.isEnabled()) {
                        globalLinkMailService.sendNotificationMail(project, STATUS_SUBMITTED);
                    }
                }
            } catch (RepositoryException | GlobalLinkServiceException ex) {
                LOGGER.error("Error while collecting project info for - " + project.getPath() + " Exception -> ", ex);
            }
    }

    private GlobalLinkProjectRequestDTO buildProjectRequestDTO(JCRNodeWrapper project, GlobalLinkConfigurationDTO config)
            throws RepositoryException {
        GlobalLinkProjectRequestDTO projectRequestDTO = new GlobalLinkProjectRequestDTO();
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

    private Map<String, JCRNodeWrapper> processDocumentForProject(GlobalLinkProjectRequestDTO requestDTO, GlobalLinkConfigurationDTO config)
            throws RepositoryException {
        if (requestDTO.getNodeWrapper().hasProperty(GBL_PROJECT_TARGET_NODE)) {
            JCRNodeWrapper node = (JCRNodeWrapper) requestDTO.getNodeWrapper().getProperty(GBL_PROJECT_TARGET_NODE).getNode();

            if (this.documentService
                    .createDocumentForProject(requestDTO, node, requestDTO.getNodeWrapper(), config.getComponentList(), sessionWrapper)) {

                String documentName = GlobalLinkUtil.getSourceDocumentPath(requestDTO, node);

                Optional.ofNullable(prepareGlobalLinkDocument(documentName, config.getFileFormat()))
                        .ifPresent(uploadFileRequest -> requestDTO.getUploadFileRequests().add(uploadFileRequest));
            }
            return Collections.singletonMap(node.getIdentifier(), requestDTO.getNodeWrapper());
        } else {
            LOGGER.error("Unable to read " + GBL_PROJECT_TARGET_NODE + " property on node " + requestDTO.getNodeWrapper().getPath());
            return Collections.emptyMap();
        }
    }

    /**
     * Process Global link submission for a given project DTO
     *
     * @param requestDTO
     * @return
     */
    private boolean submitGBLRequest(GlobalLinkProjectRequestDTO requestDTO, GlobalLinkConfigurationDTO config, GCExchange gcExchange, Map<String, JCRNodeWrapper> contentToRequestMap) {
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
            JCRNodeWrapper parent = (JCRNodeWrapper) requestDTO.getNodeWrapper().getProperty(GBL_PROJECT_TARGET_NODE).getNode();
            String pageTitle;
            if (parent.hasNode(J_TRANSLATION_PREFIX + jahiaSourceLanguage)) {
                if (parent.getNode(J_TRANSLATION_PREFIX + jahiaSourceLanguage).hasProperty("jcr:title")) {
                    pageTitle = parent.getNode(J_TRANSLATION_PREFIX + jahiaSourceLanguage).getPropertyAsString("jcr:title");
                } else {
                    pageTitle = parent.getNode(J_TRANSLATION_PREFIX + jahiaSourceLanguage).getPropertyAsString("text");
                }
            } else {
                pageTitle = parent.getName();
            }

            String suffixValue = parent.isNodeType("jnt:page") && requestDTO.isChildIncluded()?  " with sub pages" : "" ;
            if (projectRootNode.hasProperty("name")) {
                submissionName = projectRootNode.getProperty("name").getString() + suffixValue;
            } else {
                submissionName = config.getSubmissionPrefix() + " - from page " + pageTitle + suffixValue;
            }
            projectRootNode.setProperty("name", submissionName);

            String pmNotes = "Translation for " + pageTitle + " from " + siteNode.getServerName() + " (" + siteNode.getTitle() + ")";

            projectRootNode.setProperty("instructions", pmNotes);

            projectRootNode.getSession().save();

            if (projectRootNode.hasProperty(DUE_DATE)) {
                dueDate = projectRootNode.getProperty(DUE_DATE).getDate().getTime();
            } else {
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.DAY_OF_YEAR, 5);
                dueDate = calendar.getTime();
            }
            SubmissionSubmitRequest submissionSubmitRequest = new SubmissionSubmitRequest(submissionName, dueDate, sourceLanguage,
                    targetLanguages, uploadContentlist(requestDTO, gcExchange, parent, dueDate, submissionName,  contentToRequestMap));
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
            Date dueDate, String submissionName, Map<String, JCRNodeWrapper> contentToRequestMap) {
        List<UploadFileRequest> uploadFileRequests = requestDTO.getUploadFileRequests();

        List<String> contentIds = new ArrayList<>();
        uploadFileRequests.forEach(uploadFileRequest -> {
            try {
                String contentId = gcExchange.uploadContent(uploadFileRequest);
                contentIds.add(contentId);
                String id = StringUtils.substringBefore(StringUtils.substringAfterLast(uploadFileRequest.getFileName(), "___"), ".");
                if (!id.equals(parent.getIdentifier())) {
                    JCRNodeWrapper requestNode = contentToRequestMap.get(id);
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
            Map<String, JCRNodeWrapper> contentToRequestMap = new HashMap<>(processDocumentForProject(requestDTO, config));
            if (contentToRequestMap.isEmpty()) {
                LOGGER.error("Unable to process request node path {}", requestDTO.getNodeWrapper().getPath());
                return;
            }
            if (requestDTO.getNodeWrapper().hasProperty(GBL_INCLUDE_CHILD) && requestDTO.getNodeWrapper().getProperty(GBL_INCLUDE_CHILD)
                    .getBoolean()) {
                requestDTO.setChildIncluded(true);
                contentToRequestMap.putAll(processChildPages(requestDTO, (JCRNodeWrapper) requestDTO.getNodeWrapper().getProperty(GBL_PROJECT_TARGET_NODE).getNode(),
                        config));
            }
            if (!requestDTO.getUploadFileRequests().isEmpty() && this.submitGBLRequest(requestDTO, config, gcExchange, contentToRequestMap)) {
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
    private Map<String, JCRNodeWrapper> processChildPages(GlobalLinkProjectRequestDTO requestDTO, JCRNodeWrapper pageNode, GlobalLinkConfigurationDTO config) {
        Map<String, JCRNodeWrapper> contentToRequestMap = new HashMap<>();
        JCRContentUtils.getChildrenOfType(pageNode, NODE_TYPE_PAGE).forEach(child -> {
            try {
                JCRNodeWrapper requestNode = this.contentService.addGlobalLinkRequestOnChildNode(child, this.sessionWrapper, requestDTO);
                contentToRequestMap.put(child.getIdentifier(), requestNode);
                boolean documentForProject = this.documentService
                        .createDocumentForProject(requestDTO, child, requestNode, config.getComponentList(), sessionWrapper);
                if (documentForProject) {
                    String documentName = GlobalLinkUtil.getSourceDocumentPath(requestDTO, child);
                    Optional.ofNullable(prepareGlobalLinkDocument(documentName, config.getFileFormat()))
                            .ifPresent(uploadFileRequest -> requestDTO.getUploadFileRequests().add(uploadFileRequest));
                } else {
                    sendMailForEmptySubmission(requestDTO, requestNode, child);
                    requestNode.remove();
                    this.sessionWrapper.save();
                }
                if (!JCRContentUtils.getChildrenOfType(child, NODE_TYPE_PAGE).isEmpty()) {
                    contentToRequestMap.putAll(processChildPages(requestDTO, child, config));
                }
            } catch (GlobalLinkServiceException | RepositoryException se) {
                LOGGER.error("Exception while processing child page -> ", se);
            }
        });
        return contentToRequestMap;
    }

    private void sendMailForEmptySubmission(GlobalLinkProjectRequestDTO requestDTO, JCRNodeWrapper requestNode, JCRNodeWrapper page) {
        try {
            if (mailService.isEnabled()) {
                JCRNodeWrapper node = requestDTO.getNodeWrapper();
                JCRUserNode jcrUserNode = userManagerService.lookupUser(node.getCreationUser(), page.getSession());
                if (jcrUserNode.hasProperty(J_EMAIL)) {
                    MessageFormat messageFormat = new MessageFormat(
                            "Your translation submission {0} was empty for page " + page.getDisplayableName() + " and "
                                    + "so has not been submitted");
                    String name = node.getProperty("name").getString();
                    mailService.sendMessage(null, jcrUserNode.getProperty(J_EMAIL).getString(), null, null,
                            "Satus Update on your translation request " + name, messageFormat.format(new Object[] { name }));
                }
            }
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
