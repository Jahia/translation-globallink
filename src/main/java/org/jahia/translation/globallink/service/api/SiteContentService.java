package org.jahia.translation.globallink.service.api;

import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.translation.globallink.dto.GlobalLinkProjectRequestDTO;
import org.jahia.translation.globallink.exception.GlobalLinkServiceException;
import org.w3c.dom.NodeList;

/**
 * Operations for Site Content.
 *
 * @author Prince.Arora, WebItUp.
 */
public interface SiteContentService {

    /**
     * Save project submission status in JCR
     *
     * @param requestDTO
     * @param isSuccess
     * @return
     */
    boolean logProjectRequestInJcr(GlobalLinkProjectRequestDTO requestDTO, boolean isSuccess,
                                   JCRSessionWrapper sessionWrapper) throws GlobalLinkServiceException;

    /**
     * Lock the page for Translation process.
     *
     * @param nodeWrapper
     * @param sessionWrapper
     * @return
     * @throws GlobalLinkServiceException
     */
    boolean lockNode(JCRNodeWrapper nodeWrapper, JCRSessionWrapper sessionWrapper)
            throws GlobalLinkServiceException;

    /**
     * Unlock Page after translation process is complete.
     *
     * @param nodeWrapper
     * @param sessionWrapper
     * @return
     * @throws GlobalLinkServiceException
     */
    boolean unLockNode(JCRNodeWrapper nodeWrapper, JCRSessionWrapper sessionWrapper)
            throws GlobalLinkServiceException;

    /**
     * CheckIn translated content for a content node and save in JCR
     *
     * @param contentNodes
     * @param sessionWrapper
     * @param locale
     * @throws GlobalLinkServiceException
     */
    void checkInTranslatedContent(NodeList contentNodes, JCRSessionWrapper sessionWrapper, String locale, String sourceLocale)
            throws GlobalLinkServiceException;

    /**
     * Add target ticket in request node
     *
     * @param projectNode
     * @param target
     * @param sessionWrapper
     * @throws GlobalLinkServiceException
     */
    void addTargetTicketsInStatus(JCRNodeWrapper projectNode, String target, JCRSessionWrapper sessionWrapper)
            throws GlobalLinkServiceException;

    /**
     * Update request status for submission request
     *
     * @param nodeWrapper
     * @param sessionWrapper
     * @param status
     * @throws GlobalLinkServiceException
     */
    void updateRequestStatus(JCRNodeWrapper nodeWrapper, JCRSessionWrapper sessionWrapper, String status)
            throws GlobalLinkServiceException;

    /**
     * Add Global link project requestNode under a node using configurations
     * from given GlobalLinkProjectRequestDTO
     *
     * @param nodeWrapper
     * @param sessionWrapper
     * @param requestDTO
     * @throws GlobalLinkServiceException
     */
    JCRNodeWrapper addGlobalLinkRequestNode(JCRNodeWrapper nodeWrapper, JCRSessionWrapper sessionWrapper,
                                  GlobalLinkProjectRequestDTO requestDTO) throws GlobalLinkServiceException;

    /**
     * Add upload ticket for a request node in jcr.
     *
     * @param requestNode
     * @param sessionWrapper
     * @param uploadTicket
     * @throws GlobalLinkServiceException
     */
    void addUploadTicketForRequest(JCRNodeWrapper requestNode, JCRSessionWrapper sessionWrapper,
                                   String uploadTicket) throws GlobalLinkServiceException;

    /**
     * Add request id property for a Global Link request node.
     *
     * @param requestNode
     * @param sessionWrapper
     * @param requestId
     * @throws GlobalLinkServiceException
     */
    void addRequestId(JCRNodeWrapper requestNode, JCRSessionWrapper sessionWrapper, String requestId)
            throws GlobalLinkServiceException;

    /**
     * Add state for a content node to identify if the node is translated
     * previously or not.
     *
     * @param translationNode
     * @param sessionWrapper
     * @throws GlobalLinkServiceException
     */
    void addTransStateForContentNode(JCRNodeWrapper translationNode, JCRSessionWrapper sessionWrapper)
            throws GlobalLinkServiceException;

    /**
     * Add content count for global link request for a page.
     *
     * @param requestNode
     * @param sessionWrapper
     * @param count
     * @throws GlobalLinkServiceException
     */
    void addContentCount(JCRNodeWrapper requestNode, JCRSessionWrapper sessionWrapper, int count)
            throws GlobalLinkServiceException;

    void addTranslationRequestError(JCRNodeWrapper requestNode, JCRSessionWrapper sessionWrapper, String message)
            throws GlobalLinkServiceException;
}
