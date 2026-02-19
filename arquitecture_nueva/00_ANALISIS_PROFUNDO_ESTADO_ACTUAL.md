# 00 — ANÁLISIS PROFUNDO DEL ESTADO ACTUAL DE SIGEI

> **Fecha del análisis:** Febrero 2026
> **Sistema:** SIGEI — Sistema Integrado de Gestión Educativa Institucional
> **Contexto:** Sistema multi-institucional para colegios PRIVADOS de nivel inicial en Perú

---

## 📋 INVENTARIO DE MICROSERVICIOS

| # | Microservicio | Puerto | Base de Datos | Stack Reactivo | Estado |
|---|--------------|--------|---------------|----------------|--------|
| 1 | `vg-ms-institution-management` | 9080 | **MongoDB** (Atlas) | WebFlux (Reactivo) | ⚠️ Problemas |
| 2 | `vg-ms-students` | 9081 | **MongoDB** (Atlas) | WebFlux (Reactivo) | ⚠️ Problemas |
| 3 | `vg-ms-enrollments` | 9082 | **PostgreSQL** (Neon R2DBC) | WebFlux (R2DBC) | ⚠️ Problemas |
| 4 | `vg-ms-users-management` | 9083 | **MongoDB** (Atlas) | WebFlux (Reactivo) | ⚠️ Problemas |
| 5 | `vg-ms-academic-management` | 9084 | **PostgreSQL** (Neon R2DBC) | WebFlux (R2DBC) | ⚠️ Problemas |
| 6 | `vg-ms-civic-dates` | 9085 | **PostgreSQL** (Neon R2DBC) | WebFlux (R2DBC) | ⚠️ Problemas |
| 7 | `vg-ms-notes` | 9086 | **PostgreSQL** (Neon R2DBC) | WebFlux (R2DBC) | ⚠️ Problemas |
| 8 | `vg-ms-assistance` | 9087 | **PostgreSQL** (Neon R2DBC) | WebFlux (R2DBC) | ⚠️ Problemas |
| 9 | `vg-ms-disciplinary-management` | 9088 | **PostgreSQL** (Neon R2DBC) | WebFlux (R2DBC) | ⚠️ Problemas |
| 10 | `vg-ms-psychology-welfare` | 9090 | **PostgreSQL** (Neon R2DBC) | WebFlux (R2DBC) | ⚠️ Problemas |
| 11 | `vg-ms-teacher-assignment` | 9099 | **PostgreSQL** (Neon R2DBC) | WebFlux (R2DBC) | ⚠️ Problemas |
| 12 | `vg-ms-gateway` | — | — | — | 🔴 Solo README |
| 13 | `vg-ms-notifications` | — | — | — | 🔴 Solo README |
| 14 | `vg-web-sigei` (Frontend) | 5173 | — | React 19 + Tailwind 4 | ⚠️ Problemas |

---

## 🔴 PROBLEMAS CRÍTICOS DETECTADOS

### 1. CREDENCIALES EXPUESTAS EN CÓDIGO FUENTE (SEVERIDAD: CRÍTICA)

**Todos los microservicios tienen credenciales en texto plano** en `application.yml`:

```yaml
# vg-ms-academic-management — CREDENCIALES EXPUESTAS
r2dbc:
  url: r2dbc:postgresql://neondb_owner:npg_D8rhSEazgFI7@ep-bitter-truth...

# vg-ms-assistance — CREDENCIALES EXPUESTAS
username: neondb_owner
password: npg_RitqK8seGb5f
# + Supabase API key expuesta
api-key: sb_secret_Al7xwMp_uPbCsbRK5P5uPA_lw_LjZdD

# vg-ms-institution-management — CREDENCIALES EXPUESTAS
mongodb:
  uri: mongodb+srv://sistemaSigei:t2eK0JR0YSwoT02e@sigei.oub8atq.mongodb.net

# vg-ms-students — CREDENCIALES EXPUESTAS (mismas de MongoDB)
# vg-ms-users-management — CREDENCIALES EXPUESTAS (mismas de MongoDB)
```

**Impacto:** Cualquier persona con acceso al repositorio puede acceder a TODAS las bases de datos del sistema. Esto es una **violación de seguridad crítica**.

---

### 2. MEZCLA DE BASES DE DATOS SIN ESTRATEGIA (SEVERIDAD: ALTA)

