package org.jahia.translation.globallink.service.impl;

import static org.jahia.translation.globallink.common.GlobalLinkConstants.DOCUMENT_PATH;
import static org.jahia.translation.globallink.common.GlobalLinkConstants.GBL_PROJECT_REQUEST_ID;
import static org.jahia.translation.globallink.common.GlobalLinkConstants.GBL_PROJECT_SUB_TICKET;
import static org.jahia.translation.globallink.common.GlobalLinkConstants.JCR_DEFAULT_WS;
import static org.jahia.translation.globallink.common.GlobalLinkConstants.TRANSLATED_PATH;
import static org.jahia.translation.globallink.common.SubmissionStatus.STATUS_DELIVERED;
import static org.jahia.translation.globallink.common.SubmissionStatus.STATUS_PROCESSED;
import static org.jahia.translation.globallink.common.SubmissionStatus.STATUS_RETRIEVED;

import java.io.File;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.jahia.services.content.JCRNodeIteratorWrapper;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.translation.globallink.dto.GlobalLinkConfigurationDTO;
import org.jahia.translation.globallink.service.api.GlobalLinkQueryService;
import org.jahia.translation.globallink.service.api.GlobalLinkRetrieveDocumentService;
import org.jahia.translation.globallink.service.api.SiteContentService;
import org.jahia.translation.globallink.util.GlobalLinkUtil;
import org.jahia.translation.globallink.util.IOUtil;
import org.jahia.translation.globallink.util.JCRUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.globallink.api.GLExchange;
import com.globallink.api.model.Target;

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

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<GlobalLinkConfigurationDTO> retrieveCompletedProjects(List<GlobalLinkConfigurationDTO> configList) {
		try {
			LOGGER.info("====  Initializing Retrieve process  =====");
			this.sessionWrapper = JCRUtil.getRootSession(JCR_DEFAULT_WS);
			configList.forEach(config -> {
				this.retrieveDocuments(config);
			});
		} catch (Exception ex) {
			LOGGER.error("Exception while starting document retrieve process -> ", ex);
		}
		return configList;
	}

	/**
	 * Retrieve completed documents from Global link PD
	 * @param config
	 */
	private void retrieveDocuments(GlobalLinkConfigurationDTO config) {
		JCRNodeIteratorWrapper submittedRequests = this.queryService.getSubmittedRequests(config.getSiteNode().getPath(),
			  this.sessionWrapper.getWorkspace().getQueryManager());
		GLExchange glExchange = GlobalLinkUtil.getGLExchangeClient(config);
		submittedRequests.forEach((request) -> {
			processRequestForRetrieval(request, glExchange, config);
		});
	}

	/**
	 * Process a request for retrieval of all the completed targets and
	 * translated documents.
	 *
	 * @param requsetNode
	 * @param glExchange
	 * @param config
     */
	private void processRequestForRetrieval(JCRNodeWrapper requsetNode, GLExchange glExchange,
											GlobalLinkConfigurationDTO config) {
		Target[] targets = glExchange.getCompletedTargets(requsetNode.getPropertyAsString(GBL_PROJECT_SUB_TICKET), 100);
		List<Target> completedTargets = new ArrayList<>();
		if (targets.length > 0) {
			Arrays.asList(targets).forEach(target -> {
				boolean status  = processTarget(target, glExchange, config);
				if (status) {
					completedTargets.add(target);
				}
			});
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
			if (glExchange.getSubmissionStatus(requestNode.getProperty(GBL_PROJECT_SUB_TICKET).getString())
					.equals(STATUS_DELIVERED) || glExchange.getSubmissionStatus(requestNode.
					getProperty(GBL_PROJECT_SUB_TICKET).getString()).equals(STATUS_PROCESSED)) {
				this.contentService.updateRequestStatus(requestNode, this.sessionWrapper, STATUS_RETRIEVED);
				String targetStatus = target.getTargetLocale() + "_" + target.getTicket() + "_" + target.getWordCount().getTotal();
				this.contentService.addTargetTicketsInStatus(requestNode, targetStatus, this.sessionWrapper);
			}
			return true;
		} catch (Exception ex) {
			LOGGER.error("Error retreiving translated document - ", ex);
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
