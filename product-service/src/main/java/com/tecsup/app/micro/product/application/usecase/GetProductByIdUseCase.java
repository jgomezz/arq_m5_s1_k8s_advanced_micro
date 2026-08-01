package com.tecsup.app.micro.product.application.usecase;

import com.tecsup.app.micro.product.domain.exception.ProductNotFoundException;
import com.tecsup.app.micro.product.domain.exception.UserNotFoundException;
import com.tecsup.app.micro.product.domain.model.Product;
import com.tecsup.app.micro.product.domain.model.User;
import com.tecsup.app.micro.product.domain.repository.ProductRepository;
import com.tecsup.app.micro.product.infrastructure.client.UserClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Caso de uso: Obtener producto por ID
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GetProductByIdUseCase {
    
    private final ProductRepository productRepository;

    private final UserClient userClient;

    public Product execute(Long id, String jwtToken) {  // NUEVO PARAMETRO
        log.debug("Executing GetProductByIdUseCase for id: {}", id);

        Product prod = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        // --------------------------------------------------------
        // Llama al microservicio user-service
        // --------------------------------------------------------
        // Validar que el usuario existe en userdb
        User user = userClient.getUserById(prod.getCreatedBy(), jwtToken); // NUEVO PARAMETRO
        log.info("Fetching user from userdb: {}", user);

        if(user == null) {
            log.warn("User with id {} not found in userdb", prod.getCreatedBy());
            throw new UserNotFoundException(id);
        }

        prod.setCreatedByUser(user);

        return prod;
    }

}
