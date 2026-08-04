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
