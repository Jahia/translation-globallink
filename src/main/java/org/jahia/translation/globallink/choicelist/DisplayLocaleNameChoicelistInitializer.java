package org.jahia.translation.globallink.choicelist;

import org.jahia.services.content.nodetypes.ExtendedPropertyDefinition;
import org.jahia.services.content.nodetypes.initializers.ChoiceListValue;
import org.jahia.services.content.nodetypes.initializers.ModuleChoiceListInitializer;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Created by rincevent on 2016-07-21.
 */
public class DisplayLocaleNameChoicelistInitializer implements ModuleChoiceListInitializer{
    @Override
    public void setKey(String s) {
    }

    @Override
    public String getKey() {
        return "displayLocaleName";
    }

    @Override
    public List<ChoiceListValue> getChoiceListValues(ExtendedPropertyDefinition extendedPropertyDefinition, String s, List<ChoiceListValue> list, Locale locale, Map<String, Object> map) {
        return list;
    }
}
