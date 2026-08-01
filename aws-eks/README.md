# Arquitectura de Microservicios en AWS EKS

<img src="images/awk_eks_architecture.png" />


# PARTE 1: Preparación

Se crea una cuenta AWS y se instalan tres herramientas en tu máquina local: AWS CLI (para hablar con AWS), eksctl (para crear clusters EKS fácilmente) y kubectl (para controlar Kubernetes). Luego se configura un usuario IAM con permisos de administrador y se conectan las credenciales.


## 1.1: Tener cuenta en AWS

- Ir a https://aws.amazon.com


## 1.2: Instalar Herramientas

- En Windows
```
# 1. Instalar AWS CLI:

# Descargar instalador
https://awscli.amazonaws.com/AWSCLIV2.msi

# Verificar instalación
aws --version

# 2. Instalar eksctl:

# Con Chocolatey
choco install eksctl

# O descargar desde:
https://github.com/weaveworks/eksctl/releases

# 3. Verificar kubectl (ya lo tienes con Docker Desktop)

kubectl version --client

```

- En Linux
```
# AWS CLI
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
unzip awscliv2.zip
sudo ./aws/install

# eksctl
curl --silent --location "https://github.com/weaveworks/eksctl/releases/latest/download/eksctl_$(uname -s)_amd64.tar.gz" | tar xz -C /tmp
sudo mv /tmp/eksctl /usr/local/bin

# Verificar
aws --version
eksctl version
```


- En macOS
```
# AWS CLI
brew install awscli

# eksctl
brew tap weaveworks/tap
brew install weaveworks/tap/eksctl

# Verificar
aws --version
eksctl version
kubectl version --client
```



## 1.3. Configurar AWS CLI

- Crear usuario IAM

```
Ve a AWS Console → IAM → Users → Add User
Nombre: eks-admin
Permissions: ✅ AdministratorAccess
Guarda el Access Key ID y Secret Access Key
```

- Configurar credenciales

```
aws configure

# Te pedirá:
AWS Access Key ID: AKIAIOSFODNN7EXAMPLE
AWS Secret Access Key: wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
Default region name: us-east-1
Default output format: json
```

- Verificar:
```
aws sts get-caller-identity

# Deberías ver:
# {
#     "UserId": "AIDAI...",
#     "Account": "123456789012",
#     "Arn": "arn:aws:iam::123456789012:user/eks-admin"
# }
```

# PARTE 2: Crear Cluster EKS

Con un archivo de configuración YAML y un solo comando (eksctl create cluster), se levanta el cluster Kubernetes en AWS. Esto crea automáticamente la VPC, las subnets en dos zonas de disponibilidad (us-east-1a y 1b), y 2 nodos Worker de tipo t3.medium donde correrán los microservicios.


## 2.1. Crear Cluster

- Ingresar a la carpeta aws_eks
```
cd aws_eks
```

- Crear archivo de configuración: eks-cluster-config.yaml
```
apiVersion: eksctl.io/v1alpha5
kind: ClusterConfig

metadata:
  name: microservices-cluster
  region: us-east-1

nodeGroups:
  - name: standard-workers
    instanceType: t3.medium
    desiredCapacity: 2
    minSize: 1
    maxSize: 3
    volumeSize: 20
    ssh:
      allow: false
    labels:
      role: worker
    tags:
      Environment: learning
      Project: spring-microservices

availabilityZones: ["us-east-1a", "us-east-1b"]
```

Crear el cluster:

