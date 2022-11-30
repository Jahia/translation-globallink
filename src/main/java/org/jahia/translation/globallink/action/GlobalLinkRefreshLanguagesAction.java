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

import org.jahia.bin.Action;
import org.jahia.bin.ActionResult;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;
import org.json.JSONObject;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

import static javax.servlet.http.HttpServletResponse.SC_OK;

/**
 * Jahia Action to refresh the persisted languages direction related to a site configuration
 *
 * @author Aashish.Kocchar, WebitUp
 */
@Component(service = Action.class, immediate = true)
public class GlobalLinkRefreshLanguagesAction extends GlobalLinkConfigAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalLinkRefreshLanguagesAction.class);

    public GlobalLinkRefreshLanguagesAction() {
        this.setRequiredMethods("GET");
        this.setRequireAuthenticatedUser(true);
        this.setRequiredPermission("adminGlobalLinkTranslation");
    }

    @Override
    public ActionResult doExecute(HttpServletRequest request, RenderContext renderContext, Resource resource, JCRSessionWrapper jcrSessionWrapper, Map<String, List<String>> map, URLResolver urlResolver) throws Exception {
        ActionResult actionResult = new ActionResult(SC_OK, request.getRequestURL().toString(), new JSONObject());
        try {
            JCRSiteNode siteNode = renderContext.getSite();
            this.getLanguageMapping(siteNode);
            jcrSessionWrapper.save();
            actionResult.getJson().put("message", "gbl.settings.success");
        } catch (Exception ex) {
            LOGGER.error("Exception while saving GlobalLink settings!!", ex);
            actionResult.getJson().put("message", "gbl.settings.error");
        }
        return actionResult;
    }
}
