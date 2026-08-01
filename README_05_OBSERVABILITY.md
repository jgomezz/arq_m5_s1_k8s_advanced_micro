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
```