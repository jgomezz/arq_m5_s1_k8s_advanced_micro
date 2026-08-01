package com.tecsup.app.micro.product.domain.repository;

import com.tecsup.app.micro.product.domain.model.Product;

import java.util.List;
import java.util.Optional;

/**
 * Puerto del Repositorio de Producto (Interface)
 * Define el contrato para la persistencia sin depender de la implementación
 * Esta interfaz pertenece al dominio y será implementada en la capa de infraestructura
 */
public interface ProductRepository {
    
    /**
     * Obtiene todos los productos
     */
    List<Product> findAll();
    
    /**
     * Busca un producto por ID
     */
    Optional<Product> findById(Long id);
    
    /**
     * Busca productos por categoría
     */
    List<Product> findByCategory(String category);
    
    /**
     * Busca productos por el usuario que los creó
     */
    List<Product> findByCreatedBy(Long userId);
    
    /**
     * Busca productos disponibles (stock > 0)
     */
    List<Product> findAvailableProducts();
    
    /**
     * Guarda un nuevo producto o actualiza uno existente
     */
    Product save(Product product);
    
    /**
     * Elimina un producto por ID
     */
    void deleteById(Long id);
    
    /**
     * Verifica si existe un producto con el ID dado
     */
    boolean existsById(Long id);
}
