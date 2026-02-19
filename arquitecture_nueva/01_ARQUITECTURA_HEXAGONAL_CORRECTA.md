# 01 — ARQUITECTURA HEXAGONAL CORRECTA PARA SIGEI

> **Aplicación:** Correcta implementación de Arquitectura Hexagonal (Ports & Adapters) para todos los microservicios de SIGEI

---

## 📐 ¿QUÉ ES LA ARQUITECTURA HEXAGONAL?

La Arquitectura Hexagonal (también llamada Ports & Adapters, propuesta por Alistair Cockburn) se basa en un principio simple:

> **El dominio es el centro. Todo lo demás son detalles.**

```
            ┌──────────────────────────────────────────────┐
            │              ADAPTADORES PRIMARIOS            │
            │        (Driving / Entrada al sistema)         │
            │   REST Controllers │ gRPC │ CLI │ GraphQL     │
            └──────────┬───────────────────┬───────────────┘
                       │                   │
                       ▼                   ▼
            ┌──────────────────────────────────────────────┐
            │              PUERTOS DE ENTRADA               │
            │          (Interfaces de Casos de Uso)         │
            │   CreateStudentUseCase │ EnrollStudentUseCase  │
            └──────────┬───────────────────┬───────────────┘
                       │                   │
                       ▼                   ▼
            ┌──────────────────────────────────────────────┐
            │                                              │
            │             🏛️  DOMINIO (CORE)               │
            │                                              │
            │   Entities │ Value Objects │ Domain Services  │
            │   Domain Events │ Aggregates │ Exceptions     │
            │                                              │
            │          ⚠️ SIN DEPENDENCIAS EXTERNAS         │
            │          (No Spring, No JPA, No MongoDB)      │
            │                                              │
            └──────────┬───────────────────┬───────────────┘
                       │                   │
                       ▼                   ▼
            ┌──────────────────────────────────────────────┐
            │              PUERTOS DE SALIDA                │
            │       (Interfaces definidas en el dominio)    │
            │  StudentRepository │ EventPublisher │ Email   │
            └──────────┬───────────────────┬───────────────┘
                       │                   │
                       ▼                   ▼
            ┌──────────────────────────────────────────────┐
            │             ADAPTADORES SECUNDARIOS           │
            │       (Driven / Salida del sistema)          │
            │   MongoDB │ PostgreSQL │ RabbitMQ │ WebClient │
            └──────────────────────────────────────────────┘
```

---

## 🔴 QUÉ ESTÁ MAL ACTUALMENTE EN SIGEI

### Error 1: El dominio depende de la infraestructura

```java
// ❌ ACTUAL — domain/model/Institution.java
@Document(collection = "institutions")  // ← MongoDB annotation en el dominio
public class Institution {
    @Id
    private String institutionId;
    // ...
}

// ❌ ACTUAL — domain/model/Enrollment.java (R2DBC)
@Table("enrollments")       // ← R2DBC annotation en el dominio
@Column("student_id")       // ← Infraestructura en el dominio
public class Enrollment implements Persistable<String> { ... }
```

### Error 2: No existen Puertos (interfaces)

```java
// ❌ ACTUAL — El servicio depende directamente del repository de infraestructura
@Service
public class CourseServiceImpl implements CourseService {
    private final CourseRepository repository; // ← Interfaz de Spring Data, no puerto de dominio
}
```

### Error 3: Lógica de negocio en la capa de aplicación

```java
// ❌ ACTUAL — CourseServiceImpl.java
course.setInstitutionId("11111111-1111-1111-1111-111111111111"); // ← Regla de negocio hardcodeada
course.setStatus("ACTIVE"); // ← Debería ser validación del dominio
```

### Error 4: Sin Value Objects

```java
// ❌ ACTUAL — User.java
private String documentType;      // ← Solo un String, sin validación
private String documentNumber;    // ← ¿DNI? ¿CE? Sin validación
private String email;             // ← Sin validación de formato
```

---

## ✅ ARQUITECTURA HEXAGONAL CORRECTA — ESTRUCTURA DE CARPETAS

Cada microservicio DEBE seguir esta estructura:

