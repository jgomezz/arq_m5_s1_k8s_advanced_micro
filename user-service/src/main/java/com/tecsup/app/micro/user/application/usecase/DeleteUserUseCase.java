package com.tecsup.app.micro.user.application.usecase;

import com.tecsup.app.micro.user.domain.exception.UserNotFoundException;
import com.tecsup.app.micro.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Caso de uso: Eliminar un usuario
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DeleteUserUseCase {
    
    private final UserRepository userRepository;
    
    public void execute(Long id) {
        log.debug("Executing DeleteUserUseCase for id: {}", id);
        
        // Verificar que el usuario existe
        if (!userRepository.findById(id).isPresent()) {
            throw new UserNotFoundException(id);
        }
        
        // Eliminar usuario
        userRepository.deleteById(id);
        log.info("User deleted successfully with id: {}", id);
    }
}