| Motor | Microservicios | Versión/Tipo |
|-------|---------------|--------------|
| **MongoDB Atlas** | institution, students, users | Compartida (misma URI) |
| **Neon PostgreSQL** (instancia 1) | academic-management, notes | Misma instancia `ep-bitter-truth` |
| **Neon PostgreSQL** (instancia 2) | civic-dates, teacher-assignment | Misma instancia `ep-little-moon` |
| **Neon PostgreSQL** (instancia 3) | enrollments | Instancia propia |
| **Neon PostgreSQL** (instancia 4) | assistance | Instancia propia |
| **Neon PostgreSQL** (instancia 5) | disciplinary-management | Instancia propia |
| **Neon PostgreSQL** (instancia 6) | psychology-welfare | Instancia propia |

**Problemas:**

- **3 microservicios comparten la MISMA base de datos MongoDB** (institution, students, users) → **Viola el principio de Database per Service**
- **2 pares de microservicios comparten instancias PostgreSQL** → Acoplamiento de datos
- Mezcla indiscriminada de MongoDB y PostgreSQL sin justificación técnica clara
- Sin estrategia de migración ni consistencia de datos entre motores

---

### 3. AUSENCIA TOTAL DE API GATEWAY (SEVERIDAD: ALTA)

- `vg-ms-gateway` es solo un README vacío
- **El frontend se conecta directamente a cada microservicio** por su puerto
- No hay punto de entrada único
- No hay rate limiting, circuit breaker, ni load balancing
- No hay autenticación centralizada
- CORS configurado individualmente EN CADA microservicio (y de forma inconsistente)

---

### 4. ARQUITECTURA HEXAGONAL MAL IMPLEMENTADA (SEVERIDAD: ALTA)

La estructura de carpetas intenta seguir arquitectura hexagonal pero **viola sus principios fundamentales**:

#### 4.1 El dominio depende de la infraestructura

```
domain/model/Institution.java
→ Usa @Document(collection = "institutions") — Anotación de MongoDB (infraestructura)

domain/model/Enrollment.java
→ Usa @Table("enrollments"), @Column — Anotaciones de R2DBC (infraestructura)

domain/model/Student.java
→ Usa @Document(collection = "students") — Anotación de MongoDB
```

**El dominio NUNCA debería conocer la tecnología de persistencia.**

#### 4.2 Los servicios de aplicación mezclan lógica de dominio y de infraestructura

```java
// CourseServiceImpl.java — Lógica de negocio HARDCODEADA
course.setInstitutionId("11111111-1111-1111-1111-111111111111"); // ← ¿QUÉ ES ESTO?
course.setId(UUID.randomUUID()); // ← Generación de ID en capa de aplicación
course.setStatus("ACTIVE"); // ← Strings mágicos en lugar de enums del dominio
```

#### 4.3 No existen puertos ni adaptadores definidos

- No hay interfaces `Port` (puertos de entrada/salida)
- Los repositorios están directamente en `infrastructure/repository/` sin puertos de salida en el dominio
- Los servicios de aplicación dependen directamente de implementaciones de infraestructura

#### 4.4 Estructura de carpetas inconsistente entre microservicios

```
# vg-ms-notes (diferente a todos)
├── rest/          ← directamente en raíz, no en infrastructure/
├── repository/    ← directamente en raíz, no en infrastructure/

# vg-ms-civic-dates (package diferente)
pe.vallegrande.vgmsevents  ← Falta pe.edu

# vg-ms-psychology-welfare (guiones bajos)
pe.edu.vallegrande.vg_ms_psychology_welfare  ← Usa _ en lugar de camelCase

# vg-ms-assistance (guiones bajos)
pe.edu.vallegrande.vg_ms_assistance  ← Usa _ en lugar de camelCase
```

---

### 5. COMUNICACIÓN ENTRE MICROSERVICIOS DEFICIENTE (SEVERIDAD: ALTA)

#### 5.1 Solo WebClient síncrono sin resiliencia

```java
// Todos los microservicios usan WebClient sin:
// - Circuit Breaker
// - Retry
// - Timeout adecuados
// - Fallback

// StudentServiceImpl.java — Llamada sin resiliencia
institutionService.getInstitutionById(student.getInstitutionId())
// Si institution-management está caído → CASCADING FAILURE
```

#### 5.2 URLs hardcodeadas y mezcladas

