# 04 — API GATEWAY Y SERVICE DISCOVERY

> **Pregunta:** ¿API Gateway, Eureka, o ambos?
> **Respuesta:** **AMBOS** — son complementarios, no excluyentes

---

## 📊 ¿QUÉ HACE CADA UNO?

| Componente | Propósito | Analogía |
|-----------|-----------|----------|
| **Eureka** (Service Discovery) | Cada MS se registra y puede descubrir a otros | "Directorio telefónico" |
| **API Gateway** (Spring Cloud Gateway) | Punto de entrada único para el frontend | "Recepcionista del edificio" |

```
SIN Gateway ni Eureka (ACTUAL):

Frontend → :9080 (institution)
Frontend → :9081 (students)
Frontend → :9082 (enrollments)
Frontend → :9083 (users)
Frontend → :9084 (academic)
Frontend → :9085 (civic-dates)
Frontend → :9086 (notes)
Frontend → :9087 (assistance)
Frontend → :9088 (disciplinary)
Frontend → :9090 (psychology)
Frontend → :9099 (teacher-assignment)

⚠️ El frontend conoce 11 puertos diferentes
⚠️ No hay autenticación centralizada
⚠️ No hay rate limiting
⚠️ CORS configurado 11 veces

────────────────────────────────────────

CON Gateway + Eureka (PROPUESTO):

Frontend → :8080 (API Gateway) → Eureka → MS correcto

✅ El frontend solo conoce UN endpoint
✅ Autenticación centralizada
✅ Rate limiting centralizado
✅ CORS en un solo lugar
✅ Load balancing automático
```

---

## 🏗️ ARQUITECTURA PROPUESTA

```
                    ┌───────────────────┐
                    │   Frontend React  │
                    │    Puerto 5173    │
                    └────────┬──────────┘
                             │
                             │ HTTPS (un solo endpoint)
                             ▼
                    ┌───────────────────┐
                    │   API GATEWAY     │
                    │   Puerto 8080     │
                    │                   │
                    │ • Autenticación   │
                    │ • Rate Limiting   │
                    │ • CORS            │
                    │ • Load Balancing  │
                    │ • Circuit Breaker │
                    │ • Request Logging │
                    └────────┬──────────┘
                             │
                    ┌────────┴──────────┐
                    │   EUREKA SERVER   │
                    │   Puerto 8761     │
                    │                   │
                    │ Service Registry  │
                    │ Health Checks     │
                    │ Service Discovery │
                    └───┬───┬───┬───┬───┘
                        │   │   │   │
          ┌─────────────┤   │   │   ├─────────────────┐
          ▼             ▼   ▼   ▼   ▼                 ▼
    ┌──────────┐  ┌────────┐ ┌────────┐         ┌──────────┐
    │Institut. │  │Students│ │Enrollm.│   ...   │Psychology│
    │  :9080   │  │ :9081  │ │ :9082  │         │  :9090   │
    └──────────┘  └────────┘ └────────┘         └──────────┘
```

---

## 🔧 IMPLEMENTACIÓN DEL EUREKA SERVER

### Nuevo microservicio: `vg-ms-eureka-server`

```xml
<!-- pom.xml -->
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.3.0</version>
</parent>

<dependencies>
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-netflix-eureka-server</artifactId>
    </dependency>
</dependencies>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>2023.0.3</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

```java
package pe.edu.vallegrande.sigei.eureka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@SpringBootApplication
@EnableEurekaServer
public class EurekaServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }
}
```

```yaml
# application.yml — Eureka Server
server:
  port: 8761

spring:
  application:
    name: eureka-server

eureka:
  client:
    register-with-eureka: false    # El server no se registra a sí mismo
    fetch-registry: false          # No necesita buscar otros servers
  server:
    wait-time-in-ms-when-sync-empty: 0
    enable-self-preservation: false   # Desactivar en dev, activar en prod
