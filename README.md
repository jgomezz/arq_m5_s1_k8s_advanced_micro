
## 1: Diagrama de comunicación

```
┌─────────────────────────────────────────┐
│                Localhost                │
└─────────────────────────────────────────┘
           ↓                    ↓
    localhost:30081      localhost:30082
           ↓                    ↓
┌──────────▼─────────┐  ┌──────▼──────────┐
│  user-service      │←─┤ product-service │
│  Namespace         │  │ Namespace       │
│  (1 pods)          │  │ (1 pods)        │
└────────────────────┘  └─────────────────┘
         ↓                       ↓
    PostgreSQL             PostgreSQL
    (port 5432)            (port 5433)
    userdb                 productdb
```