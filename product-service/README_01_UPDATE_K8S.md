# Microservicio Product-Service - Adaptar despliegue para seguridad

### 1.- Modificar manifestos YAML

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
  name: product-service-secret
  namespace: product-service
  labels:
    app: product-service

type: Opaque

# Valores codificados en base64 (más seguro que stringData)
data:
  DB_USERNAME: cG9zdGdyZXM=                                          # postgres
  DB_PASSWORD: cG9zdGdyZXM=                                          # postgres
  JWT_SECRET: bTFTM2NyM3RLM3lKV1RfVDNjc3VwMjAyNSFAI1NlY3VyZVRva2Vu  # Misma clave que user-service para validar tokens
```


- Agregar lo siguiente en el archivo  k8s/03-deployment.yaml

```
            .....
            
            - name: JWT_SECRET
              valueFrom:
                secretKeyRef:
                  name: product-service-secret
                  key: JWT_SECRET
```

### 2.- Volver a redesplegar product-service en Kubernetes . Ver README.md para comandos detallados

```
# Compilar el proyecto (si es necesario)
mvn clean package -DskipTests

# Construir imagen Docker
docker build -t product-service:1.0 .

# Configurar Contexto para Docker Desktop
kubectl config use-context docker-desktop

# Actualizar el Secret con las nuevas variables de entorno codificadas en base64
kubectl apply -f product-service/k8s/02-secret.yaml 

# Actualizar el deployment para usar el nuevo Secret
kubectl apply -f product-service/k8s/03-deployment.yaml  

# Reiniciar el despliegue para aplicar los cambios (opcional, ya que kubectl apply debería manejarlo)
kubectl rollout restart deployment product-service -n product-service 

# Verificar que el servicio sigue funcionando y tiene la misma configuración
kubectl get service -n product-service 

# Output:
# NAME              TYPE       CLUSTER-IP      PORT(S)        AGE
# product-service   NodePort   10.96.xxx.xxx   80:30082/TCP   5s

# Verificar pods
kubectl get pods -n product-service

# Ver logs de user-service
kubectl logs -f <POD_NAME> -n product-service

```
### 3.- Probar product-service

```
# Health check
curl http://localhost:30082/actuator/health

# Output esperado:
# {"status":"UP","groups":["liveness","readiness"]}

# Listar productos
curl http://localhost:30082/api/products
```
