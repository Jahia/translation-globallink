package org.jahia.translation.globallink.service.api;

import java.util.List;

import org.jahia.translation.globallink.dto.GlobalLinkConfigurationDTO;

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
