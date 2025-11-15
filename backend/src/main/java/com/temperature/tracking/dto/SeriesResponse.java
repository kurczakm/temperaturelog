package com.temperature.tracking.dto;

import com.temperature.tracking.entity.Series;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

/**
 * Data transfer object representing a Series in API responses.
 * Contains all series metadata including creator information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeriesResponse {
    private Integer id;
    private String name;
    private String description;
    private String color;
    private String icon;
    private BigDecimal minValue;
    private BigDecimal maxValue;
    private Integer createdBy;
    private String createdByUsername;
    private ZonedDateTime createdAt;

    public static SeriesResponse fromEntity(Series series) {
        return new SeriesResponse(
                series.getId(),
                series.getName(),
                series.getDescription(),
                series.getColor(),
                series.getIcon(),
                series.getMinValue(),
                series.getMaxValue(),
                series.getCreatedBy() != null ? series.getCreatedBy().getId() : null,
                series.getCreatedBy() != null ? series.getCreatedBy().getUsername() : null,
                series.getCreatedAt()
        );
    }
}
