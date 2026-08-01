
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
