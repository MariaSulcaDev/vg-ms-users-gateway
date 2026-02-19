# 06 — ARQUITECTURA FRONTEND COMPLETA

> React 19 + TypeScript + Tailwind CSS 4 + Vite — Estructura modular escalable

---

## 📊 ESTADO ACTUAL VS PROPUESTO

| Aspecto | Actual | Propuesto |
|---------|--------|-----------|
| API Base URLs | 11 URLs diferentes (una por MS) | 1 sola URL (API Gateway) |
| State Management | Ninguno (prop drilling) | Zustand (ligero, reactivo) |
| Caché de datos | Ninguno | TanStack Query (React Query) |
| Autenticación | No existe | JWT + Context + Protected Routes |
| Manejo de errores | Sin estándar | Error Boundaries + Toast global |
| Estructura | `modules/{modulo}/` plana | Feature-based con capas claras |
| Formularios | Sin validación estándar | React Hook Form + Zod |
| Testing | Ninguno | Vitest + Testing Library |
| API Layer | axios directo en cada componente | Capa de abstracción centralizada |

---

## 📁 ESTRUCTURA DE CARPETAS PROPUESTA

```
vg-web-sigei/
├── index.html
├── package.json
├── tsconfig.json
├── tsconfig.app.json
├── tsconfig.node.json
├── vite.config.ts
├── eslint.config.js
├── .env.development                ← Variables de entorno dev
├── .env.production                 ← Variables de entorno prod
├── .env.example                    ← Template de variables
│
├── public/
│   ├── favicon.ico
│   └── assets/
│       ├── images/
│       └── fonts/
│
├── src/
│   ├── main.tsx                    ← Entry point
│   ├── App.tsx                     ← Router principal
│   ├── vite-env.d.ts
│   │
│   ├── core/                       ← 🟢 NÚCLEO — Compartido por toda la app
│   │   ├── api/                    ← Capa de abstracción HTTP
│   │   │   ├── apiClient.ts        ← Instancia axios configurada
│   │   │   ├── interceptors.ts     ← Request/Response interceptors
│   │   │   └── endpoints.ts        ← Constantes de rutas API
│   │   │
│   │   ├── auth/                   ← Autenticación
│   │   │   ├── AuthContext.tsx
│   │   │   ├── AuthProvider.tsx
│   │   │   ├── useAuth.ts
│   │   │   ├── ProtectedRoute.tsx
│   │   │   ├── RoleGuard.tsx
│   │   │   └── authService.ts
│   │   │
│   │   ├── hooks/                  ← Hooks compartidos
│   │   │   ├── useDebounce.ts
│   │   │   ├── usePagination.ts
│   │   │   ├── useLocalStorage.ts
│   │   │   └── useMediaQuery.ts
│   │   │
│   │   ├── types/                  ← Tipos globales
│   │   │   ├── api.types.ts        ← ApiResponse<T>, PaginatedResponse<T>
│   │   │   ├── auth.types.ts       ← User, Role, AuthState
│   │   │   └── common.types.ts     ← SelectOption, TableColumn, etc.
│   │   │
│   │   ├── utils/                  ← Utilidades
│   │   │   ├── formatters.ts       ← Fechas, moneda, DNI
│   │   │   ├── validators.ts       ← Validaciones comunes
│   │   │   ├── constants.ts        ← Constantes globales
│   │   │   └── storage.ts          ← localStorage wrapper
│   │   │
│   │   └── store/                  ← Estado global (Zustand)
│   │       ├── useAppStore.ts      ← Store general (sidebar, theme)
│   │       └── useNotificationStore.ts
│   │
│   ├── shared/                     ← 🟡 COMPONENTES COMPARTIDOS (UI Library)
│   │   ├── components/
│   │   │   ├── ui/                 ← Componentes base (atomos)
│   │   │   │   ├── Button.tsx
│   │   │   │   ├── Input.tsx
│   │   │   │   ├── Select.tsx
│   │   │   │   ├── Modal.tsx
│   │   │   │   ├── Table.tsx
│   │   │   │   ├── Card.tsx
│   │   │   │   ├── Badge.tsx
│   │   │   │   ├── Spinner.tsx
│   │   │   │   ├── Toast.tsx
│   │   │   │   └── index.ts        ← Barrel export
│   │   │   │
│   │   │   ├── layout/             ← Layout de la aplicación
│   │   │   │   ├── MainLayout.tsx
│   │   │   │   ├── Sidebar.tsx
│   │   │   │   ├── Header.tsx
│   │   │   │   ├── Footer.tsx
│   │   │   │   └── Breadcrumb.tsx
│   │   │   │
│   │   │   ├── feedback/           ← Componentes de feedback
│   │   │   │   ├── ErrorBoundary.tsx
│   │   │   │   ├── LoadingScreen.tsx
│   │   │   │   ├── EmptyState.tsx
│   │   │   │   └── ConfirmDialog.tsx
│   │   │   │
│   │   │   └── form/               ← Componentes de formulario
│   │   │       ├── FormField.tsx
│   │   │       ├── SearchInput.tsx
│   │   │       ├── DatePicker.tsx
│   │   │       ├── FileUpload.tsx
│   │   │       └── FormSection.tsx
│   │   │
│   │   └── styles/
│   │       └── index.css           ← Tailwind + custom styles
│   │
│   ├── features/                   ← 🔴 FEATURES (Módulos de negocio)
│   │   │
│   │   ├── auth/                   ← Módulo: Autenticación
│   │   │   ├── pages/
│   │   │   │   ├── LoginPage.tsx
│   │   │   │   ├── ForgotPasswordPage.tsx
│   │   │   │   └── ResetPasswordPage.tsx
│   │   │   ├── components/
│   │   │   │   ├── LoginForm.tsx
│   │   │   │   └── PasswordStrengthIndicator.tsx
│   │   │   └── index.ts
│   │   │
│   │   ├── institutions/           ← Módulo: Gestión de Instituciones
│   │   │   ├── types/
│   │   │   │   └── institution.types.ts
│   │   │   ├── services/
│   │   │   │   └── institutionService.ts
│   │   │   ├── hooks/
│   │   │   │   ├── useInstitutions.ts       ← TanStack Query hook
│   │   │   │   └── useInstitutionForm.ts
│   │   │   ├── components/
│   │   │   │   ├── InstitutionTable.tsx
│   │   │   │   ├── InstitutionForm.tsx
│   │   │   │   ├── InstitutionCard.tsx
│   │   │   │   ├── InstitutionFilters.tsx
│   │   │   │   └── ClassroomList.tsx
│   │   │   ├── pages/
│   │   │   │   ├── InstitutionListPage.tsx
│   │   │   │   ├── InstitutionDetailPage.tsx
│   │   │   │   └── InstitutionFormPage.tsx
│   │   │   ├── routes.tsx
│   │   │   └── index.ts
│   │   │
│   │   ├── students/               ← Módulo: Gestión de Estudiantes
│   │   │   ├── types/
│   │   │   │   └── student.types.ts
│   │   │   ├── services/
│   │   │   │   └── studentService.ts
│   │   │   ├── hooks/
│   │   │   │   ├── useStudents.ts
│   │   │   │   └── useStudentForm.ts
│   │   │   ├── components/
│   │   │   │   ├── StudentTable.tsx
│   │   │   │   ├── StudentForm.tsx
│   │   │   │   ├── StudentProfile.tsx
│   │   │   │   ├── GuardianForm.tsx
│   │   │   │   └── StudentFilters.tsx
│   │   │   ├── pages/
│   │   │   │   ├── StudentListPage.tsx
│   │   │   │   ├── StudentDetailPage.tsx
│   │   │   │   └── StudentFormPage.tsx
│   │   │   ├── routes.tsx
│   │   │   └── index.ts
│   │   │
│   │   ├── enrollments/            ← Módulo: Matrículas
│   │   │   ├── types/
│   │   │   │   └── enrollment.types.ts
│   │   │   ├── services/
│   │   │   │   └── enrollmentService.ts
│   │   │   ├── hooks/
│   │   │   │   └── useEnrollments.ts
│   │   │   ├── components/
│   │   │   │   ├── EnrollmentWizard.tsx    ← Proceso paso a paso
│   │   │   │   ├── EnrollmentTable.tsx
│   │   │   │   ├── AcademicPeriodSelect.tsx
│   │   │   │   └── SectionAssignment.tsx
│   │   │   ├── pages/
│   │   │   │   ├── EnrollmentListPage.tsx
│   │   │   │   ├── EnrollmentProcessPage.tsx
│   │   │   │   └── AcademicPeriodPage.tsx
│   │   │   ├── routes.tsx
│   │   │   └── index.ts
│   │   │
│   │   ├── academic/               ← Módulo: Gestión Académica
│   │   │   ├── types/
│   │   │   │   └── academic.types.ts
│   │   │   ├── services/
│   │   │   │   └── academicService.ts
│   │   │   ├── hooks/
│   │   │   │   └── useAcademic.ts
│   │   │   ├── components/
│   │   │   │   ├── CourseTable.tsx
│   │   │   │   ├── CompetencyTree.tsx
│   │   │   │   ├── CurriculumBuilder.tsx
│   │   │   │   └── CatalogManager.tsx
│   │   │   ├── pages/
│   │   │   │   ├── CourseListPage.tsx
│   │   │   │   ├── CompetencyPage.tsx
│   │   │   │   └── CatalogPage.tsx
│   │   │   ├── routes.tsx
│   │   │   └── index.ts
│   │   │
│   │   ├── grades/                 ← Módulo: Notas y Calificaciones
│   │   │   ├── types/
│   │   │   │   └── grade.types.ts
│   │   │   ├── services/
│   │   │   │   └── gradeService.ts
│   │   │   ├── hooks/
│   │   │   │   └── useGrades.ts
│   │   │   ├── components/
│   │   │   │   ├── GradeSheet.tsx          ← Registro de notas
│   │   │   │   ├── ReportCard.tsx          ← Boleta
│   │   │   │   ├── GradeInput.tsx          ← Input AD/A/B/C
│   │   │   │   └── GradeSummary.tsx
│   │   │   ├── pages/
│   │   │   │   ├── GradeEntryPage.tsx
│   │   │   │   ├── ReportCardPage.tsx
│   │   │   │   └── GradeReportPage.tsx
│   │   │   ├── routes.tsx
│   │   │   └── index.ts
│   │   │
│   │   ├── attendance/             ← Módulo: Asistencia
│   │   │   ├── types/
│   │   │   │   └── attendance.types.ts
│   │   │   ├── services/
│   │   │   │   └── attendanceService.ts
│   │   │   ├── hooks/
│   │   │   │   └── useAttendance.ts
│   │   │   ├── components/
│   │   │   │   ├── AttendanceSheet.tsx     ← Registro diario
│   │   │   │   ├── AttendanceCalendar.tsx
│   │   │   │   ├── AttendanceSummary.tsx
│   │   │   │   └── QuickAttendance.tsx     ← Registro rápido
│   │   │   ├── pages/
│   │   │   │   ├── AttendancePage.tsx
│   │   │   │   └── AttendanceReportPage.tsx
│   │   │   ├── routes.tsx
│   │   │   └── index.ts
│   │   │
│   │   ├── discipline/             ← Módulo: Gestión Disciplinaria
│   │   │   ├── types/
│   │   │   │   └── discipline.types.ts
│   │   │   ├── services/
│   │   │   │   └── disciplineService.ts
│   │   │   ├── hooks/
│   │   │   │   └── useDiscipline.ts
│   │   │   ├── components/
│   │   │   │   ├── IncidentForm.tsx
│   │   │   │   ├── IncidentList.tsx
│   │   │   │   └── BehaviorReport.tsx
│   │   │   ├── pages/
│   │   │   │   ├── IncidentListPage.tsx
│   │   │   │   └── IncidentDetailPage.tsx
│   │   │   ├── routes.tsx
│   │   │   └── index.ts
│   │   │
│   │   ├── psychology/             ← Módulo: Psicología y Bienestar
│   │   │   ├── types/
│   │   │   │   └── psychology.types.ts
│   │   │   ├── services/
│   │   │   │   └── psychologyService.ts
│   │   │   ├── hooks/
│   │   │   │   └── usePsychology.ts
│   │   │   ├── components/
│   │   │   │   ├── EvaluationForm.tsx
│   │   │   │   ├── SpecialNeedsForm.tsx
│   │   │   │   └── SessionTracker.tsx
│   │   │   ├── pages/
│   │   │   │   ├── EvaluationListPage.tsx
│   │   │   │   └── SpecialNeedsPage.tsx
│   │   │   ├── routes.tsx
│   │   │   └── index.ts
│   │   │
│   │   ├── teachers/               ← Módulo: Asignación Docente
│   │   │   ├── types/
│   │   │   │   └── teacher.types.ts
│   │   │   ├── services/
│   │   │   │   └── teacherService.ts
│   │   │   ├── hooks/
│   │   │   │   └── useTeachers.ts
│   │   │   ├── components/
│   │   │   │   ├── AssignmentBoard.tsx     ← Drag & drop schedule
│   │   │   │   ├── TeacherSchedule.tsx
│   │   │   │   └── AssignmentForm.tsx
│   │   │   ├── pages/
│   │   │   │   ├── AssignmentPage.tsx
│   │   │   │   └── TeacherSchedulePage.tsx
│   │   │   ├── routes.tsx
│   │   │   └── index.ts
│   │   │
│   │   ├── events/                 ← Módulo: Fechas Cívicas / Calendario
│   │   │   ├── types/
│   │   │   │   └── event.types.ts
│   │   │   ├── services/
│   │   │   │   └── eventService.ts
│   │   │   ├── hooks/
│   │   │   │   └── useEvents.ts
│   │   │   ├── components/
│   │   │   │   ├── EventCalendar.tsx
│   │   │   │   ├── EventForm.tsx
│   │   │   │   └── EventCard.tsx
│   │   │   ├── pages/
│   │   │   │   ├── CalendarPage.tsx
│   │   │   │   └── EventDetailPage.tsx
│   │   │   ├── routes.tsx
│   │   │   └── index.ts
│   │   │
│   │   ├── users/                  ← Módulo: Gestión de Usuarios
│   │   │   ├── types/
│   │   │   │   └── user.types.ts
│   │   │   ├── services/
│   │   │   │   └── userService.ts
│   │   │   ├── hooks/
│   │   │   │   └── useUsers.ts
│   │   │   ├── components/
│   │   │   │   ├── UserTable.tsx
│   │   │   │   ├── UserForm.tsx
│   │   │   │   ├── RoleManager.tsx
│   │   │   │   └── UserFilters.tsx
│   │   │   ├── pages/
│   │   │   │   ├── UserListPage.tsx
│   │   │   │   └── UserFormPage.tsx
│   │   │   ├── routes.tsx
│   │   │   └── index.ts
│   │   │
│   │   └── dashboard/              ← Módulo: Dashboard Principal
│   │       ├── components/
│   │       │   ├── StatCard.tsx
│   │       │   ├── AttendanceChart.tsx
│   │       │   ├── EnrollmentChart.tsx
│   │       │   └── QuickActions.tsx
│   │       ├── pages/
│   │       │   └── DashboardPage.tsx
│   │       └── index.ts
│   │
│   └── router/                     ← Configuración de rutas
│       ├── AppRouter.tsx
│       ├── routes.ts
│       └── routeConfig.ts
```

