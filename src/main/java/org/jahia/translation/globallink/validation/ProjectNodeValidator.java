package org.jahia.translation.globallink.validation;

import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRPropertyWrapper;
import org.jahia.services.content.decorator.validation.JCRNodeValidator;
import org.slf4j.Logger;

import javax.jcr.RepositoryException;
import javax.validation.constraints.Future;
import java.util.Calendar;

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
