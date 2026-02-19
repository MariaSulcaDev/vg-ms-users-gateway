# 07 — PATRONES DE DISEÑO RECOMENDADOS

> Patrones para microservicios Java reactivos + frontend React aplicados al sistema SIGEI

---

## 📊 MAPA DE PATRONES

```
┌─────────────────────────────────────────────────────────────────┐
│                    PATRONES DE DISEÑO — SIGEI                   │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  CREACIONALES          ESTRUCTURALES         COMPORTAMIENTO     │
│  ├─ Factory Method     ├─ Adapter            ├─ Strategy        │
│  ├─ Builder            ├─ Facade             ├─ Observer        │
│  └─ Singleton*         ├─ Decorator          ├─ Template Method │
│                        └─ Composite          └─ Chain of Resp.  │
│                                                                 │
│  MICROSERVICIOS        DATOS                 RESILIENCIA        │
│  ├─ API Gateway        ├─ Repository         ├─ Circuit Breaker │
│  ├─ Service Discovery  ├─ CQRS               ├─ Retry           │
│  ├─ Saga               ├─ Event Sourcing*    ├─ Bulkhead        │
│  ├─ BFF                └─ Unit of Work       └─ Timeout         │
│  └─ Strangler Fig                                               │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
  * = Futuro / Opcional
```

---

## 1️⃣ REPOSITORY PATTERN

> **Dónde:** Capa `domain/port/out/` → Implementación en `infrastructure/adapter/out/persistence/`

### Problema actual

```java
// ❌ ACTUAL — El servicio accede directamente al repositorio de Spring Data
@Service
public class StudentServiceImpl implements StudentService {
    @Autowired
    private StudentRepository repository;  // ← Interfaz de Spring Data directamente

    public Mono<Student> save(Student student) {
        return repository.save(student);  // ← Student tiene @Document (acoplado a MongoDB)
    }
}
```

### Solución con Repository Pattern

```java
// ✅ PROPUESTO — Puerto del dominio (interfaz pura)
package pe.edu.vallegrande.sigei.student.domain.port.out;

public interface StudentRepository {
    Mono<Student> save(Student student);          // Student es POJO puro
    Mono<Student> findById(String id);
    Flux<Student> findByInstitutionId(String institutionId);
    Mono<Boolean> existsByDni(String dni);
}

// ✅ Adaptador de Infraestructura (implementación)
package pe.edu.vallegrande.sigei.student.infrastructure.adapter.out.persistence;

@Component
public class StudentPersistenceAdapter implements StudentRepository {

    private final MongoStudentRepository mongoRepo;
    private final StudentPersistenceMapper mapper;

    @Override
    public Mono<Student> save(Student student) {
        StudentDocument doc = mapper.toDocument(student);
        return mongoRepo.save(doc).map(mapper::toDomain);
    }

    @Override
    public Mono<Student> findById(String id) {
        return mongoRepo.findById(id).map(mapper::toDomain);
    }
}
```

**Beneficio:** Si mañana migras de MongoDB a PostgreSQL, SOLO cambias el adaptador. El dominio no se toca.

---

## 2️⃣ FACTORY METHOD PATTERN

> **Dónde:** `domain/model/` — Creación controlada de entidades de dominio

### Problema actual

```java
// ❌ ACTUAL — Constructor público, sin validaciones
Student student = new Student();
student.setName("Juan");
student.setStatus("A");     // ← String mágico
// ↑ Se puede crear un Student inválido
```

### Solución con Factory Method

