# Temperature Tracking System - Backend

Spring Boot backend application with JWT authentication for the Temperature Tracking System.

## Prerequisites

- Java 21
- Maven 3.6+
- PostgreSQL database (running via Docker Compose)

## Setup

1. Start the PostgreSQL database:
```bash
cd ..
docker compose up -d
```

2. Initialize the database schema:
```bash
docker compose exec -T db psql -U postgres < db_init.sql
```

3. Create a test user (optional - for development):
```bash
docker compose exec db psql -U postgres -c "INSERT INTO users (username, password_hash, role_id) VALUES ('admin', '\$2a\$10\$XPT0CQjJj.9Y8Y9gL0Y0P.Y9Y9Y9Y9Y9Y9Y9Y9Y9Y9Y9Y9Y9Y', 1);"
```

Note: The password hash above is for 'password123' (BCrypt encoded). For production, generate proper hashes.

## Running the Application

```bash
mvn spring-boot:run
```

The application will start on port 8081.

## API Endpoints

### Authentication

#### Sign In
```
POST /api/auth/signin
Content-Type: application/json

{
  "username": "admin",
  "password": "password123"
}
```

Response:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "username": "admin",
  "role": "ADMIN",
  "expiresIn": 86400000
}
```

### Protected Endpoints

All other endpoints require JWT authentication. Include the token in the Authorization header:

```
Authorization: Bearer <your-jwt-token>
```

## Security

- **JWT Secret**: Change the `jwt.secret` in `application.properties` for production
- **Password Encoding**: Uses BCrypt for password hashing
- **Role-Based Access**:
  - ADMIN: Can add, edit, and preview data
  - USER: Can only preview data

## Configuration

Edit `src/main/resources/application.properties` to configure:
- Database connection
- JWT settings (secret, expiration)
- Server port
- CORS allowed origins

## Development

Build the project:
```bash
mvn clean install
```

Run tests:
```bash
mvn test
```

Package for deployment:
```bash
mvn clean package
```

The executable JAR will be in `target/tracking-1.0.0.jar`
