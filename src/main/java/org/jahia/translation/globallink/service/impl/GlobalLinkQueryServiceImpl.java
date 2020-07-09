package org.jahia.translation.globallink.service.impl;

import org.jahia.services.content.JCRNodeIteratorWrapper;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.jahia.services.query.QueryResultWrapper;
import org.jahia.translation.globallink.exception.GlobalLinkServiceException;
import org.jahia.translation.globallink.service.api.GlobalLinkQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.jahia.translation.globallink.common.GlobalLinkConstants.NODE_TYPE_PROJECT;
import static org.jahia.translation.globallink.common.SubmissionStatus.*;

/**
 * Implementation for JCR Query service for Global Link Translation
 *
 * @author Aashish.Kocchar, WebitUp.
 * @author Mate.Ezgeta, Jahia.
 */
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
