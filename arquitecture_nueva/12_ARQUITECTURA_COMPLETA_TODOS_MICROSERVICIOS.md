# 12 — ARQUITECTURA DE CARPETAS COMPLETA — TODOS LOS MICROSERVICIOS

> **Fecha:** Febrero 2026
> **Sistema:** SIGEI — Sistema Integrado de Gestión Educativa Institucional
> **Contexto:** Colegios PRIVADOS de nivel INICIAL en Perú
> **Patrón:** Arquitectura Hexagonal (Ports & Adapters) + DDD
> **Stack:** Java 17, Spring Boot 3.5.x, WebFlux (Reactivo), PostgreSQL R2DBC, Keycloak

---

## 📋 ÍNDICE DE MICROSERVICIOS

| # | Microservicio | Puerto | Base de Datos | Paquete Base |
|---|--------------|--------|---------------|--------------|
| 1 | [Institution Management](#1-vg-ms-institution-management) | 9080 | PostgreSQL | `pe.edu.vallegrande.sigei.institution` |
| 2 | [Students](#2-vg-ms-students) | 9081 | PostgreSQL | `pe.edu.vallegrande.sigei.students` |
| 3 | [Enrollments](#3-vg-ms-enrollments) | 9082 | PostgreSQL | `pe.edu.vallegrande.sigei.enrollments` |
| 4 | [Users Management](#4-vg-ms-users-management) | 9083 | PostgreSQL | `pe.edu.vallegrande.sigei.users` |
| 5 | [Academic Management](#5-vg-ms-academic-management) | 9084 | PostgreSQL | `pe.edu.vallegrande.sigei.academic` |
| 6 | [Civic Dates](#6-vg-ms-civic-dates) | 9085 | PostgreSQL | `pe.edu.vallegrande.sigei.civicDates` |
| 7 | [Notes](#7-vg-ms-notes) | 9086 | PostgreSQL | `pe.edu.vallegrande.sigei.notes` |
| 8 | [Assistance](#8-vg-ms-assistance) | 9087 | PostgreSQL | `pe.edu.vallegrande.sigei.assistance` |
| 9 | [Disciplinary Management](#9-vg-ms-disciplinary-management) | 9088 | PostgreSQL | `pe.edu.vallegrande.sigei.disciplinary` |
| 10 | [Psychology & Welfare](#10-vg-ms-psychology-welfare) | 9090 | PostgreSQL | `pe.edu.vallegrande.sigei.psychology` |
| 11 | [Teacher Assignment](#11-vg-ms-teacher-assignment) | 9099 | PostgreSQL | `pe.edu.vallegrande.sigei.teacherAssignment` |
| 12 | [Notifications (WhatsApp)](#12-vg-ms-notifications) | 9091 | PostgreSQL | `pe.edu.vallegrande.sigei.notifications` |
| 13 | [API Gateway](#13-vg-ms-gateway) | 8080 | — | `pe.edu.vallegrande.sigei.gateway` |

> **IMPORTANTE:** Se unifica el paquete base a `pe.edu.vallegrande.sigei.<modulo>` para TODOS los MS.
> Todos migran a **PostgreSQL + R2DBC** (los 3 que usaban MongoDB: institution, students, users).

---

## 🧩 CONVENCIONES GLOBALES

### Estructura hexagonal estándar (aplica a todos)

```
src/main/java/pe/edu/vallegrande/sigei/<modulo>/
│
├── domain/                           ← CAPA DE DOMINIO (pura, sin frameworks)
│   ├── models/                       ← Entidades y agregados
│   │   ├── Xxx.java                 ← Entidad raíz (POJO puro, SIN @Table/@Document)
│   │   └── valueobjects/            ← Enumeraciones y Value Objects
│   │       ├── XxxStatus.java       ← Enum ACTIVE/INACTIVE
│   │       └── XxxRole.java         ← Otras enumeraciones
│   ├── ports/                        ← Puertos (interfaces con prefijo I)
│   │   ├── in/                      ← Puertos de ENTRADA (casos de uso)
│   │   │   ├── ICreateXxxUseCase.java
│   │   │   ├── IGetXxxUseCase.java
│   │   │   ├── IUpdateXxxUseCase.java
│   │   │   ├── IDeleteXxxUseCase.java
│   │   │   └── IRestoreXxxUseCase.java
│   │   └── out/                     ← Puertos de SALIDA (repositorios, eventos)
│   │       ├── IXxxRepository.java  ← Interfaz pura (NO Spring Data)
│   │       └── IXxxEventPublisher.java ← Interfaz para eventos RabbitMQ
│   ├── exceptions/                   ← Excepciones de dominio
│   │   ├── DomainException.java              ← Base abstracta
│   │   ├── NotFoundException.java            ← Base para 404
│   │   ├── ConflictException.java            ← Base para 409
│   │   ├── XxxNotFoundException.java         extends NotFoundException
│   │   └── DuplicateXxxException.java        extends ConflictException
│   │
│   └── services/                     ← Servicios de dominio (opcional, lógica pura)
│       └── XxxDomainService.java
│
├── application/                      ← CAPA DE APLICACIÓN (orquestación)
│   ├── usecases/                    ← Implementación de casos de uso (1 clase = 1 caso)
│   │   ├── CreateXxxUseCaseImpl.java
│   │   ├── GetXxxUseCaseImpl.java
│   │   ├── UpdateXxxUseCaseImpl.java
│   │   ├── DeleteXxxUseCaseImpl.java
│   │   └── RestoreXxxUseCaseImpl.java
│   ├── dto/                         ← DTOs de entrada/salida
│   │   ├── common/                  ← Wrappers de respuesta API
│   │   │   ├── ApiResponse.java
│   │   │   └── ErrorResponse.java
│   │   ├── request/
│   │   │   ├── CreateXxxRequest.java
│   │   │   └── UpdateXxxRequest.java
│   │   └── response/
│   │       ├── XxxResponse.java
│   │       └── XxxDetailResponse.java
│   ├── events/                      ← Eventos de integración (RabbitMQ)
│   │   ├── XxxCreatedEvent.java     [record]
│   │   ├── XxxUpdatedEvent.java     [record]
│   │   ├── XxxDeletedEvent.java     [record]
│   │   └── XxxRestoredEvent.java    [record]
│   └── mappers/                     ← Mappers DTO ↔ Domain
│       └── XxxMapper.java
│
├── infrastructure/                   ← CAPA DE INFRAESTRUCTURA (frameworks/tecnología)
│   ├── adapters/
│   │   ├── in/
│   │   │   └── rest/               ← Adaptadores de ENTRADA
│   │   │       ├── XxxRest.java    ← Controller REST
│   │   │       └── GlobalExceptionHandler.java
│   │   └── out/
│   │       ├── persistence/        ← Adaptador de persistencia
│   │       │   └── XxxRepositoryImpl.java  ← implements IXxxRepository
│   │       ├── external/           ← Clientes HTTP a otros MS
│   │       │   └── XxxClientImpl.java
│   │       └── messaging/          ← Adaptadores de mensajería (RabbitMQ)
│   │           └── XxxEventPublisherImpl.java ← implements IXxxEventPublisher
│   ├── config/                     ← Configuración de Spring
│   │   ├── R2dbcConfig.java
│   │   ├── SecurityConfig.java
│   │   ├── RabbitMQConfig.java
│   │   └── WebClientConfig.java
│   ├── persistence/                ← Entidades y repos de BD (separados del adapter)
│   │   ├── entities/
│   │   │   └── XxxEntity.java      ← @Table("xxx") — entidad R2DBC
│   │   └── repositories/
│   │       └── XxxR2dbcRepository.java ← extends ReactiveCrudRepository
│   └── security/                   ← Seguridad (opcional)
│       └── SecurityContextAdapter.java
│
└── XxxApplication.java              ← @SpringBootApplication
│
src/main/resources/
├── application.yml                  ← Configuración base
├── application-dev.yml              ← Perfil desarrollo
├── application-prod.yml             ← Perfil producción
└── db/migration/                    ← Migraciones Flyway
    ├── V1__create_xxx_table.sql
    └── V2__add_xxx_indexes.sql
│
src/test/java/pe/edu/vallegrande/sigei/<modulo>/
├── domain/models/                   ← Tests unitarios del dominio
│   └── XxxTest.java
├── application/usecases/            ← Tests de casos de uso
│   └── CreateXxxUseCaseImplTest.java
└── infrastructure/adapters/in/rest/ ← Tests de integración
    └── XxxRestTest.java
```

---

## 📂 ARQUITECTURA POR MICROSERVICIO

---

### 1. vg-ms-institution-management

> Gestión de instituciones educativas privadas de nivel inicial y sus aulas.
> **Puerto:** 9080 | **BD:** PostgreSQL schema `institution`

```
src/main/java/pe/edu/vallegrande/sigei/institution/
│
├── domain/
│   ├── models/
│   │   ├── Institution.java
│   │   │   ├── id: String
│   │   │   ├── modularCode: String              ← Código modular UGEL (7 dígitos)
│   │   │   ├── name: String
│   │   │   ├── address: Address                  ← Value Object
│   │   │   ├── contactMethods: List<ContactMethod> ← Value Object
│   │   │   ├── schedules: List<Schedule>         ← Value Object
│   │   │   ├── gradingType: String               ← "CUALITATIVA" (AD/A/B/C)
│   │   │   ├── directorId: String
│   │   │   ├── auxiliaryIds: List<String>
│   │   │   ├── ugel: String
│   │   │   ├── dre: String
│   │   │   ├── status: InstitutionStatus
│   │   │   ├── createdAt: LocalDateTime
│   │   │   └── updatedAt: LocalDateTime
│   │   │
│   │   ├── Classroom.java
│   │   │   ├── id: String
│   │   │   ├── institutionId: String
│   │   │   ├── classroomName: String             ← "Aula Estrellitas"
│   │   │   ├── classroomAge: String              ← "3 años", "4 años", "5 años"
│   │   │   ├── capacity: Integer
│   │   │   ├── color: String
│   │   │   ├── status: ClassroomStatus
│   │   │   ├── createdAt: LocalDateTime
│   │   │   └── updatedAt: LocalDateTime
│   │   │
│   │   ├── Address.java                          ← Value Object (record)
│   │   │   ├── department: String
│   │   │   ├── province: String
│   │   │   ├── district: String
│   │   │   ├── urbanization: String
│   │   │   └── reference: String
│   │   │
│   │   ├── ContactMethod.java                    ← Value Object (record)
│   │   │   ├── type: String                      ← "EMAIL", "PHONE", "WHATSAPP"
│   │   │   └── value: String
│   │   │
│   │   ├── Schedule.java                         ← Value Object (record)
│   │   │   ├── shift: String                     ← "MAÑANA", "TARDE"
│   │   │   ├── startTime: LocalTime
│   │   │   └── endTime: LocalTime
│   │   │
│   │   └── valueobjects/
│   │       ├── InstitutionStatus.java            ← ACTIVE, INACTIVE
│   │       └── ClassroomStatus.java              ← ACTIVE, INACTIVE
│   │
│   ├── ports/
│   │   ├── in/
│   │   │   ├── ICreateInstitutionUseCase.java
│   │   │   ├── IGetInstitutionUseCase.java
│   │   │   ├── IUpdateInstitutionUseCase.java
│   │   │   ├── IDeleteInstitutionUseCase.java
│   │   │   ├── IRestoreInstitutionUseCase.java
│   │   │   ├── ICreateClassroomUseCase.java
│   │   │   ├── IGetClassroomUseCase.java
│   │   │   ├── IUpdateClassroomUseCase.java
│   │   │   ├── IDeleteClassroomUseCase.java
│   │   │   └── IRestoreClassroomUseCase.java
│   │   └── out/
│   │       ├── IInstitutionRepository.java
│   │       ├── IClassroomRepository.java
│   │       └── IInstitutionEventPublisher.java
│   │
│   └── exceptions/
│       ├── DomainException.java
│       ├── NotFoundException.java
│       ├── ConflictException.java
│       ├── InstitutionNotFoundException.java
│       ├── ClassroomNotFoundException.java
│       ├── DuplicateModularCodeException.java
│       └── ClassroomCapacityException.java
│
├── application/
│   ├── usecases/
│   │   ├── CreateInstitutionUseCaseImpl.java
│   │   ├── GetInstitutionUseCaseImpl.java
│   │   ├── UpdateInstitutionUseCaseImpl.java
│   │   ├── DeleteInstitutionUseCaseImpl.java
│   │   ├── RestoreInstitutionUseCaseImpl.java
│   │   ├── CreateClassroomUseCaseImpl.java
│   │   ├── GetClassroomUseCaseImpl.java
│   │   ├── UpdateClassroomUseCaseImpl.java
│   │   ├── DeleteClassroomUseCaseImpl.java
│   │   └── RestoreClassroomUseCaseImpl.java
│   ├── dto/
│   │   ├── common/
│   │   │   ├── ApiResponse.java
│   │   │   └── ErrorResponse.java
│   │   ├── request/
│   │   │   ├── CreateInstitutionRequest.java
│   │   │   ├── UpdateInstitutionRequest.java
│   │   │   ├── CreateClassroomRequest.java
│   │   │   └── UpdateClassroomRequest.java
│   │   └── response/
│   │       ├── InstitutionResponse.java
│   │       ├── InstitutionDetailResponse.java    ← con classrooms incluidos
│   │       └── ClassroomResponse.java
│   ├── events/
│   │   ├── InstitutionCreatedEvent.java       [record] institutionId, name, modularCode
│   │   ├── InstitutionUpdatedEvent.java       [record] institutionId, fieldsChanged
│   │   ├── ClassroomCreatedEvent.java         [record] classroomId, institutionId, classroomName, ageGroup
│   │   └── AnnouncementCreatedEvent.java      [record] institutionId, title, message, targetAudience
│   └── mappers/
│       ├── InstitutionMapper.java
│       └── ClassroomMapper.java
│
├── infrastructure/
│   ├── adapters/
│   │   ├── in/rest/
│   │   │   ├── InstitutionRest.java
│   │   │   │   ├── GET    /api/v1/institutions
│   │   │   │   ├── GET    /api/v1/institutions/active
│   │   │   │   ├── GET    /api/v1/institutions/inactive
│   │   │   │   ├── GET    /api/v1/institutions/{id}
│   │   │   │   ├── GET    /api/v1/institutions/{id}/detail    ← con classrooms y users
│   │   │   │   ├── POST   /api/v1/institutions
│   │   │   │   ├── PUT    /api/v1/institutions/{id}
│   │   │   │   ├── DELETE /api/v1/institutions/{id}
│   │   │   │   └── PATCH  /api/v1/institutions/{id}/restore
│   │   │   │
│   │   │   ├── ClassroomRest.java
│   │   │   │   ├── GET    /api/v1/classrooms
│   │   │   │   ├── GET    /api/v1/classrooms/active
│   │   │   │   ├── GET    /api/v1/classrooms/inactive
│   │   │   │   ├── GET    /api/v1/classrooms/{id}
│   │   │   │   ├── GET    /api/v1/classrooms/institution/{institutionId}
│   │   │   │   ├── POST   /api/v1/classrooms
│   │   │   │   ├── PUT    /api/v1/classrooms/{id}
│   │   │   │   ├── DELETE /api/v1/classrooms/{id}
│   │   │   │   └── PATCH  /api/v1/classrooms/{id}/restore
│   │   │   │
│   │   │   └── GlobalExceptionHandler.java
│   │   │
│   │   └── out/
│   │       ├── persistence/
│   │       │   ├── InstitutionRepositoryImpl.java
│   │       │   └── ClassroomRepositoryImpl.java
│   │       └── messaging/
│   │           └── InstitutionEventPublisherImpl.java
│   │
│   ├── persistence/
│   │   ├── entities/
│   │   │   ├── InstitutionEntity.java            ← @Table("institutions")
│   │   │   └── ClassroomEntity.java              ← @Table("classrooms")
│   │   ├── mappers/
│   │   │   ├── InstitutionPersistenceMapper.java
│   │   │   └── ClassroomPersistenceMapper.java
│   │   └── repositories/
│   │       ├── InstitutionR2dbcRepository.java
│   │       └── ClassroomR2dbcRepository.java
│   │
│   └── config/
│       ├── R2dbcConfig.java
│       ├── SecurityConfig.java
│       ├── RabbitMQConfig.java
│       └── WebClientConfig.java
│
└── InstitutionApplication.java

src/main/resources/
├── application.yml
├── application-dev.yml
├── application-vpc.yml
└── db/migration/
    ├── V1__create_institutions_table.sql
    ├── V2__create_classrooms_table.sql
    └── V3__create_institution_indexes.sql
```

---

### 2. vg-ms-students

> Gestión de estudiantes de nivel inicial (3-5 años), información personal, de salud y apoderados.
> **Puerto:** 9081 | **BD:** PostgreSQL schema `students`

```
src/main/java/pe/edu/vallegrande/sigei/students/
│
├── domain/
│   ├── models/
│   │   ├── Student.java
│   │   │   ├── id: String
│   │   │   ├── cui: String                       ← Código Único de Identidad
│   │   │   ├── personalInfo: PersonalInfo        ← Value Object
│   │   │   ├── dateOfBirth: LocalDate
│   │   │   ├── address: String
│   │   │   ├── photoUrl: String
│   │   │   ├── institutionId: String
│   │   │   ├── classroomId: String
│   │   │   ├── developmentInfo: DevelopmentInfo  ← Value Object
│   │   │   ├── healthInfo: HealthInfo            ← Value Object
│   │   │   ├── status: StudentStatus
│   │   │   ├── createdAt: LocalDateTime
│   │   │   └── updatedAt: LocalDateTime
│   │   │
│   │   ├── Guardian.java
│   │   │   ├── id: String
│   │   │   ├── studentId: String
│   │   │   ├── firstName: String
│   │   │   ├── lastName: String
│   │   │   ├── relationship: String              ← "PADRE", "MADRE", "OTRO"
│   │   │   ├── documentType: String
│   │   │   ├── documentNumber: String
│   │   │   ├── phone: String
│   │   │   ├── email: String
│   │   │   ├── isEmergencyContact: boolean
│   │   │   └── contactInfo: ContactInfo          ← Value Object
│   │   │
│   │   ├── PersonalInfo.java                     ← Value Object (record)
│   │   │   ├── firstName: String
│   │   │   ├── lastName: String
│   │   │   ├── motherLastName: String
│   │   │   ├── documentType: String
│   │   │   ├── documentNumber: String
│   │   │   └── gender: String
│   │   │
│   │   ├── DevelopmentInfo.java                  ← Value Object (record)
│   │   │   ├── motorDevelopment: String
│   │   │   ├── languageDevelopment: String
│   │   │   ├── socialDevelopment: String
│   │   │   └── observations: String
│   │   │
│   │   ├── HealthInfo.java                       ← Value Object (record)
│   │   │   ├── bloodType: String
│   │   │   ├── allergies: List<String>
│   │   │   ├── medications: List<String>
│   │   │   ├── conditions: List<String>
│   │   │   └── emergencyNotes: String
│   │   │
│   │   ├── ContactInfo.java                      ← Value Object (record)
│   │   │   ├── phone: String
│   │   │   ├── whatsapp: String
│   │   │   └── email: String
│   │   │
│   │   └── valueobjects/
│   │       └── StudentStatus.java                ← ACTIVE, INACTIVE, TRANSFERRED
│   │
│   ├── ports/
│   │   ├── in/
│   │   │   ├── ICreateStudentUseCase.java
│   │   │   ├── IGetStudentUseCase.java
│   │   │   ├── IUpdateStudentUseCase.java
│   │   │   ├── IDeleteStudentUseCase.java
│   │   │   ├── IRestoreStudentUseCase.java
│   │   │   ├── ICreateGuardianUseCase.java
│   │   │   ├── IGetGuardianUseCase.java
│   │   │   ├── IUpdateGuardianUseCase.java
│   │   │   └── IDeleteGuardianUseCase.java
│   │   └── out/
│   │       ├── IStudentRepository.java
│   │       ├── IGuardianRepository.java
│   │       └── IStudentEventPublisher.java
│   │
│   └── exceptions/
│       ├── DomainException.java
│       ├── NotFoundException.java
│       ├── ConflictException.java
│       ├── StudentNotFoundException.java
│       ├── GuardianNotFoundException.java
│       └── DuplicateCuiException.java
│
├── application/
│   ├── usecases/
│   │   ├── CreateStudentUseCaseImpl.java
│   │   ├── GetStudentUseCaseImpl.java
│   │   ├── UpdateStudentUseCaseImpl.java
│   │   ├── DeleteStudentUseCaseImpl.java
│   │   ├── RestoreStudentUseCaseImpl.java
│   │   ├── CreateGuardianUseCaseImpl.java
│   │   ├── GetGuardianUseCaseImpl.java
│   │   ├── UpdateGuardianUseCaseImpl.java
│   │   └── DeleteGuardianUseCaseImpl.java
│   ├── dto/
│   │   ├── common/
│   │   │   ├── ApiResponse.java
│   │   │   └── ErrorResponse.java
│   │   ├── request/
│   │   │   ├── CreateStudentRequest.java
│   │   │   ├── UpdateStudentRequest.java
│   │   │   ├── CreateGuardianRequest.java
│   │   │   └── UpdateGuardianRequest.java
│   │   └── response/
│   │       ├── StudentResponse.java
│   │       ├── StudentDetailResponse.java        ← con guardians y salud
│   │       └── GuardianResponse.java
│   ├── events/
│   │   ├── StudentCreatedEvent.java           [record] studentId, institutionId, classroomId, fullName
│   │   ├── StudentUpdatedEvent.java           [record] studentId, fieldsChanged
│   │   └── GuardianAddedEvent.java            [record] guardianId, studentId, phone, relationship
│   └── mappers/
│       ├── StudentMapper.java
│       └── GuardianMapper.java
│
├── infrastructure/
│   ├── adapters/
│   │   ├── in/rest/
│   │   │   ├── StudentRest.java
│   │   │   │   ├── GET    /api/v1/students
│   │   │   │   ├── GET    /api/v1/students/active
│   │   │   │   ├── GET    /api/v1/students/{id}
│   │   │   │   ├── GET    /api/v1/students/{id}/detail
│   │   │   │   ├── GET    /api/v1/students/cui/{cui}
│   │   │   │   ├── GET    /api/v1/students/classroom/{classroomId}
│   │   │   │   ├── GET    /api/v1/students/institution/{institutionId}
│   │   │   │   ├── POST   /api/v1/students
│   │   │   │   ├── PUT    /api/v1/students/{id}
│   │   │   │   ├── DELETE /api/v1/students/{id}
│   │   │   │   └── PATCH  /api/v1/students/{id}/restore
│   │   │   │
│   │   │   ├── GuardianRest.java
│   │   │   │   ├── GET    /api/v1/guardians/student/{studentId}
│   │   │   │   ├── GET    /api/v1/guardians/{id}
│   │   │   │   ├── POST   /api/v1/guardians
│   │   │   │   ├── PUT    /api/v1/guardians/{id}
│   │   │   │   └── DELETE /api/v1/guardians/{id}
│   │   │   │
│   │   │   └── GlobalExceptionHandler.java
│   │   │
│   │   └── out/
│   │       ├── persistence/
│   │       │   ├── StudentRepositoryImpl.java
│   │       │   └── GuardianRepositoryImpl.java
│   │       ├── external/
│   │       │   ├── InstitutionClientImpl.java     ← WebClient → MS Institution
│   │       │   └── ClassroomClientImpl.java
│   │       └── messaging/
│   │           └── StudentEventPublisherImpl.java
│   │
│   ├── persistence/
│   │   ├── entities/
│   │   │   ├── StudentEntity.java                ← @Table("students")
│   │   │   └── GuardianEntity.java               ← @Table("guardians")
│   │   ├── mappers/
│   │   │   ├── StudentPersistenceMapper.java
│   │   │   └── GuardianPersistenceMapper.java
│   │   └── repositories/
│   │       ├── StudentR2dbcRepository.java
│   │       └── GuardianR2dbcRepository.java
│   │
│   └── config/
│       ├── R2dbcConfig.java
│       ├── SecurityConfig.java
│       ├── RabbitMQConfig.java
│       └── WebClientConfig.java
│
└── StudentsApplication.java

src/main/resources/
├── application.yml
├── application-dev.yml
├── application-vpc.yml
└── db/migration/
    ├── V1__create_students_table.sql
    ├── V2__create_guardians_table.sql
    └── V3__create_student_indexes.sql
```

---

### 3. vg-ms-enrollments

> Matrículas escolares, períodos académicos, validación de documentos.
> **Puerto:** 9082 | **BD:** PostgreSQL schema `enrollments`

```
src/main/java/pe/edu/vallegrande/sigei/enrollments/
│
├── domain/
│   ├── models/
│   │   ├── Enrollment.java
│   │   │   ├── id: String
│   │   │   ├── studentId: String
│   │   │   ├── institutionId: String
│   │   │   ├── classroomId: String
│   │   │   ├── academicPeriodId: String
│   │   │   ├── academicYear: String
│   │   │   ├── enrollmentType: EnrollmentType    ← NUEVO, REINGRESO, TRASLADO
│   │   │   ├── enrollmentStatus: EnrollmentStatus ← PENDING, ACTIVE, CANCELLED, COMPLETED
│   │   │   ├── ageGroup: String                  ← "3 años", "4 años", "5 años"
│   │   │   ├── shift: String
│   │   │   ├── section: String
│   │   │   ├── documents: Documents              ← Value Object (checklist docs)
│   │   │   ├── observations: String
│   │   │   ├── registeredByUserId: String
│   │   │   ├── enrollmentDate: LocalDateTime
│   │   │   ├── createdAt: LocalDateTime
│   │   │   └── updatedAt: LocalDateTime
│   │   │
│   │   ├── AcademicPeriod.java
│   │   │   ├── id: String
│   │   │   ├── institutionId: String
│   │   │   ├── academicYear: String
│   │   │   ├── periodName: String
│   │   │   ├── startDate: LocalDate
│   │   │   ├── endDate: LocalDate
│   │   │   ├── enrollmentPeriodStart: LocalDate
│   │   │   ├── enrollmentPeriodEnd: LocalDate
│   │   │   ├── allowLateEnrollment: boolean
│   │   │   ├── lateEnrollmentEndDate: LocalDate
│   │   │   ├── status: PeriodStatus
│   │   │   ├── createdAt: LocalDateTime
│   │   │   └── updatedAt: LocalDateTime
│   │   │
│   │   ├── Documents.java                        ← Value Object (record)
│   │   │   ├── birthCertificate: boolean
│   │   │   ├── studentDni: boolean
│   │   │   ├── guardianDni: boolean
│   │   │   ├── vaccinationCard: boolean
│   │   │   ├── disabilityCertificate: boolean
│   │   │   ├── utilityBill: boolean
│   │   │   ├── psychologicalReport: boolean
│   │   │   └── studentPhoto: boolean
│   │   │
│   │   └── valueobjects/
│   │       ├── EnrollmentStatus.java             ← PENDING, ACTIVE, CANCELLED, COMPLETED
│   │       ├── EnrollmentType.java               ← NUEVO, REINGRESO, TRASLADO
│   │       └── PeriodStatus.java                 ← PLANNING, OPEN, CLOSED
│   │
│   ├── ports/
│   │   ├── in/
│   │   │   ├── ICreateEnrollmentUseCase.java
│   │   │   ├── IGetEnrollmentUseCase.java
│   │   │   ├── IUpdateEnrollmentStatusUseCase.java
│   │   │   ├── IValidateEnrollmentUseCase.java
│   │   │   ├── IDeleteEnrollmentUseCase.java
│   │   │   ├── IRestoreEnrollmentUseCase.java
│   │   │   ├── ICreateAcademicPeriodUseCase.java
│   │   │   ├── IGetAcademicPeriodUseCase.java
│   │   │   ├── IUpdateAcademicPeriodUseCase.java
│   │   │   ├── IDeleteAcademicPeriodUseCase.java
│   │   │   └── IRestoreAcademicPeriodUseCase.java
│   │   └── out/
│   │       ├── IEnrollmentRepository.java
│   │       ├── IAcademicPeriodRepository.java
│   │       └── IEnrollmentEventPublisher.java
│   │
│   └── exceptions/
│       ├── DomainException.java
│       ├── NotFoundException.java
│       ├── ConflictException.java
│       ├── EnrollmentNotFoundException.java
│       ├── AcademicPeriodNotFoundException.java
│       ├── DuplicateEnrollmentException.java
│       └── EnrollmentPeriodClosedException.java
│
├── application/
│   ├── usecases/
│   │   ├── CreateEnrollmentUseCaseImpl.java
│   │   ├── GetEnrollmentUseCaseImpl.java
│   │   ├── UpdateEnrollmentUseCaseImpl.java
│   │   ├── DeleteEnrollmentUseCaseImpl.java
│   │   ├── RestoreEnrollmentUseCaseImpl.java
│   │   ├── ValidateEnrollmentUseCaseImpl.java
│   │   ├── CreateAcademicPeriodUseCaseImpl.java
│   │   ├── GetAcademicPeriodUseCaseImpl.java
│   │   ├── UpdateAcademicPeriodUseCaseImpl.java
│   │   ├── DeleteAcademicPeriodUseCaseImpl.java
│   │   └── RestoreAcademicPeriodUseCaseImpl.java
│   ├── dto/
│   │   ├── common/
│   │   │   ├── ApiResponse.java
│   │   │   └── ErrorResponse.java
│   │   ├── request/
│   │   │   ├── CreateEnrollmentRequest.java
│   │   │   ├── UpdateEnrollmentRequest.java
│   │   │   ├── CreateAcademicPeriodRequest.java
│   │   │   └── UpdateStatusRequest.java
│   │   └── response/
│   │       ├── EnrollmentResponse.java
│   │       ├── EnrollmentDetailResponse.java     ← con datos de student e institution
│   │       ├── AcademicPeriodResponse.java
│   │       └── EnrollmentStatisticsResponse.java
│   ├── events/
│   │   ├── EnrollmentConfirmedEvent.java      [record] enrollmentId, studentId, institutionId, classroomId, academicYear
│   │   ├── EnrollmentCancelledEvent.java      [record] enrollmentId, studentId, reason
│   │   └── AcademicPeriodOpenedEvent.java     [record] periodId, institutionId, academicYear, startDate, endDate
│   └── mappers/
│       ├── EnrollmentMapper.java
│       └── AcademicPeriodMapper.java
│
├── infrastructure/
│   ├── adapters/
│   │   ├── in/rest/
│   │   │   ├── EnrollmentRest.java
│   │   │   │   ├── POST   /api/v1/enrollments
│   │   │   │   ├── GET    /api/v1/enrollments
│   │   │   │   ├── GET    /api/v1/enrollments/{id}
│   │   │   │   ├── GET    /api/v1/enrollments/institution/{institutionId}
│   │   │   │   ├── GET    /api/v1/enrollments/student/{studentId}
│   │   │   │   ├── GET    /api/v1/enrollments/active
│   │   │   │   ├── GET    /api/v1/enrollments/pending
│   │   │   │   ├── GET    /api/v1/enrollments/statistics/{institutionId}
│   │   │   │   ├── PUT    /api/v1/enrollments/{id}
│   │   │   │   ├── PATCH  /api/v1/enrollments/{id}/status
│   │   │   │   ├── DELETE /api/v1/enrollments/{id}
│   │   │   │   └── PATCH  /api/v1/enrollments/{id}/restore
│   │   │   │
│   │   │   ├── AcademicPeriodRest.java
│   │   │   │   ├── POST   /api/v1/academic-periods
│   │   │   │   ├── GET    /api/v1/academic-periods
│   │   │   │   ├── GET    /api/v1/academic-periods/{id}
│   │   │   │   ├── GET    /api/v1/academic-periods/institution/{institutionId}
│   │   │   │   ├── GET    /api/v1/academic-periods/year/{academicYear}
│   │   │   │   ├── PUT    /api/v1/academic-periods/{id}
│   │   │   │   ├── DELETE /api/v1/academic-periods/{id}
│   │   │   │   └── PATCH  /api/v1/academic-periods/{id}/restore
│   │   │   │
│   │   │   └── GlobalExceptionHandler.java
│   │   │
│   │   └── out/
│   │       ├── persistence/
│   │       │   ├── EnrollmentRepositoryImpl.java
│   │       │   └── AcademicPeriodRepositoryImpl.java
│   │       ├── external/
│   │       │   ├── InstitutionClientImpl.java
│   │       │   ├── StudentClientImpl.java
│   │       │   └── ClassroomClientImpl.java
│   │       └── messaging/
│   │           └── EnrollmentEventPublisherImpl.java
│   │
│   ├── persistence/
│   │   ├── entities/
│   │   │   ├── EnrollmentEntity.java             ← @Table("enrollments")
│   │   │   └── AcademicPeriodEntity.java         ← @Table("academic_periods")
│   │   ├── mappers/
│   │   │   ├── EnrollmentPersistenceMapper.java
│   │   │   └── AcademicPeriodPersistenceMapper.java
│   │   └── repositories/
│   │       ├── EnrollmentR2dbcRepository.java
│   │       └── AcademicPeriodR2dbcRepository.java
│   │
│   └── config/
│       ├── R2dbcConfig.java
│       ├── SecurityConfig.java
│       ├── RabbitMQConfig.java
│       └── WebClientConfig.java
│
└── EnrollmentsApplication.java

src/main/resources/
├── application.yml
├── application-dev.yml
├── application-vpc.yml
└── db/migration/
    ├── V1__create_enrollments_table.sql
    ├── V2__create_academic_periods_table.sql
    └── V3__create_enrollment_indexes.sql
```

---

### 4. vg-ms-users-management

> Gestión de usuarios del sistema (directores, docentes, auxiliares, psicólogos, apoderados, secretarias).
> **Puerto:** 9083 | **BD:** PostgreSQL schema `users_management`

```
src/main/java/pe/edu/vallegrande/sigei/users/
│
├── domain/
│   ├── models/
│   │   ├── User.java
│   │   │   ├── id: String
│   │   │   ├── institutionId: String
│   │   │   ├── firstName: String
│   │   │   ├── lastName: String
│   │   │   ├── documentType: String
│   │   │   ├── documentNumber: String
│   │   │   ├── phone: String
│   │   │   ├── address: String
│   │   │   ├── email: String
│   │   │   ├── userName: String
│   │   │   ├── role: UserRole
│   │   │   ├── status: UserStatus
│   │   │   ├── createdAt: LocalDateTime
│   │   │   └── updatedAt: LocalDateTime
│   │   │
│   │   └── valueobjects/
│   │       ├── UserRole.java                     ← DIRECTOR, SUBDIRECTOR, DOCENTE,
│   │       │                                        AUXILIAR, PSICOLOGO, SECRETARIA, APODERADO
│   │       └── UserStatus.java                   ← ACTIVE, INACTIVE
│   │
│   ├── ports/
│   │   ├── in/
│   │   │   ├── ICreateUserUseCase.java
│   │   │   ├── IGetUserUseCase.java
│   │   │   ├── IUpdateUserUseCase.java
│   │   │   ├── IDeleteUserUseCase.java
│   │   │   └── IRestoreUserUseCase.java
│   │   └── out/
│   │       ├── IUserRepository.java
│   │       └── IUserEventPublisher.java
│   │
│   └── exceptions/
│       ├── DomainException.java
│       ├── NotFoundException.java
│       ├── ConflictException.java
│       ├── UserNotFoundException.java
│       └── DuplicateDocumentNumberException.java
│
├── application/
│   ├── usecases/
│   │   ├── CreateUserUseCaseImpl.java
│   │   ├── GetUserUseCaseImpl.java
│   │   ├── UpdateUserUseCaseImpl.java
│   │   ├── DeleteUserUseCaseImpl.java
│   │   └── RestoreUserUseCaseImpl.java
│   ├── dto/
│   │   ├── common/
│   │   │   ├── ApiResponse.java
│   │   │   └── ErrorResponse.java
│   │   ├── request/
│   │   │   ├── CreateUserRequest.java
│   │   │   └── UpdateUserRequest.java
│   │   └── response/
│   │       └── UserResponse.java
│   ├── events/
│   │   ├── UserCreatedEvent.java              [record] userId, institutionId, role, fullName
│   │   └── UserDeactivatedEvent.java          [record] userId, institutionId, reason
│   └── mappers/
│       └── UserMapper.java
│
├── infrastructure/
│   ├── adapters/
│   │   ├── in/rest/
│   │   │   ├── UserRest.java
│   │   │   │   ├── GET    /api/v1/users
│   │   │   │   ├── GET    /api/v1/users/{id}
│   │   │   │   ├── GET    /api/v1/users/status/{status}
│   │   │   │   ├── GET    /api/v1/users/role/{role}/status/{status}
│   │   │   │   ├── GET    /api/v1/users/institution/{institutionId}
│   │   │   │   ├── POST   /api/v1/users
│   │   │   │   ├── PUT    /api/v1/users/{id}
│   │   │   │   ├── DELETE /api/v1/users/{id}
│   │   │   │   └── PATCH  /api/v1/users/{id}/restore
│   │   │   │
│   │   │   └── GlobalExceptionHandler.java
│   │   │
│   │   └── out/
│   │       ├── persistence/
│   │       │   └── UserRepositoryImpl.java
│   │       ├── external/
│   │       │   └── InstitutionClientImpl.java
│   │       └── messaging/
│   │           └── UserEventPublisherImpl.java
│   │
│   ├── persistence/
│   │   ├── entities/
│   │   │   └── UserEntity.java                   ← @Table("users")
│   │   ├── mappers/
│   │   │   └── UserPersistenceMapper.java
│   │   └── repositories/
│   │       └── UserR2dbcRepository.java
│   │
│   └── config/
│       ├── R2dbcConfig.java
│       ├── SecurityConfig.java
│       ├── RabbitMQConfig.java
│       └── WebClientConfig.java
│
└── UsersApplication.java

src/main/resources/
├── application.yml
├── application-dev.yml
├── application-vpc.yml
└── db/migration/
    ├── V1__create_users_table.sql
    └── V2__create_user_indexes.sql
```

---

### 5. vg-ms-academic-management

> Catálogo curricular: cursos, competencias, capacidades y desempeños para nivel inicial.
> **Puerto:** 9084 | **BD:** PostgreSQL schema `academic`

```
src/main/java/pe/edu/vallegrande/sigei/academic/
│
├── domain/
│   ├── models/
│   │   ├── Course.java
│   │   │   ├── id: UUID
│   │   │   ├── institutionId: String
│   │   │   ├── code: String
│   │   │   ├── name: String
│   │   │   ├── areaCurricular: String            ← "Personal Social", "Comunicación", etc.
│   │   │   ├── ageLevel: String                  ← "3 años", "4 años", "5 años"
│   │   │   ├── description: String
│   │   │   ├── status: String
│   │   │   ├── createdAt: LocalDateTime
│   │   │   └── updatedAt: LocalDateTime
│   │   │
│   │   ├── Competency.java
│   │   │   ├── id: UUID
│   │   │   ├── courseId: UUID
│   │   │   ├── institutionId: String
│   │   │   ├── code: String
│   │   │   ├── name: String
│   │   │   ├── description: String
│   │   │   ├── orderIndex: Integer
│   │   │   ├── status: String
│   │   │   ├── createdAt: LocalDateTime
│   │   │   └── updatedAt: LocalDateTime
│   │   │
│   │   ├── Capacity.java
│   │   │   ├── id: UUID
│   │   │   ├── competencyId: UUID
│   │   │   ├── institutionId: String
│   │   │   ├── code: String
│   │   │   ├── name: String
│   │   │   ├── description: String
│   │   │   ├── orderIndex: Integer
│   │   │   ├── status: String
│   │   │   ├── createdAt: LocalDateTime
│   │   │   └── updatedAt: LocalDateTime
│   │   │
│   │   ├── Performance.java
│   │   │   ├── id: UUID
│   │   │   ├── capacityId: UUID
│   │   │   ├── institutionId: String
│   │   │   ├── code: String
│   │   │   ├── description: String
│   │   │   ├── ageLevel: String
│   │   │   ├── orderIndex: Integer
│   │   │   ├── status: String
│   │   │   ├── createdAt: LocalDateTime
│   │   │   └── updatedAt: LocalDateTime
│   │   │
│   │   └── CatalogRegistration.java              ← Agregado para registro masivo
│   │       ├── institutionId: String
│   │       ├── course: Course
│   │       └── competencies: List<CompetencyWithCapacities>
│   │
│   ├── ports/
│   │   ├── in/
│   │   │   ├── ICreateCourseUseCase.java
│   │   │   ├── IGetCourseUseCase.java
│   │   │   ├── IUpdateCourseUseCase.java
│   │   │   ├── IDeleteCourseUseCase.java
│   │   │   ├── IRestoreCourseUseCase.java
│   │   │   ├── ICreateCompetencyUseCase.java
│   │   │   ├── IGetCompetencyUseCase.java
│   │   │   ├── IUpdateCompetencyUseCase.java
│   │   │   ├── IDeleteCompetencyUseCase.java
│   │   │   ├── IRestoreCompetencyUseCase.java
│   │   │   ├── ICreateCapacityUseCase.java
│   │   │   ├── IGetCapacityUseCase.java
│   │   │   ├── IUpdateCapacityUseCase.java
│   │   │   ├── IDeleteCapacityUseCase.java
│   │   │   ├── IRestoreCapacityUseCase.java
│   │   │   ├── ICreatePerformanceUseCase.java
│   │   │   ├── IGetPerformanceUseCase.java
│   │   │   ├── IUpdatePerformanceUseCase.java
│   │   │   ├── IDeletePerformanceUseCase.java
│   │   │   ├── IRestorePerformanceUseCase.java
│   │   │   └── IRegisterCatalogUseCase.java
│   │   └── out/
│   │       ├── ICourseRepository.java
│   │       ├── ICompetencyRepository.java
│   │       ├── ICapacityRepository.java
│   │       └── IPerformanceRepository.java
│   │
│   └── exceptions/
│       ├── DomainException.java
│       ├── NotFoundException.java
│       ├── ConflictException.java
│       ├── CourseNotFoundException.java
│       ├── CompetencyNotFoundException.java
│       └── DuplicateCourseCodeException.java
│
├── application/
│   ├── usecases/
│   │   ├── CreateCourseUseCaseImpl.java
│   │   ├── GetCourseUseCaseImpl.java
│   │   ├── UpdateCourseUseCaseImpl.java
│   │   ├── DeleteCourseUseCaseImpl.java
│   │   ├── RestoreCourseUseCaseImpl.java
│   │   ├── CreateCompetencyUseCaseImpl.java
│   │   ├── GetCompetencyUseCaseImpl.java
│   │   ├── UpdateCompetencyUseCaseImpl.java
│   │   ├── DeleteCompetencyUseCaseImpl.java
│   │   ├── RestoreCompetencyUseCaseImpl.java
│   │   ├── CreateCapacityUseCaseImpl.java
│   │   ├── GetCapacityUseCaseImpl.java
│   │   ├── UpdateCapacityUseCaseImpl.java
│   │   ├── DeleteCapacityUseCaseImpl.java
│   │   ├── RestoreCapacityUseCaseImpl.java
│   │   ├── CreatePerformanceUseCaseImpl.java
│   │   ├── GetPerformanceUseCaseImpl.java
│   │   ├── UpdatePerformanceUseCaseImpl.java
│   │   ├── DeletePerformanceUseCaseImpl.java
│   │   ├── RestorePerformanceUseCaseImpl.java
│   │   └── RegisterCatalogUseCaseImpl.java
│   ├── dto/
│   │   ├── common/
│   │   │   ├── ApiResponse.java
│   │   │   └── ErrorResponse.java
│   │   ├── request/
│   │   │   ├── CreateCourseRequest.java
│   │   │   ├── UpdateCourseRequest.java
│   │   │   ├── CreateCompetencyRequest.java
│   │   │   ├── CreateCapacityRequest.java
│   │   │   ├── CreatePerformanceRequest.java
│   │   │   └── CatalogRegistrationRequest.java
│   │   └── response/
│   │       ├── CourseResponse.java
│   │       ├── CompetencyResponse.java
│   │       ├── CapacityResponse.java
│   │       ├── PerformanceResponse.java
│   │       └── CatalogDetailResponse.java
│   ├── events/
│   │   ├── CatalogRegisteredEvent.java        [record] institutionId, courseId, courseName, competencyCount
│   │   └── CatalogUpdatedEvent.java           [record] institutionId, courseId, changes
│   └── mappers/
│       ├── CourseMapper.java
│       ├── CompetencyMapper.java
│       ├── CapacityMapper.java
│       └── PerformanceMapper.java
│
├── infrastructure/
│   ├── adapters/
│   │   ├── in/rest/
│   │   │   ├── CourseRest.java
│   │   │   │   ├── GET    /api/v1/courses
│   │   │   │   ├── GET    /api/v1/courses/{id}
│   │   │   │   ├── GET    /api/v1/courses/institution/{institutionId}
│   │   │   │   ├── POST   /api/v1/courses
│   │   │   │   ├── PUT    /api/v1/courses/{id}
│   │   │   │   ├── DELETE /api/v1/courses/{id}
│   │   │   │   └── PATCH  /api/v1/courses/{id}/restore
│   │   │   │
│   │   │   ├── CompetencyRest.java
│   │   │   │   ├── GET    /api/v1/competencies
│   │   │   │   ├── GET    /api/v1/competencies/{id}
│   │   │   │   ├── GET    /api/v1/competencies/course/{courseId}
│   │   │   │   ├── POST   /api/v1/competencies
│   │   │   │   ├── PUT    /api/v1/competencies/{id}
│   │   │   │   ├── DELETE /api/v1/competencies/{id}
│   │   │   │   └── PATCH  /api/v1/competencies/{id}/restore
│   │   │   │
│   │   │   ├── CapacityRest.java
│   │   │   │   ├── GET    /api/v1/capacities
│   │   │   │   ├── GET    /api/v1/capacities/{id}
│   │   │   │   ├── GET    /api/v1/capacities/competency/{competencyId}
│   │   │   │   ├── POST   /api/v1/capacities
│   │   │   │   ├── PUT    /api/v1/capacities/{id}
│   │   │   │   ├── DELETE /api/v1/capacities/{id}
│   │   │   │   └── PATCH  /api/v1/capacities/{id}/restore
│   │   │   │
│   │   │   ├── PerformanceRest.java
│   │   │   │   ├── GET    /api/v1/performances
│   │   │   │   ├── GET    /api/v1/performances/{id}
│   │   │   │   ├── GET    /api/v1/performances/capacity/{capacityId}
│   │   │   │   ├── POST   /api/v1/performances
│   │   │   │   ├── PUT    /api/v1/performances/{id}
│   │   │   │   ├── DELETE /api/v1/performances/{id}
│   │   │   │   └── PATCH  /api/v1/performances/{id}/restore
│   │   │   │
│   │   │   ├── CatalogRest.java
│   │   │   │   ├── POST   /api/v1/catalog/register
│   │   │   │   ├── PUT    /api/v1/catalog/update
│   │   │   │   ├── GET    /api/v1/catalog/{institutionId}
│   │   │   │   ├── PATCH  /api/v1/catalog/{courseId}/deactivate
│   │   │   │   └── PATCH  /api/v1/catalog/{courseId}/activate
│   │   │   │
│   │   │   └── GlobalExceptionHandler.java
│   │   │
│   │   └── out/
│   │       ├── persistence/
│   │       │   ├── CourseRepositoryImpl.java
│   │       │   ├── CompetencyRepositoryImpl.java
│   │       │   ├── CapacityRepositoryImpl.java
│   │       │   └── PerformanceRepositoryImpl.java
│   │       └── external/
│   │           └── InstitutionClientImpl.java
│   │
│   ├── persistence/
│   │   ├── entities/
│   │   │   ├── CourseEntity.java                 ← @Table("courses")
│   │   │   ├── CompetencyEntity.java             ← @Table("competencies")
│   │   │   ├── CapacityEntity.java               ← @Table("capacities")
│   │   │   └── PerformanceEntity.java            ← @Table("performances")
│   │   ├── mappers/
│   │   │   ├── CoursePersistenceMapper.java
│   │   │   ├── CompetencyPersistenceMapper.java
│   │   │   ├── CapacityPersistenceMapper.java
│   │   │   └── PerformancePersistenceMapper.java
│   │   └── repositories/
│   │       ├── CourseR2dbcRepository.java
│   │       ├── CompetencyR2dbcRepository.java
│   │       ├── CapacityR2dbcRepository.java
│   │       └── PerformanceR2dbcRepository.java
│   │
│   └── config/
│       ├── R2dbcConfig.java
│       ├── SecurityConfig.java
│       └── WebClientConfig.java
│
└── AcademicApplication.java

src/main/resources/
├── application.yml
├── application-dev.yml
├── application-vpc.yml
└── db/migration/
    ├── V1__create_courses_table.sql
    ├── V2__create_competencies_table.sql
    ├── V3__create_capacities_table.sql
    └── V4__create_performances_table.sql
```

---

### 6. vg-ms-civic-dates

> Calendario cívico escolar, eventos, feriados y calendario académico.
> **Puerto:** 9085 | **BD:** PostgreSQL schema `civic_dates`

```
src/main/java/pe/edu/vallegrande/sigei/civicDates/
│
├── domain/
│   ├── models/
│   │   ├── Event.java
│   │   │   ├── id: Long
│   │   │   ├── institutionId: String
│   │   │   ├── title: String
│   │   │   ├── description: String
│   │   │   ├── startDate: LocalDate
│   │   │   ├── endDate: LocalDate
│   │   │   ├── eventType: String                 ← "CIVICO", "CULTURAL", "RELIGIOSO", "INSTITUCIONAL"
│   │   │   ├── isHoliday: Boolean
│   │   │   ├── isRecurring: Boolean
│   │   │   ├── isNational: Boolean
│   │   │   ├── affectsClasses: Boolean
│   │   │   ├── createdBy: String
│   │   │   ├── status: EventStatus
│   │   │   ├── createdAt: LocalDateTime
│   │   │   └── updatedAt: LocalDateTime
│   │   │
│   │   ├── AcademicCalendar.java
│   │   │   ├── id: Integer
│   │   │   ├── institutionId: String
│   │   │   ├── academicYear: Integer
│   │   │   ├── startDate: LocalDate
│   │   │   ├── endDate: LocalDate
│   │   │   ├── createdAt: LocalDateTime
│   │   │   └── updatedAt: LocalDateTime
│   │   │
│   │   ├── EventCalendar.java                    ← Relación N:N evento-calendario
│   │   │   ├── id: Integer
│   │   │   ├── calendarId: Integer
│   │   │   ├── eventId: Long
│   │   │   └── createdAt: LocalDateTime
│   │   │
│   │   └── valueobjects/
│   │       └── EventStatus.java                  ← ACTIVE, INACTIVE
│   │
│   ├── ports/
│   │   ├── in/
│   │   │   ├── ICreateEventUseCase.java
│   │   │   ├── IGetEventUseCase.java
│   │   │   ├── IUpdateEventUseCase.java
│   │   │   ├── IDeleteEventUseCase.java
│   │   │   ├── IRestoreEventUseCase.java
│   │   │   ├── ICreateCalendarUseCase.java
│   │   │   ├── IGetCalendarUseCase.java
│   │   │   └── IUpdateCalendarUseCase.java
│   │   └── out/
│   │       ├── IEventRepository.java
│   │       ├── IAcademicCalendarRepository.java
│   │       └── IEventCalendarRepository.java
│   │
│   └── exceptions/
│       ├── DomainException.java
│       ├── NotFoundException.java
│       ├── ConflictException.java
│       ├── EventNotFoundException.java
│       └── CalendarNotFoundException.java
│
├── application/
│   ├── usecases/
│   │   ├── CreateEventUseCaseImpl.java
│   │   ├── GetEventUseCaseImpl.java
│   │   ├── UpdateEventUseCaseImpl.java
│   │   ├── DeleteEventUseCaseImpl.java
│   │   ├── RestoreEventUseCaseImpl.java
│   │   ├── CreateCalendarUseCaseImpl.java
│   │   ├── GetCalendarUseCaseImpl.java
│   │   └── UpdateCalendarUseCaseImpl.java
│   ├── dto/
│   │   ├── common/
│   │   │   ├── ApiResponse.java
│   │   │   └── ErrorResponse.java
│   │   ├── request/
│   │   │   ├── CreateEventRequest.java
│   │   │   ├── UpdateEventRequest.java
│   │   │   ├── CreateCalendarRequest.java
│   │   │   └── AddEventsToCalendarRequest.java
│   │   └── response/
│   │       ├── EventResponse.java
│   │       ├── CalendarResponse.java
│   │       └── CalendarWithEventsResponse.java
│   ├── events/
│   │   ├── CivicEventCreatedEvent.java        [record] eventId, institutionId, title, startDate, isHoliday
│   │   └── EventReminderEvent.java            [record] eventId, institutionId, title, daysUntilEvent
│   └── mappers/
│       ├── EventMapper.java
│       └── CalendarMapper.java
│
├── infrastructure/
│   ├── adapters/
│   │   ├── in/rest/
│   │   │   ├── EventRest.java
│   │   │   │   ├── GET    /api/v1/events
│   │   │   │   ├── GET    /api/v1/events/{id}
│   │   │   │   ├── GET    /api/v1/events/institution/{institutionId}
│   │   │   │   ├── GET    /api/v1/events/inactive
│   │   │   │   ├── POST   /api/v1/events
│   │   │   │   ├── PUT    /api/v1/events/{id}
│   │   │   │   ├── DELETE /api/v1/events/{id}
│   │   │   │   └── PATCH  /api/v1/events/{id}/restore
│   │   │   │
│   │   │   ├── CalendarRest.java
│   │   │   │   ├── GET    /api/v1/calendars
│   │   │   │   ├── GET    /api/v1/calendars/{id}
│   │   │   │   ├── GET    /api/v1/calendars/institution/{institutionId}
│   │   │   │   ├── GET    /api/v1/calendars/{id}/events
│   │   │   │   ├── POST   /api/v1/calendars
│   │   │   │   ├── POST   /api/v1/calendars/import
│   │   │   │   └── POST   /api/v1/calendars/{id}/events
│   │   │   │
│   │   │   └── GlobalExceptionHandler.java
│   │   │
│   │   └── out/
│   │       ├── persistence/
│   │       │   ├── EventRepositoryImpl.java
│   │       │   └── CalendarRepositoryImpl.java
│   │       └── external/
│   │           └── InstitutionClientImpl.java
│   │
│   ├── persistence/
│   │   ├── entities/
│   │   │   ├── EventEntity.java                  ← @Table("events")
│   │   │   ├── AcademicCalendarEntity.java       ← @Table("academic_calendar")
│   │   │   └── EventCalendarEntity.java          ← @Table("event_calendar")
│   │   ├── mappers/
│   │   │   ├── EventPersistenceMapper.java
│   │   │   └── CalendarPersistenceMapper.java
│   │   └── repositories/
│   │       ├── EventR2dbcRepository.java
│   │       ├── CalendarR2dbcRepository.java
│   │       └── EventCalendarR2dbcRepository.java
│   │
│   └── config/
│       ├── R2dbcConfig.java
│       ├── SecurityConfig.java
│       └── WebClientConfig.java
│
└── CivicDatesApplication.java

src/main/resources/
├── application.yml
├── application-dev.yml
├── application-vpc.yml
└── db/migration/
    ├── V1__create_events_table.sql
    ├── V2__create_academic_calendar_table.sql
    └── V3__create_event_calendar_table.sql
```

---

### 7. vg-ms-notes

> Evaluaciones de estudiantes, competencias evaluadas y libretas de notas.
> **Puerto:** 9086 | **BD:** PostgreSQL schema `notes`

```
src/main/java/pe/edu/vallegrande/sigei/notes/
│
├── domain/
│   ├── models/
│   │   ├── StudentEvaluation.java
│   │   │   ├── id: UUID
│   │   │   ├── studentId: String
│   │   │   ├── enrollmentId: String
│   │   │   ├── classroomId: String
│   │   │   ├── institutionId: String
│   │   │   ├── courseId: UUID
│   │   │   ├── competencyId: UUID
│   │   │   ├── academicYear: Integer
│   │   │   ├── achievementLevel: String          ← "AD", "A", "B", "C"
│   │   │   ├── description: String
│   │   │   ├── evaluatedBy: UUID
│   │   │   ├── evaluationDate: LocalDate
│   │   │   ├── observations: String
│   │   │   ├── activityContext: String
│   │   │   ├── evidenceUrls: List<String>
│   │   │   ├── createdAt: LocalDateTime
│   │   │   └── updatedAt: LocalDateTime
│   │   │
│   │   ├── ReportCard.java
│   │   │   ├── id: UUID
│   │   │   ├── studentId: String
│   │   │   ├── enrollmentId: UUID
│   │   │   ├── classroomId: UUID
│   │   │   ├── institutionId: String
│   │   │   ├── academicYear: Integer
│   │   │   ├── academicPeriodId: UUID
│   │   │   ├── periodType: String
│   │   │   ├── periodNumber: Integer
│   │   │   ├── attendancePercentage: BigDecimal
│   │   │   ├── behaviorLevel: String
│   │   │   ├── generalObservations: String
│   │   │   ├── teacherComments: String
│   │   │   ├── overallSummary: String
│   │   │   ├── recommendations: String
│   │   │   ├── status: String                    ← "DRAFT", "APPROVED", "PUBLISHED"
│   │   │   ├── generatedBy: UUID
│   │   │   ├── generatedAt: LocalDateTime
│   │   │   ├── approvedBy: UUID
│   │   │   └── approvedAt: LocalDateTime
│   │   │
│   │   └── valueobjects/
│   │       ├── AchievementLevel.java             ← AD, A, B, C
│   │       └── ReportCardStatus.java             ← DRAFT, APPROVED, PUBLISHED
│   │
│   ├── ports/
│   │   ├── in/
│   │   │   ├── ICreateEvaluationUseCase.java
│   │   │   ├── IGetEvaluationUseCase.java
│   │   │   ├── IUpdateEvaluationUseCase.java
│   │   │   ├── IDeleteEvaluationUseCase.java
│   │   │   ├── ICreateReportCardUseCase.java
│   │   │   ├── IGetReportCardUseCase.java
│   │   │   ├── IUpdateReportCardUseCase.java
│   │   │   └── IDeleteReportCardUseCase.java
│   │   └── out/
│   │       ├── IStudentEvaluationRepository.java
│   │       ├── IReportCardRepository.java
│   │       └── INotesEventPublisher.java
│   │
│   └── exceptions/
│       ├── DomainException.java
│       ├── NotFoundException.java
│       ├── ConflictException.java
│       ├── EvaluationNotFoundException.java
│       ├── ReportCardNotFoundException.java
│       └── GradeOutOfRangeException.java
│
├── application/
│   ├── usecases/
│   │   ├── CreateEvaluationUseCaseImpl.java
│   │   ├── GetEvaluationUseCaseImpl.java
│   │   ├── UpdateEvaluationUseCaseImpl.java
│   │   ├── DeleteEvaluationUseCaseImpl.java
│   │   ├── CreateReportCardUseCaseImpl.java
│   │   ├── GetReportCardUseCaseImpl.java
│   │   ├── UpdateReportCardUseCaseImpl.java
│   │   └── DeleteReportCardUseCaseImpl.java
│   ├── dto/
│   │   ├── common/
│   │   │   ├── ApiResponse.java
│   │   │   └── ErrorResponse.java
│   │   ├── request/
│   │   │   ├── CreateEvaluationRequest.java
│   │   │   ├── UpdateEvaluationRequest.java
│   │   │   ├── CreateReportCardRequest.java
│   │   │   └── UpdateReportCardRequest.java
│   │   └── response/
│   │       ├── EvaluationResponse.java
│   │       ├── EvaluationDetailResponse.java
│   │       ├── ReportCardResponse.java
│   │       └── ReportCardDetailResponse.java
│   ├── events/
│   │   ├── EvaluationRegisteredEvent.java     [record] evaluationId, studentId, courseId, achievementLevel
│   │   └── ReportCardPublishedEvent.java      [record] reportCardId, studentId, institutionId, classroomId, academicYear, periodNumber
│   └── mappers/
│       ├── EvaluationMapper.java
│       └── ReportCardMapper.java
│
├── infrastructure/
│   ├── adapters/
│   │   ├── in/rest/
│   │   │   ├── EvaluationRest.java
│   │   │   │   ├── POST   /api/v1/evaluations
│   │   │   │   ├── GET    /api/v1/evaluations
│   │   │   │   ├── GET    /api/v1/evaluations/{id}
│   │   │   │   ├── GET    /api/v1/evaluations/student/{studentId}
│   │   │   │   ├── GET    /api/v1/evaluations/classroom/{classroomId}
│   │   │   │   ├── GET    /api/v1/evaluations/course/{courseId}
│   │   │   │   ├── PUT    /api/v1/evaluations/{id}
│   │   │   │   └── DELETE /api/v1/evaluations/{id}
│   │   │   │
│   │   │   ├── ReportCardRest.java
│   │   │   │   ├── POST   /api/v1/report-cards
│   │   │   │   ├── GET    /api/v1/report-cards
│   │   │   │   ├── GET    /api/v1/report-cards/{id}
│   │   │   │   ├── GET    /api/v1/report-cards/student/{studentId}
│   │   │   │   ├── PUT    /api/v1/report-cards/{id}
│   │   │   │   ├── PATCH  /api/v1/report-cards/{id}/approve
│   │   │   │   ├── PATCH  /api/v1/report-cards/{id}/publish
│   │   │   │   └── DELETE /api/v1/report-cards/{id}
│   │   │   │
│   │   │   └── GlobalExceptionHandler.java
│   │   │
│   │   └── out/
│   │       ├── persistence/
│   │       │   ├── EvaluationRepositoryImpl.java
│   │       │   └── ReportCardRepositoryImpl.java
│   │       ├── external/
│   │       │   ├── InstitutionClientImpl.java
│   │       │   ├── StudentClientImpl.java
│   │       │   └── AcademicClientImpl.java           ← consulta cursos/competencias
│   │       └── messaging/
│   │           └── NotesEventPublisherImpl.java
│   │
│   ├── persistence/
│   │   ├── entities/
│   │   │   ├── StudentEvaluationEntity.java      ← @Table("student_evaluations")
│   │   │   └── ReportCardEntity.java             ← @Table("report_cards")
│   │   ├── mappers/
│   │   │   ├── EvaluationPersistenceMapper.java
│   │   │   └── ReportCardPersistenceMapper.java
│   │   └── repositories/
│   │       ├── EvaluationR2dbcRepository.java
│   │       └── ReportCardR2dbcRepository.java
│   │
│   └── config/
│       ├── R2dbcConfig.java
│       ├── SecurityConfig.java
│       ├── RabbitMQConfig.java
│       └── WebClientConfig.java
│
└── NotesApplication.java

src/main/resources/
├── application.yml
├── application-dev.yml
├── application-vpc.yml
└── db/migration/
    ├── V1__create_student_evaluations_table.sql
    └── V2__create_report_cards_table.sql
```

---

### 8. vg-ms-assistance

> Control de asistencia diaria, resúmenes mensuales y justificaciones.
> **Puerto:** 9087 | **BD:** PostgreSQL schema `assistance`

```
src/main/java/pe/edu/vallegrande/sigei/assistance/
│
├── domain/
│   ├── models/
│   │   ├── AttendanceRecord.java
│   │   │   ├── id: UUID
│   │   │   ├── studentId: String
│   │   │   ├── classroomId: String
│   │   │   ├── institutionId: String
│   │   │   ├── attendanceDate: LocalDate
│   │   │   ├── academicYear: Integer
│   │   │   ├── attendanceStatus: AttendanceStatus  ← PRESENT, ABSENT, LATE, JUSTIFIED
│   │   │   ├── arrivalTime: LocalTime
│   │   │   ├── departureTime: LocalTime
│   │   │   ├── justified: Boolean
│   │   │   ├── justificationReason: String
│   │   │   ├── justificationDocumentUrl: String
│   │   │   ├── registeredBy: String
│   │   │   ├── registeredAt: LocalDateTime
│   │   │   └── updatedAt: LocalDateTime
│   │   │
│   │   ├── AttendanceSummary.java
│   │   │   ├── id: UUID
│   │   │   ├── studentId: String
│   │   │   ├── classroomId: String
│   │   │   ├── institutionId: String
│   │   │   ├── academicYear: Integer
│   │   │   ├── month: Integer
│   │   │   ├── totalSchoolDays: Integer
│   │   │   ├── daysPresent: Integer
│   │   │   ├── daysAbsent: Integer
│   │   │   ├── daysLate: Integer
│   │   │   ├── daysJustified: Integer
│   │   │   ├── attendancePercentage: BigDecimal
│   │   │   └── lastUpdated: LocalDateTime
│   │   │
│   │   └── valueobjects/
│   │       └── AttendanceStatus.java             ← PRESENT, ABSENT, LATE, JUSTIFIED
│   │
│   ├── ports/
│   │   ├── in/
│   │   │   ├── IRegisterAttendanceUseCase.java
│   │   │   ├── IGetAttendanceUseCase.java
│   │   │   ├── IJustifyAttendanceUseCase.java
│   │   │   ├── IBulkAttendanceUseCase.java
│   │   │   ├── IDeleteAttendanceUseCase.java
│   │   │   └── IAttendanceSummaryUseCase.java
│   │   └── out/
│   │       ├── IAttendanceRecordRepository.java
│   │       ├── IAttendanceSummaryRepository.java
│   │       ├── IFileStoragePort.java             ← Interfaz para subir justificaciones
│   │       └── IAttendanceEventPublisher.java
│   │
│   └── exceptions/
│       ├── DomainException.java
│       ├── NotFoundException.java
│       ├── ConflictException.java
│       ├── AttendanceNotFoundException.java
│       └── AttendanceAlreadyRegisteredException.java
│
├── application/
│   ├── usecases/
│   │   ├── RegisterAttendanceUseCaseImpl.java
│   │   ├── GetAttendanceUseCaseImpl.java
│   │   ├── JustifyAttendanceUseCaseImpl.java
│   │   ├── BulkAttendanceUseCaseImpl.java
│   │   ├── DeleteAttendanceUseCaseImpl.java
│   │   ├── AttendanceSummaryUseCaseImpl.java
│   │   └── FileStorageUseCaseImpl.java
│   ├── dto/
│   │   ├── common/
│   │   │   ├── ApiResponse.java
│   │   │   └── ErrorResponse.java
│   │   ├── request/
│   │   │   ├── CreateAttendanceRequest.java
│   │   │   ├── BulkAttendanceRequest.java
│   │   │   ├── JustifyAttendanceRequest.java
│   │   │   └── RecalculateSummaryRequest.java
│   │   └── response/
│   │       ├── AttendanceResponse.java
│   │       ├── AttendanceSummaryResponse.java
│   │       └── AttendanceStatisticsResponse.java
│   ├── events/
│   │   ├── AttendanceAbsentEvent.java         [record] studentId, institutionId, classroomId, date, registeredBy
│   │   ├── AttendanceLateEvent.java           [record] studentId, institutionId, classroomId, date, arrivalTime
│   │   └── AttendanceDailySummaryEvent.java   [record] institutionId, classroomId, date, presentCount, absentCount, lateCount
│   └── mappers/
│       ├── AttendanceMapper.java
│       └── AttendanceSummaryMapper.java
│
├── infrastructure/
│   ├── adapters/
│   │   ├── in/rest/
│   │   │   ├── AttendanceRest.java
│   │   │   │   ├── POST   /api/v1/attendance
│   │   │   │   ├── POST   /api/v1/attendance/bulk
│   │   │   │   ├── GET    /api/v1/attendance/{id}
│   │   │   │   ├── GET    /api/v1/attendance/student/{studentId}
│   │   │   │   ├── GET    /api/v1/attendance/classroom/{classroomId}
│   │   │   │   ├── GET    /api/v1/attendance/classroom/{classroomId}/date/{date}
│   │   │   │   ├── GET    /api/v1/attendance/institution/{institutionId}
│   │   │   │   ├── GET    /api/v1/attendance/student/{studentId}/stats
│   │   │   │   ├── PUT    /api/v1/attendance/{id}
│   │   │   │   ├── PATCH  /api/v1/attendance/{id}/justify
│   │   │   │   └── DELETE /api/v1/attendance/{id}
│   │   │   │
│   │   │   ├── AttendanceSummaryRest.java
│   │   │   │   ├── GET    /api/v1/attendance-summary/student/{studentId}
│   │   │   │   ├── GET    /api/v1/attendance-summary/classroom/{classroomId}
│   │   │   │   ├── GET    /api/v1/attendance-summary/institution/{institutionId}
│   │   │   │   ├── GET    /api/v1/attendance-summary/statistics
│   │   │   │   └── POST   /api/v1/attendance-summary/recalculate
│   │   │   │
│   │   │   ├── FileUploadRest.java
│   │   │   │   ├── POST   /api/v1/files/upload
│   │   │   │   ├── DELETE /api/v1/files/{fileId}
│   │   │   │   └── GET    /api/v1/files/list
│   │   │   │
│   │   │   └── GlobalExceptionHandler.java
│   │   │
│   │   └── out/
│   │       ├── persistence/
│   │       │   ├── AttendanceRepositoryImpl.java
│   │       │   └── SummaryRepositoryImpl.java
│   │       ├── external/
│   │       │   ├── InstitutionClientImpl.java
│   │       │   ├── StudentClientImpl.java
│   │       │   └── ClassroomClientImpl.java
│   │       ├── storage/
│   │       │   └── SupabaseStorageAdapter.java   ← implements IFileStoragePort
│   │       └── messaging/
│   │           └── AttendanceEventPublisherImpl.java
│   │
│   ├── persistence/
│   │   ├── entities/
│   │   │   ├── AttendanceRecordEntity.java       ← @Table("attendance_records")
│   │   │   └── AttendanceSummaryEntity.java      ← @Table("attendance_summary")
│   │   ├── mappers/
│   │   │   ├── AttendancePersistenceMapper.java
│   │   │   └── SummaryPersistenceMapper.java
│   │   └── repositories/
│   │       ├── AttendanceR2dbcRepository.java
│   │       └── SummaryR2dbcRepository.java
│   │
│   └── config/
│       ├── R2dbcConfig.java
│       ├── SecurityConfig.java
│       ├── RabbitMQConfig.java
│       ├── SupabaseConfig.java
│       └── WebClientConfig.java
│
└── AssistanceApplication.java

src/main/resources/
├── application.yml
├── application-dev.yml
├── application-vpc.yml
└── db/migration/
    ├── V1__create_attendance_records_table.sql
    ├── V2__create_attendance_summary_table.sql
    └── V3__create_attendance_indexes.sql
```

---

### 9. vg-ms-disciplinary-management

> Registro de comportamiento, incidentes y seguimiento disciplinario.
> **Puerto:** 9088 | **BD:** PostgreSQL schema `disciplinary`

```
src/main/java/pe/edu/vallegrande/sigei/disciplinary/
│
├── domain/
│   ├── models/
│   │   ├── BehaviorRecord.java
│   │   │   ├── id: UUID
│   │   │   ├── studentId: String
│   │   │   ├── classroomId: String
│   │   │   ├── institutionId: String
│   │   │   ├── recordDate: LocalDate
│   │   │   ├── academicYear: Integer
│   │   │   ├── behaviorType: BehaviorType        ← POSITIVE, NEGATIVE
│   │   │   ├── behaviorLevel: BehaviorLevel      ← MINOR, MODERATE, SEVERE
│   │   │   ├── description: String
│   │   │   ├── context: String
│   │   │   ├── actionTaken: String
│   │   │   ├── requiresFollowUp: Boolean
│   │   │   ├── recordedBy: String
│   │   │   ├── recordedAt: LocalDateTime
│   │   │   └── updatedAt: LocalDateTime
│   │   │
│   │   ├── Incident.java
│   │   │   ├── id: UUID
│   │   │   ├── studentId: String
│   │   │   ├── classroomId: String
│   │   │   ├── institutionId: String
│   │   │   ├── incidentDate: LocalDate
│   │   │   ├── incidentTime: LocalTime
│   │   │   ├── academicYear: Integer
│   │   │   ├── incidentType: IncidentType        ← PHYSICAL, VERBAL, etc.
│   │   │   ├── severityLevel: SeverityLevel      ← LOW, MEDIUM, HIGH, CRITICAL
│   │   │   ├── description: String
│   │   │   ├── location: String
│   │   │   ├── witnesses: String
│   │   │   ├── otherStudentsInvolved: List<String>
│   │   │   ├── immediateAction: String
│   │   │   ├── parentsNotified: Boolean
│   │   │   ├── notificationDate: LocalDate
│   │   │   ├── followUpRequired: Boolean
│   │   │   ├── status: IncidentStatus            ← OPEN, IN_PROGRESS, RESOLVED, CLOSED
│   │   │   ├── reportedBy: String
│   │   │   ├── reportedAt: LocalDateTime
│   │   │   ├── resolvedBy: String
│   │   │   └── resolvedAt: LocalDateTime
│   │   │
│   │   └── valueobjects/
│   │       ├── BehaviorType.java
│   │       ├── BehaviorLevel.java
│   │       ├── IncidentType.java
│   │       ├── SeverityLevel.java
│   │       └── IncidentStatus.java
│   │
│   ├── ports/
│   │   ├── in/
│   │   │   ├── ICreateBehaviorRecordUseCase.java
│   │   │   ├── IGetBehaviorRecordUseCase.java
│   │   │   ├── IUpdateBehaviorRecordUseCase.java
│   │   │   ├── IDeleteBehaviorRecordUseCase.java
│   │   │   ├── ICreateIncidentUseCase.java
│   │   │   ├── IGetIncidentUseCase.java
│   │   │   ├── IUpdateIncidentUseCase.java
│   │   │   ├── IResolveIncidentUseCase.java
│   │   │   └── IDeleteIncidentUseCase.java
│   │   └── out/
│   │       ├── IBehaviorRecordRepository.java
│   │       ├── IIncidentRepository.java
│   │       └── IDisciplinaryEventPublisher.java
│   │
│   └── exceptions/
│       ├── DomainException.java
│       ├── NotFoundException.java
│       ├── ConflictException.java
│       ├── BehaviorRecordNotFoundException.java
│       └── IncidentNotFoundException.java
│
├── application/
│   ├── usecases/
│   │   ├── CreateBehaviorRecordUseCaseImpl.java
│   │   ├── GetBehaviorRecordUseCaseImpl.java
│   │   ├── UpdateBehaviorRecordUseCaseImpl.java
│   │   ├── DeleteBehaviorRecordUseCaseImpl.java
│   │   ├── CreateIncidentUseCaseImpl.java
│   │   ├── GetIncidentUseCaseImpl.java
│   │   ├── UpdateIncidentUseCaseImpl.java
│   │   ├── ResolveIncidentUseCaseImpl.java
│   │   └── DeleteIncidentUseCaseImpl.java
│   ├── dto/
│   │   ├── common/
│   │   │   ├── ApiResponse.java
│   │   │   └── ErrorResponse.java
│   │   ├── request/
│   │   │   ├── CreateBehaviorRecordRequest.java
│   │   │   ├── UpdateBehaviorRecordRequest.java
│   │   │   ├── CreateIncidentRequest.java
│   │   │   └── UpdateIncidentRequest.java
│   │   └── response/
│   │       ├── BehaviorRecordResponse.java
│   │       ├── BehaviorRecordDetailResponse.java ← con datos de student enriquecidos
│   │       ├── IncidentResponse.java
│   │       └── IncidentDetailResponse.java
│   ├── events/
│   │   ├── IncidentCreatedEvent.java          [record] incidentId, studentId, institutionId, incidentType, severityLevel, description
│   │   ├── IncidentResolvedEvent.java         [record] incidentId, studentId, resolvedBy, resolution
│   │   └── BehaviorAlertEvent.java            [record] studentId, institutionId, behaviorType, behaviorLevel, description
│   └── mappers/
│       ├── BehaviorRecordMapper.java
│       └── IncidentMapper.java
│
├── infrastructure/
│   ├── adapters/
│   │   ├── in/rest/
│   │   │   ├── BehaviorRecordRest.java
│   │   │   │   ├── POST   /api/v1/behavior-records
│   │   │   │   ├── GET    /api/v1/behavior-records
│   │   │   │   ├── GET    /api/v1/behavior-records/student/{studentId}
│   │   │   │   ├── GET    /api/v1/behavior-records/classroom/{classroomId}
│   │   │   │   ├── PUT    /api/v1/behavior-records/{id}
│   │   │   │   └── DELETE /api/v1/behavior-records/{id}
│   │   │   │
│   │   │   ├── IncidentRest.java
│   │   │   │   ├── POST   /api/v1/incidents
│   │   │   │   ├── GET    /api/v1/incidents
│   │   │   │   ├── GET    /api/v1/incidents/{id}
│   │   │   │   ├── GET    /api/v1/incidents/student/{studentId}
│   │   │   │   ├── GET    /api/v1/incidents/status/{status}
│   │   │   │   ├── PUT    /api/v1/incidents/{id}
│   │   │   │   ├── PATCH  /api/v1/incidents/{id}/resolve
│   │   │   │   └── DELETE /api/v1/incidents/{id}
│   │   │   │
│   │   │   └── GlobalExceptionHandler.java
│   │   │
│   │   └── out/
│   │       ├── persistence/
│   │       │   ├── BehaviorRepositoryImpl.java
│   │       │   └── IncidentRepositoryImpl.java
│   │       ├── external/
│   │       │   ├── InstitutionClientImpl.java
│   │       │   ├── StudentClientImpl.java
│   │       │   ├── ClassroomClientImpl.java
│   │       │   └── UserClientImpl.java
│   │       └── messaging/
│   │           └── DisciplinaryEventPublisherImpl.java
│   │
│   ├── persistence/
│   │   ├── entities/
│   │   │   ├── BehaviorRecordEntity.java          ← @Table("behavior_records")
│   │   │   └── IncidentEntity.java                ← @Table("incidents")
│   │   ├── mappers/
│   │   │   ├── BehaviorPersistenceMapper.java
│   │   │   └── IncidentPersistenceMapper.java
│   │   └── repositories/
│   │       ├── BehaviorR2dbcRepository.java
│   │       └── IncidentR2dbcRepository.java
│   │
│   └── config/
│       ├── R2dbcConfig.java
│       ├── SecurityConfig.java
│       ├── RabbitMQConfig.java
│       └── WebClientConfig.java
│
└── DisciplinaryApplication.java

src/main/resources/
├── application.yml
├── application-dev.yml
├── application-vpc.yml
└── db/migration/
    ├── V1__create_behavior_records_table.sql
    ├── V2__create_incidents_table.sql
    └── V3__create_disciplinary_indexes.sql
```

---

### 10. vg-ms-psychology-welfare

> Evaluaciones psicológicas, apoyo a necesidades especiales y bienestar estudiantil.
> **Puerto:** 9090 | **BD:** PostgreSQL schema `psychology`

```
src/main/java/pe/edu/vallegrande/sigei/psychology/
│
├── domain/
│   ├── models/
│   │   ├── PsychologicalEvaluation.java
│   │   │   ├── id: UUID
│   │   │   ├── studentId: UUID
│   │   │   ├── classroomId: UUID
│   │   │   ├── institutionId: UUID
│   │   │   ├── evaluationDate: LocalDate
│   │   │   ├── academicYear: Integer
│   │   │   ├── evaluationType: EvaluationType    ← INITIAL, FOLLOW_UP, FINAL, EMERGENCY
│   │   │   ├── evaluationReason: String
│   │   │   ├── emotionalDevelopment: DevelopmentLevel
│   │   │   ├── socialDevelopment: DevelopmentLevel
│   │   │   ├── cognitiveDevelopment: DevelopmentLevel
│   │   │   ├── motorDevelopment: DevelopmentLevel
│   │   │   ├── observations: String
│   │   │   ├── recommendations: String
│   │   │   ├── requiresFollowUp: Boolean
│   │   │   ├── followUpFrequency: String
│   │   │   ├── evaluatedBy: UUID
│   │   │   ├── status: Status
│   │   │   ├── evaluatedAt: LocalDateTime
│   │   │   └── updatedAt: LocalDateTime
│   │   │
│   │   ├── SpecialNeedsSupport.java
│   │   │   ├── id: UUID
│   │   │   ├── studentId: UUID
│   │   │   ├── classroomId: UUID
│   │   │   ├── institutionId: UUID
│   │   │   ├── academicYear: Integer
│   │   │   ├── diagnosis: String
│   │   │   ├── diagnosisDate: LocalDate
│   │   │   ├── diagnosedBy: String
│   │   │   ├── supportType: SupportType          ← SPEECH_THERAPY, OCCUPATIONAL_THERAPY, etc.
│   │   │   ├── description: String
│   │   │   ├── adaptationsRequired: List<String>
│   │   │   ├── supportMaterials: List<String>
│   │   │   ├── specialistInvolved: String
│   │   │   ├── progressNotes: String
│   │   │   ├── lastReviewDate: LocalDate
│   │   │   ├── nextReviewDate: LocalDate
│   │   │   ├── status: Status
│   │   │   ├── createdAt: LocalDateTime
│   │   │   └── updatedAt: LocalDateTime
│   │   │
│   │   └── valueobjects/
│   │       ├── EvaluationType.java               ← INITIAL, FOLLOW_UP, FINAL, EMERGENCY
│   │       ├── DevelopmentLevel.java             ← ADVANCED, EXPECTED, IN_PROGRESS, NEEDS_SUPPORT
│   │       ├── SupportType.java                  ← SPEECH_THERAPY, OCCUPATIONAL, BEHAVIORAL, etc.
│   │       └── Status.java                       ← ACTIVE, INACTIVE
│   │
│   ├── ports/
│   │   ├── in/
│   │   │   ├── ICreateEvaluationUseCase.java
│   │   │   ├── IGetEvaluationUseCase.java
│   │   │   ├── IUpdateEvaluationUseCase.java
│   │   │   ├── IDeactivateEvaluationUseCase.java
│   │   │   ├── ICreateSpecialNeedsUseCase.java
│   │   │   ├── IGetSpecialNeedsUseCase.java
│   │   │   ├── IUpdateSpecialNeedsUseCase.java
│   │   │   └── IDeleteSpecialNeedsUseCase.java
│   │   └── out/
│   │       ├── IPsychologicalEvaluationRepository.java
│   │       ├── ISpecialNeedsSupportRepository.java
│   │       └── IPsychologyEventPublisher.java
│   │
│   └── exceptions/
│       ├── DomainException.java
│       ├── NotFoundException.java
│       ├── ConflictException.java
│       ├── EvaluationNotFoundException.java
│       └── SpecialNeedsSupportNotFoundException.java
│
├── application/
│   ├── usecases/
│   │   ├── CreateEvaluationUseCaseImpl.java
│   │   ├── GetEvaluationUseCaseImpl.java
│   │   ├── UpdateEvaluationUseCaseImpl.java
│   │   ├── DeactivateEvaluationUseCaseImpl.java
│   │   ├── CreateSpecialNeedsUseCaseImpl.java
│   │   ├── GetSpecialNeedsUseCaseImpl.java
│   │   ├── UpdateSpecialNeedsUseCaseImpl.java
│   │   └── DeleteSpecialNeedsUseCaseImpl.java
│   ├── dto/
│   │   ├── common/
│   │   │   ├── ApiResponse.java
│   │   │   └── ErrorResponse.java
│   │   ├── request/
│   │   │   ├── CreateEvaluationRequest.java
│   │   │   ├── UpdateEvaluationRequest.java
│   │   │   ├── CreateSpecialNeedsRequest.java
│   │   │   └── UpdateSpecialNeedsRequest.java
│   │   └── response/
│   │       ├── EvaluationResponse.java
│   │       ├── EvaluationDetailResponse.java
│   │       ├── SpecialNeedsResponse.java
│   │       └── ReferenceDataResponse.java
│   ├── events/
│   │   ├── PsychologicalEvaluationCompletedEvent.java [record] evaluationId, studentId, institutionId, evaluationType, requiresFollowUp
│   │   └── FollowUpDueEvent.java              [record] evaluationId, studentId, institutionId, dueDate, followUpFrequency
│   └── mappers/
│       ├── EvaluationMapper.java
│       └── SpecialNeedsMapper.java
│
├── infrastructure/
│   ├── adapters/
│   │   ├── in/rest/
│   │   │   ├── PsychologicalEvaluationRest.java
│   │   │   │   ├── POST   /api/v1/psychological-evaluations
│   │   │   │   ├── GET    /api/v1/psychological-evaluations
│   │   │   │   ├── GET    /api/v1/psychological-evaluations/{id}
│   │   │   │   ├── GET    /api/v1/psychological-evaluations/student/{studentId}
│   │   │   │   ├── GET    /api/v1/psychological-evaluations/classroom/{classroomId}
│   │   │   │   ├── GET    /api/v1/psychological-evaluations/institution/{institutionId}
│   │   │   │   ├── GET    /api/v1/psychological-evaluations/active
│   │   │   │   ├── GET    /api/v1/psychological-evaluations/inactive
│   │   │   │   ├── PUT    /api/v1/psychological-evaluations/{id}
│   │   │   │   ├── PATCH  /api/v1/psychological-evaluations/{id}/deactivate
│   │   │   │   └── PATCH  /api/v1/psychological-evaluations/{id}/reactivate
│   │   │   │
│   │   │   ├── SpecialNeedsSupportRest.java
│   │   │   │   ├── POST   /api/v1/special-needs-support
│   │   │   │   ├── GET    /api/v1/special-needs-support
│   │   │   │   ├── GET    /api/v1/special-needs-support/{id}
│   │   │   │   ├── GET    /api/v1/special-needs-support/student/{studentId}
│   │   │   │   ├── GET    /api/v1/special-needs-support/type/{supportType}
│   │   │   │   ├── PUT    /api/v1/special-needs-support/{id}
│   │   │   │   ├── DELETE /api/v1/special-needs-support/{id}
│   │   │   │   └── PATCH  /api/v1/special-needs-support/{id}/activate
│   │   │   │
│   │   │   ├── ReferenceDataRest.java
│   │   │   │   ├── GET    /api/v1/reference-data/students
│   │   │   │   ├── GET    /api/v1/reference-data/classrooms
│   │   │   │   ├── GET    /api/v1/reference-data/institutions
│   │   │   │   └── GET    /api/v1/reference-data/evaluators
│   │   │   │
│   │   │   └── GlobalExceptionHandler.java
│   │   │
│   │   └── out/
│   │       ├── persistence/
│   │       │   ├── EvaluationRepositoryImpl.java
│   │       │   └── SpecialNeedsRepositoryImpl.java
│   │       ├── external/
│   │       │   ├── InstitutionClientImpl.java
│   │       │   ├── StudentClientImpl.java
│   │       │   ├── ClassroomClientImpl.java
│   │       │   └── UserClientImpl.java
│   │       └── messaging/
│   │           └── PsychologyEventPublisherImpl.java
│   │
│   ├── persistence/
│   │   ├── entities/
│   │   │   ├── EvaluationEntity.java              ← @Table("psychological_evaluations")
│   │   │   └── SpecialNeedsSupportEntity.java     ← @Table("special_needs_support")
│   │   ├── mappers/
│   │   │   ├── EvaluationPersistenceMapper.java
│   │   │   └── SpecialNeedsPersistenceMapper.java
│   │   └── repositories/
│   │       ├── EvaluationR2dbcRepository.java
│   │       └── SpecialNeedsR2dbcRepository.java
│   │
│   └── config/
│       ├── R2dbcConfig.java
│       ├── SecurityConfig.java
│       ├── RabbitMQConfig.java
│       └── WebClientConfig.java
│
└── PsychologyApplication.java

src/main/resources/
├── application.yml
├── application-dev.yml
├── application-vpc.yml
└── db/migration/
    ├── V1__create_psychological_evaluations_table.sql
    ├── V2__create_special_needs_support_table.sql
    └── V3__create_psychology_indexes.sql
```

---

### 11. vg-ms-teacher-assignment

> Asignación de docentes a aulas, horarios y gestión de carga académica.
> **Puerto:** 9099 | **BD:** PostgreSQL schema `teacher_assignment`

```
src/main/java/pe/edu/vallegrande/sigei/teacherAssignment/
│
├── domain/
│   ├── models/
│   │   ├── TeacherAssignment.java
│   │   │   ├── id: UUID
│   │   │   ├── teacherUserId: String
│   │   │   ├── institutionId: String
│   │   │   ├── assignmentType: AssignmentType    ← REGULAR, SUBSTITUTE, SUPPORT
│   │   │   ├── status: Status                    ← ACTIVE, INACTIVE, DELETED
│   │   │   ├── startDate: LocalDate
│   │   │   ├── endDate: LocalDate
│   │   │   ├── academicYear: String
│   │   │   ├── notes: String
│   │   │   ├── createdAt: LocalDateTime
│   │   │   ├── updatedAt: LocalDateTime
│   │   │   └── deletedAt: LocalDateTime
│   │   │
│   │   ├── TeacherAssignmentClassroom.java
│   │   │   ├── id: UUID
│   │   │   ├── assignmentId: UUID
│   │   │   ├── classroomId: String
│   │   │   ├── isPrimary: boolean
│   │   │   ├── createdAt: LocalDateTime
│   │   │   └── updatedAt: LocalDateTime
│   │   │
│   │   ├── TeacherAssignmentSchedule.java
│   │   │   ├── id: UUID
│   │   │   ├── assignmentId: UUID
│   │   │   ├── classroomId: String
│   │   │   ├── dayOfWeek: DayOfWeek
│   │   │   ├── startTime: LocalTime
│   │   │   ├── endTime: LocalTime
│   │   │   ├── sessionType: SessionType          ← REGULAR, TUTORIAL, EXTRA
│   │   │   └── createdAt: LocalDateTime
│   │   │
│   │   └── valueobjects/
│   │       ├── AssignmentType.java               ← REGULAR, SUBSTITUTE, SUPPORT
│   │       ├── Status.java                       ← ACTIVE, INACTIVE, DELETED
│   │       ├── DayOfWeek.java                    ← MONDAY..FRIDAY
│   │       └── SessionType.java                  ← REGULAR, TUTORIAL, EXTRA
│   │
│   ├── ports/
│   │   ├── in/
│   │   │   ├── ICreateAssignmentUseCase.java
│   │   │   ├── IGetAssignmentUseCase.java
│   │   │   ├── IUpdateAssignmentUseCase.java
│   │   │   ├── IDeleteAssignmentUseCase.java
│   │   │   ├── IManageClassroomAssignmentUseCase.java
│   │   │   └── IManageScheduleUseCase.java
│   │   └── out/
│   │       ├── ITeacherAssignmentRepository.java
│   │       ├── IAssignmentClassroomRepository.java
│   │       ├── IAssignmentScheduleRepository.java
│   │       └── IAssignmentEventPublisher.java
│   │
│   └── exceptions/
│       ├── DomainException.java
│       ├── NotFoundException.java
│       ├── ConflictException.java
│       ├── AssignmentNotFoundException.java
│       └── AssignmentConflictException.java
│
├── application/
│   ├── usecases/
│   │   ├── CreateAssignmentUseCaseImpl.java
│   │   ├── GetAssignmentUseCaseImpl.java
│   │   ├── UpdateAssignmentUseCaseImpl.java
│   │   ├── DeleteAssignmentUseCaseImpl.java
│   │   ├── ManageClassroomAssignmentUseCaseImpl.java
│   │   └── ManageScheduleUseCaseImpl.java
│   ├── dto/
│   │   ├── common/
│   │   │   ├── ApiResponse.java
│   │   │   └── ErrorResponse.java
│   │   ├── request/
│   │   │   ├── CreateAssignmentRequest.java
│   │   │   ├── UpdateAssignmentRequest.java
│   │   │   ├── AddClassroomRequest.java
│   │   │   └── AddScheduleRequest.java
│   │   └── response/
│   │       ├── AssignmentResponse.java
│   │       ├── AssignmentDetailResponse.java     ← con classrooms y schedules
│   │       ├── ClassroomAssignmentResponse.java
│   │       └── ScheduleResponse.java
│   ├── events/
│   │   ├── AssignmentCreatedEvent.java        [record] assignmentId, teacherUserId, institutionId, classroomIds, academicYear
│   │   └── AssignmentUpdatedEvent.java        [record] assignmentId, teacherUserId, changes
│   └── mappers/
│       └── AssignmentMapper.java
│
├── infrastructure/
│   ├── adapters/
│   │   ├── in/rest/
│   │   │   ├── TeacherAssignmentRest.java
│   │   │   │   ├── POST   /api/v1/teacher-assignments
│   │   │   │   ├── GET    /api/v1/teacher-assignments
│   │   │   │   ├── GET    /api/v1/teacher-assignments/{id}
│   │   │   │   ├── GET    /api/v1/teacher-assignments/teacher/{teacherUserId}
│   │   │   │   ├── GET    /api/v1/teacher-assignments/institution/{institutionId}
│   │   │   │   ├── GET    /api/v1/teacher-assignments/status/{status}
│   │   │   │   ├── GET    /api/v1/teacher-assignments/academic-year/{year}
│   │   │   │   ├── PUT    /api/v1/teacher-assignments/{id}
│   │   │   │   ├── PATCH  /api/v1/teacher-assignments/{id}/status
│   │   │   │   ├── DELETE /api/v1/teacher-assignments/{id}
│   │   │   │   └── PATCH  /api/v1/teacher-assignments/{id}/restore
│   │   │   │
│   │   │   ├── AssignmentManagementRest.java
│   │   │   │   ├── POST   /api/v1/assignments-management/{id}/classrooms
│   │   │   │   ├── DELETE /api/v1/assignments-management/{id}/classrooms/{classroomId}
│   │   │   │   ├── PATCH  /api/v1/assignments-management/{id}/classrooms/{classroomId}/primary
│   │   │   │   ├── POST   /api/v1/assignments-management/{id}/schedules
│   │   │   │   └── DELETE /api/v1/assignments-management/{id}/schedules/{scheduleId}
│   │   │   │
│   │   │   └── GlobalExceptionHandler.java
│   │   │
│   │   └── out/
│   │       ├── persistence/
│   │       │   ├── AssignmentRepositoryImpl.java
│   │       │   ├── ClassroomAssignmentRepositoryImpl.java
│   │       │   └── ScheduleRepositoryImpl.java
│   │       ├── external/
│   │       │   ├── InstitutionClientImpl.java
│   │       │   ├── UserClientImpl.java
│   │       │   ├── ClassroomClientImpl.java
│   │       │   └── CourseClientImpl.java
│   │       └── messaging/
│   │           └── AssignmentEventPublisherImpl.java
│   │
│   ├── persistence/
│   │   ├── entities/
│   │   │   ├── TeacherAssignmentEntity.java          ← @Table("teacher_assignments")
│   │   │   ├── AssignmentClassroomEntity.java        ← @Table("assignment_classrooms")
│   │   │   └── AssignmentScheduleEntity.java         ← @Table("assignment_schedules")
│   │   ├── mappers/
│   │   │   └── AssignmentPersistenceMapper.java
│   │   └── repositories/
│   │       ├── AssignmentR2dbcRepository.java
│   │       ├── AssignmentClassroomR2dbcRepository.java
│   │       └── AssignmentScheduleR2dbcRepository.java
│   │
│   └── config/
│       ├── R2dbcConfig.java
│       ├── SecurityConfig.java
│       ├── RabbitMQConfig.java
│       └── WebClientConfig.java
│
└── TeacherAssignmentApplication.java

src/main/resources/
├── application.yml
├── application-dev.yml
├── application-vpc.yml
└── db/migration/
    ├── V1__create_teacher_assignments_table.sql
    ├── V2__create_assignment_classrooms_table.sql
    ├── V3__create_assignment_schedules_table.sql
    └── V4__create_assignment_indexes.sql
```

---

### 12. vg-ms-notifications

> **Microservicio de notificaciones vía WhatsApp usando Evolution API.**
> Envía mensajes, archivos, reportes de asistencia, libretas de notas, alertas de incidentes.
> Consume eventos de TODOS los demás MS vía RabbitMQ.
> **Puerto:** 9091 | **BD:** PostgreSQL schema `notifications`

```
src/main/java/pe/edu/vallegrande/sigei/notifications/
│
├── domain/
│   ├── models/
│   │   ├── Notification.java
│   │   │   ├── id: UUID
│   │   │   ├── institutionId: String
│   │   │   ├── recipientId: String              ← userId o guardianId
│   │   │   ├── recipientPhone: String           ← "+51987654321"
│   │   │   ├── recipientName: String
│   │   │   ├── channel: NotificationChannel      ← WHATSAPP (extensible a EMAIL, SMS)
│   │   │   ├── type: NotificationType            ← ATTENDANCE, GRADES, INCIDENT, etc.
│   │   │   ├── templateKey: String               ← "attendance.absent", "grades.report_card"
│   │   │   ├── subject: String
│   │   │   ├── bodyText: String                  ← Texto del mensaje
│   │   │   ├── variables: Map<String, String>    ← Variables para plantilla
│   │   │   ├── attachments: List<Attachment>     ← Archivos adjuntos
│   │   │   ├── status: NotificationStatus        ← PENDING, SENT, DELIVERED, READ, FAILED
│   │   │   ├── retryCount: Integer
│   │   │   ├── maxRetries: Integer               ← default 3
│   │   │   ├── lastError: String
│   │   │   ├── sentAt: LocalDateTime
│   │   │   ├── deliveredAt: LocalDateTime
│   │   │   ├── readAt: LocalDateTime
│   │   │   ├── createdAt: LocalDateTime
│   │   │   └── updatedAt: LocalDateTime
│   │   │
│   │   ├── NotificationTemplate.java
│   │   │   ├── id: UUID
│   │   │   ├── templateKey: String               ← "attendance.absent" (único)
│   │   │   ├── name: String                      ← "Notificación de inasistencia"
│   │   │   ├── type: NotificationType
│   │   │   ├── bodyTemplate: String              ← "Estimado/a {{guardianName}}, le informamos..."
│   │   │   ├── variables: List<String>           ← ["guardianName", "studentName", "date"]
│   │   │   ├── isActive: boolean
│   │   │   ├── createdAt: LocalDateTime
│   │   │   └── updatedAt: LocalDateTime
│   │   │
│   │   ├── Attachment.java                       ← Value Object (record)
│   │   │   ├── fileName: String
│   │   │   ├── mimeType: String                  ← "application/pdf", "image/png"
│   │   │   ├── fileUrl: String                   ← URL del archivo o base64
│   │   │   └── fileSize: Long
│   │   │
│   │   ├── NotificationLog.java
│   │   │   ├── id: UUID
│   │   │   ├── notificationId: UUID
│   │   │   ├── action: String                    ← "SENT", "DELIVERED", "READ", "FAILED", "RETRY"
│   │   │   ├── detail: String
│   │   │   ├── evolutionResponse: String         ← JSON response de Evolution API
│   │   │   └── createdAt: LocalDateTime
│   │   │
│   │   └── valueobjects/
│   │       ├── NotificationChannel.java          ← WHATSAPP, EMAIL, SMS (futuro)
│   │       ├── NotificationType.java             ← ver detalle abajo
│   │       └── NotificationStatus.java           ← PENDING, SENT, DELIVERED, READ, FAILED
│   │
│   ├── ports/
│   │   ├── in/
│   │   │   ├── ISendNotificationUseCase.java
│   │   │   ├── IGetNotificationUseCase.java
│   │   │   ├── IRetryNotificationUseCase.java
│   │   │   ├── IManageTemplateUseCase.java
│   │   │   └── IProcessEventUseCase.java         ← Procesa eventos de RabbitMQ
│   │   └── out/
│   │       ├── INotificationRepository.java
│   │       ├── INotificationTemplateRepository.java
│   │       ├── INotificationLogRepository.java
│   │       └── IWhatsAppSenderPort.java          ← Interfaz hacia Evolution API
│   │
│   └── exceptions/
│       ├── DomainException.java
│       ├── NotFoundException.java
│       ├── ConflictException.java
│       ├── NotificationNotFoundException.java
│       ├── TemplateNotFoundException.java
│       ├── WhatsAppSendException.java
│       └── InvalidPhoneNumberException.java
│
├── application/
│   ├── usecases/
│   │   ├── SendNotificationUseCaseImpl.java
│   │   ├── GetNotificationUseCaseImpl.java
│   │   ├── RetryNotificationUseCaseImpl.java
│   │   ├── ManageTemplateUseCaseImpl.java
│   │   └── ProcessEventUseCaseImpl.java      ← Convierte eventos RabbitMQ → Notification
│   ├── dto/
│   │   ├── common/
│   │   │   ├── ApiResponse.java
│   │   │   └── ErrorResponse.java
│   │   ├── request/
│   │   │   ├── SendNotificationRequest.java
│   │   │   ├── SendBulkNotificationRequest.java  ← Envío masivo (ej: todas las faltas del día)
│   │   │   ├── CreateTemplateRequest.java
│   │   │   └── UpdateTemplateRequest.java
│   │   └── response/
│   │       ├── NotificationResponse.java
│   │       ├── NotificationDetailResponse.java   ← con logs
│   │       ├── NotificationStatsResponse.java    ← estadísticas
│   │       └── TemplateResponse.java
│   ├── mappers/
│   │   ├── NotificationMapper.java
│   │   └── TemplateMapper.java
│   └── events/                                   ← DTOs de eventos que recibe de otros MS
│       ├── AttendanceEvent.java                  ← Evento de asistencia (de MS Assistance)
│       ├── GradePublishedEvent.java              ← Libreta publicada (de MS Notes)
│       ├── IncidentCreatedEvent.java             ← Incidente (de MS Disciplinary)
│       ├── EnrollmentConfirmedEvent.java         ← Matrícula confirmada (de MS Enrollments)
│       ├── EvaluationCompletedEvent.java         ← Eval. psicológica (de MS Psychology)
│       └── InstitutionAnnouncementEvent.java     ← Comunicado general (de MS Institution)
│
├── infrastructure/
│   ├── adapters/
│   │   ├── in/
│   │   │   ├── rest/
│   │   │   │   ├── NotificationRest.java
│   │   │   │   │   ├── POST   /api/v1/notifications/send
│   │   │   │   │   ├── POST   /api/v1/notifications/send-bulk
│   │   │   │   │   ├── GET    /api/v1/notifications
│   │   │   │   │   ├── GET    /api/v1/notifications/{id}
│   │   │   │   │   ├── GET    /api/v1/notifications/{id}/logs
│   │   │   │   │   ├── GET    /api/v1/notifications/recipient/{recipientId}
│   │   │   │   │   ├── GET    /api/v1/notifications/institution/{institutionId}
│   │   │   │   │   ├── GET    /api/v1/notifications/status/{status}
│   │   │   │   │   ├── GET    /api/v1/notifications/stats/{institutionId}
│   │   │   │   │   └── POST   /api/v1/notifications/{id}/retry
│   │   │   │   │
│   │   │   │   ├── TemplateRest.java
│   │   │   │   │   ├── GET    /api/v1/templates
│   │   │   │   │   ├── GET    /api/v1/templates/{id}
│   │   │   │   │   ├── GET    /api/v1/templates/key/{templateKey}
│   │   │   │   │   ├── POST   /api/v1/templates
│   │   │   │   │   ├── PUT    /api/v1/templates/{id}
│   │   │   │   │   └── DELETE /api/v1/templates/{id}
│   │   │   │   │
│   │   │   │   ├── WebhookRest.java                  ← Recibe webhooks de Evolution API
│   │   │   │   │   └── POST   /api/v1/webhooks/evolution
│   │   │   │   │
│   │   │   │   └── GlobalExceptionHandler.java
│   │   │   │
│   │   │   └── messaging/                        ← Adaptadores de ENTRADA (RabbitMQ consumers)
│   │   │       ├── AttendanceEventListener.java
│   │   │       │   └── @RabbitListener("notification.attendance")
│   │   │       ├── GradeEventListener.java
│   │   │       │   └── @RabbitListener("notification.grades")
│   │   │       ├── IncidentEventListener.java
│   │   │       │   └── @RabbitListener("notification.incidents")
│   │   │       ├── EnrollmentEventListener.java
│   │   │       │   └── @RabbitListener("notification.enrollments")
│   │   │       ├── PsychologyEventListener.java
│   │   │       │   └── @RabbitListener("notification.psychology")
│   │   │       └── AnnouncementEventListener.java
│   │   │           └── @RabbitListener("notification.announcements")
│   │   │
│   │   └── out/
│   │       ├── persistence/
│   │       │   ├── NotificationRepositoryImpl.java
│   │       │   ├── TemplateRepositoryImpl.java
│   │       │   └── LogRepositoryImpl.java
│   │       ├── external/
│   │       │   ├── StudentClientImpl.java            ← Obtener datos del estudiante y guardián
│   │       │   ├── InstitutionClientImpl.java        ← Datos de la institución
│   │       │   └── UserClientImpl.java               ← Datos del usuario para el teléfono
│   │       └── whatsapp/                         ← Adaptador hacia EVOLUTION API
│   │           ├── EvolutionApiClient.java        ← WebClient → Evolution API
│   │           ├── EvolutionWhatsAppAdapter.java  ← implements IWhatsAppSenderPort
│   │           ├── dto/
│   │           │   ├── EvolutionSendTextRequest.java
│   │           │   ├── EvolutionSendMediaRequest.java
│   │           │   ├── EvolutionSendDocumentRequest.java
│   │           │   ├── EvolutionWebhookPayload.java
│   │           │   └── EvolutionResponse.java
│   │           └── mapper/
│   │               └── EvolutionMapper.java       ← Domain → Evolution API format
│   │
│   ├── persistence/
│   │   ├── entities/
│   │   │   ├── NotificationEntity.java           ← @Table("notifications")
│   │   │   ├── NotificationTemplateEntity.java   ← @Table("notification_templates")
│   │   │   └── NotificationLogEntity.java        ← @Table("notification_logs")
│   │   ├── mappers/
│   │   │   ├── NotificationPersistenceMapper.java
│   │   │   ├── TemplatePersistenceMapper.java
│   │   │   └── LogPersistenceMapper.java
│   │   └── repositories/
│   │       ├── NotificationR2dbcRepository.java
│   │       ├── TemplateR2dbcRepository.java
│   │       └── LogR2dbcRepository.java
│   │
│   └── config/
│       ├── R2dbcConfig.java
│       ├── SecurityConfig.java
│       ├── RabbitMQConfig.java
│       ├── EvolutionApiConfig.java               ← URL, API Key, Instance Name
│       ├── WebClientConfig.java
│       └── SchedulerConfig.java                  ← Para reintentos programados
│
└── NotificationsApplication.java

src/main/resources/
├── application.yml
├── application-dev.yml
├── application-vpc.yml
└── db/migration/
    ├── V1__create_notifications_table.sql
    ├── V2__create_notification_templates_table.sql
    ├── V3__create_notification_logs_table.sql
    ├── V4__insert_default_templates.sql
    └── V5__create_notification_indexes.sql
```

#### Tipos de Notificación (NotificationType)

```java
public enum NotificationType {
    // ── Asistencia ──
    ATTENDANCE_ABSENT,          // "Su hijo/a no asistió hoy"
    ATTENDANCE_LATE,            // "Su hijo/a llegó tarde"
    ATTENDANCE_DAILY_SUMMARY,   // Resumen diario de asistencia del aula

    // ── Notas / Evaluaciones ──
    GRADES_REPORT_CARD,         // Libreta de notas publicada (enviar PDF)
    GRADES_EVALUATION,          // Nueva evaluación registrada

    // ── Disciplina / Incidentes ──
    INCIDENT_CREATED,           // Nuevo incidente reportado
    INCIDENT_RESOLVED,          // Incidente resuelto
    BEHAVIOR_ALERT,             // Alerta de comportamiento

    // ── Psicología ──
    PSYCHOLOGY_EVALUATION,      // Evaluación psicológica completada
    PSYCHOLOGY_FOLLOW_UP,       // Recordatorio de seguimiento

    // ── Matrículas ──
    ENROLLMENT_CONFIRMED,       // Matrícula confirmada
    ENROLLMENT_PERIOD_OPEN,     // Período de matrícula abierto

    // ── Institucional ──
    ANNOUNCEMENT,               // Comunicado general de la institución
    EVENT_REMINDER,             // Recordatorio de evento cívico/escolar

    // ── Sistema ──
    CUSTOM                      // Mensaje personalizado
}
```

#### Plantillas de Mensaje (ejemplos)

```
─────────────────────────────────────────────────────────
Template Key: attendance.absent
─────────────────────────────────────────────────────────
🏫 *{{institutionName}}*

Estimado/a *{{guardianName}}*,

Le informamos que su hijo/a *{{studentName}}* del aula
*{{classroomName}}* no asistió a clases el día *{{date}}*.

Si tiene alguna justificación, acérquese a la institución
o comuníquese con la docente.

_Mensaje automático — SIGEI_

─────────────────────────────────────────────────────────
Template Key: grades.report_card
─────────────────────────────────────────────────────────
🏫 *{{institutionName}}*

Estimado/a *{{guardianName}}*,

La libreta de notas de *{{studentName}}* correspondiente
al *{{periodName}}* del año *{{academicYear}}* ha sido
publicada.

📄 Se adjunta el documento en PDF.

Si tiene consultas, comuníquese con la docente del aula
*{{classroomName}}*.

_Mensaje automático — SIGEI_

─────────────────────────────────────────────────────────
Template Key: incident.created
─────────────────────────────────────────────────────────
🏫 *{{institutionName}}*

⚠️ Estimado/a *{{guardianName}}*,

Le informamos que se ha registrado un incidente relacionado
con su hijo/a *{{studentName}}* el día *{{date}}*.

*Tipo:* {{incidentType}}
*Descripción:* {{description}}
*Acción tomada:* {{actionTaken}}

Por favor, acérquese a la institución para coordinar
el seguimiento.

_Mensaje automático — SIGEI_
```

#### Evolution API — Configuración

```yaml
# application.yml — sección de Evolution API
evolution:
  api:
    base-url: ${EVOLUTION_API_URL:http://localhost:8085}
    api-key: ${EVOLUTION_API_KEY}
    instance-name: ${EVOLUTION_INSTANCE:sigei-whatsapp}

  webhook:
    url: ${EVOLUTION_WEBHOOK_URL:http://ms-notifications:9091/api/v1/webhooks/evolution}
    events:
      - MESSAGES_UPSERT           # Mensaje enviado/recibido
      - MESSAGES_UPDATE           # Estado actualizado (delivered, read)
      - CONNECTION_UPDATE         # Estado de conexión WhatsApp
```

#### Evolution API — Endpoints que consume el MS

```
Evolution API Base: http://evolution-api:8085

POST /message/sendText/{instance}         ← Enviar mensaje de texto
POST /message/sendMedia/{instance}        ← Enviar imagen/video/audio
POST /message/sendWhatsAppAudio/{instance}
POST /message/sendDocument/{instance}     ← Enviar PDF (libretas, reportes)
POST /message/sendSticker/{instance}
GET  /instance/connectionState/{instance} ← Verificar si WhatsApp está conectado
POST /instance/create                     ← Crear instancia de WhatsApp
GET  /instance/fetchInstances             ← Listar instancias
```

---

### 13. vg-ms-gateway

> API Gateway — Punto de entrada único. Enruta requests, valida JWT de Keycloak, aplica CORS.
> **Puerto:** 8080

```
src/main/java/pe/edu/vallegrande/sigei/gateway/
│
├── config/
│   ├── CorsConfig.java                          ← CORS centralizado (ÚNICO lugar)
│   ├── SecurityConfig.java                       ← OAuth2 Resource Server + Keycloak JWT
│   ├── RouteConfig.java                          ← Rutas a todos los MS
│   ├── RateLimitConfig.java                      ← Rate limiting por IP/token
│   └── CircuitBreakerConfig.java                 ← Resilience4j fallbacks
│
├── filter/
│   ├── AuthenticationFilter.java                 ← Valida JWT en cada request
│   ├── LoggingFilter.java                        ← Log de requests entrantes
│   └── RateLimitFilter.java
│
└── GatewayApplication.java

src/main/resources/
├── application.yml
│   spring:
│     cloud:
│       gateway:
│         routes:
│           - id: ms-institution
│             uri: lb://MS-INSTITUTION
│             predicates:
│               - Path=/api/v1/institutions/**, /api/v1/classrooms/**
│           - id: ms-students
│             uri: lb://MS-STUDENTS
│             predicates:
│               - Path=/api/v1/students/**, /api/v1/guardians/**
│           - id: ms-enrollments
│             uri: lb://MS-ENROLLMENTS
│             predicates:
│               - Path=/api/v1/enrollments/**, /api/v1/academic-periods/**
│           - id: ms-users
│             uri: lb://MS-USERS
│             predicates:
│               - Path=/api/v1/users/**
│           - id: ms-academic
│             uri: lb://MS-ACADEMIC
│             predicates:
│               - Path=/api/v1/courses/**, /api/v1/competencies/**,
│                       /api/v1/capacities/**, /api/v1/performances/**,
│                       /api/v1/catalog/**
│           - id: ms-civic-dates
│             uri: lb://MS-CIVIC-DATES
│             predicates:
│               - Path=/api/v1/events/**, /api/v1/calendars/**
│           - id: ms-notes
│             uri: lb://MS-NOTES
│             predicates:
│               - Path=/api/v1/evaluations/**, /api/v1/report-cards/**
│           - id: ms-assistance
│             uri: lb://MS-ASSISTANCE
│             predicates:
│               - Path=/api/v1/attendance/**, /api/v1/attendance-summary/**
│           - id: ms-disciplinary
│             uri: lb://MS-DISCIPLINARY
│             predicates:
│               - Path=/api/v1/behavior-records/**, /api/v1/incidents/**
│           - id: ms-psychology
│             uri: lb://MS-PSYCHOLOGY
│             predicates:
│               - Path=/api/v1/psychological-evaluations/**,
│                       /api/v1/special-needs-support/**
│           - id: ms-teacher-assignment
│             uri: lb://MS-TEACHER-ASSIGNMENT
│             predicates:
│               - Path=/api/v1/teacher-assignments/**,
│                       /api/v1/assignments-management/**
│           - id: ms-notifications
│             uri: lb://MS-NOTIFICATIONS
│             predicates:
│               - Path=/api/v1/notifications/**, /api/v1/templates/**
├── application-dev.yml
└── application-vpc.yml
```
---

## 📊 MAPA DE COMUNICACIÓN ENTRE MICROSERVICIOS

```
                                    ┌──────────────┐
                          ┌────────►│ INSTITUTION  │◄─────────────────────────┐
                          │         │    :9080     │                          │
                          │         └──────────────┘                          │
                          │              ▲  ▲  ▲                              │
              ┌───────────┤              │  │  │                              │
              │           │    ┌─────────┘  │  └──────────┐                  │
              │           │    │            │              │                  │
         ┌────┴─────┐ ┌──┴────┴──┐  ┌──────┴───┐  ┌──────┴──────┐  ┌──────┴──────┐
         │ STUDENTS │ │ENROLLMENTS│  │  USERS   │  │  ACADEMIC   │  │ CIVIC DATES │
         │  :9081   │ │  :9082   │  │  :9083   │  │   :9084     │  │   :9085     │
         └────┬─────┘ └──────────┘  └──────┬───┘  └─────────────┘  └─────────────┘
              │                             │
    ┌─────────┼─────────┬─────────┬────────┤
    │         │         │         │        │
┌───┴────┐ ┌─┴──────┐ ┌┴────────┐│ ┌──────┴──────┐
│ NOTES  │ │ASSIST. │ │DISCIPL. ││ │  TEACHER    │
│ :9086  │ │ :9087  │ │ :9088   ││ │  ASSIGN.    │
└────────┘ └────────┘ └─────────┘│ │   :9099     │
                                  │ └─────────────┘
                          ┌───────┴───────┐
                          │  PSYCHOLOGY   │
                          │    :9090      │
                          └───────────────┘

                    ─── RabbitMQ (eventos) ───▼

                          ┌───────────────┐
                          │ NOTIFICATIONS │ ◄── Consume eventos de TODOS
                          │    :9091      │
                          │  (Evolution)  │ ──► WhatsApp API
                          └───────────────┘
```

### Comunicación síncrona (WebClient) — Quién llama a quién

| MS que llama | MS que consulta |
|---|---|
| Students | Institution, Users |
| Enrollments | Institution, Students |
| Users | Institution |
| Academic | Institution |
| Civic Dates | Institution |
| Notes | Institution, Students, Academic |
| Assistance | Institution, Students |
| Disciplinary | Institution, Students, Users |
| Psychology | Institution, Students, Users |
| Teacher Assignment | Institution, Users |

### Comunicación asíncrona (RabbitMQ) — Eventos hacia Notifications

| MS que publica | Evento | Cola destino |
|---|---|---|
| Assistance | `attendance.absent`, `attendance.late` | `notification.attendance` |
| Notes | `grades.report_published` | `notification.grades` |
| Disciplinary | `incident.created`, `incident.resolved` | `notification.incidents` |
| Enrollments | `enrollment.confirmed` | `notification.enrollments` |
| Psychology | `evaluation.completed`, `follow_up.due` | `notification.psychology` |
| Institution | `announcement.created` | `notification.announcements` |

---

## 📏 REGLAS DE NOMENCLATURA

| Elemento | Convención | Ejemplo |
| -------- | ---------- | ------- |
| Paquete base | `pe.edu.vallegrande.sigei.<modulo>` | `pe.edu.vallegrande.sigei.institution` |
| Carpeta modelos | `domain/models/` (plural) | `models/Institution.java` |
| Carpeta enums | `domain/models/valueobjects/` | `valueobjects/Status.java` |
| Carpeta puertos | `domain/ports/` (plural) | `ports/in/`, `ports/out/` |
| Carpeta excepciones | `domain/exceptions/` (plural) | `exceptions/DomainException.java` |
| Entidad de dominio | PascalCase, sin sufijos | `Institution`, `Student` |
| Entidad de persistencia | PascalCase + `Entity` | `InstitutionEntity` |
| Repository (dominio, puerto out) | `I<Nombre>Repository` | `IInstitutionRepository` |
| Repository (R2DBC, infra) | `<Nombre>R2dbcRepository` | `InstitutionR2dbcRepository` |
| Adapter de persistencia | `<Nombre>RepositoryImpl` | `InstitutionRepositoryImpl` |
| Use Case (puerto in) | `I<Verbo><Nombre>UseCase` | `ICreateInstitutionUseCase` |
| Use Case (implementación) | `<Verbo><Nombre>UseCaseImpl` | `CreateInstitutionUseCaseImpl` |
| Controller (REST adapter) | `<Nombre>Rest` | `InstitutionRest` |
| Event Publisher (puerto out) | `I<Nombre>EventPublisher` | `INotesEventPublisher` |
| Event Publisher (impl) | `<Nombre>EventPublisherImpl` | `NotesEventPublisherImpl` |
| Mapper (aplicación) | `<Nombre>Mapper` | `InstitutionMapper` |
| Mapper (persistencia) | `<Nombre>PersistenceMapper` | `InstitutionPersistenceMapper` |
| DTO request | `<Verbo><Nombre>Request` | `CreateInstitutionRequest` |
| DTO response | `<Nombre>Response` | `InstitutionResponse` |
| DTO comunes | `application/dto/common/` | `ApiResponse.java`, `ErrorResponse.java` |
| Eventos de dominio | `application/events/` | `InstitutionCreatedEvent.java` |
| Excepciones base | `DomainException`, `NotFoundException`, `ConflictException` | Heredan todas las demás |
| Excepción not found | `<Nombre>NotFoundException` | `InstitutionNotFoundException` |
| Client externo (impl) | `<Nombre>ClientImpl` | `InstitutionClientImpl` |
| Carpeta clients | `adapters/out/external/` | `external/InstitutionClientImpl.java` |
| Carpeta persistence (infra) | `infrastructure/persistence/` | `entities/`, `mappers/`, `repositories/` |
| Tabla BD | snake_case, plural | `institutions`, `attendance_records` |
| Migración Flyway | `V<N>__<descripcion>.sql` | `V1__create_institutions_table.sql` |
| Endpoint base | `/api/v1/<recurso>` | `/api/v1/institutions` |

---

## 🔗 RELACIÓN CON OTROS DOCUMENTOS

| Documento | Relación |
|-----------|----------|
| [01_ARQUITECTURA_HEXAGONAL](01_ARQUITECTURA_HEXAGONAL_CORRECTA.md) | Define los principios que esta estructura implementa |
| [02_COMUNICACION](02_COMUNICACION_SINCRONA_ASINCRONA.md) | Detalle de WebClient (sync) y RabbitMQ (async) |
| [03_BASE_DE_DATOS](03_BASE_DE_DATOS_RECOMENDACION.md) | PostgreSQL + schema-per-service + Flyway |
| [04_API_GATEWAY](04_API_GATEWAY_Y_SERVICE_DISCOVERY.md) | Config detallada del Gateway |
| [05_ARQUITECTURA_BACKEND](05_ARQUITECTURA_BACKEND_COMPLETA.md) | Código de cada capa (dominio, application, infra) |
| [08_SEGURIDAD_KEYCLOAK](08_SEGURIDAD_KEYCLOAK.md) | SecurityConfig.java en cada MS |
| [09_API_RESPONSE](09_API_RESPONSE_Y_ERROR_RESPONSE.md) | ApiResponse + ErrorResponse en `infrastructure/common/` |
| [10_DESPLIEGUE_VPC](10_DESPLIEGUE_VPC.md) | Docker Compose y deploy de todos estos MS |
| [11_COMUNICACION_CAPAS](11_COMUNICACION_ENTRE_CAPAS.md) | Cómo fluye una request entre las carpetas |
