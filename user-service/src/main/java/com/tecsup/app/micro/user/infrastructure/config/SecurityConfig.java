package com.tecsup.app.micro.user.infrastructure.config;

import com.tecsup.app.micro.user.infrastructure.security.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
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

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

}
