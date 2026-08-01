package com.tecsup.app.micro.product.infrastructure.persistence.mapper;

import com.tecsup.app.micro.product.domain.model.Product;
import com.tecsup.app.micro.product.infrastructure.persistence.entity.ProductEntity;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * Mapper entre entidades JPA y modelo de dominio usando MapStruct
 */
@Mapper(componentModel = "spring")
public interface ProductPersistenceMapper {
    
    /**
     * Convierte ProductEntity a Product de dominio
     */
    Product toDomain(ProductEntity entity);
    
    /**
     * Convierte Product de dominio a ProductEntity
     */
    ProductEntity toEntity(Product product);
    
    /**
     * Convierte lista de ProductEntity a lista de Product
     */
    List<Product> toDomainList(List<ProductEntity> entities);
}