```
vg-ms-{nombre}/
├── pom.xml
├── Dockerfile
├── src/
│   ├── main/
│   │   ├── java/pe/edu/vallegrande/sigei/{modulo}/
│   │   │   │
│   │   │   ├── domain/                          🏛️ DOMINIO (CAPA CORE)
│   │   │   │   ├── models/                      → Entidades y Aggregates
│   │   │   │   │   ├── Student.java             → Entidad raíz (POJO puro)
│   │   │   │   │   └── valueobjects/            → Value Objects y Enums
│   │   │   │   │       ├── PersonalInfo.java
│   │   │   │   │       ├── DocumentType.java    → Enum con validación
│   │   │   │   │       ├── StudentStatus.java   → Enum ACTIVE/INACTIVE
│   │   │   │   │       └── Email.java           → Record con validación
│   │   │   │   │
│   │   │   │   ├── exceptions/                  → Excepciones de dominio
│   │   │   │   │   ├── DomainException.java              → Base abstracta
│   │   │   │   │   ├── NotFoundException.java             → Base para 404
│   │   │   │   │   ├── ConflictException.java             → Base para 409
│   │   │   │   │   ├── StudentNotFoundException.java
│   │   │   │   │   └── DuplicateCuiException.java
│   │   │   │   │
│   │   │   │   ├── ports/                       → PUERTOS (interfaces del dominio)
│   │   │   │   │   ├── in/                      → Puertos de ENTRADA (casos de uso)
│   │   │   │   │   │   ├── ICreateStudentUseCase.java
│   │   │   │   │   │   ├── IUpdateStudentUseCase.java
│   │   │   │   │   │   ├── IGetStudentUseCase.java
│   │   │   │   │   │   ├── IDeleteStudentUseCase.java
│   │   │   │   │   │   └── IRestoreStudentUseCase.java
│   │   │   │   │   │
│   │   │   │   │   └── out/                     → Puertos de SALIDA
│   │   │   │   │       ├── IStudentRepository.java
│   │   │   │   │       ├── IStudentEventPublisher.java
│   │   │   │   │       └── IInstitutionClient.java
│   │   │   │   │
│   │   │   │   └── services/                    → Servicios de dominio (lógica pura, opcional)
│   │   │   │       └── StudentDomainService.java
│   │   │   │
│   │   │   ├── application/                     📋 CAPA DE APLICACIÓN
│   │   │   │   ├── usecases/                    → Implementa puertos de entrada (1 clase = 1 caso)
│   │   │   │   │   ├── CreateStudentUseCaseImpl.java
│   │   │   │   │   ├── UpdateStudentUseCaseImpl.java
│   │   │   │   │   ├── GetStudentUseCaseImpl.java
│   │   │   │   │   ├── DeleteStudentUseCaseImpl.java
│   │   │   │   │   └── RestoreStudentUseCaseImpl.java
│   │   │   │   │
│   │   │   │   ├── dto/                         → DTOs de entrada/salida
│   │   │   │   │   ├── common/                  → Wrappers de respuesta API
│   │   │   │   │   │   ├── ApiResponse.java
│   │   │   │   │   │   └── ErrorResponse.java
│   │   │   │   │   ├── request/
│   │   │   │   │   │   ├── CreateStudentRequest.java
│   │   │   │   │   │   └── UpdateStudentRequest.java
│   │   │   │   │   └── response/
│   │   │   │   │       └── StudentResponse.java
│   │   │   │   │
│   │   │   │   ├── events/                      → Eventos de integración (RabbitMQ)
│   │   │   │   │   ├── StudentCreatedEvent.java
│   │   │   │   │   ├── StudentUpdatedEvent.java
│   │   │   │   │   ├── StudentDeletedEvent.java
│   │   │   │   │   └── StudentRestoredEvent.java
│   │   │   │   │
│   │   │   │   └── mappers/                     → Mappers DTO ↔ Domain
│   │   │   │       └── StudentMapper.java
│   │   │   │
│   │   │   └── infrastructure/                  🔌 CAPA DE INFRAESTRUCTURA
│   │   │       ├── adapters/
│   │   │       │   ├── in/                      → Adaptadores de ENTRADA
│   │   │       │   │   └── rest/
│   │   │       │   │       ├── StudentRest.java             → Controller REST
│   │   │       │   │       └── GlobalExceptionHandler.java
│   │   │       │   │
│   │   │       │   └── out/                     → Adaptadores de SALIDA
│   │   │       │       ├── persistence/
│   │   │       │       │   └── StudentRepositoryImpl.java   → Implementa IStudentRepository
│   │   │       │       ├── external/            → Clientes HTTP a otros microservicios
│   │   │       │       │   └── InstitutionClientImpl.java   → Implementa IInstitutionClient
│   │   │       │       └── messaging/           → Mensajería (RabbitMQ)
│   │   │       │           └── StudentEventPublisherImpl.java → Implementa IStudentEventPublisher
│   │   │       │
│   │   │       ├── config/                      → Configuración de Spring
│   │   │       │   ├── R2dbcConfig.java
│   │   │       │   ├── RabbitMQConfig.java
│   │   │       │   ├── SecurityConfig.java
│   │   │       │   └── WebClientConfig.java
│   │   │       │
│   │   │       ├── persistence/                 → Entidades y repos de BD (separados del adapter)
│   │   │       │   ├── entities/
│   │   │       │   │   └── StudentEntity.java   → @Table("students")
│   │   │       │   └── repositories/
│   │   │       │       └── StudentR2dbcRepository.java → extends ReactiveCrudRepository
│   │   │       │
│   │   │       └── security/                    → Seguridad (opcional)
│   │   │           └── SecurityContextAdapter.java
│   │   │
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       ├── application-prod.yml
│   │       └── db/migration/           → Flyway migrations
│   │           ├── V1__create_students_table.sql
│   │           └── V2__add_indexes.sql
│   │
│   └── test/
│       └── java/pe/edu/vallegrande/sigei/{modulo}/
│           ├── domain/
│           │   └── models/StudentTest.java
│           ├── application/
│           │   └── usecases/CreateStudentUseCaseImplTest.java
│           └── infrastructure/
│               ├── adapters/in/rest/StudentRestTest.java
│               └── adapters/out/persistence/StudentRepositoryImplTest.java
```

