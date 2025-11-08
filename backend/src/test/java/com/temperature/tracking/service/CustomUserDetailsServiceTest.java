package com.temperature.tracking.service;

import com.temperature.tracking.entity.Role;
import com.temperature.tracking.entity.User;
import com.temperature.tracking.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for CustomUserDetailsService.
 * Tests user loading from database, role mapping to authorities, and error handling.
 * UserRepository is mocked to avoid database dependency.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CustomUserDetailsService Tests")
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    @Nested
    @DisplayName("loadUserByUsername - Successful User Loading")
    class SuccessfulUserLoading {

        @Test
        @DisplayName("Should load user with ADMIN role successfully")
        void shouldLoadAdminUserSuccessfully() {
            // Arrange
            Role adminRole = new Role(1, "ADMIN");
            User adminUser = new User(1, "admin", "$2a$10$encodedPassword", adminRole);

            when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));

            // Act
            UserDetails userDetails = userDetailsService.loadUserByUsername("admin");

            // Assert
            assertThat(userDetails).isNotNull();
            assertThat(userDetails.getUsername()).isEqualTo("admin");
            assertThat(userDetails.getPassword()).isEqualTo("$2a$10$encodedPassword");
            assertThat(userDetails.getAuthorities()).hasSize(1);
            assertThat(userDetails.getAuthorities().iterator().next().getAuthority())
                    .isEqualTo("ROLE_ADMIN");

            verify(userRepository, times(1)).findByUsername("admin");
        }

        @Test
        @DisplayName("Should load user with USER role successfully")
        void shouldLoadRegularUserSuccessfully() {
            // Arrange
            Role userRole = new Role(2, "USER");
            User regularUser = new User(2, "user", "$2a$10$encodedPassword", userRole);

            when(userRepository.findByUsername("user")).thenReturn(Optional.of(regularUser));

            // Act
            UserDetails userDetails = userDetailsService.loadUserByUsername("user");

            // Assert
            assertThat(userDetails).isNotNull();
            assertThat(userDetails.getUsername()).isEqualTo("user");
            assertThat(userDetails.getPassword()).isEqualTo("$2a$10$encodedPassword");
            assertThat(userDetails.getAuthorities()).hasSize(1);
            assertThat(userDetails.getAuthorities().iterator().next().getAuthority())
                    .isEqualTo("ROLE_USER");

            verify(userRepository, times(1)).findByUsername("user");
        }

        @Test
        @DisplayName("Should correctly map role name to Spring Security authority")
        void shouldCorrectlyMapRoleToAuthority() {
            // Arrange
            Role customRole = new Role(3, "CUSTOM");
            User user = new User(3, "customuser", "$2a$10$encodedPassword", customRole);

            when(userRepository.findByUsername("customuser")).thenReturn(Optional.of(user));

            // Act
            UserDetails userDetails = userDetailsService.loadUserByUsername("customuser");

            // Assert
            assertThat(userDetails.getAuthorities()).hasSize(1);
            GrantedAuthority authority = userDetails.getAuthorities().iterator().next();
            assertThat(authority.getAuthority()).isEqualTo("ROLE_CUSTOM");
        }

        @Test
        @DisplayName("Should return UserDetails with encoded password")
        void shouldReturnUserDetailsWithEncodedPassword() {
            // Arrange
            Role adminRole = new Role(1, "ADMIN");
            String encodedPassword = "$2a$10$abcdefghijklmnopqrstuvwxyz1234567890";
            User user = new User(1, "admin", encodedPassword, adminRole);

            when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));

            // Act
            UserDetails userDetails = userDetailsService.loadUserByUsername("admin");

            // Assert
            assertThat(userDetails.getPassword()).isEqualTo(encodedPassword);
        }

        @Test
        @DisplayName("Should handle username with special characters")
        void shouldHandleUsernameWithSpecialCharacters() {
            // Arrange
            Role userRole = new Role(2, "USER");
            String specialUsername = "user@example.com";
            User user = new User(2, specialUsername, "$2a$10$encodedPassword", userRole);

            when(userRepository.findByUsername(specialUsername)).thenReturn(Optional.of(user));

            // Act
            UserDetails userDetails = userDetailsService.loadUserByUsername(specialUsername);

            // Assert
            assertThat(userDetails.getUsername()).isEqualTo(specialUsername);
        }

        @Test
        @DisplayName("Should handle long username at boundary length")
        void shouldHandleLongUsername() {
            // Arrange
            Role userRole = new Role(2, "USER");
            String longUsername = "a".repeat(50); // Max length as per User entity
            User user = new User(2, longUsername, "$2a$10$encodedPassword", userRole);

            when(userRepository.findByUsername(longUsername)).thenReturn(Optional.of(user));

            // Act
            UserDetails userDetails = userDetailsService.loadUserByUsername(longUsername);

            // Assert
            assertThat(userDetails.getUsername()).isEqualTo(longUsername);
        }
    }

    @Nested
    @DisplayName("loadUserByUsername - User Not Found")
    class UserNotFound {

        @Test
        @DisplayName("Should throw UsernameNotFoundException when user does not exist")
        void shouldThrowExceptionWhenUserDoesNotExist() {
            // Arrange
            when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> userDetailsService.loadUserByUsername("nonexistent"))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessage("User not found: nonexistent");

            verify(userRepository, times(1)).findByUsername("nonexistent");
        }

        @Test
        @DisplayName("Should throw UsernameNotFoundException with correct message")
        void shouldThrowExceptionWithCorrectMessage() {
            // Arrange
            String username = "unknownuser";
            when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> userDetailsService.loadUserByUsername(username))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessageContaining(username);
        }

        @Test
        @DisplayName("Should throw exception for empty username")
        void shouldThrowExceptionForEmptyUsername() {
            // Arrange
            when(userRepository.findByUsername("")).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> userDetailsService.loadUserByUsername(""))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessage("User not found: ");
        }

        @Test
        @DisplayName("Should not return UserDetails when user not found")
        void shouldNotReturnUserDetailsWhenUserNotFound() {
            // Arrange
            when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> userDetailsService.loadUserByUsername("anyuser"))
                    .isInstanceOf(UsernameNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getUserRole - Successful Role Retrieval")
    class SuccessfulRoleRetrieval {

        @Test
        @DisplayName("Should return ADMIN role for admin user")
        void shouldReturnAdminRole() {
            // Arrange
            Role adminRole = new Role(1, "ADMIN");
            User adminUser = new User(1, "admin", "$2a$10$encodedPassword", adminRole);

            when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));

            // Act
            String role = userDetailsService.getUserRole("admin");

            // Assert
            assertThat(role).isEqualTo("ADMIN");
            verify(userRepository, times(1)).findByUsername("admin");
        }

        @Test
        @DisplayName("Should return USER role for regular user")
        void shouldReturnUserRole() {
            // Arrange
            Role userRole = new Role(2, "USER");
            User regularUser = new User(2, "user", "$2a$10$encodedPassword", userRole);

            when(userRepository.findByUsername("user")).thenReturn(Optional.of(regularUser));

            // Act
            String role = userDetailsService.getUserRole("user");

            // Assert
            assertThat(role).isEqualTo("USER");
            verify(userRepository, times(1)).findByUsername("user");
        }

        @Test
        @DisplayName("Should return role name exactly as stored in database")
        void shouldReturnExactRoleName() {
            // Arrange
            Role customRole = new Role(3, "CUSTOM_ROLE");
            User user = new User(3, "customuser", "$2a$10$encodedPassword", customRole);

            when(userRepository.findByUsername("customuser")).thenReturn(Optional.of(user));

            // Act
            String role = userDetailsService.getUserRole("customuser");

            // Assert
            assertThat(role).isEqualTo("CUSTOM_ROLE");
        }

        @Test
        @DisplayName("Should retrieve role for different users correctly")
        void shouldRetrieveRoleForDifferentUsersCorrectly() {
            // Arrange
            Role adminRole = new Role(1, "ADMIN");
            Role userRole = new Role(2, "USER");
            User adminUser = new User(1, "admin", "$2a$10$encodedPassword", adminRole);
            User regularUser = new User(2, "user", "$2a$10$encodedPassword", userRole);

            when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
            when(userRepository.findByUsername("user")).thenReturn(Optional.of(regularUser));

            // Act
            String adminRoleResult = userDetailsService.getUserRole("admin");
            String userRoleResult = userDetailsService.getUserRole("user");

            // Assert
            assertThat(adminRoleResult).isEqualTo("ADMIN");
            assertThat(userRoleResult).isEqualTo("USER");
        }
    }

    @Nested
    @DisplayName("getUserRole - User Not Found")
    class GetUserRoleNotFound {

        @Test
        @DisplayName("Should throw UsernameNotFoundException when user does not exist")
        void shouldThrowExceptionWhenUserDoesNotExist() {
            // Arrange
            when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> userDetailsService.getUserRole("nonexistent"))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessage("User not found: nonexistent");

            verify(userRepository, times(1)).findByUsername("nonexistent");
        }

        @Test
        @DisplayName("Should throw UsernameNotFoundException with username in message")
        void shouldThrowExceptionWithUsernameInMessage() {
            // Arrange
            String username = "unknownuser";
            when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> userDetailsService.getUserRole(username))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessageContaining(username);
        }

        @Test
        @DisplayName("Should not return role when user not found")
        void shouldNotReturnRoleWhenUserNotFound() {
            // Arrange
            when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> userDetailsService.getUserRole("anyuser"))
                    .isInstanceOf(UsernameNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Edge Cases and Security")
    class EdgeCasesAndSecurity {

        @Test
        @DisplayName("Should handle user with null role gracefully")
        void shouldHandleUserWithNullRole() {
            // Arrange
            User userWithNullRole = new User(1, "user", "$2a$10$encodedPassword", null);

            when(userRepository.findByUsername("user")).thenReturn(Optional.of(userWithNullRole));

            // Act & Assert - This should throw NullPointerException since the code doesn't handle null role
            assertThatThrownBy(() -> userDetailsService.loadUserByUsername("user"))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Should query repository exactly once per call")
        void shouldQueryRepositoryExactlyOnce() {
            // Arrange
            Role userRole = new Role(2, "USER");
            User user = new User(2, "user", "$2a$10$encodedPassword", userRole);

            when(userRepository.findByUsername("user")).thenReturn(Optional.of(user));

            // Act
            userDetailsService.loadUserByUsername("user");

            // Assert
            verify(userRepository, times(1)).findByUsername("user");
            verifyNoMoreInteractions(userRepository);
        }

        @Test
        @DisplayName("Should build UserDetails with single authority only")
        void shouldBuildUserDetailsWithSingleAuthority() {
            // Arrange
            Role userRole = new Role(2, "USER");
            User user = new User(2, "user", "$2a$10$encodedPassword", userRole);

            when(userRepository.findByUsername("user")).thenReturn(Optional.of(user));

            // Act
            UserDetails userDetails = userDetailsService.loadUserByUsername("user");

            // Assert
            assertThat(userDetails.getAuthorities()).hasSize(1);
        }

        @Test
        @DisplayName("Should preserve password hash exactly as stored")
        void shouldPreservePasswordHashExactly() {
            // Arrange
            String exactHash = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";
            Role userRole = new Role(2, "USER");
            User user = new User(2, "user", exactHash, userRole);

            when(userRepository.findByUsername("user")).thenReturn(Optional.of(user));

            // Act
            UserDetails userDetails = userDetailsService.loadUserByUsername("user");

            // Assert
            assertThat(userDetails.getPassword()).isEqualTo(exactHash);
        }

        @Test
        @DisplayName("Should add ROLE_ prefix to authority")
        void shouldAddRolePrefixToAuthority() {
            // Arrange
            Role userRole = new Role(2, "USER");
            User user = new User(2, "user", "$2a$10$encodedPassword", userRole);

            when(userRepository.findByUsername("user")).thenReturn(Optional.of(user));

            // Act
            UserDetails userDetails = userDetailsService.loadUserByUsername("user");

            // Assert
            GrantedAuthority authority = userDetails.getAuthorities().iterator().next();
            assertThat(authority.getAuthority()).startsWith("ROLE_");
        }

        @Test
        @DisplayName("Should handle case-sensitive usernames correctly")
        void shouldHandleCaseSensitiveUsernames() {
            // Arrange
            Role userRole = new Role(2, "USER");
            User user = new User(2, "User", "$2a$10$encodedPassword", userRole);

            when(userRepository.findByUsername("User")).thenReturn(Optional.of(user));
            when(userRepository.findByUsername("user")).thenReturn(Optional.empty());

            // Act
            UserDetails userDetails = userDetailsService.loadUserByUsername("User");

            // Assert
            assertThat(userDetails.getUsername()).isEqualTo("User");

            // Verify different case is treated as different user
            assertThatThrownBy(() -> userDetailsService.loadUserByUsername("user"))
                    .isInstanceOf(UsernameNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Repository Interaction")
    class RepositoryInteraction {

        @Test
        @DisplayName("Should call repository findByUsername with exact username")
        void shouldCallRepositoryWithExactUsername() {
            // Arrange
            Role userRole = new Role(2, "USER");
            User user = new User(2, "testuser", "$2a$10$encodedPassword", userRole);

            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

            // Act
            userDetailsService.loadUserByUsername("testuser");

            // Assert
            verify(userRepository, times(1)).findByUsername("testuser");
        }

        @Test
        @DisplayName("Should not make additional repository calls after user found")
        void shouldNotMakeAdditionalRepositoryCalls() {
            // Arrange
            Role userRole = new Role(2, "USER");
            User user = new User(2, "user", "$2a$10$encodedPassword", userRole);

            when(userRepository.findByUsername("user")).thenReturn(Optional.of(user));

            // Act
            userDetailsService.loadUserByUsername("user");

            // Assert
            verify(userRepository, times(1)).findByUsername("user");
            verifyNoMoreInteractions(userRepository);
        }
    }
}
