# 11 — COMUNICACIÓN ENTRE CAPAS (Arquitectura Hexagonal)

> **Objetivo:** Explicar cómo fluye una petición a través de las capas de la arquitectura hexagonal, cómo se comunican entre sí, y por qué las dependencias van siempre hacia adentro.
> **Prerrequisito:** Haber leído [01_ARQUITECTURA_HEXAGONAL_CORRECTA.md](01_ARQUITECTURA_HEXAGONAL_CORRECTA.md)

---

## 🎯 LAS 3 CAPAS Y SU PROPÓSITO

```
┌─────────────────────────────────────────────────────────────┐
│                    INFRASTRUCTURE                           │
│                (Adaptadores — el "cómo")                    │
│                                                             │
│   Adaptadores de ENTRADA         Adaptadores de SALIDA     │
│   ┌───────────────────┐         ┌───────────────────────┐  │
│   │ REST Controller   │         │ Persistence Adapter   │  │
│   │ (recibe HTTP)     │         │ (guarda en BD)        │  │
│   └────────┬──────────┘         └───────────▲───────────┘  │
│            │                                │               │
│ ═══════════╪════════════════════════════════╪═══════════════│
│            │         APPLICATION            │               │
│            │      (Orquestación)            │               │
│            ▼                                │               │
│   ┌───────────────────┐                    │               │
│   │ UseCaseImpl       │                    │               │
│   │ (orquesta el      │────────────────────┘               │
│   │  caso de uso)     │                                    │
│   └────────┬──────────┘                                    │
│            │                                               │
│ ═══════════╪═══════════════════════════════════════════════ │
│            │          DOMAIN                               │
│            │     (Reglas de negocio)                        │
│            ▼                                               │
│   ┌───────────────────┐    ┌───────────────────┐          │
│   │ Model/Entity      │    │ Port (Interface)  │          │
│   │ (Institution,     │    │ (contratos)       │          │
│   │  Student, etc.)   │    │                   │          │
│   └───────────────────┘    └───────────────────┘          │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 🔑 REGLA DE ORO: Las dependencias van hacia ADENTRO

```
Infrastructure → depende de → Application → depende de → Domain

Domain NO depende de NADIE (es el núcleo puro)
```

Esto significa:

- **Domain** no importa nada de Spring, ni de MongoDB, ni de PostgreSQL.
- **Application** solo conoce interfaces (ports) del dominio.
- **Infrastructure** implementa todo lo concreto (BD, HTTP, mensajería).

---

## 📡 FLUJO COMPLETO DE UNA PETICIÓN

### Ejemplo: `POST /api/institutions` — Crear una institución

```
PASO 1                    PASO 2                    PASO 3
────────────────         ────────────────          ────────────────
INFRASTRUCTURE           APPLICATION               DOMAIN
(Adaptador IN)           (UseCaseImpl)             (Modelo)

HTTP Request  ───►  InstitutionRest
                    │
                    │ Convierte DTO → Domain
                    │ (usando Mapper)
                    ▼
              ICreateInstitutionUseCase ◄── Puerto de ENTRADA (interface)
                    │
                    │ CreateInstitutionUseCaseImpl
                    │ (implementa el caso de uso)
                    │
                    ├─► Institution.create(...)  ◄── Lógica de dominio
                    │   (valida código modular,     (reglas de negocio)
                    │    nombre, etc.)
                    │
                    ├─► IInstitutionRepository  ◄── Puerto de SALIDA (interface)
                    │   .save(institution)          (definido en dominio)
                    │
              ──────┼──────────────────────────────
                    │
PASO 4              ▼
────────────────
INFRASTRUCTURE
(Adaptador OUT)

InstitutionRepositoryImpl
    │ implementa IInstitutionRepository
    │
    │ Convierte Domain → Entity
    │ (usando PersistenceMapper)
    ▼
InstitutionR2dbcRepository.save(entity)
    │
    ▼
 PostgreSQL
