package com.temperature.tracking.validation;

import com.temperature.tracking.dto.SeriesRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator implementation for {@link ValidMinMaxRange}.
 * Validates that minValue is less than maxValue when both are provided.
 */
public class MinMaxRangeValidator implements ConstraintValidator<ValidMinMaxRange, SeriesRequest> {

    @Override
    public boolean isValid(SeriesRequest request, ConstraintValidatorContext context) {
        if (request == null) {
            return true;
        }

        // If both minValue and maxValue are null, validation passes
        if (request.getMinValue() == null || request.getMaxValue() == null) {
            return true;
        }

        // Check that minValue < maxValue
        return request.getMinValue().compareTo(request.getMaxValue()) < 0;
    }
}
