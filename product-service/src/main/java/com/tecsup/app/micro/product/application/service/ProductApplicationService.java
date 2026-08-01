package com.tecsup.app.micro.product.application.service;

import com.tecsup.app.micro.product.application.usecase.*;
import com.tecsup.app.micro.product.domain.model.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio de Aplicación de Producto
 * Orquesta los casos de uso y maneja las transacciones
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductApplicationService {
    
    private final GetAllProductsUseCase getAllProductsUseCase;
    private final GetProductByIdUseCase getProductByIdUseCase;
    private final GetAvailableProductsUseCase getAvailableProductsUseCase;
    private final GetProductsByUserUseCase getProductsByUserUseCase;
    private final CreateProductUseCase createProductUseCase;
    private final UpdateProductUseCase updateProductUseCase;
    private final DeleteProductUseCase deleteProductUseCase;
    
    @Transactional(readOnly = true)
    public List<Product> getAllProducts() {
        return getAllProductsUseCase.execute();
    }
    
    @Transactional(readOnly = true)
    public Product getProductById(Long id) {
        return getProductByIdUseCase.execute(id);
    }
    
    @Transactional(readOnly = true)
    public List<Product> getAvailableProducts() {
        return getAvailableProductsUseCase.execute();
    }
    
    @Transactional(readOnly = true)
    public List<Product> getProductsByUser(Long userId) {
        return getProductsByUserUseCase.execute(userId);
    }
    
    @Transactional
    public Product createProduct(Product product) {
        return createProductUseCase.execute(product);
    }
    
    @Transactional
    public Product updateProduct(Long id, Product product) {
        return updateProductUseCase.execute(id, product);
    }
    
    @Transactional
    public void deleteProduct(Long id) {
        deleteProductUseCase.execute(id);
    }
}