```

---

## 🧩 CÓDIGO PASO A PASO — Cómo se comunica cada capa

### PASO 1: Controller (Infrastructure → Application)

```java
// CAPA: infrastructure/adapters/in/rest/
// FUNCIÓN: Recibir HTTP, convertir DTO, delegar al caso de uso

@RestController
@RequestMapping("/api/institutions")
public class InstitutionRest {

    private final ICreateInstitutionUseCase createUseCase;
    private final InstitutionMapper mapper;

    public InstitutionRest(ICreateInstitutionUseCase createUseCase,
                           InstitutionMapper mapper) {
        this.createUseCase = createUseCase;
        this.mapper = mapper;
    }

    @PostMapping
    public Mono<ResponseEntity<ApiResponse<InstitutionResponse>>> create(
            @Valid @RequestBody CreateInstitutionRequest request) {

        Institution institution = mapper.toDomain(request);

        return createUseCase.execute(institution)
            .map(mapper::toResponse)
            .map(resp -> ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created(resp, "Institución creada")));
    }
}
```

**¿Qué conoce el Controller (Rest)?**

- ✅ `ICreateInstitutionUseCase` (interfaz del dominio — puerto de entrada)
- ✅ `CreateInstitutionRequest` / `InstitutionResponse` (DTOs de aplicación)
- ✅ `ApiResponse` (wrapper en application/dto/common)
- ❌ NO conoce `CreateInstitutionUseCaseImpl` (la implementación concreta)
- ❌ NO conoce `InstitutionR2dbcRepository` ni `InstitutionEntity`

---

### PASO 2: Puerto de Entrada — Use Case (Dominio define el contrato)

```java
// CAPA: domain/ports/in/
// FUNCIÓN: Definir QUÉ se puede hacer (no CÓMO)

public interface ICreateInstitutionUseCase {

    Mono<Institution> execute(Institution institution);
}
```

**¿Por qué es una interfaz con prefijo `I`?**

- El dominio dice "necesito poder crear instituciones" (QUÉ)
- La capa de aplicación decide CÓMO implementarlo
- El controller solo conoce esta interfaz, no la clase concreta
- El prefijo `I` hace explícito que es interfaz → `ICreateInstitutionUseCase` → `CreateInstitutionUseCaseImpl`

---

### PASO 3: UseCaseImpl — Implementa el Use Case (Application)

```java
// CAPA: application/usecases/
// FUNCIÓN: Orquestar la lógica, coordinar dominio + puertos de salida
// REGLA: 1 clase = 1 caso de uso (Single Responsibility Principle)

@Service
public class CreateInstitutionUseCaseImpl implements ICreateInstitutionUseCase {

    private final IInstitutionRepository repository;
    private final IInstitutionEventPublisher eventPublisher;

    public CreateInstitutionUseCaseImpl(IInstitutionRepository repository,
                                        IInstitutionEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Mono<Institution> execute(Institution institution) {

        return repository.findByModularCode(institution.getModularCode())
            .flatMap(existing -> Mono.<Institution>error(
                new DuplicateModularCodeException(institution.getModularCode())))

            .switchIfEmpty(repository.save(institution))

            .doOnSuccess(saved ->
                eventPublisher.publish(new InstitutionCreatedEvent(
                    saved.getId(), saved.getName())));
    }
}
```

**¿Qué conoce el UseCaseImpl?**

- ✅ `Institution` (modelo de dominio)
- ✅ `IInstitutionRepository` (interfaz del dominio — puerto de salida)
- ✅ `IInstitutionEventPublisher` (interfaz del dominio — puerto de salida)
- ✅ Excepciones de dominio (`DuplicateModularCodeException`)
- ❌ NO conoce `InstitutionR2dbcRepository`, `InstitutionEntity`, ni `@Table`
- ❌ NO conoce `InstitutionRest`, `ApiResponse`, ni HTTP

---

### PASO 4: Puerto de Salida — Repository (Dominio define el contrato)

```java
// CAPA: domain/ports/out/
// FUNCIÓN: Definir QUÉ necesita el dominio de persistencia (no CÓMO)

public interface IInstitutionRepository {