```
# Esto tarda 15-20 minutos
eksctl create cluster -f eks-cluster-config.yaml

# Verás mensajes como:
# 2026-02-12 12:04:37 [ℹ]  eksctl version 0.222.0
# 2026-02-12 12:04:37 [ℹ]  using region us-east-1
# 2026-02-12 12:04:38 [ℹ]  subnets for us-east-1a - public:192.168.0.0/19 private:192.168.64.0/19
# 2026-02-12 12:04:38 [ℹ]  subnets for us-east-1b - public:192.168.32.0/19 private:192.168.96.0/19

# ...
# 2026-02-12 12:20:05 [✔]  created 1 nodegroup(s) in cluster "microservices-cluster"
# 2026-02-12 12:20:06 [ℹ]  creating addon: metrics-server
# 2026-02-12 12:20:06 [ℹ]  successfully created addon: metrics-server
# 2026-02-12 12:20:06 [✔]  EKS cluster "microservices-cluster" in "us-east-1" region is readyy

```
-  Verificar Cluster

```
# Ver cluster
eksctl get cluster

# NAME			REGION		EKSCTL CREATED
# microservices-cluster	us-east-1	True
```


## 2.2. Definición de Contexto en Kubernetes

Un contexto es el "perfil de conexión" que le dice a kubectl exactamente a dónde enviar tus comandos y con qué permisos. 

Es la combinación de tres elementos clave: 
- Clúster: La dirección del servidor (¿A dónde voy? EKS o Docker Desktop).
- Usuario: Tus credenciales de acceso (¿Quién soy? El admin de AWS o el usuario local de Docker).
- Namespace: El espacio de trabajo por defecto dentro de ese clúster (¿En qué sala trabajo?)

```
# Ver contextos
kubectl config get-contexts

# Cambiar el contexto "docker-desktop"
kubectl config use-context docker-desktop

# Verificar el cambio 
kubectl config current-context

```

**Como se desea trabajar con  AWS EKS , el cambio de contexto se realiza con el siguiente comando :**

```
# Configurar kubectl para usar EKS
aws eks update-kubeconfig \
  --region us-east-1 \
  --name microservices-cluster
```

### NOTA

**1. aws eks update-kubeconfig (El "Configurador")**

Este comando de la AWS CLI hace dos cosas:
- Descarga/Actualiza los datos: Obtiene los certificados y la dirección (endpoint) del clúster desde AWS.
- Cambia el contexto: Automáticamente te pone en el contexto de EKS después de configurarlo.

**¿Cuándo usarlo?**: La primera vez que te conectas al clúster o si las credenciales/endpoint han cambiado.

**2. kubectl config use-context (El "Interruptor")**

Este comando de Kubernetes solo hace una cosa:
- Cambia el puntero: Le dice a kubectl: "Ya tengo los datos guardados en mi archivo ~/.kube/config, ahora apunta a este".

**¿Cuándo usarlo?**: Siempre que quieras saltar entre Docker Desktop y EKS rápidamente, ya que ambos ya están configurados en tu terminal.


## 2.3. Cambiar de contexto a AWS EKS

```

# Configurar kubectl para usar EKS
aws eks update-kubeconfig \
  --region us-east-1 \
  --name microservices-cluster

# Configurar kubectl (ya lo hace eksctl automáticamente)
kubectl get nodes

# Deberías ver:
# NAME                             STATUS   ROLES    AGE     VERSION
# ip-192-168-10-208.ec2.internal   Ready    <none>   7m14s   v1.34.2-eks-ecaa3a6
# ip-192-168-43-80.ec2.internal    Ready    <none>   7m14s   v1.34.2-eks-ecaa3a6

```

# PARTE 3: Crear Bases de Datos RDS

Se crean dos instancias de PostgreSQL en Amazon RDS: userdb y productdb, cada una para su microservicio. Se configuran dentro de la misma VPC del cluster con Security Groups que solo permiten tráfico desde los nodos EKS (puerto 5432). Luego se usa un pod "proxy" temporal dentro del cluster para conectarse y cargar los datos iniciales.


## 3.1: Crear RDS para user-service

Desde CLI

