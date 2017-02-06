package org.jahia.translation.globallink.choicelist;

import org.apache.commons.lang.StringUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.jahia.services.content.nodetypes.ExtendedPropertyDefinition;
import org.jahia.services.content.nodetypes.initializers.ChoiceListValue;
import org.jahia.services.content.nodetypes.initializers.ModuleChoiceListInitializer;
import org.jahia.translation.globallink.common.GlobalLinkConstants;

import javax.jcr.RepositoryException;
import javax.jcr.Value;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
            if (list != null) {
                if (resolveSite.isNodeType(GlobalLinkConstants.GBL_MIXIN_TYPE) && resolveSite.getProperty(GlobalLinkConstants.GBL_PROPERTY_ENABLE).getBoolean()) {
                    for (ChoiceListValue choiceListValue : list) {
                        //Get the language code by removing -source or -target
                        String languageCode = StringUtils.substringBefore(choiceListValue.getDisplayName(), "-gblSource");
                        // get the locale object form the code
                        Locale localeFromCode = Locale.forLanguageTag(languageCode);
                        // Render the locale object in the current user language
                        choiceListValue.setDisplayName(localeFromCode.getDisplayName(userLocale));
                        choiceListValue.setStringValue(languageCode);
                        results.add(choiceListValue);
                    }
                }
            } else if (map.containsKey("sourceLanguage")) {
                JCRNodeWrapper sourceLanguageNode = resolveSite.getNode(((List) map.get("sourceLanguage")).get(0) + "-gblSource");
                for (Value targetLanguages : sourceLanguageNode.getProperty("targetLanguages").getValues()) {
                    Locale locale = Locale.forLanguageTag(targetLanguages.getString());
                    results.add(new ChoiceListValue(locale.getDisplayName(userLocale), targetLanguages.getString()));
                }
            } else {
                results.add(new ChoiceListValue("Please select a source language first", "empty"));
            }
        } catch (RepositoryException e) {
            e.printStackTrace();
        }
        return results;
    }
}
