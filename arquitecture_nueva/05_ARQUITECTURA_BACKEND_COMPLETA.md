# 05 — ARQUITECTURA BACKEND COMPLETA

> Estructura de carpetas unificada para TODOS los microservicios siguiendo Arquitectura Hexagonal + DDD

---

## 📁 ESTRUCTURA BASE — Aplicable a cada microservicio

```
vg-ms-{nombre}/
├── pom.xml
├── Dockerfile
├── README.md
├── src/
│   ├── main/
│   │   ├── java/pe/edu/vallegrande/sigei/{modulo}/
│   │   │   │
│   │   │   ├── {Modulo}Application.java          ← Clase principal
│   │   │   │
│   │   │   ├── domain/                            ← 🟢 NÚCLEO (sin dependencias externas)
│   │   │   │   ├── model/                         ← Entidades y Value Objects
│   │   │   │   │   ├── {Entidad}.java
│   │   │   │   │   ├── {ValueObject}.java
│   │   │   │   │   └── enums/
│   │   │   │   │       └── {Enum}.java
│   │   │   │   ├── port/                          ← Puertos (interfaces)
│   │   │   │   │   ├── in/                        ← Puertos de entrada
│   │   │   │   │   │   └── {Accion}UseCase.java
│   │   │   │   │   └── out/                       ← Puertos de salida
│   │   │   │   │       ├── {Entidad}Repository.java
│   │   │   │   │       └── {Servicio}Client.java
│   │   │   │   ├── exception/                     ← Excepciones de dominio
│   │   │   │   │   ├── {Entidad}NotFoundException.java
│   │   │   │   │   └── BusinessRuleViolationException.java
│   │   │   │   └── event/                         ← Eventos de dominio
│   │   │   │       └── {Entidad}{Accion}Event.java
│   │   │   │
│   │   │   ├── application/                       ← 🟡 CASOS DE USO
│   │   │   │   ├── service/                       ← Implementación de Use Cases
│   │   │   │   │   └── {Entidad}Service.java
│   │   │   │   ├── dto/                           ← DTOs de entrada/salida
│   │   │   │   │   ├── request/
│   │   │   │   │   │   └── Create{Entidad}Request.java
│   │   │   │   │   └── response/
│   │   │   │   │       └── {Entidad}Response.java
│   │   │   │   └── mapper/                        ← Mappers Dominio ↔ DTO
│   │   │   │       └── {Entidad}Mapper.java
│   │   │   │
│   │   │   └── infrastructure/                    ← 🔴 ADAPTADORES
│   │   │       ├── adapter/
│   │   │       │   ├── in/                        ← Adaptadores de entrada
│   │   │       │   │   └── rest/
│   │   │       │   │       ├── {Entidad}Controller.java
│   │   │       │   │       └── GlobalExceptionHandler.java
│   │   │       │   └── out/                       ← Adaptadores de salida
│   │   │       │       ├── persistence/
│   │   │       │       │   ├── entity/
│   │   │       │       │   │   └── {Entidad}Entity.java
│   │   │       │       │   ├── mapper/
│   │   │       │       │   │   └── {Entidad}PersistenceMapper.java
│   │   │       │       │   ├── repository/
│   │   │       │       │   │   └── R2dbc{Entidad}Repository.java
│   │   │       │       │   └── {Entidad}PersistenceAdapter.java
│   │   │       │       ├── messaging/
│   │   │       │       │   └── RabbitMQ{Evento}Publisher.java
│   │   │       │       └── client/
│   │   │       │           └── {Servicio}WebClientAdapter.java
│   │   │       └── config/
│   │   │           ├── DatabaseConfig.java
│   │   │           ├── WebClientConfig.java
│   │   │           ├── RabbitMQConfig.java
│   │   │           └── SecurityConfig.java
│   │   │
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       ├── application-prod.yml
│   │       └── db/migration/                      ← Flyway
│   │           ├── V1__create_tables.sql
│   │           └── V2__seed_data.sql
│   │
│   └── test/
│       └── java/pe/edu/vallegrande/sigei/{modulo}/
│           ├── domain/
│           │   ├── model/
│           │   │   └── {Entidad}Test.java
│           │   └── service/
│           │       └── {Entidad}ServiceTest.java  ← Tests unitarios (mockear ports)
│           ├── application/
│           │   └── service/
│           │       └── {Entidad}ServiceIntegrationTest.java
│           └── infrastructure/
│               ├── adapter/in/rest/
│               │   └── {Entidad}ControllerTest.java  ← @WebFluxTest
│               └── adapter/out/persistence/
│                   └── {Entidad}PersistenceAdapterTest.java ← @DataR2dbcTest
```

