package org.jahia.translation.globallink.service.impl;

import com.globallink.api.GLExchange;
import com.globallink.api.model.Document;
import com.globallink.api.model.Project;
import com.globallink.api.model.Submission;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.jahia.services.content.*;
import org.jahia.services.content.decorator.JCRSiteNode;
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
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import static org.jahia.translation.globallink.common.GlobalLinkConstants.*;
import static org.jahia.translation.globallink.common.SubmissionStatus.STATUS_CANCELLED;
import static org.jahia.translation.globallink.common.SubmissionStatus.STATUS_NO_DOCUMENT;

/**
 * Implementation for Global link translation project submission service
 *
 * @author Rakesh.Kumar, WebitUp.
 */
public class GlobalLinkSubmissionServiceImpl implements GlobalLinkSubmissionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalLinkSubmissionServiceImpl.class);

    private GlobalLinkQueryService gblQueryService;

    private GlobalLinkDocumentService documentService;

    private JCRSessionWrapper sessionWrapper;

    private SiteContentService contentService;

    /**
     * {@inheritDoc}
     */
    @Override
    public List<GlobalLinkConfigurationDTO> submitSiteProjects() {
        try {
            LOGGER.info("====  Initializing submission process  =====");
            this.sessionWrapper = JCRUtil.getRootSession(JCR_DEFAULT_WS);
            List<JCRSiteNode> sites = this.gblQueryService.getAllSites(this.sessionWrapper.getWorkspace().getQueryManager());
            List<GlobalLinkConfigurationDTO> configList = JCRUtil.getConfigurationList(sites);
            configList.forEach((config) -> {
                LOGGER.info("Site found with GBL Translation config - {} Sitename - {}", config.getUsername(),
                        config.getSiteNode().getName());
                this.processAllGBLTranslationProjects(config);
            });
            return configList;
        } catch (Exception ex) {
            LOGGER.error("Exception while starting submission process -> ", ex);
        }
        return null;
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

    /**
     * Check and process all the project available under a site.
     *
     * @param config
     * @return
     */
    private void processAllGBLTranslationProjects(GlobalLinkConfigurationDTO config) {
        GLExchange glExchange = GlobalLinkUtil.getGLExchangeClient(config);
        JCRNodeIteratorWrapper projects = this.gblQueryService.getGBLRequests(config.getSiteNode(),
                this.sessionWrapper.getWorkspace().getQueryManager());
        projects.forEach((project) -> {
            try {
                if (checkInterval(config)) {
                    if ((!project.hasProperty(GBL_SUBMISSION_STATE) || !project.hasProperty(GBL_PROJECT_REQUEST_ID)) &&
                            !project.hasProperty(GBL_PROJECT_ERROR)) {
                        LOGGER.info("processing project node: {}" + project.getPath());
                        GlobalLinkProjectRequestDTO projectRequestDTO = new GlobalLinkProjectRequestDTO();
                        projectRequestDTO.setFileFormat(StringUtils.substringAfter(config.getFileFormat(), "_").toLowerCase());
                        String sourceLanguage = project.getProperty(GBL_PROJECT_SOURCE_LANG).getString();
                        projectRequestDTO.setSourceLanguage(sourceLanguage);
                        projectRequestDTO.setDesLanguages(new String[]{project.getProperty(GBL_PROJECT_TARGET_LANG).getString()});
                        projectRequestDTO.setNodeWrapper(project);
                        projectRequestDTO.setRequestId(UUID.randomUUID().toString());
                        projectRequestDTO.setDocumentpath(config.getDocumentPath());
                        if (project.hasProperty(GBL_SKIP_TRANSLATED)) {
                            projectRequestDTO.setSkipTranslated(project.getProperty(GBL_SKIP_TRANSLATED).getBoolean());
                        } else {
                            projectRequestDTO.setSkipTranslated(false);
                        }
                        this.contentService.addRequestId(project, this.sessionWrapper, projectRequestDTO.getRequestId());
                        processRequestDTO(projectRequestDTO, glExchange, config);
                        config.getSiteNode().setProperty(GBL_PROPERTY_LAST_EXEC, Calendar.getInstance());
                        this.sessionWrapper.save();
                    }
                }
            } catch (RepositoryException | GlobalLinkServiceException ex) {
                LOGGER.error("Error while collecting project info for - " + project.getPath() + " Exception -> ", ex);
            }
        });
    }


    private void processDocumentForProject(GlobalLinkProjectRequestDTO requestDTO, List<String> componentList)
            throws RepositoryException {
        if (this.documentService.createDocumentForProject(requestDTO, requestDTO.getNodeWrapper().getParent(),
                requestDTO.getNodeWrapper(), componentList, sessionWrapper)) {
            Document document = prepareGlobalLinkDocument(GlobalLinkUtil.getSourceDocumentPath(requestDTO,
                    requestDTO.getNodeWrapper().getParent()));
            if (document != null) {
                requestDTO.getDocuments().add(document);
            }
        }
    }

    /**
     * Process Global link submission for a given project DTO
     *
     * @param requestDTO
     * @return
     */
    private boolean submitGBLRequest(GlobalLinkProjectRequestDTO requestDTO, GlobalLinkConfigurationDTO config,
                                     GLExchange glExchange) {
        String sourceLanguage = GlobalLinkUtil.getGLLocale(requestDTO.getSourceLanguage());
        String[] targetLanguages = requestDTO.getDesLanguages();
        JCRNodeWrapper projectRootNode = requestDTO.getNodeWrapper();
        try {
            Project project = glExchange.getProject(config.getProjectName());
            Submission submission = new Submission();
            String jahiaSourceLanguage = sourceLanguage.replace("-","_");
            JCRSiteNode siteNode = config.getSiteNode();
            if(!siteNode.getLanguages().contains(jahiaSourceLanguage)){
                jahiaSourceLanguage = StringUtils.substringBefore(jahiaSourceLanguage,"_");
                if(!siteNode.getLanguages().contains(jahiaSourceLanguage)){
                    throw new GlobalLinkServiceException("no source lang matching in site");
                }
            }
            JCRNodeWrapper parent = projectRootNode.getParent();
            String pageTitle;
            if(parent.hasNode("j:translation_" + jahiaSourceLanguage)) {
                pageTitle = parent.getNode("j:translation_" + jahiaSourceLanguage).getProperty("jcr:title").getString();
            } else {
                pageTitle = parent.getName();
            }
            if (projectRootNode.hasProperty("name")) {
                submission.setName(projectRootNode.getProperty("name").getString() + (requestDTO.isChildIncluded() ? " with " : " without ") + " sub pages");
            } else {
                submission.setName(config.getSubmissionPrefix() + " - from page " + pageTitle + (requestDTO.isChildIncluded() ? " with " : " without ") + " sub pages");
            }
            projectRootNode.setProperty("name", submission.getName());
            projectRootNode.getSession().save();
            submission.setProject(project);

            String pmNotes = "Translation for - " + pageTitle + (requestDTO.isChildIncluded() ? " with " : " without ") + " sub pages\n"
                    + "form the web site - " + siteNode.getServerName() + "( " + siteNode.getTitle() + " )";
            submission.setPmNotes(pmNotes);
            if(projectRootNode.hasProperty("dueDate")) {
                submission.setDueDate(projectRootNode.getProperty("dueDate").getDate().getTime());
            } else {
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.DAY_OF_YEAR,5);
                submission.setDueDate(calendar.getTime());
            }
            glExchange.initSubmission(submission);
            String parentIdentifier = parent.getIdentifier();
            requestDTO.getDocuments().forEach((document) -> {
                document.setFileformat(config.getFileFormat());
                document.setSourceLanguage(sourceLanguage);
                document.setTargetLanguages(targetLanguages);
                try {
                    String uploadTicket = glExchange.uploadTranslatable(document);
                    String id = StringUtils.substringBefore(StringUtils
                            .substringAfterLast(document.getName(), "___"), ".");
                    if (!id.equals(parentIdentifier)) {
                        JCRNodeWrapper requestNode = this.sessionWrapper.getNodeByIdentifier(id).getNode(NODE_NAME_GLOBAL_LINK);
                        Calendar calendar = Calendar.getInstance();
                        calendar.setTime(submission.getDueDate());
                        requestNode.setProperty("dueDate", calendar);
                        requestNode.setProperty("name", submission.getName());
                        this.contentService.addUploadTicketForRequest(requestNode, this.sessionWrapper, uploadTicket);
                    } else {
                        requestDTO.setUploadTicket(uploadTicket);
                    }
                } catch (Exception ex) {
                    LOGGER.error("Error while uploading document -> ", ex);
                    this.contentService.addTranslationRequestError(projectRootNode, this.sessionWrapper,
                            ex.getMessage());
                }
            });
            String[] submitTokens = glExchange.startSubmission();
            LOGGER.info("Submission Tickets: " + submitTokens.length);
            requestDTO.setSubmitTicket(submitTokens[0]);

            return true;
        } catch (Exception ex) {
            LOGGER.error("Error while submitting Global link request -> ", ex);
            this.contentService.addTranslationRequestError(projectRootNode, this.sessionWrapper,
                    ex.getMessage());
        }
        return false;
    }

    /**
     * Process a request for submission.
     *
     * @param requestDTO
     * @param glExchange
     * @param config
     */
    private void processRequestDTO(GlobalLinkProjectRequestDTO requestDTO, GLExchange glExchange,
                                   GlobalLinkConfigurationDTO config) {
        List<Document> documents = new ArrayList<>();
        try {
            requestDTO.setDocuments(documents);
            processDocumentForProject(requestDTO, config.getComponentList());
            if (requestDTO.getNodeWrapper().hasProperty(GBL_INCLUDE_CHILD) &&
                    requestDTO.getNodeWrapper().getProperty(GBL_INCLUDE_CHILD).getBoolean()) {
                requestDTO.setChildIncluded(true);
                processChildPages(requestDTO, requestDTO.getNodeWrapper().getParent(), config);
            }
            if (!requestDTO.getDocuments().isEmpty() && this.submitGBLRequest(requestDTO, config, glExchange)) {
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
    private void processChildPages(GlobalLinkProjectRequestDTO requestDTO, JCRNodeWrapper pageNode,
                                   GlobalLinkConfigurationDTO config) {
        List<JCRNodeWrapper> pages = JCRContentUtils.getChildrenOfType(pageNode, NODE_TYPE_PAGE);
        pages.forEach(child -> {
            try {
                JCRNodeWrapper requestNode = this.contentService.addGlobalLinkRequestNode(child, this.sessionWrapper, requestDTO);
                boolean documentForProject = this.documentService.createDocumentForProject(requestDTO, child, requestNode,
                        config.getComponentList(), sessionWrapper);
                if (documentForProject) {
                    Document document = prepareGlobalLinkDocument(GlobalLinkUtil.getSourceDocumentPath(requestDTO, child));
                    if (document != null) {
                        requestDTO.getDocuments().add(document);
                    }
                } else {
                    try {
                        requestNode.remove();
                        this.sessionWrapper.save();
                    } catch (RepositoryException e) {
                        e.printStackTrace();
                    }
                }
                if (JCRContentUtils.getChildrenOfType(child, NODE_TYPE_PAGE).size() > 0) {
                    processChildPages(requestDTO, child, config);
                }
            } catch (GlobalLinkServiceException se) {
                LOGGER.error("Exception while processing child page -> ", se);
            }
        });
    }

    /**
     * Prepare {@link Document} for a page node.
     *
     * @param filePath
     * @return
     */
    private Document prepareGlobalLinkDocument(String filePath) {
        try {
            File srcFile = new File(filePath);
            Document document = new Document();
            document.setData(IOUtils.toString(new FileReader(srcFile)));
            document.setName(srcFile.getName());
            return document;
        } catch (IOException ex) {
            LOGGER.error("Error while preparing global link source document -> ", ex);
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
            if (config.getSiteNode().hasProperty(GBL_PROPERTY_LAST_EXEC) &&
                    config.getSiteNode().hasProperty(GBL_PROPERTY_INTERVAL)) {
                Calendar lastExecuted = config.getSiteNode().getProperty(GBL_PROPERTY_LAST_EXEC).getDate();
                lastExecuted.add(Calendar.SECOND, Integer.valueOf(
                        (String.valueOf(config.getSiteNode().getProperty(GBL_PROPERTY_INTERVAL).getLong()))
                ));
                if (lastExecuted.compareTo(Calendar.getInstance()) == 1) {
                    return false;
                }
                return true;
            }
            return true;
        } catch (RepositoryException re) {
            LOGGER.error("Error while checking submission interval -> ", re);
        }
        return false;
    }
}
