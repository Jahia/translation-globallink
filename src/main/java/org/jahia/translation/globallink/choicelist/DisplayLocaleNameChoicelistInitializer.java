package org.jahia.translation.globallink.choicelist;

import org.apache.commons.lang.StringUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.jahia.services.content.nodetypes.ExtendedPropertyDefinition;
import org.jahia.services.content.nodetypes.initializers.ChoiceListValue;
import org.jahia.services.content.nodetypes.initializers.ModuleChoiceListInitializer;
import org.jahia.translation.globallink.common.GlobalLinkConstants;
import org.jahia.utils.LanguageCodeConverters;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Value;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Created by rincevent on 2016-07-21.
 */
@Component(service = ModuleChoiceListInitializer.class, immediate = true)
public class DisplayLocaleNameChoicelistInitializer implements ModuleChoiceListInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(DisplayLocaleNameChoicelistInitializer.class);
    public static final String SOURCE_LANGUAGE = "sourceLanguage";
    public static final String CONTEXT_NODE = "contextNode";

    @Override
    public String getKey() {
        return "displayLocaleName";
    }

    @Override
    public void setKey(String s) {
    }

    private JCRSiteNode getResolveSite(Map<String, Object> map) throws RepositoryException {
        return Optional.ofNullable(map.get("contextParent")).map(node -> (JCRNodeWrapper) node)
                .orElse((JCRNodeWrapper) map.get(CONTEXT_NODE)).getResolveSite();
    }

    @Override
    public List<ChoiceListValue> getChoiceListValues(ExtendedPropertyDefinition extendedPropertyDefinition, String s,
            List<ChoiceListValue> list, Locale userLocale, Map<String, Object> map) {
        List<ChoiceListValue> results = new ArrayList<>();
        try {
            JCRSiteNode resolveSite = getResolveSite(map);
            if (list != null && !list.isEmpty()) {
                if (resolveSite.isNodeType(GlobalLinkConstants.GBL_MIXIN_TYPE) && resolveSite
                        .getProperty(GlobalLinkConstants.GBL_PROPERTY_ENABLE).getBoolean()) {
                    Value[] mappings = resolveSite.getProperty("j:languageMappings").getValues();
                    for (ChoiceListValue choiceListValue : list) {
                        //Get the language code by removing -source or -target
                        String languageCode = StringUtils.substringBefore(choiceListValue.getDisplayName(), "-gblSource");
                        List<String> sourceMapping = new LinkedList<>();
                        for (Value mapping : mappings) {
                            String mappingString = mapping.getString();
                            if (mappingString.endsWith(languageCode)) {
                                sourceMapping.add(mappingString);
                            }
                        }
                        addResults(userLocale, results, sourceMapping);
                    }
                }
            } else if (map.containsKey(SOURCE_LANGUAGE) && map.get(SOURCE_LANGUAGE) != null) {
                String sourceLanguage = ((String) ((List) map.get(SOURCE_LANGUAGE)).get(0)).split("###")[1];
                addResults(userLocale, results, getTargetLanguagesMapping(sourceLanguage, resolveSite));
            } else if (map.get(CONTEXT_NODE) != null) {
                final JCRNodeWrapper contextNode = (JCRNodeWrapper) map.get(CONTEXT_NODE);
                if (contextNode.hasProperty(SOURCE_LANGUAGE)) {
                    addResults(userLocale, results,
                            getTargetLanguagesMapping(contextNode.getPropertyAsString(SOURCE_LANGUAGE).split("###")[1], resolveSite));
                } else {
                    results.add(new ChoiceListValue("Please select a source language first", "empty"));
                }
            } else {
                results.add(new ChoiceListValue("Please select a source language first", "empty"));
            }
        } catch (RepositoryException e) {
            LOGGER.error("Error while initializing language mapping", e);
        }
        return results;
    }

    private List<String> getTargetLanguagesMapping(String sourceLanguage, JCRSiteNode resolveSite) throws RepositoryException {
        List<String> sourceMapping = new LinkedList<>();
        JCRNodeWrapper sourceLanguageNode = resolveSite.getNode(sourceLanguage + "-gblSource");
        Value[] mappings = resolveSite.getProperty("j:languageMappings").getValues();
        for (Value targetLanguages : sourceLanguageNode.getProperty("targetLanguages").getValues()) {
            String languageCode = targetLanguages.getString();
            for (Value mapping : mappings) {
                String mappingString = mapping.getString();
                if (mappingString.endsWith(languageCode)) {
                    sourceMapping.add(mappingString);
                }
            }
        }
        return sourceMapping;
    }

    private void addResults(Locale userLocale, List<ChoiceListValue> results, List<String> sourceMapping) {
        for (String s1 : sourceMapping) {
            String[] locales = s1.split("###");
            // Render the locale object in the current user language
            ChoiceListValue value = new ChoiceListValue();
            value.setDisplayName(LanguageCodeConverters.getLocaleFromCode(locales[0]).getDisplayName(userLocale) + "/" + Locale
                    .forLanguageTag(locales[1]).getDisplayName(userLocale));
            value.setStringValue(s1);
            results.add(value);
        }
    }
}
