# 10 — DESPLIEGUE EN VPC (Virtual Private Cloud)

> **Objetivo:** Definir la arquitectura de red y despliegue de SIGEI en una VPC, aislando los microservicios en subredes privadas y exponiendo solo los puntos de entrada necesarios.
> **Contexto:** Sistema para colegios privados de nivel inicial en Perú.

---

## 🏗️ ARQUITECTURA DE RED — VISIÓN GENERAL

```
┌─────────────────────────────────────────────────────────────────────┐
│                          VPC: 10.0.0.0/16                          │
│                                                                     │
│  ┌──────────────────── SUBRED PÚBLICA (10.0.1.0/24) ──────────┐   │
│  │                                                               │   │
│  │  ┌──────────────┐    ┌─────────────┐    ┌────────────────┐  │   │
│  │  │ Load Balancer │    │ NAT Gateway │    │  Bastion Host  │  │   │
│  │  │  (ALB/NLB)   │    │ (salida     │    │  (acceso SSH   │  │   │
│  │  │  :443/:80    │    │  internet)  │    │   emergencia)  │  │   │
│  │  └──────┬───────┘    └──────┬──────┘    └────────────────┘  │   │
│  └─────────┼──────────────────┼────────────────────────────────┘   │
│            │                  │                                      │
│  ┌─────────┼──── SUBRED PRIVADA — APPS (10.0.10.0/24) ──────────┐ │
│  │         ▼                  │                                    │ │
│  │  ┌──────────────┐    ┌────┴───────┐    ┌──────────────────┐  │ │
│  │  │ API Gateway  │    │  Keycloak  │    │  Eureka Server   │  │ │
│  │  │  :8080       │    │  :8180     │    │  :8761           │  │ │
│  │  └──────┬───────┘    └────────────┘    └──────────────────┘  │ │
│  │         │                                                      │ │
│  │         ▼                                                      │ │
│  │  ┌─────────────────────────────────────────────────────────┐  │ │
│  │  │              MICROSERVICIOS (Contenedores)              │  │ │
│  │  │                                                         │  │ │
│  │  │  ┌─────────┐ ┌─────────┐ ┌──────────┐ ┌────────────┐ │  │ │
│  │  │  │Institut. │ │Students │ │Enrollm.  │ │  Users Mgt │ │  │ │
│  │  │  │  :9080   │ │  :9081  │ │  :9082   │ │   :9083    │ │  │ │
│  │  │  └─────────┘ └─────────┘ └──────────┘ └────────────┘ │  │ │
│  │  │                                                         │  │ │
│  │  │  ┌─────────┐ ┌─────────┐ ┌──────────┐ ┌────────────┐ │  │ │
│  │  │  │Academic  │ │  Notes  │ │Assistance│ │Disciplinary│ │  │ │
│  │  │  │  :9084   │ │  :9086  │ │  :9087   │ │   :9088    │ │  │ │
│  │  │  └─────────┘ └─────────┘ └──────────┘ └────────────┘ │  │ │
│  │  │                                                         │  │ │
│  │  │  ┌──────────┐ ┌──────────┐ ┌──────────┐               │  │ │
│  │  │  │Civic Date│ │Psych/Welf│ │Teacher   │               │  │ │
│  │  │  │  :9085   │ │  :9090   │ │  :9099   │               │  │ │
│  │  │  └──────────┘ └──────────┘ └──────────┘               │  │ │
│  │  └─────────────────────────────────────────────────────────┘  │ │
│  │                                                                │ │
│  │  ┌──────────────┐                                             │ │
│  │  │  RabbitMQ    │  (mensajería asíncrona)                     │ │
│  │  │  :5672/:15672│                                             │ │
│  │  └──────────────┘                                             │ │
│  └────────────────────────────────────────────────────────────────┘ │
│                                                                     │
│  ┌─────────── SUBRED PRIVADA — DATOS (10.0.20.0/24) ────────────┐ │
│  │                                                                │ │
│  │  ┌──────────────────┐    ┌──────────────────┐                │ │
│  │  │  PostgreSQL      │    │  Keycloak DB     │                │ │
│  │  │  (RDS/Managed)   │    │  (PostgreSQL)    │                │ │
│  │  │  :5432           │    │  :5432           │                │ │
│  │  │                  │    │                  │                │ │
│  │  │  schema_instit.  │    │  keycloak DB     │                │ │
│  │  │  schema_students │    │                  │                │ │
│  │  │  schema_enroll.  │    │                  │                │ │
│  │  │  schema_notes    │    │                  │                │ │
│  │  │  ... (1 schema   │    │                  │                │ │
│  │  │   por servicio)  │    │                  │                │ │
│  │  └──────────────────┘    └──────────────────┘                │ │
│  └────────────────────────────────────────────────────────────────┘ │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘

        ┌─────────────────────────────────────┐
        │         INTERNET / USUARIOS         │
        │                                     │
        │  Navegadores → https://sigei.pe     │
        │  React SPA → CDN/S3                 │
        └─────────────────────────────────────┘
```