---

## 🔧 CÓDIGO — Capa API (Core)

### apiClient.ts — Instancia centralizada de Axios

```typescript
// src/core/api/apiClient.ts
import axios, { AxiosInstance } from 'axios';
import { setupInterceptors } from './interceptors';

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

const apiClient: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json',
  },
});

setupInterceptors(apiClient);

export default apiClient;
```

### interceptors.ts — Manejo centralizado de auth y errores

```typescript
// src/core/api/interceptors.ts
import { AxiosInstance, InternalAxiosRequestConfig, AxiosError } from 'axios';
import { useNotificationStore } from '../store/useNotificationStore';

export function setupInterceptors(client: AxiosInstance): void {
  // Request interceptor — agrega JWT a cada llamada
  client.interceptors.request.use(
    (config: InternalAxiosRequestConfig) => {
      const token = localStorage.getItem('access_token');
      if (token) {
        config.headers.Authorization = `Bearer ${token}`;
      }

      // Agregar Institution ID del contexto del usuario
      const institutionId = localStorage.getItem('institution_id');
      if (institutionId) {
        config.headers['X-Institution-Id'] = institutionId;
      }

      return config;
    },
    (error) => Promise.reject(error)
  );

  // Response interceptor — manejo global de errores
  client.interceptors.response.use(
    (response) => response,
    async (error: AxiosError<{ message?: string }>) => {
      const { addNotification } = useNotificationStore.getState();

      if (error.response) {
        switch (error.response.status) {
          case 401:
            // Token expirado → refresh o logout
            localStorage.removeItem('access_token');
            window.location.href = '/login';
            break;
          case 403:
            addNotification({
              type: 'error',
              message: 'No tiene permisos para esta acción',
            });
            break;
          case 404:
            addNotification({
              type: 'warning',
              message: 'Recurso no encontrado',
            });
            break;
          case 422:
            addNotification({
              type: 'error',
              message: error.response.data?.message || 'Error de validación',
            });
            break;
          case 503:
            addNotification({
              type: 'error',
              message: 'Servicio no disponible. Intente más tarde.',
            });
            break;
          default:
            addNotification({
              type: 'error',
              message: 'Error inesperado del servidor',
            });
        }
      } else if (error.request) {
        addNotification({
          type: 'error',
          message: 'No se pudo conectar al servidor',
        });
      }

      return Promise.reject(error);
    }
  );
}
```

