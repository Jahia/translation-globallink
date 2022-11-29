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
package org.jahia.translation.globallink.exception;

/**
 * Service Exception for Global link translation operations.
 *
 * @author Prince.Arora, WebItUp
 */
public class GlobalLinkServiceException extends RuntimeException {

    private static final long serialVersionUID = 7049630966775813325L;

    public GlobalLinkServiceException() {
    }

    public GlobalLinkServiceException(String message) {
        super(message);
    }

    public GlobalLinkServiceException(String message, Throwable throwable) {
        super(message, throwable);
    }

}
