package org.jahia.translation.globallink.dto;

import org.jahia.services.content.JCRNodeWrapper;

import java.io.Serializable;
import java.util.List;

/**
 * Model for Global Link submission Request.
 *
 * @author Prince.Arora, WebitUp
 * @author Aashish.Kocchar, WebitUp
 */
public class GlobalLinkProjectRequestDTO implements Serializable {

    private static final long serialVersionUID = 7506550038361666995L;

    private JCRNodeWrapper nodeWrapper;
    private String sourceLanguage;
    private String[] desLanguages;
    private String uploadTicket;
    private String submitTicket;
    private boolean childIncluded;
    //TODO BACKLOG-13965
    //private List<Document> documents;
    private String fileFormat;
    private String requestId;
    private String documentpath;
    private boolean skipTranslated;

    public JCRNodeWrapper getNodeWrapper() {
        return nodeWrapper;
    }

    public void setNodeWrapper(JCRNodeWrapper nodeWrapper) {
        this.nodeWrapper = nodeWrapper;
    }

    public String getSourceLanguage() {
        return sourceLanguage;
    }

    public void setSourceLanguage(String sourceLanguage) {
        this.sourceLanguage = sourceLanguage;
    }

    public String[] getDesLanguages() {
        return desLanguages;
    }

    public void setDesLanguages(String[] desLanguages) {
        this.desLanguages = desLanguages;
    }

    public String getUploadTicket() {
        return uploadTicket;
    }

    public void setUploadTicket(String uploadTicket) {
        this.uploadTicket = uploadTicket;
    }

    public String getSubmitTicket() {
        return submitTicket;
    }

    public void setSubmitTicket(String submitTicket) {
        this.submitTicket = submitTicket;
    }

    public boolean isChildIncluded() {
        return childIncluded;
    }

    public void setChildIncluded(boolean childIncluded) {
        this.childIncluded = childIncluded;
    }

    /*public List<Document> getDocuments() {
        return documents;
    }

    public void setDocuments(List<Document> documents) {
        this.documents = documents;
    }*/

    public String getFileFormat() {
        return fileFormat;
    }

    public void setFileFormat(String fileFormat) {
        this.fileFormat = fileFormat;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getDocumentpath() {
        return documentpath;
    }

    public void setDocumentpath(String documentpath) {
        this.documentpath = documentpath;
    }

    public boolean isSkipTranslated() {
        return skipTranslated;
    }

    public void setSkipTranslated(boolean skipTranslated) {
        this.skipTranslated = skipTranslated;
    }
}

