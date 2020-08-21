package org.jahia.translation.globallink.validation;

import org.jahia.services.content.decorator.validation.JCRNodeValidatorDefinition;
import org.osgi.service.component.annotations.Component;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("rawtypes")
@Component(service = JCRNodeValidatorDefinition.class, immediate = true)
public class GlobalLinkValidatorDefinition extends JCRNodeValidatorDefinition {
    private final Map<String, Class> validators;

    public GlobalLinkValidatorDefinition() {
        this.validators = new HashMap<>();
        validators.put("gblnt:globalLinkProject", ProjectNodeValidator.class);
    }

    @Override
    public Map<String, Class> getValidators() {
        return validators;
    }
}
