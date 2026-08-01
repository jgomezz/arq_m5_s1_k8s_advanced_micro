package com.tecsup.app.micro.user.presentation.mapper;

import com.tecsup.app.micro.user.domain.model.User;
import com.tecsup.app.micro.user.presentation.dto.CreateUserRequest;
import com.tecsup.app.micro.user.presentation.dto.UpdateUserRequest;
import com.tecsup.app.micro.user.presentation.dto.UserResponse;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * Mapper entre DTOs de presentación y modelo de dominio usando MapStruct
 */
@Mapper(componentModel = "spring")
public interface UserDtoMapper {
    
    /**
     * Convierte CreateUserRequest a User de dominio
     */
    User toDomain(CreateUserRequest request);
    
    /**
     * Convierte UpdateUserRequest a User de dominio
     */
    User toDomain(UpdateUserRequest request);
    
    /**
     * Convierte User de dominio a UserResponse
     */
    UserResponse toResponse(User user);
    
    /**
     * Convierte lista de Users a lista de UserResponse
     */
    List<UserResponse> toResponseList(List<User> users);
}