```
# Obtener VPC del cluster
VPC_ID=$(aws eks describe-cluster \
  --name microservices-cluster \
  --query 'cluster.resourcesVpcConfig.vpcId' \
  --output text)

# Obtener Security Group de los nodos
NODE_SG=$(aws ec2 describe-security-groups \
  --filters "Name=vpc-id,Values=$VPC_ID" "Name=tag:Name,Values=*nodegroup*" \
  --query 'SecurityGroups[0].GroupId' \
  --output text)

# Crear Security Group para RDS
RDS_SG=$(aws ec2 create-security-group \
  --group-name eks-rds-sg \
  --description "Security group for RDS" \
  --vpc-id $VPC_ID \
  --output text)

# Verificar
echo $RDS_SG

# Se muestra
# sg-0fc973be64b3a4213	arn:aws:ec2:us-east-1:317981250767:security-group/sg-0fc973be64b3a4213

# Extraer solo el primer valor (el ID)
RDS_SG=$(echo $RDS_SG | awk '{print $1}')

# Verificar
echo $RDS_SG


# Permitir tráfico desde nodos EKS
aws ec2 authorize-security-group-ingress \
  --group-id $RDS_SG \
  --protocol tcp \
  --port 5432 \
  --source-group $NODE_SG

# Crear subnet group
aws rds create-db-subnet-group \
  --db-subnet-group-name eks-db-subnet-group \
  --db-subnet-group-description "Subnet group for EKS RDS" \
  --subnet-ids $(aws ec2 describe-subnets \
      --filters "Name=vpc-id,Values=$VPC_ID" \
      --query 'Subnets[?MapPublicIpOnLaunch==`false`].SubnetId' \
      --output text)

## --> Si ya existe la subnet group , puedes eliminarlo 
aws rds delete-db-subnet-group --db-subnet-group-name eks-db-subnet-group

# Crear RDS - userdb
aws rds create-db-instance \
  --db-instance-identifier userdb \
  --db-instance-class db.t3.micro \
  --engine postgres \
  --engine-version 15.15 \
  --master-username postgres \
  --master-user-password postgres \
  --allocated-storage 20 \
  --db-name userdb \
  --vpc-security-group-ids $RDS_SG \
  --db-subnet-group-name eks-db-subnet-group \
  --no-publicly-accessible \
  --backup-retention-period 7

```

## 3.2: Crear RDS para product-service


- Crear RDS para productdb

```
# Desde CLI
aws rds create-db-instance \
  --db-instance-identifier productdb \
  --db-instance-class db.t3.micro \
  --engine postgres \
  --engine-version 15.15 \
  --master-username postgres \
  --master-user-password postgres \
  --allocated-storage 20 \
  --db-name productdb \
  --vpc-security-group-ids $RDS_SG \
  --db-subnet-group-name eks-db-subnet-group \
  --no-publicly-accessible \
  --backup-retention-period 7

```

## 3.3: Obtener Endpoints
```
# Esperar a que estén disponibles (5-10 min)
aws rds wait db-instance-available --db-instance-identifier userdb
aws rds wait db-instance-available --db-instance-identifier productdb

# Obtener endpoints
aws rds describe-db-instances \
  --db-instance-identifier userdb \
  --query 'DBInstances[0].Endpoint.Address' \
  --output text

# Output: userdb.cvpunkj36v2t.us-east-1.rds.amazonaws.com

aws rds describe-db-instances \
  --db-instance-identifier productdb \
  --query 'DBInstances[0].Endpoint.Address' \
  --output text

# Output: productdb.cvpunkj36v2t.us-east-1.rds.amazonaws.com

```

## 3.4: Carga de datos

- Crear archivo 08-proxy.yaml
```
apiVersion: apps/v1
kind: Deployment
metadata:
  name: psql-proxy
spec:
  replicas: 1
  selector:
    matchLabels:
      app: psql-proxy
  template:
    metadata:
      labels:
        app: psql-proxy
    spec:
      containers:
      - name: postgres
        image: postgres:15-alpine
        command: ["sleep", "infinity"]

```
- Crear el pod
```
# Crear
kubectl apply -f 08-proxy.yaml

# Verificar
kubectl get pods

# Esperar hasta que este READY
# NAME                          READY   STATUS    RESTARTS   AGE
# psql-proxy-7cdffdbf75-74c5g   1/1     Running   0          11s
```