---

## 🌐 DISEÑO DE SUBREDES

### Distribución de CIDR

| Subred | CIDR | Zona | Propósito | Acceso Internet |
|--------|------|------|-----------|-----------------|
| **Pública A** | `10.0.1.0/24` | AZ-a | Load Balancer, NAT Gateway | ✅ Directo |
| **Pública B** | `10.0.2.0/24` | AZ-b | Load Balancer (HA), redundancia | ✅ Directo |
| **Privada Apps A** | `10.0.10.0/24` | AZ-a | Gateway, MS, Keycloak, Eureka, RabbitMQ | Via NAT Gateway |
| **Privada Apps B** | `10.0.11.0/24` | AZ-b | Réplicas MS (alta disponibilidad) | Via NAT Gateway |
| **Privada Datos A** | `10.0.20.0/24` | AZ-a | PostgreSQL primaria, Keycloak DB | ❌ Sin acceso |
| **Privada Datos B** | `10.0.21.0/24` | AZ-b | PostgreSQL réplica (read replica) | ❌ Sin acceso |

> **2 Zonas de Disponibilidad** como mínimo para alta disponibilidad.

---

## 🔒 SECURITY GROUPS (Firewall a nivel de instancia)

### SG: Load Balancer

```
Inbound:
  - 443/TCP desde 0.0.0.0/0        ← HTTPS desde internet
  - 80/TCP desde 0.0.0.0/0         ← HTTP → redirige a HTTPS
Outbound:
  - 8080/TCP hacia SG:Apps          ← Envía al API Gateway
```

### SG: Apps (Gateway, Microservicios, Keycloak, Eureka, RabbitMQ)

```
Inbound:
  - 8080/TCP desde SG:LoadBalancer  ← Gateway recibe del LB
  - 8180/TCP desde SG:LoadBalancer  ← Keycloak (login UI) recibe del LB
  - 8761/TCP desde SG:Apps          ← Eureka — solo MS se descubren entre sí
  - 9080-9099/TCP desde SG:Apps     ← MS se comunican entre sí
  - 5672/TCP desde SG:Apps          ← RabbitMQ — solo MS publican/consumen
  - 15672/TCP desde SG:Bastion      ← RabbitMQ Management — solo admin
  - 22/TCP desde SG:Bastion         ← SSH de emergencia
Outbound:
  - 5432/TCP hacia SG:Datos         ← Conectar a PostgreSQL
  - 443/TCP hacia 0.0.0.0/0         ← Salida internet (via NAT — descargar deps)
  - Todo tráfico hacia SG:Apps      ← Comunicación interna entre MS
```

### SG: Datos (PostgreSQL)

```
Inbound:
  - 5432/TCP desde SG:Apps          ← SOLO los MS pueden conectar
Outbound:
  - Ninguno                          ← BD no sale a internet NUNCA
```

### SG: Bastion Host

```
Inbound:
  - 22/TCP desde <IP-admin-fija>    ← SSH solo desde IP conocida
Outbound:
  - 22/TCP hacia SG:Apps            ← SSH hacia contenedores
  - 5432/TCP hacia SG:Datos         ← Conectar a BD para mantenimiento
  - 15672/TCP hacia SG:Apps         ← RabbitMQ Management UI
```