---

## 📦 EJEMPLO COMPLETO — vg-ms-enrollments

```
vg-ms-enrollments/
├── pom.xml
├── Dockerfile
├── src/
│   ├── main/
│   │   ├── java/pe/edu/vallegrande/sigei/enrollment/
│   │   │   │
│   │   │   ├── EnrollmentApplication.java
│   │   │   │
│   │   │   ├── domain/
│   │   │   │   ├── model/
│   │   │   │   │   ├── Enrollment.java
│   │   │   │   │   ├── AcademicPeriod.java
│   │   │   │   │   ├── Student.java               ← Value Object (solo ID + nombre)
│   │   │   │   │   ├── Section.java                ← Value Object
│   │   │   │   │   └── enums/
│   │   │   │   │       ├── EnrollmentStatus.java
│   │   │   │   │       ├── EnrollmentType.java
│   │   │   │   │       └── EducationLevel.java
│   │   │   │   ├── port/
│   │   │   │   │   ├── in/
│   │   │   │   │   │   ├── CreateEnrollmentUseCase.java
│   │   │   │   │   │   ├── FindEnrollmentUseCase.java
│   │   │   │   │   │   ├── UpdateEnrollmentUseCase.java
│   │   │   │   │   │   └── ManageAcademicPeriodUseCase.java
│   │   │   │   │   └── out/
│   │   │   │   │       ├── EnrollmentRepository.java
│   │   │   │   │       ├── AcademicPeriodRepository.java
│   │   │   │   │       ├── StudentServiceClient.java
│   │   │   │   │       └── InstitutionServiceClient.java
│   │   │   │   ├── exception/
│   │   │   │   │   ├── EnrollmentNotFoundException.java
│   │   │   │   │   ├── DuplicateEnrollmentException.java
│   │   │   │   │   ├── AcademicPeriodClosedException.java
│   │   │   │   │   └── SectionCapacityExceededException.java
│   │   │   │   └── event/
│   │   │   │       ├── EnrollmentCreatedEvent.java
│   │   │   │       └── EnrollmentCancelledEvent.java
│   │   │   │
│   │   │   ├── application/
│   │   │   │   ├── service/
│   │   │   │   │   ├── EnrollmentService.java
│   │   │   │   │   └── AcademicPeriodService.java
│   │   │   │   ├── dto/
│   │   │   │   │   ├── request/
│   │   │   │   │   │   ├── CreateEnrollmentRequest.java
│   │   │   │   │   │   ├── UpdateEnrollmentRequest.java
│   │   │   │   │   │   └── CreateAcademicPeriodRequest.java
│   │   │   │   │   └── response/
│   │   │   │   │       ├── EnrollmentResponse.java
│   │   │   │   │       ├── EnrollmentDetailResponse.java
│   │   │   │   │       └── AcademicPeriodResponse.java
│   │   │   │   └── mapper/
│   │   │   │       ├── EnrollmentMapper.java
│   │   │   │       └── AcademicPeriodMapper.java
│   │   │   │
│   │   │   └── infrastructure/
│   │   │       ├── adapter/
│   │   │       │   ├── in/rest/
│   │   │       │   │   ├── EnrollmentController.java
│   │   │       │   │   ├── AcademicPeriodController.java
│   │   │       │   │   └── GlobalExceptionHandler.java
│   │   │       │   └── out/
│   │   │       │       ├── persistence/
│   │   │       │       │   ├── entity/
│   │   │       │       │   │   ├── EnrollmentEntity.java    ← @Table("enrollments")
│   │   │       │       │   │   └── AcademicPeriodEntity.java
│   │   │       │       │   ├── mapper/
│   │   │       │       │   │   └── EnrollmentPersistenceMapper.java
│   │   │       │       │   ├── repository/
│   │   │       │       │   │   ├── R2dbcEnrollmentRepository.java
│   │   │       │       │   │   └── R2dbcAcademicPeriodRepository.java
│   │   │       │       │   └── EnrollmentPersistenceAdapter.java
│   │   │       │       ├── messaging/
│   │   │       │       │   └── EnrollmentEventPublisher.java
│   │   │       │       └── client/
│   │   │       │           ├── StudentWebClientAdapter.java
│   │   │       │           └── InstitutionWebClientAdapter.java
│   │   │       └── config/
│   │   │           ├── DatabaseConfig.java
│   │   │           ├── WebClientConfig.java
│   │   │           ├── RabbitMQConfig.java
│   │   │           └── R2dbcConfig.java
│   │   │
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       ├── application-prod.yml
│   │       └── db/migration/
│   │           ├── V1__create_enrollment_tables.sql
│   │           └── V2__create_academic_period_tables.sql
│   │
│   └── test/
│       └── java/pe/edu/vallegrande/sigei/enrollment/
│           ├── domain/model/
│           │   └── EnrollmentTest.java
│           ├── application/service/
│           │   └── EnrollmentServiceTest.java
│           └── infrastructure/
│               ├── adapter/in/rest/
│               │   └── EnrollmentControllerTest.java
│               └── adapter/out/persistence/
│                   └── EnrollmentPersistenceAdapterTest.java
```