- Cargar datos en "userdb" 

```
# Obtener endpoint de RDS
RDS_ENDPOINT=$(aws rds describe-db-instances --db-instance-identifier userdb --query 'DBInstances[0].Endpoint.Address' --output text)

echo $RDS_ENDPOINT

# Ejecutar SQL en userdb
kubectl exec -it deployment/psql-proxy -- psql -h $RDS_ENDPOINT -U postgres -d userdb

# Ejecutar todos los script de bbdd de user-service

Password for user postgres: 
psql (15.15)
SSL connection (protocol: TLSv1.2, cipher: ECDHE-RSA-AES256-GCM-SHA384, compression: off)
Type "help" for help.

userdb=> 

# Para salir
userdb=> \q

```

- Cargar datos en "productdb" 

```
# Obtener endpoint de RDS
RDS_ENDPOINT=$(aws rds describe-db-instances --db-instance-identifier productdb --query 'DBInstances[0].Endpoint.Address' --output text)

echo $RDS_ENDPOINT

# Ejecutar SQL en productdb
kubectl exec -it deployment/psql-proxy -- psql -h $RDS_ENDPOINT -U postgres -d productdb

# Ejecutar todos los script de bbdd de user-service

Password for user postgres: 
psql (15.15)
SSL connection (protocol: TLSv1.2, cipher: ECDHE-RSA-AES256-GCM-SHA384, compression: off)
Type "help" for help.

productdb=> 

# Para salir
productdb=> \q
```

# PARTE 4: Subir imágenes Docker a ECR

Se crean repositorios en Amazon ECR (el Docker Hub de AWS). Luego se compila cada microservicio con Maven, se construye su imagen Docker, se etiqueta con la URI de ECR y se sube. Así las imágenes quedan disponibles para que EKS las descargue al momento de crear los pods.

## 4.1: Crear Repositorios ECR
```
# Crear repositorio para user-service
aws ecr create-repository \
  --repository-name user-service \
  --region us-east-1

# Crear repositorio para product-service
aws ecr create-repository \
  --repository-name product-service \
  --region us-east-1

# Ver repositorios creados
aws ecr describe-repositories
```

**Guardar las URIs:**

<img src="images/aws_eks_uris_repositories.png"/>

```
product-service: 317981250767.dkr.ecr.us-east-1.amazonaws.com/product-service
user-service: 317981250767.dkr.ecr.us-east-1.amazonaws.com/user-service
```
## 4.2: Login a ECR
```
# Obtener tu Account ID
AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)

# Login a ECR
aws ecr get-login-password --region us-east-1 | \
  docker login --username AWS --password-stdin \
  $AWS_ACCOUNT_ID.dkr.ecr.us-east-1.amazonaws.com

# Deberías ver: Login Succeeded
```
##  4.3: Construir y Subir user-service

- **Prerequisito** : El Docker Desktop debe estar levantado

```
# Ir a la carpeta de user-service
cd ../user-service

# Compilar
mvn clean package -DskipTests

# Construir imagen
#docker build -t user-service:v1 .
docker build --platform linux/amd64 -t user-service:v1 .


# Etiquetar para ECR
docker tag user-service:v1 \
  $AWS_ACCOUNT_ID.dkr.ecr.us-east-1.amazonaws.com/user-service:v1

# Subir a ECR
docker push $AWS_ACCOUNT_ID.dkr.ecr.us-east-1.amazonaws.com/user-service:v1

```

##  4.4: Construir y Subir product-service

- **Prerequisito** : El Docker Desktop debe estar levantado