```

### Registrar CADA microservicio como cliente Eureka

```xml
<!-- pom.xml de cada microservicio — Agregar dependencia -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
```

```yaml
# application.yml de cada microservicio — Agregar configuración
eureka:
  client:
    service-url:
      defaultZone: ${EUREKA_URL:http://localhost:8761/eureka/}
    register-with-eureka: true
    fetch-registry: true
  instance:
    prefer-ip-address: true
    instance-id: ${spring.application.name}:${server.port}
```

### Comunicación entre microservicios CON Eureka

```java
// ANTES (URLs hardcodeadas):
WebClient.builder()
    .baseUrl("http://localhost:9080")  // ← Hardcodeado
    .build();

// DESPUÉS (Service Discovery):
@Bean
@LoadBalanced
public WebClient.Builder webClientBuilder() {
    return WebClient.builder();
}

// Usar el NOMBRE del servicio registrado en Eureka
WebClient institutionClient = webClientBuilder
    .baseUrl("http://vg-ms-institution-management")  // ← Nombre del servicio
    .build();

// Eureka resuelve automáticamente a la IP:puerto correcta
// Si hay múltiples instancias → Load Balancing automático
```

---

## 🔧 IMPLEMENTACIÓN DEL API GATEWAY

### Nuevo microservicio: `vg-ms-gateway`

```xml
<!-- pom.xml -->
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.3.0</version>
</parent>

<dependencies>
    <!-- API Gateway reactivo -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-gateway</artifactId>
    </dependency>

    <!-- Eureka Client (para descubrir servicios) -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
    </dependency>

    <!-- Circuit Breaker -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-circuitbreaker-reactor-resilience4j</artifactId>
    </dependency>

    <!-- Rate Limiter (Redis) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis-reactive</artifactId>
    </dependency>
</dependencies>
```

```java
package pe.edu.vallegrande.sigei.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class GatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
```

```yaml
# application.yml — API Gateway
server:
  port: 8080

spring:
  application:
    name: api-gateway

  cloud:
    gateway:
      # Descubrimiento automático de servicios via Eureka
      discovery:
        locator:
          enabled: false   # No auto-exponer todos los servicios
          lower-case-service-id: true

      # RUTAS EXPLÍCITAS
      routes:
        # ═══════════════════════════════════════════
        # INSTITUCIONES
        # ═══════════════════════════════════════════
        - id: institution-service
          uri: lb://vg-ms-institution-management    # lb:// = Load Balanced via Eureka
          predicates:
            - Path=/api/v1/institutions/**,/api/v1/classrooms/**
          filters:
            - name: CircuitBreaker
              args:
                name: institutionCircuitBreaker
                fallbackUri: forward:/fallback/institution
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 100
                redis-rate-limiter.burstCapacity: 200
            - StripPrefix=0

        # ═══════════════════════════════════════════
        # ESTUDIANTES
        # ═══════════════════════════════════════════
        - id: student-service
          uri: lb://vg-ms-students
          predicates:
            - Path=/api/v1/students/**
          filters:
            - name: CircuitBreaker
              args:
                name: studentCircuitBreaker
                fallbackUri: forward:/fallback/student
            - StripPrefix=0

        # ═══════════════════════════════════════════
        # MATRÍCULAS
        # ═══════════════════════════════════════════
        - id: enrollment-service
          uri: lb://enrollments-service
          predicates:
            - Path=/api/v1/enrollments/**,/api/v1/academic-periods/**
          filters:
            - name: CircuitBreaker
              args:
                name: enrollmentCircuitBreaker
            - StripPrefix=0

        # ═══════════════════════════════════════════
        # USUARIOS
        # ═══════════════════════════════════════════
        - id: user-service
          uri: lb://vg-ms-users-management
          predicates:
            - Path=/api/v1/users/**
          filters:
            - StripPrefix=0

        # ═══════════════════════════════════════════
        # GESTIÓN ACADÉMICA
        # ═══════════════════════════════════════════
        - id: academic-service
          uri: lb://vg-ms-academic-management
          predicates:
            - Path=/api/v1/courses/**,/api/v1/competencies/**,/api/v1/catalogs/**
          filters:
            - StripPrefix=0

        # ═══════════════════════════════════════════
        # NOTAS / CALIFICACIONES
        # ═══════════════════════════════════════════
        - id: notes-service
          uri: lb://vg-ms-notes
          predicates:
            - Path=/api/v1/notes/**,/api/v1/evaluations/**,/api/v1/report-cards/**
          filters:
            - StripPrefix=0

        # ═══════════════════════════════════════════
        # ASISTENCIA
        # ═══════════════════════════════════════════
        - id: assistance-service
          uri: lb://vg-ms-assistance
          predicates:
            - Path=/api/v1/attendance/**,/api/v1/attendance-summary/**
          filters:
            - StripPrefix=0

        # ═══════════════════════════════════════════
        # GESTIÓN DISCIPLINARIA
        # ═══════════════════════════════════════════
        - id: disciplinary-service
          uri: lb://vg-ms-disciplinary-management
          predicates:
            - Path=/api/v1/incidents/**,/api/v1/behavior-records/**
          filters:
            - StripPrefix=0

        # ═══════════════════════════════════════════
        # PSICOLOGÍA Y BIENESTAR
        # ═══════════════════════════════════════════
        - id: psychology-service
          uri: lb://vg-ms-psychology-welfare
          predicates:
            - Path=/api/v1/psychological-evaluations/**,/api/v1/special-needs/**
          filters:
            - StripPrefix=0

        # ═══════════════════════════════════════════
        # ASIGNACIÓN DE DOCENTES
        # ═══════════════════════════════════════════
        - id: teacher-assignment-service
          uri: lb://vg-ms-teacher-assignment
          predicates:
            - Path=/api/v1/teacher-assignments/**
          filters:
            - StripPrefix=0

        # ═══════════════════════════════════════════
        # FECHAS CÍVICAS / CALENDARIO
        # ═══════════════════════════════════════════
        - id: civic-dates-service
          uri: lb://evento-microservice
          predicates:
            - Path=/api/v1/events/**,/api/v1/calendars/**
          filters:
            - StripPrefix=0

        # ═══════════════════════════════════════════
        # NOTIFICACIONES
        # ═══════════════════════════════════════════
        - id: notification-service
          uri: lb://vg-ms-notifications
          predicates:
            - Path=/api/v1/notifications/**
          filters:
            - StripPrefix=0

  # ═══════════════════════════════════════════
  # CORS — Configurado SOLO aquí, no en cada MS
  # ═══════════════════════════════════════════
  cloud.gateway.globalcors:
    cors-configurations:
      '[/**]':
        allowed-origins:
          - "http://localhost:5173"
          - "https://sigei.edu.pe"
        allowed-methods: "*"
        allowed-headers: "*"
        allow-credentials: true
        max-age: 3600

# Eureka
eureka:
  client:
    service-url:
      defaultZone: ${EUREKA_URL:http://localhost:8761/eureka/}

# Circuit Breaker
resilience4j:
  circuitbreaker:
    instances:
      institutionCircuitBreaker:
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 30s
      studentCircuitBreaker:
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 30s
      enrollmentCircuitBreaker:
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 30s
```

### Controlador de Fallback

```java
package pe.edu.vallegrande.sigei.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import java.util.Map;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @RequestMapping("/institution")
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public Mono<Map<String, String>> institutionFallback() {
        return Mono.just(Map.of(
            "status", "error",
            "message", "El servicio de instituciones no está disponible. Intente más tarde.",
            "service", "institution-management"
        ));
    }

    @RequestMapping("/student")
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public Mono<Map<String, String>> studentFallback() {
        return Mono.just(Map.of(
            "status", "error",
            "message", "El servicio de estudiantes no está disponible. Intente más tarde.",
            "service", "students"
        ));
    }
}
```

---

## 🔒 SEGURIDAD EN EL GATEWAY — KEYCLOAK

> **La seguridad se implementa con Keycloak (OAuth 2.0 / OpenID Connect).**
> Ver `08_SEGURIDAD_KEYCLOAK.md` para la implementación completa.
>
> **CORS se define SOLO en el Gateway.** Los microservicios NO configuran CORS.

**Resumen:**

- Keycloak emite tokens JWT → Frontend los envía en cada request
- Gateway valida tokens con JWKS de Keycloak (no JWT manual)
- Gateway propaga headers `X-User-Id`, `X-User-Role`, `X-Institution-Id` a los MS
- Los MS solo leen headers, no validan tokens
- CORS configurado en el Gateway (`spring.cloud.gateway.globalcors`), **NUNCA en los MS**

---

## 🔄 CAMBIOS EN EL FRONTEND

```typescript
// ANTES — Múltiples base URLs
const INSTITUTION_API = "http://localhost:9080/api/v1";
const STUDENT_API = "http://localhost:9081/api/v1";
const ENROLLMENT_API = "http://localhost:9082/api/v1";
// ... 8 más

// DESPUÉS — Una sola base URL (el Gateway)
const API_BASE_URL = import.meta.env.VITE_API_URL || "http://localhost:8080";

// Todas las llamadas van al Gateway
axios.get(`${API_BASE_URL}/api/v1/institutions`);    // Gateway → Institution MS
axios.get(`${API_BASE_URL}/api/v1/students`);         // Gateway → Students MS
axios.post(`${API_BASE_URL}/api/v1/enrollments`);     // Gateway → Enrollments MS
```

```typescript
// vite.config.ts — Proxy SOLO al Gateway
export default defineConfig({
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',  // Solo el Gateway
        changeOrigin: true,
        secure: false,
      },
    },
  },
});
```

---

## 📋 PUERTOS FINALES

| Servicio | Puerto | Tipo |
|----------|--------|------|
| **Eureka Server** | 8761 | Infraestructura |
| **API Gateway** | 8080 | Punto de entrada |
| **Config Server** (futuro) | 8888 | Infraestructura |
| **RabbitMQ** | 5672 / 15672 | Infraestructura |
| vg-ms-institution-management | 9080 | Microservicio |
| vg-ms-students | 9081 | Microservicio |
| vg-ms-enrollments | 9082 | Microservicio |
| vg-ms-users-management | 9083 | Microservicio |
| vg-ms-academic-management | 9084 | Microservicio |
| vg-ms-civic-dates | 9085 | Microservicio |
| vg-ms-notes | 9086 | Microservicio |
| vg-ms-assistance | 9087 | Microservicio |
| vg-ms-disciplinary-management | 9088 | Microservicio |
| vg-ms-psychology-welfare | 9090 | Microservicio |
| vg-ms-teacher-assignment | 9099 | Microservicio |
| vg-ms-notifications | 9091 | Microservicio |
| **Frontend React** | 5173 | Cliente |

---

## 🚀 ORDEN DE ARRANQUE

```
1. Eureka Server           → Debe estar disponible primero
2. Config Server (futuro)  → Provee configuración centralizada
3. RabbitMQ                → Infraestructura de mensajería
4. API Gateway             → Se registra en Eureka
5. Microservicios          → Se registran en Eureka (orden indistinto)
6. Frontend                → Conecta al Gateway
```

---

> **Siguiente:** Ver `05_ARQUITECTURA_BACKEND_COMPLETA.md` para la estructura completa del backend.
