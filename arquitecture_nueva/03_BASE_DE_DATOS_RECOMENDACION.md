# 03 — RECOMENDACIÓN DE BASE DE DATOS PARA SIGEI

> **Contexto:** Sistema MULTI-INSTITUCIONAL para colegios PRIVADOS de nivel inicial en Perú
> **Escala:** Colegios privados de nivel inicial, cientos de instituciones, miles de estudiantes

---

## 📊 ANÁLISIS DEL CONTEXTO

### Contexto del sistema — Colegios Privados de Nivel Inicial en Perú

- Sector: **Educación privada** — colegios particulares de nivel inicial
- Nivel educativo: **Solo INICIAL** (niños de 3 a 5 años)
- Escala: Cientos de instituciones privadas, miles de estudiantes
- Regulado por UGEL (Unidad de Gestión Educativa Local) y DRE (Dirección Regional de Educación)

### Características del sistema SIGEI

- **Multi-tenant:** Cada institución es un "tenant" con sus datos
- **Datos estructurados:** Matrículas, notas, asistencia son altamente relacionales
- **Datos semi-estructurados:** Evaluaciones psicológicas, documentos de salud
- **Alta concurrencia temporal:** Período de matrículas (febrero-marzo)
- **Consultas complejas:** Reportes cruzados entre asistencia, notas, comportamiento
- **Auditoría requerida:** Sector educativo privado, necesita trazabilidad completa

---

## 🔴 PROBLEMAS ACTUALES DE BASE DE DATOS

### 1. Mezcla sin estrategia: MongoDB + PostgreSQL

| Motor | Microservicios | Problema |
|-------|---------------|----------|
| MongoDB Atlas | institution, students, users | **Comparten la MISMA base "SIGEI"** → Viola database-per-service |
| PostgreSQL Neon | Los demás 8 microservicios | 6 instancias diferentes, algunas compartidas |

### 2. MongoDB mal usado para datos relacionales

```
Students (MongoDB)
├── institutionId → referencia a Institution (MongoDB diferente colección)
├── classroomId → referencia a Classroom (MongoDB, sub-documento en Institution)
└── guardians[] → array embebido con userId → referencia a Users (MongoDB)

⚠️ Esto es un modelo RELACIONAL implementado en MongoDB.
   No aprovecha las ventajas de MongoDB y pierde las de SQL.
```

### 3. Sin consistencia transaccional

- MongoDB no garantiza transacciones entre colecciones por defecto
- Crear estudiante + crear usuario son 2 operaciones separadas sin transacción

### 4. Sin migraciones de esquema

- No se usa Flyway ni Liquibase
- Los scripts SQL están "sueltos" (`MIGRACION_BASE_DATOS.sql`)
- No hay versionamiento del esquema

---

## ✅ RECOMENDACIÓN: PostgreSQL COMO BASE DE DATOS PRINCIPAL

### ¿Por qué PostgreSQL para TODOS los microservicios?

| Criterio | PostgreSQL | MongoDB |
|----------|-----------|---------|
| **Integridad de datos** | ✅ ACID completo, FK, constraints | ⚠️ Eventual consistency |
| **Multi-tenancy** | ✅ Row Level Security (RLS), schemas | ⚠️ Requiere configuración manual |
| **Relaciones complejas** | ✅ JOINs nativos, eficientes | ❌ $lookup costoso |
| **Reportes y consultas** | ✅ SQL es el estándar, CTEs, Window Functions | ⚠️ Aggregation Pipeline complejo |
| **Datos semi-estructurados** | ✅ JSONB nativo (lo mejor de ambos mundos) | ✅ Nativo |
| **Auditoría** | ✅ Triggers, extensiones de auditoría | ⚠️ Requiere implementación manual |
| **Costo operativo** | ✅ Menor, más DBAs conocen SQL | ⚠️ Mayor, menos expertise |
| **Ecosistema R2DBC** | ✅ r2dbc-postgresql maduro | ⚠️ reactive-mongo tiene limitaciones |
| **Regulación pública** | ✅ SQL es estándar en gobierno peruano | ⚠️ No es estándar |
| **Migraciones** | ✅ Flyway/Liquibase nativos | ⚠️ Mongock (menos maduro) |

### ¿Y los datos semi-estructurados?

PostgreSQL con **JSONB** maneja perfectamente los datos que actualmente están en MongoDB:

