CREATE TABLE IF NOT EXISTS users (
    id          VARCHAR(36)  PRIMARY KEY DEFAULT gen_random_uuid()::VARCHAR,
    institution_id VARCHAR(36) NOT NULL,
    first_name  VARCHAR(100) NOT NULL,
    last_name   VARCHAR(100) NOT NULL,
    mother_last_name VARCHAR(100),
    document_type VARCHAR(20) NOT NULL,
    document_number VARCHAR(15) NOT NULL,
    phone       VARCHAR(20)  NOT NULL,
    address     VARCHAR(255),
    email       VARCHAR(150),
    user_name   VARCHAR(100) NOT NULL,
    role        VARCHAR(30)  NOT NULL,
    status      VARCHAR(10)  NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_users_document_number UNIQUE (document_number),
    CONSTRAINT uk_users_phone UNIQUE (phone),
    CONSTRAINT uk_users_email UNIQUE (email)
);


CREATE INDEX IF NOT EXISTS idx_users_institution_id ON users (institution_id);
CREATE INDEX IF NOT EXISTS idx_users_role ON users (role);
CREATE INDEX IF NOT EXISTS idx_users_status ON users (status);
CREATE INDEX IF NOT EXISTS idx_users_role_status ON users (role, status);
CREATE INDEX IF NOT EXISTS idx_users_document_number ON users (document_number);
