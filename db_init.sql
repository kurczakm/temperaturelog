-- =============================================================================
-- TABELA: roles
-- Opis: Przechowuje definicje ról użytkowników w systemie
-- Relacje: users.role_id → roles.id
-- =============================================================================
CREATE TABLE roles (
    id SERIAL PRIMARY KEY,              -- Unikalny identyfikator roli
    name VARCHAR(50) UNIQUE NOT NULL    -- Nazwa roli (np. ADMIN, USER)
);

-- =============================================================================
-- TABELA: users
-- Opis: Przechowuje konta użytkowników z danymi uwierzytelniającymi
-- Relacje: role_id → roles.id (wiele użytkowników do jednej roli)
--          series.created_by → users.id
--          measurements.created_by → users.id
-- =============================================================================
CREATE TABLE users (
    id SERIAL PRIMARY KEY,                      -- Unikalny identyfikator użytkownika
    username VARCHAR(50) UNIQUE NOT NULL,       -- Unikalna nazwa użytkownika
    password_hash VARCHAR(255) NOT NULL,        -- Zahashowane hasło (BCrypt)
    role_id INT REFERENCES roles(id)            -- Klucz obcy do tabeli roles
);

-- =============================================================================
-- TABELA: series
-- Opis: Przechowuje definicje serii pomiarowych z metadanymi
-- Relacje: created_by → users.id (wiele serii do jednego użytkownika)
--          measurements.series_id → series.id
-- Uwagi: Zawiera min/max wartości do walidacji pomiarów oraz metadane wizualne
--        (kolor, ikona) do prezentacji danych
-- =============================================================================
CREATE TABLE series (
    id SERIAL PRIMARY KEY,                      -- Unikalny identyfikator serii
    name VARCHAR(100) NOT NULL,                 -- Nazwa serii pomiarowej
    description TEXT,                           -- Opcjonalny opis serii
    color VARCHAR(20),                          -- Kolor do wizualizacji (np. hex #FF5733)
    icon VARCHAR(50),                           -- Ikona do wyświetlania
    min_value NUMERIC(5,2),                     -- Minimalna wartość dla walidacji
    max_value NUMERIC(5,2),                     -- Maksymalna wartość dla walidacji
    created_by INT REFERENCES users(id),        -- Klucz obcy: użytkownik tworzący serię
    created_at TIMESTAMP DEFAULT NOW()          -- Timestamp utworzenia serii
);

-- =============================================================================
-- TABELA: measurements
-- Opis: Przechowuje pojedyncze punkty pomiarowe przypisane do serii
-- Relacje: series_id → series.id (wiele pomiarów do jednej serii)
--          created_by → users.id (wiele pomiarów do jednego użytkownika)
-- Uwagi: Usunięcie serii powoduje kaskadowe usunięcie wszystkich jej pomiarów
--        (ON DELETE CASCADE)
-- =============================================================================
CREATE TABLE measurements (
    id SERIAL PRIMARY KEY,                                  -- Unikalny identyfikator pomiaru
    series_id INT REFERENCES series(id) ON DELETE CASCADE,  -- Klucz obcy: seria (z CASCADE)
    value NUMERIC(6,2) NOT NULL,                            -- Wartość pomiaru
    timestamp TIMESTAMP NOT NULL,                           -- Timestamp pomiaru
    created_by INT REFERENCES users(id),                    -- Klucz obcy: użytkownik tworzący pomiar
    created_at TIMESTAMP DEFAULT NOW()                      -- Timestamp utworzenia rekordu
);

-- =============================================================================
-- DANE POCZĄTKOWE
-- Opis: Inicjalizacja podstawowej roli administratora
-- =============================================================================
insert into roles(id, name) values
(1, 'ADMIN');
