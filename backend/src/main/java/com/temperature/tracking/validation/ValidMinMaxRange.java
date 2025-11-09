package com.temperature.tracking.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Custom validation annotation to ensure that minValue is less than maxValue.
 * Applied at the class level to validate cross-field constraints.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = MinMaxRangeValidator.class)
@Documented
public @interface ValidMinMaxRange {
    String message() default "Min value must be less than max value";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