---

## 📦 EJEMPLO COMPLETO — vg-ms-institution-management (MongoDB)

```
vg-ms-institution-management/
├── src/main/java/pe/edu/vallegrande/sigei/institution/
│   │
│   ├── InstitutionApplication.java
│   │
│   ├── domain/
│   │   ├── model/
│   │   │   ├── Institution.java            ← POJO puro, sin @Document
│   │   │   ├── Classroom.java
│   │   │   ├── Address.java                ← Value Object
│   │   │   ├── Schedule.java               ← Value Object
│   │   │   ├── ContactMethod.java          ← Value Object
│   │   │   └── enums/
│   │   │       ├── InstitutionStatus.java
│   │   │       └── ClassroomStatus.java
│   │   ├── port/
│   │   │   ├── in/
│   │   │   │   ├── CreateInstitutionUseCase.java
│   │   │   │   ├── FindInstitutionUseCase.java
│   │   │   │   ├── UpdateInstitutionUseCase.java
│   │   │   │   ├── ManageClassroomUseCase.java
│   │   │   │   └── ValidateInstitutionUseCase.java
│   │   │   └── out/
│   │   │       ├── InstitutionRepository.java
│   │   │       └── ClassroomRepository.java
│   │   ├── exception/
│   │   │   ├── InstitutionNotFoundException.java
│   │   │   ├── DuplicateModularCodeException.java
│   │   │   └── ClassroomCapacityException.java
│   │   └── event/
│   │       ├── InstitutionCreatedEvent.java
│   │       └── ClassroomCreatedEvent.java
│   │
│   ├── application/
│   │   ├── service/
│   │   │   ├── InstitutionService.java
│   │   │   └── ClassroomService.java
│   │   ├── dto/
│   │   │   ├── request/
│   │   │   │   ├── CreateInstitutionRequest.java
│   │   │   │   ├── UpdateInstitutionRequest.java
│   │   │   │   └── CreateClassroomRequest.java
│   │   │   └── response/
│   │   │       ├── InstitutionResponse.java
│   │   │       ├── InstitutionDetailResponse.java
│   │   │       └── ClassroomResponse.java
│   │   └── mapper/
│   │       ├── InstitutionMapper.java
│   │       └── ClassroomMapper.java
│   │
│   └── infrastructure/
│       ├── adapter/
│       │   ├── in/rest/
│       │   │   ├── InstitutionController.java
│       │   │   ├── ClassroomController.java
│       │   │   └── GlobalExceptionHandler.java
│       │   └── out/persistence/
│       │       ├── document/
│       │       │   ├── InstitutionDocument.java     ← @Document("institutions")
│       │       │   └── ClassroomDocument.java       ← @Document("classrooms")
│       │       ├── mapper/
│       │       │   └── InstitutionPersistenceMapper.java
│       │       ├── repository/
│       │       │   ├── MongoInstitutionRepository.java
│       │       │   └── MongoClassroomRepository.java
│       │       └── InstitutionPersistenceAdapter.java
│       └── config/
│           ├── MongoConfig.java
│           └── SecurityConfig.java
```

