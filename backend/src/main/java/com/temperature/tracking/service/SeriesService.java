package com.temperature.tracking.service;

import com.temperature.tracking.dto.SeriesRequest;
import com.temperature.tracking.dto.SeriesResponse;
import com.temperature.tracking.entity.Series;
import com.temperature.tracking.entity.User;
import com.temperature.tracking.exception.ResourceNotFoundException;
import com.temperature.tracking.repository.SeriesRepository;
import com.temperature.tracking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SeriesService {

    private final SeriesRepository seriesRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<SeriesResponse> getAllSeries() {
        return seriesRepository.findAll().stream()
                .map(SeriesResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SeriesResponse getSeriesById(Integer id) {
        Series series = seriesRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Series not found with id: " + id));
        return SeriesResponse.fromEntity(series);
    }

    @Transactional
    public SeriesResponse createSeries(SeriesRequest request, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        Series series = new Series();
        series.setName(request.getName());
        series.setDescription(request.getDescription());
        series.setColor(request.getColor());
        series.setIcon(request.getIcon());
        series.setMinValue(request.getMinValue());
        series.setMaxValue(request.getMaxValue());
        series.setCreatedBy(user);

        Series savedSeries = seriesRepository.save(series);
        return SeriesResponse.fromEntity(savedSeries);
    }

    @Transactional
    public SeriesResponse updateSeries(Integer id, SeriesRequest request) {
        Series series = seriesRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Series not found with id: " + id));

        series.setName(request.getName());
        series.setDescription(request.getDescription());
        series.setColor(request.getColor());
        series.setIcon(request.getIcon());
        series.setMinValue(request.getMinValue());
        series.setMaxValue(request.getMaxValue());

        Series updatedSeries = seriesRepository.save(series);
        return SeriesResponse.fromEntity(updatedSeries);
    }

    @Transactional
    public void deleteSeries(Integer id) {
        Series series = seriesRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Series not found with id: " + id));
        seriesRepository.delete(series);
    }
}
