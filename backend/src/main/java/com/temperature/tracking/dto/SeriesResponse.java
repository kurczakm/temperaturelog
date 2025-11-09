package com.temperature.tracking.dto;

import com.temperature.tracking.entity.Series;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeriesResponse {
    private Integer id;
    private String name;
    private String description;
    private String color;
    private Integer createdBy;
    private String createdByUsername;
    private LocalDateTime createdAt;

    public static SeriesResponse fromEntity(Series series) {
        return new SeriesResponse(
                series.getId(),
                series.getName(),
                series.getDescription(),
                series.getColor(),
                series.getCreatedBy() != null ? series.getCreatedBy().getId() : null,
                series.getCreatedBy() != null ? series.getCreatedBy().getUsername() : null,
                series.getCreatedAt()
        );
    }
}