---

## 🧩 CÓDIGO — Capa de Dominio (PURO, sin dependencias)

### Institution.java (Dominio)

```java
package pe.edu.vallegrande.sigei.institution.domain.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Agregado raíz del dominio de Institución.
 * SIN anotaciones de persistencia, SIN dependencias de Spring.
 */
public class Institution {

    private String id;
    private String modularCode;        // Código modular UGEL
    private String name;
    private Address address;
    private List<ContactMethod> contacts;
    private List<Schedule> schedules;
    private InstitutionStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructor privado — usar Factory Method
    private Institution() {}

    // Factory Method — con validaciones de negocio
    public static Institution create(
            String modularCode,
            String name,
            Address address) {

        // Validaciones de dominio
        if (modularCode == null || modularCode.length() != 7) {
            throw new IllegalArgumentException(
                "El código modular debe tener 7 dígitos (estándar MINEDU)");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("El nombre es obligatorio");
        }

        Institution institution = new Institution();
        institution.modularCode = modularCode;
        institution.name = name.toUpperCase();
        // NOTA: Todas las instituciones son de nivel INICIAL y gestión PRIVADA
        // No se necesita enum de tipo ni de gestión
        institution.address = address;
        institution.status = InstitutionStatus.ACTIVE;
        institution.createdAt = LocalDateTime.now();
        institution.updatedAt = LocalDateTime.now();
        return institution;
    }

    // Comportamiento de dominio
    public void deactivate() {
        if (this.status == InstitutionStatus.INACTIVE) {
            throw new IllegalStateException("La institución ya está inactiva");
        }
        this.status = InstitutionStatus.INACTIVE;
        this.updatedAt = LocalDateTime.now();
    }

    public void reactivate() {
        this.status = InstitutionStatus.ACTIVE;
        this.updatedAt = LocalDateTime.now();
    }

    // Getters (sin setters — inmutable excepto por métodos de dominio)
    public String getId() { return id; }
    public String getModularCode() { return modularCode; }
    public String getName() { return name; }
    public Address getAddress() { return address; }
    public List<ContactMethod> getContacts() { return contacts; }
    public List<Schedule> getSchedules() { return schedules; }
    public InstitutionStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
```

### Enums del Dominio

```java
// InstitutionStatus.java
public enum InstitutionStatus {
    ACTIVE, INACTIVE;
}

// NOTA: No se necesitan enums InstitutionType ni ManagementType
// porque TODAS las instituciones de SIGEI son:
//   - Nivel: INICIAL (3-5 años)
//   - Gestión: PRIVADA
// Esto simplifica el modelo y evita complejidad innecesaria.
```

### Value Objects

```java
// Address.java
public record Address(
    String department,     // Departamento (Lima, Arequipa, etc.)
    String province,       // Provincia
    String district,       // Distrito
    String urbanization,   // Urbanización/AA.HH.
    String street,         // Dirección
    String reference,      // Referencia
    String ubigeo          // Código UBIGEO (6 dígitos — INEI)
) {
    public Address {
        if (ubigeo != null && ubigeo.length() != 6) {
            throw new IllegalArgumentException("UBIGEO debe tener 6 dígitos");
        }
    }
}

// ContactMethod.java
public record ContactMethod(
    String type,     // PHONE, EMAIL, WEBSITE
    String value,
    boolean primary
) {}

// Schedule.java
public record Schedule(
    String shift,       // MAÑANA, TARDE, NOCHE
    String startTime,   // "07:30"
    String endTime      // "12:30"
) {}
```