```
# Ir a la carpeta de user-service
cd ../product-service

# Compilar
mvn clean package -DskipTests

# Construir imagen
# docker build -t product-service:v1 .
docker build --platform linux/amd64 -t product-service:v1 .

# Etiquetar para ECR
docker tag product-service:v1 \
  $AWS_ACCOUNT_ID.dkr.ecr.us-east-1.amazonaws.com/product-service:v1

# Subir a ECR
docker push $AWS_ACCOUNT_ID.dkr.ecr.us-east-1.amazonaws.com/product-service:v1

```

## 4.5: Verificar Imágenes

```
# Ver imágenes en ECR
aws ecr list-images --repository-name user-service
aws ecr list-images --repository-name product-service

# Deberías ver tus imágenes con tag "v1"
```

# PARTE 5: Desplegar microservicios en EKS

Se aplican los manifiestos de Kubernetes: ConfigMaps con las URLs de las bases de datos, Deployments que definen 2 réplicas de cada microservicio, y Services de tipo ClusterIP para la comunicación interna. Se instala también el AWS Load Balancer Controller mediante Helm, necesario para el siguiente paso.

## 5.1: Editar los Manifiestos

Volver a la carpeta de los archivos manifiestos de AWK EKS
```
cd ../aws_eks
```


- En 01-user-config.yaml y 04-product-config.yaml:

```
# Reemplaza con los endpoints de RDS que obtuviste antes (paso 3.3: Obtener Endpoints)
DB_URL: "jdbc:postgresql://TU-ENDPOINT-AQUI.rds.amazonaws.com:5432/userdb"
DB_PASSWORD: "TuPasswordQueUsaste"
```

- En 02-user-deployment.yaml y 05-product-deployment.yaml:
```
# Reemplaza 123456789012 con tu AWS Account ID
image: 123456789012.dkr.ecr.us-east-1.amazonaws.com/user-service:v1
```

- Para obtener el AWS Account ID:
```
aws sts get-caller-identity --query Account --output text
```

## 5.2: Instalar AWS Load Balancer Controller

Este controller es necesario para crear el ALB (Application Load Balancer).

Instalar Jelm

- Windows:
```
choco install kubernetes-helm
```

- macOS:
```
brew install helm
```

- Linux:
```
curl https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash
```

Instalar el Controller:
```
# 1. Obtener tu Account ID
export AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)

# 2. Descargar política IAM
curl -o iam_policy.json https://raw.githubusercontent.com/kubernetes-sigs/aws-load-balancer-controller/v2.6.0/docs/install/iam_policy.json

# 3. Crear política IAM
aws iam create-policy \
    --policy-name AWSLoadBalancerControllerIAMPolicy \
    --policy-document file://iam_policy.json

# Eliminar , si en caso exista ( NO RECOMENDADO)
aws iam delete-policy \
  --policy-arn arn:aws:iam::$AWS_ACCOUNT_ID:policy/AWSLoadBalancerControllerIAMPolicy



# 4. Asociar el proveedor IAM OIDC
eksctl utils associate-iam-oidc-provider \
  --region=us-east-1 \
  --cluster=microservices-cluster \
  --approve

# 5. Crear service account
eksctl create iamserviceaccount \
  --cluster=microservices-cluster \
  --namespace=kube-system \
  --name=aws-load-balancer-controller \
  --role-name AmazonEKSLoadBalancerControllerRole \
  --attach-policy-arn=arn:aws:iam::$AWS_ACCOUNT_ID:policy/AWSLoadBalancerControllerIAMPolicy \
  --approve

# 6. Instalar con Helm
helm repo add eks https://aws.github.io/eks-charts
helm repo update

helm install aws-load-balancer-controller eks/aws-load-balancer-controller \
  -n kube-system \
  --set clusterName=microservices-cluster \
  --set serviceAccount.create=false \
  --set serviceAccount.name=aws-load-balancer-controller

# 7. Verificar instalación
kubectl get deployment -n kube-system aws-load-balancer-controller

# Esperar hasta que este disponible
# NAME                           READY   UP-TO-DATE   AVAILABLE   AGE
# aws-load-balancer-controller   2/2     2            2           17s
```
## 5.3: Desplegar los Microservicios

