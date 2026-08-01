#  Microservicio User-Service - JWT y OAuth 2.0 en Kubernetes


## JWT y  OAuth 2.0
¿Por qué no basta HTTP Basic?

El cliente envía email:password en cada petición codificado en Base64. Problemas:

- Las credenciales viajan en cada request (aunque sea Base64, no es encriptación)
- El servidor debe consultar la BD en cada petición para validar
- No hay forma de revocar una sesión sin cambiar la contraseña
- Imposible propagar identidad entre microservicios sin compartir credenciales

JWT resuelve esto: el servidor valida una vez, genera un token firmado, y el cliente lo presenta en cada request. El servidor solo verifica la firma, sin consultar BD.

##  JWT (JSON Web Token)



#### Flujo JWT en nuestra arquitectura
```
┌──────────┐     POST /api/auth/login       ┌──────────-────┐
│  Cliente  │ ──── email + password ───────→│ user-service  │
│  (curl)   │                               │   :8081       │
│           │ ←──── JWT Token ──────────────│  (genera JWT) │
└─────┬─────┘                               └───────────-───┘
      │
      │  GET /api/products (Authorization: Bearer <JWT>)
      │
      ▼
┌──────────────┐    Valida JWT (misma clave)    ┌────-──────────┐
│product-service│ ─── propaga JWT ─────────────→│ user-service  │
│   :8082       │    GET /api/users/1           │   :8081       │
│ (solo valida) │ ←── respuesta ─────────-──────│               │
└──────────────┘                                └─────-─────────┘
```

#### OAuth 2.0

OAuth 2.0 es un **framework de autorización** (no de autenticación). Define cómo una aplicación obtiene acceso limitado a recursos de un usuario sin conocer su contraseña.

**Roles:**

| Rol | En nuestro proyecto |
|-----|-------------------|
| Resource Owner | El usuario (Juan, María) |
| Client | La aplicación frontend (curl, Postman, React app) |
| Authorization Server | user-service (genera JWT) / en prod: AWS Cognito |
| Resource Server | user-service, product-service (validan JWT) |

**Flows principales:**

| Flow | Cuándo usarlo |
|------|--------------|
| **Authorization Code** | Apps con frontend (browser). El más seguro. Redirect al auth server, obtiene code, intercambia por token. |
| **Authorization Code + PKCE** | SPAs y apps móviles (sin backend seguro). Agrega code_verifier para prevenir intercepción. |
| **Client Credentials** | Servicio-a-servicio (sin usuario). El cliente se autentica con client_id + client_secret. |
| **Resource Owner Password** | **Lo que usamos en el lab.** El cliente envía email+password directo. Simple pero menos seguro. Solo para APIs propias de confianza. |

Nuestro `POST /api/auth/login` es esencialmente el flow **Resource Owner Password Credentials** simplificado.

---

## Estructura



<img src="images/auth2_jwt_class.png" width="500"/>

## 1.- Modificar aplicación para agregar JWT


```xml
.
.
.
    <properties>
        <java.version>21</java.version>
        <mapstruct.version>1.5.5.Final</mapstruct.version>
        <jjwt.version>0.12.6</jjwt.version>
    </properties>
.
.
        <!-- ============================================ -->
        <!-- NUEVO - Módulo 4 Session 2: JWT (JJWT)       -->
        <!-- ============================================ -->
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>${jjwt.version}</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>

```
### 1.2.- Creación de clases

#### 1.2.1. DTOs de autenticación

- LoginRequest.java 
```java
package com.tecsup.app.micro.user.presentation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para la petición de login
 *
 * Paquete: com.tecsup.app.micro.user.presentation.dto
 * Sesión 2 - Módulo 4: OAuth 2.0 y JWT
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;
}

```

- LoginResponse.java
```java
package com.tecsup.app.micro.user.presentation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO para la respuesta de login
 *
 * Paquete: com.tecsup.app.micro.user.presentation.dto
 * Sesión 2 - Módulo 4: OAuth 2.0 y JWT
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private String token;
    private String type;
    private String email;
    private List<String> roles;
}

```

#### 1.2.2. Generación y validación de JWT

- JwtTokenProvider.java 

Genera y valida tokens. El JWT_SECRET viene de application.yaml (local) o del K8s Secret (Kubernetes)

```java
package com.tecsup.app.micro.user.infrastructure.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Proveedor de tokens JWT
 *
 * Paquete: com.tecsup.app.micro.user.infrastructure.config
 * Sesión 2 - Módulo 4: OAuth 2.0 y JWT
 *
 * Responsabilidades:
 *   - Generar tokens JWT con email y roles
 *   - Validar tokens JWT
 *   - Extraer claims (email, roles) de un token
 *
 * El JWT_SECRET viene de:
 *   - application.yaml: jwt.secret (desarrollo local)
 *   - K8s Secret: JWT_SECRET (Kubernetes)
 */
@Component
@Slf4j
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration:3600000}")  // Default: 1 hora
    private long jwtExpiration;

    /**
     * Genera un JWT con email como subject y roles como claim
     */
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));

        return Jwts.builder()
                .claims(claims)
                .subject(userDetails.getUsername())  // email
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Extrae el email (subject) del token
     */
    public String getEmailFromToken(String token) {
        return getClaims(token).getSubject();
    }

    /**
     * Extrae los roles del token
     */
    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        Claims claims = getClaims(token);
        return claims.get("roles", List.class);
    }

    /**
     * Valida que el token sea válido (firma correcta, no expirado)
     */
    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("Token JWT expirado: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("Token JWT malformado: {}", e.getMessage());
        } catch (JwtException e) {
            log.warn("Token JWT inválido: {}", e.getMessage());
        }
        return false;
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }
}

```

