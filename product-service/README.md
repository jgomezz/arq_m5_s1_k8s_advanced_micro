# KUBERNETES CON SPRING BOOT Y DOCKER

### 1.- Pre-requisitos

#### Verificar que user-service está ejecutandose
```
# Verificar que user-service está en Kubernetes
kubectl get all -n user-service

# Deberías ver:
# - 1 pods corriendo
# - 1 deployment
# - 1 service

# Probar que user-service funciona
curl http://localhost:30081/api/users
```

#### Verificar PostgreSQL de user-service
```
# Verificar que PostgreSQL de user está corriendo
docker ps | grep postgres-user

# Output esperado:
# postgres-userdb  postgres:16-alpine  Up  0.0.0.0:5432->5432/tcp

```


### 2.- Actualizar profile de Kubernetes `application-kubernetes.yaml`

``` yaml 
# ============================================
# APPLICATION CONFIGURATION FOR KUBERNETES
# ============================================
# Product Service - Configuración para Kubernetes

server:
  port: 8082

spring:
  application:
    name: product-service
  
  # ============================================
  # DATASOURCE - ProductDB
  # ============================================
  datasource:
    url: ${DB_URL:jdbc:postgresql://host.docker.internal:5433/productdb}
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:postgres}
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: ${POOL_SIZE:10}
      minimum-idle: 5
      connection-timeout: 20000
  
  # ============================================
  # JPA CONFIGURATION
  # ============================================
  jpa:
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
        jdbc:
          lob:
            non_contextual_creation: true
    hibernate:
      ddl-auto: ${DDL_AUTO:validate}
    show-sql: ${SHOW_SQL:false}

# ============================================
# ACTUATOR (Para health checks de Kubernetes)
# ============================================
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      probes:
        enabled: true
      show-details: when-authorized
  health:
    livenessState:
      enabled: true
    readinessState:
      enabled: true

# ============================================
# LOGGING
# ============================================
logging:
  level:
    com.tecsup.app.micro.product: ${LOG_LEVEL:INFO}
    org.hibernate.SQL: ${SQL_LOG_LEVEL:WARN}

# ============================================
# USER SERVICE URL - Comunicación entre servicios
# ============================================
# En Kubernetes, usar DNS interno
# Formato: http://<service-name>.<namespace>.svc.cluster.local
user:
  service:
    url: ${USER_SERVICE_URL:http://user-service.user-service.svc.cluster.local}
```


### 3.- Agregar Actuator

- Agregar dependencia de Actuator en `pom.xml`

```xml

<!-- Spring Boot Actuator (para health checks de Kubernetes) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>

```

- Compilar para verificar
```
cd product-service
mvn clean compile
```


### 4 - Iniciar PostgreSQL para productdb

#### Iniciar PostreSQL

```
# Desde el directorio raíz del proyecto
docker-compose -f ../docker-compose.yml up -d

# Ver logs
docker logs postgres-product

```

#### Crear las tablas 
```
```



### 5.- Dockerizar product-service

#### Compilar con perfil Kubernetes

```
mvn clean package -DskipTests
```

#### Dockerfile

```
# ============================================
# ETAPA 2: RUNTIME
# ============================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copiar JAR desde etapa de build
COPY target/*.jar /app/product-service.jar

# Puerto
EXPOSE 8082

# Variables de entorno
ENV JAVA_OPTS="-Xmx512m -Xms256m"

# Comando de inicio
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/product-service.jar"]
```

#### Constuir imagen

```
# Construir imagen
docker build -t product-service:1.0 .

# Este proceso toma 2-3 minutos la primera vez
# Ver progreso: [1/2] STEP X/Y...

# Verificar imagen creada
```

#### Probar la imagen Docker
```
# Ejecutar contenedor
docker run -p 8082:8082 \
  -e SPRING_PROFILES_ACTIVE=kubernetes \
  -e DB_URL=jdbc:postgresql://host.docker.internal:5433/productdb \
  -e DB_USERNAME=postgres \
  -e DB_PASSWORD=postgres \
  -e USER_SERVICE_URL=http://host.docker.internal:30081 \
  product-service:1.0

# En otra terminal, probar
curl http://localhost:8082/actuator/health

# Ctrl+C para detener
```

