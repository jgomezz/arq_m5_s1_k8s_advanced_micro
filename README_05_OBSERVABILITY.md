#  Microservicio Product-Service and User-service : Observability

```
┌──────────────────────────────────────────────────────────────────┐
│                     Docker Desktop / K8s                         │
│                                                                  │
│  ┌───────────────┐      ┌───────────────┐                        │
│  │ user-service  │      │product-service│                        │
│  │   :8081       │      │   :8082       │                        │
│  │  /actuator/   │      │  /actuator/   │                        │
│  │  prometheus   │      │  prometheus   │                        │
│  └───────┬───────┘      └───────┬───────┘                        │
│          │  métricas            │  métricas                      │
│          ▼                      ▼                                │
│  ┌─────────────────────────────────┐     ┌────────────────┐      │
│  │         PROMETHEUS              │     │    ZIPKIN      │      │
│  │    (scrape cada 15s)            │     │  (recibe trazas│      │
│  │         :9090                   │     │   via HTTP)    │      │
│  └──────────────┬──────────────────┘     │    :9411       │      │
│                 │  datasource            └────────────────┘      │
│                 ▼                                                │
│  ┌─────────────────────────────────┐                             │
│  │          GRAFANA                │                             │
│  │   (dashboards + alertas)        │                             │
│  │          :3000                  │                             │
│  └─────────────────────────────────┘                             │
└──────────────────────────────────────────────────────────────────┘

```
## 1.- Estructura de la solución de Observabilidad

```

```

## 2.- Modificar microservicios : product-service y user-service

```xml
        <!-- ============================================ -->
        <!-- NUEVO - Módulo 5 Sesión 1: Observabilidad    -->
        <!-- ============================================ -->

        <!-- Micrometer → Prometheus (métricas) -->
        <dependency>
            <groupId>io.micrometer</groupId>
            <artifactId>micrometer-registry-prometheus</artifactId>
        </dependency>

        <!-- Micrometer Tracing → Brave (trazas distribuidas) -->
        <dependency>
            <groupId>io.micrometer</groupId>
            <artifactId>micrometer-tracing-bridge-brave</artifactId>
        </dependency>

        <!-- Reporter: enviar trazas a Zipkin via HTTP -->
        <dependency>
            <groupId>io.zipkin.reporter2</groupId>
            <artifactId>zipkin-reporter-brave</artifactId>
        </dependency>

```

## 3.- Modificar application.yaml y application-kubernetes.yaml en user-service y product-service

- Adaptar los parámetros de management y logging

```yml

# ============================================
# OBSERVABILIDAD - Módulo 5 Sesión 1
# ============================================
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus    # ← Agregar prometheus
  endpoint:
    health:
      probes:
        enabled: true
      show-details: when-authorized
  # Métricas Prometheus
  prometheus:
    metrics:
      export:
        enabled: true
  # Trazas distribuidas
  tracing:
    sampling:
      probability: 1.0        # 1.0 = 100% de trazas (solo para desarrollo)
                                # En producción usar 0.1 (10%)
  # Tags comunes en todas las métricas
  metrics:
    tags:
      application: ${spring.application.name}
    distribution:
      percentiles-histogram:
        http.server.requests: true    # Habilitar histograma de latencia

# Zipkin endpoint
management.zipkin.tracing:
  endpoint: ${ZIPKIN_URL:http://localhost:9411/api/v2/spans}

# Logging con traceId (se inyecta automáticamente)
logging:
  pattern:
    level: "%5p [${spring.application.name},%X{traceId:-},%X{spanId:-}]"
  level:
    com.tecsup.app.micro.user: ${LOG_LEVEL:INFO}
    org.hibernate.SQL: ${SQL_LOG_LEVEL:WARN}

```


## 4.- SecurityConfig 

- En user-service SecurityConfig.java 
Se tiene :
```java
.requestMatchers("/actuator/health/**").permitAll()
```
Cambiar a:
```java
.requestMatchers("/actuator/**").permitAll()    // Permitir todos los actuator
                                                 // En producción: restringir por IP
```

- En product-service SecurityConfig.java 
Se tiene :
```java
.requestMatchers("/actuator/health/**").permitAll()
```
Cambiar a:
```java
.requestMatchers("/actuator/**").permitAll()    // Permitir todos los actuator
                                                 // En producción: restringir por IP
```

## 5.- Probar métricas de Prometheus

- Microservices user-services
```bash
curl http://localhost:8081/actuator/prometheus
```

- Microservices product-services
```bash
curl http://localhost:8082/actuator/prometheus
```

