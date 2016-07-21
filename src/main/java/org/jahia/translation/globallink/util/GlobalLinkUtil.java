package org.jahia.translation.globallink.util;

import static org.jahia.translation.globallink.common.GlobalLinkConstants.DOCUMENT_PATH;

import java.io.File;
import java.nio.file.FileSystems;
import java.util.*;

import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NoSuchNodeTypeException;

import org.apache.commons.lang.StringUtils;
import org.jahia.services.content.JCRContentUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRValueWrapper;
import org.jahia.services.content.nodetypes.ExtendedNodeType;
import org.jahia.services.content.nodetypes.ExtendedPropertyDefinition;
import org.jahia.services.content.nodetypes.NodeTypeRegistry;
import org.jahia.translation.globallink.dto.GlobalLinkConfigurationDTO;
import org.jahia.translation.globallink.dto.GlobalLinkProjectRequestDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.globallink.api.GLExchange;
import com.globallink.api.config.ProjectDirectorConfig;

/**
 * Utility class for common methods.
 *
 * @author Prince.Arora, WebItUp.
 * @author Rakesh.Kumar, WebitUp.
 */
public class GlobalLinkUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalLinkUtil.class);

    /**
     * Get locale to use in jahia.
     *
     * @param fileName
     * @return
     */
    public static String getTargetLocale(String fileName) {
        String locale = StringUtils.substringBefore(fileName, "_");
        return locale;
    }

    /**
     * Get Full locale code with region code.
     *
     * @param locale
     * @return
     */
    public static String getFullLocale(String locale) {
        if (locale.equals("en")) {
            return "en-US";
        } else if (!locale.contains("_")) {
            return locale + "-" + locale.toUpperCase();
        } else {
            return locale.replace("_", "-");
        }
    }

    public static String getShortLocale(String locale) {
        if (locale.equals("en-US")) {
            return "en";
        } else if (locale.contains("-")) {
            if (StringUtils.substringAfter(locale, "-").equals(StringUtils.substringBefore(locale, "-").toUpperCase())) {
                return StringUtils.substringBefore(locale, "-");
            } else {
                return locale.replace("-", "_");
            }
        }
        return locale;
    }

    /**
     * Get Global link PD Client.
     *
     * @param config
     * @return
     */
    public static GLExchange getGLExchangeClient(GlobalLinkConfigurationDTO config) {
        // Creating configuration for global link
        ProjectDirectorConfig gblConfig = new ProjectDirectorConfig();
        gblConfig.setUrl(config.getUrl());
        gblConfig.setUsername(config.getUsername());
        gblConfig.setPassword(config.getPassword());
        gblConfig.setUserAgent(config.getUserAgent());
        try {
            GLExchange glExchange = new GLExchange(gblConfig);
            return glExchange;
        } catch (Exception ex) {
            LOGGER.error("Error while generating GLExchange client: ", ex);
        }
        return null;
    }

    public static String getSourceDocumentPath(GlobalLinkProjectRequestDTO requestDTO, JCRNodeWrapper pageNode) {
        try {
            String docPath = DOCUMENT_PATH;
            if (requestDTO.getDocumentpath() != null && !requestDTO.getDocumentpath().equals("")) {
                docPath = requestDTO.getDocumentpath();
            }
            IOUtil.createDirectories(FileSystems.getDefault().getPath(docPath) + File.separator + requestDTO.getRequestId());
            return docPath + File.separator + requestDTO.getRequestId() + File.separator
                    + requestDTO.getSourceLanguage() + "_" + pageNode.getIdentifier() + "." + requestDTO.getFileFormat();
        } catch (Exception ex) {
            LOGGER.error("Exception while preparing source file path -> ", ex);
        }
        return null;
    }

    /**
     * Reading global link translation config property file and getting
     * list of all components that should be excluded from translation
     * components list shown in global link translation settings page.
     *
     * @return
     */
    public static List<String> getExcludedComponents(JCRValueWrapper[] valueWrappers) {
        List<String> excludeList = new ArrayList<>();
        if (valueWrappers != null && valueWrappers.length > 0) {
            Arrays.asList(valueWrappers).forEach(jcrValueWrapper -> {
                try {
                    excludeList.add(StringUtils.substringAfter(jcrValueWrapper.getString(), "-"));
                } catch (RepositoryException e) {
                    LOGGER.error("Cannot get string value for jcr value wrapper: {} exception {}", jcrValueWrapper, e);
                }
            });
            return excludeList;
        }
        return null;
    }

    public static Map<String, String> filterComponentsList(Map<String, String> components) {
        Map<String, String> filteredMap = new HashMap<>();
        components.forEach((key, value) -> {
            try {
                ExtendedNodeType extendedNodeType = NodeTypeRegistry.getInstance().getNodeType(key);
                ExtendedPropertyDefinition[] definitions = extendedNodeType.getDeclaredPropertyDefinitions();
                for (ExtendedPropertyDefinition definition : definitions) {
                    if (definition.isInternationalized()) {
                        filteredMap.put(key, value);
                        break;
                    }
                }
            } catch (NoSuchNodeTypeException e) {
                LOGGER.error("failed to check internationalized properties", e);
            }
        });
        return filteredMap;
    }
}