---

## 🧩 CÓDIGO — Puertos (Interfaces)

```java
// === Puerto de ENTRADA (Use Case) ===
package pe.edu.vallegrande.sigei.institution.domain.port.in;

import reactor.core.publisher.Mono;

public interface CreateInstitutionUseCase {
    Mono<Institution> create(Institution institution);
}

public interface FindInstitutionUseCase {
    Mono<Institution> findById(String id);
    Flux<Institution> findByStatus(InstitutionStatus status);
    Flux<Institution> findByDepartmentAndProvince(String department, String province);
    Mono<Institution> findByModularCode(String modularCode);
}

// === Puerto de SALIDA (Repository) ===
package pe.edu.vallegrande.sigei.institution.domain.port.out;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface InstitutionRepository {
    Mono<Institution> save(Institution institution);
    Mono<Institution> findById(String id);
    Mono<Institution> findByModularCode(String modularCode);
    Flux<Institution> findByStatus(InstitutionStatus status);
    Flux<Institution> findByAddressDepartmentAndAddressProvince(String dept, String prov);
    Mono<Boolean> existsByModularCode(String modularCode);
}
```

---

## 🧩 CÓDIGO — Capa de Aplicación (Orquesta Use Cases)

```java
package pe.edu.vallegrande.sigei.institution.application.service;

import pe.edu.vallegrande.sigei.institution.domain.model.Institution;
import pe.edu.vallegrande.sigei.institution.domain.port.in.CreateInstitutionUseCase;
import pe.edu.vallegrande.sigei.institution.domain.port.in.FindInstitutionUseCase;
import pe.edu.vallegrande.sigei.institution.domain.port.out.InstitutionRepository;
import pe.edu.vallegrande.sigei.institution.domain.exception.DuplicateModularCodeException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class InstitutionService implements CreateInstitutionUseCase, FindInstitutionUseCase {

    private final InstitutionRepository repository;

    public InstitutionService(InstitutionRepository repository) {
        this.repository = repository;
    }

    @Override
    public Mono<Institution> create(Institution institution) {
        return repository.existsByModularCode(institution.getModularCode())
            .flatMap(exists -> {
                if (exists) {
                    return Mono.error(new DuplicateModularCodeException(
                        institution.getModularCode()));
                }
                return repository.save(institution);
            });
    }

    @Override
    public Mono<Institution> findById(String id) {
        return repository.findById(id);
    }

    @Override
    public Flux<Institution> findByStatus(InstitutionStatus status) {
        return repository.findByStatus(status);
    }

    @Override
    public Flux<Institution> findByDepartmentAndProvince(
            String department, String province) {
        return repository.findByAddressDepartmentAndAddressProvince(
            department, province);
    }

    @Override
    public Mono<Institution> findByModularCode(String modularCode) {
        return repository.findByModularCode(modularCode);
    }
}
```

---

## 🧩 CÓDIGO — Capa de Infraestructura

### Controller (Adaptador de Entrada)

