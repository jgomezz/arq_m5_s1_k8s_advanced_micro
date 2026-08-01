package com.tecsup.app.micro.product.infrastructure.web.mapper;

import com.tecsup.app.micro.product.domain.model.Product;

import com.tecsup.app.micro.product.infrastructure.web.dto.CreateProductRequest;
import com.tecsup.app.micro.product.infrastructure.web.dto.ProductResponse;
import com.tecsup.app.micro.product.infrastructure.web.dto.UpdateProductRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * Mapper entre DTOs de presentación y modelo de dominio usando MapStruct
 */
@Mapper(componentModel = "spring")
public interface ProductDtoMapper {
    
    /**
     * Convierte CreateProductRequest a Product de dominio
     */
    Product toDomain(CreateProductRequest request);
    
    /**
     * Convierte UpdateProductRequest a Product de dominio
     */
    Product toDomain(UpdateProductRequest request);
    
    /**
     * Convierte Product de dominio a ProductResponse
     */
    @Mapping(target = "available", expression = "java(product.isAvailable())")
    ProductResponse toResponse(Product product);
    
    /**
     * Convierte lista de Products a lista de ProductResponse
     */
    List<ProductResponse> toResponseList(List<Product> products);
}