```java
// ✅ PROPUESTO
public class Enrollment {
    private String id;
    private String studentId;
    private String academicPeriodId;
    private String sectionId;
    private EnrollmentStatus status;
    private EnrollmentType type;
    private LocalDateTime enrollmentDate;

    // Constructor privado
    private Enrollment() {}

    // Factory Method — NUEVA matrícula
    public static Enrollment createNew(
            String studentId,
            String academicPeriodId,
            String sectionId,
            EnrollmentType type) {

        Objects.requireNonNull(studentId, "studentId es obligatorio");
        Objects.requireNonNull(academicPeriodId, "academicPeriodId es obligatorio");
        Objects.requireNonNull(sectionId, "sectionId es obligatorio");

        Enrollment enrollment = new Enrollment();
        enrollment.id = UUID.randomUUID().toString();
        enrollment.studentId = studentId;
        enrollment.academicPeriodId = academicPeriodId;
        enrollment.sectionId = sectionId;
        enrollment.status = EnrollmentStatus.PENDING;  // Siempre inicia pendiente
        enrollment.type = type;
        enrollment.enrollmentDate = LocalDateTime.now();
        return enrollment;
    }

    // Factory Method — REINGRESO
    public static Enrollment createReentry(
            String studentId,
            String academicPeriodId,
            String sectionId,
            String previousEnrollmentId) {

        Enrollment enrollment = createNew(
            studentId, academicPeriodId, sectionId, EnrollmentType.REINGRESO);
        // Lógica adicional para reingreso...
        return enrollment;
    }

    // Factory Method — Reconstituir desde persistencia
    public static Enrollment reconstitute(
            String id, String studentId, String academicPeriodId,
            String sectionId, EnrollmentStatus status, EnrollmentType type,
            LocalDateTime enrollmentDate) {
        Enrollment e = new Enrollment();
        e.id = id;
        e.studentId = studentId;
        e.academicPeriodId = academicPeriodId;
        e.sectionId = sectionId;
        e.status = status;
        e.type = type;
        e.enrollmentDate = enrollmentDate;
        return e;
    }

    // Comportamiento de dominio
    public void approve() {
        if (this.status != EnrollmentStatus.PENDING) {
            throw new IllegalStateException(
                "Solo se puede aprobar una matrícula pendiente");
        }
        this.status = EnrollmentStatus.APPROVED;
    }

    public void cancel(String reason) {
        if (this.status == EnrollmentStatus.CANCELLED) {
            throw new IllegalStateException("La matrícula ya está cancelada");
        }
        this.status = EnrollmentStatus.CANCELLED;
    }
}
```

---

## 3️⃣ STRATEGY PATTERN

> **Dónde:** Validaciones y calificaciones que varían según nivel educativo

### Caso: Sistema de calificación para nivel INICIAL

> **Nota:** Dado que SIGEI es exclusivamente para colegios privados de nivel INICIAL, solo se necesita UNA estrategia de calificación. Sin embargo, el patrón Strategy se mantiene para que sea extensible si en el futuro se añaden otros niveles.

```java
// Interfaz de estrategia
public interface GradingStrategy {
    GradeResult evaluate(double score);
    boolean isApproved(GradeResult result);
    String getScaleDescription();
}

// Estrategia para Nivel INICIAL (evaluación cualitativa)
// Es la Única estrategia necesaria actualmente.
public class InitialLevelGradingStrategy implements GradingStrategy {

    // En nivel inicial (MINEDU), la evaluación es literal:
    // AD = Logro Destacado, A = Logrado, B = En Proceso, C = En Inicio

    @Override
    public GradeResult evaluate(double score) {
        if (score >= 18) return new GradeResult("AD", "Logro Destacado");
        if (score >= 14) return new GradeResult("A", "Logrado");
        if (score >= 11) return new GradeResult("B", "En Proceso");
        return new GradeResult("C", "En Inicio");
    }

    @Override
    public boolean isApproved(GradeResult result) {
        return "AD".equals(result.literal()) || "A".equals(result.literal());
    }

    @Override
    public String getScaleDescription() {
        return "Escala cualitativa: AD, A, B, C (RVM N° 094-2020-MINEDU)";
    }
}

// Factory — Por ahora solo devuelve InitialLevel,
// pero si se agregan más niveles en el futuro, se registran aquí.
@Component
public class GradingStrategyFactory {

    private static final Map<EducationLevel, GradingStrategy> STRATEGIES = Map.of(
        EducationLevel.INICIAL, new InitialLevelGradingStrategy()
    );

    public GradingStrategy getStrategy(EducationLevel level) {
        return Optional.ofNullable(STRATEGIES.get(level))
            .orElseThrow(() -> new IllegalArgumentException(
                "No hay estrategia de calificación para nivel: " + level));
    }
}

// Uso en el servicio
@Service
public class GradeService {

    private final GradingStrategyFactory strategyFactory;

    public Mono<Grade> recordGrade(String studentId, String courseId,
                                    double score, EducationLevel level) {
        GradingStrategy strategy = strategyFactory.getStrategy(level);
        GradeResult result = strategy.evaluate(score);

        Grade grade = Grade.create(studentId, courseId, score, result);
        return gradeRepository.save(grade);
    }
}
```

