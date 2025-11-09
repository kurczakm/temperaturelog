package com.temperature.tracking.repository;

import com.temperature.tracking.entity.Measurement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MeasurementRepository extends JpaRepository<Measurement, Integer> {
    List<Measurement> findBySeriesId(Integer seriesId);
    List<Measurement> findByCreatedById(Integer userId);
}
