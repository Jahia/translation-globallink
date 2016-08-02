package org.jahia.translation.globallink.choicelist;

import org.apache.commons.lang.StringUtils;
import org.jahia.services.content.nodetypes.ExtendedPropertyDefinition;
import org.jahia.services.content.nodetypes.initializers.ChoiceListValue;
import org.jahia.services.content.nodetypes.initializers.ModuleChoiceListInitializer;
import org.jahia.utils.LanguageCodeConverters;

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
        for (ChoiceListValue choiceListValue : list) {
            //Get the language code by removing -source or -target
            String languageCode = StringUtils.substringBefore(choiceListValue.getDisplayName(), "-");
            // get the locale object form the code
            Locale localeFromCode = LanguageCodeConverters.getLocaleFromCode(languageCode);
            // Render the locale object in the current user language
            choiceListValue.setDisplayName(localeFromCode.getDisplayName(userLocale));
            choiceListValue.setStringValue(languageCode);
        }
        return list;
    }
}
