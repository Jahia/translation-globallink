package org.jahia.translation.globallink.service.impl;

import static org.jahia.translation.globallink.common.GlobalLinkConstants.*;
import static org.jahia.translation.globallink.common.SubmissionStatus.STATUS_CONTENT_ERROR;
import static org.jahia.translation.globallink.common.SubmissionStatus.STATUS_TRANSLATED;

import java.io.File;
import java.util.List;

import org.jahia.services.content.JCRNodeIteratorWrapper;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRValueWrapper;
import org.jahia.translation.globallink.dto.GlobalLinkConfigurationDTO;
import org.jahia.translation.globallink.exception.GlobalLinkServiceException;
import org.jahia.translation.globallink.service.api.GlobalLinkDocumentService;
import org.jahia.translation.globallink.service.api.GlobalLinkQueryService;
import org.jahia.translation.globallink.service.api.GlobalLinkTranslatedContentProcessService;
import org.jahia.translation.globallink.service.api.SiteContentService;
import org.jahia.translation.globallink.util.GlobalLinkUtil;
import org.jahia.translation.globallink.util.IOUtil;
import org.jahia.translation.globallink.util.JCRUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.NodeList;

import javax.jcr.RepositoryException;

/**
 * Implementation for Global Link translated content process service
 * Check ALL translated documents retrieved from Global Link Server and
 * persist translated content nodes in JCR.
 *
 * @author Prince.Arora, WebItUp.
 * @author Aashish.Kocchar, WebitUp.
 */
public class GlobalLinkTranslatedContentProcessServiceImpl implements GlobalLinkTranslatedContentProcessService {

	private static final Logger LOGGER = LoggerFactory.getLogger(GlobalLinkTranslatedContentProcessServiceImpl.class);
	
	private GlobalLinkQueryService gblQueryService;
	
	private GlobalLinkDocumentService documentService;
	
	private JCRSessionWrapper sessionWrapper;
	
	private SiteContentService contentService; 
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void processTranslatedContent(List<GlobalLinkConfigurationDTO> configList) {
		this.sessionWrapper = JCRUtil.getRootSession(JCR_DEFAULT_WS);
		configList.forEach(config -> {
			processRetrievedRequestsForConfig(config);
		});
	}

	/**
	 * Process all requests for a site.
	 * @param config
     */
	private void processRetrievedRequestsForConfig(GlobalLinkConfigurationDTO config) {
		JCRNodeIteratorWrapper retrievedRequests = this.gblQueryService.getRetrievedRequests(config.getSiteNode().getPath(),
				this.sessionWrapper.getWorkspace().getQueryManager());
		retrievedRequests.forEach(request -> {
			processRequest(request, config);
		});
	}

	/**
	 * Process a request to save translated content back in jcr.
	 *
	 * @param requestNode
	 * @param config
     */
	private void processRequest(JCRNodeWrapper requestNode, GlobalLinkConfigurationDTO config) {
		String requestId = requestNode.getPropertyAsString(GBL_PROJECT_REQUEST_ID);
		try {
			JCRValueWrapper[] values = requestNode.getProperty(GBL_PROJECT_TARGET_LANG).getValues();
			for (int index = 0; index < values.length ; index++) {
				String language = this.sessionWrapper.getNodeByUUID(values[index].getString())
						.getNode("j:translation_en").getPropertyAsString("jcr:title");
				String fileName = "";
				if (config.getDocumentPath() != null && !config.getDocumentPath().equals("")) {
					fileName = config.getDocumentPath() + File.separator + requestId + File.separator +TRANSLATED_PATH
							+ File.separator + GlobalLinkUtil.getFullLocale(language) + "_"
							+ requestNode.getParent().getIdentifier() + FILE_EXT_XML;
				} else {
					fileName = DOCUMENT_PATH + File.separator + requestId + File.separator +TRANSLATED_PATH
							+ File.separator + GlobalLinkUtil.getFullLocale(language) + "_"
							+ requestNode.getParent().getIdentifier() + FILE_EXT_XML;
				}
				File file = IOUtil.getFile(fileName);
				if (file != null) {
					processTranslatedDocument(file, requestNode);
				}
			}
		} catch (RepositoryException re) {
			LOGGER.error("Error while processing request node: {} Exception {}", requestNode, re);
		}
	}

	/**
	 * Handle translated document and check all translation content
	 * for respective bigtext nodes.
	 */
	private void processTranslatedDocument(File file, JCRNodeWrapper requestNode) {
		try {
			JCRNodeWrapper pageNode = requestNode.getParent();
			this.contentService.lockNode(pageNode, this.sessionWrapper);
			String locale = GlobalLinkUtil.getTargetLocale(file.getName());
			NodeList contentNodes = this.documentService.getTranslatedContentList(file);
			String sourceLocale = this.sessionWrapper.getNodeByUUID(requestNode.getProperty(GBL_PROJECT_SOURCE_LANG)
					.getString()).getNode("j:translation_en").getPropertyAsString("jcr:title");
			try {
				this.contentService.checkInTranslatedContent(contentNodes, this.sessionWrapper, locale, sourceLocale);
				this.contentService.updateRequestStatus(requestNode, this.sessionWrapper, STATUS_TRANSLATED);
			} catch (GlobalLinkServiceException ex) {
				this.contentService.updateRequestStatus(requestNode, this.sessionWrapper, STATUS_CONTENT_ERROR);
				LOGGER.error("Error while checking in content: ", ex);
			}
			this.contentService.unLockNode(pageNode, this.sessionWrapper);
		} catch (Exception ex) {
			LOGGER.error("Error while processing document: ", ex);
		}
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
	
}
