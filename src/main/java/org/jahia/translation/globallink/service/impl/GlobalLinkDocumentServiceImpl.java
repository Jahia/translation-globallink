package org.jahia.translation.globallink.service.impl;

import org.apache.commons.lang.StringUtils;
import org.jahia.services.content.JCRContentUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.nodetypes.ExtendedPropertyDefinition;
import org.jahia.translation.globallink.dto.GlobalLinkProjectRequestDTO;
import org.jahia.translation.globallink.exception.GlobalLinkServiceException;
import org.jahia.translation.globallink.service.api.GlobalLinkDocumentService;
import org.jahia.translation.globallink.service.api.SiteContentService;
import org.jahia.translation.globallink.util.GlobalLinkUtil;
import org.jahia.translation.globallink.util.IOUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.util.Arrays;
import java.util.List;

import static org.jahia.translation.globallink.common.GlobalLinkConstants.*;

/**
 * Implementation for document service
 *
 * @author Prince.Arora, WebItUp.
 */
public class GlobalLinkDocumentServiceImpl implements GlobalLinkDocumentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalLinkDocumentServiceImpl.class);

    private SiteContentService contentService;

    private int count = 0;

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean saveDocument(Document document, String path, String fileName) throws GlobalLinkServiceException {
        try {
            IOUtil.createDirectories(FileSystems.getDefault().getPath(path));
            String documentPath = path + File.separator + fileName;
            return IOUtil.createFile(documentPath);
        } catch (Exception ex) {
            LOGGER.error("Service Exception -> ", ex);
            throw new GlobalLinkServiceException(ex.getMessage(), ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean createDocumentForProject(GlobalLinkProjectRequestDTO project, JCRNodeWrapper pageNode,
                                            JCRNodeWrapper requestNode, List<String> componentList,
                                            JCRSessionWrapper sessionWrapper) throws GlobalLinkServiceException {
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            docFactory.setValidating(false);
            docFactory.setNamespaceAware(false);
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document document = docBuilder.newDocument();
            Element rootElement = document.createElement(DOCUMENT_ROOT_NODE);
            List<JCRNodeWrapper> contentListNodes = JCRContentUtils.getChildrenOfType(pageNode, NODE_TYPE_CONTENTLIST);
            String sourceLanguage = project.getSourceLanguage();
            if (!pageNode.getResolveSite().getLanguages().contains(sourceLanguage)) {
                sourceLanguage = StringUtils.substringBefore(sourceLanguage, "_");
                if (!pageNode.getResolveSite().getLanguages().contains(sourceLanguage)) {
                    throw new GlobalLinkServiceException("There is no language matching this source on this site");
                }
            }
            String finalSourceLanguage = sourceLanguage;
            this.processContentNodeForDocument(pageNode, componentList, document, rootElement,
                    finalSourceLanguage, sessionWrapper, project.isSkipTranslated());
            contentListNodes.forEach(childNode -> {

                this.processContentNodeForDocument(childNode, componentList, document, rootElement,
                        finalSourceLanguage, sessionWrapper, project.isSkipTranslated());
            });
            this.contentService.addContentCount(requestNode, sessionWrapper, count);
            if (this.count > 0) {
                document.appendChild(rootElement);
                File srcFile = new File(GlobalLinkUtil.getSourceDocumentPath(project, pageNode));
                TransformerFactory transformerFactory = TransformerFactory.newInstance();
                Transformer transformer = transformerFactory.newTransformer();
                DOMSource domSource = new DOMSource(document);

                StreamResult result = new StreamResult(srcFile);
                transformer.transform(domSource, result);
                this.count = 0;
                return true;
            }
            return false;
        } catch (ParserConfigurationException | TransformerException | DOMException | RepositoryException ex) {
            LOGGER.error("Service Exception -> ", ex);
            throw new GlobalLinkServiceException(ex.getMessage(), ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeList getTranslatedContentList(File documentFile) throws GlobalLinkServiceException {
        try {
            DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document document = documentBuilder.parse(documentFile);
            return document.getElementsByTagName(DOCUMENT_CONTENT_NODE);
        } catch (ParserConfigurationException | SAXException | IOException ex) {
            LOGGER.error("Service Exception -> ", ex);
            throw new GlobalLinkServiceException(ex.getMessage(), ex);
        }
    }

    /**
     * Process a content node for a page to match all the components listed
     * in configurations for translation and get i18n properties from these
     * content nodes to prepare xml document to submit for translation.
     *
     * @param nodeWrapper
     * @param componentList
     * @param document
     * @param rootElement
     * @param locale
     * @param sessionWrapper
     * @param skipTranslated
     */
    @SuppressWarnings("unchecked")
    private void processContentNodeForDocument(JCRNodeWrapper nodeWrapper, List<String> componentList, Document document,
                                               Element rootElement, String locale, JCRSessionWrapper sessionWrapper, boolean skipTranslated) {
        try {
            if (componentList.contains(nodeWrapper.getPrimaryNodeTypeName())) {
                Element contentElement = document.createElement(DOCUMENT_CONTENT_NODE);
                contentElement.setAttribute(DOCUMENT_CONTENT_PROP_UUID, nodeWrapper.getIdentifier());
                if (nodeWrapper.getNode(NODE_TRANSLATE_PREFIX + locale) != null) {
                    JCRNodeWrapper translationNode = nodeWrapper.getNode(NODE_TRANSLATE_PREFIX + locale);
                    if ((skipTranslated && !translationNode.hasProperty(NODE_NAME_TRANS_PROP)) ||
                            (skipTranslated && !translationNode.getProperty(NODE_NAME_TRANS_PROP).getBoolean()) || !skipTranslated) {
                        this.contentService.lockNode(translationNode, sessionWrapper);
                        nodeWrapper.getNode(NODE_TRANSLATE_PREFIX + locale).getProperties().forEachRemaining(property -> {
                            try {
                                ExtendedPropertyDefinition propertyDefinition =
                                        translationNode.getApplicablePropertyDefinition(((Property) property).getName());
                                if (propertyDefinition != null && propertyDefinition.isInternationalized()) {
                                    String propertyName = ((Property) property).getName();
                                    if (propertyName.contains(":")) {
                                        // change by cedric
                                        propertyName = propertyName.replace(":", "_");
                                    }
                                    Element propertyElement = document.createElement(propertyName);
                                    if (propertyDefinition.isMultiple()) {
                                        Value[] values = ((Property) property).getValues();
                                        Arrays.asList(values).forEach(value -> {
                                            try {
                                                Element textElement = document.createElement(DOCUMENT_CONTENT_PROP_TEXT);
                                                textElement.setAttribute(DOCUMENT_CONTENT_PROP_TRAS, "yes");
                                                textElement.setTextContent(value.getString());
                                                propertyElement.appendChild(textElement);
                                            } catch (RepositoryException re) {
                                                LOGGER.error("Error while getting value -> ", re);
                                            }
                                        });
                                    } else {
                                        Element textElement = document.createElement(DOCUMENT_CONTENT_PROP_TEXT);
                                        textElement.setAttribute(DOCUMENT_CONTENT_PROP_TRAS, "yes");
                                        textElement.setTextContent(((Property) property).getValue().getString());
                                        propertyElement.appendChild(textElement);
                                    }
                                    contentElement.appendChild(propertyElement);
                                }
                            } catch (RepositoryException re) {
                                LOGGER.error("Exception while checking property -> {} Exception {} ", property, re);
                            }
                        });
                        this.count++;
                        rootElement.appendChild(contentElement);
                    }
                }
            }
            if (nodeWrapper.hasNodes()) {
                nodeWrapper.getNodes().forEach(node -> {
                    processContentNodeForDocument(node, componentList, document, rootElement, locale,
                            sessionWrapper, skipTranslated);
                });
            }
        } catch (RepositoryException re) {
            LOGGER.error("Error while processing content node {} exception {}", nodeWrapper, re);
        }
    }

    public void setContentService(SiteContentService contentService) {
        this.contentService = contentService;
    }
}
