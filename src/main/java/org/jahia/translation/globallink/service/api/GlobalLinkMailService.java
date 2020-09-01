package org.jahia.translation.globallink.service.api;

import org.jahia.services.content.JCRNodeWrapper;

/**
 * Service to perform mail related operations.
 */
public interface GlobalLinkMailService {

    /**
     * Send a mail notification for a request node, the mail content is related to the status of the request node
     *
     * @param requestNode node containing the informations for the mail
     * @param submissionStatus status of the submission
     */
    void sendNotificationMail(JCRNodeWrapper requestNode, String submissionStatus);
}