#### 1.2.3. Filtros JWT

- JwtAuthenticationFilter.java

Valida JWT y carga el usuario desde la BD con CustomUserDetailsService.


```java

package com.tecsup.app.micro.user.infrastructure.config;

import com.tecsup.app.micro.user.infrastructure.security.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro JWT que intercepta cada petición HTTP
 *
 * Paquete: com.tecsup.app.micro.user.infrastructure.config
 * Sesión 2 - Módulo 4: OAuth 2.0 y JWT
 *
 * Flujo:
 *   1. Extrae el token del header Authorization: Bearer <token>
 *   2. Valida el token con JwtTokenProvider
 *   3. Carga el usuario desde BD con CustomUserDetailsService
 *   4. Establece la autenticación en el SecurityContext
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService customUserDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String token = extractTokenFromRequest(request);

        if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
            String email = jwtTokenProvider.getEmailFromToken(token);
            log.debug("JWT válido para usuario: {}", email);

            UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );

            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extrae el token JWT del header Authorization
     * Formato esperado: Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}

```

#### 1.2.4. Activar JWT en SecurityConfig

- SecurityConfig.java
```java
package com.tecsup.app.micro.user.infrastructure.config;

import com.tecsup.app.micro.user.infrastructure.security.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuración de Spring Security para user-service
 *
 * Paquete: com.tecsup.app.micro.user.infrastructure.config
 * Sesión 1: HTTP Basic + roles
 * Sesión 2: Se reemplaza HTTP Basic por JWT (descomentar líneas marcadas)
 *
 * Endpoints:
 *   POST /api/auth/login       → público (Sesión 2)
 *   POST /api/auth/register    → público (Sesión 2)
 *   GET  /api/users/health     → público
 *   GET  /api/users/me         → autenticado
 *   GET/POST/PUT/DELETE /api/users/** → ADMIN
 *   Actuator /actuator/health  → público
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // Habilita @PreAuthorize, @Secured
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;

    // =============================================
    // Descomentar para Sesión 2 (JWT)
    // =============================================
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain userServiceSecurity(HttpSecurity http) throws Exception {
        http
                // Deshabilitar CSRF (no necesario en APIs REST stateless)
                .csrf(csrf -> csrf.disable())

                // Política de sesión: STATELESS (sin estado en servidor)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Reglas de autorización por URL
                .authorizeHttpRequests(auth -> auth

                        // Endpoints públicos
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/users/health").permitAll()
                        .requestMatchers("/actuator/health/**").permitAll()

                        // Solo ADMIN puede gestionar usuarios
                        .requestMatchers("/api/users/**").hasRole("ADMIN")

                        // Todo lo demás requiere autenticación
                        .anyRequest().authenticated()
                )

                // =============================================
                // Sesión 1: HTTP Basic (comentar en Sesión 2)
                // =============================================
//                .httpBasic(basic -> basic
//                        .authenticationEntryPoint((request, response, authException) -> {
//                            response.setStatus(HttpStatus.UNAUTHORIZED.value());
//                            response.setContentType("application/json");
//                            response.getWriter().write(
//                                    """
//                                            {
//                                                "error"  : "No autenticado",
//                                                "status" : 401,
//                                                "message": "Debes autenticarte para acceder a este recurso"
//                                             }
//                                       """);
//                        })
//                )

                // =============================================
                // Sesión 2: JWT (descomentar y comentar httpBasic)
                // =============================================
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                // Manejo de errores de autorización (403)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpStatus.UNAUTHORIZED.value());
                            response.setContentType("application/json");
                            response.getWriter().write(
                                    """
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
                            response.getWriter().write(
                                    """
                                        {
                                            "error"   : "Acceso denegado", 
                                            "status"  : 403,
                                            "message" : "No tienes permisos para acceder a este recurso"
                                        }
                                      """);
                        })
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * AuthenticationManager necesario para el endpoint /api/auth/login (Sesión 2)
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

}

```

#### 1.2.5. Controlador de autenticación

