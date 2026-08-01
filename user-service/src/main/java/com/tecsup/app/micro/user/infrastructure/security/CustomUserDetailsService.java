package com.tecsup.app.micro.user.infrastructure.security;

import com.tecsup.app.micro.user.infrastructure.persistence.entity.UserEntity;
import com.tecsup.app.micro.user.infrastructure.persistence.repository.JpaUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio de autenticación que carga usuarios desde userdb (PostgreSQL)
 *
 * Paquete: com.tecsup.app.micro.user.infrastructure.security
 * Sesión 1 - Módulo 4: Seguridad en Microservicios
 *
 * Usa el email como username para autenticación.
 * Lee los roles desde la tabla user_roles (relación N:N).
 * Los passwords están almacenados con BCrypt en la tabla users.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final JpaUserRepository jpaUserRepository;

    /**
     * Carga un usuario por email desde userdb.
     * Spring Security invoca este método automáticamente durante la autenticación.
     *
     * @param email El email del usuario (usado como username)
     * @return UserDetails con email, password (BCrypt) y roles
     * @throws UsernameNotFoundException si el email no existe en la BD
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.info("Autenticando usuario con email: {}", email);

        UserEntity userEntity = jpaUserRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Usuario no encontrado: {}", email);
                    return new UsernameNotFoundException("Usuario no encontrado con email: " + email);
                });

        // Convertir roles de la BD a GrantedAuthority de Spring Security
        // Ejemplo: RoleEntity(name="ROLE_ADMIN") → SimpleGrantedAuthority("ROLE_ADMIN")
        List<GrantedAuthority> authorities = userEntity.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName()))
                .collect(Collectors.toList());

        log.info("Usuario autenticado: {} con roles: {}", email, authorities);

        return new User(
                userEntity.getEmail(),       // username = email
                userEntity.getPassword(),    // password BCrypt desde BD
                userEntity.getEnabled(),     // enabled
                true,                        // accountNonExpired
                true,                        // credentialsNonExpired
                true,                        // accountNonLocked
                authorities                  // roles
        );
    }
}