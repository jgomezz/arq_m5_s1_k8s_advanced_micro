package com.tecsup.app.micro.product.infrastructure.persistence.repository;

import com.tecsup.app.micro.product.domain.model.Product;
import com.tecsup.app.micro.product.domain.repository.ProductRepository;
import com.tecsup.app.micro.product.infrastructure.persistence.entity.ProductEntity;
import com.tecsup.app.micro.product.infrastructure.persistence.mapper.ProductPersistenceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Implementación del repositorio de Producto (Adaptador)
 * Conecta el dominio con la infraestructura de persistencia usando MapStruct
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class ProductRepositoryImpl implements ProductRepository {
    
    private final JpaProductRepository jpaProductRepository;
    private final ProductPersistenceMapper mapper;
    
    @Override
    public List<Product> findAll() {
        log.debug("Finding all products");
        return mapper.toDomainList(jpaProductRepository.findAll());
    }
    
    @Override
    public Optional<Product> findById(Long id) {
        log.debug("Finding product by id: {}", id);
        return jpaProductRepository.findById(id)
                .map(mapper::toDomain);
    }
    
    @Override
    public List<Product> findByCategory(String category) {
        log.debug("Finding products by category: {}", category);
        return mapper.toDomainList(jpaProductRepository.findByCategory(category));
    }
    
    @Override
    public List<Product> findByCreatedBy(Long userId) {
        log.debug("Finding products by createdBy: {}", userId);
        return mapper.toDomainList(jpaProductRepository.findByCreatedBy(userId));
    }
    
    @Override
    public List<Product> findAvailableProducts() {
        log.debug("Finding available products");
        return mapper.toDomainList(jpaProductRepository.findAvailableProducts());
    }
    
    @Override
    public Product save(Product product) {
        log.debug("Saving product: {}", product.getName());
        ProductEntity entity = mapper.toEntity(product);
        ProductEntity savedEntity = jpaProductRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }
    
    @Override
    public void deleteById(Long id) {
        log.debug("Deleting product by id: {}", id);
        jpaProductRepository.deleteById(id);
    }
    
    @Override
    public boolean existsById(Long id) {
        log.debug("Checking if product exists: {}", id);
        return jpaProductRepository.existsById(id);
    }
}
