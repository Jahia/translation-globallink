package org.jahia.translation.globallink.service.api;

import org.jahia.translation.globallink.dto.GlobalLinkConfigurationDTO;

import java.util.List;

/**
 * Submission service for Global link translation project
 *
 * @author Rakesh.Kumar, WebItUp.
 */
public interface GlobalLinkSubmissionService {

    /**
     * Submitting new projects for global link translation under sites
     *
     * @return
     */
    List<GlobalLinkConfigurationDTO> submitSiteProjects();

}