## 6.- Crear el  docker-compose con stack de observabilidad (docker-compose-observability.yml)
```yml

# ============================================
# Stack de observabilidad para desarrollo
# Módulo 5 - Sesión 1
#
# Uso: docker compose -f docker-compose-observability.yml up -d
# ============================================

services:

  # ============================================
  # PROMETHEUS - Recolector de métricas
  # ============================================
  prometheus:
    image: prom/prometheus:v2.51.0
    container_name: prometheus
    ports:
      - "9090:9090"
    volumes:
      - ./observability/prometheus/prometheus.yml:/etc/prometheus/prometheus.yml:ro
    extra_hosts:
      - "host.docker.internal:host-gateway"
    restart: unless-stopped
```

 ## 7.- Configuración de Prometheus

- Archivo : observability/prometheus/prometheus.yml

```yml
# ============================================
# Prometheus Configuration
# Módulo 5 - Sesión 1
# ============================================

global:
  scrape_interval: 15s          # Cada 15 segundos consulta las métricas
  evaluation_interval: 15s      # Cada 15 segundos evalúa reglas

# ============================================
# SCRAPE CONFIGS - Endpoints a monitorear
# ============================================
scrape_configs:

  # Prometheus se monitorea a sí mismo
  - job_name: 'prometheus'
    static_configs:
      - targets: ['localhost:9090']

  # user-service (corriendo en el host, no en Docker)
  - job_name: 'user-service'
    metrics_path: '/actuator/prometheus'
    scrape_interval: 10s
    static_configs:
      - targets: ['host.docker.internal:8081']
        labels:
          application: 'user-service'

  # product-service (corriendo en el host, no en Docker)
  - job_name: 'product-service'
    metrics_path: '/actuator/prometheus'
    scrape_interval: 10s
    static_configs:
      - targets: ['host.docker.internal:8082']
        labels:
          application: 'product-service'

```   
## 8.- Verificar el servidor de Prometheus

**NOTA** : Los microservicios user-service y product-service deben estar ejecutandose


- Levantar stack de observabilidad

```   
docker compose -f docker-compose-observability.yml up -d
```   

- Verificar contenedores
```
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
```

- Documentación Oficial : https://prometheus.io/docs/prometheus/latest/querying/basics/

- Explorar Prometheus

Abrir http://localhost:9090 y ejecutar estas queries en el menu de Graph:

```   
# Query 1: Total de requests por servicio
http_server_requests_seconds_count

# Query 2: Tasa de requests por segundo
rate(http_server_requests_seconds_count[1m])

# Query 3: Latencia p95 de cada endpoint
histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[5m])) by (le, uri, application))

# Query 4: Conexiones de base de datos activas
hikaricp_connections_active

# Query 5: Memoria JVM usada (%)
jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} * 100

# Query 6: Errores 5xx
sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) by (application)

```   
## 9.- Configuracion de persistencia de Prometheus

- Realizar los siguiente cambios en el archivo docker-compose-observability.yml
```yml

# ============================================
# Stack de observabilidad para desarrollo
# Módulo 5 - Sesión 1
#
# Uso: docker compose -f docker-compose-observability.yml up -d
# ============================================

services:

  # ============================================
  # PROMETHEUS - Recolector de métricas
  # ============================================
  prometheus:
    image: prom/prometheus:v2.51.0
    container_name: prometheus
    ports:
      - "9090:9090"
    volumes:
      - ./observability/prometheus/prometheus.yml:/etc/prometheus/prometheus.yml:ro
      - ./observability/prometheus/data:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
      - '--storage.tsdb.retention.time=30d'                  # Retención: 30 días
      - '--web.enable-admin-api'  # Habilita la API de administración para Prometheus      
    extra_hosts:
      - "host.docker.internal:host-gateway"
    restart: unless-stopped
```

- Restaurar el servidor de Prometheus
```
 docker compose -f docker-compose-observability.yml up -d      
```

- Realizar operaciones con los microservicios user-services y product-service, luego forzar la generación de los archivos TSDB atraves de un snapshot

```
curl -X POST http://localhost:9090/api/v1/admin/tsdb/snapshot
```

- Revisar la carperta : observability/prometheus/data/snapshots

## 10.- Configuración de Grafana en Docker Compose

- Agregar al final del archivo docker-compose-observability.yml

```yml

  # ============================================
  # GRAFANA - Dashboards y visualización
  # ============================================
  grafana:
    image: grafana/grafana:10.4.0
    container_name: grafana
    ports:
      - "3000:3000"
    environment:
      - GF_SECURITY_ADMIN_USER=admin
      - GF_SECURITY_ADMIN_PASSWORD=admin
      - GF_USERS_ALLOW_SIGN_UP=false
    volumes:
      - ./observability/grafana/provisioning:/etc/grafana/provisioning:ro
      - grafana-data:/var/lib/grafana
    depends_on:
      - prometheus
    restart: unless-stopped

volumes:
  grafana-data:
    driver: local

```

