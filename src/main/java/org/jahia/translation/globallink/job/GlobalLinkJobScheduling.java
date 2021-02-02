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
 *  *     Copyright (C) 2002-2021 Jahia Solutions Group. All rights reserved.
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
package org.jahia.translation.globallink.job;

import org.jahia.services.scheduler.JobSchedulingBean;
import org.jahia.services.scheduler.SchedulerService;
import org.jahia.settings.SettingsBean;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.Scheduler;

import java.text.ParseException;

@Component(service = GlobalLinkJobScheduling.class, immediate = true)
public class GlobalLinkJobScheduling extends JobSchedulingBean {
    SchedulerService schedulerService;

    public GlobalLinkJobScheduling() throws ParseException {
        setRamJob(false);
        setJobDetail(new JobDetail("translationJob", Scheduler.DEFAULT_GROUP, GlobalLinkTranslationJob.class));
        setTrigger(new CronTrigger("TranslationJobTrigger", null, "15 0/1 * * * ?"));
    }

    @Activate
    public void activate() throws Exception {
        this.setSettingsBean(SettingsBean.getInstance());
        this.setSchedulerService(schedulerService);
        this.afterPropertiesSet();
    }

    @Deactivate
    public void deactivate() throws Exception {
        this.destroy();
    }

    @Reference
    public void wireSchedulerService(SchedulerService schedulerService) {
        this.schedulerService = schedulerService;
    }
}
