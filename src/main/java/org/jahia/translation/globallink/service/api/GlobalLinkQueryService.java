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
package org.jahia.translation.globallink.service.api;

import org.jahia.services.content.JCRNodeIteratorWrapper;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.jahia.translation.globallink.exception.GlobalLinkServiceException;

import javax.jcr.query.QueryManager;
import java.util.List;
import java.util.Set;

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
     */
    List<JCRSiteNode> getAllSites(QueryManager queryManager);

    /**
     * Get {@link JCRNodeIteratorWrapper} for all Global link translation projects under a given site node
     *
     * @param siteNode
     * @return
     * @throws GlobalLinkServiceException
     */
    JCRNodeIteratorWrapper getGBLRequests(JCRSiteNode siteNode, QueryManager queryManager);

    /**
     * Find request submission node by given document id.
     *
     * @param contentId
     * @param queryManager
     * @return
     */
    JCRNodeIteratorWrapper getSubmissionNodeByContentId(String contentId, QueryManager queryManager);

    /**
     * Get list of all request nodes that has a given Reques Id.
     *
     * @param requestId
     * @param queryManager
     * @return
     */
    JCRNodeIteratorWrapper getRequestNodeList(String requestId, QueryManager queryManager);

    /**
     * Get all the active (not canceled) requests that has status as submitted.
     *
     * @param path
     * @param queryManager
     * @return
     */
    @Deprecated
    JCRNodeIteratorWrapper getActiveSubmittedRequests(String path, QueryManager queryManager);

    /**
     * Get request descendants of the path
     * @param path root path
     * @param queryManager query manager
     * @return Set of result descendant of the root path
     */
    Set<JCRNodeWrapper> getRequestsFilteredByPath(String path, QueryManager queryManager);

    /**
     * Get all submission requests that in retrieved state.
     *
     * @param path
     * @param queryManager
     * @return
     */
    JCRNodeIteratorWrapper getRetrievedRequests(String path, QueryManager queryManager);
}
