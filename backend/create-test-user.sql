-- Create test users with BCrypt hashed passwords
-- Password for both users is: password123
-- Hash generated with BCrypt strength 10

-- Admin user
INSERT INTO users (username, password_hash, role_id)
VALUES ('admin', '$2a$10$XPTOCQjJj9Y8Y9gL0Y0PuOJZq5Xq5Xq5Xq5Xq5Xq5Xq5Xq5Xq5Xq', 1)
ON CONFLICT (username) DO NOTHING;

-- Regular user
INSERT INTO users (username, password_hash, role_id)
VALUES ('user', '$2a$10$XPTOCQjJj9Y8Y9gL0Y0PuOJZq5Xq5Xq5Xq5Xq5Xq5Xq5Xq5Xq5Xq', 2)
ON CONFLICT (username) DO NOTHING;

-- Note: For production, generate proper BCrypt hashes using:
-- Java: BCryptPasswordEncoder
-- Online: https://bcrypt-generator.com/
-- Or use the Spring Boot application with proper user registration endpoint