```sql
-- Institution con datos semi-estructurados en JSONB
CREATE TABLE institutions (
    institution_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code_institution VARCHAR(20) UNIQUE NOT NULL,
    institution_name VARCHAR(200) NOT NULL,
    modular_code VARCHAR(20),
    institution_type VARCHAR(50),
    institution_level VARCHAR(50) DEFAULT 'INICIAL',
    gender VARCHAR(20),

    -- Datos semi-estructurados en JSONB ← Lo mejor de ambos mundos
    address JSONB NOT NULL DEFAULT '{}',
    contact_methods JSONB DEFAULT '[]',
    schedules JSONB DEFAULT '[]',

    grading_type VARCHAR(50),
    classroom_type VARCHAR(50),
    ugel VARCHAR(100),
    dre VARCHAR(100),
    director_id UUID,

    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);

-- Student con JSONB para datos complejos
CREATE TABLE students (
    student_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cui VARCHAR(12) UNIQUE NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    date_of_birth DATE NOT NULL,
    address TEXT,
    photo_url TEXT,

    -- Datos complejos en JSONB
    personal_info JSONB DEFAULT '{}',
    health_info JSONB DEFAULT '{}',
    development_info JSONB DEFAULT '{}',

    institution_id UUID NOT NULL REFERENCES institutions(institution_id),
    classroom_id UUID REFERENCES classrooms(classroom_id),

    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Índices GIN para búsqueda eficiente en JSONB
CREATE INDEX idx_institutions_address ON institutions USING GIN (address);
CREATE INDEX idx_students_health ON students USING GIN (health_info);
```

---

## 🏗️ ESTRATEGIA DATABASE-PER-SERVICE

### Opción Recomendada: Un PostgreSQL, schemas separados

```
┌──────────────────────────────────────────────────────┐
│                PostgreSQL Server                      │
│                                                      │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐   │
│  │   schema:   │ │   schema:   │ │   schema:   │   │
│  │ institution │ │  students   │ │   users     │   │
│  │             │ │             │ │             │   │
│  │ institutions│ │  students   │ │  users      │   │
│  │ classrooms  │ │  guardians  │ │             │   │
│  └─────────────┘ └─────────────┘ └─────────────┘   │
│                                                      │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐   │
│  │   schema:   │ │   schema:   │ │   schema:   │   │
│  │ enrollments │ │  academic   │ │   notes     │   │
│  │             │ │             │ │             │   │
│  │ enrollments │ │  courses    │ │  courses    │   │
│  │ academic_   │ │  competency │ │  evaluations│   │
│  │  periods    │ │  capacity   │ │  report_card│   │
│  └─────────────┘ │  performance│ └─────────────┘   │
│                   └─────────────┘                    │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐   │
│  │   schema:   │ │   schema:   │ │   schema:   │   │
│  │ assistance  │ │disciplinary │ │ psychology  │   │
│  │             │ │             │ │             │   │
│  │ attendance  │ │  incidents  │ │ evaluations │   │
│  │ att_summary │ │  behavior   │ │ special_    │   │
│  └─────────────┘ │  _records   │ │  needs      │   │
│                   └─────────────┘ └─────────────┘   │
│                                                      │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐   │
│  │   schema:   │ │   schema:   │ │   schema:   │   │
│  │ teacher_    │ │ civic_dates │ │notifications│   │
│  │ assignment  │ │             │ │             │   │
│  │ assignments │ │  events     │ │notification │   │
│  │ schedules   │ │  calendars  │ │  templates  │   │
│  └─────────────┘ └─────────────┘ └─────────────┘   │
│                                                      │
└──────────────────────────────────────────────────────┘
```

### Configuración R2DBC por esquema

```yaml
# application.yml — Cada microservicio usa su propio schema
spring:
  r2dbc:
    url: r2dbc:postgresql://${DB_HOST:localhost}:5432/${DB_NAME:sigei}?schema=${DB_SCHEMA:students}
    username: ${DB_USER:sigei_students}
    password: ${DB_PASS}
    pool:
      initial-size: 5
      max-size: 20
      max-idle-time: 30m
```