- AuthController.java
```java

package com.tecsup.app.micro.user.presentation.controller;

import com.tecsup.app.micro.user.infrastructure.config.JwtTokenProvider;
import com.tecsup.app.micro.user.infrastructure.security.CustomUserDetailsService;
import com.tecsup.app.micro.user.presentation.dto.LoginRequest;
import com.tecsup.app.micro.user.presentation.dto.LoginResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controlador de autenticación
 *
 * Paquete: com.tecsup.app.micro.user.presentation.controller
 * Sesión 2 - Módulo 4: OAuth 2.0 y JWT
 *
 * Endpoints:
 *   POST /api/auth/login → Recibe email/password, retorna JWT
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService customUserDetailsService;

    /**
     * Endpoint de login
     *
     * Flujo:
     *   1. Recibe email y password
     *   2. AuthenticationManager valida contra BD (CustomUserDetailsService + BCrypt)
     *   3. Si es válido, genera JWT con roles
     *   4. Retorna el token al cliente
     *
     * Ejemplo:
     *   POST /api/auth/login
     *   Body: {"email": "juan.perez@example.com", "password": "admin123"}
     *   Response: {"token": "eyJhbG...", "type": "Bearer", "email": "...", "roles": ["ROLE_ADMIN"]}
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        log.info("Intento de login para: {}", request.getEmail());

        try {
            // Autenticar con email y password
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );

            // Cargar detalles del usuario
            UserDetails userDetails = customUserDetailsService.loadUserByUsername(request.getEmail());

            // Generar JWT
            String token = jwtTokenProvider.generateToken(userDetails);

            // Construir respuesta
            LoginResponse response = LoginResponse.builder()
                    .token(token)
                    .type("Bearer")
                    .email(request.getEmail())
                    .roles(userDetails.getAuthorities().stream()
                            .map(GrantedAuthority::getAuthority)
                            .collect(Collectors.toList()))
                    .build();

            log.info("Login exitoso para: {}", request.getEmail());
            return ResponseEntity.ok(response);

        } catch (BadCredentialsException e) {
            log.warn("Credenciales inválidas para: {}", request.getEmail());
            return ResponseEntity.status(401).body(Map.of(
                    "error", "Credenciales inválidas",
                    "status", 401,
                    "message", "Email o password incorrectos"
            ));
        }
    }
}

```

#### 1.2.6. Configuración application.yaml y application-kubernetes.yaml

- application.yaml y application-kubernetes.yaml

Agregar configuración de JWT

```yaml


# ============================================
# JWT CONFIGURATION (Sesión 2)
# ============================================
jwt:
  secret: ${JWT_SECRET:m1S3cr3tK3yJWT_T3csup2025!@#SecureToken}
  expiration: 3600000  # 1 hora en milisegundos



```

### 1.3.- Verificar en localhost

- Ejecutar la aplicación y probar los endpoints con Postman o curl.
```
# Sin autenticación → 401
curl http://localhost:8081/api/users

# Con ADMIN → 401
curl -u juan.perez@example.com:admin123 http://localhost:8081/api/users

# Con USER intentando acceso ADMIN → 401
curl -u maria.garcia@example.com:user123 http://localhost:8081/api/users
```

## 2.- Desplegar en Kubernetes con AUTH2.0 y JWT


### 2.1.- Construir imagen Docker y probar localmente (ver README.md)

#### Constuir imagen

```
# Compilar el proyecto (si es necesario)
mvn clean package -DskipTests

# Construir imagen
docker build -t user-service:1.0 .

# Verificar imagen creada
docker images | grep user-service

# Deberías ver:
# user-service   1.0   abc123def456   1 minute ago   230MB

```

#### Probar la imagen Docker

```
# Ejecutar contenedor de la app
docker run -p 8081:8081 \
-e SPRING_PROFILES_ACTIVE=kubernetes \
-e DB_URL=jdbc:postgresql://host.docker.internal:5434/userdb \
-e DB_USERNAME=postgres \
-e DB_PASSWORD=postgres \
user-service:1.0

# En otra terminal, probar

# Health check
curl http://localhost:8081/actuator/health

# Respuesta esperada:
# {"status":"UP","groups":["liveness","readiness"]}

# Listar usuarios
curl http://localhost:8081/api/users

# Respuesta esperada
 {
     "error": "No autenticado",
     "status": 401,
     "message": "Debes autenticarte para acceder a este recurso"
 }

# Detener el servicio Ctrl + C
```

### 2.2.- Desplegar en Kubernetes (ver README.md)

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
 kubectl rollout restart deployment user-service -n user-service
```
- Verificar despliegue, servicio y pods:
```
# Verificar despliegue
kubectl get deployments -n user-service

# Verificar servicio
kubectl get service -n user-service

# Verificar pods
kubectl get pods  -n user-service

# Ver detalles de un pod
kubectl describe pod <POD_NAME> -n user-service

# Ver logs en tiempo real
kubectl logs -f <POD_NAME> -n user-service

```


### 2.3.- Probar autenticación en Kubernetes

```

# 1. Acceder sin autorizacion
curl http://localhost:30081/api/users

# Resultado esperado
 {
     "error": "No autenticado",
     "status": 401,
     "message": "Debes autenticarte para acceder a este recurso"
 }

# 2. Login como ADMIN → obtener JWT
TOKEN=$(curl -s -X POST http://localhost:30081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"juan.perez@example.com","password":"admin123"}' \
  | jq -r '.token')

# 3. Verificar que el TOKEN se ha creado
echo $TOKEN

# 4. Acceder con JWT de ADMIN → 200
curl -H "Authorization: Bearer $TOKEN" http://localhost:30081/api/users

```