## 11.-  Auto-provisioning de Grafana

- Archivo : observability/grafana/provisioning/datasources/datasources.yml

```yml
# ============================================
# Grafana: Datasource automático (Prometheus)
# ============================================
apiVersion: 1

datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: true
    editable: true
```

- Archivo : observability/grafana/provisioning/dashboards/dashboards.yml

```yml
# ============================================
# Grafana: Provisioning de dashboards
# ============================================
apiVersion: 1

providers:
  - name: 'default'
    orgId: 1
    folder: 'Microservices'
    type: file
    disableDeletion: false
    editable: true
    options:
      path: /etc/grafana/provisioning/dashboards/json
      foldersFromFilesStructure: false
```

- Restaurar el servidor de Prometheus y Grafana
```
 docker compose -f docker-compose-observability.yml up -d      
```

- Ingresar a Grafana. ( credenciales admin/admin)
```
   Abrir http://localhost:3000
```

## 12.-  Importar Dashboard en Grafana

### Dashboard de Spring Boot

- Importar Dashboard
```
- En menu lateral izquierdo , ingresar a Dashboard -> New -> Import
- Luego aparece una ventana para ingresar el ID del Dashboard. Ingresar 19004 y presionar en el boton "Load"
- Aparece una nueva pantalla , Seleccionar datasource : Prometheus

```

- Generar peticiones a la base de datos

Linux/Mac
```
for i in $(seq 1 60); do curl -s http://localhost:8082/api/products > /dev/null; sleep 0.75; done
```

Windows
```
1..60 | ForEach-Object { Invoke-WebRequest -Uri "http://localhost:8082/api/products" -UseBasicParsing | Out-Null; Start-Sleep -Milliseconds 750 }
```

### Otros Dashboard de Prometheus

- Dashboard JVM --> ID 4701 (INFO : Heap Memory, Threads)
- Dashboard HikariCP --> ID 6083 (INFO : Conexiones activas, Idle, Timeouts )

### Dashboard personalizado

- En el menu de Dashboard seleccionar : New -> New Dashboard 
- Se muestra un pantalla, seleccionar Add Visualization
- Seleccionar Datasource : Prometheus
- Ingresar Query : rate(http_server_requests_seconds_count{application="product-service"}[1m])
- Colocar como Titulo "Product Service - Requests/seg"
- Grabar

## 13.-  Definiendo Alert Rules

- Crear la alerta

<img src="observability/images/alert_rule_step_1.png " />

Hacer un click en "New alert rule". y completar los campos indicandos abajo.

<img src="observability/images/alert_rule_step_3.png " />

<img src="observability/images/alert_rule_step_4.png " />

<img src="observability/images/alert_rule_step_5.png " />

<img src="observability/images/alert_rule_step_6.png " />


- Estados de las Alert Rules : NORMAL -> PENDING -> FIRING

- Generamos la carga simulada

```
for i in $(seq 1 300); do curl -s http://localhost:8082/api/products > /dev/null; sleep 0.05; done
```


## 14.-  Configuracion de Zipkin 

- Agregar en docker-compose-observability.yml lo siguiente, antes de los volumes:

```
  # ============================================
  # ZIPKIN - Trazas distribuidas
  # ============================================
  zipkin:
    image: openzipkin/zipkin:3.4
    container_name: zipkin
    ports:
      - "9411:9411"
    environment:
      - STORAGE_TYPE=mem              # En memoria (desarrollo)
    restart: unless-stopped

volumes:
  grafana-data:
    driver: local
```
- Restaurar el servidor de Prometheus, Grafana y ZipKin
```
 docker compose -f docker-compose-observability.yml up -d      
```

## 15.-  Uso de ZipKin

- Ingresar a http://localhost:9411 y ejecutar "Run Query" para ver las trazas distribuidas

- Generar peticiones y realizar la  búsqueda por trace ID
```
TOKEN=$(curl -s -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"juan.perez@example.com","password":"admin123"}' \
  | jq -r '.token')
```

```
curl -H "Authorization: Bearer $TOKEN"  http://localhost:8082/api/products/1

```

- Identificar la trace ID, en la figura mostrada se tiene el valor de 6a74065045770b8d7f9e2527cad331c5

<img src="observability/images/zipkin_step_1.png " />

- En la consola de ZipKin se busca el trace ID 6a74065045770b8d7f9e2527cad331c5 y se identifica los microservicios donde la peticiones ha viajado.

<img src="observability/images/zipkin_step_2.png " />