### endpoints.ts — Rutas API centralizadas

```typescript
// src/core/api/endpoints.ts

// TODAS las rutas apuntan al API Gateway (:8080)
// El Gateway enruta al microservicio correcto
export const ENDPOINTS = {
  AUTH: {
    LOGIN: '/api/v1/auth/login',
    REFRESH: '/api/v1/auth/refresh',
    LOGOUT: '/api/v1/auth/logout',
  },
  INSTITUTIONS: {
    BASE: '/api/v1/institutions',
    BY_ID: (id: string) => `/api/v1/institutions/${id}`,
    CLASSROOMS: (institutionId: string) =>
      `/api/v1/institutions/${institutionId}/classrooms`,
  },
  STUDENTS: {
    BASE: '/api/v1/students',
    BY_ID: (id: string) => `/api/v1/students/${id}`,
    BY_INSTITUTION: (institutionId: string) =>
      `/api/v1/students?institutionId=${institutionId}`,
  },
  ENROLLMENTS: {
    BASE: '/api/v1/enrollments',
    BY_ID: (id: string) => `/api/v1/enrollments/${id}`,
    ACADEMIC_PERIODS: '/api/v1/academic-periods',
  },
  USERS: {
    BASE: '/api/v1/users',
    BY_ID: (id: string) => `/api/v1/users/${id}`,
    ROLES: '/api/v1/users/roles',
  },
  ACADEMIC: {
    COURSES: '/api/v1/courses',
    COMPETENCIES: '/api/v1/competencies',
    CATALOGS: '/api/v1/catalogs',
  },
  GRADES: {
    BASE: '/api/v1/notes',
    EVALUATIONS: '/api/v1/evaluations',
    REPORT_CARDS: '/api/v1/report-cards',
  },
  ATTENDANCE: {
    BASE: '/api/v1/attendance',
    SUMMARY: '/api/v1/attendance-summary',
  },
  DISCIPLINE: {
    INCIDENTS: '/api/v1/incidents',
    BEHAVIOR: '/api/v1/behavior-records',
  },
  PSYCHOLOGY: {
    EVALUATIONS: '/api/v1/psychological-evaluations',
    SPECIAL_NEEDS: '/api/v1/special-needs',
  },
  TEACHERS: {
    ASSIGNMENTS: '/api/v1/teacher-assignments',
  },
  EVENTS: {
    BASE: '/api/v1/events',
    CALENDARS: '/api/v1/calendars',
  },
  NOTIFICATIONS: {
    BASE: '/api/v1/notifications',
  },
} as const;
```

