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

import org.jahia.services.content.JCRContentUtils;
import org.jahia.services.content.JCRNodeIteratorWrapper;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.translation.globallink.dto.GlobalLinkProjectRequestDTO;
import org.jahia.translation.globallink.exception.GlobalLinkServiceException;
import org.jahia.translation.globallink.service.api.GlobalLinkQueryService;
import org.jahia.translation.globallink.service.api.SiteContentService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
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
@Component(service = SiteContentService.class, immediate = true)
public class SiteContentServiceImpl implements SiteContentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SiteContentServiceImpl.class);

    private GlobalLinkQueryService queryService;

    /**
     * {@inheritDoc}
     */
    @Override
    public void logProjectRequestInJcr(GlobalLinkProjectRequestDTO requestDTO, boolean isSuccess,
                                       JCRSessionWrapper sessionWrapper) {
        try {
            LOGGER.info("Creating status node");
            JCRNodeWrapper statusNode = requestDTO.getNodeWrapper();
            if (isSuccess) {
                statusNode.setProperty(GBL_SUBMISSION_STATE, STATUS_SUBMITTED);
                statusNode.setProperty(GBL_PROJECT_SUB_TICKET, requestDTO.getSubmissionId());
                statusNode.setProperty(GBL_PROJECT_UPLOAD_TICKET, requestDTO.getUploadTicket());
                if (requestDTO.isChildIncluded()) {
                    JCRNodeIteratorWrapper nodeWrapperList = this.queryService.getRequestNodeList(requestDTO.getRequestId(),
                            sessionWrapper.getWorkspace().getQueryManager());
                    while (nodeWrapperList.hasNext()) {
                        JCRNodeWrapper nodeWrapper = (JCRNodeWrapper) nodeWrapperList.next();
                        try {
                            nodeWrapper.setProperty(GBL_PROJECT_SUB_TICKET, requestDTO.getSubmissionId());
                            nodeWrapper.setProperty(GBL_SUBMISSION_STATE, STATUS_SUBMITTED);
                        } catch (RepositoryException re) {
                            LOGGER.error("Exception while adding submission ticket for Child Page -> ", re);
                        }
                    }
                }
            }
            sessionWrapper.save();
        } catch (Exception ex) {
            throw new GlobalLinkServiceException(ex.getMessage(), ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean lockNode(JCRNodeWrapper nodeWrapper, JCRSessionWrapper sessionWrapper) {
        try {
            if (!nodeWrapper.isLocked() && nodeWrapper.isLockable()) {
                nodeWrapper.lock(false, true);
                sessionWrapper.save();
                return true;
            }
            return false;
        } catch (Exception ex) {
            throw new GlobalLinkServiceException(ex.getMessage(), ex);
        }
    }

    public boolean lockTranslationNode(JCRNodeWrapper nodeWrapper, JCRSessionWrapper sessionWrapper) {
        try {
            if (!nodeWrapper.isLocked() && nodeWrapper.isLockable()) {
                nodeWrapper.lockAndStoreToken("translation", " globalLink ");
                sessionWrapper.save();
                return true;
            }
            return false;
        } catch (Exception ex) {
            throw new GlobalLinkServiceException(ex.getMessage(), ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean unLockNode(JCRNodeWrapper nodeWrapper, JCRSessionWrapper sessionWrapper) {
        try {
            if (nodeWrapper.isLocked()) {
                nodeWrapper.unlock();
                sessionWrapper.save();
                return true;
            }
            return false;
        } catch (Exception ex) {
            throw new GlobalLinkServiceException(ex.getMessage(), ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void checkInTranslatedContent(NodeList contentNodes, JCRSessionWrapper sessionWrapper, String locale, String sourceLocale) {
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
            throw new GlobalLinkServiceException(ex.getMessage(), ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addTargetTicketsInStatus(JCRNodeWrapper projectNode, String target, JCRSessionWrapper sessionWrapper) {
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
            throw new GlobalLinkServiceException(ex.getMessage(), ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateRequestStatus(JCRNodeWrapper nodeWrapper, JCRSessionWrapper sessionWrapper, String status) {
        try {
            if (!status.equals(nodeWrapper.getPropertyAsString(GBL_SUBMISSION_STATE))) {
                nodeWrapper.setProperty(GBL_SUBMISSION_STATE, status);
                sessionWrapper.save();
            }
        } catch (RepositoryException ex) {
            throw new GlobalLinkServiceException(ex.getMessage(), ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JCRNodeWrapper addGlobalLinkRequestOnChildNode(JCRNodeWrapper nodeWrapper, JCRSessionWrapper sessionWrapper,
                                                          GlobalLinkProjectRequestDTO requestDTO) {
        try {
            final JCRNodeWrapper requestsFolderNode = nodeWrapper.getResolveSite().getNode(NODE_NAME_PROJECT_REQUESTS);
            final String newRequestName = JCRContentUtils.findAvailableNodeName(requestsFolderNode, NODE_NAME_GLOBAL_LINK);
            JCRNodeWrapper requestNode = requestsFolderNode.addNode(newRequestName, NODE_TYPE_PROJECT);
            requestNode.setProperty(GBL_PROJECT_TARGET_NODE, nodeWrapper);
            requestNode.setProperty(GBL_PROJECT_SOURCE_LANG, requestDTO.getSourceLanguage());
            requestNode.setProperty(GBL_PROJECT_TARGET_LANG, requestDTO.getDesLanguages());
            requestNode.setProperty(GBL_PROJECT_REQUEST_ID, requestDTO.getRequestId());
            requestNode.setProperty(GBL_SKIP_TRANSLATED, requestDTO.isSkipTranslated());
            requestNode.setProperty(GBL_INCLUDE_CHILD, requestDTO.isChildIncluded());
            requestNode.setProperty(GBL_IS_CHILD_REQUEST, true);
            sessionWrapper.save();
            return requestNode;
        } catch (RepositoryException re) {
            throw new GlobalLinkServiceException(re.getMessage(), re);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addUploadTicketForRequest(JCRNodeWrapper requestNode, JCRSessionWrapper sessionWrapper, String uploadTicket) {
        try {
            requestNode.setProperty(GBL_PROJECT_UPLOAD_TICKET, uploadTicket);
            sessionWrapper.save();
        } catch (Exception ex) {
            throw new GlobalLinkServiceException(ex.getMessage(), ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addRequestId(JCRNodeWrapper requestNode, JCRSessionWrapper sessionWrapper, String requestId) {
        try {
            requestNode.setProperty(GBL_PROJECT_REQUEST_ID, requestId);
            sessionWrapper.save();
        } catch (Exception ex) {
            throw new GlobalLinkServiceException(ex.getMessage(), ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addTransStateForContentNode(JCRNodeWrapper translationNode, JCRSessionWrapper sessionWrapper) {
        try {
            if (translationNode.isLocked()) {
                translationNode.unlock("translation", " globalLink ");
            }
            translationNode.setProperty(NODE_NAME_TRANS_PROP, true);
            sessionWrapper.save();
        } catch (Exception ex) {
            throw new GlobalLinkServiceException(ex.getMessage(), ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addContentCount(JCRNodeWrapper requestNode, JCRSessionWrapper sessionWrapper, int count) {
        try {
            requestNode.setProperty(GBL_PROJECT_CONTENT_COUNT, count);
            sessionWrapper.save();
        } catch (Exception ex) {
            throw new GlobalLinkServiceException(ex.getMessage(), ex);
        }
    }

    @Override
    public void addTranslationRequestError(JCRNodeWrapper requestNode, JCRSessionWrapper sessionWrapper, String message) {
        try {
            if (!requestNode.hasProperty(GBL_PROJECT_ERROR)) {
                requestNode.setProperty(GBL_PROJECT_ERROR, message);
                sessionWrapper.save();
            }
        } catch (Exception ex) {
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
       try {
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

                   String xmlNodeName = currentNode.getNodeName();
                   if (currentNode.getAttributes() != null && currentNode.getAttributes().getNamedItem("realName") != null) {
                       xmlNodeName = currentNode.getAttributes().getNamedItem("realName").getNodeValue();
                   }

                   if (currentNode.getChildNodes().getLength() > 1) {
                       NodeList contentNodeList = currentNode.getChildNodes();
                       int length = contentNodeList.getLength();
                       String[] transContentList = new String[length];
                       for (int i = 0; i < length; i++) {
                           transContentList[i] = contentNodeList.item(i).getTextContent();
                       }
                       translationNode.setProperty(xmlNodeName, transContentList);
                   } else {
                       translationNode.setProperty(xmlNodeName, currentNode.getFirstChild().getTextContent());
                   }
               }
           }
           //Adding translation status for source node.
           this.addTransStateForContentNode(jcrContentNode.getNode(NODE_TRANSLATE_PREFIX + sourceLocale),
               sessionWrapper);
       } finally {
           if (translationNode != null && translationNode.isLocked()) {
               LOGGER.warn("Translation have not been unlocked when adding the translated values. Node unlocked after the execution. Node: {}", translationNode.getPath());
               translationNode.unlock("translation", " globalLink ");
           }
       }
    }

    @Reference
    public void setQueryService(GlobalLinkQueryService queryService) {
        this.queryService = queryService;
    }
}
