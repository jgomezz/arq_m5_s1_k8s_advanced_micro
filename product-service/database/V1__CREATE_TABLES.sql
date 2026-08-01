    -- ============================================
    -- Migration: V1__CREATE_TABLES.sql
    -- Database: productdb (Docker container: postgres-product)
    -- ============================================

    -- Función para auto-actualizar updated_at
    CREATE OR REPLACE FUNCTION update_updated_at_column()
    RETURNS TRIGGER AS $$
    BEGIN
        NEW.updated_at = CURRENT_TIMESTAMP;
        RETURN NEW;
    END;
    $$ language 'plpgsql';

    -- Tabla de productos
    CREATE TABLE IF NOT EXISTS products (
        id BIGSERIAL PRIMARY KEY,
        name VARCHAR(200) NOT NULL,
        description TEXT,
        price NUMERIC(10, 2) NOT NULL,
        stock INTEGER NOT NULL DEFAULT 0,
        category VARCHAR(50),
        created_by BIGINT,
        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

        CONSTRAINT chk_name_not_empty CHECK (LENGTH(TRIM(name)) > 0),
        CONSTRAINT chk_price_positive CHECK (price >= 0),
        CONSTRAINT chk_stock_non_negative CHECK (stock >= 0)
    );

    -- Trigger para actualizar updated_at
    CREATE TRIGGER update_products_updated_at
        BEFORE UPDATE ON products
        FOR EACH ROW
        EXECUTE FUNCTION update_updated_at_column();

    -- Comentarios
    COMMENT ON TABLE products IS 'Productos del sistema - DB en Docker';
    COMMENT ON COLUMN products.created_by IS 'Usuario creador (ref. lógica a userdb.users.id en otro contenedor)';
