package org.jahia.translation.globallink.service.impl;

import org.apache.commons.lang.StringUtils;
import org.gs4tr.gcc.restclient.GCExchange;
import org.gs4tr.gcc.restclient.model.GCTask;
import org.gs4tr.gcc.restclient.model.Status;
import org.gs4tr.gcc.restclient.model.SubmissionWordCountData;
import org.gs4tr.gcc.restclient.model.TaskStatus;
import org.gs4tr.gcc.restclient.operation.Tasks.TasksResponseData;
import org.gs4tr.gcc.restclient.request.TaskListRequest;
import org.jahia.services.content.JCRNodeIteratorWrapper;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.mail.MailService;
import org.jahia.translation.globallink.dto.GlobalLinkConfigurationDTO;
import org.jahia.translation.globallink.service.api.GlobalLinkMailService;
import org.jahia.translation.globallink.service.api.GlobalLinkQueryService;
import org.jahia.translation.globallink.service.api.GlobalLinkRetrieveDocumentService;
import org.jahia.translation.globallink.service.api.SiteContentService;
import org.jahia.translation.globallink.util.GlobalLinkUtil;
import org.jahia.translation.globallink.util.IOUtil;
import org.jahia.translation.globallink.util.JCRUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.io.File;
import java.nio.file.FileSystems;
import java.util.Collections;
import java.util.List;

import static org.jahia.translation.globallink.common.GlobalLinkConstants.*;
import static org.jahia.translation.globallink.common.SubmissionStatus.*;

/**
 * Document service to retrieve all translated documents from Global Link.
 *
 * @author Rakesh.Kumar, WebitUp.
 * @author Prince.Arora, WebItUp.
 */