---

## 🐳 ORQUESTACIÓN DE CONTENEDORES

### Opción recomendada: Docker Compose + Docker Swarm (Inicio) → Kubernetes (Escalamiento)

### Fase 1 — Docker Compose (MVP / Producción inicial)

```yaml
# docker-compose.vpc.yml
version: '3.9'

# ═══════════════════════════════════════
# RED INTERNA (simula la VPC)
# ═══════════════════════════════════════
networks:
  sigei-public:
    driver: bridge
    ipam:
      config:
        - subnet: 172.20.1.0/24
  sigei-apps:
    driver: bridge
    internal: true            # ← SIN acceso a internet
    ipam:
      config:
        - subnet: 172.20.10.0/24
  sigei-data:
    driver: bridge
    internal: true            # ← SIN acceso a internet
    ipam:
      config:
        - subnet: 172.20.20.0/24

services:

  # ─── CAPA PÚBLICA ───

  nginx-lb:
    image: nginx:alpine
    ports:
      - "443:443"
      - "80:80"
    volumes:
      - ./nginx/nginx.conf:/etc/nginx/nginx.conf
      - ./certs:/etc/nginx/certs
    networks:
      - sigei-public
      - sigei-apps
    depends_on:
      - api-gateway
      - keycloak
    restart: always

  # ─── CAPA DE APLICACIONES ───

  eureka-server:
    image: sigei/eureka-server:latest
    environment:
      - SPRING_PROFILES_ACTIVE=vpc
    networks:
      - sigei-apps
    restart: always

  keycloak:
    image: quay.io/keycloak/keycloak:24.0
    command: start
    environment:
      KC_DB: postgres
      KC_DB_URL: jdbc:postgresql://keycloak-db:5432/keycloak
      KC_DB_USERNAME: ${KC_DB_USER}
      KC_DB_PASSWORD: ${KC_DB_PASS}
      KC_HOSTNAME: auth.sigei.pe
      KC_PROXY: edge
      KEYCLOAK_ADMIN: ${KC_ADMIN_USER}
      KEYCLOAK_ADMIN_PASSWORD: ${KC_ADMIN_PASS}
    networks:
      - sigei-apps
      - sigei-data
    depends_on:
      - keycloak-db
    restart: always

  api-gateway:
    image: sigei/api-gateway:latest
    environment:
      - SPRING_PROFILES_ACTIVE=vpc
      - EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE=http://eureka-server:8761/eureka
      - SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI=http://keycloak:8080/realms/sigei
    networks:
      - sigei-apps
    depends_on:
      - eureka-server
      - keycloak
    restart: always

  rabbitmq:
    image: rabbitmq:3-management-alpine
    environment:
      RABBITMQ_DEFAULT_USER: ${RABBITMQ_USER}
      RABBITMQ_DEFAULT_PASS: ${RABBITMQ_PASS}
    networks:
      - sigei-apps
    restart: always

  # ─── MICROSERVICIOS (todos en red interna) ───

  ms-institution:
    image: sigei/ms-institution:latest
    environment:
      - SPRING_PROFILES_ACTIVE=vpc
      - SPRING_R2DBC_URL=r2dbc:postgresql://postgres-main:5432/sigei?currentSchema=institution
      - SPRING_R2DBC_USERNAME=${DB_USER}
      - SPRING_R2DBC_PASSWORD=${DB_PASS}
      - EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE=http://eureka-server:8761/eureka
      - SPRING_RABBITMQ_HOST=rabbitmq
    networks:
      - sigei-apps
      - sigei-data
    depends_on:
      - eureka-server
      - postgres-main
      - rabbitmq
    restart: always
    deploy:
      resources:
        limits:
          memory: 512M
          cpus: '0.5'

  ms-students:
    image: sigei/ms-students:latest
    environment:
      - SPRING_PROFILES_ACTIVE=vpc
      - SPRING_R2DBC_URL=r2dbc:postgresql://postgres-main:5432/sigei?currentSchema=students
      - SPRING_R2DBC_USERNAME=${DB_USER}
      - SPRING_R2DBC_PASSWORD=${DB_PASS}
      - EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE=http://eureka-server:8761/eureka
      - SPRING_RABBITMQ_HOST=rabbitmq
    networks:
      - sigei-apps
      - sigei-data
    depends_on:
      - eureka-server
      - postgres-main
      - rabbitmq
    restart: always
    deploy:
      resources:
        limits:
          memory: 512M
          cpus: '0.5'

  ms-enrollments:
    image: sigei/ms-enrollments:latest
    environment:
      - SPRING_PROFILES_ACTIVE=vpc
      - SPRING_R2DBC_URL=r2dbc:postgresql://postgres-main:5432/sigei?currentSchema=enrollments
      - SPRING_R2DBC_USERNAME=${DB_USER}
      - SPRING_R2DBC_PASSWORD=${DB_PASS}
      - EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE=http://eureka-server:8761/eureka
      - SPRING_RABBITMQ_HOST=rabbitmq
    networks:
      - sigei-apps
      - sigei-data
    restart: always
    deploy:
      resources:
        limits:
          memory: 512M
          cpus: '0.5'

  ms-users:
    image: sigei/ms-users:latest
    environment:
      - SPRING_PROFILES_ACTIVE=vpc
      - SPRING_R2DBC_URL=r2dbc:postgresql://postgres-main:5432/sigei?currentSchema=users
      - SPRING_R2DBC_USERNAME=${DB_USER}
      - SPRING_R2DBC_PASSWORD=${DB_PASS}
      - EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE=http://eureka-server:8761/eureka
      - SPRING_RABBITMQ_HOST=rabbitmq
    networks:
      - sigei-apps
      - sigei-data
    restart: always
    deploy:
      resources:
        limits:
          memory: 512M
          cpus: '0.5'

  ms-academic:
    image: sigei/ms-academic:latest
    environment:
      - SPRING_PROFILES_ACTIVE=vpc
      - SPRING_R2DBC_URL=r2dbc:postgresql://postgres-main:5432/sigei?currentSchema=academic
      - SPRING_R2DBC_USERNAME=${DB_USER}
      - SPRING_R2DBC_PASSWORD=${DB_PASS}
      - EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE=http://eureka-server:8761/eureka
      - SPRING_RABBITMQ_HOST=rabbitmq
    networks:
      - sigei-apps
      - sigei-data
    restart: always
    deploy:
      resources:
        limits:
          memory: 512M
          cpus: '0.5'

  ms-notes:
    image: sigei/ms-notes:latest
    environment:
      - SPRING_PROFILES_ACTIVE=vpc
      - SPRING_R2DBC_URL=r2dbc:postgresql://postgres-main:5432/sigei?currentSchema=notes
      - SPRING_R2DBC_USERNAME=${DB_USER}
      - SPRING_R2DBC_PASSWORD=${DB_PASS}
      - EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE=http://eureka-server:8761/eureka
      - SPRING_RABBITMQ_HOST=rabbitmq
    networks:
      - sigei-apps
      - sigei-data
    restart: always
    deploy:
      resources:
        limits:
          memory: 512M
          cpus: '0.5'

  ms-assistance:
    image: sigei/ms-assistance:latest
    environment:
      - SPRING_PROFILES_ACTIVE=vpc
      - SPRING_R2DBC_URL=r2dbc:postgresql://postgres-main:5432/sigei?currentSchema=assistance
      - SPRING_R2DBC_USERNAME=${DB_USER}
      - SPRING_R2DBC_PASSWORD=${DB_PASS}
      - EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE=http://eureka-server:8761/eureka
      - SPRING_RABBITMQ_HOST=rabbitmq
    networks:
      - sigei-apps
      - sigei-data
    restart: always
    deploy:
      resources:
        limits:
          memory: 512M
          cpus: '0.5'

  ms-disciplinary:
    image: sigei/ms-disciplinary:latest
    environment:
      - SPRING_PROFILES_ACTIVE=vpc
      - SPRING_R2DBC_URL=r2dbc:postgresql://postgres-main:5432/sigei?currentSchema=disciplinary
      - SPRING_R2DBC_USERNAME=${DB_USER}
      - SPRING_R2DBC_PASSWORD=${DB_PASS}
      - EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE=http://eureka-server:8761/eureka
      - SPRING_RABBITMQ_HOST=rabbitmq
    networks:
      - sigei-apps
      - sigei-data
    restart: always
    deploy:
      resources:
        limits:
          memory: 512M
          cpus: '0.5'

  ms-civic-dates:
    image: sigei/ms-civic-dates:latest
    environment:
      - SPRING_PROFILES_ACTIVE=vpc
      - SPRING_R2DBC_URL=r2dbc:postgresql://postgres-main:5432/sigei?currentSchema=civic_dates
      - SPRING_R2DBC_USERNAME=${DB_USER}
      - SPRING_R2DBC_PASSWORD=${DB_PASS}
      - EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE=http://eureka-server:8761/eureka
      - SPRING_RABBITMQ_HOST=rabbitmq
    networks:
      - sigei-apps
      - sigei-data
    restart: always
    deploy:
      resources:
        limits:
          memory: 512M
          cpus: '0.5'

  ms-psychology:
    image: sigei/ms-psychology:latest
    environment:
      - SPRING_PROFILES_ACTIVE=vpc
      - SPRING_R2DBC_URL=r2dbc:postgresql://postgres-main:5432/sigei?currentSchema=psychology
      - SPRING_R2DBC_USERNAME=${DB_USER}
      - SPRING_R2DBC_PASSWORD=${DB_PASS}
      - EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE=http://eureka-server:8761/eureka
      - SPRING_RABBITMQ_HOST=rabbitmq
    networks:
      - sigei-apps
      - sigei-data
    restart: always
    deploy:
      resources:
        limits:
          memory: 512M
          cpus: '0.5'

  ms-teacher-assignment:
    image: sigei/ms-teacher-assignment:latest
    environment:
      - SPRING_PROFILES_ACTIVE=vpc
      - SPRING_R2DBC_URL=r2dbc:postgresql://postgres-main:5432/sigei?currentSchema=teacher_assignment
      - SPRING_R2DBC_USERNAME=${DB_USER}
      - SPRING_R2DBC_PASSWORD=${DB_PASS}
      - EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE=http://eureka-server:8761/eureka
      - SPRING_RABBITMQ_HOST=rabbitmq
    networks:
      - sigei-apps
      - sigei-data
    restart: always
    deploy:
      resources:
        limits:
          memory: 512M
          cpus: '0.5'

  # ─── CAPA DE DATOS ───

  postgres-main:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: sigei
      POSTGRES_USER: ${DB_USER}
      POSTGRES_PASSWORD: ${DB_PASS}
    volumes:
      - postgres-data:/var/lib/postgresql/data
      - ./init-schemas.sql:/docker-entrypoint-initdb.d/01-schemas.sql
    networks:
      - sigei-data
    restart: always
    deploy:
      resources:
        limits:
          memory: 1G
          cpus: '1.0'

  keycloak-db:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: keycloak
      POSTGRES_USER: ${KC_DB_USER}
      POSTGRES_PASSWORD: ${KC_DB_PASS}
    volumes:
      - keycloak-db-data:/var/lib/postgresql/data
    networks:
      - sigei-data
    restart: always

volumes:
  postgres-data:
  keycloak-db-data:
```

