# Microservicio User-Service - Adaptar base de datos y despliegue para seguridad

### 1.- Modificar estructura de base de datos

<img src="images/userdb_update.png" alt="Estructura de la base de datos" />


- Crear database/V4__ADD_SECURITY_TABLES.sql
```
-- ============================================
-- Migration: V4__ADD_SECURITY_TABLES.sql
-- Description: Crear tablas de seguridad (roles, user_roles)
--              y agregar campos de autenticación a users
-- Database: userdb
-- ============================================

-- ============================================
-- 1. Agregar campos de seguridad a tabla users
-- ============================================
ALTER TABLE users ADD COLUMN password VARCHAR(100);
ALTER TABLE users ADD COLUMN enabled BOOLEAN NOT NULL DEFAULT true;

-- ============================================
-- 2. Tabla de roles
-- ============================================
CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    description VARCHAR(200),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_roles_name UNIQUE (name),
    CONSTRAINT chk_role_name_not_empty CHECK (LENGTH(TRIM(name)) > 0)
);

COMMENT ON TABLE roles IS 'Roles del sistema para autorización';
COMMENT ON COLUMN roles.name IS 'Nombre del rol (ROLE_ADMIN, ROLE_USER, etc.)';

-- ============================================
-- 3. Tabla intermedia user_roles (N:N)
-- ============================================
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (user_id, role_id),

    CONSTRAINT fk_user_roles_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_user_roles_role
        FOREIGN KEY (role_id)
        REFERENCES roles(id)
        ON DELETE CASCADE
);

COMMENT ON TABLE user_roles IS 'Relación N:N entre usuarios y roles';

-- ============================================
-- 4. Índices
-- ============================================
CREATE INDEX idx_user_roles_user_id ON user_roles(user_id);
CREATE INDEX idx_user_roles_role_id ON user_roles(role_id);
CREATE INDEX idx_users_enabled ON users(enabled);
```

- Crear database/V5__INSERT_SECURITY_DATA.sql

```
-- ============================================
-- Migration: V5__INSERT_SECURITY_DATA.sql
-- Description: Insertar roles y asignar credenciales a usuarios
-- Database: userdb
-- ============================================
-- Passwords codificados con BCrypt (strength 10):
--   admin123 → $2a$10$e77InV9/.OZ68nmbd9Co2uhuYu9g7eBNqu3nDyRHcC5x0cIH0YBJW
--   user123  → $2a$10$tQWrbvoAohyaYiDC6e9rNO9Wf7w0eLQamxD2TJhCWKXbJjqjRTXUu
-- ============================================

-- ============================================
-- 1. Insertar roles
-- ============================================
INSERT INTO roles (name, description) VALUES
('ROLE_ADMIN', 'Administrador del sistema - acceso total'),
('ROLE_USER',  'Usuario estándar - acceso limitado');

-- ============================================
-- 2. Actualizar passwords de usuarios existentes
-- ============================================
-- Juan Pérez → admin (password: admin123)
UPDATE users
SET password = '$2a$10$e77InV9/.OZ68nmbd9Co2uhuYu9g7eBNqu3nDyRHcC5x0cIH0YBJW',
    enabled = true
WHERE id = 1;

-- María García → user (password: user123)
UPDATE users
SET password = '$2a$10$tQWrbvoAohyaYiDC6e9rNO9Wf7w0eLQamxD2TJhCWKXbJjqjRTXUu',
    enabled = true
WHERE id = 2;

-- Carlos López → user (password: user123)
UPDATE users
SET password = '$2a$10$tQWrbvoAohyaYiDC6e9rNO9Wf7w0eLQamxD2TJhCWKXbJjqjRTXUu',
    enabled = true
WHERE id = 3;

-- Ana Torres → admin (password: admin123)
UPDATE users
SET password = '$2a$10$e77InV9/.OZ68nmbd9Co2uhuYu9g7eBNqu3nDyRHcC5x0cIH0YBJW',
    enabled = true
WHERE id = 4;

-- Roberto Sánchez → user (password: user123)
UPDATE users
SET password = '$2a$10$tQWrbvoAohyaYiDC6e9rNO9Wf7w0eLQamxD2TJhCWKXbJjqjRTXUu',
    enabled = true
WHERE id = 5;

-- ============================================
-- 3. Hacer password NOT NULL después de poblar
-- ============================================
ALTER TABLE users ALTER COLUMN password SET NOT NULL;

-- ============================================
-- 4. Asignar roles a usuarios
-- ============================================
-- Juan Pérez (id=1) → ADMIN + USER
INSERT INTO user_roles (user_id, role_id) VALUES
(1, (SELECT id FROM roles WHERE name = 'ROLE_ADMIN')),
(1, (SELECT id FROM roles WHERE name = 'ROLE_USER'));

-- María García (id=2) → USER
INSERT INTO user_roles (user_id, role_id) VALUES
(2, (SELECT id FROM roles WHERE name = 'ROLE_USER'));

-- Carlos López (id=3) → USER
INSERT INTO user_roles (user_id, role_id) VALUES
(3, (SELECT id FROM roles WHERE name = 'ROLE_USER'));

-- Ana Torres (id=4) → ADMIN + USER
INSERT INTO user_roles (user_id, role_id) VALUES
(4, (SELECT id FROM roles WHERE name = 'ROLE_ADMIN')),
(4, (SELECT id FROM roles WHERE name = 'ROLE_USER'));

-- Roberto Sánchez (id=5) → USER
INSERT INTO user_roles (user_id, role_id) VALUES
(5, (SELECT id FROM roles WHERE name = 'ROLE_USER'));

```
- Cargar migraciones a la base de datos : userdb