```java
package pe.edu.vallegrande.sigei.institution.infrastructure.adapter.in.rest;

import pe.edu.vallegrande.sigei.institution.application.dto.request.CreateInstitutionRequest;
import pe.edu.vallegrande.sigei.institution.application.dto.response.InstitutionResponse;
import pe.edu.vallegrande.sigei.institution.application.mapper.InstitutionMapper;
import pe.edu.vallegrande.sigei.institution.domain.port.in.CreateInstitutionUseCase;
import pe.edu.vallegrande.sigei.institution.domain.port.in.FindInstitutionUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/institutions")
public class InstitutionController {

    private final CreateInstitutionUseCase createUseCase;
    private final FindInstitutionUseCase findUseCase;
    private final InstitutionMapper mapper;

    public InstitutionController(
            CreateInstitutionUseCase createUseCase,
            FindInstitutionUseCase findUseCase,
            InstitutionMapper mapper) {
        this.createUseCase = createUseCase;
        this.findUseCase = findUseCase;
        this.mapper = mapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<InstitutionResponse> create(@RequestBody CreateInstitutionRequest request) {
        return createUseCase.create(mapper.toDomain(request))
            .map(mapper::toResponse);
    }

    @GetMapping("/{id}")
    public Mono<InstitutionResponse> findById(@PathVariable String id) {
        return findUseCase.findById(id)
            .map(mapper::toResponse);
    }

    @GetMapping("/modular-code/{code}")
    public Mono<InstitutionResponse> findByModularCode(@PathVariable String code) {
        return findUseCase.findByModularCode(code)
            .map(mapper::toResponse);
    }

    @GetMapping
    public Flux<InstitutionResponse> findByStatus(
            @RequestParam(defaultValue = "ACTIVE") String status) {
        return findUseCase.findByStatus(InstitutionStatus.valueOf(status))
            .map(mapper::toResponse);
    }
}
```

### Persistence Adapter (Adaptador de Salida — MongoDB)

```java
package pe.edu.vallegrande.sigei.institution.infrastructure.adapter.out.persistence;

import pe.edu.vallegrande.sigei.institution.domain.model.Institution;
import pe.edu.vallegrande.sigei.institution.domain.port.out.InstitutionRepository;
import pe.edu.vallegrande.sigei.institution.infrastructure.adapter.out.persistence.document.InstitutionDocument;
import pe.edu.vallegrande.sigei.institution.infrastructure.adapter.out.persistence.mapper.InstitutionPersistenceMapper;
import pe.edu.vallegrande.sigei.institution.infrastructure.adapter.out.persistence.repository.MongoInstitutionRepository;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class InstitutionPersistenceAdapter implements InstitutionRepository {

    private final MongoInstitutionRepository mongoRepository;
    private final InstitutionPersistenceMapper mapper;

    public InstitutionPersistenceAdapter(
            MongoInstitutionRepository mongoRepository,
            InstitutionPersistenceMapper mapper) {
        this.mongoRepository = mongoRepository;
        this.mapper = mapper;
    }

    @Override
    public Mono<Institution> save(Institution institution) {
        InstitutionDocument doc = mapper.toDocument(institution);
        return mongoRepository.save(doc)
            .map(mapper::toDomain);
    }

    @Override
    public Mono<Institution> findById(String id) {
        return mongoRepository.findById(id)
            .map(mapper::toDomain);
    }

    @Override
    public Mono<Institution> findByModularCode(String modularCode) {
        return mongoRepository.findByModularCode(modularCode)
            .map(mapper::toDomain);
    }

    @Override
    public Flux<Institution> findByStatus(InstitutionStatus status) {
        return mongoRepository.findByStatus(status.name())
            .map(mapper::toDomain);
    }

    @Override
    public Mono<Boolean> existsByModularCode(String modularCode) {
        return mongoRepository.existsByModularCode(modularCode);
    }
}
```

### Document (Entidad de Persistencia — MongoDB)

```java
package pe.edu.vallegrande.sigei.institution.infrastructure.adapter.out.persistence.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

/**
 * Entidad de persistencia MongoDB.
 * Las anotaciones de persistencia SOLO van aquí, NUNCA en el dominio.
 */
@Document(collection = "institutions")
public class InstitutionDocument {

    @Id
    private String id;

    @Indexed(unique = true)
    private String modularCode;

    private String name;
    private AddressDocument address;
    private List<ContactMethodDocument> contacts;
    private List<ScheduleDocument> schedules;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Getters y Setters (o usar @Data de Lombok)
}
```

### GlobalExceptionHandler

