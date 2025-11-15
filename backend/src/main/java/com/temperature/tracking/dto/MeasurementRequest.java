package com.temperature.tracking.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MeasurementRequest {
    @NotNull(message = "Series ID is required")
    private Integer seriesId;

    @NotNull(message = "Value is required")
    @DecimalMin(value = "-999.99", message = "Value must be at least -999.99")
    @DecimalMax(value = "999.99", message = "Value must not exceed 999.99")
    private BigDecimal value;

    @NotNull(message = "Timestamp is required")
    @PastOrPresent(message = "Timestamp cannot be in the future")
    private ZonedDateTime timestamp;
}
