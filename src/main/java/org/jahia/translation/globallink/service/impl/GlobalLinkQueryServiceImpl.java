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

import org.jahia.services.content.JCRNodeIteratorWrapper;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.jahia.services.query.QueryResultWrapper;
import org.jahia.services.query.ScrollableQuery;
import org.jahia.services.query.ScrollableQueryCallback;
import org.jahia.translation.globallink.exception.GlobalLinkServiceException;
import org.jahia.translation.globallink.service.api.GlobalLinkQueryService;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import java.util.*;

import static org.jahia.translation.globallink.common.GlobalLinkConstants.NODE_TYPE_PROJECT;
import static org.jahia.translation.globallink.common.SubmissionStatus.*;

/**
 * Implementation for JCR Query service for Global Link Translation
 *
 * @author Aashish.Kocchar, WebitUp.
 * @author Mate.Ezgeta, Jahia.
 */
@Component(service = GlobalLinkQueryService.class, immediate = true)
public class GlobalLinkQueryServiceImpl implements GlobalLinkQueryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalLinkQueryServiceImpl.class);

    private static final String SITE_QUERY = "select * from [gblmix:globalLinkBaseSettings] as site where ISDESCENDANTNODE(site, [/sites])";

    /**
     * {@inheritDoc}
     */
    @Override public JCRNodeIteratorWrapper getQueryResult(String query, QueryManager queryManager) {
        try {
            Query jcrQuery = queryManager.createQuery(query, Query.JCR_SQL2);
            QueryResultWrapper queryResult = (QueryResultWrapper) jcrQuery.execute();
            return queryResult.getNodes();
        } catch (Exception ex) {
            throw new GlobalLinkServiceException(ex.getMessage(), ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override public List<JCRSiteNode> getAllSites(QueryManager queryManager) {
        try {
            List<JCRSiteNode> siteList = new ArrayList<>();
            Query jcrQuery = queryManager.createQuery(SITE_QUERY, Query.JCR_SQL2);
            QueryResultWrapper queryResult = (QueryResultWrapper) jcrQuery.execute();
            JCRNodeIteratorWrapper nodes = queryResult.getNodes();
            while (nodes.hasNext()) {
                JCRNodeWrapper node = (JCRNodeWrapper) nodes.next();
                JCRSiteNode siteNode = getResolveSite(node);
                if (siteNode != null) {
                    siteList.add(siteNode);
                }
            }
            return siteList;
        } catch (Exception ex) {
            throw new GlobalLinkServiceException(ex.getMessage(), ex);
        }
    }

    private JCRSiteNode getResolveSite(JCRNodeWrapper node) {
        try {
            return node.getResolveSite();
        } catch (Exception e) {
            LOGGER.error("Cannot get site for node -> {}", node.getPath());
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override public JCRNodeIteratorWrapper getGBLRequests(JCRSiteNode siteNode, QueryManager queryManager) {
        try {
            String query =
                    "select * from [" + NODE_TYPE_PROJECT + "] as gblProject where ISDESCENDANTNODE(gblProject, [" + siteNode.getPath()
                            + "])";
            Query jcrQuery = queryManager.createQuery(query, Query.JCR_SQL2);
            QueryResultWrapper queryResult = (QueryResultWrapper) jcrQuery.execute();
            return queryResult.getNodes();
        } catch (RepositoryException ex) {
            throw new GlobalLinkServiceException(ex.getMessage(), ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override public JCRNodeIteratorWrapper getSubmissionNodeByContentId(String contentId, QueryManager queryManager) {
        return Optional.of(new StringBuilder()).map(stringBuilder -> stringBuilder.append("select * from [").append(NODE_TYPE_PROJECT)
                .append("] as gblProject where gblProject.uploadTicket = '").append(contentId)
                .append("' AND ISDESCENDANTNODE(gblProject, [/sites])")).map(stringBuilder -> {
            try {
                Query jcrQuery = queryManager.createQuery(stringBuilder.toString(), Query.JCR_SQL2);
                return ((QueryResultWrapper) jcrQuery.execute()).getNodes();
            } catch (RepositoryException e) {
                throw new GlobalLinkServiceException(e.getMessage(), e);
            }
        }).orElse(null);
    }

    /**
     * {@inheritDoc}
     */
    @Override public JCRNodeIteratorWrapper getRequestNodeList(String requestId, QueryManager queryManager) {
        try {
            String query = "select * from [" + NODE_TYPE_PROJECT + "] as gblProject where gblProject.gblRequestId = '" + requestId
                    + "' AND ISDESCENDANTNODE(gblProject, [/sites])";
            Query jcrQuery = queryManager.createQuery(query, Query.JCR_SQL2);
            QueryResultWrapper queryResult = (QueryResultWrapper) jcrQuery.execute();
            return queryResult.getNodes();
        } catch (Exception ex) {
            throw new GlobalLinkServiceException(ex.getMessage(), ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Deprecated
    @Override public JCRNodeIteratorWrapper getSubmittedRequests(String path, QueryManager queryManager) {
        try {
            String query = "select * from [" + NODE_TYPE_PROJECT + "] as gblProject where gblProject.gblSubmitState = '" + STATUS_SUBMITTED
                    + "' OR gblProject.gblSubmitState = '" + STATUS_CANCELLED + "' OR gblProject.gblSubmitState = '" + STATUS_TRANSLATED
                    + "' AND ISDESCENDANTNODE(gblProject, [" + path + "])";
            Query jcrQuery = queryManager.createQuery(query, Query.JCR_SQL2);
            QueryResultWrapper queryResult = (QueryResultWrapper) jcrQuery.execute();
            return queryResult.getNodes();
        } catch (Exception ex) {
            throw new GlobalLinkServiceException(ex.getMessage(), ex);
        }
    }

    public Set<JCRNodeWrapper> getRequestsFilteredByPath(String path, QueryManager queryManager) {
        try {
            String query = "select * from [" + NODE_TYPE_PROJECT + "] as gblProject where gblProject.gblSubmitState = '" + STATUS_SUBMITTED
                + "' OR gblProject.gblSubmitState = '" + STATUS_CANCELLED + "' OR gblProject.gblSubmitState = '" + STATUS_TRANSLATED + "'";
            Query jcrQuery = queryManager.createQuery(query, Query.JCR_SQL2);
            ScrollableQuery scrollableQuery = new ScrollableQuery(500, jcrQuery);

            return scrollableQuery.execute(new ScrollableQueryCallback<Set<JCRNodeWrapper>>() {
                final Set<JCRNodeWrapper> result = new HashSet<>();

                @Override
                public boolean scroll() throws RepositoryException {
                    stepResult.getNodes().forEachRemaining(node -> {
                        JCRNodeWrapper currentNode = (JCRNodeWrapper) node;
                        try {
                            if ((currentNode.getProperty("targetNode").getNode().getPath() + "/").startsWith(path + "/")) {
                                result.add(currentNode);
                            }
                        } catch (RepositoryException e) {
                            LOGGER.error("Error while getting target node: {}",e.getMessage());
                            LOGGER.debug("Error: ", e);
                        }
                    });
                    return true;
                }

                @Override
                protected Set<JCRNodeWrapper> getResult() {
                    return result;
                }
            });
        } catch (Exception ex) {
            throw new GlobalLinkServiceException(ex.getMessage(), ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override public JCRNodeIteratorWrapper getRetrievedRequests(String path, QueryManager queryManager) {
        try {
            String query = "select * from [" + NODE_TYPE_PROJECT + "] as gblProject where gblProject.gblSubmitState = '" + STATUS_RETRIEVED
                    + "' AND ISDESCENDANTNODE(gblProject, [" + path + "])";
            Query jcrQuery = queryManager.createQuery(query, Query.JCR_SQL2);
            QueryResultWrapper queryResult = (QueryResultWrapper) jcrQuery.execute();
            return queryResult.getNodes();
        } catch (Exception ex) {
            throw new GlobalLinkServiceException(ex.getMessage(), ex);
        }
    }

}
