package com.temperature.tracking.repository;

import com.temperature.tracking.entity.Series;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SeriesRepository extends JpaRepository<Series, Integer> {
    List<Series> findByCreatedById(Integer userId);
}
