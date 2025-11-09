package com.temperature.tracking.dto;

import com.temperature.tracking.entity.Measurement;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MeasurementResponse {
    private Integer id;
    private Integer seriesId;
    private String seriesName;
    private BigDecimal value;
    private LocalDateTime timestamp;
    private Integer createdBy;
    private String createdByUsername;
    private LocalDateTime createdAt;

    public static MeasurementResponse fromEntity(Measurement measurement) {
        return new MeasurementResponse(
                measurement.getId(),
                measurement.getSeries() != null ? measurement.getSeries().getId() : null,
                measurement.getSeries() != null ? measurement.getSeries().getName() : null,
                measurement.getValue(),
                measurement.getTimestamp(),
                measurement.getCreatedBy() != null ? measurement.getCreatedBy().getId() : null,
                measurement.getCreatedBy() != null ? measurement.getCreatedBy().getUsername() : null,
                measurement.getCreatedAt()
        );
    }
}