### Script de inicialización de schemas

```sql
-- init-schemas.sql
-- Se ejecuta al crear el contenedor PostgreSQL por primera vez

-- Crear schemas (uno por microservicio)
CREATE SCHEMA IF NOT EXISTS institution;
CREATE SCHEMA IF NOT EXISTS students;
CREATE SCHEMA IF NOT EXISTS enrollments;
CREATE SCHEMA IF NOT EXISTS users_management;
CREATE SCHEMA IF NOT EXISTS academic;
CREATE SCHEMA IF NOT EXISTS notes;
CREATE SCHEMA IF NOT EXISTS assistance;
CREATE SCHEMA IF NOT EXISTS disciplinary;
CREATE SCHEMA IF NOT EXISTS civic_dates;
CREATE SCHEMA IF NOT EXISTS psychology;
CREATE SCHEMA IF NOT EXISTS teacher_assignment;

-- Crear usuario por microservicio (principio de mínimo privilegio)
CREATE USER ms_institution WITH PASSWORD '${DB_PASS_INSTITUTION}';
CREATE USER ms_students WITH PASSWORD '${DB_PASS_STUDENTS}';
CREATE USER ms_enrollments WITH PASSWORD '${DB_PASS_ENROLLMENTS}';
CREATE USER ms_users WITH PASSWORD '${DB_PASS_USERS}';
CREATE USER ms_academic WITH PASSWORD '${DB_PASS_ACADEMIC}';
CREATE USER ms_notes WITH PASSWORD '${DB_PASS_NOTES}';
CREATE USER ms_assistance WITH PASSWORD '${DB_PASS_ASSISTANCE}';
CREATE USER ms_disciplinary WITH PASSWORD '${DB_PASS_DISCIPLINARY}';
CREATE USER ms_civic_dates WITH PASSWORD '${DB_PASS_CIVIC}';
CREATE USER ms_psychology WITH PASSWORD '${DB_PASS_PSYCHOLOGY}';
CREATE USER ms_teacher WITH PASSWORD '${DB_PASS_TEACHER}';

-- Permisos: cada usuario SOLO accede a su schema
GRANT USAGE ON SCHEMA institution TO ms_institution;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA institution TO ms_institution;
ALTER DEFAULT PRIVILEGES IN SCHEMA institution GRANT ALL ON TABLES TO ms_institution;

GRANT USAGE ON SCHEMA students TO ms_students;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA students TO ms_students;
ALTER DEFAULT PRIVILEGES IN SCHEMA students GRANT ALL ON TABLES TO ms_students;

-- ... (repetir para cada microservicio)
```

