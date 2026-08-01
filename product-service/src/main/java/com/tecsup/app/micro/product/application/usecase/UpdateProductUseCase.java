package com.tecsup.app.micro.product.application.usecase;

import com.tecsup.app.micro.product.domain.exception.InvalidProductDataException;
import com.tecsup.app.micro.product.domain.exception.ProductNotFoundException;
import com.tecsup.app.micro.product.domain.model.Product;
import com.tecsup.app.micro.product.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Caso de uso: Actualizar un producto existente
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UpdateProductUseCase {
    
    private final ProductRepository productRepository;
    
    public Product execute(Long id, Product productDetails) {
        log.debug("Executing UpdateProductUseCase for id: {}", id);
        
        // Verificar que el producto existe
        Product existingProduct = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        
        // Validar datos del producto
        if (!productDetails.isValid()) {
            throw new InvalidProductDataException("Invalid product data. Name, valid price and stock are required.");
        }
        
        // Actualizar campos
        existingProduct.setName(productDetails.getName());
        existingProduct.setDescription(productDetails.getDescription());
        existingProduct.setPrice(productDetails.getPrice());
        existingProduct.setStock(productDetails.getStock());
        existingProduct.setCategory(productDetails.getCategory());
        
        // Guardar cambios
        Product updatedProduct = productRepository.save(existingProduct);
        log.info("Product updated successfully with id: {}", updatedProduct.getId());
        
        return updatedProduct;
    }
}
