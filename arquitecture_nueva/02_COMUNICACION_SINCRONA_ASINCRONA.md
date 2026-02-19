# 02 — COMUNICACIÓN SÍNCRONA Y ASÍNCRONA ENTRE MICROSERVICIOS

> **Objetivo:** Definir cuándo usar comunicación síncrona vs asíncrona en SIGEI, con implementaciones concretas

---

## 📊 ESTADO ACTUAL: 100% SÍNCRONO, 0% RESILIENTE

Actualmente **TODA** la comunicación entre microservicios es:

- **Síncrona** (WebClient HTTP bloqueante en cadena)
- **Sin Circuit Breaker** (si un servicio cae, todo el flujo falla)
- **Sin Retry configurable** (excepto enrollments con 3 intentos)
- **Sin Fallback** (no hay respuesta alternativa ante fallos)

```
Ejemplo actual — Crear Estudiante:

Frontend → POST /api/v1/students → Students MS
    Students MS → POST /api/v1/users → Users MS (síncrono, sin fallback)
    Students MS → GET /api/v1/institutions/{id} → Institution MS (síncrono)
    Students MS → GET /api/v1/classrooms/{id} → Institution MS (síncrono)

⚠️ Si Users MS está caído → El estudiante NO se crea
⚠️ Si Institution MS está lento → TODO se ralentiza
⚠️ No hay compensación: si el usuario se crea pero el estudiante falla → datos inconsistentes
```

---

## ✅ ESTRATEGIA PROPUESTA: COMUNICACIÓN HÍBRIDA

### REGLA GENERAL

| Tipo | Cuándo usar | Tecnología |
|------|-------------|------------|
| **Síncrona** | Cuando necesitas la respuesta INMEDIATAMENTE para continuar | WebClient + Resilience4j |
| **Asíncrona** | Cuando puedes continuar sin esperar la respuesta | RabbitMQ (AMQP) |

---

## 🔵 COMUNICACIÓN SÍNCRONA — WebClient + Resilience4j

### ¿Cuándo es necesaria la comunicación síncrona?

| Operación | Motivo de sincronía |
|-----------|-------------------|
| Validar que institución existe al crear matrícula | Necesitas respuesta inmediata para continuar/rechazar |
| Obtener datos de estudiante para mostrar en evaluación | El frontend necesita la data completa |
| Validar aula al asignar docente | Necesitas confirmar disponibilidad |
| Obtener notas de un estudiante junto con datos del curso | Consulta compuesta para el frontend |

### Implementación con Resilience4j

```xml
<!-- pom.xml — Agregar a TODOS los microservicios -->
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId>
</dependency>
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-reactor</artifactId>
</dependency>
```

```yaml
# application.yml — Configuración de Resilience4j
resilience4j:
  circuitbreaker:
    instances:
      institutionService:
        registerHealthIndicator: true
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        failureRateThreshold: 50
        waitDurationInOpenState: 30s
        permittedNumberOfCallsInHalfOpenState: 3
        slidingWindowType: COUNT_BASED
      studentService:
        registerHealthIndicator: true
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 30s

  retry:
    instances:
      institutionService:
        maxAttempts: 3
        waitDuration: 1s
        retryExceptions:
          - java.net.ConnectException
          - java.util.concurrent.TimeoutException

  timelimiter:
    instances:
      institutionService:
        timeoutDuration: 5s
```

```java
package pe.edu.vallegrande.sigei.student.infrastructure.adapter.out.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import pe.edu.vallegrande.sigei.student.domain.port.out.InstitutionClientPort;
import reactor.core.publisher.Mono;

/**
 * Adaptador de salida — Cliente HTTP con resiliencia.
 * Implementa el puerto de salida del dominio.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InstitutionClientAdapter implements InstitutionClientPort {

    private final WebClient institutionWebClient;

    @Override
    @CircuitBreaker(name = "institutionService", fallbackMethod = "existsFallback")
    @Retry(name = "institutionService")
    @TimeLimiter(name = "institutionService")
    public Mono<Boolean> existsAndIsActive(String institutionId) {
        return institutionWebClient.get()
            .uri("/api/v1/institutions/{id}", institutionId)
            .retrieve()
            .bodyToMono(InstitutionDto.class)
            .map(inst -> "ACTIVE".equals(inst.getStatus()))
            .onErrorReturn(false);
    }

    // Fallback cuando el Circuit Breaker está abierto
    private Mono<Boolean> existsFallback(String institutionId, Throwable t) {
        log.warn("Circuit Breaker abierto para institution-service. " +
                 "Usando fallback para institutionId: {}", institutionId);
        // Fallback: asumir que existe si es un ID conocido (caché, etc.)
        return Mono.just(false);
    }
}
```

---

## 🟢 COMUNICACIÓN ASÍNCRONA — RabbitMQ