---

## ⚙️ application-vpc.yml — Perfil de Spring Boot para VPC

Cada microservicio tiene un perfil `vpc` que sobreescribe la configuración:

```yaml
# application-vpc.yml (ejemplo para ms-institution)
spring:
  profiles: vpc

  r2dbc:
    url: r2dbc:postgresql://${DB_HOST:postgres-main}:5432/sigei?currentSchema=institution
    username: ${DB_USER}
    password: ${DB_PASS}

  rabbitmq:
    host: ${RABBITMQ_HOST:rabbitmq}
    port: 5672
    username: ${RABBITMQ_USER}
    password: ${RABBITMQ_PASS}

  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://${KEYCLOAK_HOST:keycloak}:8080/realms/sigei

eureka:
  client:
    service-url:
      defaultZone: http://${EUREKA_HOST:eureka-server}:8761/eureka
  instance:
    prefer-ip-address: true   # ← En VPC, los MS se comunican por IP privada

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: when-authorized

server:
  port: ${SERVER_PORT:9080}

logging:
  level:
    root: INFO
    pe.edu.vallegrande: INFO    # ← En VPC: INFO (no DEBUG)
```

---

## 🔐 GESTIÓN DE SECRETOS

### Archivo .env (NUNCA en Git)

```bash
# .env — Variables sensibles para Docker Compose
# ⚠️ AGREGAR A .gitignore

# ─── Base de Datos Principal ───
DB_USER=sigei_admin
DB_PASS=<contraseña-generada-aleatoria-32-chars>
DB_HOST=postgres-main

# ─── Keycloak ───
KC_ADMIN_USER=admin
KC_ADMIN_PASS=<contraseña-keycloak-admin>
KC_DB_USER=keycloak
KC_DB_PASS=<contraseña-keycloak-db>

# ─── RabbitMQ ───
RABBITMQ_USER=sigei_mq
RABBITMQ_PASS=<contraseña-rabbitmq>

# ─── Contraseñas por MS (mínimo privilegio) ───
DB_PASS_INSTITUTION=<pass-inst>
DB_PASS_STUDENTS=<pass-stud>
DB_PASS_ENROLLMENTS=<pass-enrol>
DB_PASS_USERS=<pass-users>
DB_PASS_ACADEMIC=<pass-acad>
DB_PASS_NOTES=<pass-notes>
DB_PASS_ASSISTANCE=<pass-asist>
DB_PASS_DISCIPLINARY=<pass-disc>
DB_PASS_CIVIC=<pass-civic>
DB_PASS_PSYCHOLOGY=<pass-psych>
DB_PASS_TEACHER=<pass-teach>
```

