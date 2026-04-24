CREATE TABLE members (
                         id BIGSERIAL PRIMARY KEY,
                         full_name VARCHAR(255) NOT NULL,
                         cpf VARCHAR(20) NOT NULL UNIQUE,
                         birth_date DATE NOT NULL,
                         email VARCHAR(255),
                         phone VARCHAR(50),
                         membership_start_date DATE,
                         description TEXT,
                         active BOOLEAN NOT NULL DEFAULT TRUE
);