---

## 🔧 CÓDIGO — Feature Service (Ejemplo: Students)

### student.types.ts

```typescript
// src/features/students/types/student.types.ts

export interface Student {
  id: string;
  personalInfo: PersonalInfo;
  guardian: Guardian;
  healthInfo?: HealthInfo;
  institutionId: string;
  status: StudentStatus;
  createdAt: string;
  updatedAt: string;
}

export interface PersonalInfo {
  firstName: string;
  lastName: string;
  dni: string;
  birthDate: string;
  gender: Gender;
  address?: string;
  phone?: string;
}

export interface Guardian {
  fullName: string;
  dni: string;
  relation: GuardianRelation;
  phone: string;
  email?: string;
  occupation?: string;
}

export interface HealthInfo {
  bloodType?: string;
  allergies?: string[];
  disabilities?: string[];
  insurance?: string;
}

export type StudentStatus = 'ACTIVE' | 'INACTIVE' | 'TRANSFERRED' | 'GRADUATED';
export type Gender = 'MALE' | 'FEMALE';
export type GuardianRelation = 'FATHER' | 'MOTHER' | 'GUARDIAN' | 'OTHER';

// DTOs
export interface CreateStudentRequest {
  personalInfo: PersonalInfo;
  guardian: Guardian;
  healthInfo?: HealthInfo;
  institutionId: string;
}

export interface StudentFilters {
  status?: StudentStatus;
  institutionId?: string;
  search?: string;
  page?: number;
  size?: number;
}
```