```

# 1. Desplegar user-service
kubectl apply -f 01-user-config.yaml
kubectl apply -f 02-user-deployment.yaml
kubectl apply -f 03-user-service.yaml

# 2. Desplegar product-service
kubectl apply -f 04-product-config.yaml
kubectl apply -f 05-product-deployment.yaml
kubectl apply -f 06-product-service.yaml

# 3. Verificar pods
kubectl get pods

# Deberías ver:
# NAME                               READY   STATUS    RESTARTS   AGE
# product-service-5b585cd847-4bh2l   1/1     Running   0          90s
# product-service-5b585cd847-xxzxj   1/1     Running   0          90s
# psql-proxy-7cdffdbf75-4x9m7        1/1     Running   0          10m
# user-service-5cf7c8dd97-5jmps      1/1     Running   0          16m
# user-service-5cf7c8dd97-7z97z      1/1     Running   0          16m


# 4. Ver logs (si hay problemas)
kubectl logs -f deployment/user-service
kubectl logs -f deployment/product-service
```

## 5.4: Eliminar despliegue
```
kubectl delete -f 01-user-config.yaml
kubectl delete -f 02-user-deployment.yaml
kubectl delete -f 03-user-service.yaml

kubectl delete -f 04-product-config.yaml
kubectl delete -f 05-product-deployment.yaml
kubectl delete -f 06-product-service.yaml
```

# PARTE 6: Exponer con Ingress (ALB)

Se crea un recurso Ingress que automáticamente provisiona un Application Load Balancer en AWS. Este ALB enruta las peticiones: /api/users va al user-service y /api/products va al product-service. 


## 6.1: Crear el Ingress (ALB)
```
# Aplicar Ingress
kubectl apply -f 07-ingress.yaml

# Verificar creación del Ingress
kubectl get ingress

# Esperar 2-3 minutos hasta que aparezca la ADDRESS
kubectl get ingress -w

# Output esperado:
# NAME                    CLASS   HOSTS   ADDRESS                                              PORTS   AGE
# microservices-ingress   alb     *       k8s-default-microser-xxxx.us-east-1.elb.amazonaws.com   80      2m
```
- **NOTA** Si no aparece ningún valor en el ADDRESS ejecutar el paso **6.3: Patch el ALB**


## 6.2:  Verificar que NO hay errores en el ALB:

```
kubectl describe ingress microservices-ingress | grep -A 5 "Events:"

# Debe decir:

Events:
  Type    Reason                  Age   From     Message
  ----    ------                  ----  ----     -------
  Normal  SuccessfullyReconciled  1m    ingress  Successfully reconciled

✅ Si dice "SuccessfullyReconciled": ¡Listo!
❌ Si dice "AccessDenied": Algo falló, avísame

# Obtener URL

export ALB_URL=$(kubectl get ingress microservices-ingress -o jsonpath='{.status.loadBalancer.ingress[0].hostname}')

echo "URL del ALB: http://$ALB_URL"

# Probar el microservicio

curl http://$ALB_URL/api/users

curl http://$ALB_URL/api/products

```


## 6.3: Patch el ALB

AccessDenied: User is not authorized to perform: 

elasticloadbalancing:DescribeListenerAttributes ( LA POLICY NO TIENE ESTE PERMISO)

Causa: La política v2.6.0 que usaste NO tiene este permiso.

Solución: Actualizar a v2.7.0

### Descargar policy

```
# Descargar policy v2.7.0 (tiene el permiso que falta)

curl -o iam_policy_v270.json https://raw.githubusercontent.com/kubernetes-sigs/aws-load-balancer-controller/v2.7.0/docs/install/iam_policy.json

```

