package org.jahia.translation.globallink.service.impl;

import org.apache.commons.lang.StringUtils;
import org.jahia.services.content.JCRNodeIteratorWrapper;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.nodetypes.ExtendedPropertyDefinition;
import org.jahia.translation.globallink.dto.GlobalLinkProjectRequestDTO;
import org.jahia.translation.globallink.exception.GlobalLinkServiceException;
import org.jahia.translation.globallink.service.api.GlobalLinkDocumentService;
import org.jahia.translation.globallink.service.api.SiteContentService;
import org.jahia.translation.globallink.util.GlobalLinkUtil;
import org.jahia.translation.globallink.util.IOUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
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
import java.util.List;

import static org.jahia.translation.globallink.common.GlobalLinkConstants.*;

/**
 * Implementation for document service
 *
 * @author Prince.Arora, WebItUp.
 */
@Component(service = GlobalLinkDocumentService.class, immediate = true)
public class GlobalLinkDocumentServiceImpl implements GlobalLinkDocumentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalLinkDocumentServiceImpl.class);

    private SiteContentService contentService;

    private int count = 0;

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean saveDocument(Document document, String path, String fileName) {
        try {
            IOUtil.createDirectories(FileSystems.getDefault().getPath(path));
            String documentPath = path + File.separator + fileName;
            return IOUtil.createFile(documentPath);
        } catch (Exception ex) {
            throw new GlobalLinkServiceException(ex.getMessage(), ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean createDocumentForProject(GlobalLinkProjectRequestDTO project, JCRNodeWrapper pageNode,
                                            JCRNodeWrapper requestNode, List<String> componentList,
                                            JCRSessionWrapper sessionWrapper){
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            docFactory.setValidating(false);
            docFactory.setNamespaceAware(false);
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document document = docBuilder.newDocument();
            Element rootElement = document.createElement(DOCUMENT_ROOT_NODE);
            String sourceLanguage = StringUtils.substringBefore(project.getSourceLanguage(),"###");
            this.processContentNodeForDocument(pageNode, componentList, document, rootElement,
                    sourceLanguage, sessionWrapper, project.isSkipTranslated(), !project.isChildIncluded());

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
        } catch (ParserConfigurationException | TransformerException | DOMException ex) {
            throw new GlobalLinkServiceException(ex.getMessage(), ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeList getTranslatedContentList(File documentFile) {
        try {
            DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document document = documentBuilder.parse(documentFile);
            return document.getElementsByTagName(DOCUMENT_CONTENT_NODE);
        } catch (ParserConfigurationException | SAXException | IOException ex) {
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
                                               Element rootElement, String locale, JCRSessionWrapper sessionWrapper, boolean skipTranslated, boolean skipSubPages) {
        try {
            if (componentList.contains(nodeWrapper.getPrimaryNodeTypeName())) {
                Element contentElement = document.createElement(DOCUMENT_CONTENT_NODE);
                contentElement.setAttribute(DOCUMENT_CONTENT_PROP_UUID, nodeWrapper.getIdentifier());
                if (nodeWrapper.hasNode(NODE_TRANSLATE_PREFIX + locale)) {
                    JCRNodeWrapper translationNode = nodeWrapper.getNode(NODE_TRANSLATE_PREFIX + locale);
                    if ((skipTranslated && !translationNode.hasProperty(NODE_NAME_TRANS_PROP)) ||
                            (skipTranslated && !translationNode.getProperty(NODE_NAME_TRANS_PROP).getBoolean()) || !skipTranslated) {
                        this.contentService.lockTranslationNode(translationNode, sessionWrapper);
                        PropertyIterator properties = nodeWrapper.getNode(NODE_TRANSLATE_PREFIX + locale).getProperties();
                        while (properties.hasNext()) {
                            Property property = (Property) properties.next();
                            try {
                                ExtendedPropertyDefinition propertyDefinition =
                                        nodeWrapper.getApplicablePropertyDefinition(property.getName());
                                if (propertyDefinition != null && propertyDefinition.isInternationalized() && !propertyDefinition.isProtected() && propertyDefinition.getRequiredType() == PropertyType.STRING) {
                                    String propertyName = property.getName();
                                    if (propertyName.contains(":")) {
                                        // change by cedric
                                        propertyName = propertyName.replace(":", "_");
                                    }
                                    Element propertyElement = document.createElement(propertyName);
                                    if (propertyDefinition.isMultiple()) {
                                        Value[] values = property.getValues();
                                        for (Value value : values) {
                                            try {
                                                Element textElement = document.createElement(DOCUMENT_CONTENT_PROP_TEXT);
                                                textElement.setAttribute(DOCUMENT_CONTENT_PROP_TRAS, "yes");
                                                textElement.setTextContent(value.getString());
                                                propertyElement.appendChild(textElement);
                                            } catch (RepositoryException re) {
                                                LOGGER.error("Error while getting value -> ", re);
                                            }
                                        }
                                    } else {
                                        Element textElement = document.createElement(DOCUMENT_CONTENT_PROP_TEXT);
                                        textElement.setAttribute(DOCUMENT_CONTENT_PROP_TRAS, "yes");
                                        textElement.setTextContent(property.getValue().getString());
                                        propertyElement.appendChild(textElement);
                                    }
                                    contentElement.appendChild(propertyElement);
                                }
                            } catch (RepositoryException re) {
                                LOGGER.error("Exception while checking property -> {} Exception {} ", property, re);
                            }
                        }
                        this.count++;
                        rootElement.appendChild(contentElement);
                    }
                }
            }
            if (nodeWrapper.hasNodes()) {
                JCRNodeIteratorWrapper nodes = nodeWrapper.getNodes();
                while (nodes.hasNext()) {
                    JCRNodeWrapper node = (JCRNodeWrapper) nodes.next();
                    try {
                        if(!(skipSubPages && node.isNodeType("jnt:page"))) {
                            processContentNodeForDocument(node, componentList, document, rootElement, locale,
                                    sessionWrapper, skipTranslated, skipSubPages);
                        }
                    } catch (RepositoryException e) {
                        LOGGER.error("Error while processing content node {} exception {}", nodeWrapper, e);
                    }
                }
            }
        } catch (RepositoryException re) {
            LOGGER.error("Error while processing content node {} exception {}", nodeWrapper, re);
        }
    }

    @Reference
    public void setContentService(SiteContentService contentService) {
        this.contentService = contentService;
    }
}