### ¿Cuándo usar comunicación asíncrona?

| Evento | Productor | Consumidores | Motivo |
|--------|-----------|-------------|--------|
| `student.created` | Students | Enrollments, Notifications | Enrollments puede preparar datos; Notificación a padres |
| `student.enrolled` | Enrollments | Notifications, Assistance | Notificar; Inicializar registro de asistencia |
| `enrollment.approved` | Enrollments | Students, Notifications | Actualizar estado del estudiante; Notificar |
| `institution.created` | Institution | Academic-Management | Preparar catálogo académico base |
| `institution.updated` | Institution | Students, Users, Enrollments | Propagar cambios de nombre, etc. |
| `attendance.marked` | Assistance | Notifications, Psychology | Alertar ausencias frecuentes |
| `incident.created` | Disciplinary | Notifications, Psychology | Alertar a padres; Evaluar necesidad psicológica |
| `evaluation.completed` | Notes | Notifications | Notificar notas publicadas a padres |
| `user.created` | Users | Institution | Vincular usuario al rol institucional |
| `user.deactivated` | Users | Institution, Teacher-Assignment | Reasignar docente, desvincular director |

### Implementación con RabbitMQ

```xml
<!-- pom.xml — Agregar a microservicios que publican/consumen eventos -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

```yaml
# application.yml
spring:
  rabbitmq:
    host: ${RABBITMQ_HOST:localhost}
    port: ${RABBITMQ_PORT:5672}
    username: ${RABBITMQ_USER:sigei}
    password: ${RABBITMQ_PASS:sigei_password}
    virtual-host: /sigei
```

#### Configuración de Exchanges, Queues y Bindings

```java
package pe.edu.vallegrande.sigei.student.infrastructure.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    // Exchanges (Topic para flexibilidad de routing)
    public static final String STUDENT_EXCHANGE = "sigei.student.events";
    public static final String ENROLLMENT_EXCHANGE = "sigei.enrollment.events";
    public static final String INSTITUTION_EXCHANGE = "sigei.institution.events";
    public static final String ATTENDANCE_EXCHANGE = "sigei.attendance.events";
    public static final String INCIDENT_EXCHANGE = "sigei.incident.events";

    // Queues
    public static final String ENROLLMENT_STUDENT_CREATED_QUEUE = "enrollment.student.created";
    public static final String NOTIFICATION_STUDENT_CREATED_QUEUE = "notification.student.created";
    public static final String ASSISTANCE_ENROLLMENT_APPROVED_QUEUE = "assistance.enrollment.approved";

    @Bean
    public TopicExchange studentExchange() {
        return new TopicExchange(STUDENT_EXCHANGE);
    }

    @Bean
    public Queue enrollmentStudentCreatedQueue() {
        return QueueBuilder.durable(ENROLLMENT_STUDENT_CREATED_QUEUE).build();
    }

    @Bean
    public Queue notificationStudentCreatedQueue() {
        return QueueBuilder.durable(NOTIFICATION_STUDENT_CREATED_QUEUE).build();
    }

    @Bean
    public Binding enrollmentStudentCreatedBinding() {
        return BindingBuilder
            .bind(enrollmentStudentCreatedQueue())
            .to(studentExchange())
            .with("student.created");
    }

    @Bean
    public Binding notificationStudentCreatedBinding() {
        return BindingBuilder
            .bind(notificationStudentCreatedQueue())
            .to(studentExchange())
            .with("student.created");
    }

    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
