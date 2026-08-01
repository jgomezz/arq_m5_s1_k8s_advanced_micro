-- ============================================
-- Migration: V3__INSERT_DATA.sql
-- ============================================

INSERT INTO products (name, description, price, stock, category, created_by) VALUES
('Laptop Dell XPS 15', 'Laptop empresarial de alta gama', 1299.99, 15, 'Electronics', 1),
('Mouse Logitech MX Master 3', 'Mouse inalámbrico ergonómico', 99.99, 50, 'Electronics', 1),
('Teclado Mecánico Keychron K8', 'Teclado mecánico RGB', 89.99, 30, 'Electronics', 2),
('Monitor LG UltraWide 34"', 'Monitor curvo UltraWide', 449.99, 8, 'Electronics', 2),
('Auriculares Sony WH-1000XM5', 'Auriculares con cancelación de ruido', 349.99, 20, 'Electronics', 3);
