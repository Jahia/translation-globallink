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
 *  *     Copyright (C) 2002-2021 Jahia Solutions Group. All rights reserved.
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

import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.translation.globallink.dto.GlobalLinkProjectRequestDTO;
import org.jahia.translation.globallink.exception.GlobalLinkServiceException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.List;

/**
 * Service to perform document related operations.
 *
 * @author Prince.Arora, WebItUp.
 */
public interface GlobalLinkDocumentService {

    /**
     * Save the document in a given location with given filename
     *
     * @param document
     * @param path
     * @param fileName
     * @return
     * @throws GlobalLinkServiceException
     */
    boolean saveDocument(Document document, String path, String fileName) throws GlobalLinkServiceException;

    /**
     * Prepare an XML Document for all big text nodes under a project.
     *
     * @param project
     * @return
     * @throws GlobalLinkServiceException
     */
    boolean createDocumentForProject(GlobalLinkProjectRequestDTO project, JCRNodeWrapper pageNode, JCRNodeWrapper requestNode,
                                     List<String> componentList, JCRSessionWrapper sessionWrapper) throws GlobalLinkServiceException;

    /**
     * Read translated document and get list of translated content nodes.
     *
     * @param documentFile
     * @return
     * @throws GlobalLinkServiceException
     */
    NodeList getTranslatedContentList(File documentFile) throws GlobalLinkServiceException;
}