> **En producción real:** Usar un servicio de secretos (AWS Secrets Manager, HashiCorp Vault, Azure Key Vault) en lugar de `.env`.

---

## 🌍 NGINX — Reverse Proxy / Load Balancer

```nginx
# nginx/nginx.conf

upstream api_gateway {
    server api-gateway:8080;
}

upstream keycloak {
    server keycloak:8080;
}

server {
    listen 80;
    server_name sigei.pe www.sigei.pe;
    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl;
    server_name sigei.pe;

    ssl_certificate     /etc/nginx/certs/sigei.pe.crt;
    ssl_certificate_key /etc/nginx/certs/sigei.pe.key;
    ssl_protocols       TLSv1.2 TLSv1.3;

    # ─── Frontend (SPA React) ───
    location / {
        root /usr/share/nginx/html;
        try_files $uri $uri/ /index.html;  # ← SPA routing
    }

    # ─── API Gateway (todos los /api/*) ───
    location /api/ {
        proxy_pass http://api_gateway;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # Timeouts
        proxy_connect_timeout 10s;
        proxy_read_timeout 30s;
        proxy_send_timeout 30s;
    }

    # ─── Keycloak (autenticación) ───
    location /auth/ {
        proxy_pass http://keycloak;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # ─── Health check ───
    location /health {
        proxy_pass http://api_gateway/actuator/health;
    }
}
```

