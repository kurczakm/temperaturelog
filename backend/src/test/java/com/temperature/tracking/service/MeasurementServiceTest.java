package com.temperature.tracking.service;

import com.temperature.tracking.dto.MeasurementRequest;
import com.temperature.tracking.dto.MeasurementResponse;
import com.temperature.tracking.entity.Measurement;
import com.temperature.tracking.entity.Series;
import com.temperature.tracking.entity.User;
import com.temperature.tracking.exception.ResourceNotFoundException;
import com.temperature.tracking.exception.ValidationException;
import com.temperature.tracking.repository.MeasurementRepository;
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

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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
 * Comprehensive unit tests for MeasurementService.
 * Tests all CRUD operations, business logic, validation, and error scenarios.
 * All repository dependencies are mocked using Mockito.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MeasurementService Tests")
class MeasurementServiceTest {

    @Mock
    private MeasurementRepository measurementRepository;

    @Mock
    private SeriesRepository seriesRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private MeasurementService measurementService;

    private User testUser;
    private Series testSeries;
    private Measurement testMeasurement;
    private ZonedDateTime testTimestamp;

    @BeforeEach
    void setUp() {
        testTimestamp = ZonedDateTime.of(2025, 11, 8, 12, 0, 0, 0, ZoneId.of("UTC"));

        testUser = new User();
        testUser.setId(1);
        testUser.setUsername("admin");
        testUser.setPasswordHash("$2a$10$encodedPassword");

        testSeries = new Series();
        testSeries.setId(1);
        testSeries.setName("Temperature");
        testSeries.setDescription("Temperature measurements");
        testSeries.setColor("#FF5733");
        testSeries.setCreatedBy(testUser);
        testSeries.setCreatedAt(testTimestamp.minusDays(1));

        testMeasurement = new Measurement();
        testMeasurement.setId(1);
        testMeasurement.setSeries(testSeries);
        testMeasurement.setValue(new BigDecimal("23.50"));
        testMeasurement.setTimestamp(testTimestamp);
        testMeasurement.setCreatedBy(testUser);
        testMeasurement.setCreatedAt(testTimestamp);
    }

    @Nested
    @DisplayName("Get All Measurements")
    class GetAllMeasurements {

        @Test
        @DisplayName("Should return all measurements when multiple exist")
        void shouldReturnAllMeasurementsWhenMultipleExist() {
            // Arrange
            Measurement measurement2 = new Measurement();
            measurement2.setId(2);
            measurement2.setSeries(testSeries);
            measurement2.setValue(new BigDecimal("24.75"));
            measurement2.setTimestamp(testTimestamp.plusHours(1));
            measurement2.setCreatedBy(testUser);
            measurement2.setCreatedAt(testTimestamp.plusHours(1));

            List<Measurement> measurements = Arrays.asList(testMeasurement, measurement2);
            when(measurementRepository.findAll()).thenReturn(measurements);

            // Act
            List<MeasurementResponse> result = measurementService.getAllMeasurements();

            // Assert
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getId()).isEqualTo(1);
            assertThat(result.get(0).getValue()).isEqualByComparingTo(new BigDecimal("23.50"));
            assertThat(result.get(1).getId()).isEqualTo(2);
            assertThat(result.get(1).getValue()).isEqualByComparingTo(new BigDecimal("24.75"));

            verify(measurementRepository, times(1)).findAll();
        }

        @Test
        @DisplayName("Should return empty list when no measurements exist")
        void shouldReturnEmptyListWhenNoMeasurementsExist() {
            // Arrange
            when(measurementRepository.findAll()).thenReturn(Collections.emptyList());

            // Act
            List<MeasurementResponse> result = measurementService.getAllMeasurements();

            // Assert
            assertThat(result).isEmpty();
            verify(measurementRepository, times(1)).findAll();
        }