---

## 4️⃣ OBSERVER / EVENT PATTERN

> **Dónde:** Comunicación asíncrona entre microservicios via RabbitMQ

```java
// === EVENTO DE DOMINIO ===
public record EnrollmentCreatedEvent(
    String enrollmentId,
    String studentId,
    String institutionId,
    String academicPeriodId,
    String sectionId,
    LocalDateTime occurredAt
) {
    public static EnrollmentCreatedEvent from(Enrollment enrollment) {
        return new EnrollmentCreatedEvent(
            enrollment.getId(),
            enrollment.getStudentId(),
            enrollment.getInstitutionId(),
            enrollment.getAcademicPeriodId(),
            enrollment.getSectionId(),
            LocalDateTime.now()
        );
    }
}

// === PUBLISHER (en vg-ms-enrollments) ===
@Component
public class EnrollmentEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public Mono<Void> publishEnrollmentCreated(EnrollmentCreatedEvent event) {
        return Mono.fromRunnable(() ->
            rabbitTemplate.convertAndSend(
                "enrollment.exchange",
                "enrollment.created",
                event
            )
        );
    }
}

// === LISTENER (en vg-ms-attendance — crea registro de asistencia) ===
@Component
public class EnrollmentEventListener {

    private final AttendanceRepository attendanceRepository;

    @RabbitListener(queues = "attendance.enrollment.created.queue")
    public void onEnrollmentCreated(EnrollmentCreatedEvent event) {
        // Crear registros de asistencia para el nuevo estudiante matriculado
        attendanceRepository.initializeForStudent(
            event.studentId(),
            event.sectionId(),
            event.academicPeriodId()
        ).subscribe();
    }
}

// === LISTENER (en vg-ms-notes — inicializa boleta) ===
@Component
public class EnrollmentGradeListener {

    @RabbitListener(queues = "grades.enrollment.created.queue")
    public void onEnrollmentCreated(EnrollmentCreatedEvent event) {
        // Crear estructura de boleta vacía para el estudiante
        gradeRepository.initializeReportCard(
            event.studentId(),
            event.academicPeriodId(),
            event.sectionId()
        ).subscribe();
    }
}
```

---

## 5️⃣ SAGA PATTERN

> **Dónde:** Operaciones distribuidas que abarcan múltiples microservicios

### Caso: Proceso de Matrícula (involucra 4 microservicios)

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│  Enrollments │────→│   Students   │────→│  Attendance  │────→│    Notes     │
│              │     │              │     │              │     │              │
│ 1. Crear     │     │ 2. Validar   │     │ 3. Iniciar   │     │ 4. Crear     │
│    matrícula │     │    estudiante│     │    registros │     │    boleta    │
│              │     │              │     │              │     │              │
│  Si falla ←──│─────│── Compensar ←│─────│── Compensar ←│─────│── Compensar  │
│  anular todo │     │  revertir    │     │  eliminar    │     │  eliminar    │
└──────────────┘     └──────────────┘     └──────────────┘     └──────────────┘
```

```java
// === ORQUESTADOR DE SAGA (en vg-ms-enrollments) ===
@Service
public class EnrollmentSagaOrchestrator {

    private final EnrollmentRepository enrollmentRepo;
    private final StudentServiceClient studentClient;
    private final EventPublisher eventPublisher;

