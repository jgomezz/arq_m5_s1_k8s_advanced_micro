package com.tecsup.app.micro.product.application.usecase;

import com.tecsup.app.micro.product.domain.exception.ProductNotFoundException;
import com.tecsup.app.micro.product.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Caso de uso: Eliminar un producto
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DeleteProductUseCase {
    
    private final ProductRepository productRepository;
    
    public void execute(Long id) {
        log.debug("Executing DeleteProductUseCase for id: {}", id);
        
        // Verificar que el producto existe
        if (!productRepository.existsById(id)) {
            throw new ProductNotFoundException(id);
        }
        
        // Eliminar producto
        productRepository.deleteById(id);
        log.info("Product deleted successfully with id: {}", id);
    }
}