```yaml
# vg-ms-students — URLs de PRODUCCIÓN en application.yml
institution:
  url: https://musical-couscous-69v9q7g576gpfr6pw-9080.app.github.dev/api/v1/institutions
user:
  url: https://vg-ms-users-management-ly73.onrender.com/api/v1/users

# vg-ms-teacher-assignment — MÁS URLs de producción hardcodeadas
vg-ms-institution:
  url: https://musical-couscous-69v9q7g576gpfr6pw-9080.app.github.dev

# vg-ms-enrollments — IP PRIVADA hardcodeada
student-service:
  base-url: http://192.168.18.32:9081  # ← IP local de un desarrollador
```

#### 5.3 No existe comunicación asíncrona

- **CERO uso de mensajería** (RabbitMQ, Kafka, etc.)
- Todas las operaciones son síncronas y bloqueantes entre servicios
- Ejemplo: Crear estudiante → Crear usuario (síncrono) → Si falla, inconsistencia

---

### 6. AUSENCIA TOTAL DE SEGURIDAD (SEVERIDAD: CRÍTICA)

- **No existe autenticación** (no hay JWT, OAuth2, ni ningún mecanismo)
- **No existe autorización** (cualquier endpoint es accesible por cualquiera)
- CORS abierto a `"*"` en todos los servicios
- `allow-credentials: true` CON `allowed-origins: "*"` en enrollments — **Esto es inválido según la especificación CORS**
- No hay validación de tokens entre microservicios
- No hay HTTPS forzado

---

### 7. PROBLEMAS EN EL MODELO DE DOMINIO (SEVERIDAD: ALTA)

#### 7.1 Strings mágicos en lugar de Enums

```java
// En múltiples servicios:
course.setStatus("ACTIVE");      // ← String mágico
course.setStatus("INACTIVE");    // ← String mágico
enrollment.setEnrollmentStatus("ACTIVE"); // ← String mágico

// Aunque EXISTEN enums definidos (EnrollmentStatus, Status, etc.)
// NO SE USAN en las entidades
```

#### 7.2 Enrollment.java es un "God Entity"

```java
// 40+ campos incluyendo:
private String documents;    // ← JSON como String
private String workflow;     // ← JSON como String
// 11 campos booleanos de documentos individuales
// Campos redundantes: institutionId + schoolId, classroomId + roomId
```

#### 7.3 Sin validaciones en el dominio

```java
// CreateStudentRequest — Sin validaciones
// No hay @NotNull, @NotBlank, @Valid
// Las validaciones son manuales e inconsistentes
```

---

### 8. PROBLEMAS EN EL CÓDIGO (SEVERIDAD: MEDIA-ALTA)

#### 8.1 Código duplicado masivamente

```java
// TODOS los servicios tienen el mismo patrón de delete/restore:
public Mono<Entity> delete(ID id) {
    return repository.findById(id)
        .flatMap(entity -> {
            entity.setStatus("INACTIVE"); // o StatusEnum
            entity.setUpdatedAt(LocalDateTime.now());
            return repository.save(entity);
        });
}
// Esto se repite en los 11 microservicios × N entidades
```

#### 8.2 Inyección de dependencias inconsistente

```java
// Algunos usan @RequiredArgsConstructor (Lombok)
@RequiredArgsConstructor
public class PsychologicalEvaluationServiceImpl { ... }

// Otros usan @Autowired (anti-pattern)
public class StudentServiceImpl {
    @Autowired private StudentRepository studentRepository;
    @Autowired private InstitutionService institutionService;
}

// Otros usan constructor manual
public class UserServiceImpl {
    public UserServiceImpl(UserRepository userRepository) { ... }
}
```

#### 8.3 Manejo de errores deficiente

```java
// Errores genéricos sin información útil
return Mono.error(new RuntimeException("Institución no encontrada"));
// Sin códigos de error, sin HTTP status específicos
// Sin GlobalExceptionHandler en la mayoría de microservicios
```

#### 8.4 CatalogServiceImpl — Callback Hell reactivo

```java
// 200+ líneas de flatMap anidados (4-5 niveles de profundidad)
return courseRepository.findAll()
    .flatMap(course ->
        competencyRepository.findByCourseId(course.getId())
            .collectList()
            .flatMap(competencies -> {
                return Flux.fromIterable(competencies)
                    .flatMap(competency ->
                        capacityRepository.findByCompetencyId(competency.getId())
                            .collectList()
                            .flatMap(capacities -> {
                                return Flux.fromIterable(capacities)
                                    .flatMap(capacity ->
                                        performanceRepository.findByCapacityId(capacity.getId())
                                            // ... más anidación
```

