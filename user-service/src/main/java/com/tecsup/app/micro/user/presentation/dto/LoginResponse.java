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
