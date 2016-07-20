package org.jahia.translation.globallink.service.api;

import org.jahia.translation.globallink.dto.GlobalLinkConfigurationDTO;
import org.jahia.translation.globallink.exception.GlobalLinkServiceException;

import java.util.List;

/**
 * Document service to retrieve all translated documents from Global Link PD.
 *
 * @author Rakesh.Kumar, WebitUp.
 * @author Prince.Arora, WebItUp.
 */
public interface GlobalLinkRetrieveDocumentService {

    /**
     * Retrieving documents for all completed targets.
     *
     * @return
     * @throws GlobalLinkServiceException
     */
    List<GlobalLinkConfigurationDTO> retrieveCompletedProjects(List<GlobalLinkConfigurationDTO> configList);

}
