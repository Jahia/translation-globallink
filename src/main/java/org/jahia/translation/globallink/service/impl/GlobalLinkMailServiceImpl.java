package org.jahia.translation.globallink.service.impl;

import org.apache.commons.lang.StringUtils;
import org.apache.velocity.tools.generic.DateTool;
import org.jahia.bin.Jahia;
import org.jahia.data.viewhelper.principal.PrincipalViewHelper;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.jahia.services.content.decorator.JCRUserNode;
import org.jahia.services.mail.MailService;
import org.jahia.services.usermanager.JahiaUserManagerService;
import org.jahia.settings.SettingsBean;
import org.jahia.translation.globallink.exception.GlobalLinkServiceException;
import org.jahia.translation.globallink.service.api.GlobalLinkMailService;
import org.jahia.translation.globallink.util.JCRUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.script.ScriptException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.jahia.translation.globallink.common.GlobalLinkConstants.GBL_PROJECT_SOURCE_LANG;
import static org.jahia.translation.globallink.common.GlobalLinkConstants.GBL_PROJECT_TARGET_LANG;
import static org.jahia.translation.globallink.common.GlobalLinkConstants.GBL_PROJECT_TARGET_NODE;
import static org.jahia.translation.globallink.common.GlobalLinkConstants.JCR_DEFAULT_WS;
import static org.jahia.translation.globallink.common.SubmissionStatus.STATUS_CONTENT_ERROR;
import static org.jahia.translation.globallink.common.SubmissionStatus.STATUS_SUBMITTED;
import static org.jahia.translation.globallink.common.SubmissionStatus.STATUS_TRANSLATED;

/**
 * {@inheritDoc}
 */
@Component(service = GlobalLinkMailService.class, immediate = true)
public class GlobalLinkMailServiceImpl implements GlobalLinkMailService {

    public static final String SHORT = "short";
    public static final String DUE_DATE = "dueDate";
    public static final String J_EMAIL = "j:email";
    public static final String TRANSLATION_COMPLETE = "Translation complete";
    public static final String TRANSLATION_FAILED = "Translation failed";

    private MailService mailService;
    private JahiaUserManagerService userManagerService;

    @Reference
    public void setMailService(MailService mailService) {
        this.mailService = mailService;
    }

