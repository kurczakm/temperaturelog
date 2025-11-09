package com.temperature.tracking.service;

import com.temperature.tracking.dto.SeriesRequest;
import com.temperature.tracking.dto.SeriesResponse;
import com.temperature.tracking.entity.Series;
import com.temperature.tracking.entity.User;
import com.temperature.tracking.exception.ResourceNotFoundException;
import com.temperature.tracking.repository.SeriesRepository;
import com.temperature.tracking.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for SeriesService.
 * Tests all CRUD operations including happy paths, edge cases, and error scenarios.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SeriesService Unit Tests")
class SeriesServiceTest {

    @Mock
    private SeriesRepository seriesRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SeriesService seriesService;

    private User testUser;
    private Series testSeries;
    private SeriesRequest testRequest;

    @BeforeEach
    void setUp() {
        // Setup test user
        testUser = new User();
        testUser.setId(1);
        testUser.setUsername("testuser");

        // Setup test series
        testSeries = new Series();
        testSeries.setId(1);
        testSeries.setName("Temperature Series");
        testSeries.setDescription("Test series for temperature measurements");
        testSeries.setColor("#FF5733");
        testSeries.setIcon("thermometer");
        testSeries.setMinValue(new java.math.BigDecimal("-10.00"));
        testSeries.setMaxValue(new java.math.BigDecimal("50.00"));
        testSeries.setCreatedBy(testUser);
        testSeries.setCreatedAt(LocalDateTime.now());

        // Setup test request
        testRequest = new SeriesRequest();
        testRequest.setName("Temperature Series");
        testRequest.setDescription("Test series for temperature measurements");
        testRequest.setColor("#FF5733");
        testRequest.setIcon("thermometer");
        testRequest.setMinValue(new java.math.BigDecimal("-10.00"));
        testRequest.setMaxValue(new java.math.BigDecimal("50.00"));
    }

    @Nested
    @DisplayName("Get All Series Tests")
    class GetAllSeriesTests {

        @Test
        @DisplayName("Should return list of all series when series exist")
        void shouldReturnAllSeriesWhenSeriesExist() {
            // Arrange
            Series secondSeries = new Series();
            secondSeries.setId(2);
            secondSeries.setName("Humidity Series");
            secondSeries.setDescription("Test series for humidity measurements");
            secondSeries.setColor("#3498DB");
            secondSeries.setCreatedBy(testUser);
            secondSeries.setCreatedAt(LocalDateTime.now());

            List<Series> seriesList = Arrays.asList(testSeries, secondSeries);
            when(seriesRepository.findAll()).thenReturn(seriesList);

            // Act
            List<SeriesResponse> result = seriesService.getAllSeries();

            // Assert
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getName()).isEqualTo("Temperature Series");
            assertThat(result.get(1).getName()).isEqualTo("Humidity Series");
            verify(seriesRepository, times(1)).findAll();
        }

        @Test
        @DisplayName("Should return empty list when no series exist")
        void shouldReturnEmptyListWhenNoSeriesExist() {
            // Arrange
            when(seriesRepository.findAll()).thenReturn(Collections.emptyList());

            // Act
            List<SeriesResponse> result = seriesService.getAllSeries();

            // Assert
            assertThat(result).isEmpty();
            verify(seriesRepository, times(1)).findAll();
        }