### studentService.ts — Capa de servicio API

```typescript
// src/features/students/services/studentService.ts
import apiClient from '@/core/api/apiClient';
import { ENDPOINTS } from '@/core/api/endpoints';
import type { Student, CreateStudentRequest, StudentFilters } from '../types/student.types';
import type { PaginatedResponse } from '@/core/types/api.types';

export const studentService = {
  getAll: async (filters?: StudentFilters): Promise<PaginatedResponse<Student>> => {
    const { data } = await apiClient.get(ENDPOINTS.STUDENTS.BASE, {
      params: filters,
    });
    return data;
  },

  getById: async (id: string): Promise<Student> => {
    const { data } = await apiClient.get(ENDPOINTS.STUDENTS.BY_ID(id));
    return data;
  },

  create: async (request: CreateStudentRequest): Promise<Student> => {
    const { data } = await apiClient.post(ENDPOINTS.STUDENTS.BASE, request);
    return data;
  },

  update: async (id: string, request: Partial<CreateStudentRequest>): Promise<Student> => {
    const { data } = await apiClient.put(ENDPOINTS.STUDENTS.BY_ID(id), request);
    return data;
  },

  deactivate: async (id: string): Promise<Student> => {
    const { data } = await apiClient.patch(`${ENDPOINTS.STUDENTS.BY_ID(id)}/deactivate`);
    return data;
  },
};
```

