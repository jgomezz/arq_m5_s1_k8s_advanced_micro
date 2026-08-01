package com.tecsup.app.micro.user.application.usecase;

import com.tecsup.app.micro.user.domain.exception.DuplicateEmailException;
import com.tecsup.app.micro.user.domain.exception.InvalidUserDataException;
import com.tecsup.app.micro.user.domain.exception.UserNotFoundException;
import com.tecsup.app.micro.user.domain.model.User;
import com.tecsup.app.micro.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Caso de uso: Actualizar un usuario existente
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UpdateUserUseCase {
    
    private final UserRepository userRepository;
    
    public User execute(Long id, User userDetails) {
        log.debug("Executing UpdateUserUseCase for id: {}", id);
        
        // Verificar que el usuario existe
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        
        // Validar datos del usuario
        if (!userDetails.isValid()) {
            throw new InvalidUserDataException("Invalid user data. Name and valid email are required.");
        }
        
        // Si el email cambió, verificar que no exista en otro usuario
        if (!existingUser.getEmail().equals(userDetails.getEmail())) {
            if (userRepository.existsByEmail(userDetails.getEmail())) {
                throw new DuplicateEmailException(userDetails.getEmail());
            }
        }
        
        // Actualizar campos
        existingUser.setName(userDetails.getName());
        existingUser.setEmail(userDetails.getEmail());
        existingUser.setPhone(userDetails.getPhone());
        existingUser.setAddress(userDetails.getAddress());
        
        // Guardar cambios
        User updatedUser = userRepository.save(existingUser);
        log.info("User updated successfully with id: {}", updatedUser.getId());
        
        return updatedUser;
    }
}
