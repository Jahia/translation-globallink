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
package org.jahia.translation.globallink.common;

/**
 * Constants for Submission related things.
 *
 * @author Prince.Arora, WebItUp
 */
public class SubmissionStatus {

    private SubmissionStatus() {
    }

    public static final String STATUS_SUBMITTED = "request.submitted";

    public static final String STATUS_RETRIEVED = "request.retrieved";

    public static final String STATUS_TRANSLATED = "request.translated";

    public static final String STATUS_CANCELLED = "request.cancelled";

    public static final String STATUS_DELETED = "request.deleted";

    public static final String STATUS_NO_DOCUMENT = "request.no.document";

    public static final String STATUS_CONTENT_ERROR = "request.content.error";

    public static final String STATUS_PRE_PROCESS = "Pre-process";

    public static final String STATUS_TRANSLATE = "Translate";

}
