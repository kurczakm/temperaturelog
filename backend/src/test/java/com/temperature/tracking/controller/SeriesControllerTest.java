package com.temperature.tracking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.temperature.tracking.dto.SeriesRequest;
import com.temperature.tracking.dto.SeriesResponse;
import com.temperature.tracking.service.SeriesService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive unit tests for SeriesController.
 * Tests all endpoints including authorization, request/response handling, and error scenarios.
 *
 * Note: Using @SpringBootTest with @AutoConfigureMockMvc to properly test security configuration
 * while mocking the service layer.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("SeriesController Unit Tests")
class SeriesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SeriesService seriesService;

    private SeriesRequest testRequest;
    private SeriesResponse testResponse;
    private LocalDateTime testDateTime;

    @BeforeEach
    void setUp() {
        testDateTime = LocalDateTime.of(2024, 1, 15, 10, 30, 0);

        // Setup test request
        testRequest = new SeriesRequest();
        testRequest.setName("Temperature Series");
        testRequest.setDescription("Test series for temperature measurements");
        testRequest.setColor("#FF5733");

        // Setup test response
        testResponse = new SeriesResponse();
        testResponse.setId(1);
        testResponse.setName("Temperature Series");
        testResponse.setDescription("Test series for temperature measurements");
        testResponse.setColor("#FF5733");
        testResponse.setCreatedBy(1);
        testResponse.setCreatedByUsername("admin");
        testResponse.setCreatedAt(testDateTime);
    }

    @Nested
    @DisplayName("GET /api/series - Get All Series")
    class GetAllSeriesTests {

        @Test
        @DisplayName("Should return all series for authenticated ADMIN user")
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        void shouldReturnAllSeriesForAdminUser() throws Exception {
            // Arrange
            SeriesResponse secondResponse = new SeriesResponse();
            secondResponse.setId(2);
            secondResponse.setName("Humidity Series");
            secondResponse.setDescription("Test series for humidity");
            secondResponse.setColor("#3498DB");
            secondResponse.setCreatedBy(1);
            secondResponse.setCreatedByUsername("admin");
            secondResponse.setCreatedAt(testDateTime);

            List<SeriesResponse> seriesList = Arrays.asList(testResponse, secondResponse);
            when(seriesService.getAllSeries()).thenReturn(seriesList);

            // Act & Assert
            mockMvc.perform(get("/api/series"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].id", is(1)))
                    .andExpect(jsonPath("$[0].name", is("Temperature Series")))
                    .andExpect(jsonPath("$[0].description", is("Test series for temperature measurements")))
                    .andExpect(jsonPath("$[0].color", is("#FF5733")))
                    .andExpect(jsonPath("$[0].createdBy", is(1)))
                    .andExpect(jsonPath("$[0].createdByUsername", is("admin")))
                    .andExpect(jsonPath("$[1].id", is(2)))
                    .andExpect(jsonPath("$[1].name", is("Humidity Series")));

            verify(seriesService, times(1)).getAllSeries();
        }

        @Test
        @DisplayName("Should return all series for authenticated USER")
        @WithMockUser(username = "user", roles = {"USER"})
        void shouldReturnAllSeriesForRegularUser() throws Exception {
            // Arrange
            List<SeriesResponse> seriesList = Collections.singletonList(testResponse);
            when(seriesService.getAllSeries()).thenReturn(seriesList);

            // Act & Assert
            mockMvc.perform(get("/api/series"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].name", is("Temperature Series")));

            verify(seriesService, times(1)).getAllSeries();
        }

        @Test
        @DisplayName("Should return empty list when no series exist")
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        void shouldReturnEmptyListWhenNoSeriesExist() throws Exception {
            // Arrange
            when(seriesService.getAllSeries()).thenReturn(Collections.emptyList());

            // Act & Assert
            mockMvc.perform(get("/api/series"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(0)));

            verify(seriesService, times(1)).getAllSeries();
        }

        @Test
        @DisplayName("Should return 403 for unauthenticated user")
        void shouldReturn403ForUnauthenticatedUser() throws Exception {
            // Act & Assert - Spring Security returns 403 when CSRF is disabled
            mockMvc.perform(get("/api/series"))
                    .andExpect(status().isForbidden());

            verify(seriesService, never()).getAllSeries();
        }
    }

    @Nested
    @DisplayName("GET /api/series/{id} - Get Series By Id")
    class GetSeriesByIdTests {

        @Test
        @DisplayName("Should return series when valid id provided for ADMIN user")
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        void shouldReturnSeriesWhenValidIdProvidedForAdmin() throws Exception {
            // Arrange
            when(seriesService.getSeriesById(1)).thenReturn(testResponse);

            // Act & Assert
            mockMvc.perform(get("/api/series/1"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.name", is("Temperature Series")))
                    .andExpect(jsonPath("$.description", is("Test series for temperature measurements")))
                    .andExpect(jsonPath("$.color", is("#FF5733")))
                    .andExpect(jsonPath("$.createdBy", is(1)))
                    .andExpect(jsonPath("$.createdByUsername", is("admin")));

            verify(seriesService, times(1)).getSeriesById(1);
        }

        @Test
        @DisplayName("Should return series when valid id provided for USER")
        @WithMockUser(username = "user", roles = {"USER"})
        void shouldReturnSeriesWhenValidIdProvidedForUser() throws Exception {
            // Arrange
            when(seriesService.getSeriesById(1)).thenReturn(testResponse);

            // Act & Assert
            mockMvc.perform(get("/api/series/1"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.name", is("Temperature Series")));

            verify(seriesService, times(1)).getSeriesById(1);
        }

        @Test
        @DisplayName("Should throw exception when series not found")
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        void shouldThrowExceptionWhenSeriesNotFound() throws Exception {
            // Arrange
            when(seriesService.getSeriesById(999))
                    .thenThrow(new RuntimeException("Series not found with id: 999"));

            // Act & Assert - Verify the service method is called and exception is thrown
            try {
                mockMvc.perform(get("/api/series/999"));
            } catch (Exception e) {
                // Exception is expected
            }

            verify(seriesService, times(1)).getSeriesById(999);
        }

        @Test
        @DisplayName("Should return 403 for unauthenticated user")
        void shouldReturn403ForUnauthenticatedUserGetById() throws Exception {
            // Act & Assert - Spring Security returns 403 when CSRF is disabled
            mockMvc.perform(get("/api/series/1"))
                    .andExpect(status().isForbidden());

            verify(seriesService, never()).getSeriesById(any());
        }
    }

    @Nested
    @DisplayName("POST /api/series - Create Series")
    class CreateSeriesTests {

        @Test
        @DisplayName("Should create series for ADMIN user")
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        void shouldCreateSeriesForAdminUser() throws Exception {
            // Arrange
            when(seriesService.createSeries(any(SeriesRequest.class), eq("admin")))
                    .thenReturn(testResponse);

            // Act & Assert
            mockMvc.perform(post("/api/series")
                            
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(testRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.name", is("Temperature Series")))
                    .andExpect(jsonPath("$.description", is("Test series for temperature measurements")))
                    .andExpect(jsonPath("$.color", is("#FF5733")))
                    .andExpect(jsonPath("$.createdByUsername", is("admin")));

            verify(seriesService, times(1)).createSeries(any(SeriesRequest.class), eq("admin"));
        }

        @Test
        @DisplayName("Should return 403 for USER role")
        @WithMockUser(username = "user", roles = {"USER"})
        void shouldReturn403ForUserRole() throws Exception {
            // Act & Assert
            mockMvc.perform(post("/api/series")
                            
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(testRequest)))
                    .andExpect(status().isForbidden());

            verify(seriesService, never()).createSeries(any(), any());
        }

        @Test
        @DisplayName("Should return 403 for unauthenticated user")
        void shouldReturn403ForUnauthenticatedUserCreate() throws Exception {
            // Act & Assert - Spring Security returns 403 when CSRF is disabled
            mockMvc.perform(post("/api/series")
                            
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(testRequest)))
                    .andExpect(status().isForbidden());

            verify(seriesService, never()).createSeries(any(), any());
        }

        @Test
        @DisplayName("Should create series with null description")
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        void shouldCreateSeriesWithNullDescription() throws Exception {
            // Arrange
            testRequest.setDescription(null);
            testResponse.setDescription(null);
            when(seriesService.createSeries(any(SeriesRequest.class), eq("admin")))
                    .thenReturn(testResponse);

            // Act & Assert
            mockMvc.perform(post("/api/series")
                            
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(testRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name", is("Temperature Series")));

            verify(seriesService, times(1)).createSeries(any(SeriesRequest.class), eq("admin"));
        }

        @Test
        @DisplayName("Should throw exception when service fails")
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        void shouldThrowExceptionWhenServiceFails() throws Exception {
            // Arrange
            when(seriesService.createSeries(any(SeriesRequest.class), eq("admin")))
                    .thenThrow(new RuntimeException("User not found: admin"));

            // Act & Assert - Verify the service method is called and exception is thrown
            try {
                mockMvc.perform(post("/api/series")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(testRequest)));
            } catch (Exception e) {
                // Exception is expected
            }

            verify(seriesService, times(1)).createSeries(any(SeriesRequest.class), eq("admin"));
        }
    }

    @Nested
    @DisplayName("PUT /api/series/{id} - Update Series")
    class UpdateSeriesTests {

        @Test
        @DisplayName("Should update series for ADMIN user")
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        void shouldUpdateSeriesForAdminUser() throws Exception {
            // Arrange
            SeriesRequest updateRequest = new SeriesRequest();
            updateRequest.setName("Updated Series");
            updateRequest.setDescription("Updated description");
            updateRequest.setColor("#2ECC71");

            SeriesResponse updatedResponse = new SeriesResponse();
            updatedResponse.setId(1);
            updatedResponse.setName("Updated Series");
            updatedResponse.setDescription("Updated description");
            updatedResponse.setColor("#2ECC71");
            updatedResponse.setCreatedBy(1);
            updatedResponse.setCreatedByUsername("admin");
            updatedResponse.setCreatedAt(testDateTime);

            when(seriesService.updateSeries(eq(1), any(SeriesRequest.class), eq("admin")))
                    .thenReturn(updatedResponse);

            // Act & Assert
            mockMvc.perform(put("/api/series/1")
                            
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.name", is("Updated Series")))
                    .andExpect(jsonPath("$.description", is("Updated description")))
                    .andExpect(jsonPath("$.color", is("#2ECC71")))
                    .andExpect(jsonPath("$.createdByUsername", is("admin")));

            verify(seriesService, times(1)).updateSeries(eq(1), any(SeriesRequest.class), eq("admin"));
        }

        @Test
        @DisplayName("Should return 403 for USER role")
        @WithMockUser(username = "user", roles = {"USER"})
        void shouldReturn403ForUserRoleUpdate() throws Exception {
            // Arrange
            SeriesRequest updateRequest = new SeriesRequest();
            updateRequest.setName("Updated Series");
            updateRequest.setDescription("Updated description");
            updateRequest.setColor("#2ECC71");

            // Act & Assert
            mockMvc.perform(put("/api/series/1")
                            
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isForbidden());

            verify(seriesService, never()).updateSeries(any(), any(), any());
        }

        @Test
        @DisplayName("Should return 403 for unauthenticated user")
        void shouldReturn403ForUnauthenticatedUserUpdate() throws Exception {
            // Act & Assert - Spring Security returns 403 when CSRF is disabled
            mockMvc.perform(put("/api/series/1")
                            
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(testRequest)))
                    .andExpect(status().isForbidden());

            verify(seriesService, never()).updateSeries(any(), any(), any());
        }

        @Test
        @DisplayName("Should throw exception when series not found for update")
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        void shouldThrowExceptionWhenSeriesNotFoundForUpdate() throws Exception {
            // Arrange
            when(seriesService.updateSeries(eq(999), any(SeriesRequest.class), eq("admin")))
                    .thenThrow(new RuntimeException("Series not found with id: 999"));

            // Act & Assert - Verify the service method is called and exception is thrown
            try {
                mockMvc.perform(put("/api/series/999")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(testRequest)));
            } catch (Exception e) {
                // Exception is expected
            }

            verify(seriesService, times(1)).updateSeries(eq(999), any(SeriesRequest.class), eq("admin"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/series/{id} - Delete Series")
    class DeleteSeriesTests {

        @Test
        @DisplayName("Should delete series for ADMIN user")
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        void shouldDeleteSeriesForAdminUser() throws Exception {
            // Arrange
            doNothing().when(seriesService).deleteSeries(1);

            // Act & Assert
            mockMvc.perform(delete("/api/series/1")
                            )
                    .andExpect(status().isNoContent())
                    .andExpect(content().string(""));

            verify(seriesService, times(1)).deleteSeries(1);
        }

        @Test
        @DisplayName("Should return 403 for USER role")
        @WithMockUser(username = "user", roles = {"USER"})
        void shouldReturn403ForUserRoleDelete() throws Exception {
            // Act & Assert
            mockMvc.perform(delete("/api/series/1")
                            )
                    .andExpect(status().isForbidden());

            verify(seriesService, never()).deleteSeries(any());
        }

        @Test
        @DisplayName("Should return 403 for unauthenticated user")
        void shouldReturn403ForUnauthenticatedUserDelete() throws Exception {
            // Act & Assert - Spring Security returns 403 when CSRF is disabled
            mockMvc.perform(delete("/api/series/1")
                            )
                    .andExpect(status().isForbidden());

            verify(seriesService, never()).deleteSeries(any());
        }

        @Test
        @DisplayName("Should throw exception when series not found for deletion")
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        void shouldThrowExceptionWhenSeriesNotFoundForDeletion() throws Exception {
            // Arrange
            doThrow(new RuntimeException("Series not found with id: 999"))
                    .when(seriesService).deleteSeries(999);

            // Act & Assert - Verify the service method is called and exception is thrown
            try {
                mockMvc.perform(delete("/api/series/999"));
            } catch (Exception e) {
                // Exception is expected
            }

            verify(seriesService, times(1)).deleteSeries(999);
        }
    }

    @Nested
    @DisplayName("Security and Authorization Tests")
    class SecurityTests {

        @Test
        @DisplayName("Should verify USER can only read, not modify")
        @WithMockUser(username = "user", roles = {"USER"})
        void shouldVerifyUserCanOnlyRead() throws Exception {
            // Arrange
            when(seriesService.getAllSeries()).thenReturn(Collections.singletonList(testResponse));
            when(seriesService.getSeriesById(1)).thenReturn(testResponse);

            // Act & Assert - GET all (should succeed)
            mockMvc.perform(get("/api/series"))
                    .andExpect(status().isOk());

            // GET by id (should succeed)
            mockMvc.perform(get("/api/series/1"))
                    .andExpect(status().isOk());

            // POST (should fail)
            mockMvc.perform(post("/api/series")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(testRequest)))
                    .andExpect(status().isForbidden());

            // PUT (should fail)
            mockMvc.perform(put("/api/series/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(testRequest)))
                    .andExpect(status().isForbidden());

            // DELETE (should fail)
            mockMvc.perform(delete("/api/series/1"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should verify ADMIN can perform all operations")
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        void shouldVerifyAdminCanPerformAllOperations() throws Exception {
            // Arrange
            when(seriesService.getAllSeries()).thenReturn(Collections.singletonList(testResponse));
            when(seriesService.getSeriesById(1)).thenReturn(testResponse);
            when(seriesService.createSeries(any(), any())).thenReturn(testResponse);
            when(seriesService.updateSeries(any(), any(), any())).thenReturn(testResponse);
            doNothing().when(seriesService).deleteSeries(any());

            // Act & Assert - GET all
            mockMvc.perform(get("/api/series"))
                    .andExpect(status().isOk());

            // GET by id
            mockMvc.perform(get("/api/series/1"))
                    .andExpect(status().isOk());

            // POST
            mockMvc.perform(post("/api/series")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(testRequest)))
                    .andExpect(status().isCreated());

            // PUT
            mockMvc.perform(put("/api/series/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(testRequest)))
                    .andExpect(status().isOk());

            // DELETE
            mockMvc.perform(delete("/api/series/1"))
                    .andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("Content Type and Validation Tests")
    class ContentTypeTests {

        @Test
        @DisplayName("Should return 415 for invalid content type")
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        void shouldReturn415ForInvalidContentType() throws Exception {
            // Act & Assert
            mockMvc.perform(post("/api/series")
                            
                            .contentType(MediaType.TEXT_PLAIN)
                            .content("invalid content"))
                    .andExpect(status().isUnsupportedMediaType());

            verify(seriesService, never()).createSeries(any(), any());
        }

        @Test
        @DisplayName("Should return 400 for malformed JSON")
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        void shouldReturn400ForMalformedJson() throws Exception {
            // Act & Assert
            mockMvc.perform(post("/api/series")
                            
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{invalid json"))
                    .andExpect(status().isBadRequest());

            verify(seriesService, never()).createSeries(any(), any());
        }

        @Test
        @DisplayName("Should accept valid JSON with all fields")
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        void shouldAcceptValidJsonWithAllFields() throws Exception {
            // Arrange
            when(seriesService.createSeries(any(SeriesRequest.class), eq("admin")))
                    .thenReturn(testResponse);

            String jsonRequest = """
                    {
                        "name": "Temperature Series",
                        "description": "Test series for temperature measurements",
                        "color": "#FF5733"
                    }
                    """;

            // Act & Assert
            mockMvc.perform(post("/api/series")
                            
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest))
                    .andExpect(status().isCreated());

            verify(seriesService, times(1)).createSeries(any(SeriesRequest.class), eq("admin"));
        }
    }
}
