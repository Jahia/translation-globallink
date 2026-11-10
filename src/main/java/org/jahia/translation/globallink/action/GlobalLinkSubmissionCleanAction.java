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
package org.jahia.translation.globallink.action;

import org.jahia.api.Constants;
import org.jahia.bin.Action;
import org.jahia.bin.ActionResult;
import org.jahia.services.content.JCRCallback;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRTemplate;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;
import org.joda.time.DateTime;
import org.json.JSONObject;
import org.joda.time.MutableDateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.servlet.http.HttpServletRequest;
import java.text.MessageFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_OK;

/**
 * Created by rincevent on 2017-01-26.
 */
@Component(service = Action.class, immediate = true)
public class GlobalLinkSubmissionCleanAction extends Action {
    JCRTemplate jcrTemplate;

    @Reference
    public void setJcrTemplate(JCRTemplate jcrTemplate) {
        this.jcrTemplate = jcrTemplate;
    }

    public GlobalLinkSubmissionCleanAction() {
        this.setRequiredMethods("POST");
        this.setRequireAuthenticatedUser(true);
        this.setRequiredPermission("adminGlobalLinkTranslation");
    }

    /**
     * Deletes the translation requests of the site created more than {@code daysOld} days ago.
     * Answers {@code {"deleted": <count>}}, or 400 when {@code daysOld} is not a positive number.
     */
    @Override
    public ActionResult doExecute(HttpServletRequest req, final RenderContext renderContext, Resource resource, JCRSessionWrapper session, Map<String, List<String>> parameters, URLResolver urlResolver) throws Exception {
        final List<String> daysOld = parameters.get("daysOld");
        final int days;
        try {
            days = daysOld != null && !daysOld.isEmpty() ? Integer.parseInt(daysOld.get(0).trim()) : -1;
        } catch (NumberFormatException e) {
            return new ActionResult(SC_BAD_REQUEST, null, new JSONObject().put("error", "daysOld"));
        }
        if (days < 0) {
            return new ActionResult(SC_BAD_REQUEST, null, new JSONObject().put("error", "daysOld"));
        }
        // The site of the node the action is called on: renderContext.getSite() follows the server name
        final String sitePath = resource.getNode().getResolveSite().getPath();
        Integer deleted = jcrTemplate.doExecuteWithSystemSessionAsUser(getCurrentUser(), Constants.EDIT_WORKSPACE, Locale.ENGLISH, new JCRCallback<Integer>() {
            @Override
            public Integer doInJCR(JCRSessionWrapper systemSession) throws RepositoryException {
                String statement = "SELECT * FROM [gblnt:globalLinkProject] AS formResult WHERE isdescendantnode([''{0}'']) AND [jcr:created] <= CAST(''{1}'' AS DATE)";
                MutableDateTime dateTime = new DateTime(Calendar.getInstance()).minusDays(days).toMutableDateTime();
                dateTime.setMillisOfDay(0);
                String toString = dateTime.toString(ISODateTimeFormat.dateTime());
                Query query = systemSession.getWorkspace().getQueryManager().createQuery(MessageFormat.format(statement, sitePath, toString), Query.JCR_SQL2);
                NodeIterator nodes = query.execute().getNodes();
                int count = 0;
                while (nodes.hasNext()) {
                    JCRNodeWrapper node = (JCRNodeWrapper) nodes.nextNode();
                    node.remove();
                    count++;
                }
                systemSession.save();
                return count;
            }
        });
        return new ActionResult(SC_OK, null, new JSONObject().put("deleted", deleted));
    }
}