    Mono<Institution> save(Institution institution);
    Mono<Institution> findById(String id);
    Mono<Institution> findByModularCode(String modularCode);
    Flux<Institution> findAll();
    Mono<Void> deleteById(String id);
}
```

**¿Por qué está en el dominio?**

- El dominio dice "necesito guardar y buscar instituciones" (QUÉ)
- La infraestructura decide si usa MongoDB, PostgreSQL, API externa, etc. (CÓMO)
- Si mañana cambias de MongoDB a PostgreSQL, el dominio NO se modifica

---

### PASO 5: Adaptador de Persistencia (Infrastructure implementa el puerto)

```java
// CAPA: infrastructure/adapters/out/persistence/
// FUNCIÓN: Implementar el puerto de salida usando tecnología concreta

@Component
public class InstitutionRepositoryImpl implements IInstitutionRepository {

    private final InstitutionR2dbcRepository r2dbcRepository;
    private final InstitutionPersistenceMapper mapper;

    public InstitutionRepositoryImpl(
            InstitutionR2dbcRepository r2dbcRepository,
            InstitutionPersistenceMapper mapper) {
        this.r2dbcRepository = r2dbcRepository;
        this.mapper = mapper;
    }

    @Override
    public Mono<Institution> save(Institution institution) {
        InstitutionEntity entity = mapper.toEntity(institution);
        return r2dbcRepository.save(entity)
            .map(mapper::toDomain);
    }

    @Override
    public Mono<Institution> findById(String id) {
        return r2dbcRepository.findById(id)
            .map(mapper::toDomain);
    }

    @Override
    public Mono<Institution> findByModularCode(String modularCode) {
        return r2dbcRepository.findByModularCode(modularCode)
            .map(mapper::toDomain);
    }

    @Override
    public Flux<Institution> findAll() {
        return r2dbcRepository.findAll()
            .map(mapper::toDomain);
    }