    public Mono<Enrollment> executeEnrollmentSaga(CreateEnrollmentCommand cmd) {
        return Mono.defer(() -> {

            // PASO 1: Validar que el estudiante existe y está activo
            return studentClient.validateStudent(cmd.studentId())

            // PASO 2: Verificar que no exista matrícula duplicada
            .then(enrollmentRepo.existsByStudentAndPeriod(
                cmd.studentId(), cmd.academicPeriodId()))
            .flatMap(exists -> {
                if (exists) {
                    return Mono.error(new DuplicateEnrollmentException(
                        "El estudiante ya tiene matrícula en este período"));
                }
                return Mono.empty();
            })

            // PASO 3: Crear la matrícula
            .then(Mono.fromCallable(() ->
                Enrollment.createNew(
                    cmd.studentId(),
                    cmd.academicPeriodId(),
                    cmd.sectionId(),
                    cmd.type())))
            .flatMap(enrollmentRepo::save)

            // PASO 4: Publicar evento (asistencia y notas reaccionan)
            .flatMap(enrollment ->
                eventPublisher.publishEnrollmentCreated(
                    EnrollmentCreatedEvent.from(enrollment))
                .thenReturn(enrollment))

            // COMPENSACIÓN: Si algo falla, revertir
            .onErrorResume(error -> {
                log.error("Saga de matrícula falló: {}", error.getMessage());
                return compensateEnrollment(cmd.studentId(), cmd.academicPeriodId())
                    .then(Mono.error(error));
            });
        });
    }

    private Mono<Void> compensateEnrollment(String studentId, String periodId) {
        return enrollmentRepo.deleteByStudentAndPeriod(studentId, periodId)
            .then(eventPublisher.publishEnrollmentCancelled(
                new EnrollmentCancelledEvent(studentId, periodId)));
    }
}
```

---

## 6️⃣ ADAPTER PATTERN

> **Dónde:** `infrastructure/adapter/out/client/` — Comunicación con otros microservicios

```java
// === Puerto del dominio (interfaz genérica) ===
package pe.edu.vallegrande.sigei.enrollment.domain.port.out;

public interface StudentServiceClient {
    Mono<StudentInfo> getStudentInfo(String studentId);
    Mono<Boolean> validateStudentExists(String studentId);
}

// === Adaptador WebClient (implementación) ===
package pe.edu.vallegrande.sigei.enrollment.infrastructure.adapter.out.client;

@Component
public class StudentWebClientAdapter implements StudentServiceClient {

    private final WebClient webClient;
    private final CircuitBreakerFactory cbFactory;

    public StudentWebClientAdapter(
            @LoadBalanced WebClient.Builder webClientBuilder,
            CircuitBreakerFactory cbFactory) {
        this.webClient = webClientBuilder
            .baseUrl("http://vg-ms-students")  // Eureka resolve
            .build();
        this.cbFactory = cbFactory;
    }

    @Override
    public Mono<StudentInfo> getStudentInfo(String studentId) {
        return webClient.get()
            .uri("/api/v1/students/{id}", studentId)
            .retrieve()
            .bodyToMono(StudentInfoDto.class)
            .map(this::toDomain)
            .transform(CircuitBreakerOperator.of(
                cbFactory.create("studentService")))
            .timeout(Duration.ofSeconds(5))
            .onErrorResume(ex -> {
                log.warn("No se pudo obtener info de estudiante {}: {}",
                    studentId, ex.getMessage());
                return Mono.error(new ServiceUnavailableException(
                    "Servicio de estudiantes no disponible"));
            });
    }

    @Override
    public Mono<Boolean> validateStudentExists(String studentId) {
        return getStudentInfo(studentId)
            .map(info -> true)
            .onErrorReturn(false);
    }

    private StudentInfo toDomain(StudentInfoDto dto) {
        return new StudentInfo(dto.getId(), dto.getFullName(), dto.getDni());
    }
}
```

---

## 7️⃣ BUILDER PATTERN

> **Dónde:** DTOs complejos y queries con muchos filtros

```java
// Builder para queries complejas de matrícula
public class EnrollmentQuery {
    private String institutionId;
    private String academicPeriodId;
    private String sectionId;
    private EnrollmentStatus status;
    private EducationLevel level;
    private String studentDni;
    private int page;
    private int size;
    private String sortBy;
    private String sortDirection;

    private EnrollmentQuery() {}

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final EnrollmentQuery query = new EnrollmentQuery();

