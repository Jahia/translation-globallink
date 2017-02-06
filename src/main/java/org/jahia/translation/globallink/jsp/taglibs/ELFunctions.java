package org.jahia.translation.globallink.jsp.taglibs;

import com.globallink.api.GLExchange;
import com.globallink.api.model.LanguageDirection;
import com.globallink.api.model.Project;
import org.apache.commons.lang.StringUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRValueWrapper;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.jahia.services.render.scripting.Script;
import org.jahia.services.templates.ComponentRegistry;
import org.jahia.translation.globallink.common.GlobalLinkConstants;
import org.jahia.translation.globallink.dto.GlobalLinkConfigurationDTO;
import org.jahia.translation.globallink.util.GlobalLinkUtil;
import org.jahia.translation.globallink.util.JCRUtil;
import org.jahia.utils.LanguageCodeConverters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;


/**
 * Class for all ELFunctions for Global link translation jsp taglibs.
 *
 * @author Prince.Arora, Webitup
 */
public class ELFunctions {

    private static final Logger LOGGER = LoggerFactory.getLogger(ELFunctions.class);

    /**
     * Get list of all the components allowed in current site.
     *
     * @param nodeWrapper
     * @param locale
     * @param script
     * @return
     */
    public static Map<String, String> getComponentList(JCRNodeWrapper nodeWrapper, Locale locale, Script script,
                                                       JCRValueWrapper[] valueWrappers) {
        try {
            return GlobalLinkUtil.filterComponentsList(ComponentRegistry.getComponentTypes(nodeWrapper, null,
                    GlobalLinkUtil.getExcludedComponents(valueWrappers), locale));
        } catch (Exception ex) {
            LOGGER.error("Error while fetching components list -> ", ex);
        }
        return null;
    }

    /**
     * Check a global link configuration settings and populate language
     * directions from project directory.
     *
     * @param nodeWrapper
     * @return
     * @throws RepositoryException
     */
    public static String checkAndGetProjectInfo(JCRNodeWrapper nodeWrapper) throws RepositoryException {
        List<JCRSiteNode> siteNodeList = new ArrayList<>();
        String directionsString = "";
        if (nodeWrapper != null) {
            siteNodeList.add(nodeWrapper.getResolveSite());
            List<GlobalLinkConfigurationDTO> configList = JCRUtil.getConfigurationList(siteNodeList);
            GlobalLinkConfigurationDTO config = null;
            if (configList != null && configList.size() > 0) {
                config = configList.get(0);
                GLExchange glExchange = null;
                if (config != null) {
                    glExchange = GlobalLinkUtil.getGLExchangeClient(config);
                }
                if (glExchange != null) {
                    try {
                        Project project = glExchange.getProject(config.getProjectName());
                        LanguageDirection[] directions = project.getLanguageDirections();
                        for (LanguageDirection languageDirection : directions) {
                            if (directionsString.equals("")) {
                                directionsString = languageDirection.sourceLanguage + " -> " + languageDirection.targetLanguage;
                            } else {
                                directionsString = directionsString + ", " + languageDirection.sourceLanguage + " -> " +
                                        languageDirection.targetLanguage;
                            }
                        }
                    } catch (Exception e) {
                        directionsString = e.getLocalizedMessage();
                    }
                } else {
                    directionsString = "NA";
                }
            } else {
                directionsString = "NS";
            }
        }
        return directionsString;
    }

    public static JCRNodeWrapper getNodeFromId(String nodeId) throws RepositoryException {
        JCRSessionWrapper jcrSessionWrapper = JCRSessionFactory.getInstance()
                .getCurrentUserSession(GlobalLinkConstants.JCR_DEFAULT_WS);
        return jcrSessionWrapper.getNodeByUUID(nodeId).getNode("j:translation_en");
    }

    public static String displayLocale(String locale, Locale toDisplayNameWith) {
        return Locale.forLanguageTag(locale).getDisplayName(toDisplayNameWith);
    }
}
