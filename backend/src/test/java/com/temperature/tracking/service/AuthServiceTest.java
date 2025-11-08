package com.temperature.tracking.service;

import com.temperature.tracking.dto.LoginRequest;
import com.temperature.tracking.dto.LoginResponse;
import com.temperature.tracking.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for AuthService.
 * Tests authentication logic, token generation, and role retrieval.
 * All external dependencies (AuthenticationManager, JwtUtil, CustomUserDetailsService) are mocked.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Tests")
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthService authService;

    private static final Long JWT_EXPIRATION = 86400000L; // 24 hours

    @BeforeEach
    void setUp() {
        // Inject the JWT expiration value using ReflectionTestUtils
        ReflectionTestUtils.setField(authService, "jwtExpiration", JWT_EXPIRATION);
    }

    @Nested
    @DisplayName("Successful Authentication")
    class SuccessfulAuthentication {

        @Test
        @DisplayName("Should authenticate user with valid credentials and ADMIN role")
        void shouldAuthenticateAdminUserWithValidCredentials() {
            // Arrange
            LoginRequest loginRequest = new LoginRequest("admin", "password123");
            UserDetails userDetails = User.builder()
                    .username("admin")
                    .password("$2a$10$encodedPassword")
                    .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")))
                    .build();

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(userDetails);
            when(userDetailsService.getUserRole("admin")).thenReturn("ADMIN");
            when(jwtUtil.generateToken("admin", "ADMIN")).thenReturn("jwt.token.here");

            // Act
            LoginResponse response = authService.login(loginRequest);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getToken()).isEqualTo("jwt.token.here");
            assertThat(response.getUsername()).isEqualTo("admin");
            assertThat(response.getRole()).isEqualTo("ADMIN");
            assertThat(response.getExpiresIn()).isEqualTo(JWT_EXPIRATION);

            verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
            verify(userDetailsService, times(1)).getUserRole("admin");
            verify(jwtUtil, times(1)).generateToken("admin", "ADMIN");
        }

        @Test
        @DisplayName("Should authenticate user with valid credentials and USER role")
        void shouldAuthenticateRegularUserWithValidCredentials() {
            // Arrange
            LoginRequest loginRequest = new LoginRequest("user", "password123");
            UserDetails userDetails = User.builder()
                    .username("user")
                    .password("$2a$10$encodedPassword")
                    .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")))
                    .build();

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(userDetails);
            when(userDetailsService.getUserRole("user")).thenReturn("USER");
            when(jwtUtil.generateToken("user", "USER")).thenReturn("jwt.user.token");

            // Act
            LoginResponse response = authService.login(loginRequest);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getToken()).isEqualTo("jwt.user.token");
            assertThat(response.getUsername()).isEqualTo("user");
            assertThat(response.getRole()).isEqualTo("USER");
            assertThat(response.getExpiresIn()).isEqualTo(JWT_EXPIRATION);

            verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
            verify(userDetailsService, times(1)).getUserRole("user");
            verify(jwtUtil, times(1)).generateToken("user", "USER");
        }

        @Test
        @DisplayName("Should create correct authentication token with username and password")
        void shouldCreateCorrectAuthenticationToken() {
            // Arrange
            LoginRequest loginRequest = new LoginRequest("testuser", "testpass");
            UserDetails userDetails = User.builder()
                    .username("testuser")
                    .password("$2a$10$encodedPassword")
                    .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")))
                    .build();

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenAnswer(invocation -> {
                        UsernamePasswordAuthenticationToken token = invocation.getArgument(0);
                        assertThat(token.getPrincipal()).isEqualTo("testuser");
                        assertThat(token.getCredentials()).isEqualTo("testpass");
                        return authentication;
                    });
            when(authentication.getPrincipal()).thenReturn(userDetails);
            when(userDetailsService.getUserRole("testuser")).thenReturn("USER");
            when(jwtUtil.generateToken("testuser", "USER")).thenReturn("token");

            // Act
            authService.login(loginRequest);

            // Assert
            verify(authenticationManager, times(1)).authenticate(
                    argThat(token ->
                            token.getPrincipal().equals("testuser") &&
                                    token.getCredentials().equals("testpass")
                    )
            );
        }

        @Test
        @DisplayName("Should pass correct username and role to JWT generation")
        void shouldPassCorrectParametersToJwtGeneration() {
            // Arrange
            LoginRequest loginRequest = new LoginRequest("admin", "password");
            UserDetails userDetails = User.builder()
                    .username("admin")
                    .password("$2a$10$encodedPassword")
                    .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")))
                    .build();

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(userDetails);
            when(userDetailsService.getUserRole("admin")).thenReturn("ADMIN");
            when(jwtUtil.generateToken("admin", "ADMIN")).thenReturn("token");

            // Act
            authService.login(loginRequest);

            // Assert
            verify(jwtUtil, times(1)).generateToken("admin", "ADMIN");
        }
    }

    @Nested
    @DisplayName("Failed Authentication")
    class FailedAuthentication {

        @Test
        @DisplayName("Should throw BadCredentialsException when username is invalid")
        void shouldThrowBadCredentialsExceptionWhenUsernameIsInvalid() {
            // Arrange
            LoginRequest loginRequest = new LoginRequest("invaliduser", "password123");

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Invalid username or password"));

            // Act & Assert
            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessage("Invalid username or password");

            verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
            verify(userDetailsService, never()).getUserRole(anyString());
            verify(jwtUtil, never()).generateToken(anyString(), anyString());
        }

        @Test
        @DisplayName("Should throw BadCredentialsException when password is invalid")
        void shouldThrowBadCredentialsExceptionWhenPasswordIsInvalid() {
            // Arrange
            LoginRequest loginRequest = new LoginRequest("admin", "wrongpassword");

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Invalid username or password"));

            // Act & Assert
            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessage("Invalid username or password");

            verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
            verify(userDetailsService, never()).getUserRole(anyString());
            verify(jwtUtil, never()).generateToken(anyString(), anyString());
        }

        @Test
        @DisplayName("Should throw BadCredentialsException when both credentials are invalid")
        void shouldThrowBadCredentialsExceptionWhenBothCredentialsAreInvalid() {
            // Arrange
            LoginRequest loginRequest = new LoginRequest("invaliduser", "wrongpassword");

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Invalid username or password"));

            // Act & Assert
            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessage("Invalid username or password");

            verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        }

        @Test
        @DisplayName("Should not call JWT generation when authentication fails")
        void shouldNotCallJwtGenerationWhenAuthenticationFails() {
            // Arrange
            LoginRequest loginRequest = new LoginRequest("admin", "wrongpassword");

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Invalid username or password"));

            // Act & Assert
            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(BadCredentialsException.class);

            verify(jwtUtil, never()).generateToken(anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("Should handle empty username in login request")
        void shouldHandleEmptyUsername() {
            // Arrange
            LoginRequest loginRequest = new LoginRequest("", "password123");

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Invalid username or password"));

            // Act & Assert
            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(BadCredentialsException.class);
        }

        @Test
        @DisplayName("Should handle empty password in login request")
        void shouldHandleEmptyPassword() {
            // Arrange
            LoginRequest loginRequest = new LoginRequest("admin", "");

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Invalid username or password"));

            // Act & Assert
            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(BadCredentialsException.class);
        }

        @Test
        @DisplayName("Should handle username with special characters")
        void shouldHandleUsernameWithSpecialCharacters() {
            // Arrange
            LoginRequest loginRequest = new LoginRequest("user@example.com", "password123");
            UserDetails userDetails = User.builder()
                    .username("user@example.com")
                    .password("$2a$10$encodedPassword")
                    .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")))
                    .build();

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(userDetails);
            when(userDetailsService.getUserRole("user@example.com")).thenReturn("USER");
            when(jwtUtil.generateToken("user@example.com", "USER")).thenReturn("token");

            // Act
            LoginResponse response = authService.login(loginRequest);

            // Assert
            assertThat(response.getUsername()).isEqualTo("user@example.com");
        }

        @Test
        @DisplayName("Should handle very long username")
        void shouldHandleVeryLongUsername() {
            // Arrange
            String longUsername = "a".repeat(50);
            LoginRequest loginRequest = new LoginRequest(longUsername, "password123");
            UserDetails userDetails = User.builder()
                    .username(longUsername)
                    .password("$2a$10$encodedPassword")
                    .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")))
                    .build();

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(userDetails);
            when(userDetailsService.getUserRole(longUsername)).thenReturn("USER");
            when(jwtUtil.generateToken(longUsername, "USER")).thenReturn("token");

            // Act
            LoginResponse response = authService.login(loginRequest);

            // Assert
            assertThat(response.getUsername()).isEqualTo(longUsername);
        }

        @Test
        @DisplayName("Should include correct expiration time in response")
        void shouldIncludeCorrectExpirationTimeInResponse() {
            // Arrange
            LoginRequest loginRequest = new LoginRequest("admin", "password123");
            UserDetails userDetails = User.builder()
                    .username("admin")
                    .password("$2a$10$encodedPassword")
                    .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")))
                    .build();

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(userDetails);
            when(userDetailsService.getUserRole("admin")).thenReturn("ADMIN");
            when(jwtUtil.generateToken("admin", "ADMIN")).thenReturn("token");

            // Act
            LoginResponse response = authService.login(loginRequest);

            // Assert
            assertThat(response.getExpiresIn()).isEqualTo(JWT_EXPIRATION);
        }
    }

    @Nested
    @DisplayName("Service Interaction")
    class ServiceInteraction {

        @Test
        @DisplayName("Should call userDetailsService to get user role")
        void shouldCallUserDetailsServiceToGetUserRole() {
            // Arrange
            LoginRequest loginRequest = new LoginRequest("admin", "password123");
            UserDetails userDetails = User.builder()
                    .username("admin")
                    .password("$2a$10$encodedPassword")
                    .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")))
                    .build();

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(userDetails);
            when(userDetailsService.getUserRole("admin")).thenReturn("ADMIN");
            when(jwtUtil.generateToken("admin", "ADMIN")).thenReturn("token");

            // Act
            authService.login(loginRequest);

            // Assert
            verify(userDetailsService, times(1)).getUserRole("admin");
        }

        @Test
        @DisplayName("Should call all services in correct order")
        void shouldCallAllServicesInCorrectOrder() {
            // Arrange
            LoginRequest loginRequest = new LoginRequest("admin", "password123");
            UserDetails userDetails = User.builder()
                    .username("admin")
                    .password("$2a$10$encodedPassword")
                    .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")))
                    .build();

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(userDetails);
            when(userDetailsService.getUserRole("admin")).thenReturn("ADMIN");
            when(jwtUtil.generateToken("admin", "ADMIN")).thenReturn("token");

            // Act
            authService.login(loginRequest);

            // Assert - verify order of invocations
            var inOrder = inOrder(authenticationManager, userDetailsService, jwtUtil);
            inOrder.verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
            inOrder.verify(userDetailsService).getUserRole("admin");
            inOrder.verify(jwtUtil).generateToken("admin", "ADMIN");
        }
    }
}