        public Builder institutionId(String id) { query.institutionId = id; return this; }
        public Builder period(String id) { query.academicPeriodId = id; return this; }
        public Builder section(String id) { query.sectionId = id; return this; }
        public Builder status(EnrollmentStatus s) { query.status = s; return this; }
        public Builder level(EducationLevel l) { query.level = l; return this; }
        public Builder studentDni(String dni) { query.studentDni = dni; return this; }
        public Builder page(int p) { query.page = p; return this; }
        public Builder size(int s) { query.size = Math.min(s, 100); return this; }
        public Builder sortBy(String f) { query.sortBy = f; return this; }
        public Builder ascending() { query.sortDirection = "ASC"; return this; }
        public Builder descending() { query.sortDirection = "DESC"; return this; }

        public EnrollmentQuery build() {
            if (query.page < 0) query.page = 0;
            if (query.size <= 0) query.size = 20;
            if (query.sortBy == null) query.sortBy = "enrollmentDate";
            if (query.sortDirection == null) query.sortDirection = "DESC";
            return query;
        }
    }
}

// Uso limpio
EnrollmentQuery query = EnrollmentQuery.builder()
    .institutionId("inst-001")
    .period("2025-1")
    .status(EnrollmentStatus.APPROVED)
    .level(EducationLevel.INICIAL)
    .page(0)
    .size(50)
    .descending()
    .build();
```

---

## 8️⃣ CHAIN OF RESPONSIBILITY

> **Dónde:** Validaciones de matrícula en cadena

```java
// Interfaz de validador
public interface EnrollmentValidator {
    Mono<Void> validate(CreateEnrollmentCommand command);
}

// Validador 1: Período académico abierto
@Component
@Order(1)
public class AcademicPeriodOpenValidator implements EnrollmentValidator {
    private final AcademicPeriodRepository periodRepo;

    @Override
    public Mono<Void> validate(CreateEnrollmentCommand cmd) {
        return periodRepo.findById(cmd.academicPeriodId())
            .switchIfEmpty(Mono.error(new NotFoundException("Período no encontrado")))
            .flatMap(period -> {
                if (!period.isOpen()) {
                    return Mono.error(new AcademicPeriodClosedException(
                        "El período académico no está abierto para matrículas"));
                }
                return Mono.empty();
            });
    }
}

// Validador 2: Estudiante no duplicado
@Component
@Order(2)
public class DuplicateEnrollmentValidator implements EnrollmentValidator {
    private final EnrollmentRepository enrollmentRepo;

    @Override
    public Mono<Void> validate(CreateEnrollmentCommand cmd) {
        return enrollmentRepo.existsByStudentAndPeriod(
            cmd.studentId(), cmd.academicPeriodId())
            .flatMap(exists -> exists
                ? Mono.error(new DuplicateEnrollmentException("Matrícula duplicada"))
                : Mono.empty());
    }
}

// Validador 3: Capacidad de sección
@Component
@Order(3)
public class SectionCapacityValidator implements EnrollmentValidator {
    private final EnrollmentRepository enrollmentRepo;
    private final SectionRepository sectionRepo;

    @Override
    public Mono<Void> validate(CreateEnrollmentCommand cmd) {
        return Mono.zip(
            sectionRepo.findById(cmd.sectionId()),
            enrollmentRepo.countBySectionAndPeriod(cmd.sectionId(), cmd.academicPeriodId())
        ).flatMap(tuple -> {
            var section = tuple.getT1();
            var currentCount = tuple.getT2();
            if (currentCount >= section.getCapacity()) {
                return Mono.error(new SectionCapacityExceededException(
                    "La sección ha alcanzado su capacidad máxima de " + section.getCapacity()));
            }
            return Mono.empty();
        });
    }
}

// Cadena de validación
@Component
public class EnrollmentValidationChain {

    private final List<EnrollmentValidator> validators;

    public EnrollmentValidationChain(List<EnrollmentValidator> validators) {
        this.validators = validators; // Spring inyecta en orden @Order
    }