```

#### Publicador de Eventos

```java
package pe.edu.vallegrande.sigei.student.infrastructure.adapter.out.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import pe.edu.vallegrande.sigei.student.domain.event.StudentCreated;
import pe.edu.vallegrande.sigei.student.domain.port.out.EventPublisherPort;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitEventPublisher implements EventPublisherPort {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public Mono<Void> publish(StudentCreated event) {
        return Mono.fromRunnable(() -> {
            log.info("Publicando evento student.created: {}", event.studentId());
            rabbitTemplate.convertAndSend(
                "sigei.student.events",    // exchange
                "student.created",          // routing key
                event                       // mensaje (serializado a JSON)
            );
        });
    }
}
```

#### Consumidor de Eventos

```java
package pe.edu.vallegrande.sigei.enrollment.infrastructure.adapter.out.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StudentEventListener {

    @RabbitListener(queues = "enrollment.student.created")
    public void handleStudentCreated(StudentCreatedEvent event) {
        log.info("Recibido evento student.created: {}", event.getStudentId());
        // Lógica: pre-cargar datos del estudiante, preparar matrícula pendiente, etc.
    }
}
```

---

## 📋 MAPA COMPLETO DE COMUNICACIÓN

```
┌─────────────────────────────────────────────────────────────────┐
│                     COMUNICACIÓN SÍNCRONA (HTTP)                │
│                         WebClient + Resilience4j                │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Enrollments ──HTTP──► Institution (validar institución)        │
│  Enrollments ──HTTP──► Students (validar estudiante)            │
│  Students ──HTTP──► Institution (obtener datos de institución)  │
│  Notes ──HTTP──► Institution (obtener institución)              │
│  Notes ──HTTP──► Students (obtener datos estudiante)            │
│  Assistance ──HTTP──► Institution (obtener aulas)               │
│  Assistance ──HTTP──► Students (obtener estudiantes)            │
│  Teacher-Assignment ──HTTP──► Users (validar docente)           │
│  Teacher-Assignment ──HTTP──► Institution (validar aula)        │
│  Teacher-Assignment ──HTTP──► Academic (validar curso)          │
│  Disciplinary ──HTTP──► Students (obtener datos)                │
│  Disciplinary ──HTTP──► Institution (obtener datos)             │
│  Psychology ──HTTP──► Students (obtener datos)                  │
│  Civic-Dates ──HTTP──► Institution (obtener institución)        │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                   COMUNICACIÓN ASÍNCRONA (RabbitMQ)             │
│                        Eventos de Dominio                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌──────────────┐   student.created   ┌──────────────┐         │
│  │   Students   │──────────────────►  │ Enrollments  │         │
│  │              │──────────────────►  │ Notifications│         │
│  └──────────────┘                     └──────────────┘         │
│                                                                 │
│  ┌──────────────┐  enrollment.approved ┌──────────────┐        │
│  │ Enrollments  │──────────────────►   │ Assistance   │        │
│  │              │──────────────────►   │ Notifications│        │
│  └──────────────┘                      └──────────────┘        │
│                                                                 │
│  ┌──────────────┐ attendance.absent_3+ ┌──────────────┐        │
│  │ Assistance   │──────────────────►   │ Notifications│        │
│  │              │──────────────────►   │ Psychology   │        │
│  └──────────────┘                      └──────────────┘        │
│                                                                 │
│  ┌──────────────┐  incident.created    ┌──────────────┐        │
│  │ Disciplinary │──────────────────►   │ Notifications│        │
│  │              │──────────────────►   │ Psychology   │        │
│  └──────────────┘                      └──────────────┘        │
│                                                                 │
│  ┌──────────────┐  evaluation.graded   ┌──────────────┐        │
│  │    Notes     │──────────────────►   │ Notifications│        │
│  └──────────────┘                      └──────────────┘        │
│                                                                 │
│  ┌──────────────┐ institution.updated  ┌──────────────┐        │
│  │ Institution  │──────────────────►   │  Students    │        │
│  │              │──────────────────►   │  Enrollments │        │
│  │              │──────────────────►   │  Academic    │        │
│  └──────────────┘                      └──────────────┘        │
│                                                                 │
│  ┌──────────────┐   user.deactivated   ┌──────────────┐        │
│  │    Users     │──────────────────►   │ Institution  │        │
│  │              │──────────────────►   │ Teacher-Assg │        │
│  └──────────────┘                      └──────────────┘        │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🔧 PATRÓN SAGA PARA OPERACIONES DISTRIBUIDAS

Para operaciones que involucran múltiples microservicios (como crear un estudiante con sus apoderados), usar **Saga Coreografiada**:

```
1. Students MS → Crea estudiante con estado PENDING
2. Students MS → Publica evento "student.creation_requested"
3. Users MS → Consume evento, crea usuarios apoderados
4. Users MS → Publica evento "guardians.created" con IDs
5. Students MS → Consume evento, actualiza guardians del estudiante
6. Students MS → Cambia estado a ACTIVE
7. Students MS → Publica evento "student.created"

Compensación (si Users MS falla):
3b. Users MS → Publica evento "guardians.creation_failed"
4b. Students MS → Consume evento, elimina estudiante pendiente
4c. Students MS → Publica evento "student.creation_failed"
```

---

## 📊 RESUMEN DE BENEFICIOS

| Aspecto | Antes | Después |
|---------|-------|---------|
| Resiliencia | ❌ Cascading failures | ✅ Circuit Breaker + Fallback |
| Acoplamiento | ❌ Alto (todos dependen de todos) | ✅ Bajo (eventos desacoplados) |
| Disponibilidad | ❌ Si 1 cae, todos fallan | ✅ Degradación graceful |
| Notificaciones | ❌ No existen | ✅ Reactivas por eventos |
| Consistencia | ❌ Inconsistencia silenciosa | ✅ Eventual consistency con Sagas |
| Escalabilidad | ❌ Limitada (todo síncrono) | ✅ Consumers independientes escalables |

---

> **Siguiente:** Ver `03_BASE_DE_DATOS_RECOMENDACION.md` para la estrategia de bases de datos.
