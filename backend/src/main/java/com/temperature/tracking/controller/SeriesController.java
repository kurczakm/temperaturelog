package com.temperature.tracking.controller;

import com.temperature.tracking.dto.SeriesRequest;
import com.temperature.tracking.dto.SeriesResponse;
import com.temperature.tracking.service.SeriesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/series")
@RequiredArgsConstructor
@Validated
public class SeriesController {

    private final SeriesService seriesService;

    @GetMapping
    public ResponseEntity<List<SeriesResponse>> getAllSeries() {
        List<SeriesResponse> series = seriesService.getAllSeries();
        return ResponseEntity.ok(series);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SeriesResponse> getSeriesById(@PathVariable Integer id) {
        SeriesResponse series = seriesService.getSeriesById(id);
        return ResponseEntity.ok(series);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SeriesResponse> createSeries(@Valid @RequestBody SeriesRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        SeriesResponse series = seriesService.createSeries(request, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(series);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SeriesResponse> updateSeries(
            @PathVariable Integer id,
            @Valid @RequestBody SeriesRequest request) {
        SeriesResponse series = seriesService.updateSeries(id, request);
        return ResponseEntity.ok(series);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteSeries(@PathVariable Integer id) {
        seriesService.deleteSeries(id);
        return ResponseEntity.noContent().build();
    }
}