        @Test
        @DisplayName("Should correctly map entity fields to response DTO")
        void shouldCorrectlyMapEntityFieldsToResponseDto() {
            // Arrange
            when(measurementRepository.findAll()).thenReturn(Collections.singletonList(testMeasurement));

            // Act
            List<MeasurementResponse> result = measurementService.getAllMeasurements();

            // Assert
            assertThat(result).hasSize(1);
            MeasurementResponse response = result.get(0);
            assertThat(response.getId()).isEqualTo(testMeasurement.getId());
            assertThat(response.getSeriesId()).isEqualTo(testSeries.getId());
            assertThat(response.getSeriesName()).isEqualTo(testSeries.getName());
            assertThat(response.getValue()).isEqualByComparingTo(testMeasurement.getValue());
            assertThat(response.getTimestamp()).isEqualTo(testMeasurement.getTimestamp());
            assertThat(response.getCreatedBy()).isEqualTo(testUser.getId());
            assertThat(response.getCreatedByUsername()).isEqualTo(testUser.getUsername());
            assertThat(response.getCreatedAt()).isEqualTo(testMeasurement.getCreatedAt());
        }
    }

    @Nested
    @DisplayName("Get Measurement By ID")
    class GetMeasurementById {

        @Test
        @DisplayName("Should return measurement when valid ID is provided")
        void shouldReturnMeasurementWhenValidIdIsProvided() {
            // Arrange
            when(measurementRepository.findById(1)).thenReturn(Optional.of(testMeasurement));

            // Act
            MeasurementResponse result = measurementService.getMeasurementById(1);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1);
            assertThat(result.getValue()).isEqualByComparingTo(new BigDecimal("23.50"));
            assertThat(result.getSeriesId()).isEqualTo(testSeries.getId());

            verify(measurementRepository, times(1)).findById(1);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when measurement not found")
        void shouldThrowResourceNotFoundExceptionWhenMeasurementNotFound() {
            // Arrange
            when(measurementRepository.findById(999)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> measurementService.getMeasurementById(999))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Measurement not found with id: 999");

            verify(measurementRepository, times(1)).findById(999);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when ID is null")
        void shouldThrowExceptionWhenIdIsNull() {
            // Arrange
            when(measurementRepository.findById(null)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> measurementService.getMeasurementById(null))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(measurementRepository, times(1)).findById(null);
        }

        @Test
        @DisplayName("Should correctly map all fields from entity")
        void shouldCorrectlyMapAllFieldsFromEntity() {
            // Arrange
            when(measurementRepository.findById(1)).thenReturn(Optional.of(testMeasurement));

            // Act
            MeasurementResponse result = measurementService.getMeasurementById(1);

            // Assert
            assertThat(result.getId()).isEqualTo(testMeasurement.getId());
            assertThat(result.getSeriesId()).isEqualTo(testSeries.getId());
            assertThat(result.getSeriesName()).isEqualTo(testSeries.getName());
            assertThat(result.getValue()).isEqualByComparingTo(testMeasurement.getValue());
            assertThat(result.getTimestamp()).isEqualTo(testMeasurement.getTimestamp());
            assertThat(result.getCreatedBy()).isEqualTo(testUser.getId());
            assertThat(result.getCreatedByUsername()).isEqualTo(testUser.getUsername());
        }
    }

    @Nested
    @DisplayName("Get Measurements By Series ID")
    class GetMeasurementsBySeriesId {

        @Test
        @DisplayName("Should return all measurements for a specific series")
        void shouldReturnAllMeasurementsForSpecificSeries() {
            // Arrange
            Measurement measurement2 = new Measurement();
            measurement2.setId(2);
            measurement2.setSeries(testSeries);
            measurement2.setValue(new BigDecimal("25.00"));
            measurement2.setTimestamp(testTimestamp.plusHours(2));
            measurement2.setCreatedBy(testUser);
            measurement2.setCreatedAt(testTimestamp.plusHours(2));

            List<Measurement> measurements = Arrays.asList(testMeasurement, measurement2);
            when(measurementRepository.findBySeriesId(1)).thenReturn(measurements);

            // Act
            List<MeasurementResponse> result = measurementService.getMeasurementsBySeriesId(1);

            // Assert
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getSeriesId()).isEqualTo(1);
            assertThat(result.get(1).getSeriesId()).isEqualTo(1);
            assertThat(result.get(0).getValue()).isEqualByComparingTo(new BigDecimal("23.50"));
            assertThat(result.get(1).getValue()).isEqualByComparingTo(new BigDecimal("25.00"));

            verify(measurementRepository, times(1)).findBySeriesId(1);
        }

        @Test
        @DisplayName("Should return empty list when series has no measurements")
        void shouldReturnEmptyListWhenSeriesHasNoMeasurements() {
            // Arrange
            when(measurementRepository.findBySeriesId(1)).thenReturn(Collections.emptyList());

            // Act
            List<MeasurementResponse> result = measurementService.getMeasurementsBySeriesId(1);

            // Assert
            assertThat(result).isEmpty();
            verify(measurementRepository, times(1)).findBySeriesId(1);
        }

        @Test
        @DisplayName("Should return empty list when series does not exist")
        void shouldReturnEmptyListWhenSeriesDoesNotExist() {
            // Arrange
            when(measurementRepository.findBySeriesId(999)).thenReturn(Collections.emptyList());

            // Act
            List<MeasurementResponse> result = measurementService.getMeasurementsBySeriesId(999);

            // Assert
            assertThat(result).isEmpty();
            verify(measurementRepository, times(1)).findBySeriesId(999);
        }

        @Test
        @DisplayName("Should handle multiple measurements with different timestamps")
        void shouldHandleMultipleMeasurementsWithDifferentTimestamps() {
            // Arrange
            Measurement earlyMeasurement = new Measurement();
            earlyMeasurement.setId(1);
            earlyMeasurement.setSeries(testSeries);
            earlyMeasurement.setValue(new BigDecimal("20.00"));
            earlyMeasurement.setTimestamp(testTimestamp.minusHours(2));
            earlyMeasurement.setCreatedBy(testUser);
            earlyMeasurement.setCreatedAt(testTimestamp.minusHours(2));

            Measurement lateMeasurement = new Measurement();
            lateMeasurement.setId(2);
            lateMeasurement.setSeries(testSeries);
            lateMeasurement.setValue(new BigDecimal("30.00"));
            lateMeasurement.setTimestamp(testTimestamp.plusHours(2));
            lateMeasurement.setCreatedBy(testUser);
            lateMeasurement.setCreatedAt(testTimestamp.plusHours(2));

            when(measurementRepository.findBySeriesId(1))
                    .thenReturn(Arrays.asList(earlyMeasurement, lateMeasurement));

            // Act
            List<MeasurementResponse> result = measurementService.getMeasurementsBySeriesId(1);

            // Assert
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getTimestamp()).isEqualTo(testTimestamp.minusHours(2));
            assertThat(result.get(1).getTimestamp()).isEqualTo(testTimestamp.plusHours(2));
        }
    }

    @Nested
    @DisplayName("Create Measurement")
    class CreateMeasurement {

        @Test
        @DisplayName("Should create measurement with valid data")
        void shouldCreateMeasurementWithValidData() {
            // Arrange
            MeasurementRequest request = new MeasurementRequest(
                    1,
                    new BigDecimal("22.50"),
                    testTimestamp
            );

            when(userRepository.findByUsername("admin")).thenReturn(Optional.of(testUser));
            when(seriesRepository.findById(1)).thenReturn(Optional.of(testSeries));
            when(measurementRepository.save(any(Measurement.class))).thenReturn(testMeasurement);

            // Act
            MeasurementResponse result = measurementService.createMeasurement(request, "admin");

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1);
            assertThat(result.getValue()).isEqualByComparingTo(new BigDecimal("23.50"));
            assertThat(result.getSeriesId()).isEqualTo(1);
            assertThat(result.getCreatedByUsername()).isEqualTo("admin");

            verify(userRepository, times(1)).findByUsername("admin");
            verify(seriesRepository, times(1)).findById(1);
            verify(measurementRepository, times(1)).save(any(Measurement.class));
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when user not found")
        void shouldThrowResourceNotFoundExceptionWhenUserNotFound() {
            // Arrange
            MeasurementRequest request = new MeasurementRequest(
                    1,
                    new BigDecimal("22.50"),
                    testTimestamp
            );

            when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> measurementService.createMeasurement(request, "nonexistent"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User not found: nonexistent");

            verify(userRepository, times(1)).findByUsername("nonexistent");
            verify(seriesRepository, never()).findById(anyInt());
            verify(measurementRepository, never()).save(any(Measurement.class));
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when series not found")
        void shouldThrowResourceNotFoundExceptionWhenSeriesNotFound() {
            // Arrange
            MeasurementRequest request = new MeasurementRequest(
                    999,
                    new BigDecimal("22.50"),
                    testTimestamp
            );

            when(userRepository.findByUsername("admin")).thenReturn(Optional.of(testUser));
            when(seriesRepository.findById(999)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> measurementService.createMeasurement(request, "admin"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Series not found with id: 999");

            verify(userRepository, times(1)).findByUsername("admin");
            verify(seriesRepository, times(1)).findById(999);
            verify(measurementRepository, never()).save(any(Measurement.class));
        }

        @Test
        @DisplayName("Should correctly set all measurement fields before saving")
        void shouldCorrectlySetAllMeasurementFieldsBeforeSaving() {
            // Arrange
            MeasurementRequest request = new MeasurementRequest(
                    1,
                    new BigDecimal("22.50"),
                    testTimestamp
            );

            when(userRepository.findByUsername("admin")).thenReturn(Optional.of(testUser));
            when(seriesRepository.findById(1)).thenReturn(Optional.of(testSeries));
            when(measurementRepository.save(any(Measurement.class))).thenAnswer(invocation -> {
                Measurement saved = invocation.getArgument(0);
                assertThat(saved.getSeries()).isEqualTo(testSeries);
                assertThat(saved.getValue()).isEqualByComparingTo(new BigDecimal("22.50"));
                assertThat(saved.getTimestamp()).isEqualTo(testTimestamp);
                assertThat(saved.getCreatedBy()).isEqualTo(testUser);
                return testMeasurement;
            });

            // Act
            measurementService.createMeasurement(request, "admin");

            // Assert
            verify(measurementRepository, times(1)).save(any(Measurement.class));
        }

        @Test
        @DisplayName("Should handle negative measurement values")
        void shouldHandleNegativeMeasurementValues() {
            // Arrange
            MeasurementRequest request = new MeasurementRequest(
                    1,
                    new BigDecimal("-15.50"),
                    testTimestamp
            );

            Measurement negativeMeasurement = new Measurement();
            negativeMeasurement.setId(2);
            negativeMeasurement.setSeries(testSeries);
            negativeMeasurement.setValue(new BigDecimal("-15.50"));
            negativeMeasurement.setTimestamp(testTimestamp);
            negativeMeasurement.setCreatedBy(testUser);
            negativeMeasurement.setCreatedAt(testTimestamp);

            when(userRepository.findByUsername("admin")).thenReturn(Optional.of(testUser));
            when(seriesRepository.findById(1)).thenReturn(Optional.of(testSeries));
            when(measurementRepository.save(any(Measurement.class))).thenReturn(negativeMeasurement);

            // Act
            MeasurementResponse result = measurementService.createMeasurement(request, "admin");

            // Assert
            assertThat(result.getValue()).isEqualByComparingTo(new BigDecimal("-15.50"));
        }

        @Test
        @DisplayName("Should handle decimal values with precision")
        void shouldHandleDecimalValuesWithPrecision() {
            // Arrange
            MeasurementRequest request = new MeasurementRequest(
                    1,
                    new BigDecimal("23.456"),
                    testTimestamp
            );

            Measurement precisionMeasurement = new Measurement();
            precisionMeasurement.setId(2);
            precisionMeasurement.setSeries(testSeries);
            precisionMeasurement.setValue(new BigDecimal("23.456"));
            precisionMeasurement.setTimestamp(testTimestamp);
            precisionMeasurement.setCreatedBy(testUser);
            precisionMeasurement.setCreatedAt(testTimestamp);

            when(userRepository.findByUsername("admin")).thenReturn(Optional.of(testUser));
            when(seriesRepository.findById(1)).thenReturn(Optional.of(testSeries));
            when(measurementRepository.save(any(Measurement.class))).thenReturn(precisionMeasurement);

            // Act
            MeasurementResponse result = measurementService.createMeasurement(request, "admin");

            // Assert
            assertThat(result.getValue()).isEqualByComparingTo(new BigDecimal("23.456"));
        }
    }

    @Nested
    @DisplayName("Update Measurement")
    class UpdateMeasurement {

        @Test
        @DisplayName("Should update measurement with new value and timestamp")
        void shouldUpdateMeasurementWithNewValueAndTimestamp() {
            // Arrange
            MeasurementRequest request = new MeasurementRequest(
                    1,
                    new BigDecimal("25.00"),
                    testTimestamp.plusHours(1)
            );

            Measurement updatedMeasurement = new Measurement();
            updatedMeasurement.setId(1);
            updatedMeasurement.setSeries(testSeries);
            updatedMeasurement.setValue(new BigDecimal("25.00"));
            updatedMeasurement.setTimestamp(testTimestamp.plusHours(1));
            updatedMeasurement.setCreatedBy(testUser);
            updatedMeasurement.setCreatedAt(testTimestamp);

            when(measurementRepository.findById(1)).thenReturn(Optional.of(testMeasurement));
            when(seriesRepository.findById(1)).thenReturn(Optional.of(testSeries));
            when(measurementRepository.save(any(Measurement.class))).thenReturn(updatedMeasurement);

            // Act
            MeasurementResponse result = measurementService.updateMeasurement(1, request);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getValue()).isEqualByComparingTo(new BigDecimal("25.00"));
            assertThat(result.getTimestamp()).isEqualTo(testTimestamp.plusHours(1));

            verify(measurementRepository, times(1)).findById(1);
            verify(seriesRepository, times(1)).findById(1);
            verify(measurementRepository, times(1)).save(any(Measurement.class));
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when measurement not found")
        void shouldThrowResourceNotFoundExceptionWhenMeasurementNotFound() {
            // Arrange
            MeasurementRequest request = new MeasurementRequest(
                    1,
                    new BigDecimal("25.00"),
                    testTimestamp
            );

            when(measurementRepository.findById(999)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> measurementService.updateMeasurement(999, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Measurement not found with id: 999");

            verify(measurementRepository, times(1)).findById(999);
            verify(seriesRepository, never()).findById(anyInt());
            verify(measurementRepository, never()).save(any(Measurement.class));
        }

        @Test
        @DisplayName("Should update series when new series ID is provided")
        void shouldUpdateSeriesWhenNewSeriesIdIsProvided() {
            // Arrange
            Series newSeries = new Series();
            newSeries.setId(2);
            newSeries.setName("Humidity");
            newSeries.setCreatedBy(testUser);

            MeasurementRequest request = new MeasurementRequest(
                    2,
                    new BigDecimal("25.00"),
                    testTimestamp
            );

            when(measurementRepository.findById(1)).thenReturn(Optional.of(testMeasurement));
            when(seriesRepository.findById(2)).thenReturn(Optional.of(newSeries));
            when(measurementRepository.save(any(Measurement.class))).thenAnswer(invocation -> {
                Measurement saved = invocation.getArgument(0);
                assertThat(saved.getSeries()).isEqualTo(newSeries);
                return saved;
            });

            // Act
            measurementService.updateMeasurement(1, request);

            // Assert
            verify(seriesRepository, times(1)).findById(2);
            verify(measurementRepository, times(1)).save(any(Measurement.class));
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when new series not found")
        void shouldThrowResourceNotFoundExceptionWhenNewSeriesNotFound() {
            // Arrange
            MeasurementRequest request = new MeasurementRequest(
                    999,
                    new BigDecimal("25.00"),
                    testTimestamp
            );

            when(measurementRepository.findById(1)).thenReturn(Optional.of(testMeasurement));
            when(seriesRepository.findById(999)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> measurementService.updateMeasurement(1, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Series not found with id: 999");

            verify(measurementRepository, times(1)).findById(1);
            verify(seriesRepository, times(1)).findById(999);
            verify(measurementRepository, never()).save(any(Measurement.class));
        }

        @Test
        @DisplayName("Should not update series when series ID is null")
        void shouldNotUpdateSeriesWhenSeriesIdIsNull() {
            // Arrange
            MeasurementRequest request = new MeasurementRequest(
                    null,
                    new BigDecimal("25.00"),
                    testTimestamp
            );

            when(measurementRepository.findById(1)).thenReturn(Optional.of(testMeasurement));
            when(measurementRepository.save(any(Measurement.class))).thenAnswer(invocation -> {
                Measurement saved = invocation.getArgument(0);
                // Series should remain unchanged
                assertThat(saved.getSeries()).isEqualTo(testSeries);
                return saved;
            });

            // Act
            measurementService.updateMeasurement(1, request);

            // Assert
            verify(seriesRepository, never()).findById(anyInt());
            verify(measurementRepository, times(1)).save(any(Measurement.class));
        }

        @Test
        @DisplayName("Should update only value and timestamp when series ID is same")
        void shouldUpdateOnlyValueAndTimestampWhenSeriesIdIsSame() {
            // Arrange
            MeasurementRequest request = new MeasurementRequest(
                    1,
                    new BigDecimal("26.50"),
                    testTimestamp.plusHours(2)
            );

            Measurement updatedMeasurement = new Measurement();
            updatedMeasurement.setId(1);
            updatedMeasurement.setSeries(testSeries);
            updatedMeasurement.setValue(new BigDecimal("26.50"));
            updatedMeasurement.setTimestamp(testTimestamp.plusHours(2));
            updatedMeasurement.setCreatedBy(testUser);
            updatedMeasurement.setCreatedAt(testTimestamp);

            when(measurementRepository.findById(1)).thenReturn(Optional.of(testMeasurement));
            when(seriesRepository.findById(1)).thenReturn(Optional.of(testSeries));
            when(measurementRepository.save(any(Measurement.class))).thenReturn(updatedMeasurement);

            // Act
            MeasurementResponse result = measurementService.updateMeasurement(1, request);

            // Assert
            assertThat(result.getValue()).isEqualByComparingTo(new BigDecimal("26.50"));
            assertThat(result.getTimestamp()).isEqualTo(testTimestamp.plusHours(2));
            assertThat(result.getSeriesId()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Delete Measurement")
    class DeleteMeasurement {

        @Test
        @DisplayName("Should delete measurement when it exists")
        void shouldDeleteMeasurementWhenItExists() {
            // Arrange
            when(measurementRepository.existsById(1)).thenReturn(true);
            doNothing().when(measurementRepository).deleteById(1);

            // Act
            measurementService.deleteMeasurement(1);

            // Assert
            verify(measurementRepository, times(1)).existsById(1);
            verify(measurementRepository, times(1)).deleteById(1);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when measurement not found")
        void shouldThrowResourceNotFoundExceptionWhenMeasurementNotFound() {
            // Arrange
            when(measurementRepository.existsById(999)).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> measurementService.deleteMeasurement(999))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Measurement not found with id: 999");

            verify(measurementRepository, times(1)).existsById(999);
            verify(measurementRepository, never()).deleteById(anyInt());
        }

        @Test
        @DisplayName("Should not call deleteById when measurement does not exist")
        void shouldNotCallDeleteByIdWhenMeasurementDoesNotExist() {
            // Arrange
            when(measurementRepository.existsById(999)).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> measurementService.deleteMeasurement(999))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(measurementRepository, never()).deleteById(anyInt());
        }

        @Test
        @DisplayName("Should check existence before attempting delete")
        void shouldCheckExistenceBeforeAttemptingDelete() {
            // Arrange
            when(measurementRepository.existsById(1)).thenReturn(true);

            // Act
            measurementService.deleteMeasurement(1);

            // Assert - verify order of operations
            var inOrder = inOrder(measurementRepository);
            inOrder.verify(measurementRepository).existsById(1);
            inOrder.verify(measurementRepository).deleteById(1);
        }
    }

    @Nested
    @DisplayName("Edge Cases and Boundary Conditions")
    class EdgeCasesAndBoundaryConditions {

        @Test
        @DisplayName("Should handle zero measurement value")
        void shouldHandleZeroMeasurementValue() {
            // Arrange
            MeasurementRequest request = new MeasurementRequest(
                    1,
                    BigDecimal.ZERO,
                    testTimestamp
            );

            Measurement zeroMeasurement = new Measurement();
            zeroMeasurement.setId(2);
            zeroMeasurement.setSeries(testSeries);
            zeroMeasurement.setValue(BigDecimal.ZERO);
            zeroMeasurement.setTimestamp(testTimestamp);
            zeroMeasurement.setCreatedBy(testUser);
            zeroMeasurement.setCreatedAt(testTimestamp);

            when(userRepository.findByUsername("admin")).thenReturn(Optional.of(testUser));
            when(seriesRepository.findById(1)).thenReturn(Optional.of(testSeries));
            when(measurementRepository.save(any(Measurement.class))).thenReturn(zeroMeasurement);

            // Act
            MeasurementResponse result = measurementService.createMeasurement(request, "admin");

            // Assert
            assertThat(result.getValue()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Should handle very large measurement values")
        void shouldHandleVeryLargeMeasurementValues() {
            // Arrange
            BigDecimal largeValue = new BigDecimal("999.99");
            MeasurementRequest request = new MeasurementRequest(
                    1,
                    largeValue,
                    testTimestamp
            );

            Measurement largeMeasurement = new Measurement();
            largeMeasurement.setId(2);
            largeMeasurement.setSeries(testSeries);
            largeMeasurement.setValue(largeValue);
            largeMeasurement.setTimestamp(testTimestamp);
            largeMeasurement.setCreatedBy(testUser);
            largeMeasurement.setCreatedAt(testTimestamp);

            when(userRepository.findByUsername("admin")).thenReturn(Optional.of(testUser));
            when(seriesRepository.findById(1)).thenReturn(Optional.of(testSeries));
            when(measurementRepository.save(any(Measurement.class))).thenReturn(largeMeasurement);

            // Act
            MeasurementResponse result = measurementService.createMeasurement(request, "admin");

            // Assert
            assertThat(result.getValue()).isEqualByComparingTo(largeValue);
        }

        @Test
        @DisplayName("Should handle timestamp in the future")
        void shouldHandleTimestampInTheFuture() {
            // Arrange
            ZonedDateTime futureTimestamp = ZonedDateTime.now().plusYears(1);
            MeasurementRequest request = new MeasurementRequest(
                    1,
                    new BigDecimal("23.50"),
                    futureTimestamp
            );

            Measurement futureMeasurement = new Measurement();
            futureMeasurement.setId(2);
            futureMeasurement.setSeries(testSeries);
            futureMeasurement.setValue(new BigDecimal("23.50"));
            futureMeasurement.setTimestamp(futureTimestamp);
            futureMeasurement.setCreatedBy(testUser);
            futureMeasurement.setCreatedAt(testTimestamp);

            when(userRepository.findByUsername("admin")).thenReturn(Optional.of(testUser));
            when(seriesRepository.findById(1)).thenReturn(Optional.of(testSeries));
            when(measurementRepository.save(any(Measurement.class))).thenReturn(futureMeasurement);

            // Act
            MeasurementResponse result = measurementService.createMeasurement(request, "admin");

            // Assert
            assertThat(result.getTimestamp()).isEqualTo(futureTimestamp);
        }

        @Test
        @DisplayName("Should handle timestamp in the distant past")
        void shouldHandleTimestampInTheDistantPast() {
            // Arrange
            ZonedDateTime pastTimestamp = ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
            MeasurementRequest request = new MeasurementRequest(
                    1,
                    new BigDecimal("23.50"),
                    pastTimestamp
            );

            Measurement pastMeasurement = new Measurement();
            pastMeasurement.setId(2);
            pastMeasurement.setSeries(testSeries);
            pastMeasurement.setValue(new BigDecimal("23.50"));
            pastMeasurement.setTimestamp(pastTimestamp);
            pastMeasurement.setCreatedBy(testUser);
            pastMeasurement.setCreatedAt(testTimestamp);

            when(userRepository.findByUsername("admin")).thenReturn(Optional.of(testUser));
            when(seriesRepository.findById(1)).thenReturn(Optional.of(testSeries));
            when(measurementRepository.save(any(Measurement.class))).thenReturn(pastMeasurement);

            // Act
            MeasurementResponse result = measurementService.createMeasurement(request, "admin");

            // Assert
            assertThat(result.getTimestamp()).isEqualTo(pastTimestamp);
        }
    }

    @Nested
    @DisplayName("Value Validation Against Series Bounds")
    class ValueValidation {

        private Series seriesWithBounds;
        private Series seriesNoBounds;

        @BeforeEach
        void setUpValidation() {
            // Series with min=0.00 and max=100.00
            seriesWithBounds = new Series();
            seriesWithBounds.setId(2);
            seriesWithBounds.setName("Bounded Temperature");
            seriesWithBounds.setDescription("Temperature with min/max bounds");
            seriesWithBounds.setMinValue(new BigDecimal("0.00"));
            seriesWithBounds.setMaxValue(new BigDecimal("100.00"));
            seriesWithBounds.setCreatedBy(testUser);
            seriesWithBounds.setCreatedAt(testTimestamp);

            // Series with no bounds (null min/max)
            seriesNoBounds = new Series();
            seriesNoBounds.setId(3);
            seriesNoBounds.setName("Unbounded Temperature");
            seriesNoBounds.setDescription("Temperature without min/max bounds");
            seriesNoBounds.setMinValue(null);
            seriesNoBounds.setMaxValue(null);
            seriesNoBounds.setCreatedBy(testUser);
            seriesNoBounds.setCreatedAt(testTimestamp);
        }

        @Nested
        @DisplayName("Create Measurement Validation")
        class CreateMeasurementValidation {

            @Test
            @DisplayName("Should successfully create measurement with value within bounds")
            void shouldSuccessfullyCreateMeasurementWithValueWithinBounds() {
                // Arrange
                MeasurementRequest request = new MeasurementRequest(
                        2,
                        new BigDecimal("50.00"),
                        testTimestamp
                );

                Measurement savedMeasurement = new Measurement();
                savedMeasurement.setId(1);
                savedMeasurement.setSeries(seriesWithBounds);
                savedMeasurement.setValue(new BigDecimal("50.00"));
                savedMeasurement.setTimestamp(testTimestamp);
                savedMeasurement.setCreatedBy(testUser);

                when(userRepository.findByUsername("admin")).thenReturn(Optional.of(testUser));
                when(seriesRepository.findById(2)).thenReturn(Optional.of(seriesWithBounds));
                when(measurementRepository.save(any(Measurement.class))).thenReturn(savedMeasurement);

                // Act
                MeasurementResponse result = measurementService.createMeasurement(request, "admin");

                // Assert
                assertThat(result).isNotNull();
                assertThat(result.getValue()).isEqualByComparingTo(new BigDecimal("50.00"));
                verify(measurementRepository, times(1)).save(any(Measurement.class));
            }

            @Test
            @DisplayName("Should successfully create measurement with value exactly at minimum")
            void shouldSuccessfullyCreateMeasurementWithValueExactlyAtMinimum() {
                // Arrange
                MeasurementRequest request = new MeasurementRequest(
                        2,
                        new BigDecimal("0.00"),
                        testTimestamp
                );

                Measurement savedMeasurement = new Measurement();
                savedMeasurement.setId(1);
                savedMeasurement.setSeries(seriesWithBounds);
                savedMeasurement.setValue(new BigDecimal("0.00"));
                savedMeasurement.setTimestamp(testTimestamp);
                savedMeasurement.setCreatedBy(testUser);

                when(userRepository.findByUsername("admin")).thenReturn(Optional.of(testUser));
                when(seriesRepository.findById(2)).thenReturn(Optional.of(seriesWithBounds));
                when(measurementRepository.save(any(Measurement.class))).thenReturn(savedMeasurement);

                // Act
                MeasurementResponse result = measurementService.createMeasurement(request, "admin");

                // Assert
                assertThat(result).isNotNull();
                assertThat(result.getValue()).isEqualByComparingTo(new BigDecimal("0.00"));
                verify(measurementRepository, times(1)).save(any(Measurement.class));
            }

            @Test
            @DisplayName("Should successfully create measurement with value exactly at maximum")
            void shouldSuccessfullyCreateMeasurementWithValueExactlyAtMaximum() {
                // Arrange
                MeasurementRequest request = new MeasurementRequest(
                        2,
                        new BigDecimal("100.00"),
                        testTimestamp
                );

                Measurement savedMeasurement = new Measurement();
                savedMeasurement.setId(1);
                savedMeasurement.setSeries(seriesWithBounds);
                savedMeasurement.setValue(new BigDecimal("100.00"));
                savedMeasurement.setTimestamp(testTimestamp);
                savedMeasurement.setCreatedBy(testUser);

                when(userRepository.findByUsername("admin")).thenReturn(Optional.of(testUser));
                when(seriesRepository.findById(2)).thenReturn(Optional.of(seriesWithBounds));
                when(measurementRepository.save(any(Measurement.class))).thenReturn(savedMeasurement);

                // Act
                MeasurementResponse result = measurementService.createMeasurement(request, "admin");

                // Assert
                assertThat(result).isNotNull();
                assertThat(result.getValue()).isEqualByComparingTo(new BigDecimal("100.00"));
                verify(measurementRepository, times(1)).save(any(Measurement.class));
            }

            @Test
            @DisplayName("Should throw ValidationException when value is below minimum")
            void shouldThrowValidationExceptionWhenValueIsBelowMinimum() {
                // Arrange
                MeasurementRequest request = new MeasurementRequest(
                        2,
                        new BigDecimal("-1.00"),
                        testTimestamp
                );

                when(userRepository.findByUsername("admin")).thenReturn(Optional.of(testUser));
                when(seriesRepository.findById(2)).thenReturn(Optional.of(seriesWithBounds));

                // Act & Assert
                assertThatThrownBy(() -> measurementService.createMeasurement(request, "admin"))
                        .isInstanceOf(ValidationException.class)
                        .hasMessageContaining("below the minimum allowed value")
                        .hasMessageContaining("-1.00")
                        .hasMessageContaining("0.00")
                        .hasMessageContaining("Bounded Temperature");

                verify(measurementRepository, never()).save(any(Measurement.class));
            }

            @Test
            @DisplayName("Should throw ValidationException when value is above maximum")
            void shouldThrowValidationExceptionWhenValueIsAboveMaximum() {
                // Arrange
                MeasurementRequest request = new MeasurementRequest(
                        2,
                        new BigDecimal("150.00"),
                        testTimestamp
                );

                when(userRepository.findByUsername("admin")).thenReturn(Optional.of(testUser));
                when(seriesRepository.findById(2)).thenReturn(Optional.of(seriesWithBounds));

                // Act & Assert
                assertThatThrownBy(() -> measurementService.createMeasurement(request, "admin"))
                        .isInstanceOf(ValidationException.class)
                        .hasMessageContaining("exceeds the maximum allowed value")
                        .hasMessageContaining("150.00")
                        .hasMessageContaining("100.00")
                        .hasMessageContaining("Bounded Temperature");

                verify(measurementRepository, never()).save(any(Measurement.class));
            }

            @Test
            @DisplayName("Should throw ValidationException when value is slightly below minimum")
            void shouldThrowValidationExceptionWhenValueIsSlightlyBelowMinimum() {
                // Arrange
                MeasurementRequest request = new MeasurementRequest(
                        2,
                        new BigDecimal("-0.01"),
                        testTimestamp
                );

                when(userRepository.findByUsername("admin")).thenReturn(Optional.of(testUser));
                when(seriesRepository.findById(2)).thenReturn(Optional.of(seriesWithBounds));

                // Act & Assert
                assertThatThrownBy(() -> measurementService.createMeasurement(request, "admin"))
                        .isInstanceOf(ValidationException.class)
                        .hasMessageContaining("below the minimum allowed value");

                verify(measurementRepository, never()).save(any(Measurement.class));
            }

            @Test
            @DisplayName("Should throw ValidationException when value is slightly above maximum")
            void shouldThrowValidationExceptionWhenValueIsSlightlyAboveMaximum() {
                // Arrange
                MeasurementRequest request = new MeasurementRequest(
                        2,
                        new BigDecimal("100.01"),
                        testTimestamp
                );

                when(userRepository.findByUsername("admin")).thenReturn(Optional.of(testUser));
                when(seriesRepository.findById(2)).thenReturn(Optional.of(seriesWithBounds));

                // Act & Assert
                assertThatThrownBy(() -> measurementService.createMeasurement(request, "admin"))
                        .isInstanceOf(ValidationException.class)
                        .hasMessageContaining("exceeds the maximum allowed value");

                verify(measurementRepository, never()).save(any(Measurement.class));
            }

            @Test
            @DisplayName("Should allow any value when series has no bounds")
            void shouldAllowAnyValueWhenSeriesHasNoBounds() {
                // Arrange
                MeasurementRequest request = new MeasurementRequest(
                        3,
                        new BigDecimal("999999.99"),
                        testTimestamp
                );

                Measurement savedMeasurement = new Measurement();
                savedMeasurement.setId(1);
                savedMeasurement.setSeries(seriesNoBounds);
                savedMeasurement.setValue(new BigDecimal("999999.99"));
                savedMeasurement.setTimestamp(testTimestamp);
                savedMeasurement.setCreatedBy(testUser);

                when(userRepository.findByUsername("admin")).thenReturn(Optional.of(testUser));
                when(seriesRepository.findById(3)).thenReturn(Optional.of(seriesNoBounds));
                when(measurementRepository.save(any(Measurement.class))).thenReturn(savedMeasurement);

                // Act
                MeasurementResponse result = measurementService.createMeasurement(request, "admin");

                // Assert
                assertThat(result).isNotNull();
                assertThat(result.getValue()).isEqualByComparingTo(new BigDecimal("999999.99"));
                verify(measurementRepository, times(1)).save(any(Measurement.class));
            }

            @Test
            @DisplayName("Should allow negative values when series has no bounds")
            void shouldAllowNegativeValuesWhenSeriesHasNoBounds() {
                // Arrange
                MeasurementRequest request = new MeasurementRequest(
                        3,
                        new BigDecimal("-999999.99"),
                        testTimestamp
                );

                Measurement savedMeasurement = new Measurement();
                savedMeasurement.setId(1);
                savedMeasurement.setSeries(seriesNoBounds);
                savedMeasurement.setValue(new BigDecimal("-999999.99"));
                savedMeasurement.setTimestamp(testTimestamp);
                savedMeasurement.setCreatedBy(testUser);

                when(userRepository.findByUsername("admin")).thenReturn(Optional.of(testUser));
                when(seriesRepository.findById(3)).thenReturn(Optional.of(seriesNoBounds));
                when(measurementRepository.save(any(Measurement.class))).thenReturn(savedMeasurement);

                // Act
                MeasurementResponse result = measurementService.createMeasurement(request, "admin");

                // Assert
                assertThat(result).isNotNull();
                assertThat(result.getValue()).isEqualByComparingTo(new BigDecimal("-999999.99"));
                verify(measurementRepository, times(1)).save(any(Measurement.class));
            }
        }

        @Nested
        @DisplayName("Update Measurement Validation")
        class UpdateMeasurementValidation {

            @Test
            @DisplayName("Should successfully update measurement with value within bounds")
            void shouldSuccessfullyUpdateMeasurementWithValueWithinBounds() {
                // Arrange
                Measurement existingMeasurement = new Measurement();
                existingMeasurement.setId(1);
                existingMeasurement.setSeries(seriesWithBounds);
                existingMeasurement.setValue(new BigDecimal("50.00"));
                existingMeasurement.setTimestamp(testTimestamp);
                existingMeasurement.setCreatedBy(testUser);

                MeasurementRequest request = new MeasurementRequest(
                        2,
                        new BigDecimal("75.00"),
                        testTimestamp.plusHours(1)
                );

                Measurement updatedMeasurement = new Measurement();
                updatedMeasurement.setId(1);
                updatedMeasurement.setSeries(seriesWithBounds);
                updatedMeasurement.setValue(new BigDecimal("75.00"));
                updatedMeasurement.setTimestamp(testTimestamp.plusHours(1));
                updatedMeasurement.setCreatedBy(testUser);

                when(measurementRepository.findById(1)).thenReturn(Optional.of(existingMeasurement));
                when(seriesRepository.findById(2)).thenReturn(Optional.of(seriesWithBounds));
                when(measurementRepository.save(any(Measurement.class))).thenReturn(updatedMeasurement);

                // Act
                MeasurementResponse result = measurementService.updateMeasurement(1, request);

                // Assert
                assertThat(result).isNotNull();
                assertThat(result.getValue()).isEqualByComparingTo(new BigDecimal("75.00"));
                verify(measurementRepository, times(1)).save(any(Measurement.class));
            }

            @Test
            @DisplayName("Should successfully update measurement with value at minimum")
            void shouldSuccessfullyUpdateMeasurementWithValueAtMinimum() {
                // Arrange
                Measurement existingMeasurement = new Measurement();
                existingMeasurement.setId(1);
                existingMeasurement.setSeries(seriesWithBounds);
                existingMeasurement.setValue(new BigDecimal("50.00"));
                existingMeasurement.setTimestamp(testTimestamp);
                existingMeasurement.setCreatedBy(testUser);

                MeasurementRequest request = new MeasurementRequest(
                        null,
                        new BigDecimal("0.00"),
                        testTimestamp.plusHours(1)
                );

                Measurement updatedMeasurement = new Measurement();
                updatedMeasurement.setId(1);
                updatedMeasurement.setSeries(seriesWithBounds);
                updatedMeasurement.setValue(new BigDecimal("0.00"));
                updatedMeasurement.setTimestamp(testTimestamp.plusHours(1));
                updatedMeasurement.setCreatedBy(testUser);

                when(measurementRepository.findById(1)).thenReturn(Optional.of(existingMeasurement));
                when(measurementRepository.save(any(Measurement.class))).thenReturn(updatedMeasurement);

                // Act
                MeasurementResponse result = measurementService.updateMeasurement(1, request);

                // Assert
                assertThat(result).isNotNull();
                assertThat(result.getValue()).isEqualByComparingTo(new BigDecimal("0.00"));
                verify(measurementRepository, times(1)).save(any(Measurement.class));
            }

            @Test
            @DisplayName("Should successfully update measurement with value at maximum")
            void shouldSuccessfullyUpdateMeasurementWithValueAtMaximum() {
                // Arrange
                Measurement existingMeasurement = new Measurement();
                existingMeasurement.setId(1);
                existingMeasurement.setSeries(seriesWithBounds);
                existingMeasurement.setValue(new BigDecimal("50.00"));
                existingMeasurement.setTimestamp(testTimestamp);
                existingMeasurement.setCreatedBy(testUser);

                MeasurementRequest request = new MeasurementRequest(
                        null,
                        new BigDecimal("100.00"),
                        testTimestamp.plusHours(1)
                );

                Measurement updatedMeasurement = new Measurement();
                updatedMeasurement.setId(1);
                updatedMeasurement.setSeries(seriesWithBounds);
                updatedMeasurement.setValue(new BigDecimal("100.00"));
                updatedMeasurement.setTimestamp(testTimestamp.plusHours(1));
                updatedMeasurement.setCreatedBy(testUser);

                when(measurementRepository.findById(1)).thenReturn(Optional.of(existingMeasurement));
                when(measurementRepository.save(any(Measurement.class))).thenReturn(updatedMeasurement);

                // Act
                MeasurementResponse result = measurementService.updateMeasurement(1, request);

                // Assert
                assertThat(result).isNotNull();
                assertThat(result.getValue()).isEqualByComparingTo(new BigDecimal("100.00"));
                verify(measurementRepository, times(1)).save(any(Measurement.class));
            }

            @Test
            @DisplayName("Should throw ValidationException when updating with value below minimum")
            void shouldThrowValidationExceptionWhenUpdatingWithValueBelowMinimum() {
                // Arrange
                Measurement existingMeasurement = new Measurement();
                existingMeasurement.setId(1);
                existingMeasurement.setSeries(seriesWithBounds);
                existingMeasurement.setValue(new BigDecimal("50.00"));
                existingMeasurement.setTimestamp(testTimestamp);
                existingMeasurement.setCreatedBy(testUser);

                MeasurementRequest request = new MeasurementRequest(
                        null,
                        new BigDecimal("-5.00"),
                        testTimestamp.plusHours(1)
                );

                when(measurementRepository.findById(1)).thenReturn(Optional.of(existingMeasurement));

                // Act & Assert
                assertThatThrownBy(() -> measurementService.updateMeasurement(1, request))
                        .isInstanceOf(ValidationException.class)
                        .hasMessageContaining("below the minimum allowed value")
                        .hasMessageContaining("-5.00")
                        .hasMessageContaining("0.00");

                verify(measurementRepository, never()).save(any(Measurement.class));
            }

            @Test
            @DisplayName("Should throw ValidationException when updating with value above maximum")
            void shouldThrowValidationExceptionWhenUpdatingWithValueAboveMaximum() {
                // Arrange
                Measurement existingMeasurement = new Measurement();
                existingMeasurement.setId(1);
                existingMeasurement.setSeries(seriesWithBounds);
                existingMeasurement.setValue(new BigDecimal("50.00"));
                existingMeasurement.setTimestamp(testTimestamp);
                existingMeasurement.setCreatedBy(testUser);

                MeasurementRequest request = new MeasurementRequest(
                        null,
                        new BigDecimal("200.00"),
                        testTimestamp.plusHours(1)
                );

                when(measurementRepository.findById(1)).thenReturn(Optional.of(existingMeasurement));

                // Act & Assert
                assertThatThrownBy(() -> measurementService.updateMeasurement(1, request))
                        .isInstanceOf(ValidationException.class)
                        .hasMessageContaining("exceeds the maximum allowed value")
                        .hasMessageContaining("200.00")
                        .hasMessageContaining("100.00");

                verify(measurementRepository, never()).save(any(Measurement.class));
            }

            @Test
            @DisplayName("Should validate against new series bounds when changing series")
            void shouldValidateAgainstNewSeriesBoundsWhenChangingSeries() {
                // Arrange - existing measurement in unbounded series
                Measurement existingMeasurement = new Measurement();
                existingMeasurement.setId(1);
                existingMeasurement.setSeries(seriesNoBounds);
                existingMeasurement.setValue(new BigDecimal("500.00"));
                existingMeasurement.setTimestamp(testTimestamp);
                existingMeasurement.setCreatedBy(testUser);

                // Try to move to bounded series with a value that's out of bounds
                MeasurementRequest request = new MeasurementRequest(
                        2,
                        new BigDecimal("500.00"),
                        testTimestamp.plusHours(1)
                );

                when(measurementRepository.findById(1)).thenReturn(Optional.of(existingMeasurement));
                when(seriesRepository.findById(2)).thenReturn(Optional.of(seriesWithBounds));

                // Act & Assert
                assertThatThrownBy(() -> measurementService.updateMeasurement(1, request))
                        .isInstanceOf(ValidationException.class)
                        .hasMessageContaining("exceeds the maximum allowed value")
                        .hasMessageContaining("500.00")
                        .hasMessageContaining("100.00")
                        .hasMessageContaining("Bounded Temperature");

                verify(measurementRepository, never()).save(any(Measurement.class));
            }

            @Test
            @DisplayName("Should allow updating from bounded to unbounded series with out-of-bounds value")
            void shouldAllowUpdatingFromBoundedToUnboundedSeriesWithOutOfBoundsValue() {
                // Arrange - existing measurement in bounded series
                Measurement existingMeasurement = new Measurement();
                existingMeasurement.setId(1);
                existingMeasurement.setSeries(seriesWithBounds);
                existingMeasurement.setValue(new BigDecimal("50.00"));
                existingMeasurement.setTimestamp(testTimestamp);
                existingMeasurement.setCreatedBy(testUser);

                // Move to unbounded series with a previously invalid value
                MeasurementRequest request = new MeasurementRequest(
                        3,
                        new BigDecimal("500.00"),
                        testTimestamp.plusHours(1)
                );

                Measurement updatedMeasurement = new Measurement();
                updatedMeasurement.setId(1);
                updatedMeasurement.setSeries(seriesNoBounds);
                updatedMeasurement.setValue(new BigDecimal("500.00"));
                updatedMeasurement.setTimestamp(testTimestamp.plusHours(1));
                updatedMeasurement.setCreatedBy(testUser);

                when(measurementRepository.findById(1)).thenReturn(Optional.of(existingMeasurement));
                when(seriesRepository.findById(3)).thenReturn(Optional.of(seriesNoBounds));
                when(measurementRepository.save(any(Measurement.class))).thenReturn(updatedMeasurement);

                // Act
                MeasurementResponse result = measurementService.updateMeasurement(1, request);

                // Assert
                assertThat(result).isNotNull();
                assertThat(result.getValue()).isEqualByComparingTo(new BigDecimal("500.00"));
                verify(measurementRepository, times(1)).save(any(Measurement.class));
            }
        }

        @Nested
        @DisplayName("Series With Only Min or Only Max Bound")
        class PartialBoundsValidation {

            private Series seriesOnlyMin;
            private Series seriesOnlyMax;

            @BeforeEach
            void setUpPartialBounds() {
                // Series with only min bound
                seriesOnlyMin = new Series();
                seriesOnlyMin.setId(4);
                seriesOnlyMin.setName("Min Only Series");
                seriesOnlyMin.setMinValue(new BigDecimal("0.00"));
                seriesOnlyMin.setMaxValue(null);
                seriesOnlyMin.setCreatedBy(testUser);
                seriesOnlyMin.setCreatedAt(testTimestamp);

                // Series with only max bound
                seriesOnlyMax = new Series();
                seriesOnlyMax.setId(5);
                seriesOnlyMax.setName("Max Only Series");
                seriesOnlyMax.setMinValue(null);
                seriesOnlyMax.setMaxValue(new BigDecimal("100.00"));
                seriesOnlyMax.setCreatedBy(testUser);
                seriesOnlyMax.setCreatedAt(testTimestamp);
            }

            @Test
            @DisplayName("Should allow any value above min when only min is set")
            void shouldAllowAnyValueAboveMinWhenOnlyMinIsSet() {
                // Arrange
                MeasurementRequest request = new MeasurementRequest(
                        4,
                        new BigDecimal("999999.99"),
                        testTimestamp
                );

                Measurement savedMeasurement = new Measurement();
                savedMeasurement.setId(1);
                savedMeasurement.setSeries(seriesOnlyMin);
                savedMeasurement.setValue(new BigDecimal("999999.99"));
                savedMeasurement.setTimestamp(testTimestamp);
                savedMeasurement.setCreatedBy(testUser);

                when(userRepository.findByUsername("admin")).thenReturn(Optional.of(testUser));
                when(seriesRepository.findById(4)).thenReturn(Optional.of(seriesOnlyMin));
                when(measurementRepository.save(any(Measurement.class))).thenReturn(savedMeasurement);

                // Act
                MeasurementResponse result = measurementService.createMeasurement(request, "admin");

                // Assert
                assertThat(result).isNotNull();
                assertThat(result.getValue()).isEqualByComparingTo(new BigDecimal("999999.99"));
            }

            @Test
            @DisplayName("Should reject value below min when only min is set")
            void shouldRejectValueBelowMinWhenOnlyMinIsSet() {
                // Arrange
                MeasurementRequest request = new MeasurementRequest(
                        4,
                        new BigDecimal("-1.00"),
                        testTimestamp
                );

                when(userRepository.findByUsername("admin")).thenReturn(Optional.of(testUser));
                when(seriesRepository.findById(4)).thenReturn(Optional.of(seriesOnlyMin));

                // Act & Assert
                assertThatThrownBy(() -> measurementService.createMeasurement(request, "admin"))
                        .isInstanceOf(ValidationException.class)
                        .hasMessageContaining("below the minimum allowed value");

                verify(measurementRepository, never()).save(any(Measurement.class));
            }

            @Test
            @DisplayName("Should allow any value below max when only max is set")
            void shouldAllowAnyValueBelowMaxWhenOnlyMaxIsSet() {
                // Arrange
                MeasurementRequest request = new MeasurementRequest(
                        5,
                        new BigDecimal("-999999.99"),
                        testTimestamp
                );

                Measurement savedMeasurement = new Measurement();
                savedMeasurement.setId(1);
                savedMeasurement.setSeries(seriesOnlyMax);
                savedMeasurement.setValue(new BigDecimal("-999999.99"));
                savedMeasurement.setTimestamp(testTimestamp);
                savedMeasurement.setCreatedBy(testUser);

                when(userRepository.findByUsername("admin")).thenReturn(Optional.of(testUser));
                when(seriesRepository.findById(5)).thenReturn(Optional.of(seriesOnlyMax));
                when(measurementRepository.save(any(Measurement.class))).thenReturn(savedMeasurement);

                // Act
                MeasurementResponse result = measurementService.createMeasurement(request, "admin");

                // Assert
                assertThat(result).isNotNull();
                assertThat(result.getValue()).isEqualByComparingTo(new BigDecimal("-999999.99"));
            }

            @Test
            @DisplayName("Should reject value above max when only max is set")
            void shouldRejectValueAboveMaxWhenOnlyMaxIsSet() {
                // Arrange
                MeasurementRequest request = new MeasurementRequest(
                        5,
                        new BigDecimal("101.00"),
                        testTimestamp
                );

                when(userRepository.findByUsername("admin")).thenReturn(Optional.of(testUser));
                when(seriesRepository.findById(5)).thenReturn(Optional.of(seriesOnlyMax));

                // Act & Assert
                assertThatThrownBy(() -> measurementService.createMeasurement(request, "admin"))
                        .isInstanceOf(ValidationException.class)
                        .hasMessageContaining("exceeds the maximum allowed value");

                verify(measurementRepository, never()).save(any(Measurement.class));
            }
        }

        @Nested
        @DisplayName("BigDecimal Precision and Edge Cases")
        class BigDecimalPrecisionTests {

            @Test
            @DisplayName("Should handle values with different scales in comparison")
            void shouldHandleValuesWithDifferentScalesInComparison() {
                // Arrange - Series with 2 decimal places
                Series series = new Series();
                series.setId(6);
                series.setName("Precise Series");
                series.setMinValue(new BigDecimal("0.00"));
                series.setMaxValue(new BigDecimal("100.00"));
                series.setCreatedBy(testUser);

                // Value with 3 decimal places that should be valid
                MeasurementRequest request = new MeasurementRequest(
                        6,
                        new BigDecimal("50.123"),
                        testTimestamp
                );

                Measurement savedMeasurement = new Measurement();
                savedMeasurement.setId(1);
                savedMeasurement.setSeries(series);
                savedMeasurement.setValue(new BigDecimal("50.123"));
                savedMeasurement.setTimestamp(testTimestamp);
                savedMeasurement.setCreatedBy(testUser);

                when(userRepository.findByUsername("admin")).thenReturn(Optional.of(testUser));
                when(seriesRepository.findById(6)).thenReturn(Optional.of(series));
                when(measurementRepository.save(any(Measurement.class))).thenReturn(savedMeasurement);

                // Act
                MeasurementResponse result = measurementService.createMeasurement(request, "admin");

                // Assert
                assertThat(result).isNotNull();
                assertThat(result.getValue()).isEqualByComparingTo(new BigDecimal("50.123"));
            }

            @Test
            @DisplayName("Should correctly compare BigDecimal with different representations")
            void shouldCorrectlyCompareBigDecimalWithDifferentRepresentations() {
                // Arrange - Series with bounds
                Series series = new Series();
                series.setId(6);
                series.setName("Precise Series");
                series.setMinValue(new BigDecimal("0.00"));
                series.setMaxValue(new BigDecimal("100"));
                series.setCreatedBy(testUser);

                // Value equal to max but different representation
                MeasurementRequest request = new MeasurementRequest(
                        6,
                        new BigDecimal("100.00"),
                        testTimestamp
                );

                Measurement savedMeasurement = new Measurement();
                savedMeasurement.setId(1);
                savedMeasurement.setSeries(series);
                savedMeasurement.setValue(new BigDecimal("100.00"));
                savedMeasurement.setTimestamp(testTimestamp);
                savedMeasurement.setCreatedBy(testUser);

                when(userRepository.findByUsername("admin")).thenReturn(Optional.of(testUser));
                when(seriesRepository.findById(6)).thenReturn(Optional.of(series));
                when(measurementRepository.save(any(Measurement.class))).thenReturn(savedMeasurement);

                // Act
                MeasurementResponse result = measurementService.createMeasurement(request, "admin");

                // Assert - Should pass because compareTo handles different scales correctly
                assertThat(result).isNotNull();
                assertThat(result.getValue()).isEqualByComparingTo(new BigDecimal("100"));
            }

            @Test
            @DisplayName("Should handle zero value correctly")
            void shouldHandleZeroValueCorrectly() {
                // Arrange
                MeasurementRequest request = new MeasurementRequest(
                        2,
                        BigDecimal.ZERO,
                        testTimestamp
                );

                Measurement savedMeasurement = new Measurement();
                savedMeasurement.setId(1);
                savedMeasurement.setSeries(seriesWithBounds);
                savedMeasurement.setValue(BigDecimal.ZERO);
                savedMeasurement.setTimestamp(testTimestamp);
                savedMeasurement.setCreatedBy(testUser);

                when(userRepository.findByUsername("admin")).thenReturn(Optional.of(testUser));
                when(seriesRepository.findById(2)).thenReturn(Optional.of(seriesWithBounds));
                when(measurementRepository.save(any(Measurement.class))).thenReturn(savedMeasurement);

                // Act
                MeasurementResponse result = measurementService.createMeasurement(request, "admin");

                // Assert
                assertThat(result).isNotNull();
                assertThat(result.getValue()).isEqualByComparingTo(BigDecimal.ZERO);
            }
        }
    }
}
