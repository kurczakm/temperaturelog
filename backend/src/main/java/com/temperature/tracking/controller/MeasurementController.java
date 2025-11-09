package com.temperature.tracking.controller;

import com.temperature.tracking.dto.MeasurementRequest;
import com.temperature.tracking.dto.MeasurementResponse;
import com.temperature.tracking.service.MeasurementService;
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
@RequestMapping("/api/measurements")
@RequiredArgsConstructor
@Validated
public class MeasurementController {

    private final MeasurementService measurementService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<MeasurementResponse>> getAllMeasurements() {
        List<MeasurementResponse> measurements = measurementService.getAllMeasurements();
        return ResponseEntity.ok(measurements);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<MeasurementResponse> getMeasurementById(@PathVariable Integer id) {
        MeasurementResponse measurement = measurementService.getMeasurementById(id);
        return ResponseEntity.ok(measurement);
    }

    @GetMapping("/series/{seriesId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<MeasurementResponse>> getMeasurementsBySeriesId(@PathVariable Integer seriesId) {
        List<MeasurementResponse> measurements = measurementService.getMeasurementsBySeriesId(seriesId);
        return ResponseEntity.ok(measurements);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MeasurementResponse> createMeasurement(@Valid @RequestBody MeasurementRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        MeasurementResponse measurement = measurementService.createMeasurement(request, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(measurement);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MeasurementResponse> updateMeasurement(
            @PathVariable Integer id,
            @Valid @RequestBody MeasurementRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        MeasurementResponse measurement = measurementService.updateMeasurement(id, request, username);
        return ResponseEntity.ok(measurement);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteMeasurement(@PathVariable Integer id) {
        measurementService.deleteMeasurement(id);
        return ResponseEntity.noContent().build();
    }
}
