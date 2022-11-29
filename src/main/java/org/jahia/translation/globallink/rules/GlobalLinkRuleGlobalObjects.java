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
package org.jahia.translation.globallink.rules;

import org.jahia.services.content.rules.ModuleGlobalObject;
import org.jahia.services.mail.MailService;
import org.jahia.services.usermanager.JahiaUserManagerService;
import org.jahia.translation.globallink.service.api.GlobalLinkQueryService;
import org.jahia.translation.globallink.service.api.SiteContentService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.util.HashMap;
import java.util.Map;

@Component(service = ModuleGlobalObject.class, immediate = true)
public class GlobalLinkRuleGlobalObjects extends ModuleGlobalObject {
    private GlobalLinkQueryService queryService;
    private SiteContentService contentService;
    private MailService mailService;
    private JahiaUserManagerService userManagerService;

    @Activate
    public void activate() {
        Map<String, Object> objects = new HashMap<>();
        objects.put("gblSubmissionService", new GlobalLinkSubmissionService(queryService, contentService, mailService, userManagerService));
        setGlobalRulesObject(objects);
    }

    @Reference
    public void setQueryService(GlobalLinkQueryService queryService) {
        this.queryService = queryService;
    }

    @Reference
    public void setContentService(SiteContentService contentService) {
        this.contentService = contentService;
    }

    @Reference
    public void setMailService(MailService mailService) {
        this.mailService = mailService;
    }

    @Reference
    public void setUserManagerService(JahiaUserManagerService userManagerService) {
        this.userManagerService = userManagerService;
    }
}