Editar el archivo iam_policy_v270.json y agregar la Policy que falta
```
....
            "Effect": "Allow",
            "Action": [
                "ec2:DescribeAccountAttributes",
                "ec2:DescribeAddresses",
                "ec2:DescribeAvailabilityZones",
                "ec2:DescribeInternetGateways",
                "ec2:DescribeVpcs",
                "ec2:DescribeVpcPeeringConnections",
                "ec2:DescribeSubnets",
                "ec2:DescribeSecurityGroups",
                "ec2:DescribeInstances",
                "ec2:DescribeNetworkInterfaces",
                "ec2:DescribeTags",
                "ec2:GetCoipPoolUsage",
                "ec2:DescribeCoipPools",
                "elasticloadbalancing:DescribeLoadBalancers",
                "elasticloadbalancing:DescribeLoadBalancerAttributes",
                "elasticloadbalancing:DescribeListeners",
                "elasticloadbalancing:DescribeListenerCertificates",
                "elasticloadbalancing:DescribeSSLPolicies",
                "elasticloadbalancing:DescribeRules",
                "elasticloadbalancing:DescribeTargetGroups",
                "elasticloadbalancing:DescribeTargetGroupAttributes",
                "elasticloadbalancing:DescribeTargetHealth",
                "elasticloadbalancing:DescribeTags",
                "elasticloadbalancing:DescribeListenerAttributes",        // FALTA         
                "elasticloadbalancing:DescribeTrustStores"
            ],
            "Resource": "*"

...
```

###  Crear Nueva Versión de la Policy
```
# Obtener tu Account ID
export AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)

# Crear versión  de la política
aws iam create-policy-version \
  --policy-arn arn:aws:iam::$AWS_ACCOUNT_ID:policy/AWSLoadBalancerControllerIAMPolicy \
  --policy-document file://iam_policy_v270.json \
  --set-as-default

```


###  Verificar la Nueva Policy

```
# Ver versiones
aws iam list-policy-versions \
  --policy-arn arn:aws:iam::$AWS_ACCOUNT_ID:policy/AWSLoadBalancerControllerIAMPolicy

# Por ejemplo si se quiere revisar la version v2, se debe realizar lo siguiente :

# Verificar que v2 tiene el permiso
aws iam get-policy-version \
  --policy-arn arn:aws:iam::$AWS_ACCOUNT_ID:policy/AWSLoadBalancerControllerIAMPolicy \
  --version-id v2 \
  --query 'PolicyVersion.Document' | grep "DescribeListenerAttributes"


# Debe aparecer:
# "elasticloadbalancing:DescribeListenerAttributes"
```

### Reiniciar Load Balancer Controller
```
# Reiniciar para que tome la nueva política
kubectl rollout restart deployment aws-load-balancer-controller -n kube-system

# Esperar a que termine (30-60 segundos):

# Ver progreso
kubectl get pods -n kube-system -l app.kubernetes.io/name=aws-load-balancer-controller

# Cuando ambos pods digan Running con AGE reciente (ej: 45s), continúa.

```

###  Recrear el Ingress

```
# Eliminar
kubectl delete ingress microservices-ingress
# Esperar 10 segundos (cuenta mentalmente o mira el reloj)

# Recrear
kubectl apply -f 07-ingress.yaml

# Verificar

# Ver creación del ALB (esperar 2-3 minutos)
kubectl get ingress microservices-ingress

# Repite el comando cada 30 segundos hasta que aparezca ADDRESS:**

NAME                    CLASS   HOSTS   ADDRESS                                           PORTS   AGE
microservices-ingress   alb     *       k8s-default-microser-xxxxx.elb.amazonaws.com     80      2m
```

### Verificar que NO hay errores:

```
kubectl describe ingress microservices-ingress | grep -A 5 "Events:"

# Debe decir:

Events:
  Type    Reason                  Age   From     Message
  ----    ------                  ----  ----     -------
  Normal  SuccessfullyReconciled  1m    ingress  Successfully reconciled
✅ Si dice "SuccessfullyReconciled": ¡Listo!
❌ Si dice "AccessDenied": Algo falló, avísame

# Obtener URL
kubectl get ingress microservices-ingress -o jsonpath='{.status.loadBalancer.ingress[0].hostname}'

# Reemplaza la URL con la tuya
curl http://k8s-default-microser-xxxxx.us-east-1.elb.amazonaws.com/api/users
```

