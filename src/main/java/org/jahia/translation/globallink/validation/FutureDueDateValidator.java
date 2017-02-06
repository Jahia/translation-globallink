package org.jahia.translation.globallink.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Calendar;

/**
 * Created by rincevent on 2017-02-06.
 */
public class FutureDueDateValidator implements ConstraintValidator<FutureDueDate, Calendar> {
    @Override
    public void initialize(FutureDueDate constraintAnnotation) {

    }

    @Override
    public boolean isValid(Calendar cal, ConstraintValidatorContext context) {
        //null values are valid
        if ( cal == null ) {
            return true;
        }
        Calendar instance = Calendar.getInstance();
        instance.add(Calendar.DAY_OF_YEAR,1);
        return cal.after( instance );
    }
}