    @Override
    public Mono<Void> deleteById(String id) {
        return r2dbcRepository.deleteById(id);
    }
}
```

**¿Qué conoce el RepositoryImpl?**

- ✅ `IInstitutionRepository` (interfaz del dominio que implementa)
- ✅ `Institution` (modelo del dominio)
- ✅ `InstitutionR2dbcRepository` (Spring Data R2DBC — tecnología)
- ✅ `InstitutionEntity` (entidad con `@Table` — tecnología)
- ✅ `InstitutionPersistenceMapper` (convierte Domain ↔ Entity)
- ❌ NO conoce al UseCaseImpl ni al Controller

---

## 🔄 DIAGRAMA DE DEPENDENCIAS (Imports)

```
┌──────────────────────────────────────────────────────────────────┐
│                                                                  │
│  InstitutionRest                                                │
│  ├── import ICreateInstitutionUseCase ←── (domain/ports/in)      │
│  ├── import InstitutionMapper         ←── (application/mappers)  │
│  ├── import CreateInstitutionRequest  ←── (application/dto)      │
│  ├── import InstitutionResponse       ←── (application/dto)      │
│  └── import ApiResponse               ←── (application/dto/common)│
│                                                                  │
│  CreateInstitutionUseCaseImpl                                    │
│  ├── import ICreateInstitutionUseCase ←── (domain/ports/in)      │
│  ├── import IInstitutionRepository    ←── (domain/ports/out)     │
│  ├── import Institution               ←── (domain/models)        │
│  └── import DuplicateModularCodeEx.   ←── (domain/exceptions)    │
│                                                                  │
│  InstitutionRepositoryImpl                                       │
│  ├── import IInstitutionRepository    ←── (domain/ports/out)     │
│  ├── import Institution               ←── (domain/models)        │
│  ├── import InstitutionEntity         ←── (infrastructure)       │
│  └── import InstitutionR2dbcRepo      ←── (infrastructure)       │
│                                                                  │
│  Institution (DOMINIO)                                           │
│  └── import NADA externo              ←── (0 dependencias)      │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

**Observa:**

- `Institution.java` NO importa nada de Spring, R2DBC, ni PostgreSQL.
- `CreateInstitutionUseCaseImpl` solo importa interfaces (ports) y modelos del dominio.
- Solo `InstitutionRepositoryImpl` importa cosas de R2DBC/Spring Data.

---

## 🔌 ¿CÓMO SE CONECTAN? — Inyección de Dependencias (Spring)

Spring Boot conecta todo automáticamente gracias a `@Component`, `@Service`, etc.:

```
Spring IoC Container:
│
├── Busca: ¿Quién implementa ICreateInstitutionUseCase?
│   └── Encuentra: CreateInstitutionUseCaseImpl (@Service)
│       └── Inyecta en InstitutionRest
│
├── Busca: ¿Quién implementa IInstitutionRepository?
│   └── Encuentra: InstitutionRepositoryImpl (@Component)
│       └── Inyecta en CreateInstitutionUseCaseImpl
│
└── Resultado:
    Rest → UseCaseImpl → RepositoryImpl
    (pero cada uno SOLO conoce la INTERFAZ del anterior)
```

```java
// Spring ve esto en tiempo de ejecución:

// 1. Controller pide CreateInstitutionUseCase
//    → Spring le da InstitutionService

// 2. InstitutionService pide InstitutionRepository
//    → Spring le da InstitutionPersistenceAdapter

// 3. InstitutionPersistenceAdapter pide MongoInstitutionRepository
//    → Spring Data crea una implementación automática
```

---

## 🗺️ MAPPERS — Los traductores entre capas

Cada capa tiene su propia representación de los datos. Los Mappers traducen entre ellos:

```
HTTP Request                Domain Model              Persistence Document
(DTO de entrada)           (Objeto puro)              (Entidad de BD)

CreateInstitutionRequest  →  Institution             →  InstitutionDocument
{                            {                           {
  "name": "IE ...",           id: null,                   _id: ObjectId(...),
  "modularCode": "123",      modularCode: "1234567",     modularCode: "1234567",
  "address": {...}            name: "IE ...",             name: "IE ...",
}                             status: ACTIVE,             status: "ACTIVE",
                              createdAt: now()            createdAt: ISODate(...)
                            }                           }

         ▲                        ▲                         ▲
         │                        │                         │
   InstitutionMapper       (objeto en memoria)     PersistenceMapper
   (application/mapper)                            (infrastructure/mapper)
         │                        │                         │
         ▼                        ▼                         ▼

InstitutionResponse     ←  Institution             ←  InstitutionDocument
{                            (retorno)                  (lectura de BD)
  "id": "abc-123",
  "name": "IE ...",
  "status": "ACTIVE"
}
```

### Código de los Mappers

```java
// ─── Mapper de Aplicación (DTO ↔ Domain) ───
// CAPA: application/mapper/

@Component
public class InstitutionMapper {

    /** DTO Request → Domain Model */
    public Institution toDomain(CreateInstitutionRequest request) {
        return Institution.create(
            request.modularCode(),
            request.name(),
            Address.of(request.department(), request.province(), request.district())
        );
    }

    /** Domain Model → DTO Response */
    public InstitutionResponse toResponse(Institution institution) {
        return new InstitutionResponse(
            institution.getId(),
            institution.getModularCode(),
            institution.getName(),
            institution.getStatus().name(),
            institution.getCreatedAt()
        );
    }
}

// ─── Mapper de Persistencia (Domain ↔ Document) ───
// CAPA: infrastructure/adapter/out/persistence/mapper/

@Component
public class InstitutionPersistenceMapper {

    /** Domain Model → Persistence Document */
    public InstitutionDocument toDocument(Institution institution) {
        InstitutionDocument doc = new InstitutionDocument();
        doc.setId(institution.getId());
        doc.setModularCode(institution.getModularCode());
        doc.setName(institution.getName());
        doc.setStatus(institution.getStatus().name());
        doc.setCreatedAt(institution.getCreatedAt());
        doc.setUpdatedAt(institution.getUpdatedAt());
        return doc;
    }

    /** Persistence Document → Domain Model */
    public Institution toDomain(InstitutionDocument doc) {
        return Institution.reconstitute(    // ← factory diferente, sin validaciones
            doc.getId(),
            doc.getModularCode(),
            doc.getName(),
            InstitutionStatus.valueOf(doc.getStatus()),
            doc.getCreatedAt(),
            doc.getUpdatedAt()
        );
    }
}
```

---

## ❓ PREGUNTAS FRECUENTES

### ¿Por qué no pongo @Document directo en Institution.java?

```
❌ INCORRECTO (lo que tienen hoy):
@Document(collection = "institutions")
public class Institution {
    @Id
    private String id;
    ...
}

Problema: El dominio "sabe" que usa MongoDB.
Si cambias a PostgreSQL, tienes que modificar tu modelo de negocio.
```

```
✅ CORRECTO (hexagonal):
// domain/model/Institution.java — CERO anotaciones de BD
public class Institution {
    private String id;
    ...
}

// infrastructure/persistence/InstitutionDocument.java — aquí van las anotaciones
@Document(collection = "institutions")
public class InstitutionDocument {
    @Id
    private String id;
    ...
}
```

### ¿Controller puede llamar directo al Repository?

```
❌ NUNCA:
Controller → Repository     (salta la lógica de negocio)

✅ SIEMPRE:
Controller → UseCase → Service → Repository
```

Si el controller llama directo al repository, estás haciendo una API CRUD sin reglas de negocio. Cualquier validación se pierde.

### ¿El Service puede retornar un DTO?

```
❌ INCORRECTO:
public Mono<InstitutionResponse> execute(...) {
    // El service conoce DTOs de HTTP → acoplamiento
}

✅ CORRECTO:
public Mono<Institution> execute(...) {
    // El service retorna objetos de DOMINIO
    // El controller/mapper convierte a DTO
}
```

### ¿Dónde pongo las validaciones?

```
┌────────────────────────────────────────────────────────────┐
│ TIPO DE VALIDACIÓN           │ DÓNDE VA                   │
├──────────────────────────────┼────────────────────────────│
│ Formato (email, longitud)    │ DTO Request (@Valid)        │
│ Regla de negocio simple      │ Domain Model (constructor)  │
│ Regla que necesita BD        │ Application Service         │
│ (ej: "código no duplicado")  │ (usa Repository para        │
│                              │  verificar)                 │
└────────────────────────────────────────────────────────────┘
```

---

## 📊 RESUMEN VISUAL — Quién conoce a quién

```
                    CONOCE                  NO CONOCE
                    ──────                  ─────────
Controller      →   UseCase (interfaz)      Service (clase concreta)
                    DTO Request/Response    Document, @Document
                    ApiResponse             MongoDB, PostgreSQL
                    Mapper de aplicación

Service         →   Domain Model            Controller
                    Port In (interfaz)      DTO Request/Response
                    Port Out (interfaz)     ApiResponse
                    Excepciones dominio     @Document, @Table

PersistenceAdap →   Port Out (interfaz)     Controller
                    Domain Model            Service
                    Document/Entity         DTO Request/Response
                    Spring Data Repository  ApiResponse

Domain Model    →   NADA externo            Spring, MongoDB, R2DBC
                    Solo Java puro          HTTP, JSON, REST
                    Value Objects propios   Annotations de frameworks
```

---

## 🔗 RELACIÓN CON OTROS DOCUMENTOS

| Documento | Relación |
|-----------|----------|
| [01_ARQUITECTURA_HEXAGONAL](01_ARQUITECTURA_HEXAGONAL_CORRECTA.md) | Define la estructura completa de la hexagonal |
| [05_ARQUITECTURA_BACKEND](05_ARQUITECTURA_BACKEND_COMPLETA.md) | Estructura de carpetas que refleja estas capas |
| [07_PATRONES_DISEÑO](07_PATRONES_DISENO_RECOMENDADOS.md) | Patrones que se aplican dentro de cada capa |
| [09_API_RESPONSE](09_API_RESPONSE_Y_ERROR_RESPONSE.md) | ApiResponse vive SOLO en infrastructure, el dominio no lo conoce |