---

## ✅ EJEMPLO CONCRETO — MICROSERVICIO DE ESTUDIANTES

### 1. DOMINIO — Entidad Student (SIN dependencias externas)

```java
package pe.edu.vallegrande.sigei.student.domain.model;

import pe.edu.vallegrande.sigei.student.domain.vo.*;
import pe.edu.vallegrande.sigei.student.domain.exception.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Collections;

/**
 * Aggregate Root de Estudiante.
 * NO tiene anotaciones de Spring, MongoDB, JPA, R2DBC, etc.
 */
public class Student {

    private StudentId id;
    private Cui cui;
    private PersonalInfo personalInfo;
    private LocalDate dateOfBirth;
    private String address;
    private String photoUrl;
    private StudentStatus status;
    private InstitutionId institutionId;
    private ClassroomId classroomId;
    private List<Guardian> guardians;
    private DevelopmentInfo developmentInfo;
    private HealthInfo healthInfo;

    // Constructor privado — Factory Method
    private Student() {}

    public static Student create(
            Cui cui,
            PersonalInfo personalInfo,
            LocalDate dateOfBirth,
            InstitutionId institutionId,
            ClassroomId classroomId,
            List<Guardian> guardians) {

        if (cui == null) throw new InvalidStudentDataException("CUI es requerido");
        if (personalInfo == null) throw new InvalidStudentDataException("Información personal es requerida");
        if (dateOfBirth == null) throw new InvalidStudentDataException("Fecha de nacimiento es requerida");
        if (institutionId == null) throw new InvalidStudentDataException("Institución es requerida");
        if (guardians == null || guardians.isEmpty())
            throw new InvalidStudentDataException("Al menos un apoderado es requerido");

        Student student = new Student();
        student.cui = cui;
        student.personalInfo = personalInfo;
        student.dateOfBirth = dateOfBirth;
        student.institutionId = institutionId;
        student.classroomId = classroomId;
        student.guardians = List.copyOf(guardians);
        student.status = StudentStatus.ACTIVE;
        return student;
    }

    public void deactivate() {
        if (this.status == StudentStatus.INACTIVE) {
            throw new InvalidStudentStateException("El estudiante ya está inactivo");
        }
        this.status = StudentStatus.INACTIVE;
    }

    public void activate() {
        if (this.status == StudentStatus.ACTIVE) {
            throw new InvalidStudentStateException("El estudiante ya está activo");
        }
        this.status = StudentStatus.ACTIVE;
    }

    public void transferToClassroom(ClassroomId newClassroomId) {
        if (newClassroomId == null) {
            throw new InvalidStudentDataException("El aula destino es requerida");
        }
        this.classroomId = newClassroomId;
    }

    // Getters (sin setters expuestos para proteger invariantes)
    public StudentId getId() { return id; }
    public Cui getCui() { return cui; }
    public PersonalInfo getPersonalInfo() { return personalInfo; }
    public StudentStatus getStatus() { return status; }
    public InstitutionId getInstitutionId() { return institutionId; }
    public ClassroomId getClassroomId() { return classroomId; }
    public List<Guardian> getGuardians() { return Collections.unmodifiableList(guardians); }
    // ... más getters
}
```

