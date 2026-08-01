package com.tecsup.app.micro.product.infrastructure.client.mapper;


import com.tecsup.app.micro.product.domain.model.User;
import com.tecsup.app.micro.product.infrastructure.client.dto.UserDTO;
import com.tecsup.app.micro.product.infrastructure.web.dto.UserResponse; 
import org.mapstruct.Mapper;

/**
 * Mapper entre entidades JPA y modelo de dominio usando MapStruct
 */
@Mapper(componentModel = "spring")
public interface UserDtoMapper {
    
    User toDomain(UserDTO dto);

    UserResponse toResponse(User user);

}