### useStudents.ts — TanStack Query hooks

```typescript
// src/features/students/hooks/useStudents.ts
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { studentService } from '../services/studentService';
import type { CreateStudentRequest, StudentFilters } from '../types/student.types';
import { useNotificationStore } from '@/core/store/useNotificationStore';

const QUERY_KEY = 'students';

export function useStudents(filters?: StudentFilters) {
  return useQuery({
    queryKey: [QUERY_KEY, filters],
    queryFn: () => studentService.getAll(filters),
    staleTime: 30_000,         // Datos válidos por 30s
    placeholderData: (prev) => prev, // Mantener datos previos mientras recarga
  });
}

export function useStudent(id: string) {
  return useQuery({
    queryKey: [QUERY_KEY, id],
    queryFn: () => studentService.getById(id),
    enabled: !!id,
  });
}

export function useCreateStudent() {
  const queryClient = useQueryClient();
  const { addNotification } = useNotificationStore();

  return useMutation({
    mutationFn: (data: CreateStudentRequest) => studentService.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [QUERY_KEY] });
      addNotification({
        type: 'success',
        message: 'Estudiante registrado exitosamente',
      });
    },
    onError: () => {
      addNotification({
        type: 'error',
        message: 'Error al registrar estudiante',
      });
    },
  });
}

export function useDeactivateStudent() {
  const queryClient = useQueryClient();
  const { addNotification } = useNotificationStore();

  return useMutation({
    mutationFn: (id: string) => studentService.deactivate(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [QUERY_KEY] });
      addNotification({
        type: 'success',
        message: 'Estudiante dado de baja',
      });
    },
  });
}
```

---

## 🔧 CÓDIGO — Autenticación

### AuthContext.tsx