### 2. DOMINIO — Value Objects con validación

```java
package pe.edu.vallegrande.sigei.student.domain.vo;

/**
 * Value Object — CUI (Código Único de Identidad)
 * Inmutable, con validación self-contained.
 */
public record Cui(String value) {

    public Cui {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("CUI no puede estar vacío");
        }
        if (!value.matches("\\d{8,12}")) {
            throw new IllegalArgumentException("CUI debe tener entre 8 y 12 dígitos");
        }
    }
}

/**
 * Value Object — Email
 */
public record Email(String value) {

    private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";

    public Email {
        if (value == null || !value.matches(EMAIL_REGEX)) {
            throw new IllegalArgumentException("Email inválido: " + value);
        }
    }
}

/**
 * Value Object — DocumentNumber (DNI, CE, etc.)
 */
public record DocumentNumber(DocumentType type, String number) {

    public DocumentNumber {
        if (type == null) throw new IllegalArgumentException("Tipo de documento requerido");
        if (number == null || number.isBlank())
            throw new IllegalArgumentException("Número de documento requerido");
        switch (type) {
            case DNI -> { if (!number.matches("\\d{8}")) throw new IllegalArgumentException("DNI debe tener 8 dígitos"); }
            case CE -> { if (!number.matches("\\d{9,12}")) throw new IllegalArgumentException("CE inválido"); }
        }
    }
}
```

### 3. DOMINIO — Puertos de Entrada (Use Cases)

```java
package pe.edu.vallegrande.sigei.student.domain.port.in;

import pe.edu.vallegrande.sigei.student.application.dto.command.CreateStudentCommand;
import pe.edu.vallegrande.sigei.student.application.dto.query.StudentResponse;
import reactor.core.publisher.Mono;

/**
 * Puerto de entrada — Caso de uso: Crear Estudiante.
 * Define QUÉ se puede hacer, no CÓMO.
 */
public interface CreateStudentUseCase {
    Mono<StudentResponse> execute(CreateStudentCommand command);
}
```

### 4. DOMINIO — Puertos de Salida (Repository Port)

```java
package pe.edu.vallegrande.sigei.student.domain.port.out;

import pe.edu.vallegrande.sigei.student.domain.model.Student;
import pe.edu.vallegrande.sigei.student.domain.vo.Cui;
import pe.edu.vallegrande.sigei.student.domain.vo.StudentId;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Puerto de salida — Repositorio de Estudiantes.
 * Interfaz definida por el DOMINIO, implementada por INFRAESTRUCTURA.
 * NO depende de Spring Data, R2DBC, MongoDB, etc.
 */
public interface StudentRepositoryPort {
    Mono<Student> save(Student student);
    Mono<Student> findById(StudentId id);
    Mono<Student> findByCui(Cui cui);
    Flux<Student> findByInstitutionId(String institutionId);
    Flux<Student> findByClassroomId(String classroomId);
    Flux<Student> findAll();
    Mono<Boolean> existsByCui(Cui cui);
}
```

### 5. APLICACIÓN — Caso de Uso (Implementación del Puerto de Entrada)

