package com.tecsup.app.micro.user.application.service;

import com.tecsup.app.micro.user.application.usecase.*;
import com.tecsup.app.micro.user.domain.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio de Aplicación de Usuario
 * Orquesta los casos de uso y maneja las transacciones
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserApplicationService {
    
    private final GetAllUsersUseCase getAllUsersUseCase;
    private final GetUserByIdUseCase getUserByIdUseCase;
    private final CreateUserUseCase createUserUseCase;
    private final UpdateUserUseCase updateUserUseCase;
    private final DeleteUserUseCase deleteUserUseCase;
    
    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return getAllUsersUseCase.execute();
    }
    
    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        return getUserByIdUseCase.execute(id);
    }
    
    @Transactional
    public User createUser(User user) {
        return createUserUseCase.execute(user);
    }
    
    @Transactional
    public User updateUser(Long id, User user) {
        return updateUserUseCase.execute(id, user);
    }
    
    @Transactional
    public void deleteUser(Long id) {
        deleteUserUseCase.execute(id);
    }
}
