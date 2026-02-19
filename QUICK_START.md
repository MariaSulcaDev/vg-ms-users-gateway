# 🚀 SIGEI - Guía de Inicio Rápido

Guía para levantar el sistema SIGEI con Gateway, MS Users, Keycloak, PostgreSQL y RabbitMQ.

## 📋 Requisitos Previos

- Java 17
- Maven 3.8+
- Docker Desktop
- curl o Postman (para pruebas)

## 🏗️ Arquitectura Implementada

```
┌─────────────────┐
│   Keycloak      │ :8080 (OAuth2/OIDC)
└────────┬────────┘
         │
┌────────▼────────┐
│  API Gateway    │ :8888 (Spring Cloud Gateway)
└────────┬────────┘
         │
         ├──────► vg-ms-users-management     :9083
         │
┌────────▼────────┐
│ Eureka Server   │ :8761 (Service Discovery)
└─────────────────┘

┌─────────────────┐  ┌─────────────────┐
│   PostgreSQL    │  │    RabbitMQ     │
│     :5432       │  │ :5672 / :15672  │
└─────────────────┘  └─────────────────┘
```

## 🎯 Paso a Paso

### 1️⃣ Levantar Infraestructura con Docker

```bash
# En la raíz del proyecto
docker-compose up -d

# Verificar que todo esté corriendo
docker-compose ps
```

**Servicios levantados:**

- ✅ PostgreSQL (puerto 5432)
- ✅ RabbitMQ (puertos 5672, 15672)
- ✅ Keycloak (puerto 8080)
- ✅ Eureka Server (puerto 8761)

**Verificación:**

```bash
# PostgreSQL
docker exec -it sigei-postgres pg_isready

# RabbitMQ
curl http://localhost:15672

# Keycloak
curl http://localhost:8080/health/ready

# Eureka
curl http://localhost:8761
```

### 2️⃣ Configurar Keycloak

#### 2.1 Acceder a Keycloak Admin Console

- URL: <http://localhost:8080>
- Usuario: `admin`
- Password: `admin`

#### 2.2 Crear Realm "sigei"

1. Click en **Master** (dropdown arriba izquierda)
2. Click **Create Realm**
3. Nombre: `sigei`
4. Click **Create**

#### 2.3 Crear Client "sigei-gateway"

1. Ir a **Clients** → **Create client**
2. **Client ID**: `sigei-gateway`
3. Click **Next**
4. Configurar:
   - **Client authentication**: ON
   - **Standard flow**: ON
   - **Direct access grants**: ON
5. Click **Next**
6. Configurar URLs:
   - **Valid redirect URIs**: `http://localhost:8888/*`
   - **Web origins**: `*`
7. Click **Save**

#### 2.4 Configurar Client Secret

1. En el client **sigei-gateway**, ir a **Credentials**
2. Copiar el **Client Secret** o regenerar uno
3. Si es diferente a `sigei-gateway`, actualizar en:
   - `vg-ms-gateway/src/main/resources/application-prod.yml`
   - Variable `client-secret`

#### 2.5 Crear Roles

En **Realm roles**, crear:

- `DIRECTOR`
- `SUBDIRECTOR`
- `DOCENTE`
- `AUXILIAR`
- `PSICOLOGO`
- `SECRETARIA`
- `APODERADO`
- `ADMIN`

#### 2.6 Crear Usuario de Prueba

1. **Users** → **Add user**
   - Username: `testuser`
   - Email: `test@vallegrande.edu.pe`
   - First name: `Test`
   - Last name: `User`
   - Email verified: ON
2. En **Credentials**:
   - Password: `test123`
   - Temporary: OFF
3. En **Role mapping**:
   - Asignar roles: `DIRECTOR`, `ADMIN`

### 3️⃣ Compilar Microservicios

#### Opción A: Compilar todos a la vez

```bash
# MS Users
cd new_ms_users
mvn clean install -DskipTests

# Gateway
cd ../vg-ms-gateway
mvn clean install -DskipTests
```

#### Opción B: Compilar con Docker (futuro)

```bash
# Crear imágenes Docker para cada MS
# (Dockerfiles ya creados en cada proyecto)
```

### 4️⃣ Levantar Microservicios

Puedes usar **perfil dev** (sin autenticación) o **perfil prod** (con Keycloak).

#### Perfil Development (Recomendado para desarrollo)

**Terminal 1 - MS Users:**

```bash
cd new_ms_users
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

- Sin autenticación OAuth2
- Todos los endpoints abiertos
- Puerto 9083

**Terminal 2 - Gateway:**

```bash
cd vg-ms-gateway
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

- Sin validación de tokens
- Rutas a MS Users en localhost:9083
- Puerto 8888

#### Perfil Production (Con Keycloak)

**Terminal 1 - MS Users:**

```bash
cd new_ms_users
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

**Terminal 2 - Gateway:**

```bash
cd vg-ms-gateway
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

### 5️⃣ Verificar que Todo Funciona

#### Opción 1: Sin Autenticación (perfil dev)

```bash
# Crear usuario
curl -X POST http://localhost:8888/api/v1/users \
  -H "Content-Type: application/json" \
  -d '{
    "username": "jperez",
    "email": "jperez@vallegrande.edu.pe",
    "firstName": "Juan",
    "lastName": "Pérez",
    "role": "DOCENTE",
    "institutionId": 1
  }'

# Listar usuarios
curl http://localhost:8888/api/v1/users
```

#### Opción 2: Con Autenticación (perfil prod)

**Paso 1: Obtener Token**