```typescript
// src/core/auth/AuthContext.tsx
import { createContext, useContext } from 'react';

export interface AuthUser {
  id: string;
  email: string;
  fullName: string;
  role: UserRole;
  institutionId: string;
  institutionName: string;
}

export type UserRole =
  | 'ADMIN'           // Administrador del sistema
  | 'DIRECTOR'        // Director de IE
  | 'SUBDIRECTOR'     // Subdirector
  | 'DOCENTE'         // Docente
  | 'AUXILIAR'        // Auxiliar de educación
  | 'PSICOLOGO'       // Psicólogo
  | 'SECRETARIA'      // Secretaría
  | 'APODERADO';      // Padre/Madre de familia

export interface AuthContextType {
  user: AuthUser | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (email: string, password: string) => Promise<void>;
  logout: () => void;
  hasRole: (roles: UserRole[]) => boolean;
}

export const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function useAuth(): AuthContextType {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
```

### ProtectedRoute.tsx

```typescript
// src/core/auth/ProtectedRoute.tsx
import { Navigate, Outlet } from 'react-router-dom';
import { useAuth, type UserRole } from './AuthContext';
import { LoadingScreen } from '@/shared/components/feedback/LoadingScreen';

interface ProtectedRouteProps {
  allowedRoles?: UserRole[];
}

export function ProtectedRoute({ allowedRoles }: ProtectedRouteProps) {
  const { isAuthenticated, isLoading, user, hasRole } = useAuth();

  if (isLoading) return <LoadingScreen />;

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  if (allowedRoles && !hasRole(allowedRoles)) {
    return <Navigate to="/unauthorized" replace />;
  }

  return <Outlet />;
}
```

---

## 🔧 CÓDIGO — Router Principal

```typescript
// src/router/AppRouter.tsx
import { createBrowserRouter, RouterProvider } from 'react-router-dom';
import { ProtectedRoute } from '@/core/auth/ProtectedRoute';
import { MainLayout } from '@/shared/components/layout/MainLayout';

// Lazy loading de features
const LoginPage = lazy(() => import('@/features/auth/pages/LoginPage'));
const DashboardPage = lazy(() => import('@/features/dashboard/pages/DashboardPage'));
const InstitutionListPage = lazy(() => import('@/features/institutions/pages/InstitutionListPage'));
const StudentListPage = lazy(() => import('@/features/students/pages/StudentListPage'));
const EnrollmentListPage = lazy(() => import('@/features/enrollments/pages/EnrollmentListPage'));
// ... más imports lazy

const router = createBrowserRouter([
  // Rutas públicas
  {
    path: '/login',
    element: <Suspense fallback={<LoadingScreen />}><LoginPage /></Suspense>,
  },

  // Rutas protegidas
  {
    element: <ProtectedRoute />,
    children: [
      {
        element: <MainLayout />,
        children: [
          // Dashboard
          {
            path: '/',
            element: <Suspense fallback={<LoadingScreen />}><DashboardPage /></Suspense>,
          },

          // Instituciones — Solo ADMIN y DIRECTOR
          {
            element: <ProtectedRoute allowedRoles={['ADMIN', 'DIRECTOR']} />,
            children: [
              {
                path: '/institutions',
                element: <Suspense fallback={<LoadingScreen />}><InstitutionListPage /></Suspense>,
              },
              {
                path: '/institutions/:id',
                element: <Suspense fallback={<LoadingScreen />}><InstitutionDetailPage /></Suspense>,
              },
            ],
          },

          // Estudiantes — ADMIN, DIRECTOR, SUBDIRECTOR, DOCENTE, SECRETARIA
          {
            element: <ProtectedRoute allowedRoles={['ADMIN', 'DIRECTOR', 'SUBDIRECTOR', 'DOCENTE', 'SECRETARIA']} />,
            children: [
              {
                path: '/students',
                element: <Suspense fallback={<LoadingScreen />}><StudentListPage /></Suspense>,
              },
            ],
          },

          // Matrículas — ADMIN, DIRECTOR, SECRETARIA
          {
            element: <ProtectedRoute allowedRoles={['ADMIN', 'DIRECTOR', 'SECRETARIA']} />,
            children: [
              {
                path: '/enrollments',
                element: <Suspense fallback={<LoadingScreen />}><EnrollmentListPage /></Suspense>,
              },
            ],
          },

          // Notas — ADMIN, DIRECTOR, DOCENTE
          {
            element: <ProtectedRoute allowedRoles={['ADMIN', 'DIRECTOR', 'DOCENTE']} />,
            children: [
              {
                path: '/grades',
                element: <Suspense fallback={<LoadingScreen />}><GradeEntryPage /></Suspense>,
              },
            ],
          },

          // Asistencia — ADMIN, DIRECTOR, DOCENTE, AUXILIAR
          {
            element: <ProtectedRoute allowedRoles={['ADMIN', 'DIRECTOR', 'DOCENTE', 'AUXILIAR']} />,
            children: [
              {
                path: '/attendance',
                element: <Suspense fallback={<LoadingScreen />}><AttendancePage /></Suspense>,
              },
            ],
          },

          // Psicología — ADMIN, DIRECTOR, PSICOLOGO
          {
            element: <ProtectedRoute allowedRoles={['ADMIN', 'DIRECTOR', 'PSICOLOGO']} />,
            children: [
              {
                path: '/psychology',
                element: <Suspense fallback={<LoadingScreen />}><EvaluationListPage /></Suspense>,
              },
            ],
          },

          // Usuarios — Solo ADMIN
          {
            element: <ProtectedRoute allowedRoles={['ADMIN']} />,
            children: [
              {
                path: '/users',
                element: <Suspense fallback={<LoadingScreen />}><UserListPage /></Suspense>,
              },
            ],
          },
        ],
      },
    ],
  },
]);

export function AppRouter() {
  return <RouterProvider router={router} />;
}
```

