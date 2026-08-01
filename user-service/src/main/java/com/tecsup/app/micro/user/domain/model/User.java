package com.tecsup.app.micro.user.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * User Domain Model (Core Business Entity)
 * Esta es la entidad de dominio pura, sin dependencias de frameworks
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    
    private Long id;
    private String name;
    private String email;
    private String phone;
    private String address;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    /**
     * Valida que el usuario tenga los datos mínimos requeridos
     */
    public boolean isValid() {
        return name != null && !name.trim().isEmpty() 
            && email != null && !email.trim().isEmpty()
            && isValidEmail(email);
    }
    
    /**
     * Valida el formato del email
     */
    private boolean isValidEmail(String email) {
        String emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return email.matches(emailRegex);
    }
}
