package com.temperature.tracking.repository;

import com.temperature.tracking.entity.Series;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SeriesRepository extends JpaRepository<Series, Integer> {

    @EntityGraph(attributePaths = {"createdBy"})
    @Override
    Optional<Series> findById(Integer id);

    @EntityGraph(attributePaths = {"createdBy"})
    @Override
    List<Series> findAll();

    List<Series> findByCreatedById(Integer userId);
}