```sql
-- Crear usuarios por microservicio (aislamiento de acceso)
CREATE USER sigei_institution WITH PASSWORD 'xxx';
CREATE USER sigei_students WITH PASSWORD 'xxx';
CREATE USER sigei_enrollments WITH PASSWORD 'xxx';
-- ...

-- Asignar permisos SOLO al schema correspondiente
GRANT USAGE ON SCHEMA institution TO sigei_institution;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA institution TO sigei_institution;
-- El usuario de students NO puede acceder a schema de institution
```

---

## 🌐 MULTI-TENANCY CON ROW LEVEL SECURITY (RLS)

Para un sistema multi-institucional, PostgreSQL RLS es ideal:

```sql
-- Habilitar RLS en tablas que contienen datos por institución
ALTER TABLE students.students ENABLE ROW LEVEL SECURITY;

-- Política: cada usuario de la app solo ve datos de su institución
CREATE POLICY institution_isolation ON students.students
    USING (institution_id = current_setting('app.current_institution_id')::UUID);

-- Al conectar, el API Gateway setea el tenant:
SET app.current_institution_id = '550e8400-e29b-41d4-a716-446655440000';

-- Automáticamente, cualquier SELECT solo devuelve datos de ESA institución
SELECT * FROM students; -- Solo devuelve estudiantes de la institución seteada
```

---

## 📊 ESQUEMAS SQL RECOMENDADOS POR MICROSERVICIO

### Schema: `institution`

```sql
CREATE SCHEMA IF NOT EXISTS institution;

CREATE TABLE institution.institutions (
    institution_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code_institution VARCHAR(20) UNIQUE NOT NULL,
    modular_code VARCHAR(20) UNIQUE,
    institution_name VARCHAR(200) NOT NULL,
    institution_type VARCHAR(50) NOT NULL,
    institution_level VARCHAR(50) DEFAULT 'INICIAL',
    gender VARCHAR(20),
    slogan TEXT,
    logo_url TEXT,
    address JSONB NOT NULL DEFAULT '{}',
    contact_methods JSONB DEFAULT '[]',
    schedules JSONB DEFAULT '[]',
    grading_type VARCHAR(50),
    classroom_type VARCHAR(50),
    ugel VARCHAR(100),
    dre VARCHAR(100),
    director_id UUID,
    status VARCHAR(20) DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);

CREATE TABLE institution.classrooms (
    classroom_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL REFERENCES institution.institutions(institution_id),
    classroom_name VARCHAR(100) NOT NULL,
    classroom_age VARCHAR(20) NOT NULL,
    capacity INTEGER CHECK (capacity > 0 AND capacity <= 30),
    color VARCHAR(20),
    status VARCHAR(20) DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_classrooms_institution ON institution.classrooms(institution_id);
CREATE INDEX idx_institutions_status ON institution.institutions(status);
CREATE INDEX idx_institutions_ugel ON institution.institutions(ugel);
```

### Schema: `students`

```sql
CREATE SCHEMA IF NOT EXISTS students;

CREATE TABLE students.students (
    student_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cui VARCHAR(12) UNIQUE NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    date_of_birth DATE NOT NULL,
    gender VARCHAR(10),
    address TEXT,
    photo_url TEXT,
    personal_info JSONB DEFAULT '{}',
    health_info JSONB DEFAULT '{}',
    development_info JSONB DEFAULT '{}',
    institution_id UUID NOT NULL,
    classroom_id UUID,
    status VARCHAR(20) DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE', 'TRANSFERRED', 'GRADUATED')),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE students.guardians (
    guardian_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id UUID NOT NULL REFERENCES students.students(student_id),
    user_id UUID,
    relationship VARCHAR(20) NOT NULL CHECK (relationship IN ('PADRE', 'MADRE', 'TUTOR', 'OTRO')),
    names VARCHAR(200) NOT NULL,
    last_names VARCHAR(200) NOT NULL,
    document_type VARCHAR(10),
    document_number VARCHAR(20),
    phone VARCHAR(20),
    is_primary BOOLEAN DEFAULT false,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_students_institution ON students.students(institution_id);
CREATE INDEX idx_students_classroom ON students.students(classroom_id);
CREATE INDEX idx_students_status ON students.students(status);
CREATE INDEX idx_guardians_student ON students.guardians(student_id);
```

### Schema: `enrollments`

