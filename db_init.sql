CREATE TABLE roles (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL
);

CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role_id INT REFERENCES roles(id)
);

CREATE TABLE series (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    min_value NUMERIC(5,2) NOT NULL,
    max_value NUMERIC(5,2) NOT NULL,
    color VARCHAR(20),
    icon VARCHAR(50),
    created_by INT REFERENCES users(id),
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE measurements (
    id SERIAL PRIMARY KEY,
    series_id INT REFERENCES series(id) ON DELETE CASCADE,
    value NUMERIC(5,2) NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    created_by INT REFERENCES users(id),
    created_at TIMESTAMP DEFAULT NOW()
);

insert into roles(id, name) values
(1, 'ADMIN'),
(2, 'USER');
