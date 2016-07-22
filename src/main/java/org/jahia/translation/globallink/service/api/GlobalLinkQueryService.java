package org.jahia.translation.globallink.service.api;

import org.jahia.services.content.JCRNodeIteratorWrapper;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.jahia.translation.globallink.exception.GlobalLinkServiceException;

import javax.jcr.query.QueryManager;
import java.util.List;

/**
 * JCR Query Service for Global Link Translation module.
 *
 * @author Aashish.Kocchar, WebItUp.
 */
public interface GlobalLinkQueryService {

    /**
     * Executes JCR-SQL2 query and provides {@link JCRNodeIteratorWrapper}
     * for the same using the {@link QueryManager} provided in argument.
     *
     * @param query
     * @param queryManager
     * @return
     * @throws GlobalLinkServiceException
     */
    JCRNodeIteratorWrapper getQueryResult(String query, QueryManager queryManager) throws GlobalLinkServiceException;

    /**
     * Executes JCR-SQL2 query and provides {@link JCRNodeIteratorWrapper}
     * for all the projects under sites
     *
     * @param queryManager
     * @return
     * @throws GlobalLinkServiceException
     */
    List<JCRSiteNode> getAllSites(QueryManager queryManager) throws GlobalLinkServiceException;

    /**
     * Get {@link JCRNodeIteratorWrapper} for all Global link translation projects under a given site node
     *
     * @param siteNode
     * @return
     * @throws GlobalLinkServiceException
     */
    JCRNodeIteratorWrapper getGBLRequests(JCRSiteNode siteNode, QueryManager queryManager) throws GlobalLinkServiceException;

    /**
     * Get {@link JCRNodeIteratorWrapper} for all big text nodes under a given page node.
     *
     * @param node
     * @return
     * @throws GlobalLinkServiceException
     */
    JCRNodeIteratorWrapper getAllBigText(JCRNodeWrapper node, QueryManager queryManager) throws GlobalLinkServiceException;

    /**
     * Find request submission node by given document ticket.
     *
     * @param documentTicket
     * @param queryManager
     * @return
     * @throws GlobalLinkServiceException
     */
    JCRNodeIteratorWrapper getSubmissionNodeByDocumentTicket(String documentTicket, QueryManager queryManager)
            throws GlobalLinkServiceException;

    /**
     * Get list of all request nodes that has a given Reques Id.
     *
     * @param requestId
     * @param queryManager
     * @return
     * @throws GlobalLinkServiceException
     */
    JCRNodeIteratorWrapper getRequestNodeList(String requestId, QueryManager queryManager) throws GlobalLinkServiceException;

    /**
     * Get all the requests that has status as submitted.
     *
     * @param path
     * @param queryManager
     * @return
     * @throws GlobalLinkServiceException
     */
    JCRNodeIteratorWrapper getSubmittedRequests(String path, QueryManager queryManager) throws GlobalLinkServiceException;

    /**
     * Get all submission requests that in retrieved state.
     *
     * @param path
     * @param queryManager
     * @return
     * @throws GlobalLinkServiceException
     */
    JCRNodeIteratorWrapper getRetrievedRequests(String path, QueryManager queryManager) throws GlobalLinkServiceException;
}