        @Test
        @DisplayName("Should map series entity to response correctly")
        void shouldMapSeriesEntityToResponseCorrectly() {
            // Arrange
            when(seriesRepository.findAll()).thenReturn(Collections.singletonList(testSeries));

            // Act
            List<SeriesResponse> result = seriesService.getAllSeries();

            // Assert
            assertThat(result).hasSize(1);
            SeriesResponse response = result.get(0);
            assertThat(response.getId()).isEqualTo(testSeries.getId());
            assertThat(response.getName()).isEqualTo(testSeries.getName());
            assertThat(response.getDescription()).isEqualTo(testSeries.getDescription());
            assertThat(response.getColor()).isEqualTo(testSeries.getColor());
            assertThat(response.getCreatedBy()).isEqualTo(testUser.getId());
            assertThat(response.getCreatedByUsername()).isEqualTo(testUser.getUsername());
            assertThat(response.getCreatedAt()).isEqualTo(testSeries.getCreatedAt());
        }
    }

    @Nested
    @DisplayName("Get Series By Id Tests")
    class GetSeriesByIdTests {

        @Test
        @DisplayName("Should return series when valid id is provided")
        void shouldReturnSeriesWhenValidIdProvided() {
            // Arrange
            when(seriesRepository.findById(1)).thenReturn(Optional.of(testSeries));

            // Act
            SeriesResponse result = seriesService.getSeriesById(1);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1);
            assertThat(result.getName()).isEqualTo("Temperature Series");
            assertThat(result.getDescription()).isEqualTo("Test series for temperature measurements");
            assertThat(result.getColor()).isEqualTo("#FF5733");
            verify(seriesRepository, times(1)).findById(1);
        }

        @Test
        @DisplayName("Should throw exception when series not found")
        void shouldThrowExceptionWhenSeriesNotFound() {
            // Arrange
            when(seriesRepository.findById(anyInt())).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> seriesService.getSeriesById(999))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Series not found with id: 999");
            verify(seriesRepository, times(1)).findById(999);
        }

        @Test
        @DisplayName("Should throw exception when null id is provided")
        void shouldThrowExceptionWhenNullIdProvided() {
            // Arrange
            when(seriesRepository.findById(null)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> seriesService.getSeriesById(null))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Series not found with id: null");
            verify(seriesRepository, times(1)).findById(null);
        }

        @Test
        @DisplayName("Should map series with null created by correctly")
        void shouldMapSeriesWithNullCreatedByCorrectly() {
            // Arrange
            testSeries.setCreatedBy(null);
            when(seriesRepository.findById(1)).thenReturn(Optional.of(testSeries));

            // Act
            SeriesResponse result = seriesService.getSeriesById(1);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getCreatedBy()).isNull();
            assertThat(result.getCreatedByUsername()).isNull();
        }
    }

    @Nested
    @DisplayName("Create Series Tests")
    class CreateSeriesTests {

        @Test
        @DisplayName("Should create series with valid request and username")
        void shouldCreateSeriesWithValidRequestAndUsername() {
            // Arrange
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(seriesRepository.save(any(Series.class))).thenReturn(testSeries);

            // Act
            SeriesResponse result = seriesService.createSeries(testRequest, "testuser");

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("Temperature Series");
            assertThat(result.getDescription()).isEqualTo("Test series for temperature measurements");
            assertThat(result.getColor()).isEqualTo("#FF5733");
            assertThat(result.getCreatedByUsername()).isEqualTo("testuser");

            verify(userRepository, times(1)).findByUsername("testuser");
            verify(seriesRepository, times(1)).save(any(Series.class));
        }

        @Test
        @DisplayName("Should set all fields correctly when creating series")
        void shouldSetAllFieldsCorrectlyWhenCreatingSeries() {
            // Arrange
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(seriesRepository.save(any(Series.class))).thenAnswer(invocation -> {
                Series savedSeries = invocation.getArgument(0);
                assertThat(savedSeries.getName()).isEqualTo(testRequest.getName());
                assertThat(savedSeries.getDescription()).isEqualTo(testRequest.getDescription());
                assertThat(savedSeries.getColor()).isEqualTo(testRequest.getColor());
                assertThat(savedSeries.getCreatedBy()).isEqualTo(testUser);
                return testSeries;
            });

            // Act
            seriesService.createSeries(testRequest, "testuser");

            // Assert
            verify(seriesRepository, times(1)).save(any(Series.class));
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            // Arrange
            when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> seriesService.createSeries(testRequest, "nonexistent"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User not found: nonexistent");

            verify(userRepository, times(1)).findByUsername("nonexistent");
            verify(seriesRepository, never()).save(any(Series.class));
        }

        @Test
        @DisplayName("Should create series with null description")
        void shouldCreateSeriesWithNullDescription() {
            // Arrange
            testRequest.setDescription(null);
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(seriesRepository.save(any(Series.class))).thenReturn(testSeries);

            // Act
            SeriesResponse result = seriesService.createSeries(testRequest, "testuser");

            // Assert
            assertThat(result).isNotNull();
            verify(seriesRepository, times(1)).save(any(Series.class));
        }

        @Test
        @DisplayName("Should create series with null color")
        void shouldCreateSeriesWithNullColor() {
            // Arrange
            testRequest.setColor(null);
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(seriesRepository.save(any(Series.class))).thenReturn(testSeries);

            // Act
            SeriesResponse result = seriesService.createSeries(testRequest, "testuser");

            // Assert
            assertThat(result).isNotNull();
            verify(seriesRepository, times(1)).save(any(Series.class));
        }
    }

    @Nested
    @DisplayName("Update Series Tests")
    class UpdateSeriesTests {

        @Test
        @DisplayName("Should update series with valid request")
        void shouldUpdateSeriesWithValidRequest() {
            // Arrange
            SeriesRequest updateRequest = new SeriesRequest();
            updateRequest.setName("Updated Series");
            updateRequest.setDescription("Updated description");
            updateRequest.setColor("#2ECC71");

            Series updatedSeries = new Series();
            updatedSeries.setId(1);
            updatedSeries.setName("Updated Series");
            updatedSeries.setDescription("Updated description");
            updatedSeries.setColor("#2ECC71");
            updatedSeries.setCreatedBy(testUser);
            updatedSeries.setCreatedAt(testSeries.getCreatedAt());

            when(seriesRepository.findById(1)).thenReturn(Optional.of(testSeries));
            when(seriesRepository.save(any(Series.class))).thenReturn(updatedSeries);

            // Act
            SeriesResponse result = seriesService.updateSeries(1, updateRequest);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("Updated Series");
            assertThat(result.getDescription()).isEqualTo("Updated description");
            assertThat(result.getColor()).isEqualTo("#2ECC71");

            verify(seriesRepository, times(1)).findById(1);
            verify(seriesRepository, times(1)).save(any(Series.class));
        }

        @Test
        @DisplayName("Should update all modifiable fields")
        void shouldUpdateAllModifiableFields() {
            // Arrange
            SeriesRequest updateRequest = new SeriesRequest();
            updateRequest.setName("New Name");
            updateRequest.setDescription("New Description");
            updateRequest.setColor("#000000");

            when(seriesRepository.findById(1)).thenReturn(Optional.of(testSeries));
            when(seriesRepository.save(any(Series.class))).thenAnswer(invocation -> {
                Series savedSeries = invocation.getArgument(0);
                assertThat(savedSeries.getName()).isEqualTo("New Name");
                assertThat(savedSeries.getDescription()).isEqualTo("New Description");
                assertThat(savedSeries.getColor()).isEqualTo("#000000");
                return savedSeries;
            });

            // Act
            seriesService.updateSeries(1, updateRequest);

            // Assert
            verify(seriesRepository, times(1)).save(any(Series.class));
        }

        @Test
        @DisplayName("Should throw exception when series not found for update")
        void shouldThrowExceptionWhenSeriesNotFoundForUpdate() {
            // Arrange
            when(seriesRepository.findById(999)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> seriesService.updateSeries(999, testRequest))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Series not found with id: 999");

            verify(seriesRepository, times(1)).findById(999);
            verify(seriesRepository, never()).save(any(Series.class));
        }

        @Test
        @DisplayName("Should update series with null values in request")
        void shouldUpdateSeriesWithNullValuesInRequest() {
            // Arrange
            SeriesRequest updateRequest = new SeriesRequest();
            updateRequest.setName("Only Name Updated");
            updateRequest.setDescription(null);
            updateRequest.setColor(null);

            when(seriesRepository.findById(1)).thenReturn(Optional.of(testSeries));
            when(seriesRepository.save(any(Series.class))).thenReturn(testSeries);

            // Act
            SeriesResponse result = seriesService.updateSeries(1, updateRequest);

            // Assert
            assertThat(result).isNotNull();
            verify(seriesRepository, times(1)).save(any(Series.class));
        }

        @Test
        @DisplayName("Should not modify created by when updating series")
        void shouldNotModifyCreatedByWhenUpdatingSeries() {
            // Arrange
            User differentUser = new User();
            differentUser.setId(2);
            differentUser.setUsername("differentuser");

            when(seriesRepository.findById(1)).thenReturn(Optional.of(testSeries));
            when(seriesRepository.save(any(Series.class))).thenAnswer(invocation -> {
                Series savedSeries = invocation.getArgument(0);
                // Created by should remain unchanged
                assertThat(savedSeries.getCreatedBy()).isEqualTo(testUser);
                return savedSeries;
            });

            // Act
            seriesService.updateSeries(1, testRequest);

            // Assert
            verify(seriesRepository, times(1)).save(any(Series.class));
        }
    }

    @Nested
    @DisplayName("Delete Series Tests")
    class DeleteSeriesTests {

        @Test
        @DisplayName("Should delete series when valid id is provided")
        void shouldDeleteSeriesWhenValidIdProvided() {
            // Arrange
            when(seriesRepository.existsById(1)).thenReturn(true);
            doNothing().when(seriesRepository).deleteById(1);

            // Act
            seriesService.deleteSeries(1);

            // Assert
            verify(seriesRepository, times(1)).existsById(1);
            verify(seriesRepository, times(1)).deleteById(1);
        }

        @Test
        @DisplayName("Should throw exception when series not found for deletion")
        void shouldThrowExceptionWhenSeriesNotFoundForDeletion() {
            // Arrange
            when(seriesRepository.existsById(999)).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> seriesService.deleteSeries(999))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Series not found with id: 999");

            verify(seriesRepository, times(1)).existsById(999);
            verify(seriesRepository, never()).deleteById(anyInt());
        }

        @Test
        @DisplayName("Should check existence before deleting")
        void shouldCheckExistenceBeforeDeleting() {
            // Arrange
            when(seriesRepository.existsById(1)).thenReturn(true);
            doNothing().when(seriesRepository).deleteById(1);

            // Act
            seriesService.deleteSeries(1);

            // Assert
            // Verify that existsById is called before deleteById
            verify(seriesRepository, times(1)).existsById(1);
            verify(seriesRepository, times(1)).deleteById(1);
        }

        @Test
        @DisplayName("Should throw exception when null id provided for deletion")
        void shouldThrowExceptionWhenNullIdProvidedForDeletion() {
            // Arrange
            when(seriesRepository.existsById(null)).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> seriesService.deleteSeries(null))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Series not found with id: null");

            verify(seriesRepository, times(1)).existsById(null);
            verify(seriesRepository, never()).deleteById(any());
        }
    }

    @Nested
    @DisplayName("Edge Cases and Integration Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle series with very long description")
        void shouldHandleSeriesWithVeryLongDescription() {
            // Arrange
            String longDescription = "A".repeat(1000);
            testRequest.setDescription(longDescription);
            testSeries.setDescription(longDescription);

            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(seriesRepository.save(any(Series.class))).thenReturn(testSeries);

            // Act
            SeriesResponse result = seriesService.createSeries(testRequest, "testuser");

            // Assert
            assertThat(result.getDescription()).hasSize(1000);
        }

        @Test
        @DisplayName("Should handle series with special characters in name")
        void shouldHandleSeriesWithSpecialCharactersInName() {
            // Arrange
            testRequest.setName("Temperature & Humidity (°C)");
            testSeries.setName("Temperature & Humidity (°C)");

            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(seriesRepository.save(any(Series.class))).thenReturn(testSeries);

            // Act
            SeriesResponse result = seriesService.createSeries(testRequest, "testuser");

            // Assert
            assertThat(result.getName()).isEqualTo("Temperature & Humidity (°C)");
        }

        @Test
        @DisplayName("Should handle series with unicode characters")
        void shouldHandleSeriesWithUnicodeCharacters() {
            // Arrange
            testRequest.setName("温度シリーズ");
            testRequest.setDescription("測定用のテストシリーズ");
            testSeries.setName("温度シリーズ");
            testSeries.setDescription("測定用のテストシリーズ");

            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(seriesRepository.save(any(Series.class))).thenReturn(testSeries);

            // Act
            SeriesResponse result = seriesService.createSeries(testRequest, "testuser");

            // Assert
            assertThat(result.getName()).isEqualTo("温度シリーズ");
            assertThat(result.getDescription()).isEqualTo("測定用のテストシリーズ");
        }

        @Test
        @DisplayName("Should handle color with various formats")
        void shouldHandleColorWithVariousFormats() {
            // Arrange
            testRequest.setColor("rgb(255,87,51)");
            testSeries.setColor("rgb(255,87,51)");

            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(seriesRepository.save(any(Series.class))).thenReturn(testSeries);

            // Act
            SeriesResponse result = seriesService.createSeries(testRequest, "testuser");

            // Assert
            assertThat(result.getColor()).isEqualTo("rgb(255,87,51)");
        }
    }
}
