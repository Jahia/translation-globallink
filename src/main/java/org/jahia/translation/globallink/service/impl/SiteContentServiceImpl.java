package org.jahia.translation.globallink.service.impl;

import org.jahia.services.content.JCRNodeIteratorWrapper;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.translation.globallink.dto.GlobalLinkProjectRequestDTO;
import org.jahia.translation.globallink.exception.GlobalLinkServiceException;
import org.jahia.translation.globallink.service.api.GlobalLinkQueryService;
import org.jahia.translation.globallink.service.api.SiteContentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.jcr.RepositoryException;

import static org.jahia.translation.globallink.common.GlobalLinkConstants.*;
import static org.jahia.translation.globallink.common.SubmissionStatus.STATUS_SUBMITTED;

/**
 * Implementation for content service
 *
 * @author Prince.Arora, WebItUp.
 */
public class SiteContentServiceImpl implements SiteContentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SiteContentServiceImpl.class);

    private GlobalLinkQueryService queryService;

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean logProjectRequestInJcr(GlobalLinkProjectRequestDTO requestDTO, boolean isSuccess,
                                          JCRSessionWrapper sessionWrapper) throws GlobalLinkServiceException {
        try {
            LOGGER.info("Creating status node");
            JCRNodeWrapper statusNode = requestDTO.getNodeWrapper();
            if (isSuccess) {
                statusNode.setProperty(GBL_SUBMISSION_STATE, STATUS_SUBMITTED);
                statusNode.setProperty(GBL_PROJECT_SUB_TICKET, requestDTO.getSubmitTicket());
                statusNode.setProperty(GBL_PROJECT_UPLOAD_TICKET, requestDTO.getUploadTicket());
                if (requestDTO.isChildIncluded()) {
                    JCRNodeIteratorWrapper nodeWrapperList = this.queryService.getRequestNodeList(requestDTO.getRequestId(),
                            sessionWrapper.getWorkspace().getQueryManager());
                    while (nodeWrapperList.hasNext()) {
                        JCRNodeWrapper nodeWrapper = (JCRNodeWrapper) nodeWrapperList.next();
                        try {
                            nodeWrapper.setProperty(GBL_PROJECT_SUB_TICKET, requestDTO.getSubmitTicket());
                            nodeWrapper.setProperty(GBL_SUBMISSION_STATE, STATUS_SUBMITTED);
                        } catch (RepositoryException re) {
                            LOGGER.error("Exception while adding submission ticket for Child Page -> ", re);
                        }
                    }
                }
            }
            sessionWrapper.save();
            return true;
        } catch (Exception ex) {
            LOGGER.error("Service Exception -> ", ex);
            throw new GlobalLinkServiceException(ex.getMessage(), ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean lockNode(JCRNodeWrapper nodeWrapper, JCRSessionWrapper sessionWrapper) throws GlobalLinkServiceException {
        try {
            if (!nodeWrapper.isLocked() && nodeWrapper.isLockable()) {
                nodeWrapper.lock(false, true);
                sessionWrapper.save();
                return true;
            }
            return false;
        } catch (Exception ex) {
            LOGGER.error("Service Exception -> ", ex);
            throw new GlobalLinkServiceException(ex.getMessage(), ex);
        }
    }

    public boolean lockTranslationNode(JCRNodeWrapper nodeWrapper, JCRSessionWrapper sessionWrapper) throws GlobalLinkServiceException {
        try {
            if (!nodeWrapper.isLocked() && nodeWrapper.isLockable()) {
                nodeWrapper.lockAndStoreToken("translation"," globalLink ");
                sessionWrapper.save();
                return true;
            }
            return false;
        } catch (Exception ex) {
            LOGGER.error("Service Exception -> ", ex);
            throw new GlobalLinkServiceException(ex.getMessage(), ex);
        }
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean unLockNode(JCRNodeWrapper nodeWrapper, JCRSessionWrapper sessionWrapper) throws GlobalLinkServiceException {
        try {
            if (nodeWrapper.isLocked()) {
                nodeWrapper.unlock();
                sessionWrapper.save();
                return true;
            }
            return false;
        } catch (Exception ex) {
            LOGGER.error("Service Exception -> ", ex);
            throw new GlobalLinkServiceException(ex.getMessage(), ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void checkInTranslatedContent(NodeList contentNodes, JCRSessionWrapper sessionWrapper, String locale, String sourceLocale)
            throws GlobalLinkServiceException {
        try {
            for (int index = 0; index < contentNodes.getLength(); index++) {
                Node contentNode = contentNodes.item(index);
                if (contentNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) contentNode;
                    JCRNodeWrapper jcrContentNode = sessionWrapper.getNodeByIdentifier(element.getAttribute(DOCUMENT_CONTENT_PROP_UUID));
                    savePropertiesInJcr(element, jcrContentNode, locale, sourceLocale, sessionWrapper);
                }
            }
            sessionWrapper.save();
        } catch (RepositoryException ex) {
            LOGGER.error("Service Exception -> ", ex);
            throw new GlobalLinkServiceException(ex.getMessage(), ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addTargetTicketsInStatus(JCRNodeWrapper projectNode, String target, JCRSessionWrapper sessionWrapper)
            throws GlobalLinkServiceException {
        try {
            if (projectNode.hasProperty(GBL_PROJECT_TARGET) &&
                    !projectNode.getPropertyAsString(GBL_PROJECT_TARGET).equals("")) {
                projectNode.setProperty(GBL_PROJECT_TARGET, projectNode.getPropertyAsString(GBL_PROJECT_TARGET) + ","
                        + target);
            } else {
                projectNode.setProperty(GBL_PROJECT_TARGET, target);
            }
            sessionWrapper.save();
        } catch (RepositoryException ex) {
            LOGGER.error("Service Exception -> ", ex);
            throw new GlobalLinkServiceException(ex.getMessage(), ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateRequestStatus(JCRNodeWrapper nodeWrapper, JCRSessionWrapper sessionWrapper, String status)
            throws GlobalLinkServiceException {
        try {
            if(!nodeWrapper.isLocked() && !status.equals(nodeWrapper.getPropertyAsString(GBL_SUBMISSION_STATE))) {
                nodeWrapper.setProperty(GBL_SUBMISSION_STATE, status);
                sessionWrapper.save();
            }
        } catch (RepositoryException ex) {
            LOGGER.error("Service Exception -> ", ex);
            throw new GlobalLinkServiceException(ex.getMessage(), ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JCRNodeWrapper addGlobalLinkRequestNode(JCRNodeWrapper nodeWrapper, JCRSessionWrapper sessionWrapper,
                                                   GlobalLinkProjectRequestDTO requestDTO) throws GlobalLinkServiceException {
        try {
            if (nodeWrapper.hasNode(NODE_NAME_GLOBAL_LINK)) {
                nodeWrapper.getNode(NODE_NAME_GLOBAL_LINK).remove();
            }
            JCRNodeWrapper requestNode = nodeWrapper.addNode(NODE_NAME_GLOBAL_LINK, NODE_TYPE_PROJECT);
            requestNode.setProperty(GBL_PROJECT_SOURCE_LANG, requestDTO.getSourceLanguage());
            requestNode.setProperty(GBL_PROJECT_TARGET_LANG, requestDTO.getDesLanguages());
            requestNode.setProperty(GBL_PROJECT_REQUEST_ID, requestDTO.getRequestId());
            requestNode.setProperty(GBL_SKIP_TRANSLATED, requestDTO.isSkipTranslated());
            requestNode.setProperty(GBL_INCLUDE_CHILD, requestDTO.isChildIncluded());
            sessionWrapper.save();
            return requestNode;
        } catch (RepositoryException re) {
            LOGGER.error("Service Exception -> ", re);
            throw new GlobalLinkServiceException(re.getMessage(), re);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addUploadTicketForRequest(JCRNodeWrapper requestNode, JCRSessionWrapper sessionWrapper, String uploadTicket)
            throws GlobalLinkServiceException {
        try {
            requestNode.setProperty(GBL_PROJECT_UPLOAD_TICKET, uploadTicket);
            sessionWrapper.save();
        } catch (Exception ex) {
            LOGGER.error("Service Exception -> ", ex);
            throw new GlobalLinkServiceException(ex.getMessage(), ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addRequestId(JCRNodeWrapper requestNode, JCRSessionWrapper sessionWrapper, String requestId)
            throws GlobalLinkServiceException {
        try {
            requestNode.setProperty(GBL_PROJECT_REQUEST_ID, requestId);
            sessionWrapper.save();
        } catch (Exception ex) {
            LOGGER.error("Service Exception -> ", ex);
            throw new GlobalLinkServiceException(ex.getMessage(), ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addTransStateForContentNode(JCRNodeWrapper translationNode, JCRSessionWrapper sessionWrapper)
            throws GlobalLinkServiceException {
        try {
            if (translationNode.isLocked()) {
                translationNode.unlock("translation"," globalLink ");
            }
            translationNode.setProperty(NODE_NAME_TRANS_PROP, true);
            sessionWrapper.save();
        } catch (Exception ex) {
            LOGGER.error("Service Exception -> ", ex);
            throw new GlobalLinkServiceException(ex.getMessage(), ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addContentCount(JCRNodeWrapper requestNode, JCRSessionWrapper sessionWrapper, int count)
            throws GlobalLinkServiceException {
        try {
            requestNode.setProperty(GBL_PROJECT_CONTENT_COUNT, count);
            sessionWrapper.save();
        } catch (Exception ex) {
            LOGGER.error("Service Exception -> ", ex);
            throw new GlobalLinkServiceException(ex.getMessage(), ex);
        }
    }

    @Override
    public void addTranslationRequestError(JCRNodeWrapper requestNode, JCRSessionWrapper sessionWrapper, String message) throws GlobalLinkServiceException {
        try {
            if (!requestNode.hasProperty(GBL_PROJECT_ERROR)) {
                requestNode.setProperty(GBL_PROJECT_ERROR, message);
                sessionWrapper.save();
            }
        } catch (Exception ex) {
            LOGGER.error("Service Exception -> ", ex);
            throw new GlobalLinkServiceException(ex.getMessage(), ex);
        }
    }

    /**
     * Save translate content from translated document in respective node
     * properties.
     *
     * @param element
     * @param jcrContentNode
     * @param locale
     * @param sourceLocale
     * @param sessionWrapper
     * @throws RepositoryException
     */
    private void savePropertiesInJcr(Element element, JCRNodeWrapper jcrContentNode, String locale, String sourceLocale,
                                     JCRSessionWrapper sessionWrapper) throws RepositoryException {
        JCRNodeWrapper translationNode = null;
        if (jcrContentNode.hasNode(NODE_TRANSLATE_PREFIX + locale)) {
            if (!jcrContentNode.getNode(NODE_TRANSLATE_PREFIX + locale).hasProperty(NODE_PROP_LANGUAGE)) {
                jcrContentNode.getNode(NODE_TRANSLATE_PREFIX + locale).setProperty(NODE_PROP_LANGUAGE, locale);
            }
            translationNode = jcrContentNode.getNode(NODE_TRANSLATE_PREFIX + locale);
        } else {
            translationNode = jcrContentNode.addNode(NODE_TRANSLATE_PREFIX + locale, NODE_TRANLSATE_TYPE);
            translationNode.setProperty(NODE_PROP_LANGUAGE, locale);
        }
        NodeList nodeList = element.getChildNodes();
        if (nodeList != null) {
            for (int index = 0; index < nodeList.getLength(); index++) {
                Node currentNode = nodeList.item(index);
                String xmlNodename = currentNode.getNodeName();
                if (xmlNodename.contains("_")) {
                    xmlNodename = xmlNodename.replace("_", ":");
                }
                if (currentNode.getChildNodes().getLength() > 1) {
                    NodeList contentNodeList = currentNode.getChildNodes();
                    int length = contentNodeList.getLength();
                    String[] transContentList = new String[length];
                    for (int i = 0; i < length; i++) {
                        transContentList[i] = contentNodeList.item(i).getTextContent();
                    }
                    translationNode.setProperty(xmlNodename, transContentList);
                } else {
                    translationNode.setProperty(xmlNodename, currentNode.getFirstChild().getTextContent());
                }
            }
        }
        //Adding translation status for source node.
        this.addTransStateForContentNode(jcrContentNode.getNode(NODE_TRANSLATE_PREFIX + sourceLocale),
                sessionWrapper);
    }

    public void setQueryService(GlobalLinkQueryService queryService) {
        this.queryService = queryService;
    }
}