---

## 📊 FLUJO DE UNA REQUEST EN LA VPC

```
USUARIO (Internet)
    │
    ▼
┌──────────────┐
│   NGINX LB   │  443/HTTPS ← Termina SSL aquí
│   (Público)   │
└──────┬───────┘
       │ HTTP (interno)
       ▼
┌──────────────┐
│  API Gateway  │  :8080  ← Valida JWT de Keycloak
│  (Privado)    │          ← Aplica rate limiting
└──────┬───────┘          ← Aplica CORS (doc 08)
       │
       ▼
┌──────────────┐
│Eureka Server  │  :8761  ← Gateway pregunta: "¿dónde está ms-students?"
│  (Privado)    │          ← Responde: "10.0.10.15:9081"
└──────────────┘
       │
       ▼
┌──────────────┐
│ Microservicio │  :9081  ← Ejecuta lógica de negocio
│  (Privado)    │          ← Retorna ApiResponse<T> (doc 09)
└──────┬───────┘
       │
       ▼
┌──────────────┐
│  PostgreSQL   │  :5432  ← Solo accesible desde SG:Apps
│  (Privado)    │          ← Schema: students
└──────────────┘
```

---

## 🚀 COMANDOS DE DESPLIEGUE

### Build de imágenes Docker

```bash
# Desde la raíz del proyecto
# Cada MS tiene su propio Dockerfile

# Build todos los microservicios
for ms in institution students enrollments users academic notes \
          assistance disciplinary civic-dates psychology teacher-assignment; do
    echo "Building ms-${ms}..."
    cd "vg-ms-${ms}-develop"
    docker build -t "sigei/ms-${ms}:latest" .
    cd ..
done

# Build del API Gateway y Eureka Server
cd vg-ms-gateway-develop && docker build -t sigei/api-gateway:latest . && cd ..
cd vg-ms-eureka-server && docker build -t sigei/eureka-server:latest . && cd ..
```

### Levantar todo el stack

```bash
# Cargar variables de entorno
export $(cat .env | xargs)

# Levantar infraestructura primero
docker compose -f docker-compose.vpc.yml up -d postgres-main keycloak-db rabbitmq
echo "Esperando a que PostgreSQL esté listo..."
sleep 10

# Levantar servicios de plataforma
docker compose -f docker-compose.vpc.yml up -d eureka-server keycloak
echo "Esperando a que Eureka y Keycloak estén listos..."
sleep 15

# Levantar API Gateway
docker compose -f docker-compose.vpc.yml up -d api-gateway
sleep 5

# Levantar todos los microservicios
docker compose -f docker-compose.vpc.yml up -d \
  ms-institution ms-students ms-enrollments ms-users \
  ms-academic ms-notes ms-assistance ms-disciplinary \
  ms-civic-dates ms-psychology ms-teacher-assignment

# Levantar NGINX (último)
docker compose -f docker-compose.vpc.yml up -d nginx-lb

echo "✅ SIGEI desplegado en VPC"
echo "   → https://sigei.pe (Frontend + API)"
echo "   → https://sigei.pe/auth (Keycloak)"
```