    public Mono<Void> validate(CreateEnrollmentCommand command) {
        return Flux.fromIterable(validators)
            .concatMap(validator -> validator.validate(command))
            .then();
    }
}
```

---

## 9️⃣ CIRCUIT BREAKER + RETRY + TIMEOUT (Resiliencia)

> **Dónde:** Comunicación síncrona entre microservicios

```yaml
# application.yml — Resilience4j
resilience4j:
  circuitbreaker:
    instances:
      studentService:
        sliding-window-size: 10           # Ventana de 10 llamadas
        failure-rate-threshold: 50        # Si 50% falla → abre circuito
        wait-duration-in-open-state: 30s  # Espera 30s antes de intentar
        permitted-number-of-calls-in-half-open-state: 3

  retry:
    instances:
      studentService:
        max-attempts: 3
        wait-duration: 500ms
        exponential-backoff-multiplier: 2  # 500ms, 1s, 2s
        retry-exceptions:
          - java.io.IOException
          - java.util.concurrent.TimeoutException
        ignore-exceptions:
          - pe.edu.vallegrande.sigei.enrollment.domain.exception.BusinessRuleViolationException

  timelimiter:
    instances:
      studentService:
        timeout-duration: 5s
```

```java
// Uso en código
@Component
public class ResilientStudentClient implements StudentServiceClient {

    private final WebClient webClient;

    @CircuitBreaker(name = "studentService", fallbackMethod = "studentFallback")
    @Retry(name = "studentService")
    @TimeLimiter(name = "studentService")
    @Override
    public Mono<StudentInfo> getStudentInfo(String studentId) {
        return webClient.get()
            .uri("/api/v1/students/{id}", studentId)
            .retrieve()
            .bodyToMono(StudentInfo.class);
    }

    // Fallback cuando el circuito está abierto
    private Mono<StudentInfo> studentFallback(String studentId, Throwable t) {
        log.warn("Fallback para estudiante {}: {}", studentId, t.getMessage());
        // Retornar datos de caché, o información parcial
        return cachedStudentService.getCachedStudent(studentId)
            .switchIfEmpty(Mono.error(new ServiceUnavailableException(
                "Servicio de estudiantes temporalmente no disponible")));
    }
}
```

---

## 🔟 PATRONES DEL FRONTEND (React)

### Custom Hook Pattern (Encapsula lógica reutilizable)

```typescript
// Hook que encapsula toda la lógica de paginación + filtros + búsqueda
function useDataTable<T>(
  queryKey: string,
  fetchFn: (params: DataTableParams) => Promise<PaginatedResponse<T>>,
  initialFilters: Partial<DataTableParams> = {}
) {
  const [params, setParams] = useState<DataTableParams>({
    page: 0,
    size: 20,
    sortBy: 'createdAt',
    sortDir: 'desc',
    search: '',
    ...initialFilters,
  });

  const query = useQuery({
    queryKey: [queryKey, params],
    queryFn: () => fetchFn(params),
    placeholderData: (prev) => prev,
  });

  const setPage = (page: number) => setParams(p => ({ ...p, page }));
  const setPageSize = (size: number) => setParams(p => ({ ...p, size, page: 0 }));
  const setSearch = useDebounce((search: string) =>
    setParams(p => ({ ...p, search, page: 0 })), 300);
  const setSort = (sortBy: string) => setParams(p => ({
    ...p, sortBy,
    sortDir: p.sortBy === sortBy && p.sortDir === 'asc' ? 'desc' : 'asc',
  }));

  return { ...query, params, setPage, setPageSize, setSearch, setSort };
}

// Uso
function StudentListPage() {
  const {
    data, isLoading, params,
    setPage, setPageSize, setSearch, setSort,
  } = useDataTable('students', studentService.getAll, {
    sortBy: 'lastName',
  });

  return (
    <div>
      <SearchInput onChange={setSearch} />
      <Table
        data={data?.content ?? []}
        loading={isLoading}
        onSort={setSort}
        sortBy={params.sortBy}
        sortDir={params.sortDir}
      />
      <Pagination
        page={params.page}
        totalPages={data?.totalPages ?? 0}
        onPageChange={setPage}
      />
    </div>
  );
}
```

### Compound Component Pattern (Componentes complejos)

```typescript
// Formulario de matrícula paso a paso
function EnrollmentWizard({ onComplete }: { onComplete: (data: Enrollment) => void }) {
  return (
    <Wizard onComplete={onComplete}>
      <Wizard.Step title="Seleccionar Estudiante">
        <StudentSelector />
      </Wizard.Step>
      <Wizard.Step title="Período y Sección">
        <PeriodSectionSelector />
      </Wizard.Step>
      <Wizard.Step title="Documentos">
        <DocumentUploader />
      </Wizard.Step>
      <Wizard.Step title="Confirmación">
        <EnrollmentSummary />
      </Wizard.Step>
    </Wizard>
  );
}
```

### Render Props / HOC Pattern (Permisos)

```typescript
// Componente que verifica permisos
function RoleGuard({
  allowedRoles,
  children,
  fallback = null
}: {
  allowedRoles: UserRole[];
  children: React.ReactNode;
  fallback?: React.ReactNode;
}) {
  const { hasRole } = useAuth();

  if (!hasRole(allowedRoles)) {
    return fallback;
  }

  return <>{children}</>;
}

