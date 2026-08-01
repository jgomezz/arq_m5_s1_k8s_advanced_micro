package com.tecsup.app.micro.product.infrastructure.client;

import com.tecsup.app.micro.product.domain.model.User;
import com.tecsup.app.micro.product.infrastructure.client.dto.UserDTO;
import com.tecsup.app.micro.product.infrastructure.client.mapper.UserDtoMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Cliente HTTP para comunicarse con user-service
 *
 * Paquete: com.tecsup.app.micro.product.infrastructure.client
 *
 * MODIFICADO en Módulo 4:
 *   Sesión 2: Propaga JWT en el header Authorization
 *   Sesión 3: Agrega Circuit Breaker, Retry y Fallback con Resilience4j
 *
 * Flujo:
 *   1. Recibe el JWT del request original (propagado desde el controlador)
 *   2. Incluye el JWT en la llamada a user-service
 *   3. Si user-service falla → Retry (3 intentos)
 *   4. Si sigue fallando → Circuit Breaker abre → Fallback
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserClient {

    private final RestTemplate restTemplate;
    private final UserDtoMapper userDTOMapper;

    @Value("${user.service.url}")
    private String userServiceUrl;

    /**
     * Obtiene un usuario por ID desde user-service
     *
     * @param userId ID del usuario a buscar
     * @param jwtToken Token JWT para autenticación (Sesión 2)
     * @return User del dominio
     *
     * Anotaciones Resilience4j (Sesión 3):
     *   @CircuitBreaker: Si el 50% de las últimas 10 llamadas fallan,
     *                    abre el circuito por 10 segundos
     *   @Retry: Reintenta hasta 3 veces con 1 segundo entre intentos
     */
    @CircuitBreaker(name = "userService")
    @Retry(name = "userService", fallbackMethod = "getUserFallback")
    public User getUserById(Long userId, String jwtToken) {
        log.info("Calling User Service (PostgreSQL userdb) to get user with id: {}", userId);

        String url = this.userServiceUrl + "/api/users/" + userId;

        // =============================================
        // Sesión 2: Propagar JWT en el header
        // =============================================
        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_JSON);
        
        if (jwtToken != null && !jwtToken.isEmpty()) {
            headers.setBearerAuth(jwtToken);
        } else {
            log.warn("No JWT token provided for User Service call");
        }

        // Set JWT token in the Authorization header
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {

//            UserDto user = restTemplate.getForObject(url, UserDto.class);
//            log.info("User retrieved successfully from userdb: {}", user);
//            return userDTOMapper.toDomain(user);

            ResponseEntity<UserDTO> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, UserDTO.class
            );
            log.info("User retrieved successfully from user-service: {}", response.getBody());
            return userDTOMapper.toDomain(response.getBody());

        } catch (Exception e) {
            log.error("Error calling User Service: {}", e.getMessage());
            throw new RuntimeException("Error calling User Service: " + e.getMessage());
        }
    }


    /**
     * Metodo de versión anterior (sin JWT) - mantener para compatibilidad
     * Se puede eliminar una vez que JWT esté completamente implementado
     */
    public User getUserById(Long userId) {
        return getUserById(userId, null);
    }

    /**
     * Fallback cuando user-service no está disponible (Sesión 3)
     *
     * Se ejecuta cuando:
     *   - El Circuit Breaker está abierto
     *   - Se agotaron los reintentos del Retry
     *   - user-service no responde o retorna error
     *
     * @return User con datos parciales (indica que el servicio no está disponible)
     */
    public User getUserFallback(Long userId, String jwtToken, Throwable throwable) {
        log.warn("FALLBACK: User Service no disponible para userId: {}. Razón: {}",
                userId, throwable.getMessage());

        return User.builder()
                .id(userId)
                .name("Usuario no disponible")
                .email("N/A")
                .build();
    }
}