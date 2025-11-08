package com.temperature.tracking.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Date;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive unit tests for JwtUtil.
 * Tests JWT token generation, validation, claim extraction, and expiration handling.
 * Uses reflection to inject configuration values to avoid Spring context dependency.
 */
@DisplayName("JwtUtil Tests")
class JwtUtilTest {

    private JwtUtil jwtUtil;

    private static final String SECRET = "mySecretKeyForJWTTokenGenerationAndValidationMustBeLongEnough1234567890";
    private static final Long EXPIRATION = 86400000L; // 24 hours in milliseconds
    private static final Long SHORT_EXPIRATION = 1000L; // 1 second for expiration tests

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expiration", EXPIRATION);
    }

    @Nested
    @DisplayName("Token Generation")
    class TokenGeneration {

        @Test
        @DisplayName("Should generate valid JWT token with username and role")
        void shouldGenerateValidTokenWithUsernameAndRole() {
            // Act
            String token = jwtUtil.generateToken("admin", "ADMIN");

            // Assert
            assertThat(token).isNotNull();
            assertThat(token).isNotEmpty();
            assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts: header.payload.signature
        }

        @Test
        @DisplayName("Should generate different tokens for different users")
        void shouldGenerateDifferentTokensForDifferentUsers() {
            // Act
            String token1 = jwtUtil.generateToken("admin", "ADMIN");
            String token2 = jwtUtil.generateToken("user", "USER");

            // Assert
            assertThat(token1).isNotEqualTo(token2);
        }

        @Test
        @DisplayName("Should generate different tokens for same user at different times")
        void shouldGenerateDifferentTokensAtDifferentTimes() throws InterruptedException {
            // Act
            String token1 = jwtUtil.generateToken("admin", "ADMIN");
            Thread.sleep(1100); // Wait for JWT timestamp to change (JWT uses seconds precision)
            String token2 = jwtUtil.generateToken("admin", "ADMIN");

            // Assert
            assertThat(token1).isNotEqualTo(token2);
        }

        @Test
        @DisplayName("Should include username in token")
        void shouldIncludeUsernameInToken() {
            // Arrange
            String username = "testuser";

            // Act
            String token = jwtUtil.generateToken(username, "USER");
            String extractedUsername = jwtUtil.extractUsername(token);

            // Assert
            assertThat(extractedUsername).isEqualTo(username);
        }

        @Test
        @DisplayName("Should include role in token claims")
        void shouldIncludeRoleInTokenClaims() {
            // Arrange
            String role = "ADMIN";

            // Act
            String token = jwtUtil.generateToken("admin", role);
            String extractedRole = jwtUtil.extractRole(token);

            // Assert
            assertThat(extractedRole).isEqualTo(role);
        }

        @Test
        @DisplayName("Should handle username with special characters")
        void shouldHandleUsernameWithSpecialCharacters() {
            // Arrange
            String specialUsername = "user@example.com";

            // Act
            String token = jwtUtil.generateToken(specialUsername, "USER");
            String extractedUsername = jwtUtil.extractUsername(token);

            // Assert
            assertThat(extractedUsername).isEqualTo(specialUsername);
        }

        @Test
        @DisplayName("Should handle long username")
        void shouldHandleLongUsername() {
            // Arrange
            String longUsername = "a".repeat(50);

            // Act
            String token = jwtUtil.generateToken(longUsername, "USER");
            String extractedUsername = jwtUtil.extractUsername(token);

            // Assert
            assertThat(extractedUsername).isEqualTo(longUsername);
        }

        @Test
        @DisplayName("Should set expiration date in future")
        void shouldSetExpirationDateInFuture() {
            // Act
            String token = jwtUtil.generateToken("admin", "ADMIN");
            Date expirationDate = jwtUtil.extractExpiration(token);

            // Assert
            assertThat(expirationDate).isAfter(new Date());
        }
    }

    @Nested
    @DisplayName("Username Extraction")
    class UsernameExtraction {

        @Test
        @DisplayName("Should extract correct username from token")
        void shouldExtractCorrectUsernameFromToken() {
            // Arrange
            String username = "admin";
            String token = jwtUtil.generateToken(username, "ADMIN");

            // Act
            String extractedUsername = jwtUtil.extractUsername(token);

            // Assert
            assertThat(extractedUsername).isEqualTo(username);
        }

        @Test
        @DisplayName("Should extract username for different users")
        void shouldExtractUsernameForDifferentUsers() {
            // Arrange
            String token1 = jwtUtil.generateToken("admin", "ADMIN");
            String token2 = jwtUtil.generateToken("user", "USER");

            // Act
            String username1 = jwtUtil.extractUsername(token1);
            String username2 = jwtUtil.extractUsername(token2);

            // Assert
            assertThat(username1).isEqualTo("admin");
            assertThat(username2).isEqualTo("user");
        }

        @Test
        @DisplayName("Should throw exception for invalid token format")
        void shouldThrowExceptionForInvalidTokenFormat() {
            // Arrange
            String invalidToken = "invalid.token.format";

            // Act & Assert
            assertThatThrownBy(() -> jwtUtil.extractUsername(invalidToken))
                    .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("Should throw exception for token with wrong signature")
        void shouldThrowExceptionForTokenWithWrongSignature() {
            // Arrange
            String token = jwtUtil.generateToken("admin", "ADMIN");
            String tamperedToken = token.substring(0, token.length() - 5) + "12345";

            // Act & Assert
            assertThatThrownBy(() -> jwtUtil.extractUsername(tamperedToken))
                    .isInstanceOf(SignatureException.class);
        }
    }

    @Nested
    @DisplayName("Role Extraction")
    class RoleExtraction {

        @Test
        @DisplayName("Should extract correct role from token")
        void shouldExtractCorrectRoleFromToken() {
            // Arrange
            String role = "ADMIN";
            String token = jwtUtil.generateToken("admin", role);

            // Act
            String extractedRole = jwtUtil.extractRole(token);

            // Assert
            assertThat(extractedRole).isEqualTo(role);
        }

        @Test
        @DisplayName("Should extract different roles correctly")
        void shouldExtractDifferentRolesCorrectly() {
            // Arrange
            String adminToken = jwtUtil.generateToken("admin", "ADMIN");
            String userToken = jwtUtil.generateToken("user", "USER");

            // Act
            String adminRole = jwtUtil.extractRole(adminToken);
            String userRole = jwtUtil.extractRole(userToken);

            // Assert
            assertThat(adminRole).isEqualTo("ADMIN");
            assertThat(userRole).isEqualTo("USER");
        }

        @Test
        @DisplayName("Should handle custom role names")
        void shouldHandleCustomRoleNames() {
            // Arrange
            String customRole = "CUSTOM_ROLE";
            String token = jwtUtil.generateToken("user", customRole);

            // Act
            String extractedRole = jwtUtil.extractRole(token);

            // Assert
            assertThat(extractedRole).isEqualTo(customRole);
        }
    }

    @Nested
    @DisplayName("Expiration Extraction")
    class ExpirationExtraction {

        @Test
        @DisplayName("Should extract expiration date from token")
        void shouldExtractExpirationDateFromToken() {
            // Arrange
            String token = jwtUtil.generateToken("admin", "ADMIN");

            // Act
            Date expirationDate = jwtUtil.extractExpiration(token);

            // Assert
            assertThat(expirationDate).isNotNull();
            assertThat(expirationDate).isAfter(new Date());
        }

        @Test
        @DisplayName("Should have expiration approximately equal to configured duration")
        void shouldHaveExpirationApproximatelyEqualToConfiguredDuration() {
            // Arrange
            String token = jwtUtil.generateToken("admin", "ADMIN");
            long expectedExpirationTime = System.currentTimeMillis() + EXPIRATION;

            // Act
            Date expirationDate = jwtUtil.extractExpiration(token);

            // Assert - Allow 1 second tolerance for test execution time
            assertThat(expirationDate.getTime())
                    .isCloseTo(expectedExpirationTime, within(1000L));
        }
    }

    @Nested
    @DisplayName("Token Validation")
    class TokenValidation {

        @Test
        @DisplayName("Should validate token with correct username and not expired")
        void shouldValidateTokenWithCorrectUsernameAndNotExpired() {
            // Arrange
            String username = "admin";
            String token = jwtUtil.generateToken(username, "ADMIN");
            UserDetails userDetails = createUserDetails(username, "ROLE_ADMIN");

            // Act
            Boolean isValid = jwtUtil.validateToken(token, userDetails);

            // Assert
            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("Should invalidate token with wrong username")
        void shouldInvalidateTokenWithWrongUsername() {
            // Arrange
            String token = jwtUtil.generateToken("admin", "ADMIN");
            UserDetails userDetails = createUserDetails("differentuser", "ROLE_ADMIN");

            // Act
            Boolean isValid = jwtUtil.validateToken(token, userDetails);

            // Assert
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("Should validate token for different users separately")
        void shouldValidateTokenForDifferentUsersSeparately() {
            // Arrange
            String adminToken = jwtUtil.generateToken("admin", "ADMIN");
            String userToken = jwtUtil.generateToken("user", "USER");

            UserDetails adminDetails = createUserDetails("admin", "ROLE_ADMIN");
            UserDetails userDetails = createUserDetails("user", "ROLE_USER");

            // Act & Assert
            assertThat(jwtUtil.validateToken(adminToken, adminDetails)).isTrue();
            assertThat(jwtUtil.validateToken(userToken, userDetails)).isTrue();
            assertThat(jwtUtil.validateToken(adminToken, userDetails)).isFalse();
            assertThat(jwtUtil.validateToken(userToken, adminDetails)).isFalse();
        }

        @Test
        @DisplayName("Should throw exception when extracting username from expired token")
        void shouldThrowExceptionWhenExtractingFromExpiredToken() throws InterruptedException {
            // Arrange - Create JwtUtil with short expiration (1 second)
            JwtUtil shortExpirationJwtUtil = new JwtUtil();
            ReflectionTestUtils.setField(shortExpirationJwtUtil, "secret", SECRET);
            ReflectionTestUtils.setField(shortExpirationJwtUtil, "expiration", 1000L);

            String token = shortExpirationJwtUtil.generateToken("admin", "ADMIN");

            // Wait for token to expire
            Thread.sleep(1500); // Wait longer than expiration

            // Act & Assert - Extracting claims from expired token should throw ExpiredJwtException
            assertThatThrownBy(() -> shortExpirationJwtUtil.extractUsername(token))
                    .isInstanceOf(ExpiredJwtException.class);
        }

        @Test
        @DisplayName("Should reject token signed with different secret")
        void shouldRejectTokenSignedWithDifferentSecret() {
            // Arrange
            JwtUtil differentSecretJwtUtil = new JwtUtil();
            ReflectionTestUtils.setField(differentSecretJwtUtil, "secret",
                "differentSecretKeyForJWTTokenGenerationMustBeLongEnough12345678");
            ReflectionTestUtils.setField(differentSecretJwtUtil, "expiration", EXPIRATION);

            String token = differentSecretJwtUtil.generateToken("admin", "ADMIN");

            // Act & Assert - Original jwtUtil should reject token signed with different secret
            assertThatThrownBy(() -> jwtUtil.extractUsername(token))
                    .isInstanceOf(SignatureException.class);
        }
    }

    @Nested
    @DisplayName("Edge Cases and Security")
    class EdgeCasesAndSecurity {

        @Test
        @DisplayName("Should generate token with null username (without throwing exception)")
        void shouldGenerateTokenWithNullUsername() {
            // Act - JwtUtil doesn't validate null values, it creates token with null subject
            String token = jwtUtil.generateToken(null, "ADMIN");

            // Assert - Token should be created (though it may not be meaningful)
            assertThat(token).isNotNull();
        }

        @Test
        @DisplayName("Should generate token with null role (without throwing exception)")
        void shouldGenerateTokenWithNullRole() {
            // Act - JwtUtil doesn't validate null values
            String token = jwtUtil.generateToken("admin", null);

            // Assert - Token should be created
            assertThat(token).isNotNull();
        }

        @Test
        @DisplayName("Should handle empty string username")
        void shouldHandleEmptyStringUsername() {
            // Act
            String token = jwtUtil.generateToken("", "ADMIN");
            String extractedUsername = jwtUtil.extractUsername(token);

            // Assert - Empty string results in empty or null username depending on JWT implementation
            // Both are acceptable since empty username is not a valid use case
            assertThat(extractedUsername == null || extractedUsername.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("Should handle empty string role")
        void shouldHandleEmptyStringRole() {
            // Act
            String token = jwtUtil.generateToken("admin", "");
            String extractedRole = jwtUtil.extractRole(token);

            // Assert
            assertThat(extractedRole).isEmpty();
        }

        @Test
        @DisplayName("Should reject completely invalid token")
        void shouldRejectCompletelyInvalidToken() {
            // Arrange
            String invalidToken = "not.a.valid.jwt.token";

            // Act & Assert
            assertThatThrownBy(() -> jwtUtil.extractUsername(invalidToken))
                    .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("Should reject empty token string")
        void shouldRejectEmptyTokenString() {
            // Act & Assert
            assertThatThrownBy(() -> jwtUtil.extractUsername(""))
                    .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("Should reject null token")
        void shouldRejectNullToken() {
            // Act & Assert
            assertThatThrownBy(() -> jwtUtil.extractUsername(null))
                    .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("Should handle very long token content")
        void shouldHandleVeryLongTokenContent() {
            // Arrange
            String longUsername = "a".repeat(200);
            String longRole = "b".repeat(200);

            // Act
            String token = jwtUtil.generateToken(longUsername, longRole);
            String extractedUsername = jwtUtil.extractUsername(token);
            String extractedRole = jwtUtil.extractRole(token);

            // Assert
            assertThat(extractedUsername).isEqualTo(longUsername);
            assertThat(extractedRole).isEqualTo(longRole);
        }

        @Test
        @DisplayName("Should handle special characters in username and role")
        void shouldHandleSpecialCharactersInUsernameAndRole() {
            // Arrange
            String specialUsername = "user@example.com!#$%";
            String specialRole = "ROLE-WITH-SPECIAL_CHARS";

            // Act
            String token = jwtUtil.generateToken(specialUsername, specialRole);
            String extractedUsername = jwtUtil.extractUsername(token);
            String extractedRole = jwtUtil.extractRole(token);

            // Assert
            assertThat(extractedUsername).isEqualTo(specialUsername);
            assertThat(extractedRole).isEqualTo(specialRole);
        }

        @Test
        @DisplayName("Should handle unicode characters")
        void shouldHandleUnicodeCharacters() {
            // Arrange
            String unicodeUsername = "user_\u00E9\u00E8\u00EA"; // user with accented characters
            String unicodeRole = "ROLE_\u4E2D\u6587"; // ROLE with Chinese characters

            // Act
            String token = jwtUtil.generateToken(unicodeUsername, unicodeRole);
            String extractedUsername = jwtUtil.extractUsername(token);
            String extractedRole = jwtUtil.extractRole(token);

            // Assert
            assertThat(extractedUsername).isEqualTo(unicodeUsername);
            assertThat(extractedRole).isEqualTo(unicodeRole);
        }
    }

    @Nested
    @DisplayName("Token Structure")
    class TokenStructure {

        @Test
        @DisplayName("Should generate JWT with three parts separated by dots")
        void shouldGenerateJwtWithThreeParts() {
            // Act
            String token = jwtUtil.generateToken("admin", "ADMIN");

            // Assert
            String[] parts = token.split("\\.");
            assertThat(parts).hasSize(3);
            assertThat(parts[0]).isNotEmpty(); // Header
            assertThat(parts[1]).isNotEmpty(); // Payload
            assertThat(parts[2]).isNotEmpty(); // Signature
        }

        @Test
        @DisplayName("Should use Base64 URL encoding for token parts")
        void shouldUseBase64UrlEncodingForTokenParts() {
            // Act
            String token = jwtUtil.generateToken("admin", "ADMIN");

            // Assert
            String[] parts = token.split("\\.");
            // Base64 URL encoded strings should not contain '+' or '/'
            for (String part : parts) {
                assertThat(part).doesNotContain("+");
                assertThat(part).doesNotContain("/");
            }
        }
    }

    // Helper method to create UserDetails for testing
    private UserDetails createUserDetails(String username, String role) {
        return User.builder()
                .username(username)
                .password("password")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority(role)))
                .build();
    }
}
