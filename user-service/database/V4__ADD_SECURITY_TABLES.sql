-- ============================================
-- Migration: V4__ADD_SECURITY_TABLES.sql
-- Description: Crear tablas de seguridad (roles, user_roles)
--              y agregar campos de autenticación a users
-- Database: userdb
-- ============================================

-- ============================================
-- 1. Agregar campos de seguridad a tabla users
-- ============================================
ALTER TABLE users ADD COLUMN password VARCHAR(100);
ALTER TABLE users ADD COLUMN enabled BOOLEAN NOT NULL DEFAULT true;

-- ============================================
-- 2. Tabla de roles
-- ============================================
CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    description VARCHAR(200),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_roles_name UNIQUE (name),
    CONSTRAINT chk_role_name_not_empty CHECK (LENGTH(TRIM(name)) > 0)
);

COMMENT ON TABLE roles IS 'Roles del sistema para autorización';
COMMENT ON COLUMN roles.name IS 'Nombre del rol (ROLE_ADMIN, ROLE_USER, etc.)';

-- ============================================
-- 3. Tabla intermedia user_roles (N:N)
-- ============================================
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (user_id, role_id),

    CONSTRAINT fk_user_roles_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_user_roles_role
        FOREIGN KEY (role_id)
        REFERENCES roles(id)
        ON DELETE CASCADE
);

COMMENT ON TABLE user_roles IS 'Relación N:N entre usuarios y roles';

-- ============================================
-- 4. Índices
-- ============================================
CREATE INDEX idx_user_roles_user_id ON user_roles(user_id);
CREATE INDEX idx_user_roles_role_id ON user_roles(role_id);
CREATE INDEX idx_users_enabled ON users(enabled);