    @Reference
    public void setUserManagerService(JahiaUserManagerService userManagerService) {
        this.userManagerService = userManagerService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sendNotificationMail(JCRNodeWrapper requestNode, String submissionStatus) {
        try {
            JCRSessionWrapper sessionWrapper = JCRUtil.getRootSession(JCR_DEFAULT_WS);
            JCRUserNode user = userManagerService.lookupUser(requestNode.getCreationUser(), sessionWrapper);
            Map<String, Object> bindings = buildMailDataBinding(requestNode, user, submissionStatus);
            String mailRecipient = user.hasProperty(J_EMAIL) ? user.getPropertyAsString(J_EMAIL) : mailService.defaultRecipient();
           // TODO handle the user languages BACKLOG-14543
            mailService.sendMessageWithTemplate("/META-INF/mails/templates/notification.body.vm", bindings, mailRecipient,
                    mailService.getSettings().getFrom(), null, null, new Locale("en"),
                    "Jahia GlobalLink Translation Connector");
        } catch (RepositoryException | ScriptException ex) {
            throw new GlobalLinkServiceException("Error while sending notification mail", ex);
        }
    }

    private Map<String, Object> buildMailDataBinding(JCRNodeWrapper requestNode, JCRUserNode user, String submissionStatus)
            throws RepositoryException {
        Map<String, Object> bindings = new HashMap<>();
        DateTool dateTool = new DateTool();
        Locale userLocale = new Locale(user.getPropertyAsString("preferredLanguage"));

        JCRNodeWrapper targetNode = (JCRNodeWrapper) requestNode.getProperty(GBL_PROJECT_TARGET_NODE).getNode();
        bindings.put("PrincipalViewHelper", PrincipalViewHelper.class);
        bindings.put("user", user);
        bindings.put("date", new DateTool());
        bindings.put("requestNode", requestNode);
        bindings.put("locale", userLocale);
        JCRSiteNode siteNode = requestNode.getResolveSite();
        bindings.put("site", requestNode.getResolveSite());
        bindings.put("contextPath", Jahia.getContextPath());

        final int siteURLPortOverride = SettingsBean.getInstance().getSiteURLPortOverride();
        String servername = "http" + (siteURLPortOverride == 443 ? "s" : "") + "://" + siteNode.getServerName() + ((siteURLPortOverride != 0
                && siteURLPortOverride != 80 && siteURLPortOverride != 443) ? ":" + siteURLPortOverride : "");
        bindings.put("servername", servername);
        String jContentFolder = targetNode.isNodeType("jnt:page") ? "pages" : "content-folders";

        bindings.put("jContentPath",
                Jahia.getContextPath() + "/cms/contentmanager/" + siteNode.getName() + "/" + userLocale.getLanguage() + "/" + jContentFolder
                        + StringUtils.substringAfter(targetNode.getPath(), "/sites/" + siteNode.getName()));

        bindings.put("dashboardPath", Jahia.getContextPath() + "/cms/edit/default/" + userLocale.getLanguage()
                + "/sites/" + siteNode.getName() + ".globallink-translation-requests.html");

        bindings.put("imageName", getImageName(submissionStatus));
        bindings.put("headerStatus", getHeaderLabel(submissionStatus));
        Map<String, String> datas = new LinkedHashMap<>();
        datas.put("Due date", dateTool.format(SHORT, SHORT, requestNode.getProperty(DUE_DATE).getDate(), userLocale));
        datas.put("Submission name", requestNode.getPropertyAsString("name"));
        datas.put("Submission Id", requestNode.getPropertyAsString("submissionTicket"));
        datas.put("Submission Date", dateTool.format(SHORT, SHORT, requestNode.getProperty("jcr:lastModified").getDate(), userLocale));
        datas.put("Site name", siteNode.getDisplayableName());
        datas.put("Page name", targetNode.getDisplayableName());
        datas.put("Source language",
                StringUtils.substringBefore(requestNode.getPropertyAsString(GBL_PROJECT_SOURCE_LANG), "###").toUpperCase());

        List<String> allTargetLanguages = new ArrayList<>();
        for (Value targetLanguages : requestNode.getProperty(GBL_PROJECT_TARGET_LANG).getValues()) {
            allTargetLanguages.add(StringUtils.substringBefore(targetLanguages.getString(), "###"));
        }
        datas.put("Target language(s)", String.join(",", allTargetLanguages).toUpperCase());

        datas.put("Status", getStatusLabel(submissionStatus));
        datas.put("Content count", requestNode.getPropertyAsString("gblContentCount"));
        datas.put("Instructions", requestNode.getPropertyAsString("instructions"));

        bindings.put("datas", datas);

        return bindings;
    }

    private String getHeaderLabel(String submissionStatus) {
        String submissionLabel;
        switch (submissionStatus) {
            case STATUS_SUBMITTED:
                submissionLabel = "has been received by <span><span>translations.</span>com</span>";
                break;
            case STATUS_TRANSLATED:
                submissionLabel = TRANSLATION_COMPLETE;
                break;
            case STATUS_CONTENT_ERROR:
                submissionLabel = TRANSLATION_FAILED;
                break;
            default:
                submissionLabel = "";
        }
        return submissionLabel;
    }

    private String getStatusLabel(String submissionStatus) {
        String submissionLabel;
        switch (submissionStatus) {
            case STATUS_SUBMITTED:
                submissionLabel = "Received by <span><span>translations.</span>com</span>";
                break;
            case STATUS_TRANSLATED:
                submissionLabel = TRANSLATION_COMPLETE;
                break;
            case STATUS_CONTENT_ERROR:
                submissionLabel = TRANSLATION_FAILED;
                break;
            default:
                submissionLabel = "";
        }
        return submissionLabel;
    }

    private String getImageName(String submissionStatus) {
        String imageName;
        switch (submissionStatus) {
            case STATUS_SUBMITTED:
                imageName = "mail-submitted.png";
                break;
            case STATUS_TRANSLATED:
                imageName = "mail-translated.png";
                break;
            case STATUS_CONTENT_ERROR:
                imageName = "mail-error.png";
                break;
            default:
                imageName = "";
        }
        return imageName;
    }
}