```java
package pe.edu.vallegrande.sigei.institution.infrastructure.adapter.in.rest;

import pe.edu.vallegrande.sigei.institution.domain.exception.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import java.time.LocalDateTime;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InstitutionNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Mono<Map<String, Object>> handleNotFound(InstitutionNotFoundException ex) {
        return Mono.just(Map.of(
            "timestamp", LocalDateTime.now().toString(),
            "status", 404,
            "error", "Not Found",
            "message", ex.getMessage()
        ));
    }

    @ExceptionHandler(DuplicateModularCodeException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Mono<Map<String, Object>> handleDuplicate(DuplicateModularCodeException ex) {
        return Mono.just(Map.of(
            "timestamp", LocalDateTime.now().toString(),
            "status", 409,
            "error", "Conflict",
            "message", ex.getMessage()
        ));
    }

    @ExceptionHandler(BusinessRuleViolationException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public Mono<Map<String, Object>> handleBusinessRule(BusinessRuleViolationException ex) {
        return Mono.just(Map.of(
            "timestamp", LocalDateTime.now().toString(),
            "status", 422,
            "error", "Business Rule Violation",
            "message", ex.getMessage()
        ));
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Mono<Map<String, Object>> handleGeneral(Exception ex) {
        return Mono.just(Map.of(
            "timestamp", LocalDateTime.now().toString(),
            "status", 500,
            "error", "Internal Server Error",
            "message", "Error interno del servidor"
        ));
    }
}
```

---

## 📦 ESTRUCTURA POR MICROSERVICIO — Resumen

### vg-ms-students

```
domain/model/    → Student, PersonalInfo(record), Guardian(record),
                   HealthInfo(record), DevelopmentInfo(record)
                   enums/ → StudentStatus, GuardianRelation, Gender
domain/port/in/  → CreateStudentUseCase, FindStudentUseCase,
                   UpdateStudentUseCase, TransferStudentUseCase
domain/port/out/ → StudentRepository, InstitutionServiceClient
```

### vg-ms-users-management

```
domain/model/    → User, UserProfile(record)
                   enums/ → UserRole, UserStatus
domain/port/in/  → CreateUserUseCase, AuthenticateUserUseCase,
                   ManageRolesUseCase, FindUserUseCase
domain/port/out/ → UserRepository, InstitutionServiceClient
```

### vg-ms-academic-management

```
domain/model/    → Course, Competency, Capacity, Performance,
                   AcademicCatalog
                   enums/ → CourseStatus, EducationLevel
domain/port/in/  → ManageCourseUseCase, ManageCompetencyUseCase,
                   ManageCatalogUseCase
domain/port/out/ → CourseRepository, CompetencyRepository
```

### vg-ms-notes

```
domain/model/    → Grade, Evaluation, ReportCard
                   enums/ → GradeType, EvaluationStatus,
                   LiteralGrade (AD, A, B, C)
domain/port/in/  → RecordGradeUseCase, GenerateReportCardUseCase,
                   FindGradeUseCase
domain/port/out/ → GradeRepository, StudentServiceClient,
                   AcademicServiceClient
```

### vg-ms-assistance

```
domain/model/    → Attendance, AttendanceSummary
                   enums/ → AttendanceStatus (PRESENTE, TARDANZA,
                   FALTA_JUSTIFICADA, FALTA_INJUSTIFICADA)
domain/port/in/  → RecordAttendanceUseCase, GenerateSummaryUseCase
domain/port/out/ → AttendanceRepository, StudentServiceClient,
                   StorageClient
```

### vg-ms-disciplinary-management

```
domain/model/    → DisciplinaryIncident, BehaviorRecord,
                   CorrectiveAction
                   enums/ → IncidentSeverity, IncidentStatus
domain/port/in/  → RecordIncidentUseCase, ManageBehaviorUseCase
domain/port/out/ → IncidentRepository, StudentServiceClient,
                   NotificationClient
```

### vg-ms-psychology-welfare

```
domain/model/    → PsychologicalEvaluation, SpecialNeedsSupport,
                   TherapySession
                   enums/ → EvaluationResult, SupportType, SessionStatus
domain/port/in/  → ConductEvaluationUseCase, ManageSupportUseCase
domain/port/out/ → EvaluationRepository, SupportRepository,
                   StudentServiceClient
```

### vg-ms-teacher-assignment

