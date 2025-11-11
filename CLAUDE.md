# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a temperature tracking system with PostgreSQL database backend, containerized using Docker Compose. The application manages time-series data organized into series, with user authentication and role-based access control. There are two roles: USER who can only preview the data ADMIN who can add, edit and preview data.

## Database Architecture

The schema consists of four main tables:
- `roles` - User role definitions (For now there is only one rule: ADMIN, but in the future there can be more)
- `users` - User accounts with authentication
- `series` - Measurement series with metadata (name, description, min/max values, color, icon)
- `measurements` - Individual data points linked to series with timestamps

Key relationships:
- Users have roles (many-to-one)
- Series track min/max bounds for validation
- Measurements belong to series (with CASCADE delete)

## Roles
Series and masurements are managed by users with role ADMIN.

## Application Architecture

Backend application is provided in Java 21. It uses Spring Boot, Hibernate, and JWT for authentication.
Frontend application is provided in Angular 20.

## Development Commands

### Backend

#### Run backend application
```bash
cd backend
mvn spring-boot:run
```

#### Build backend
```bash
cd backend
mvn clean install
```

#### Run backend tests
```bash
cd backend
mvn test
```

#### Package for deployment
```bash
cd backend
mvn clean package
```

### Database

### Start the environment
```bash
docker compose up -d
```

### Stop the environment
```bash
docker compose down
```

### Initialize/Reset database
```bash
docker compose exec -T db psql -U postgres < db_init.sql
```

### Access database directly
```bash
docker compose exec db psql -U postgres
```

### Access Adminer web interface
Navigate to http://localhost:8080 after starting services. Use:
- System: PostgreSQL
- Server: db
- Username: postgres
- Password: example

## Services

- **Backend API** (port 8081): Spring Boot REST API with JWT authentication
- **PostgreSQL** (port 5432): Main database with password 'example'

## Authentication

The backend uses JWT (JSON Web Token) for authentication.

### Sign In Endpoint
```
POST http://localhost:8081/api/auth/signin
Content-Type: application/json

{
  "username": "admin",
  "password": "password123"
}
```

Response includes:
- `token`: JWT token for subsequent authenticated requests
- `username`: Authenticated user's username
- `role`: User's role (ADMIN or USER)
- `expiresIn`: Token expiration time in milliseconds

### Using JWT Token
Include the token in the Authorization header for protected endpoints:
```
Authorization: Bearer <your-jwt-token>
```

### Security Configuration
- JWT secret is configured in `backend/src/main/resources/application.properties`
- Token expiration is set to 24 hours by default
- Passwords are BCrypt encoded
- CORS is configured for Angular frontend (http://localhost:4200)

## Roles
There is only one role for authenticated users: ADMIN. But some actions are also available for unauthenticated users.
Add and edit can only ADMIN. But preview data can both ADMIN and unauthenticated user.
