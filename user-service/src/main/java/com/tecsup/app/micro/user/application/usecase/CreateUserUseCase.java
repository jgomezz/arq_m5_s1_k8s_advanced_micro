package com.tecsup.app.micro.user.application.usecase;

import com.tecsup.app.micro.user.domain.exception.DuplicateEmailException;
import com.tecsup.app.micro.user.domain.exception.InvalidUserDataException;
import com.tecsup.app.micro.user.domain.model.User;
import com.tecsup.app.micro.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Caso de uso: Crear un nuevo usuario
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CreateUserUseCase {
    
    private final UserRepository userRepository;
    
    public User execute(User user) {
        log.debug("Executing CreateUserUseCase for email: {}", user.getEmail());
        
        // Validar datos del usuario
        if (!user.isValid()) {
            throw new InvalidUserDataException("Invalid user data. Name and valid email are required.");
        }
        
        // Verificar que el email no exista
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new DuplicateEmailException(user.getEmail());
        }
        
        // Guardar usuario
        User savedUser = userRepository.save(user);
        log.info("User created successfully with id: {}", savedUser.getId());
        
        return savedUser;
    }
}
