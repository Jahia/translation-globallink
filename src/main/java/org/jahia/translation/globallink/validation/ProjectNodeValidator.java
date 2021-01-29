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