---

### 9. PROBLEMAS EN EL FRONTEND (SEVERIDAD: MEDIA)

#### 9.1 Conexión directa a múltiples puertos

```typescript
// vite.config.ts — Solo proxy a UN microservicio
proxy: {
  '/api': {
    target: 'http://localhost:9082', // Solo enrollments
  },
}
// ¿Y los otros 10 microservicios? → Conexión directa, CORS problems
```

#### 9.2 Arquitectura modular pero sin capas de abstracción

```
modules/
├── institution/
│   ├── service/Institution.service.tsx  ← Lógica de API directa
│   ├── components/
│   └── pages/
```

- Sin capa de repositorio/API centralizada
- Sin manejo de estado global (no hay Redux, Zustand, etc.)
- Sin interceptors de HTTP centralizados

#### 9.3 Tailwind v4 (beta) en producción

```json
"tailwindcss": "^4.0.0"
// Tailwind v4 aún es relativamente nuevo, posibles breaking changes
```

---

### 10. PROBLEMAS DE INFRAESTRUCTURA (SEVERIDAD: ALTA)

#### 10.1 Sin Service Discovery

- Cada microservicio necesita conocer las URLs y puertos de los demás
- Cambiar un puerto requiere modificar TODOS los servicios que lo consumen
- Sin Eureka, Consul, ni similar

#### 10.2 Sin configuración centralizada

- No existe Config Server
- Cada microservicio tiene su propia configuración independiente
- Cambiar credenciales requiere modificar N archivos

#### 10.3 Sin observabilidad

- Sin distributed tracing (Zipkin/Jaeger)
- Sin métricas centralizadas (Prometheus/Grafana)
- Logging inconsistente (algunos usan DEBUG, otros INFO)
- Sin health checks estandarizados

#### 10.4 Sin testing

- Solo existe un test vacío por microservicio (`contextLoads`)
- Cero tests unitarios
- Cero tests de integración

---

## 📊 RESUMEN DE HALLAZGOS

| Categoría | Severidad | Cantidad de hallazgos |
|-----------|-----------|----------------------|
| 🔴 Seguridad | CRÍTICA | 6 |
| 🔴 Arquitectura | ALTA | 12 |
| 🟠 Diseño de código | MEDIA-ALTA | 8 |
| 🟡 Frontend | MEDIA | 4 |
| 🟠 Infraestructura | ALTA | 5 |
| **TOTAL** | — | **35 hallazgos** |

---

## 🔗 MAPA DE DEPENDENCIAS ENTRE MICROSERVICIOS

```
┌──────────────────────────────────────────────────────────┐
│                    FRONTEND (React 19)                    │
│               Puerto 5173 — vg-web-sigei                 │
└──────────┬──────┬──────┬──────┬──────┬──────┬──────┬─────┘
           │      │      │      │      │      │      │
           ▼      ▼      ▼      ▼      ▼      ▼      ▼
      ┌────────┐ ┌────┐ ┌────┐ ┌────┐ ┌────┐ ┌────┐ ┌────┐
      │Institut│ │Stud│ │Enro│ │User│ │Note│ │Assi│ │Psyc│
      │  9080  │ │9081│ │9082│ │9083│ │9086│ │9087│ │9090│
      └───┬────┘ └─┬──┘ └─┬──┘ └────┘ └─┬──┘ └─┬──┘ └────┘
          │        │      │              │      │
          │◄───────┘      │              │      │
          │  consulta     │              │      │
          │◄──────────────┘              │      │
          │  consulta                    │      │
          │                              │      │
          │◄─────────────────────────────┘      │
          │  consulta                           │
          │◄────────────────────────────────────┘
          │  consulta
          ▼
     ┌─────────┐                 ┌──────────────┐
     │ MongoDB │                 │ PostgreSQL   │
     │ Atlas   │                 │ Neon (6 inst)│
     └─────────┘                 └──────────────┘
```

**Todas las flechas son síncronas (WebClient HTTP).** No existe comunicación asíncrona.

---

> **Conclusión:** El sistema tiene problemas estructurales graves que deben abordarse antes de continuar añadiendo funcionalidades. Los siguientes documentos proponen la arquitectura correcta para resolver cada uno de estos problemas.
