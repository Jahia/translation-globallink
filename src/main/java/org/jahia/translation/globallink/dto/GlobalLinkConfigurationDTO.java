package org.jahia.translation.globallink.dto;

import org.jahia.services.content.decorator.JCRSiteNode;

import java.io.Serializable;
import java.util.List;

/**
 * Model class for Global Link global configuration.
 *
 * @author Aashish.Kocchar, WebitUp.
 */
public class GlobalLinkConfigurationDTO implements Serializable {

    private static final long serialVersionUID = 6423569512504343234L;

    private boolean isEnable;
    private String username;
    private String password;
    private String url;
    private String userAgent;
    private String projectName;
    private String submissionPrefix;
    private JCRSiteNode siteNode;
    private String fileFormat;
    private String documentPath;
    private List<String> componentList;

    public boolean isEnable() {
        return isEnable;
    }

    public void setEnable(boolean isEnable) {
        this.isEnable = isEnable;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getSubmissionPrefix() {
        return submissionPrefix;
    }

    public void setSubmissionPrefix(String submissionPrefix) {
        this.submissionPrefix = submissionPrefix;
    }

    public JCRSiteNode getSiteNode() {
        return siteNode;
    }

    public void setSiteNode(JCRSiteNode siteNode) {
        this.siteNode = siteNode;
    }

    public String getFileFormat() {
        return fileFormat;
    }

    public void setFileFormat(String fileFormat) {
        this.fileFormat = fileFormat;
    }

    public String getDocumentPath() {
        return documentPath;
    }

    public void setDocumentPath(String documentPath) {
        this.documentPath = documentPath;
    }

    public List<String> getComponentList() {
        return componentList;
    }

    public void setComponentList(List<String> componentList) {
        this.componentList = componentList;
    }

    public String toString() {
        return "Project -> " + this.projectName + " For User -> " + this.username +
                " With User Agent -> " + this.userAgent + " And URL -> " + this.url;
    }
}