```sql
CREATE SCHEMA IF NOT EXISTS enrollments;

CREATE TABLE enrollments.academic_periods (
    period_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    academic_year SMALLINT NOT NULL,
    period_name VARCHAR(100) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    enrollment_start DATE NOT NULL,
    enrollment_end DATE NOT NULL,
    allow_late_enrollment BOOLEAN DEFAULT false,
    late_enrollment_end DATE,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT unique_period UNIQUE (institution_id, academic_year, period_name)
);

CREATE TABLE enrollments.enrollments (
    enrollment_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id UUID NOT NULL,
    institution_id UUID NOT NULL,
    classroom_id UUID NOT NULL,
    academic_period_id UUID NOT NULL REFERENCES enrollments.academic_periods(period_id),
    enrollment_date TIMESTAMPTZ DEFAULT NOW(),
    enrollment_type VARCHAR(20) DEFAULT 'NUEVA' CHECK (enrollment_type IN ('NUEVA', 'REINSCRIPCION', 'TRASLADO')),
    enrollment_status VARCHAR(20) DEFAULT 'PENDING' CHECK (enrollment_status IN ('PENDING', 'ACTIVE', 'INACTIVE', 'CANCELLED')),
    age_group VARCHAR(20),
    shift VARCHAR(20),
    section VARCHAR(10),
    modality VARCHAR(30),
    educational_level VARCHAR(30) DEFAULT 'INICIAL',
    previous_institution TEXT,
    observations TEXT,
    documents JSONB DEFAULT '{}',
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT unique_enrollment UNIQUE (student_id, academic_period_id)
);

CREATE INDEX idx_enrollments_student ON enrollments.enrollments(student_id);
CREATE INDEX idx_enrollments_institution ON enrollments.enrollments(institution_id);
CREATE INDEX idx_enrollments_period ON enrollments.enrollments(academic_period_id);
```

---

## 🔧 MIGRACIONES CON FLYWAY

```xml
<!-- pom.xml — Agregar Flyway a CADA microservicio -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-postgresql</artifactId>
</dependency>
```

```yaml
# application.yml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    schemas: students
    baseline-on-migrate: true
```

```
src/main/resources/db/migration/
├── V1__create_students_table.sql
├── V2__create_guardians_table.sql
├── V3__add_indexes.sql
└── V4__add_audit_columns.sql
```

---

## 🏗️ OPCIÓN DE INFRAESTRUCTURA

### Para Desarrollo y Staging

- **Supabase** (PostgreSQL gestionado, gratis para desarrollo, tiene RLS nativo)
- O **Neon** (lo que ya usan, pero con schemas separados)

### Para Producción

- **Amazon RDS PostgreSQL** o **Azure Database for PostgreSQL**
- Opciones peruanas: servidores en la nube del gobierno peruano (SGDI - PCM)
- **Backups automáticos**, réplicas de lectura, alta disponibilidad

---

## 📊 RESUMEN DE MIGRACIÓN

| Microservicio | Antes | Después | Cambio |
|--------------|-------|---------|--------|
| institution | MongoDB Atlas | PostgreSQL schema `institution` | Migrar colecciones a tablas + JSONB |
| students | MongoDB Atlas (compartida) | PostgreSQL schema `students` | Migrar, separar guardians a tabla |
| users | MongoDB Atlas (compartida) | PostgreSQL schema `users` | Migrar colección a tabla |
| enrollments | PostgreSQL Neon | PostgreSQL schema `enrollments` | Refactorizar tablas, normalizar |
| academic | PostgreSQL Neon | PostgreSQL schema `academic` | Mantener R2DBC, agregar Flyway |
| notes | PostgreSQL Neon | PostgreSQL schema `notes` | Normalizar esquema |
| assistance | PostgreSQL Neon | PostgreSQL schema `assistance` | Mantener, agregar FK references |
| disciplinary | PostgreSQL Neon | PostgreSQL schema `disciplinary` | Mantener, normalizar |
| psychology | PostgreSQL Neon | PostgreSQL schema `psychology` | Mantener, normalizar |
| teacher-assignment | PostgreSQL Neon | PostgreSQL schema `teacher_assignment` | Mantener |
| civic-dates | PostgreSQL Neon | PostgreSQL schema `civic_dates` | Mantener |
| notifications | No existe | PostgreSQL schema `notifications` | Crear desde cero |

---

> **Siguiente:** Ver `04_API_GATEWAY_Y_SERVICE_DISCOVERY.md` para la estrategia de API Gateway y Eureka.
