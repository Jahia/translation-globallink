package org.jahia.translation.globallink.common;

/**
 * Constants for Global Link Translation module.
 *
 * @author Prince.Arora, WebItUp
 */
public class GlobalLinkConstants {

    public static final String JCR_DEFAULT_WS = "default";

    public static final String JCR_LIVE_WS = "live";

    public static final String GBL_MIXIN_TYPE = "gblmix:globalLinkBaseSettings";

    public static final String GBL_PROPERTY_USERNAME = "j:globalLinkUsername";

    public static final String GBL_PROPERTY_ENABLE = "j:globalLinkActivated";

    public static final String GBL_PROPERTY_PASSWORD = "j:globalLinkPassword";

    public static final String GBL_PROPERTY_URL = "j:globalLinkUrl";

    public static final String GBL_PROPERTY_AGENT = "j:globalLinkUserAgent";

    public static final String GBL_PROPERTY_CONNECTOR_NAME = "j:globalLinkConnectorName";

    public static final String GBL_PROPERTY_PREFIX = "j:globalLinkSubmissionPrefix";

    public static final String GBL_PROPERTY_FORMAT = "j:globalLinkfileFormat";

    public static final String GBL_PROPERTY_COMPONENTS = "j:componentsList";

    public static final String GBL_PROPERTY_DOC_LOCATION = "j:globalLinkfileLocation";

    public static final String GBL_PROPERTY_INTERVAL = "j:globalLinkInterval";

    public static final String GBL_PROPERTY_LAST_EXEC = "j:globalLinkLastExec";

    public static final String GBL_PROPERTY_SOURCE_DIRECTIONS = "gblnt:globalLinkSourceLanguage";

    public static final String GBL_PROJECT_SOURCE_LANG = "sourceLanguage";

    public static final String GBL_PROJECT_TARGET_LANG = "targetLanguage";

    public static final String GBL_PROJECT_UPLOAD_TICKET = "uploadTicket";

    public static final String GBL_PROJECT_SUB_TICKET = "submissionTicket";

    public static final String GBL_PROJECT_REQUEST_ID = "gblRequestId";

    public static final String GBL_PROJECT_TARGET = "targetTicket";

    public static final String GBL_SUBMISSION_STATE = "gblSubmitState";

    public static final String GBL_PROJECT_CONTENT_COUNT = "gblContentCount";

    public static final String GBL_PROJECT_ERROR = "gblError";

    public static final String GBL_INCLUDE_CHILD = "includeChildPages";

    public static final String GBL_SKIP_TRANSLATED = "skipTranslated";

    public static final String NODE_TYPE_BIGTEXT = "jnt:bigText";

    public static final String NODE_TYPE_PROJECT = "gblnt:globalLinkProject";

    public static final String DOCUMENT_ROOT_NODE = "page";

    public static final String DOCUMENT_CONTENT_NODE = "content";

    public static final String DOCUMENT_CONTENT_PROP_UUID = "UUID";

    public static final String DOCUMENT_CONTENT_PROP_TRAS = "translatable";

    public static final String DOCUMENT_CONTENT_PROP_TEXT = "tcontent";

    public static final String NODE_TRANSLATE_PREFIX = "j:translation_";

    public static final String NODE_TRANLSATE_TYPE = "jnt:translation";

    public static final String NODE_MIXIN_TITLE = "mix:title";

    public static final String NODE_PROP_LANGUAGE = "jcr:language";

    public static final String DOCUMENT_PATH = System.getProperty("java.io.tmpdir") + "/gbl";

    public static final String TRANSLATED_PATH = "translated";

    public static final String NODE_TYPE_PAGE = "jnt:page";

    public static final String NODE_NAME_GLOBAL_LINK = "globallink-request";

    public static final String NODE_NAME_TRANS_PROP = "isTranslated";

    public static final String FILE_EXT_XML = ".xml";

    public static final String NODE_TYPE_CONTENTLIST = "jnt:contentList";
}