### Verificar estado

```bash
# Ver estado de todos los contenedores
docker compose -f docker-compose.vpc.yml ps

# Ver logs de un microservicio específico
docker compose -f docker-compose.vpc.yml logs -f ms-institution

# Health check del Gateway
curl -s http://localhost:8080/actuator/health | jq .

# Ver instancias registradas en Eureka
curl -s http://localhost:8761/eureka/apps | jq .
```

---

## 📐 FASE 2 — Migración a Kubernetes (cuando se necesite escalar)

```
VPC
├── EKS Cluster (Kubernetes)
│   ├── Namespace: sigei-platform
│   │   ├── Deployment: api-gateway (2 réplicas)
│   │   ├── Deployment: eureka-server (2 réplicas)
│   │   ├── Deployment: keycloak (2 réplicas)
│   │   └── StatefulSet: rabbitmq (3 réplicas, clúster)
│   │
│   ├── Namespace: sigei-services
│   │   ├── Deployment: ms-institution (2 réplicas)
│   │   ├── Deployment: ms-students (2 réplicas)
│   │   ├── Deployment: ms-enrollments (3 réplicas)  ← más carga en matrículas
│   │   ├── Deployment: ms-notes (2 réplicas)
│   │   └── ... (cada MS con HPA - Horizontal Pod Autoscaler)
│   │
│   ├── Namespace: sigei-monitoring
│   │   ├── Prometheus
│   │   ├── Grafana
│   │   └── Loki (logs)
│   │
│   └── Ingress Controller (NGINX Ingress)
│       └── Termina TLS, enruta a api-gateway
│
├── RDS PostgreSQL (Multi-AZ, fuera del clúster)
└── ElastiCache Redis (sesiones Keycloak, caché)
```

> **Cuándo migrar:** Cuando Docker Compose no alcance (>50 usuarios concurrentes, necesidad de auto-scaling, zero-downtime deployments).

---

## 📏 REGLAS DE DESPLIEGUE VPC

| Regla | Descripción |
|-------|-------------|
| **V1** | Los microservicios NUNCA tienen IP pública |
| **V2** | Solo NGINX y NAT Gateway están en subred pública |
| **V3** | La base de datos SOLO acepta conexiones de SG:Apps |
| **V4** | Credenciales SIEMPRE via variables de entorno, NUNCA en código |
| **V5** | SSL termina en NGINX — tráfico interno es HTTP |
| **V6** | Cada MS tiene limits de CPU y memoria definidos |
| **V7** | Logs centralizados (no depender de `docker logs`) |
| **V8** | Backups automáticos de PostgreSQL cada 6 horas |
| **V9** | Health checks en todos los contenedores |
| **V10** | `SPRING_PROFILES_ACTIVE=vpc` en todos los MS |

---

## 🔗 RELACIÓN CON OTROS DOCUMENTOS

| Documento | Relación |
|-----------|----------|
| [03_BASE_DE_DATOS](03_BASE_DE_DATOS_RECOMENDACION.md) | PostgreSQL con schema-per-service desplegado en subred de datos |
| [04_API_GATEWAY](04_API_GATEWAY_Y_SERVICE_DISCOVERY.md) | Gateway es el único punto de entrada desde NGINX |
| [08_SEGURIDAD_KEYCLOAK](08_SEGURIDAD_KEYCLOAK.md) | Keycloak en subred de apps, BD en subred de datos |
| [09_API_RESPONSE](09_API_RESPONSE_Y_ERROR_RESPONSE.md) | Los MS retornan ApiResponse/ErrorResponse que NGINX no modifica |
