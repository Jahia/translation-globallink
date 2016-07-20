package org.jahia.translation.globallink.service.api;

import org.jahia.translation.globallink.dto.GlobalLinkConfigurationDTO;
import org.jahia.translation.globallink.exception.GlobalLinkServiceException;

import java.util.List;

/**
 * Service to process all translated content from saved documents
 *
 * @author Prince.Arora, WebItUp.
 * @author Aashish.Kocchar, WebitUp.
 */
public interface GlobalLinkTranslatedContentProcessService {

    /**
     * Process all translated documents and persist in Content Repository
     *
     * @throws GlobalLinkServiceException
     */
    void processTranslatedContent(List<GlobalLinkConfigurationDTO> configList);

}
