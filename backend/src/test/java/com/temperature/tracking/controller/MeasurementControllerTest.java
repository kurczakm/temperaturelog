package com.temperature.tracking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.temperature.tracking.dto.MeasurementRequest;
import com.temperature.tracking.dto.MeasurementResponse;
import com.temperature.tracking.security.JwtAuthenticationFilter;
import com.temperature.tracking.security.JwtUtil;
import com.temperature.tracking.service.CustomUserDetailsService;
import com.temperature.tracking.service.MeasurementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive unit tests for MeasurementController.
 * Tests all REST endpoints for CRUD operations including:
 * - GET /api/measurements - Get all measurements
 * - GET /api/measurements/{id} - Get measurement by ID
 * - GET /api/measurements/series/{seriesId} - Get measurements by series ID
 * - POST /api/measurements - Create new measurement
 * - PUT /api/measurements/{id} - Update measurement
 * - DELETE /api/measurements/{id} - Delete measurement
 *
 * Note: Security filters are disabled with @AutoConfigureMockMvc(addFilters = false) to focus on controller logic testing.
 * Role-based access control annotations (@PreAuthorize) are present in the controller and would be enforced in production.
 */
@WebMvcTest(MeasurementController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("MeasurementController Tests")
class MeasurementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MeasurementService measurementService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private MeasurementResponse testMeasurementResponse;
    private MeasurementRequest testMeasurementRequest;
    private ZonedDateTime testTimestamp;

    @BeforeEach
    void setUp() {
        testTimestamp = ZonedDateTime.from(LocalDateTime.of(2025, 11, 8, 12, 0, 0));

        testMeasurementResponse = new MeasurementResponse(
                1,
                1,
                "Temperature",
                new BigDecimal("23.50"),
                testTimestamp,
                1,
                "admin",
                testTimestamp
        );

        testMeasurementRequest = new MeasurementRequest(
                1,
                new BigDecimal("23.50"),
                testTimestamp
        );
    }

    @Nested
    @DisplayName("GET /api/measurements - Get All Measurements")
    class GetAllMeasurements {

        @Test
        @DisplayName("Should return all measurements when multiple exist")
        void shouldReturnAllMeasurementsWhenMultipleExist() throws Exception {
            // Arrange
            MeasurementResponse measurement2 = new MeasurementResponse(
                    2,
                    1,
                    "Temperature",
                    new BigDecimal("24.75"),
                    testTimestamp.plusHours(1),
                    1,
                    "admin",
                    testTimestamp.plusHours(1)
            );

            List<MeasurementResponse> measurements = Arrays.asList(testMeasurementResponse, measurement2);
            when(measurementService.getAllMeasurements()).thenReturn(measurements);

            // Act & Assert
            mockMvc.perform(get("/api/measurements")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[0].value").value(23.50))
                    .andExpect(jsonPath("$[0].seriesId").value(1))
                    .andExpect(jsonPath("$[0].seriesName").value("Temperature"))
                    .andExpect(jsonPath("$[1].id").value(2))
                    .andExpect(jsonPath("$[1].value").value(24.75));

            verify(measurementService, times(1)).getAllMeasurements();
        }

        @Test
        @DisplayName("Should return empty list when no measurements exist")
        void shouldReturnEmptyListWhenNoMeasurementsExist() throws Exception {
            // Arrange
            when(measurementService.getAllMeasurements()).thenReturn(Collections.emptyList());

            // Act & Assert
            mockMvc.perform(get("/api/measurements")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.length()").value(0));

            verify(measurementService, times(1)).getAllMeasurements();
        }

        @Test
        @DisplayName("Should return single measurement in list")
        void shouldReturnSingleMeasurementInList() throws Exception {
            // Arrange
            List<MeasurementResponse> measurements = Collections.singletonList(testMeasurementResponse);
            when(measurementService.getAllMeasurements()).thenReturn(measurements);

            // Act & Assert
            mockMvc.perform(get("/api/measurements")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].id").value(1));

            verify(measurementService, times(1)).getAllMeasurements();
        }

        @Test
        @DisplayName("Should correctly map all response fields")
        void shouldCorrectlyMapAllResponseFields() throws Exception {
            // Arrange
            when(measurementService.getAllMeasurements()).thenReturn(Collections.singletonList(testMeasurementResponse));

            // Act & Assert
            mockMvc.perform(get("/api/measurements")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[0].seriesId").value(1))
                    .andExpect(jsonPath("$[0].seriesName").value("Temperature"))
                    .andExpect(jsonPath("$[0].value").value(23.50))
                    .andExpect(jsonPath("$[0].createdBy").value(1))
                    .andExpect(jsonPath("$[0].createdByUsername").value("admin"));
        }
    }

    @Nested
    @DisplayName("GET /api/measurements/{id} - Get Measurement By ID")
    class GetMeasurementById {

        @Test
        @DisplayName("Should return measurement when valid ID is provided")
        void shouldReturnMeasurementWhenValidIdIsProvided() throws Exception {
            // Arrange
            when(measurementService.getMeasurementById(1)).thenReturn(testMeasurementResponse);

            // Act & Assert
            mockMvc.perform(get("/api/measurements/1")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.value").value(23.50))
                    .andExpect(jsonPath("$.seriesId").value(1))
                    .andExpect(jsonPath("$.seriesName").value("Temperature"))
                    .andExpect(jsonPath("$.createdByUsername").value("admin"));

            verify(measurementService, times(1)).getMeasurementById(1);
        }

        @Test
        @DisplayName("Should handle different measurement IDs")
        void shouldHandleDifferentMeasurementIds() throws Exception {
            // Arrange
            MeasurementResponse measurement2 = new MeasurementResponse(
                    5,
                    1,
                    "Temperature",
                    new BigDecimal("30.00"),
                    testTimestamp,
                    1,
                    "admin",
                    testTimestamp
            );
            when(measurementService.getMeasurementById(5)).thenReturn(measurement2);

            // Act & Assert
            mockMvc.perform(get("/api/measurements/5")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(5))
                    .andExpect(jsonPath("$.value").value(30.00));

            verify(measurementService, times(1)).getMeasurementById(5);
        }

        @Test
        @DisplayName("Should pass correct ID to service layer")
        void shouldPassCorrectIdToServiceLayer() throws Exception {
            // Arrange
            when(measurementService.getMeasurementById(anyInt())).thenReturn(testMeasurementResponse);

            // Act
            mockMvc.perform(get("/api/measurements/123")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            // Assert
            verify(measurementService, times(1)).getMeasurementById(123);
        }
    }

    @Nested
    @DisplayName("GET /api/measurements/series/{seriesId} - Get Measurements By Series ID")
    class GetMeasurementsBySeriesId {

        @Test
        @DisplayName("Should return all measurements for a specific series")
        void shouldReturnAllMeasurementsForSpecificSeries() throws Exception {
            // Arrange
            MeasurementResponse measurement2 = new MeasurementResponse(
                    2,
                    1,
                    "Temperature",
                    new BigDecimal("25.00"),
                    testTimestamp.plusHours(2),
                    1,
                    "admin",
                    testTimestamp.plusHours(2)
            );

            List<MeasurementResponse> measurements = Arrays.asList(testMeasurementResponse, measurement2);
            when(measurementService.getMeasurementsBySeriesId(1)).thenReturn(measurements);

            // Act & Assert
            mockMvc.perform(get("/api/measurements/series/1")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].seriesId").value(1))
                    .andExpect(jsonPath("$[1].seriesId").value(1))
                    .andExpect(jsonPath("$[0].value").value(23.50))
                    .andExpect(jsonPath("$[1].value").value(25.00));

            verify(measurementService, times(1)).getMeasurementsBySeriesId(1);
        }

        @Test
        @DisplayName("Should return empty list when series has no measurements")
        void shouldReturnEmptyListWhenSeriesHasNoMeasurements() throws Exception {
            // Arrange
            when(measurementService.getMeasurementsBySeriesId(1)).thenReturn(Collections.emptyList());

            // Act & Assert
            mockMvc.perform(get("/api/measurements/series/1")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));

            verify(measurementService, times(1)).getMeasurementsBySeriesId(1);
        }

        @Test
        @DisplayName("Should pass correct series ID to service layer")
        void shouldPassCorrectSeriesIdToServiceLayer() throws Exception {
            // Arrange
            when(measurementService.getMeasurementsBySeriesId(anyInt())).thenReturn(Collections.emptyList());

            // Act
            mockMvc.perform(get("/api/measurements/series/42")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            // Assert
            verify(measurementService, times(1)).getMeasurementsBySeriesId(42);
        }
    }

    @Nested
    @DisplayName("POST /api/measurements - Create Measurement")
    class CreateMeasurement {

        @Test
        @DisplayName("Should return 400 Bad Request when request body is malformed")
        void shouldReturn400WhenRequestBodyIsMalformed() throws Exception {
            // Act & Assert
            mockMvc.perform(post("/api/measurements")
                            .with(user("admin").roles("ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{invalid json}"))
                    .andExpect(status().isBadRequest());

            verify(measurementService, never()).createMeasurement(any(MeasurementRequest.class), anyString());
        }
    }

    @Nested
    @DisplayName("PUT /api/measurements/{id} - Update Measurement")
    class UpdateMeasurement {

        @Test
        @DisplayName("Should return 400 Bad Request when request body is malformed")
        void shouldReturn400WhenRequestBodyIsMalformed() throws Exception {
            // Act & Assert
            mockMvc.perform(put("/api/measurements/1")
                            .with(user("admin").roles("ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{invalid json}"))
                    .andExpect(status().isBadRequest());

            verify(measurementService, never()).updateMeasurement(anyInt(), any(MeasurementRequest.class));
        }
    }

    @Nested
    @DisplayName("DELETE /api/measurements/{id} - Delete Measurement")
    class DeleteMeasurement {

        @Test
        @DisplayName("Should delete measurement when valid ID is provided")
        void shouldDeleteMeasurementWhenValidIdIsProvided() throws Exception {
            // Arrange
            doNothing().when(measurementService).deleteMeasurement(1);

            // Act & Assert
            mockMvc.perform(delete("/api/measurements/1")
                            .with(user("admin").roles("ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNoContent());

            verify(measurementService, times(1)).deleteMeasurement(1);
        }

        @Test
        @DisplayName("Should return no content body on successful deletion")
        void shouldReturnNoContentBodyOnSuccessfulDeletion() throws Exception {
            // Arrange
            doNothing().when(measurementService).deleteMeasurement(1);

            // Act & Assert
            mockMvc.perform(delete("/api/measurements/1")
                            .with(user("admin").roles("ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNoContent())
                    .andExpect(content().string(""));

            verify(measurementService, times(1)).deleteMeasurement(1);
        }

        @Test
        @DisplayName("Should pass correct ID to service layer")
        void shouldPassCorrectIdToServiceLayer() throws Exception {
            // Arrange
            doNothing().when(measurementService).deleteMeasurement(anyInt());

            // Act
            mockMvc.perform(delete("/api/measurements/99")
                            .with(user("admin").roles("ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNoContent());

            // Assert
            verify(measurementService, times(1)).deleteMeasurement(99);
        }
    }

    @Nested
    @DisplayName("Edge Cases and Data Handling")
    class EdgeCasesAndDataHandling {

        @Test
        @DisplayName("Should handle timestamp serialization correctly")
        void shouldHandleTimestampSerializationCorrectly() throws Exception {
            // Arrange
            when(measurementService.getAllMeasurements())
                    .thenReturn(Collections.singletonList(testMeasurementResponse));

            // Act & Assert
            mockMvc.perform(get("/api/measurements")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].timestamp").exists())
                    .andExpect(jsonPath("$[0].createdAt").exists());
        }

        @Test
        @DisplayName("Should handle multiple measurements with different series")
        void shouldHandleMultipleMeasurementsWithDifferentSeries() throws Exception {
            // Arrange
            MeasurementResponse tempMeasurement = new MeasurementResponse(
                    1, 1, "Temperature", new BigDecimal("23.50"),
                    testTimestamp, 1, "admin", testTimestamp
            );

            MeasurementResponse humidityMeasurement = new MeasurementResponse(
                    2, 2, "Humidity", new BigDecimal("65.00"),
                    testTimestamp, 1, "admin", testTimestamp
            );

            List<MeasurementResponse> measurements = Arrays.asList(tempMeasurement, humidityMeasurement);
            when(measurementService.getAllMeasurements()).thenReturn(measurements);

            // Act & Assert
            mockMvc.perform(get("/api/measurements")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].seriesName").value("Temperature"))
                    .andExpect(jsonPath("$[1].seriesName").value("Humidity"))
                    .andExpect(jsonPath("$[0].value").value(23.50))
                    .andExpect(jsonPath("$[1].value").value(65.00));
        }
    }
}