```bash
curl -X POST http://localhost:8080/realms/sigei/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=sigei-gateway" \
  -d "client_secret=sigei-gateway" \
  -d "grant_type=password" \
  -d "username=testuser" \
  -d "password=test123"
```

Copiar el `access_token` de la respuesta.

**Paso 2: Usar Token en Peticiones**

```bash
TOKEN="tu_access_token_aqui"

# Crear usuario
curl -X POST http://localhost:8888/api/v1/users \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "username": "mlopez",
    "email": "mlopez@vallegrande.edu.pe",
    "firstName": "María",
    "lastName": "López",
    "role": "DIRECTOR",
    "institutionId": 1
  }'

# Listar usuarios
curl http://localhost:8888/api/v1/users \
  -H "Authorization: Bearer $TOKEN"
```

## 📡 Endpoints Disponibles

### Via Gateway (<http://localhost:8888>)

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/api/v1/users` | Listar todos los usuarios |
| GET | `/api/v1/users/{id}` | Obtener usuario por ID |
| GET | `/api/v1/users/status/{status}` | Usuarios por estado (ACTIVE/INACTIVE) |
| GET | `/api/v1/users/role/{role}/status/{status}` | Por rol y estado |
| GET | `/api/v1/users/institution/{id}` | Por institución |
| POST | `/api/v1/users` | Crear usuario |
| PUT | `/api/v1/users/{id}` | Actualizar usuario |
| DELETE | `/api/v1/users/{id}` | Eliminar usuario (soft delete) |
| PATCH | `/api/v1/users/{id}/restore` | Restaurar usuario eliminado |

### Roles Disponibles

- `DIRECTOR`
- `SUBDIRECTOR`
- `DOCENTE`
- `AUXILIAR`
- `PSICOLOGO`
- `SECRETARIA`
- `APODERADO`

## 🔍 Monitoreo y Herramientas

### Dashboards Web

| Servicio | URL | Credenciales |
|----------|-----|--------------|
| **Keycloak Admin** | <http://localhost:8080> | admin / admin |
| **RabbitMQ Management** | <http://localhost:15672> | guest / guest |
| **Eureka Dashboard** | <http://localhost:8761> | - |
| **Gateway Actuator** | <http://localhost:8888/actuator/health> | - |
| **MS Users Actuator** | <http://localhost:9083/actuator/health> | - |

### Ver Logs

```bash
# Todos los servicios Docker
docker-compose logs -f

# Un servicio específico
docker-compose logs -f keycloak
docker-compose logs -f postgres
docker-compose logs -f rabbitmq

# Logs de microservicios (en sus respectivas terminales)
```

### Ver Rutas del Gateway

```bash
curl http://localhost:8888/actuator/gateway/routes | jq
```

### Verificar RabbitMQ

1. Abrir <http://localhost:15672>
2. Login: guest / guest
3. Ir a **Queues** → verificar que exista `user.events`
4. Ir a **Exchanges** → verificar `sigei.events`

## 🛑 Detener Todo

```bash
# Detener microservicios (Ctrl+C en cada terminal)

# Detener Docker Compose
docker-compose down

# Detener y eliminar datos (cuidado!)
docker-compose down -v
```

## ⚙️ Perfiles de Configuración

### Dev Profile

- ✅ Sin autenticación
- ✅ Todos los endpoints abiertos
- ✅ Logs en DEBUG
- ✅ Ideal para desarrollo local
- ✅ Conexiones directas a localhost

### Prod Profile

- 🔐 Autenticación OAuth2 con Keycloak
- 🔐 Validación de tokens JWT
- 🔐 Extracción de roles desde Keycloak
- 📊 Logs en WARN
- 🌐 Service discovery con Eureka

## 🐛 Troubleshooting

### Error: "Connection refused" en PostgreSQL

```bash
# Verificar que PostgreSQL esté corriendo
docker-compose ps postgres

# Ver logs
docker-compose logs postgres

# Reiniciar servicio
docker-compose restart postgres
```

### Error: "Unauthorized" en peticiones

- Verificar que el token no haya expirado (15 min)
- Verificar que el perfil sea `prod` en ambos servicios
- Verificar que Keycloak esté corriendo
- Regenerar token

### MS Users no se registra en Eureka

- Verificar que Eureka esté corriendo: <http://localhost:8761>
- Verificar logs de MS Users
- Verificar configuración `eureka.client.service-url.defaultZone`

### Gateway no encuentra MS Users

- Verificar que MS Users esté registrado en Eureka
- Verificar ruta en `application.yml`: `lb://vg-ms-users-management`
- Verificar logs del Gateway

## 📚 Documentación Adicional

- [KEYCLOAK_SETUP.md](KEYCLOAK_SETUP.md) - Configuración detallada de Keycloak
- [README.md](README.md) - Documentación completa de arquitectura
- [new_ms_users/README.md](new_ms_users/README.md) - Documentación MS Users
- [vg-ms-gateway/README.md](vg-ms-gateway/README.md) - Documentación Gateway

## 🎯 Próximos Pasos

1. ✅ Implementar más microservicios (Students, Institution, etc.)
2. ✅ Agregar rutas al Gateway para nuevos MS
3. ✅ Configurar circuit breaker en Gateway
4. ✅ Implementar rate limiting
5. ✅ Agregar observabilidad (Prometheus + Grafana)
6. ✅ CI/CD con GitHub Actions
7. ✅ Deploy en Kubernetes

## 💡 Tips

- Usa **perfil dev** para desarrollo rápido sin autenticación
- Usa **perfil prod** para probar flujo completo con Keycloak
- Guarda los tokens en variables de entorno para reutilizar
- Revisa los logs de Docker Compose ante cualquier error
- Usa Postman para crear colecciones de peticiones
