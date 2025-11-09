package com.temperature.tracking.service;

import com.temperature.tracking.dto.MeasurementRequest;
import com.temperature.tracking.dto.MeasurementResponse;
import com.temperature.tracking.entity.Measurement;
import com.temperature.tracking.entity.Series;
import com.temperature.tracking.entity.User;
import com.temperature.tracking.exception.ResourceNotFoundException;
import com.temperature.tracking.repository.MeasurementRepository;
import com.temperature.tracking.repository.SeriesRepository;
import com.temperature.tracking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MeasurementService {

    private final MeasurementRepository measurementRepository;
    private final SeriesRepository seriesRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<MeasurementResponse> getAllMeasurements() {
        return measurementRepository.findAll().stream()
                .map(MeasurementResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MeasurementResponse getMeasurementById(Integer id) {
        Measurement measurement = measurementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Measurement not found with id: " + id));
        return MeasurementResponse.fromEntity(measurement);
    }

    @Transactional(readOnly = true)
    public List<MeasurementResponse> getMeasurementsBySeriesId(Integer seriesId) {
        return measurementRepository.findBySeriesId(seriesId).stream()
                .map(MeasurementResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public MeasurementResponse createMeasurement(MeasurementRequest request, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        Series series = seriesRepository.findById(request.getSeriesId())
                .orElseThrow(() -> new ResourceNotFoundException("Series not found with id: " + request.getSeriesId()));

        Measurement measurement = new Measurement();
        measurement.setSeries(series);
        measurement.setValue(request.getValue());
        measurement.setTimestamp(request.getTimestamp());
        measurement.setCreatedBy(user);

        Measurement savedMeasurement = measurementRepository.save(measurement);
        return MeasurementResponse.fromEntity(savedMeasurement);
    }

    @Transactional
    public MeasurementResponse updateMeasurement(Integer id, MeasurementRequest request, String username) {
        Measurement measurement = measurementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Measurement not found with id: " + id));

        if (request.getSeriesId() != null) {
            Series series = seriesRepository.findById(request.getSeriesId())
                    .orElseThrow(() -> new ResourceNotFoundException("Series not found with id: " + request.getSeriesId()));
            measurement.setSeries(series);
        }

        measurement.setValue(request.getValue());
        measurement.setTimestamp(request.getTimestamp());

        Measurement updatedMeasurement = measurementRepository.save(measurement);
        return MeasurementResponse.fromEntity(updatedMeasurement);
    }

    @Transactional
    public void deleteMeasurement(Integer id) {
        Measurement measurement = measurementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Measurement not found with id: " + id));
        measurementRepository.delete(measurement);
    }
}