// Uso
function StudentActions({ student }: { student: Student }) {
  return (
    <div>
      <RoleGuard allowedRoles={['ADMIN', 'DIRECTOR', 'SECRETARIA']}>
        <Button onClick={handleEdit}>Editar</Button>
      </RoleGuard>

      <RoleGuard allowedRoles={['ADMIN']}>
        <Button variant="destructive" onClick={handleDelete}>
          Eliminar
        </Button>
      </RoleGuard>
    </div>
  );
}
```

---

## 📋 RESUMEN — Qué patrón usar y dónde

| Patrón | Microservicio | Ubicación | Propósito |
|--------|-----------|-----------|-----------|
| **Repository** | TODOS | domain/port/out → infrastructure/adapter/out | Desacoplar persistencia |
| **Factory Method** | TODOS | domain/model/ | Crear entidades válidas |
| **Strategy** | notes, grades | domain/ | Calificación por nivel educativo |
| **Observer/Events** | enrollments → attendance, notes | infrastructure/messaging | Comunicación asíncrona |
| **Saga** | enrollments | application/service/ | Matrícula distribuida |
| **Adapter** | TODOS | infrastructure/adapter/ | Adaptadores REST, DB, messaging |
| **Builder** | enrollments, academic | application/dto/ | Queries complejas |
| **Chain of Responsibility** | enrollments | application/service/ | Validaciones en cadena |
| **Circuit Breaker** | TODOS (inter-servicio) | infrastructure/adapter/out/client | Resiliencia |
| **Custom Hooks** | Frontend | features/*/hooks/ | Lógica reutilizable |
| **Compound Components** | Frontend | features/*/components/ | UI compleja |
| **RoleGuard** | Frontend | core/auth/ + features/ | Control de acceso |

---

## 📋 PRIORIZACIÓN DE IMPLEMENTACIÓN

```
FASE 1 (Semana 1-2) — Fundamentos:
  ✅ Repository Pattern en todos los MS
  ✅ Factory Method en entidades de dominio
  ✅ Adapter Pattern para persistencia

FASE 2 (Semana 3-4) — Comunicación:
  ✅ Observer/Events con RabbitMQ
  ✅ Circuit Breaker + Retry
  ✅ Saga para matrícula

FASE 3 (Semana 5-6) — Especialización:
  ✅ Strategy para calificaciones
  ✅ Chain of Responsibility para validaciones
  ✅ Builder para queries complejas

FASE 4 (Semana 7-8) — Frontend:
  ✅ Custom Hooks (useDataTable, useDebounce)
  ✅ Compound Components (Wizard)
  ✅ RoleGuard y ProtectedRoute
```

---

> **Fin de la documentación de arquitectura para SIGEI.**
>
> **Archivos generados:**
>
> - `00_ANALISIS_PROFUNDO_ESTADO_ACTUAL.md` — Auditoría completa (35 hallazgos)
> - `01_ARQUITECTURA_HEXAGONAL_CORRECTA.md` — Guía de hexagonal + DDD
> - `02_COMUNICACION_SINCRONA_ASINCRONA.md` — Estrategia sync/async
> - `03_BASE_DE_DATOS_RECOMENDACION.md` — PostgreSQL + multi-tenancy
> - `04_API_GATEWAY_Y_SERVICE_DISCOVERY.md` — Gateway + Eureka
> - `05_ARQUITECTURA_BACKEND_COMPLETA.md` — Estructura backend completa
> - `06_ARQUITECTURA_FRONTEND_COMPLETA.md` — Estructura frontend completa
> - `07_PATRONES_DISENO_RECOMENDADOS.md` — 10+ patrones con código
