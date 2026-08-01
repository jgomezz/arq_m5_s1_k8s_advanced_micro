package com.tecsup.app.micro.user.application.usecase;

import com.tecsup.app.micro.user.domain.model.User;
import com.tecsup.app.micro.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Caso de uso: Obtener todos los usuarios
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GetAllUsersUseCase {
    
    private final UserRepository userRepository;
    
    public List<User> execute() {
        log.debug("Executing GetAllUsersUseCase");
        return userRepository.findAll();
    }
}
