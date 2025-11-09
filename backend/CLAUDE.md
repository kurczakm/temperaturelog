# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this backend project.

## Application Architecture

Backend application is provided in Java 21. It uses Spring Boot, Hibernate, and JWT for authentication.

### Backend Structure
- `entity/` - JPA entities (User, Role)
- `repository/` - Spring Data JPA repositories
- `service/` - Business logic (AuthService, CustomUserDetailsService)
- `controller/` - REST API controllers (AuthController at `/api/auth`)
- `security/` - JWT utilities and authentication filter
- `dto/` - Data transfer objects for API requests/responses
- `config/` - Spring Security configuration with role-based access control

## Authentication

The backend uses JWT (JSON Web Token) for authentication.

Response includes:
- `token`: JWT token for subsequent authenticated requests
- `username`: Authenticated user's username
- `role`: User's role (ADMIN or USER)
- `expiresIn`: Token expiration time in milliseconds

## Requirements

- Java 21,
To run the application, you have to export the `JAVA_HOME` environment variable to the path of your JDK.
Run command `export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64/` to set the environment variable before running the application.
