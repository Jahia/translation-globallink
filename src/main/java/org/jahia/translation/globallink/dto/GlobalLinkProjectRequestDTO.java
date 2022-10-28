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
package org.jahia.translation.globallink.dto;

import org.gs4tr.gcc.restclient.request.UploadFileRequest;
import org.jahia.services.content.JCRNodeWrapper;

import java.io.Serializable;
import java.util.ArrayList;
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
    private Long submissionId;
    private boolean childIncluded;
    private List<UploadFileRequest> uploadFileRequests;
    private String requestId;
    private String documentpath;
    private boolean skipTranslated;

    public GlobalLinkProjectRequestDTO() {
        uploadFileRequests = new ArrayList<>();
    }

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

    public Long getSubmissionId() {
        return submissionId;
    }

    public void setSubmissionId(Long submissionId) {
        this.submissionId = submissionId;
    }

    public boolean isChildIncluded() {
        return childIncluded;
    }

    public void setChildIncluded(boolean childIncluded) {
        this.childIncluded = childIncluded;
    }

    public List<UploadFileRequest> getUploadFileRequests() {
        return uploadFileRequests;
    }

    public void setUploadFileRequests(List<UploadFileRequest> uploadFileRequests) {
        this.uploadFileRequests = uploadFileRequests;
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