### 2.- Modificar manifestos k8s

- Codificar en Base64 las nuevas variables de entorno (DB_PASSWORD y DB_USERNAME)

- Modificar k8s/02-secret.yaml
```
# ============================================
# SECRET - Datos SENSIBLES (codificados en base64)
# ============================================
# Para codificar:   echo -n "valor" | base64
# Para decodificar: echo "dmFsb3I=" | base64 -d
# ============================================
apiVersion: v1
kind: Secret
metadata:
  name: user-service-secret
  namespace: user-service
  labels:
    app: user-service

type: Opaque

# Valores codificados en base64 (más seguro que stringData)                          # CAMBIOS
data:
  DB_USERNAME: cG9zdGdyZXM=                                          # postgres
  DB_PASSWORD: cG9zdGdyZXM=                                          # postgres
  JWT_SECRET: bTFTM2NyM3RLM3lKV1RfVDNjc3VwMjAyNSFAI1NlY3VyZVRva2Vu  # m1S3cr3tK3yJWT_T3csup2025!@#SecureToken
```

- Modificar k8s/01-configmap.yaml: **Se elimina DB_USERNAME porque se movió al k8s/02-secret.yaml**
```
# ============================================
# CONFIGMAP - Configuración NO sensible
# ============================================
# Nota: DB_USERNAME se movió a Secret (02-secret.yaml)
# ============================================
apiVersion: v1
kind: ConfigMap
metadata:
  name: user-service-config
  namespace: user-service
  labels:
    app: user-service

# Pares clave-valor
data:
  # URL de la base de datos
  # host.docker.internal apunta a localhost de tu máquina
  DB_URL: "jdbc:postgresql://host.docker.internal:5434/userdb"

  # Configuración de JPA
  DDL_AUTO: "validate"
  SHOW_SQL: "false"

  # Pool de conexiones
  POOL_SIZE: "10"

  # Logging
  LOG_LEVEL: "INFO"
  SQL_LOG_LEVEL: "WARN"

  # JVM
  JAVA_OPTS: "-Xmx512m -Xms256m"

```

