package com.temperature.tracking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.temperature.tracking.dto.LoginRequest;
import com.temperature.tracking.dto.LoginResponse;
import com.temperature.tracking.security.JwtAuthenticationFilter;
import com.temperature.tracking.security.JwtUtil;
import com.temperature.tracking.service.AuthService;
import com.temperature.tracking.service.CustomUserDetailsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive unit tests for AuthController.
 * Tests the /api/auth/signin endpoint for various scenarios including:
 * - Successful authentication
 * - Invalid credentials
 * - Validation errors
 * - Internal server errors
 *
 * Note: @WithMockUser is used to bypass Spring Security for testing the controller logic.
 */
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AuthController Tests")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Nested
    @DisplayName("POST /api/auth/signin - Successful Authentication")
    class SuccessfulAuthentication {

        @Test
        @DisplayName("Should return 200 OK with valid credentials and ADMIN role")
        void shouldReturnOkWithValidCredentialsForAdmin() throws Exception {
            // Arrange
            LoginRequest request = new LoginRequest("admin", "password123");
            LoginResponse response = new LoginResponse(
                    "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test",
                    "admin",
                    "ADMIN",
                    86400000L
            );

            when(authService.login(any(LoginRequest.class))).thenReturn(response);

            // Act & Assert
            mockMvc.perform(post("/api/auth/signin")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.token").value("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test"))
                    .andExpect(jsonPath("$.username").value("admin"))
                    .andExpect(jsonPath("$.role").value("ADMIN"))
                    .andExpect(jsonPath("$.expiresIn").value(86400000L));

            verify(authService, times(1)).login(any(LoginRequest.class));
        }

        @Test
        @DisplayName("Should return 200 OK with valid credentials and USER role")
        void shouldReturnOkWithValidCredentialsForUser() throws Exception {
            // Arrange
            LoginRequest request = new LoginRequest("user", "password123");
            LoginResponse response = new LoginResponse(
                    "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.userToken",
                    "user",
                    "USER",
                    86400000L
            );

            when(authService.login(any(LoginRequest.class))).thenReturn(response);

            // Act & Assert
            mockMvc.perform(post("/api/auth/signin")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.token").value("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.userToken"))
                    .andExpect(jsonPath("$.username").value("user"))
                    .andExpect(jsonPath("$.role").value("USER"))
                    .andExpect(jsonPath("$.expiresIn").value(86400000L));

            verify(authService, times(1)).login(any(LoginRequest.class));
        }

        @Test
        @DisplayName("Should handle different expiration times correctly")
        void shouldHandleDifferentExpirationTimes() throws Exception {
            // Arrange
            LoginRequest request = new LoginRequest("admin", "password123");
            Long customExpiration = 3600000L; // 1 hour
            LoginResponse response = new LoginResponse(
                    "token123",
                    "admin",
                    "ADMIN",
                    customExpiration
            );

            when(authService.login(any(LoginRequest.class))).thenReturn(response);

            // Act & Assert
            mockMvc.perform(post("/api/auth/signin")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.expiresIn").value(customExpiration));
        }
    }

    @Nested
    @DisplayName("POST /api/auth/signin - Failed Authentication")
    class FailedAuthentication {

        @Test
        @DisplayName("Should return 401 Unauthorized with invalid username")
        void shouldReturn401WithInvalidUsername() throws Exception {
            // Arrange
            LoginRequest request = new LoginRequest("nonexistent", "password123");

            when(authService.login(any(LoginRequest.class)))
                    .thenThrow(new BadCredentialsException("Invalid username or password"));

            // Act & Assert
            mockMvc.perform(post("/api/auth/signin")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().string("Invalid username or password"));

            verify(authService, times(1)).login(any(LoginRequest.class));
        }

        @Test
        @DisplayName("Should return 401 Unauthorized with invalid password")
        void shouldReturn401WithInvalidPassword() throws Exception {
            // Arrange
            LoginRequest request = new LoginRequest("admin", "wrongpassword");

            when(authService.login(any(LoginRequest.class)))
                    .thenThrow(new BadCredentialsException("Invalid username or password"));

            // Act & Assert
            mockMvc.perform(post("/api/auth/signin")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().string("Invalid username or password"));

            verify(authService, times(1)).login(any(LoginRequest.class));
        }

        @Test
        @DisplayName("Should return 400 Bad Request with empty credentials due to validation")
        void shouldReturn400WithEmptyCredentials() throws Exception {
            // Arrange
            LoginRequest request = new LoginRequest("", "");

            // Act & Assert - Empty strings fail @NotBlank validation before reaching service
            mockMvc.perform(post("/api/auth/signin")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            // Service should not be called due to validation failure
            verify(authService, never()).login(any(LoginRequest.class));
        }
    }

    @Nested
    @DisplayName("POST /api/auth/signin - Validation Errors")
    class ValidationErrors {

        @Test
        @DisplayName("Should return 400 Bad Request when username is blank")
        void shouldReturn400WhenUsernameIsBlank() throws Exception {
            // Arrange
            LoginRequest request = new LoginRequest("", "password123");

            // Act & Assert
            mockMvc.perform(post("/api/auth/signin")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verify(authService, never()).login(any(LoginRequest.class));
        }

        @Test
        @DisplayName("Should return 400 Bad Request when password is blank")
        void shouldReturn400WhenPasswordIsBlank() throws Exception {
            // Arrange
            LoginRequest request = new LoginRequest("admin", "");

            // Act & Assert
            mockMvc.perform(post("/api/auth/signin")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verify(authService, never()).login(any(LoginRequest.class));
        }

        @Test
        @DisplayName("Should return 400 Bad Request when both fields are blank")
        void shouldReturn400WhenBothFieldsAreBlank() throws Exception {
            // Arrange
            LoginRequest request = new LoginRequest("", "");

            // Act & Assert
            mockMvc.perform(post("/api/auth/signin")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verify(authService, never()).login(any(LoginRequest.class));
        }

        @Test
        @DisplayName("Should return 400 Bad Request when request body is empty")
        void shouldReturn400WhenRequestBodyIsEmpty() throws Exception {
            // Act & Assert
            mockMvc.perform(post("/api/auth/signin")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());

            verify(authService, never()).login(any(LoginRequest.class));
        }

        @Test
        @DisplayName("Should return 400 Bad Request when request body is malformed")
        void shouldReturn400WhenRequestBodyIsMalformed() throws Exception {
            // Act & Assert
            mockMvc.perform(post("/api/auth/signin")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{invalid json}"))
                    .andExpect(status().isBadRequest());

            verify(authService, never()).login(any(LoginRequest.class));
        }
    }

    @Nested
    @DisplayName("POST /api/auth/signin - Error Handling")
    class ErrorHandling {

        @Test
        @DisplayName("Should return 500 Internal Server Error when unexpected exception occurs")
        void shouldReturn500WhenUnexpectedExceptionOccurs() throws Exception {
            // Arrange
            LoginRequest request = new LoginRequest("admin", "password123");

            when(authService.login(any(LoginRequest.class)))
                    .thenThrow(new RuntimeException("Database connection failed"));

            // Act & Assert
            mockMvc.perform(post("/api/auth/signin")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isInternalServerError())
                    .andExpect(content().string("An error occurred during authentication"));

            verify(authService, times(1)).login(any(LoginRequest.class));
        }

        @Test
        @DisplayName("Should return 500 when NullPointerException occurs")
        void shouldReturn500WhenNullPointerExceptionOccurs() throws Exception {
            // Arrange
            LoginRequest request = new LoginRequest("admin", "password123");

            when(authService.login(any(LoginRequest.class)))
                    .thenThrow(new NullPointerException("Null value encountered"));

            // Act & Assert
            mockMvc.perform(post("/api/auth/signin")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isInternalServerError())
                    .andExpect(content().string("An error occurred during authentication"));
        }
    }

    @Nested
    @DisplayName("POST /api/auth/signin - Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("Should handle username with special characters")
        void shouldHandleUsernameWithSpecialCharacters() throws Exception {
            // Arrange
            LoginRequest request = new LoginRequest("user@example.com", "password123");
            LoginResponse response = new LoginResponse(
                    "token123",
                    "user@example.com",
                    "USER",
                    86400000L
            );

            when(authService.login(any(LoginRequest.class))).thenReturn(response);

            // Act & Assert
            mockMvc.perform(post("/api/auth/signin")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value("user@example.com"));
        }

        @Test
        @DisplayName("Should handle very long username")
        void shouldHandleVeryLongUsername() throws Exception {
            // Arrange
            String longUsername = "a".repeat(50); // Max length as per User entity
            LoginRequest request = new LoginRequest(longUsername, "password123");
            LoginResponse response = new LoginResponse(
                    "token123",
                    longUsername,
                    "USER",
                    86400000L
            );

            when(authService.login(any(LoginRequest.class))).thenReturn(response);

            // Act & Assert
            mockMvc.perform(post("/api/auth/signin")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value(longUsername));
        }

        @Test
        @DisplayName("Should handle password with special characters")
        void shouldHandlePasswordWithSpecialCharacters() throws Exception {
            // Arrange
            LoginRequest request = new LoginRequest("admin", "p@ssw0rd!#$%^&*()");
            LoginResponse response = new LoginResponse(
                    "token123",
                    "admin",
                    "ADMIN",
                    86400000L
            );

            when(authService.login(any(LoginRequest.class))).thenReturn(response);

            // Act & Assert
            mockMvc.perform(post("/api/auth/signin")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should handle username with only whitespace")
        void shouldHandleUsernameWithOnlyWhitespace() throws Exception {
            // Arrange
            LoginRequest request = new LoginRequest("   ", "password123");

            // Act & Assert
            mockMvc.perform(post("/api/auth/signin")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verify(authService, never()).login(any(LoginRequest.class));
        }
    }
}
