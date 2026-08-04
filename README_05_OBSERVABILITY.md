#  Microservicio Product-Service - Observability

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

- Agregar al final de los archivos 

```xml

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

```bash
curl http://localhost:8082/actuator/prometheus
```
