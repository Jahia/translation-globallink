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
package org.jahia.translation.globallink.validation;

import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRPropertyWrapper;
import org.jahia.services.content.decorator.validation.JCRNodeValidator;
import org.slf4j.Logger;

import javax.jcr.RepositoryException;
import java.util.Calendar;

import static org.jahia.translation.globallink.common.GlobalLinkConstants.GBL_IS_CHILD_REQUEST;

/**
 * Created by rincevent on 2017-02-06.
 */
public class ProjectNodeValidator implements JCRNodeValidator {
    private static Logger logger = org.slf4j.LoggerFactory.getLogger(ProjectNodeValidator.class);
    private JCRNodeWrapper node;

    public ProjectNodeValidator(JCRNodeWrapper node) {
        this.node = node;
    }

    @FutureDueDate
    public Calendar getDueDate() {
        try {
            // Skip for sub pages requests
            if((node.hasProperty(GBL_IS_CHILD_REQUEST) && node.getProperty(GBL_IS_CHILD_REQUEST).getBoolean()) || !node.isNew()) {
                return null;
            }
            JCRPropertyWrapper property = node.getProperty("dueDate");
            if (property != null) {
                return property.getDate();
            }
        } catch (RepositoryException e) {
            logger.error(e.getMessage());
        }
        return null;
    }
}
