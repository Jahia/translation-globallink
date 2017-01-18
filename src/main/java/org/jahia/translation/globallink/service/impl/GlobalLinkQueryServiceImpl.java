package org.jahia.translation.globallink.service.impl;

import org.jahia.services.content.JCRNodeIteratorWrapper;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.jahia.services.query.QueryResultWrapper;
import org.jahia.translation.globallink.exception.GlobalLinkServiceException;
import org.jahia.translation.globallink.service.api.GlobalLinkQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import java.util.ArrayList;
import java.util.List;

import static org.jahia.translation.globallink.common.GlobalLinkConstants.NODE_TYPE_BIGTEXT;
import static org.jahia.translation.globallink.common.GlobalLinkConstants.NODE_TYPE_PROJECT;
import static org.jahia.translation.globallink.common.SubmissionStatus.STATUS_CANCELLED;
import static org.jahia.translation.globallink.common.SubmissionStatus.STATUS_RETRIEVED;
import static org.jahia.translation.globallink.common.SubmissionStatus.STATUS_SUBMITTED;

/**
 * Implementation for JCR Query service for Global Link Translation
 *
 * @author Aashish.Kocchar, WebitUp.
 */
public class GlobalLinkQueryServiceImpl implements GlobalLinkQueryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalLinkQueryServiceImpl.class);

    private final String sitesQuery = "select * from [jmix:globalLinkBaseSettings] as site where ISDESCENDANTNODE(site, [/sites])";

    /**
     * {@inheritDoc}
     */
    @Override
    public JCRNodeIteratorWrapper getQueryResult(String query, QueryManager queryManager) throws GlobalLinkServiceException {
        try {
            Query jcrQuery = queryManager.createQuery(query, Query.JCR_SQL2);
            QueryResultWrapper queryResult = (QueryResultWrapper) jcrQuery.execute();
            return queryResult.getNodes();
        } catch (Exception ex) {
            LOGGER.error("Service Exception -> ", ex);
            throw new GlobalLinkServiceException(ex.getMessage(), ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<JCRSiteNode> getAllSites(QueryManager queryManager) throws GlobalLinkServiceException {
        try {
            List<JCRSiteNode> siteList = new ArrayList<>();
            Query jcrQuery = queryManager.createQuery(this.sitesQuery, Query.JCR_SQL2);
            QueryResultWrapper queryResult = (QueryResultWrapper) jcrQuery.execute();
            queryResult.getNodes().forEach(node -> {
                JCRSiteNode siteNode;
                try {
                    siteNode = node.getResolveSite();
                    siteList.add(siteNode);
                } catch (Exception e) {
                    LOGGER.error("Cannot get site for node -> " + node.getPath());
                }
            });
            return siteList;
        } catch (Exception ex) {
            LOGGER.error("Service Exception -> ", ex);
            throw new GlobalLinkServiceException(ex.getMessage(), ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JCRNodeIteratorWrapper getGBLRequests(JCRSiteNode siteNode, QueryManager queryManager)
            throws GlobalLinkServiceException {
        try {
            String query = "select * from [" + NODE_TYPE_PROJECT + "] as gblProject where ISDESCENDANTNODE(gblProject, [" + siteNode.getPath() + "])";
            Query jcrQuery = queryManager.createQuery(query, Query.JCR_SQL2);
            QueryResultWrapper queryResult = (QueryResultWrapper) jcrQuery.execute();
            return queryResult.getNodes();
        } catch (Exception ex) {
            LOGGER.error("Service Exception -> ", ex);
            throw new GlobalLinkServiceException(ex.getMessage(), ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JCRNodeIteratorWrapper getSubmissionNodeByDocumentTicket(String documentTicket, QueryManager queryManager)
            throws GlobalLinkServiceException {
        try {
            String query = "select * from [" + NODE_TYPE_PROJECT + "] as gblProject where gblProject.uploadTicket = '"
                    + documentTicket + "' AND ISDESCENDANTNODE(gblProject, [/sites])";
            Query jcrQuery = queryManager.createQuery(query, Query.JCR_SQL2);
            QueryResultWrapper queryResult = (QueryResultWrapper) jcrQuery.execute();
            return queryResult.getNodes();
        } catch (Exception ex) {
            LOGGER.error("Service Exception -> ", ex);
            throw new GlobalLinkServiceException(ex.getMessage(), ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JCRNodeIteratorWrapper getRequestNodeList(String requestId, QueryManager queryManager) throws GlobalLinkServiceException {
        try {
            String query = "select * from [" + NODE_TYPE_PROJECT + "] as gblProject where gblProject.gblRequestId = '"
                    + requestId + "' AND ISDESCENDANTNODE(gblProject, [/sites])";
            Query jcrQuery = queryManager.createQuery(query, Query.JCR_SQL2);
            QueryResultWrapper queryResult = (QueryResultWrapper) jcrQuery.execute();
            return queryResult.getNodes();
        } catch (Exception ex) {
            LOGGER.error("Service Exception -> ", ex);
            throw new GlobalLinkServiceException(ex.getMessage(), ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JCRNodeIteratorWrapper getSubmittedRequests(String path, QueryManager queryManager) throws GlobalLinkServiceException {
        try {
            String query = "select * from [" + NODE_TYPE_PROJECT + "] as gblProject where gblProject.gblSubmitState = '"
                    + STATUS_SUBMITTED + "' OR gblProject.gblSubmitState = '"
                    + STATUS_CANCELLED + "' AND ISDESCENDANTNODE(gblProject, [" + path + "])";
            Query jcrQuery = queryManager.createQuery(query, Query.JCR_SQL2);
            QueryResultWrapper queryResult = (QueryResultWrapper) jcrQuery.execute();
            return queryResult.getNodes();
        } catch (Exception ex) {
            LOGGER.error("Service Exception -> ", ex);
            throw new GlobalLinkServiceException(ex.getMessage(), ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JCRNodeIteratorWrapper getRetrievedRequests(String path, QueryManager queryManager)
            throws GlobalLinkServiceException {
        try {
            String query = "select * from [" + NODE_TYPE_PROJECT + "] as gblProject where gblProject.gblSubmitState = '"
                    + STATUS_RETRIEVED + "' AND ISDESCENDANTNODE(gblProject, [" + path + "])";
            Query jcrQuery = queryManager.createQuery(query, Query.JCR_SQL2);
            QueryResultWrapper queryResult = (QueryResultWrapper) jcrQuery.execute();
            return queryResult.getNodes();
        } catch (Exception ex) {
            LOGGER.error("Service Exception -> ", ex);
            throw new GlobalLinkServiceException(ex.getMessage(), ex);
        }
    }

}