---

## 📋 DEPENDENCIAS RECOMENDADAS (package.json)

```json
{
  "dependencies": {
    "react": "^19.0.0",
    "react-dom": "^19.0.0",
    "react-router-dom": "^7.9.0",
    "axios": "^1.7.0",
    "@tanstack/react-query": "^5.50.0",
    "@tanstack/react-query-devtools": "^5.50.0",
    "zustand": "^5.0.0",
    "react-hook-form": "^7.52.0",
    "@hookform/resolvers": "^3.9.0",
    "zod": "^3.23.0",
    "tailwindcss": "^4.0.0",
    "lucide-react": "^0.400.0",
    "date-fns": "^3.6.0",
    "recharts": "^2.12.0",
    "react-hot-toast": "^2.4.0"
  },
  "devDependencies": {
    "typescript": "^5.9.0",
    "@types/react": "^19.0.0",
    "@types/react-dom": "^19.0.0",
    "vite": "^7.0.0",
    "@vitejs/plugin-react-swc": "^4.0.0",
    "vitest": "^2.0.0",
    "@testing-library/react": "^16.0.0",
    "@testing-library/jest-dom": "^6.4.0",
    "msw": "^2.3.0",
    "eslint": "^9.5.0",
    "prettier": "^3.3.0"
  }
}
```

---

## 📋 VARIABLES DE ENTORNO

```bash
# .env.development
VITE_API_URL=http://localhost:8080
VITE_APP_NAME=SIGEI
VITE_APP_VERSION=1.0.0
VITE_ENABLE_DEVTOOLS=true

# .env.production
VITE_API_URL=https://api.sigei.edu.pe
VITE_APP_NAME=SIGEI
VITE_APP_VERSION=1.0.0
VITE_ENABLE_DEVTOOLS=false

# .env.example
VITE_API_URL=http://localhost:8080
VITE_APP_NAME=SIGEI
VITE_APP_VERSION=1.0.0
VITE_ENABLE_DEVTOOLS=true
```

---

## 📋 vite.config.ts ACTUALIZADO

```typescript
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react-swc';
import path from 'path';

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',  // Solo el API Gateway
        changeOrigin: true,
        secure: false,
      },
    },
  },
  build: {
    rollupOptions: {
      output: {
        manualChunks: {
          vendor: ['react', 'react-dom', 'react-router-dom'],
          query: ['@tanstack/react-query'],
          ui: ['lucide-react', 'recharts'],
        },
      },
    },
  },
});
```

---

> **Siguiente:** Ver `07_PATRONES_DISENO_RECOMENDADOS.md` para los patrones de diseño a implementar.
