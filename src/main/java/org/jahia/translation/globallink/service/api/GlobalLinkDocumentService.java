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
