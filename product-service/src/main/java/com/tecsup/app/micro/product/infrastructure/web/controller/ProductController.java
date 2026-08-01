package com.tecsup.app.micro.product.infrastructure.web.controller;


import com.tecsup.app.micro.product.application.service.ProductApplicationService;
import com.tecsup.app.micro.product.domain.model.Product;
import com.tecsup.app.micro.product.infrastructure.web.dto.CreateProductRequest;
import com.tecsup.app.micro.product.infrastructure.web.dto.ProductResponse;
import com.tecsup.app.micro.product.infrastructure.web.dto.UpdateProductRequest;
import com.tecsup.app.micro.product.infrastructure.web.mapper.ProductDtoMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST de Productos
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Slf4j
public class ProductController {
    
    private final ProductApplicationService productApplicationService;
    private final ProductDtoMapper productDtoMapper;
    
    /**
     * Obtiene todos los productos
     */
    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAllProducts() {
        log.info("REST request to get all products");
        List<Product> products = productApplicationService.getAllProducts();
        return ResponseEntity.ok(productDtoMapper.toResponseList(products));
    }
    
    /**
     * Obtiene productos disponibles (stock > 0)
     */
    @GetMapping("/available")
    public ResponseEntity<List<ProductResponse>> getAvailableProducts() {
        log.info("REST request to get available products");
        List<Product> products = productApplicationService.getAvailableProducts();
        return ResponseEntity.ok(productDtoMapper.toResponseList(products));
    }
    
    /**
     * Obtiene un producto por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable Long id) {
        log.info("REST request to get product by id: {}", id);
        Product product = productApplicationService.getProductById(id);
        return ResponseEntity.ok(productDtoMapper.toResponse(product));
    }
    
    /**
     * Obtiene productos por usuario creador (autenticado)
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ProductResponse>> getProductsByUser(@PathVariable Long userId) {
        log.info("REST request to get products by user: {}", userId);
        List<Product> products = productApplicationService.getProductsByUser(userId);
        return ResponseEntity.ok(productDtoMapper.toResponseList(products));
    }

    /**
     * Crea un nuevo producto (solo ADMIN)
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody CreateProductRequest request) {
        log.info("REST request to create product: {}", request.getName());
        Product product = productDtoMapper.toDomain(request);
        Product createdProduct = productApplicationService.createProduct(product);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productDtoMapper.toResponse(createdProduct));
    }

    /**
     * Actualiza un producto existente (solo ADMIN)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProductRequest request) {
        log.info("REST request to update product with id: {}", id);
        Product product = productDtoMapper.toDomain(request);
        Product updatedProduct = productApplicationService.updateProduct(id, product);
        return ResponseEntity.ok(productDtoMapper.toResponse(updatedProduct));
    }

    /**
     * Elimina un producto (solo ADMIN)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        log.info("REST request to delete product with id: {}", id);
        productApplicationService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Endpoint de salud
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Product Service running with Clean Architecture!");
    }
}
