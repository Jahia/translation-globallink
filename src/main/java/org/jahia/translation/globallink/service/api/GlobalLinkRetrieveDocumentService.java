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
 *  *     Copyright (C) 2002-2022 Jahia Solutions Group. All rights reserved.
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
     * @throws GlobalLinkServiceException
     */
    void retrieveCompletedProjects(List<GlobalLinkConfigurationDTO> configList);

}