- Modificar k8s/03-deployment.yaml: **Se eliminan las variables de entorno DB_USERNAME y DB_PASSWORD porque se movieron al k8s/02-secret.yaml**
```
# ============================================
# DEPLOYMENT - Gestión de Pods
# ============================================
apiVersion: apps/v1
kind: Deployment
metadata:
  name: user-service
  namespace: user-service
  labels:
    app: user-service

spec:
  # ========================================
  # Número de réplicas (copias)
  # ========================================
  replicas: 1  # Empezamos con 1 pod

  # ========================================
  # Selector: ¿Qué pods gestionar?
  # ========================================
  selector:
    matchLabels:
      app: user-service

  # ========================================
  # Template: Cómo crear cada pod
  # ========================================
  template:
    metadata:
      labels:
        app: user-service

    spec:
      containers:
        - name: user-service

          # Imagen Docker
          image: user-service:1.0

          # imagePullPolicy:
          # - Never: Solo usar imagen local (desarrollo)
          # - Always: Siempre descargar (producción)
          imagePullPolicy: Never

          # Puerto del contenedor
          ports:
            - containerPort: 8081
              name: http

          # ====================================
          # VARIABLES DE ENTORNO
          # ====================================
          env:
            # Spring profile
            - name: SPRING_PROFILES_ACTIVE
              value: "kubernetes"

            # Desde ConfigMap
            - name: DDL_AUTO
              valueFrom:
                configMapKeyRef:
                  name: user-service-config
                  key: DDL_AUTO

            - name: LOG_LEVEL
              valueFrom:
                configMapKeyRef:
                  name: user-service-config
                  key: LOG_LEVEL

            - name: JAVA_OPTS
              valueFrom:
                configMapKeyRef:
                  name: user-service-config
                  key: JAVA_OPTS

            # Desde Secret
            - name: DB_USERNAME
              valueFrom:
                secretKeyRef:
                  name: user-service-secret
                  key: DB_USERNAME

            - name: DB_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: user-service-secret
                  key: DB_PASSWORD

            - name: JWT_SECRET
              valueFrom:
                secretKeyRef:
                  name: user-service-secret
                  key: JWT_SECRET

```

### 3.- Desplegar en Kubernetes

- Configurar Contexto para Docker Desktop
```
# Ver los contextos
kubectl config get-contexts

# Cambiar al contexto de Docker Desktop
kubectl config use-context docker-desktop

```

- Borrar el despliegue de user-service 
```
# Borrar todo el namespace (incluye deployments, services, configmaps, secrets)
kubectl delete -f k8s/00-namespace.yaml
kubectl delete -f k8s/01-configmap.yaml
kubectl delete -f k8s/02-secret.yaml
kubectl delete -f k8s/03-deployment.yaml
kubectl delete -f k8s/04-service.yaml
```

- Volver a desplegar user-service
```
kubectl apply -f k8s/00-namespace.yaml
kubectl apply -f k8s/01-configmap.yaml
kubectl apply -f k8s/02-secret.yaml
kubectl apply -f k8s/03-deployment.yaml
kubectl apply -f k8s/04-service.yaml
```

- Verificar el despliegue
```
# Verificar el deployment  
kubectl get deployments -n user-service

# Verificar Service
kubectl get service -n user-service
 
# Verificar pods
kubectl get pods -n user-service  
```

- En caso necesites redesplegar (por ejemplo, después de corregir un error en el Deployment):
```
 kubectl rollout restart deployment user-service -n user-service
```

- Ver logs
```
# Ver logs
kubectl logs -f <POD_NAME> -n user-service

# Ver descripción completa del pod
kubectl describe pod <POD_NAME> -n user-service

```

- Probar user-service
```
# Health check
curl http://localhost:30081/actuator/health

# Output esperado:
# {"status":"UP","groups":["liveness","readiness"]}
```
### 4.- Listar users
```
curl http://localhost:30081/api/users
```

#### Ver logs
```
# Ver logs de user-service
kubectl logs -f <POD_NAME> -n user-service


