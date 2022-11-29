/*
 * /*
 *  * ==========================================================================================
 *  * =                            JAHIA'S ENTERPRISE DISTRIBUTION                             =
 *  * ==========================================================================================
 *  *
 *  *                                  http://www.jahia.com
 *  *
 *  * JAHIA'S ENTERPRISE DISTRIBUTIONS LICENSING - IMPORTANT INFORMATION
 *  * ==========================================================================================
 *  *
 *  *     Copyright (C) 2002-2022 Jahia Solutions Group. All rights reserved.
 *  *
 *  *     This file is part of a Jahia's Enterprise Distribution.
 *  *
 *  *     Jahia's Enterprise Distributions must be used in accordance with the terms
 *  *     contained in the Jahia Solutions Group Terms & Conditions as well as
 *  *     the Jahia Sustainable Enterprise License (JSEL).
 *  *
 *  *     For questions regarding licensing, support, production usage...
 *  *     please contact our team at sales@jahia.com or go to http://www.jahia.com/license.
 *  *
 *  * ==========================================================================================
 *  */
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
