package com.temperature.tracking.dto;

import com.temperature.tracking.validation.ValidMinMaxRange;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Data transfer object for creating or updating a Series.
 * Min and max values are optional and define validation bounds for measurements.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ValidMinMaxRange
public class SeriesRequest {
    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Color must be a valid hex color (e.g., #FF5733)")
    @Size(max = 20)
    private String color;

    @Size(max = 50, message = "Icon must not exceed 50 characters")
    private String icon;

    /**
     * Minimum allowed value for measurements in this series.
     * If null, no minimum limit is enforced.
     */
    @Digits(integer = 3, fraction = 2, message = "Min value must have at most 3 integer digits and 2 decimal places")
    private BigDecimal minValue;

    /**
     * Maximum allowed value for measurements in this series.
     * If null, no maximum limit is enforced.
     */
    @Digits(integer = 3, fraction = 2, message = "Max value must have at most 3 integer digits and 2 decimal places")
    private BigDecimal maxValue;
}