# PARTE 7: Realizar pruebas con los microservicios


Una vez activo el ALB, se obtiene la URL pública y ya se puede acceder a los microservicios desde Internet.


## 7.1: Obtener la URL del ALB

```
# Obtener URL
export ALB_URL=$(kubectl get ingress microservices-ingress -o jsonpath='{.status.loadBalancer.ingress[0].hostname}')

echo "URL del ALB: http://$ALB_URL"
```

## 7.2: Probar user-service

```
# Health check
# curl http://$ALB_URL/api/users/health

# Crear un usuario
curl -X POST http://$ALB_URL/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Juan Perez",
    "email": "juan@example.com"
  }'

# Listar usuarios
curl http://$ALB_URL/api/users

# Obtener usuario por ID
curl http://$ALB_URL/api/users/1
```

## 7.3: Probar product-service
```
# Health check
# curl http://$ALB_URL/api/products/health

# Crear un producto
curl -X POST http://$ALB_URL/api/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Laptop Dell",
    "price": 1200.00,
    "userId": 1
  }'

# Listar productos
curl http://$ALB_URL/api/products

# Obtener producto por ID
curl http://$ALB_URL/api/products/1
```

## 7.4: Verificar Comunicación entre Servicios
```

# Ver logs de product-service
kubectl logs -l app=product-service --tail=50

# Deberías ver logs mostrando que llama a user-service:
# "Calling User Service to get user with id: 1"
# "User retrieved successfully: UserDTO(...)"
```


# 📊 Comandos Útiles de Monitoreo
```

# Ver todos los recursos
kubectl get all

# Ver estado de pods
kubectl get pods -o wide

# Describir un pod (si hay problemas)
kubectl describe pod <pod-name>

# Ver logs en tiempo real
kubectl logs -f deployment/user-service
kubectl logs -f deployment/product-service

# Ver eventos del cluster
kubectl get events --sort-by=.metadata.creationTimestamp

# Ver uso de recursos
kubectl top nodes
kubectl top pods

# Acceder a un pod (debug)
kubectl exec -it <pod-name> -- /bin/sh
```

# 🧹 Limpieza (Importante para no generar costos)


```
# 1. Eliminar Ingress (elimina el ALB)
kubectl delete -f 07-ingress.yaml

# 2. Eliminar servicios
kubectl delete -f 01-user-config.yaml
kubectl delete -f 02-user-deployment.yaml
kubectl delete -f 03-user-service.yaml
kubectl delete -f 04-product-config.yaml
kubectl delete -f 05-product-deployment.yaml
kubectl delete -f 06-product-service.yaml
kubectl delete -f 08-proxy.yaml

# 3. Eliminar cluster EKS ( Como alternativa eliminar desde AWS Console)
eksctl delete cluster --name microservices-cluster

```
**NOTA** Eliminar desde AWS Console

- Clusters

<img src="images/aws_console_clusters.png"/>

- Auto Scaling

<img src="images/aws_console_auto_scaling.png"/>

- EC2

<img src="images/aws_console_ec2.png"/>

- VPN

<img src="images/aws_console_vpn.png"/>


- CloudFormation

<img src="images/aws_console_cloud_formation.png"/>


```
# 4. Eliminar RDS
aws rds delete-db-instance \
  --db-instance-identifier userdb \
  --skip-final-snapshot

aws rds delete-db-instance \
  --db-instance-identifier productdb \
  --skip-final-snapshot

# 5. Eliminar imágenes de ECR (opcional)
aws ecr delete-repository \
  --repository-name user-service \
  --force

aws ecr delete-repository \
  --repository-name product-service \
  --force
```