```java
package pe.edu.vallegrande.sigei.student.application.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pe.edu.vallegrande.sigei.student.application.dto.command.CreateStudentCommand;
import pe.edu.vallegrande.sigei.student.application.dto.query.StudentResponse;
import pe.edu.vallegrande.sigei.student.application.mapper.StudentApplicationMapper;
import pe.edu.vallegrande.sigei.student.domain.exception.DuplicateCuiException;
import pe.edu.vallegrande.sigei.student.domain.model.Student;
import pe.edu.vallegrande.sigei.student.domain.port.in.CreateStudentUseCase;
import pe.edu.vallegrande.sigei.student.domain.port.out.StudentRepositoryPort;
import pe.edu.vallegrande.sigei.student.domain.port.out.InstitutionClientPort;
import pe.edu.vallegrande.sigei.student.domain.port.out.EventPublisherPort;
import pe.edu.vallegrande.sigei.student.domain.vo.Cui;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class CreateStudentUseCaseImpl implements CreateStudentUseCase {

    private final StudentRepositoryPort studentRepository;      // Puerto de salida
    private final InstitutionClientPort institutionClient;       // Puerto de salida
    private final EventPublisherPort eventPublisher;             // Puerto de salida
    private final StudentApplicationMapper mapper;

    @Override
    public Mono<StudentResponse> execute(CreateStudentCommand command) {
        Cui cui = new Cui(command.cui());

        return studentRepository.existsByCui(cui)
            .flatMap(exists -> {
                if (exists) return Mono.error(new DuplicateCuiException(cui.value()));
                return institutionClient.existsAndIsActive(command.institutionId());
            })
            .flatMap(institutionActive -> {
                if (!institutionActive) {
                    return Mono.error(new IllegalArgumentException("Institución no activa"));
                }
                Student student = mapper.toDomain(command);
                return studentRepository.save(student);
            })
            .flatMap(savedStudent -> {
                // Publicar evento de dominio (asíncrono)
                return eventPublisher.publish(new StudentCreated(savedStudent.getId()))
                    .thenReturn(mapper.toResponse(savedStudent));
            });
    }
}
```

### 6. INFRAESTRUCTURA — Adaptador de Persistencia

```java
package pe.edu.vallegrande.sigei.student.infrastructure.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pe.edu.vallegrande.sigei.student.domain.model.Student;
import pe.edu.vallegrande.sigei.student.domain.port.out.StudentRepositoryPort;
import pe.edu.vallegrande.sigei.student.domain.vo.*;
import pe.edu.vallegrande.sigei.student.infrastructure.adapter.out.persistence.entity.StudentEntity;
import pe.edu.vallegrande.sigei.student.infrastructure.adapter.out.persistence.mapper.StudentPersistenceMapper;
import pe.edu.vallegrande.sigei.student.infrastructure.adapter.out.persistence.repository.StudentR2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Adaptador de salida — Implementa el puerto de repositorio.
 * AQUÍ está la dependencia con R2DBC/MongoDB, NO en el dominio.
 */
@Component
@RequiredArgsConstructor
public class StudentPersistenceAdapter implements StudentRepositoryPort {

    private final StudentR2dbcRepository r2dbcRepository;
    private final StudentPersistenceMapper mapper;

    @Override
    public Mono<Student> save(Student student) {
        StudentEntity entity = mapper.toEntity(student);
        return r2dbcRepository.save(entity)
            .map(mapper::toDomain);
    }

    @Override
    public Mono<Student> findById(StudentId id) {
        return r2dbcRepository.findById(id.value())
            .map(mapper::toDomain);
    }

    @Override
    public Flux<Student> findByInstitutionId(String institutionId) {
        return r2dbcRepository.findByInstitutionId(institutionId)
            .map(mapper::toDomain);
    }

    // ... más métodos
}
```

### 7. INFRAESTRUCTURA — Entidad de Persistencia (CON anotaciones)

```java
package pe.edu.vallegrande.sigei.student.infrastructure.adapter.out.persistence.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entidad de persistencia — SOLO infraestructura.
 * Las anotaciones de BD van AQUÍ, no en el dominio.
 */
@Data
@Table("students")
public class StudentEntity {
    @Id
    private String id;
    @Column("cui") private String cui;
    @Column("first_name") private String firstName;
    @Column("last_name") private String lastName;
    @Column("date_of_birth") private LocalDate dateOfBirth;
    @Column("institution_id") private String institutionId;
    @Column("classroom_id") private String classroomId;
    @Column("status") private String status;
    @Column("created_at") private LocalDateTime createdAt;
    @Column("updated_at") private LocalDateTime updatedAt;
}
```

