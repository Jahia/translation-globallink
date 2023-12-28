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
import org.jahia.services.content.JCRNodeIteratorWrapper;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.mail.MailService;
import org.jahia.translation.globallink.dto.GlobalLinkConfigurationDTO;
import org.jahia.translation.globallink.exception.GlobalLinkServiceException;
import org.jahia.translation.globallink.service.api.GlobalLinkDocumentService;
import org.jahia.translation.globallink.service.api.GlobalLinkMailService;
import org.jahia.translation.globallink.service.api.GlobalLinkQueryService;
import org.jahia.translation.globallink.service.api.GlobalLinkTranslatedContentProcessService;
import org.jahia.translation.globallink.service.api.SiteContentService;
import org.jahia.translation.globallink.util.GlobalLinkUtil;
import org.jahia.translation.globallink.util.IOUtil;
import org.jahia.translation.globallink.util.JCRUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.NodeList;

import javax.jcr.RepositoryException;
import javax.jcr.Value;
import java.io.File;
import java.util.List;

import static org.jahia.translation.globallink.common.GlobalLinkConstants.DOCUMENT_PATH;
import static org.jahia.translation.globallink.common.GlobalLinkConstants.FILE_EXT_XML;
import static org.jahia.translation.globallink.common.GlobalLinkConstants.GBL_PROJECT_REQUEST_ID;
import static org.jahia.translation.globallink.common.GlobalLinkConstants.GBL_PROJECT_SOURCE_LANG;
import static org.jahia.translation.globallink.common.GlobalLinkConstants.GBL_PROJECT_TARGET_LANG;
import static org.jahia.translation.globallink.common.GlobalLinkConstants.GBL_PROJECT_TARGET_NODE;
import static org.jahia.translation.globallink.common.GlobalLinkConstants.JCR_DEFAULT_WS;
import static org.jahia.translation.globallink.common.GlobalLinkConstants.TRANSLATED_PATH;
import static org.jahia.translation.globallink.common.SubmissionStatus.STATUS_CONTENT_ERROR;
import static org.jahia.translation.globallink.common.SubmissionStatus.STATUS_TRANSLATED;

/**
 * Implementation for Global Link translated content process service
 * Check ALL translated documents retrieved from Global Link Server and
 * persist translated content nodes in JCR.
 *
 * @author Prince.Arora, WebItUp.
 * @author Aashish.Kocchar, WebitUp.
 */
@Component(service = GlobalLinkTranslatedContentProcessService.class, immediate = true)
public class GlobalLinkTranslatedContentProcessServiceImpl implements GlobalLinkTranslatedContentProcessService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalLinkTranslatedContentProcessServiceImpl.class);

    private GlobalLinkQueryService gblQueryService;

    private GlobalLinkDocumentService documentService;

    private JCRSessionWrapper sessionWrapper;

    private SiteContentService contentService;
    private MailService mailService;
    private GlobalLinkMailService globalLinkMailService;

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
    @Override
    public void processTranslatedContent(List<GlobalLinkConfigurationDTO> configList) {
        LOGGER.info("==== Initializing Process Translated content  =====");
        this.sessionWrapper = JCRUtil.getRootSession(JCR_DEFAULT_WS);
        for (GlobalLinkConfigurationDTO config : configList) {
            processRetrievedRequestsForConfig(config);
        }
        LOGGER.info("==== End of Process Translated content  =====");
    }

    /**
     * Process all requests for a site.
     *
     * @param config
     */
    private void processRetrievedRequestsForConfig(GlobalLinkConfigurationDTO config) {
        JCRNodeIteratorWrapper retrievedRequests = this.gblQueryService
                .getRetrievedRequests(config.getSiteNode().getPath(), this.sessionWrapper.getWorkspace().getQueryManager());
        while (retrievedRequests.hasNext()) {
            JCRNodeWrapper request = (JCRNodeWrapper) retrievedRequests.next();
            processRequest(request, config);
        }
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
            JCRNodeWrapper node = (JCRNodeWrapper) requestNode.getProperty(GBL_PROJECT_TARGET_NODE).getNode();
            boolean allCorrectlyTranslated = true;
            for (Value targetLanguages : requestNode.getProperty(GBL_PROJECT_TARGET_LANG).getValues()) {
                String languageMapping = targetLanguages.getString();
                String language = StringUtils.substringAfter(languageMapping, "###");
                String fileName = "";
                if (config.getDocumentPath() != null && !config.getDocumentPath().equals("")) {
                    fileName = config.getDocumentPath() + File.separator + requestId + File.separator + TRANSLATED_PATH + File.separator
                            + GlobalLinkUtil.getGLLocale(language) + "_" + node.getIdentifier() + FILE_EXT_XML;
                } else {
                    fileName =
                            DOCUMENT_PATH + File.separator + requestId + File.separator + TRANSLATED_PATH + File.separator + GlobalLinkUtil
                                    .getGLLocale(language) + "_" + node.getIdentifier() + FILE_EXT_XML;
                }
                File file = IOUtil.getFile(fileName);
                if (file != null) {
                    allCorrectlyTranslated = processTranslatedDocument(file, requestNode, languageMapping) && allCorrectlyTranslated;
                }
            }
            if (allCorrectlyTranslated && mailService.isEnabled()) {
                globalLinkMailService.sendNotificationMail(requestNode, STATUS_TRANSLATED);
            }
        } catch (RepositoryException re) {
            LOGGER.error("Error while processing request node: {} Exception {}", requestNode, re);
        }
    }

    /**
     * Handle translated document and check all translation content
     * for respective bigtext nodes.
     */
    private boolean processTranslatedDocument(File file, JCRNodeWrapper requestNode, String language) {
        LOGGER.info("Handle received translation for {} request  in {} language", requestNode.getName(), language);
        JCRNodeWrapper pageNode = null;
        try {
            pageNode = (JCRNodeWrapper) requestNode.getProperty(GBL_PROJECT_TARGET_NODE).getNode();
            this.contentService.lockNode(pageNode, this.sessionWrapper);
            String locale = StringUtils.substringBefore(language, "###");
            NodeList contentNodes = this.documentService.getTranslatedContentList(file);
            String sourceLanguage = StringUtils.substringBefore(requestNode.getProperty(GBL_PROJECT_SOURCE_LANG).getString(), "###");

            if (!pageNode.getResolveSite().getLanguages().contains(sourceLanguage)) {
                throw new GlobalLinkServiceException("There is no language matching this source on this site");
            }
            if (!pageNode.getResolveSite().getLanguages().contains(locale)) {
                throw new GlobalLinkServiceException("There is no language matching this target on this site");
            }
            this.contentService.checkInTranslatedContent(contentNodes, this.sessionWrapper, locale, sourceLanguage);
            this.contentService.updateRequestStatus(requestNode, this.sessionWrapper, STATUS_TRANSLATED);
            this.contentService.unLockNode(pageNode, this.sessionWrapper);
            return true;
        } catch (GlobalLinkServiceException ex) {
            this.contentService.updateRequestStatus(requestNode, this.sessionWrapper, STATUS_CONTENT_ERROR);
            if (mailService.isEnabled()) {
                globalLinkMailService.sendNotificationMail(requestNode, STATUS_CONTENT_ERROR);
            }
            LOGGER.error("Error while checking in content: ", ex);
        } catch (Exception ex) {
            LOGGER.error("Error while processing document: ", ex);
        } finally {
            if (pageNode != null) {
                this.contentService.unLockNode(pageNode, this.sessionWrapper);
            }
        }
        return false;
    }
}
