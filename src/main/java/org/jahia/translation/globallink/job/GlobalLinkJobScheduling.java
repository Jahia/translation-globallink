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
        setRamJob(true);
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
