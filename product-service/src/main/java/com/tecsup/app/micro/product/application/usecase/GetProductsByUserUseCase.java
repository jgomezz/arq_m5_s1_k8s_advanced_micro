package com.tecsup.app.micro.product.application.usecase;

import com.tecsup.app.micro.product.domain.model.Product;
import com.tecsup.app.micro.product.domain.repository.ProductRepository;
import com.tecsup.app.micro.product.infrastructure.client.UserClient;
import com.tecsup.app.micro.product.infrastructure.client.dto.UserDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Caso de uso: Obtener productos por usuario creador
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GetProductsByUserUseCase {
    
    private final ProductRepository productRepository;
    private final UserClient userClient;

    public List<Product> execute(Long userId) {

        // --------------------------------------------------------
        // Llama al microservicio user-service
        // --------------------------------------------------------
        // Validar que el usuario existe en userdb
        UserDTO user = userClient.getUserById(userId);
        log.info("Fetching products for user from userdb: {}", user.getName());

        // TODO : Validar existencia de usuario o lanzar excepcion

        log.debug("Executing GetProductsByUserUseCase for userId: {}", userId);
        return productRepository.findByCreatedBy(userId);
    }
}