```
domain/model/    → TeacherAssignment, Schedule
                   enums/ → AssignmentStatus, DayOfWeek
domain/port/in/  → AssignTeacherUseCase, FindAssignmentUseCase
domain/port/out/ → AssignmentRepository, UserServiceClient,
                   InstitutionServiceClient
```

### vg-ms-civic-dates

```
domain/model/    → CivicEvent, Calendar
                   enums/ → EventType, EventStatus
domain/port/in/  → ManageEventUseCase, ManageCalendarUseCase
domain/port/out/ → EventRepository, CalendarRepository
```

### vg-ms-notifications

```
domain/model/    → Notification, NotificationTemplate
                   enums/ → NotificationType (EMAIL, SMS, PUSH),
                   NotificationStatus
domain/port/in/  → SendNotificationUseCase, ManageTemplateUseCase
domain/port/out/ → NotificationRepository, EmailSender, SmsSender,
                   PushNotificationSender
```

---

## 📋 REGLAS DE DEPENDENCIA

```
┌──────────────────────────────────────────────────────┐
│                                                      │
│  domain/  ← NO depende de NADA externo               │
│    │        (sin Spring, sin JPA, sin MongoDB,        │
│    │         sin R2DBC, sin anotaciones)              │
│    │                                                  │
│    ▼                                                  │
│  application/  ← Depende SOLO del domain/             │
│    │             (puede usar @Service de Spring)      │
│    │                                                  │
│    ▼                                                  │
│  infrastructure/  ← Depende de domain/ y application/ │
│                     (aquí van TODAS las dependencias   │
│                      externas: Spring, DB, HTTP, etc.) │
│                                                      │
└──────────────────────────────────────────────────────┘

⛔ PROHIBIDO:
  domain/ → infrastructure/   (el dominio NO conoce la infraestructura)
  domain/ → application/      (el dominio NO conoce los casos de uso)
  application/ → infrastructure/  (la aplicación NO conoce los adaptadores)
```

---

## 📋 POM.XML ESTÁNDAR

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.3.0</version>
    </parent>

    <groupId>pe.edu.vallegrande</groupId>
    <artifactId>vg-ms-institution-management</artifactId>
    <version>1.0.0</version>
    <name>SIGEI - Institution Management</name>

    <properties>
        <java.version>17</java.version>
        <spring-cloud.version>2023.0.3</spring-cloud.version>
    </properties>

    <dependencies>
        <!-- WebFlux (Reactivo) -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-webflux</artifactId>
        </dependency>

        <!-- MongoDB Reactivo -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-mongodb-reactive</artifactId>
        </dependency>

        <!-- Eureka Client -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
        </dependency>

        <!-- RabbitMQ -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-amqp</artifactId>
        </dependency>

        <!-- Resilience4J -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-circuitbreaker-reactor-resilience4j</artifactId>
        </dependency>

        <!-- Validación -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- Actuator (Health Checks para Eureka) -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <!-- Lombok (opcional, reduce boilerplate) -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Test -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>io.projectreactor</groupId>
            <artifactId>reactor-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>
</project>
```

---

## 📋 APPLICATION.YML ESTÁNDAR (CON PERFILES)

```yaml
# application.yml — Configuración por defecto
spring:
  application:
    name: vg-ms-institution-management
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}

server:
  port: ${SERVER_PORT:9080}

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics

---
# application-dev.yml
spring:
  data:
    mongodb:
      uri: ${MONGODB_URI:mongodb://localhost:27017/sigei_institution}

eureka:
  client:
    service-url:
      defaultZone: ${EUREKA_URL:http://localhost:8761/eureka/}

logging:
  level:
    pe.edu.vallegrande: DEBUG

---
# application-prod.yml
spring:
  data:
    mongodb:
      uri: ${MONGODB_URI}

eureka:
  client:
    service-url:
      defaultZone: ${EUREKA_URL}

logging:
  level:
    pe.edu.vallegrande: INFO
```

> **⚠️ IMPORTANTE:** Nunca credenciales en texto plano. Usar variables de entorno o Config Server.

---

> **Siguiente:** Ver `06_ARQUITECTURA_FRONTEND_COMPLETA.md` para la estructura completa del frontend.