### 8. INFRAESTRUCTURA — Controlador REST (Adaptador de Entrada)

```java
package pe.edu.vallegrande.sigei.student.infrastructure.adapter.in.rest;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pe.edu.vallegrande.sigei.student.domain.port.in.*;
import pe.edu.vallegrande.sigei.student.infrastructure.adapter.in.rest.dto.*;
import pe.edu.vallegrande.sigei.student.infrastructure.adapter.in.rest.mapper.StudentRestMapper;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import jakarta.validation.Valid;

/**
 * Adaptador de entrada REST.
 * Depende SOLO de los puertos de entrada (Use Cases), NO del servicio directamente.
 */
@RestController
@RequestMapping("/api/v1/students")
@RequiredArgsConstructor
public class StudentController {

    private final CreateStudentUseCase createStudentUseCase;
    private final FindStudentUseCase findStudentUseCase;
    private final UpdateStudentUseCase updateStudentUseCase;
    private final DeactivateStudentUseCase deactivateStudentUseCase;
    private final StudentRestMapper mapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<StudentResponseDto> create(@Valid @RequestBody CreateStudentRequestDto request) {
        return createStudentUseCase.execute(mapper.toCommand(request))
            .map(mapper::toResponseDto);
    }

    @GetMapping("/{id}")
    public Mono<StudentResponseDto> findById(@PathVariable String id) {
        return findStudentUseCase.findById(id)
            .map(mapper::toResponseDto);
    }

    @GetMapping
    public Flux<StudentResponseDto> findAll() {
        return findStudentUseCase.findAll()
            .map(mapper::toResponseDto);
    }
}
```

---

## 📏 REGLAS DE DEPENDENCIA (OBLIGATORIAS)

```
┌────────────────────────────────────────────┐
│                                            │
│  INFRAESTRUCTURA  ──────→  APLICACIÓN      │
│         │                      │           │
│         │                      │           │
│         └──────────→  DOMINIO  ◄───────────┘
│                                            │
│  ⚠️ DOMINIO no depende de NADA             │
│  ⚠️ APLICACIÓN solo depende de DOMINIO     │
│  ⚠️ INFRAESTRUCTURA depende de ambos       │
│                                            │
└────────────────────────────────────────────┘
```

| Capa | Puede importar de | NO puede importar de |
|------|-------------------|---------------------|
| `domain` | `java.*`, `java.time.*` | Spring, Lombok, R2DBC, MongoDB, Jackson |
| `application` | `domain`, Spring (`@Service`) | `infrastructure` |
| `infrastructure` | `domain`, `application`, Spring, R2DBC, MongoDB, WebClient | — |

> **Excepción de Lombok:** Se permite `@RequiredArgsConstructor` en application e infrastructure, pero NO en domain. En domain, los constructores deben ser explícitos para proteger invariantes.

---

## 🔧 MIGRACIÓN DESDE EL ESTADO ACTUAL

### Paso 1: Extraer modelo de dominio limpio

- Crear entidades de dominio SIN anotaciones de persistencia
- Crear Value Objects con validación
- Crear excepciones de dominio específicas

### Paso 2: Crear Puertos

- Definir interfaces de casos de uso (puertos de entrada)
- Definir interfaces de repositorio (puertos de salida)
- Definir interfaces de clientes externos (puertos de salida)

### Paso 3: Implementar casos de uso

- Mover lógica de negocio de los `*ServiceImpl` a casos de uso
- Los casos de uso dependen SOLO de puertos

### Paso 4: Crear adaptadores

- Crear entidades de persistencia separadas con anotaciones
- Crear adaptadores de persistencia que implementen los puertos
- Crear mappers entre entidades de dominio y de persistencia
- Mover controllers a adaptadores de entrada

---

> **Siguiente:** Ver `02_COMUNICACION_SINCRONA_ASINCRONA.md` para la estrategia de comunicación entre microservicios.
