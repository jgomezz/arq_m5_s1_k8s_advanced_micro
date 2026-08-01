#  Microservicio Product-Service - Seguridad y Despliegue en Kubernetes

<img src="images/spring_security_class.png" alt="Spring Boot Logo" />

## 1.- Modificar aplicación para agregar seguridad con Spring Security

### 1.1.- Agregar Spring Security

```xml
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.security</groupId>
        <artifactId>spring-security-test</artifactId>
        <scope>test</scope>
    </dependency>
```
### 1.2.- Creación de clases

#### 1.2.4. Configuración de seguridad

- SecurityConfig.java
```java
package com.tecsup.app.micro.product.infrastructure.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuración de Spring Security para product-service
 *
 * Paquete: com.tecsup.app.micro.product.infrastructure.config
 * Sesión 1: Autorización por URL
 * Sesión 2: Validación de JWT (product-service NO genera tokens, solo los valida)
 *
 * Endpoints:
 *   GET  /api/products             → público
 *   GET  /api/products/available   → público
 *   GET  /api/products/{id}        → público
 *   GET  /api/products/user/{userId} → autenticado
 *   POST /api/products             → ADMIN
 *   PUT  /api/products/{id}        → ADMIN
 *   DELETE /api/products/{id}      → ADMIN
 *   POST /api/orders               → autenticado (Sesión 3)
 *   GET  /api/products/health      → público
 *   Actuator /actuator/health      → público
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

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
                        .requestMatchers(HttpMethod.GET, "/api/products/{id}").permitAll()
                        .requestMatchers("/actuator/health/**").permitAll()

                        // Solo ADMIN puede crear, actualizar, eliminar productos
                        .requestMatchers(HttpMethod.POST, "/api/products").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/products/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasRole("ADMIN")

                        // Todo lo demás requiere autenticación
                        .anyRequest().authenticated()
                )


                // Manejo de errores
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpStatus.UNAUTHORIZED.value());
                            response.setContentType("application/json");
                            response.getWriter().write("""
                                        {
                                            "error": "No autenticado", 
                                            "status": 401,
                                            "message": "Debes autenticarte para acceder a este recurso"
                                         }
                                    """);
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpStatus.FORBIDDEN.value());
                            response.setContentType("application/json");
                            response.getWriter().write("""
                                        {
                                            "error": "Acceso denegado", 
                                            "status": 403,
                                            "message": "No tienes permisos para acceder a este recurso"
                                         }
                                    """);
                        })
                );

        return http.build();
    }
}

```

#### 1.2.5. Controladores con @PreAuthorize

- ProductController.java
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

/**
 * Controlador REST de Productos
 * MODIFICADO en Módulo 4 - Sesión 1: Se agregan anotaciones @PreAuthorize
 *
 * Reglas de acceso:
 *   GET  /api/products             → público
 *   GET  /api/products/available   → público
 *   GET  /api/products/{id}        → público
 *   GET  /api/products/user/{uid}  → autenticado
 *   POST /api/products             → ADMIN
 *   PUT  /api/products/{id}        → ADMIN
 *   DELETE /api/products/{id}      → ADMIN
 *   GET  /api/products/health      → público
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Slf4j
public class ProductController {

    private final ProductApplicationService productApplicationService;
    private final ProductDtoMapper productDtoMapper;

    /**
     * Obtiene todos los productos (público)
     */
    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAllProducts() {
        log.info("REST request to get all products");
        List<Product> products = productApplicationService.getAllProducts();
        return ResponseEntity.ok(productDtoMapper.toResponseList(products));
    }

    /**
     * Obtiene productos disponibles (stock > 0) (público)
     */
    @GetMapping("/available")
    public ResponseEntity<List<ProductResponse>> getAvailableProducts() {
        log.info("REST request to get available products");
        List<Product> products = productApplicationService.getAvailableProducts();
        return ResponseEntity.ok(productDtoMapper.toResponseList(products));
    }

    /**
     * Obtiene un producto por ID (público)
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
     * Endpoint de salud (público)
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Product Service running with Clean Architecture!");
    }
}


```

### 1.3.- Verificar en localhost

- Ejecutar la aplicación y probar los endpoints con Postman o curl.
```
# Endpoint público → 200
curl http://localhost:8082/api/products

# POST sin auth → 401
curl -X POST http://localhost:8082/api/products \
  -H "Content-Type: application/json" \
  -d '{"name":"Test","price":10,"stock":5}'
```

## 2.- Desplegar en Kubernetes con seguridad

### 2.2.- Construir imagen Docker y probar localmente (ver README.md)

#### Constuir imagen

```
# Compilar el proyecto (si es necesario)
mvn clean package -DskipTests

# Construir imagen
docker build -t product-service:1.0 .

# Verificar imagen creada
docker images | grep product-service

# Deberías ver:
# product-service   1.0   abc123def456   1 minute ago   230MB

```

#### Probar la imagen Docker

```
# Ejecutar contenedor
docker run -p 8082:8082 \
  -e SPRING_PROFILES_ACTIVE=kubernetes \
  -e DB_URL=jdbc:postgresql://host.docker.internal:5433/productdb \
  -e DB_USERNAME=postgres \
  -e DB_PASSWORD=postgres \
  -e USER_SERVICE_URL=http://host.docker.internal:30081 \
  product-service:1.0

# Deberías ver:
# Started ProductServiceApplication in X seconds

# En otra terminal, probar

# Health check
curl http://localhost:8082/actuator/health

# Respuesta esperada:
# {"status":"UP","groups":["liveness","readiness"]}

# Listar productos (público)
curl http://localhost:8082/api/products

```

### 2.3.- Desplegar en Kubernetes (ver README.md)

- En caso se haya modificado el código después del despliegue inicial, reiniciar el deployment para aplicar los cambios:
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


### 2.4.- Probar autenticación en Kubernetes

```
# Endpoint público → 200
curl http://localhost:30082/api/products

# POST sin auth → 401
curl -X POST http://localhost:30082/api/products \
  -H "Content-Type: application/json" \
  -d '{"name":"Test","price":10,"stock":5}'
```