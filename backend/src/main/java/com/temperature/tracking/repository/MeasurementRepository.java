package com.temperature.tracking.repository;

import com.temperature.tracking.entity.Measurement;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MeasurementRepository extends JpaRepository<Measurement, Integer> {

    @EntityGraph(attributePaths = {"series", "createdBy"})
    @Override
    Optional<Measurement> findById(Integer id);

    @EntityGraph(attributePaths = {"series", "createdBy"})
    @Override
    List<Measurement> findAll();

    @EntityGraph(attributePaths = {"series", "createdBy"})
    List<Measurement> findBySeriesId(Integer seriesId);

    @EntityGraph(attributePaths = {"series", "createdBy"})
    List<Measurement> findByCreatedById(Integer userId);
}
