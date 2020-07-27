package org.jahia.translation.globallink.choicelist;

import org.apache.commons.lang.StringUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.jahia.services.content.nodetypes.ExtendedPropertyDefinition;
import org.jahia.services.content.nodetypes.initializers.ChoiceListValue;
import org.jahia.services.content.nodetypes.initializers.ModuleChoiceListInitializer;
import org.jahia.translation.globallink.common.GlobalLinkConstants;
import org.jahia.utils.LanguageCodeConverters;

import javax.jcr.RepositoryException;
import javax.jcr.Value;
import java.util.*;

/**
 * Created by rincevent on 2016-07-21.
 */
public class DisplayLocaleNameChoicelistInitializer implements ModuleChoiceListInitializer {
    @Override
    public String getKey() {
        return "displayLocaleName";
    }

    @Override
    public void setKey(String s) {
    }

    @Override
    public List<ChoiceListValue> getChoiceListValues(ExtendedPropertyDefinition extendedPropertyDefinition, String s, List<ChoiceListValue> list, Locale userLocale, Map<String, Object> map) {
        List<ChoiceListValue> results = new ArrayList<>();
        try {
            JCRNodeWrapper nodeWrapper = (JCRNodeWrapper) map.get("contextParent");
            JCRSiteNode resolveSite = nodeWrapper.getResolveSite();
            if (list != null && !list.isEmpty()) {
                if (resolveSite.isNodeType(GlobalLinkConstants.GBL_MIXIN_TYPE) && resolveSite.getProperty(GlobalLinkConstants.GBL_PROPERTY_ENABLE).getBoolean()) {
                    Value[] mappings = resolveSite.getProperty("j:languageMappings").getValues();
                    for (ChoiceListValue choiceListValue : list) {
                        //Get the language code by removing -source or -target
                        String languageCode = StringUtils.substringBefore(choiceListValue.getDisplayName(), "-gblSource");
                        List<String> sourceMapping = new LinkedList<>();
                        for (Value mapping : mappings) {
                            String mappingString = mapping.getString();
                            if(mappingString.endsWith(languageCode)) {
                                sourceMapping.add(mappingString);
                            }
                        }
                        addResults(userLocale, results, sourceMapping);
                    }
                }
            } else if (map.containsKey("sourceLanguage")) {
                String sourceLanguage = ((String) ((List) map.get("sourceLanguage")).get(0)).split("###")[1];
                JCRNodeWrapper sourceLanguageNode = resolveSite.getNode(sourceLanguage + "-gblSource");
                Value[] mappings = resolveSite.getProperty("j:languageMappings").getValues();
                for (Value targetLanguages : sourceLanguageNode.getProperty("targetLanguages").getValues()) {
                    String languageCode = targetLanguages.getString();
                    List<String> sourceMapping = new LinkedList<>();
                    for (Value mapping : mappings) {
                        String mappingString = mapping.getString();
                        if(mappingString.endsWith(languageCode)) {
                            sourceMapping.add(mappingString);
                        }
                    }
                    addResults(userLocale, results, sourceMapping);
                }
            } else {
                results.add(new ChoiceListValue("Please select a source language first", "empty"));
            }
        } catch (RepositoryException e) {
            e.printStackTrace();
        }
        return results;
    }

    private void addResults(Locale userLocale, List<ChoiceListValue> results, List<String> sourceMapping) {
        for (String s1 : sourceMapping) {
            String[] locales = s1.split("###");
            // Render the locale object in the current user language
            ChoiceListValue value = new ChoiceListValue();
            value.setDisplayName(LanguageCodeConverters.getLocaleFromCode(locales[0]).getDisplayName(userLocale)+" <---> "+Locale.forLanguageTag(locales[1]).getDisplayName(userLocale));
            value.setStringValue(s1);
            results.add(value);
        }
    }
}