@Component(service = GlobalLinkRetrieveDocumentService.class, immediate = true)
public class GlobalLinkRetrieveDocumentServiceImpl implements GlobalLinkRetrieveDocumentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalLinkRetrieveDocumentServiceImpl.class);

    private JCRSessionWrapper sessionWrapper;

    private SiteContentService contentService;

    private GlobalLinkQueryService queryService;

    private MailService mailService;
    private GlobalLinkMailService globalLinkMailService;

    @Reference
    public void setMailService(MailService mailService) {
        this.mailService = mailService;
    }

    @Reference
    public void setGlobalLinkMailService(GlobalLinkMailService globalLinkMailService) {
        this.globalLinkMailService = globalLinkMailService;
    }
    /**
     * {@inheritDoc}
     */
    @Override public void retrieveCompletedProjects(List<GlobalLinkConfigurationDTO> configList) {
        try {
            LOGGER.info("====  Initializing Retrieve process  =====");
            this.sessionWrapper = JCRUtil.getRootSession(JCR_DEFAULT_WS);
            for (GlobalLinkConfigurationDTO config : configList) {
                this.retrieveDocuments(config);
            }
        } catch (Exception ex) {
            LOGGER.error("Exception while starting document retrieve process -> ", ex);
        }
    }

    /**
     * Retrieve all documents and process them according to their status
     *
     * @param config config for the connection
     */
    private void retrieveDocuments(GlobalLinkConfigurationDTO config) {
        JCRNodeIteratorWrapper submittedRequests = this.queryService
                .getSubmittedRequests(config.getSiteNode().getPath(), this.sessionWrapper.getWorkspace().getQueryManager());

        submittedRequests.forEach(request -> {
            try {
                processExistingRequests(request, GlobalLinkUtil.getGlobalLinkClient(config), config);
            } catch (RepositoryException e) {
                LOGGER.error("Error while retrieving tasks: ", e);
            }
        });
    }

    private void processExistingRequests(JCRNodeWrapper requestNode, GCExchange gcExchange, GlobalLinkConfigurationDTO config)
            throws RepositoryException {
        processCompletedTranslations(requestNode, gcExchange, config);
        processCancelledTranslations(requestNode, gcExchange);
        processForOtherStatus(requestNode, gcExchange);
    }

    private void processCompletedTranslations(JCRNodeWrapper requestNode, GCExchange gcExchange, GlobalLinkConfigurationDTO config)
            throws RepositoryException {
        TasksResponseData tasksResponseData = getTasksListByStatus(requestNode, gcExchange, TaskStatus.Completed);
        tasksResponseData.getTasks().forEach(task -> processTask(task, gcExchange, config));
    }

    private void processCancelledTranslations(JCRNodeWrapper requestNode, GCExchange gcExchange) throws RepositoryException {
        TasksResponseData tasksResponseData = getTasksListByStatus(requestNode, gcExchange, TaskStatus.Cancelled);
        tasksResponseData.getTasks().forEach(task -> {
            try {
                if (requestNode.getProperty(GBL_PROJECT_UPLOAD_TICKET).getString().equals(task.getContentId())) {
                    this.contentService.updateRequestStatus(requestNode, this.sessionWrapper, STATUS_CANCELLED);
                }
            } catch (RepositoryException e) {
                LOGGER.error("Error cancelling submission - ", e);
            }
        });
    }

    private TasksResponseData getTasksListByStatus(JCRNodeWrapper requestNode, GCExchange gcExchange, TaskStatus completed)
            throws RepositoryException {
        TaskListRequest taskListRequest = new TaskListRequest();
        taskListRequest.setSubmissionId(requestNode.getProperty(GBL_PROJECT_SUB_TICKET).getLong());
        List<TaskStatus> tasks = Collections.singletonList(completed);
        taskListRequest.setTaskStatuses(tasks.toArray(new TaskStatus[0]));
        taskListRequest.setPageSize(100L);
        return gcExchange.getTasksList(taskListRequest);
    }

    private void processForOtherStatus(JCRNodeWrapper requestNode, GCExchange gcExchange) {
        try {
            Status submissionStatus = gcExchange.getSubmissionStatus(requestNode.getProperty(GBL_PROJECT_SUB_TICKET).getLong());
            if (submissionStatus == null) {
                this.contentService.updateRequestStatus(requestNode, this.sessionWrapper, STATUS_DELETED);
            } else if (submissionStatus.getStatusName().equals(STATUS_PRE_PROCESS)) {
                this.contentService.updateRequestStatus(requestNode, this.sessionWrapper, STATUS_SUBMITTED);
            }
        } catch (Exception e) {
            LOGGER.error("Error retrieving translated document - ", e);
        }
    }

    private void processTask(GCTask task, GCExchange gcExchange, GlobalLinkConfigurationDTO config) {
        JCRNodeWrapper requestNode = (JCRNodeWrapper) this.queryService.
                getSubmissionNodeByContentId(task.getContentId(), this.sessionWrapper.getWorkspace().getQueryManager()).next();
        try {
            LOGGER.info("Submission: {} - Job: {} - Task: {}", task.getSubmissionId(), task.getJobId(), task.getTaskId());
            this.contentService.unLockNode((JCRNodeWrapper) requestNode.getProperty(GBL_PROJECT_TARGET_NODE).getNode(), this.sessionWrapper);
            String docPath;
            if (config.getDocumentPath() != null && !config.getDocumentPath().equals("")) {
                docPath =
                        config.getDocumentPath() + File.separator + requestNode.getPropertyAsString(GBL_PROJECT_REQUEST_ID) + File.separator
                                + TRANSLATED_PATH;
            } else {
                docPath = DOCUMENT_PATH + File.separator + requestNode.getPropertyAsString(GBL_PROJECT_REQUEST_ID) + File.separator
                        + TRANSLATED_PATH;
            }
            IOUtil.createDirectories(FileSystems.getDefault().getPath(docPath));
            String fileName = task.getTargetLocale().getLocale() + "_" + StringUtils.substringAfterLast(task.getName(), "_");
            String filePath = docPath + File.separator + fileName;
            if (IOUtil.createFile(gcExchange.downloadTask(task.getTaskId()), filePath)) {
                gcExchange.confirmTask(task.getTaskId());
            }
            this.contentService.updateRequestStatus(requestNode, this.sessionWrapper, STATUS_RETRIEVED);

            List<SubmissionWordCountData> submissionWordCountDataList = gcExchange.getSubmissionWordCount(task.getSubmissionId());
            submissionWordCountDataList.stream()
                    .filter(submissionWordCountData -> submissionWordCountData.getTargetLocale().equals(task.getTargetLocale()))
                    .flatMap(submissionWordCountData -> submissionWordCountData.getWordcountSummary().stream())
                    .filter(wordCountSummary -> wordCountSummary.getKey().equals("total")).findFirst().ifPresent(wordCountSummary -> {
                String targetStatus = task.getTargetLocale() + "_" + task.getTaskId() + "_" + wordCountSummary.getCount();
                this.contentService.addTargetTicketsInStatus(requestNode, targetStatus, this.sessionWrapper);
            });
        } catch (Exception ex) {
            LOGGER.error("Error retrieving translated document - ", ex);
            this.contentService.updateRequestStatus(requestNode, this.sessionWrapper, STATUS_CONTENT_ERROR);
            if (mailService.isEnabled()) {
                globalLinkMailService.sendNotificationMail(requestNode, STATUS_CONTENT_ERROR);
            }
        }
    }

    @Reference
    public void setContentService(SiteContentService contentService) {
        this.contentService = contentService;
    }

    @Reference
    public void setQueryService(GlobalLinkQueryService queryService) {
        this.queryService = queryService;
    }
}
