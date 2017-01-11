package org.jahia.translation.globallink.choicelist;

import org.apache.commons.lang.StringUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.nodetypes.ExtendedPropertyDefinition;
import org.jahia.services.content.nodetypes.initializers.ChoiceListValue;
import org.jahia.services.content.nodetypes.initializers.ModuleChoiceListInitializer;
import org.jahia.utils.LanguageCodeConverters;

import javax.jcr.RepositoryException;
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
        List<ChoiceListValue> results = new ArrayList<>(list.size());
        try {
            Locale contextLocale = ((JCRNodeWrapper) map.get("contextParent")).getSession().getLocale();
            for (ChoiceListValue choiceListValue : list) {
                //Get the language code by removing -source or -target
                String languageCode = StringUtils.substringBefore(choiceListValue.getDisplayName(), "-");
                // get the locale object form the code
                Locale localeFromCode = LanguageCodeConverters.getLocaleFromCode(languageCode);
                if ((extendedPropertyDefinition.getName().equals("sourceLanguage") && contextLocale.getLanguage().equals(localeFromCode.getLanguage())) ||
                        (extendedPropertyDefinition.getName().equals("targetLanguage") && !contextLocale.getLanguage().equals(localeFromCode.getLanguage()))) {
                    // Render the locale object in the current user language
                    choiceListValue.setDisplayName(localeFromCode.getDisplayName(userLocale));
                    choiceListValue.setStringValue(languageCode);
                    results.add(choiceListValue);
                }
            }
        } catch (RepositoryException e) {
            e.printStackTrace();
        }
        return results;
    }
}
