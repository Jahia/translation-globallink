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
package org.jahia.translation.globallink.rules;

import org.drools.core.spi.KnowledgeHelper;
import org.gs4tr.gcc.restclient.GCExchange;
import org.gs4tr.gcc.restclient.model.Status;
import org.jahia.services.content.JCRNodeIteratorWrapper;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.rules.AddedNodeFact;
import org.jahia.services.content.rules.DeletedNodeFact;
import org.jahia.services.mail.MailService;
import org.jahia.services.usermanager.JahiaUserManagerService;
import org.jahia.translation.globallink.dto.GlobalLinkConfigurationDTO;
import org.jahia.translation.globallink.service.api.GlobalLinkQueryService;
import org.jahia.translation.globallink.service.api.SiteContentService;
import org.jahia.translation.globallink.util.GlobalLinkUtil;
import org.jahia.translation.globallink.util.JCRUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.util.List;

import static org.jahia.translation.globallink.common.GlobalLinkConstants.GBL_PROJECT_SUB_TICKET;
import static org.jahia.translation.globallink.common.GlobalLinkConstants.JCR_DEFAULT_WS;
import static org.jahia.translation.globallink.common.SubmissionStatus.STATUS_CANCELLED;
import static org.jahia.translation.globallink.common.SubmissionStatus.STATUS_TRANSLATE;

/**
 * Created by rincevent on 2017-01-18.
 */
public class GlobalLinkSubmissionService {
    private final GlobalLinkQueryService queryService;
    private final SiteContentService contentService;
    private final MailService mailService;
    private final JahiaUserManagerService userManagerService;

    public GlobalLinkSubmissionService(GlobalLinkQueryService queryService, SiteContentService contentService, MailService mailService,
            JahiaUserManagerService userManagerService) {
        this.queryService = queryService;
        this.contentService = contentService;
        this.mailService = mailService;
        this.userManagerService = userManagerService;
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalLinkSubmissionService.class);

    public void removeEmptySubmission(AddedNodeFact addedNodeFact, KnowledgeHelper drools) {

        JCRSessionWrapper rootSession = JCRUtil.getRootSession(JCR_DEFAULT_WS);
        List<GlobalLinkConfigurationDTO> configList = JCRUtil
                .getConfigurationList(queryService.getAllSites(rootSession.getWorkspace().getQueryManager()));
        JCRNodeWrapper node = addedNodeFact.getNode();
        for (GlobalLinkConfigurationDTO config : configList) {

            if (node.getPath().startsWith(config.getSiteNode().getPath())) {
                JCRNodeIteratorWrapper submittedRequests = queryService
                        .getSubmittedRequests(node.getPath(), rootSession.getWorkspace().getQueryManager());
                GCExchange gcExchange = GlobalLinkUtil.getGlobalLinkClient(config);
                if (gcExchange != null) {
                    for (JCRNodeWrapper request : submittedRequests) {
                        try {
                            Long submissionId = request.getProperty(GBL_PROJECT_SUB_TICKET).getLong();
                            Status submissionStatus = gcExchange.getSubmissionStatus(submissionId);
                            if (submissionStatus.getStatusName().equals(STATUS_TRANSLATE)) {
                                LOGGER.info("Cancel submission {}, node identifier {}", submissionId, addedNodeFact.getIdentifier());
                                LOGGER.info("Request node {}", request.getIdentifier());
                                gcExchange.cancelSubmission(submissionId);
                                contentService.updateRequestStatus(request, rootSession, STATUS_CANCELLED);
                            }
                            drools.insert(new DeletedNodeFact(addedNodeFact, request.getPath()));
                        } catch (Exception e) {
                            LOGGER.error("Error while releting translation", e);
                        }
                    }
                }
            }
        }
    }

    public void lockTranslationNode(AddedNodeFact nodeFact) {
        try {
            JCRSessionWrapper rootSession = JCRUtil.getRootSession(JCR_DEFAULT_WS);
            JCRNodeWrapper node = rootSession.getNode(nodeFact.getPath());
            node.lockAndStoreToken("infiniteTranslationLock");
            rootSession.save();
        } catch (RepositoryException e) {
            LOGGER.error("Fail while adding lock infiniteTranslationLock");
        }
    }
}
