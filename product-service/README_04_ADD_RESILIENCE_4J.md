#  Microservicio Product-Service - Resilience4j


### 1.- Modificar pom.xml


```xml
.
.
.
    <properties>
        <java.version>21</java.version>
        <mapstruct.version>1.5.5.Final</mapstruct.version>
        <jjwt.version>0.12.6</jjwt.version>
        <resilience4j.version>2.2.0</resilience4j.version>
    </properties>
.
.
        <!-- ============================================ -->
        <!-- NUEVO - Módulo 4 Sesión 3: Resilience4j      -->
        <!-- ============================================ -->

        <dependency>
            <groupId>io.github.resilience4j</groupId>
            <artifactId>resilience4j-spring-boot3</artifactId>
            <version>${resilience4j.version}</version>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-aop</artifactId>
        </dependency>

.
.
.


```

### 2.- Agregar parámetros de Resilience4j en application.yaml y application-kubernetes.yaml

```yaml

# ============================================
# RESILIENCE4J (Sesión 3)
# ============================================
resilience4j:
  circuitbreaker:
    instances:
      userService:
        registerHealthIndicator: true
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        failureRateThreshold: 50
        waitDurationInOpenState: 10000    # 10 segundos en OPEN
        permittedNumberOfCallsInHalfOpenState: 3
  retry:
    instances:
      userService:
        maxAttempts: 3
        waitDuration: 1000                # 1 segundo entre reintentos

```

### 3.- UserClient.java con JWT + Resilience4j

```java

package com.tecsup.app.micro.product.infrastructure.client;

import com.tecsup.app.micro.product.domain.model.User;
import com.tecsup.app.micro.product.infrastructure.client.dto.UserDto;
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
    @CircuitBreaker(name = "userService", fallbackMethod = "getUserFallback")
    @Retry(name = "userService")
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

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {

//            UserDto user = restTemplate.getForObject(url, UserDto.class);
//            log.info("User retrieved successfully from userdb: {}", user);
//            return userDTOMapper.toDomain(user);

            ResponseEntity<UserDto> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, UserDto.class
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

```

### 4.- ProductController.java (método getProductById )

```java

package com.tecsup.app.micro.product.presentation.controller;


import com.tecsup.app.micro.product.application.service.ProductApplicationService;
import com.tecsup.app.micro.product.domain.model.Product;
import com.tecsup.app.micro.product.presentation.dto.CreateProductRequest;
import com.tecsup.app.micro.product.presentation.dto.ProductResponse;
import com.tecsup.app.micro.product.presentation.dto.UpdateProductRequest;
import com.tecsup.app.micro.product.presentation.mapper.ProductDtoMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Slf4j
public class ProductController {

    ...

    /**
     * Obtiene un producto por ID (público)
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProductResponse> getProductById(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        log.info("REST request to get product by id: {}", id);

        // Extraer JWT del header
        String jwtToken = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            jwtToken = authHeader.substring(7);
        } else {
            log.warn("No Authorization header with Bearer token found for product retrieval");
        }

        log.info("jwtToken extracted for product retrieval: {}", jwtToken != null);

        Product product = productApplicationService.getProductById(id, jwtToken);
        
        return ResponseEntity.ok(productDtoMapper.toResponse(product));
    }

    ...
    
    
}
```


### 5.- Adaptar las clases 

- ProductApplicationService.java

```java

package com.tecsup.app.micro.product.application.service;
...

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductApplicationService {
    
    ...
    
    @Transactional(readOnly = true)
    public Product getProductById(Long id, String jwtToken) {
        return getProductByIdUseCase.execute(id, jwtToken);
    }
    
    ...

}

```


- GetProductByIdUseCase.java

```java

package com.tecsup.app.micro.product.application.usecase;

...

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

```

- SecurityConfig.java

```java
package com.tecsup.app.micro.product.infrastructure.config;

...

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

...

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())

                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .authorizeHttpRequests(auth -> auth
                        // Endpoints públicos (lectura de productos)
                        .requestMatchers(HttpMethod.GET, "/api/products").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/products/available").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/products/health").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/products/{id}").hasRole("ADMIN") //.permitAll() // CAMBIO 
                        .requestMatchers("/actuator/health/**").permitAll()

                        // Solo ADMIN puede crear, actualizar, eliminar productos
                        .requestMatchers(HttpMethod.POST, "/api/products").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/products/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasRole("ADMIN")

                        // Todo lo demás requiere autenticación
                        .anyRequest().authenticated()
                )

    ...
    
    }

...

}

```

### 6.- Pruebas


```sh
# Obtener producto sin login
curl http://localhost:8082/api/products/1

#    {
#        "error": "No autenticado",
#        "status": 401,
#        "message": "Debes autenticarte para acceder a este recurso"
#     }


# Obtener token
TOKEN=$(curl -s -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"juan.perez@example.com","password":"admin123"}' \
  | jq -r '.token')

# Ver token
echo $TOKEN

# Consulta de producto con Token
curl -H "Authorization: Bearer $TOKEN"  http://localhost:8082/api/products/1

```


### 7.- Desplegar en Kubernetes (ver README.md)

- Verificar el contexto de Docker Desktop

```
# Ver contextos
kubectl config get-contexts

# Cambiar el contexto "docker-desktop"
kubectl config use-context docker-desktop

# Verificar el cambio 
kubectl config current-context

```
- Reiniciar el deployment para aplicar los cambios:
```
 kubectl rollout restart deployment product-service -n product-service
```
- Verificar despliegue, servicio y pods:
```
# Verificar despliegue
kubectl get deployments -n product-service

# Verificar servicio
kubectl get service -n product-service

# Verificar pods
kubectl get pods  -n product-service

# Ver detalles de un pod
kubectl describe pod <POD_NAME> -n product-service

# Ver logs en tiempo real
kubectl logs -f <POD_NAME> -n product-service

```


### 8.- Probar en Kubernetes

```

# 1. Login como ADMIN → obtener JWT
TOKEN=$(curl -s -X POST http://localhost:30081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"juan.perez@example.com","password":"admin123"}' \
  | jq -r '.token')

echo $TOKEN

# 2. Acceder con JWT de ADMIN → 200
curl -H "Authorization: Bearer $TOKEN" http://localhost:30081/api/users

# 3. Crear producto con JWT de ADMIN → 201
curl -X POST http://localhost:30082/api/products \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Test Product","description":"Test Product Description", "price":99.99,"stock":10, "category":"Electronics","createdBy":1}'

# 4. Login como USER
TOKEN_USER=$(curl -s -X POST http://localhost:30081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"maria.garcia@example.com","password":"user123"}' \
  | jq -r '.token')

echo $TOKEN_USER

# 5. Crear producto con USER → 403
curl -X POST http://localhost:30082/api/products \
  -H "Authorization: Bearer $TOKEN_USER" \
  -H "Content-Type: application/json" \
  -d '{"name":"Test Product ","description":"Test Product Description", "price":99.99,"stock":10, "category":"Electronics","createdBy":1}'

#
#     {
#        "error": "Acceso denegado",
#        "status": 403,
#        "message": "No tienes permisos para acceder a este recurso"
#     }
#

# 6. Sin JWT → 401
curl -X POST http://localhost:30082/api/products \
  -H "Content-Type: application/json" \
  -d '{"name":"Test Product ","description":"Test Product Description", "price":99.99,"stock":10, "category":"Electronics","createdBy":1}'
  
#
# {
#     "error": "No autenticado",
#     "status": 401,
#     "message": "Debes autenticarte para acceder a este recurso"
# }
#

# 7. Propagacion



```