### 6.- Desplegar en Kubernetes

#### Crear Namespace en Kubernetes

- Aplicar namespace
```
kubectl apply -f k8s/00-namespace.yaml

# Output:
# namespace/product-service created
```
- Verificar namespace
```
kubectl get namespaces  

# Deberías ver:
# user-service       Active   X minutes
# product-service    Active   5s
```

#### Crear ConfigMap

- Aplicar ConfigMap
```
kubectl apply -f k8s/01-configmap.yaml

# Output:
# configmap/product-service-config created
```
- Verificar ConfigMap
```
kubectl get configmap -n product-service

# Ver contenido
kubectl describe configmap product-service-config -n product-service
```


#### Entender la URL de user-service

```
http://user-service.user-service.svc.cluster.local
      └─────┬─────┘ └────┬─────┘ └┬┘ └────┬────┘
        Service       Namespace  Tipo   Cluster

Explicación:
    user-service: Nombre del Service en K8s
    user-service: Namespace donde está
    svc: Tipo "service"
    cluster.local: Dominio del cluster
```

#### Crear Secret
```
# Aplicar
kubectl apply -f k8s/02-secret.yaml

# Output:
# secret/product-service-secret created

# Verificar
kubectl get secret -n product-service

# Ver detalle 
kubectl describe secret product-service-secret -n product-service

```

#### Desplegar Product-Service

- Aplicar Deployment
```
kubectl apply -f k8s/03-deployment.yaml

# Output:
# deployment.apps/product-service created
```


- En caso necesites redesplegar (por ejemplo, después de corregir un error en el Deployment):
```
 kubectl rollout restart deployment product-service -n product-service
```


- Verificar pods
```
kubectl get pods -n product-service 
```

- Ver logs
```
# Ver logs
kubectl logs -f <POD_NAME> -n product-service

# Ver descripción completa del pod
kubectl describe pod <POD_NAME> -n product-service

```

- Verificar variables de entorno

```
# Entrar al pod
kubectl exec -it <POD_NAME> -n product-service -- /bin/sh

# Ver variables
env | grep DB_
env | grep USER_SERVICE_URL

# Deberías ver:
# USER_SERVICE_URL=http://user-service.user-service.svc.cluster.local

# Salir
exit

```

#### Exponer con Service

- Aplicar Service

```
kubectl apply -f k8s/04-service.yaml

# Output:
# service/product-service created
```

- Verificar Service
```

kubectl get service -n product-service

# Output:
# NAME              TYPE       CLUSTER-IP      PORT(S)        AGE
# product-service   NodePort   10.96.xxx.xxx   80:30082/TCP   5s

```

- Probar product-service
```
# Health check
curl http://localhost:30082/actuator/health

# Output esperado:
# {"status":"UP"}
```
# Listar productos
```
curl http://localhost:30082/api/products
```

### 7.- Probar Comunicación Entre Servicios

#### Ver todos los servicios

```
# En namespace user-service
kubectl get all -n user-service

# En namespace product-service
kubectl get all -n product-service
```

#### Probar comunicación desde product-service a user-service

```
# Obtener un producto (debería incluir info del owner desde user-service)
curl http://localhost:30082/api/products/1

# Output esperado (ejemplo):
# {
#   "id": 1,
#   "name": "Laptop",
#   "owner": {
#     "id": 1,
#     "name": "Juan Perez",  ← Obtenido desde user-service
#     "email": "juan@example.com"
#   }
# }
```

#### Ver logs de comunicación
```
# Ver logs de product-service
kubectl logs -f <POD_NAME> -n product-service

# Deberías ver:
# Calling User Service (PostgreSQL userdb) to get user with id: 1
# User retrieved successfully from userdb: UserDTO(id=1, name=Juan Perez, ...)
```