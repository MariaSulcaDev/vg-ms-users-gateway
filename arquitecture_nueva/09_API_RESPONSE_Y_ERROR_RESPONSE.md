# 09 — API RESPONSE Y ERROR RESPONSE ESTANDARIZADO

> **Objetivo:** Definir un formato de respuesta unificado (`ApiResponse<T>` y `ErrorResponse`) para TODOS los microservicios de SIGEI, garantizando consistencia en la comunicación con el frontend y entre servicios.

---

## 📋 PROBLEMA QUE SE RESUELVE

### ❌ Estado actual — Respuestas inconsistentes

```
// MS Institution → devuelve el objeto directamente
GET /api/institutions/1
{
  "id": "1",
  "name": "IE Inicial Los Angelitos",
  "status": "ACTIVE"
}

// MS Students → devuelve envuelto, pero diferente estructura
GET /api/students/5
{
  "data": { "id": "5", "name": "María" },
  "status": true
}

// MS Enrollments → devuelve con otro formato
GET /api/enrollments/10
{
  "success": true,
  "enrollment": { ... }
}

// Errores → cada MS lanza formato distinto
{
  "error": "Not Found"          // MS 1
}
{
  "message": "Student not found",  // MS 2
  "code": 404
}
{
  "timestamp": "...",             // MS 3
  "status": 500,
  "error": "Internal Server Error"
}
```

**Resultado:** El frontend necesita lógica diferente para cada microservicio. Imposible hacer una función genérica de manejo de respuestas.

### ✅ Con ApiResponse y ErrorResponse estandarizado

```
// TODA respuesta exitosa tiene el MISMO formato
{
  "success": true,
  "message": "Institución encontrada",
  "data": { "id": "1", "name": "IE Inicial Los Angelitos" },
  "timestamp": "2026-02-15T10:30:00",
  "path": "/api/institutions/1"
}

// TODO error tiene el MISMO formato
{
  "success": false,
  "message": "Estudiante no encontrado",
  "errorCode": "STUDENT_NOT_FOUND",
  "status": 404,
  "timestamp": "2026-02-15T10:30:00",
  "path": "/api/students/999",
  "details": [
    "No existe un estudiante con ID: 999"
  ]
}
```

---

## 🏗️ ESTRUCTURA DE CLASES

### Ubicación en cada microservicio (Hexagonal)

```
src/main/java/pe/edu/vallegrande/sigei/<ms>/
├── application/
│   └── dto/
│       ├── common/
│       │   ├── ApiResponse.java               ← Wrapper de respuestas exitosas
│       │   └── ErrorResponse.java             ← Wrapper de respuestas de error
│       ├── request/
│       │   └── CreateXxxRequest.java
│       └── response/
│           └── XxxResponse.java
├── infrastructure/
│   └── adapters/in/rest/
│       ├── XxxRest.java
│       └── GlobalExceptionHandler.java        ← Usa ErrorResponse
```

> **Nota:** `ApiResponse` y `ErrorResponse` viven en `application.dto.common` porque son DTOs de respuesta de la API, agrupados con los demás DTOs. El dominio NO los conoce. El controller (infraestructura) los importa desde application.

---

## 📦 CÓDIGO COMPLETO

### 1. ApiResponse\<T\> — Wrapper genérico de éxito

```java
package pe.edu.vallegrande.sigei.<modulo>.application.dto.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;

/**
 * Envoltorio genérico para TODAS las respuestas exitosas de la API.
 *
 * @param <T> el tipo de datos que envuelve (puede ser un objeto, lista, Page, etc.)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;
    private final String message;
    private final T data;
    private final LocalDateTime timestamp;
    private final String path;

    // ─── Constructor privado (usar Factory Methods) ───
    private ApiResponse(boolean success, String message, T data, String path) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.timestamp = LocalDateTime.now();
        this.path = path;
    }

    // ═══════════════════════════════════════════════════
    // FACTORY METHODS — Formas de crear ApiResponse
    // ═══════════════════════════════════════════════════

    /** Respuesta exitosa CON datos y mensaje */
    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(true, message, data, null);
    }

    /** Respuesta exitosa CON datos (mensaje por defecto) */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "Operación exitosa", data, null);
    }

    /** Respuesta exitosa CON datos, mensaje y path */
    public static <T> ApiResponse<T> ok(T data, String message, String path) {
        return new ApiResponse<>(true, message, data, path);
    }

    /** Respuesta de creación (HTTP 201) */
    public static <T> ApiResponse<T> created(T data, String message) {
        return new ApiResponse<>(true, message, data, null);
    }

    /** Respuesta vacía (HTTP 204 — No Content) */
    public static <Void> ApiResponse<Void> noContent(String message) {
        return new ApiResponse<>(true, message, null, null);
    }

    // ─── Getters ───
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public T getData() { return data; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getPath() { return path; }
}
```

### 2. ErrorResponse — Wrapper de errores

```java
package pe.edu.vallegrande.sigei.<modulo>.application.dto.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Envoltorio para TODAS las respuestas de error de la API.
 * Nunca se debe devolver un error sin este wrapper.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private final boolean success = false;
    private final int status;
    private final String errorCode;
    private final String message;
    private final List<String> details;
    private final String path;
    private final LocalDateTime timestamp;

    // ─── Constructor privado (usar Factory Methods) ───
    private ErrorResponse(int status, String errorCode, String message,
                          List<String> details, String path) {
        this.status = status;
        this.errorCode = errorCode;
        this.message = message;
        this.details = details;
        this.path = path;
        this.timestamp = LocalDateTime.now();
    }

    // ═══════════════════════════════════════════════════
    // FACTORY METHODS
    // ═══════════════════════════════════════════════════

    /** Error genérico */
    public static ErrorResponse of(int status, String errorCode,
                                    String message, String path) {
        return new ErrorResponse(status, errorCode, message, null, path);
    }

    /** Error con detalles (validación, múltiples errores) */
    public static ErrorResponse withDetails(int status, String errorCode,
                                             String message, List<String> details,
                                             String path) {
        return new ErrorResponse(status, errorCode, message, details, path);
    }

    /** Error 404 — No encontrado */
    public static ErrorResponse notFound(String message, String path) {
        return new ErrorResponse(404, "RESOURCE_NOT_FOUND", message, null, path);
    }

    /** Error 400 — Petición inválida */
    public static ErrorResponse badRequest(String message, List<String> details,
                                            String path) {
        return new ErrorResponse(400, "VALIDATION_ERROR", message, details, path);
    }

    /** Error 409 — Conflicto (duplicado, estado inválido) */
    public static ErrorResponse conflict(String message, String path) {
        return new ErrorResponse(409, "CONFLICT", message, null, path);
    }

    /** Error 500 — Error interno */
    public static ErrorResponse internal(String path) {
        return new ErrorResponse(500, "INTERNAL_ERROR",
            "Error interno del servidor. Contacte al administrador.", null, path);
    }

    /** Error 403 — Acceso denegado */
    public static ErrorResponse forbidden(String path) {
        return new ErrorResponse(403, "ACCESS_DENIED",
            "No tiene permisos para realizar esta acción.", null, path);
    }

    // ─── Getters ───
    public boolean isSuccess() { return success; }
    public int getStatus() { return status; }
    public String getErrorCode() { return errorCode; }
    public String getMessage() { return message; }
    public List<String> getDetails() { return details; }
    public String getPath() { return path; }
    public LocalDateTime getTimestamp() { return timestamp; }
}
```

---

## 🔧 GLOBAL EXCEPTION HANDLER — Centraliza el manejo de errores

```java
package pe.edu.vallegrande.sigei.<modulo>.infrastructure.adapters.in.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import pe.edu.vallegrande.sigei.<modulo>.application.dto.common.ErrorResponse;

import java.util.List;

/**
 * Manejador global de excepciones para TODA la aplicación.
 * Captura excepciones y las convierte a ErrorResponse estandarizado.
 *
 * CADA microservicio tiene su propio GlobalExceptionHandler
 * pero TODOS siguen esta MISMA estructura.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ═══════════════════════════════════════════════════
    // EXCEPCIONES DE DOMINIO (las que tú defines)
    // ═══════════════════════════════════════════════════

    /**
     * Recurso no encontrado.
     * Ejemplo: InstitutionNotFoundException, StudentNotFoundException
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleNotFound(
            ResourceNotFoundException ex, ServerWebExchange exchange) {

        String path = exchange.getRequest().getPath().value();
        log.warn("Recurso no encontrado: {} en {}", ex.getMessage(), path);

        return Mono.just(ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ErrorResponse.notFound(ex.getMessage(), path)));
    }

    /**
     * Conflicto de negocio (duplicado, estado inválido).
     * Ejemplo: DuplicateModularCodeException, InvalidStatusTransitionException
     */
    @ExceptionHandler(BusinessConflictException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleConflict(
            BusinessConflictException ex, ServerWebExchange exchange) {

        String path = exchange.getRequest().getPath().value();
        log.warn("Conflicto de negocio: {} en {}", ex.getMessage(), path);

        return Mono.just(ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(ErrorResponse.conflict(ex.getMessage(), path)));
    }

    /**
     * Argumento inválido (validación de dominio).
     * Ejemplo: "El código modular debe tener 7 dígitos"
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleIllegalArgument(
            IllegalArgumentException ex, ServerWebExchange exchange) {

        String path = exchange.getRequest().getPath().value();
        log.warn("Argumento inválido: {} en {}", ex.getMessage(), path);

        return Mono.just(ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse.badRequest(ex.getMessage(), null, path)));
    }

    // ═══════════════════════════════════════════════════
    // EXCEPCIONES DE VALIDACIÓN (Bean Validation @Valid)
    // ═══════════════════════════════════════════════════

    /**
     * Errores de validación del DTO (@Valid, @NotBlank, @Size, etc.).
     */
    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleValidation(
            WebExchangeBindException ex, ServerWebExchange exchange) {

        String path = exchange.getRequest().getPath().value();

        List<String> details = ex.getFieldErrors().stream()
            .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
            .toList();

        log.warn("Errores de validación en {}: {}", path, details);

        return Mono.just(ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse.badRequest(
                "Error de validación en los datos enviados", details, path)));
    }

    // ═══════════════════════════════════════════════════
    // EXCEPCIÓN GENÉRICA (catch-all)
    // ═══════════════════════════════════════════════════

    /**
     * Cualquier excepción no manejada.
     * NUNCA se expone el stacktrace al cliente.
     */
    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ErrorResponse>> handleGeneric(
            Exception ex, ServerWebExchange exchange) {

        String path = exchange.getRequest().getPath().value();
        log.error("Error interno no manejado en {}: {}", path, ex.getMessage(), ex);

        return Mono.just(ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse.internal(path)));
    }
}
```

---

## 🏷️ EXCEPCIONES BASE DEL DOMINIO

Cada microservicio define sus excepciones que extienden de estas bases:

```java
// ─── Excepción base: Recurso no encontrado ───
// Cada MS tiene sus propias excepciones base en domain/exceptions/
package pe.edu.vallegrande.sigei.<modulo>.domain.exceptions;

public class ResourceNotFoundException extends RuntimeException {
    private final String resourceName;
    private final String fieldName;
    private final Object fieldValue;

    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s no encontrado con %s: '%s'", resourceName, fieldName, fieldValue));
        this.resourceName = resourceName;
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }

    public String getResourceName() { return resourceName; }
    public String getFieldName() { return fieldName; }
    public Object getFieldValue() { return fieldValue; }
}

// ─── Excepción base: Conflicto de negocio ───
package pe.edu.vallegrande.sigei.<modulo>.domain.exceptions;

public class BusinessConflictException extends RuntimeException {
    private final String errorCode;

    public BusinessConflictException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() { return errorCode; }
}
```

### Uso por microservicio — Excepciones específicas

```java
// ─── MS Institution ───
public class InstitutionNotFoundException extends ResourceNotFoundException {
    public InstitutionNotFoundException(String id) {
        super("Institución", "id", id);
    }
}

public class DuplicateModularCodeException extends BusinessConflictException {
    public DuplicateModularCodeException(String code) {
        super("Ya existe una institución con código modular: " + code,
              "DUPLICATE_MODULAR_CODE");
    }
}

// ─── MS Students ───
public class StudentNotFoundException extends ResourceNotFoundException {
    public StudentNotFoundException(String id) {
        super("Estudiante", "id", id);
    }
}

// ─── MS Enrollments ───
public class EnrollmentNotFoundException extends ResourceNotFoundException {
    public EnrollmentNotFoundException(String id) {
        super("Matrícula", "id", id);
    }
}

public class DuplicateEnrollmentException extends BusinessConflictException {
    public DuplicateEnrollmentException(String studentId, String year) {
        super("El estudiante " + studentId + " ya tiene matrícula en " + year,
              "DUPLICATE_ENROLLMENT");
    }
}
```

---

## 🎮 USO EN CONTROLLERS (WebFlux Reactivo)

### Ejemplo completo — InstitutionController

```java
@RestController
@RequestMapping("/api/institutions")
public class InstitutionController {

    private final CreateInstitutionUseCase createUseCase;
    private final FindInstitutionUseCase findUseCase;
    private final InstitutionMapper mapper;

    // ─── GET — Obtener por ID ───
    @GetMapping("/{id}")
    public Mono<ResponseEntity<ApiResponse<InstitutionResponse>>> findById(
            @PathVariable String id, ServerWebExchange exchange) {

        String path = exchange.getRequest().getPath().value();

        return findUseCase.findById(id)
            .map(mapper::toResponse)
            .map(response -> ResponseEntity.ok(
                ApiResponse.ok(response, "Institución encontrada", path)))
            .switchIfEmpty(Mono.error(
                new InstitutionNotFoundException(id)));
    }

    // ─── GET — Listar todos ───
    @GetMapping
    public Mono<ResponseEntity<ApiResponse<List<InstitutionResponse>>>> findAll(
            ServerWebExchange exchange) {

        String path = exchange.getRequest().getPath().value();

        return findUseCase.findAll()
            .map(mapper::toResponse)
            .collectList()
            .map(list -> ResponseEntity.ok(
                ApiResponse.ok(list, "Se encontraron " + list.size() + " instituciones", path)));
    }

    // ─── POST — Crear ───
    @PostMapping
    public Mono<ResponseEntity<ApiResponse<InstitutionResponse>>> create(
            @Valid @RequestBody CreateInstitutionRequest request,
            ServerWebExchange exchange) {

        String path = exchange.getRequest().getPath().value();

        return createUseCase.create(mapper.toDomain(request))
            .map(mapper::toResponse)
            .map(response -> ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "Institución creada exitosamente")));
    }

    // ─── PUT — Actualizar ───
    @PutMapping("/{id}")
    public Mono<ResponseEntity<ApiResponse<InstitutionResponse>>> update(
            @PathVariable String id,
            @Valid @RequestBody UpdateInstitutionRequest request,
            ServerWebExchange exchange) {

        String path = exchange.getRequest().getPath().value();

        return findUseCase.findById(id)
            .switchIfEmpty(Mono.error(new InstitutionNotFoundException(id)))
            .flatMap(existing -> createUseCase.update(id, mapper.toDomain(request)))
            .map(mapper::toResponse)
            .map(response -> ResponseEntity.ok(
                ApiResponse.ok(response, "Institución actualizada exitosamente", path)));
    }

    // ─── PATCH — Cambiar estado (soft delete) ───
    @PatchMapping("/{id}/status")
    public Mono<ResponseEntity<ApiResponse<Void>>> toggleStatus(
            @PathVariable String id, ServerWebExchange exchange) {

        String path = exchange.getRequest().getPath().value();

        return createUseCase.toggleStatus(id)
            .then(Mono.just(ResponseEntity.ok(
                ApiResponse.<Void>noContent("Estado de la institución actualizado"))));
    }
}
```

---

## 📱 INTEGRACIÓN CON EL FRONTEND (React + TypeScript)

### Tipos TypeScript que coinciden con el backend

```typescript
// src/shared/types/api.types.ts

/** Respuesta exitosa del backend */
export interface ApiResponse<T> {
  success: true;
  message: string;
  data: T;
  timestamp: string;
  path?: string;
}

/** Respuesta de error del backend */
export interface ErrorResponse {
  success: false;
  status: number;
  errorCode: string;
  message: string;
  details?: string[];
  path: string;
  timestamp: string;
}

/** Unión discriminada — facilita el manejo */
export type ApiResult<T> = ApiResponse<T> | ErrorResponse;
```

### Función genérica de fetch

```typescript
// src/shared/api/apiClient.ts

import axios, { AxiosInstance, AxiosError } from 'axios';
import { ApiResponse, ErrorResponse } from '../types/api.types';

const apiClient: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_GATEWAY_URL, // http://localhost:8080
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
});

// ─── Interceptor: adjunta token Keycloak ───
apiClient.interceptors.request.use((config) => {
  const token = keycloak.token; // del contexto de Keycloak (ver doc 08)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// ─── Interceptor: manejo global de errores ───
apiClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ErrorResponse>) => {
    const errorData = error.response?.data;

    if (errorData?.success === false) {
      // Error estructurado del backend
      console.error(`[${errorData.errorCode}] ${errorData.message}`);

      if (errorData.status === 401) {
        // Token expirado → refrescar o redirigir a login
        keycloak.updateToken(5).catch(() => keycloak.login());
      }
    }

    return Promise.reject(error);
  }
);

export default apiClient;

// ─── Funciones helper tipadas ───
export async function apiGet<T>(url: string): Promise<ApiResponse<T>> {
  const { data } = await apiClient.get<ApiResponse<T>>(url);
  return data;
}

export async function apiPost<T, B>(url: string, body: B): Promise<ApiResponse<T>> {
  const { data } = await apiClient.post<ApiResponse<T>>(url, body);
  return data;
}

export async function apiPut<T, B>(url: string, body: B): Promise<ApiResponse<T>> {
  const { data } = await apiClient.put<ApiResponse<T>>(url, body);
  return data;
}

export async function apiPatch<T>(url: string): Promise<ApiResponse<T>> {
  const { data } = await apiClient.patch<ApiResponse<T>>(url);
  return data;
}
```

### Uso con TanStack Query

```typescript
// src/features/institutions/hooks/useInstitutions.ts
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiGet, apiPost } from '@/shared/api/apiClient';
import type { InstitutionResponse, CreateInstitutionRequest } from '../types';

export function useInstitutions() {
  return useQuery({
    queryKey: ['institutions'],
    queryFn: async () => {
      const response = await apiGet<InstitutionResponse[]>('/api/institutions');
      return response.data; // ← desenvuelve ApiResponse, devuelve T directamente
    },
  });
}

export function useCreateInstitution() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: CreateInstitutionRequest) =>
      apiPost<InstitutionResponse, CreateInstitutionRequest>(
        '/api/institutions', request),
    onSuccess: (response) => {
      queryClient.invalidateQueries({ queryKey: ['institutions'] });
      toast.success(response.message); // ← usa el mensaje del backend
    },
    onError: (error: any) => {
      const err = error.response?.data as ErrorResponse;
      if (err?.details) {
        err.details.forEach(d => toast.error(d));
      } else {
        toast.error(err?.message || 'Error desconocido');
      }
    },
  });
}
```

---

## 📊 CATÁLOGO DE CÓDIGOS DE ERROR POR MICROSERVICIO

| Código de Error | HTTP | Microservicio | Descripción |
|----------------|------|--------------|-------------|
| `RESOURCE_NOT_FOUND` | 404 | Todos | Recurso no existe |
| `VALIDATION_ERROR` | 400 | Todos | Error en @Valid |
| `INTERNAL_ERROR` | 500 | Todos | Error no manejado |
| `ACCESS_DENIED` | 403 | Todos | Sin permisos Keycloak |
| `CONFLICT` | 409 | Todos | Conflicto genérico |
| `DUPLICATE_MODULAR_CODE` | 409 | Institution | Código modular ya existe |
| `DUPLICATE_ENROLLMENT` | 409 | Enrollments | Matrícula duplicada del año |
| `INVALID_STATUS_TRANSITION` | 409 | Students, Enrollments | Cambio de estado inválido |
| `STUDENT_NOT_FOUND` | 404 | Students | Estudiante no existe |
| `INSTITUTION_NOT_FOUND` | 404 | Institution | Institución no existe |
| `CLASSROOM_CAPACITY_EXCEEDED` | 409 | Institution | Aula llena |
| `GRADE_OUT_OF_RANGE` | 400 | Notes | Nota fuera del rango válido |
| `ATTENDANCE_ALREADY_REGISTERED` | 409 | Assistance | Asistencia ya registrada |
| `EVALUATION_NOT_FOUND` | 404 | Psychology | Evaluación no encontrada |
| `ASSIGNMENT_CONFLICT` | 409 | Teacher Assignment | Horario ya asignado |

---

## 📐 FORMATO JSON — Ejemplos completos

### Éxito — Obtener institución

```json
HTTP 200 OK
{
  "success": true,
  "message": "Institución encontrada",
  "data": {
    "id": "abc-123",
    "modularCode": "1234567",
    "name": "IE INICIAL LOS ANGELITOS",
    "address": {
      "department": "Lima",
      "province": "Lima",
      "district": "San Isidro"
    },
    "status": "ACTIVE",
    "createdAt": "2026-01-15T08:00:00"
  },
  "timestamp": "2026-02-15T10:30:00",
  "path": "/api/institutions/abc-123"
}
```

### Éxito — Crear estudiante

```json
HTTP 201 CREATED
{
  "success": true,
  "message": "Estudiante registrado exitosamente",
  "data": {
    "id": "stu-456",
    "fullName": "María Elena García Pérez",
    "dni": "76543210",
    "age": 4,
    "classroom": "Aula Estrellitas"
  },
  "timestamp": "2026-02-15T10:35:00",
  "path": null
}
```

### Error — Validación (400)

```json
HTTP 400 BAD REQUEST
{
  "success": false,
  "status": 400,
  "errorCode": "VALIDATION_ERROR",
  "message": "Error de validación en los datos enviados",
  "details": [
    "name: El nombre es obligatorio",
    "modularCode: El código modular debe tener 7 dígitos",
    "address.district: El distrito es obligatorio"
  ],
  "path": "/api/institutions",
  "timestamp": "2026-02-15T10:40:00"
}
```

### Error — No encontrado (404)

```json
HTTP 404 NOT FOUND
{
  "success": false,
  "status": 404,
  "errorCode": "RESOURCE_NOT_FOUND",
  "message": "Estudiante no encontrado con id: 'stu-999'",
  "details": null,
  "path": "/api/students/stu-999",
  "timestamp": "2026-02-15T10:45:00"
}
```

### Error — Conflicto (409)

```json
HTTP 409 CONFLICT
{
  "success": false,
  "status": 409,
  "errorCode": "DUPLICATE_ENROLLMENT",
  "message": "El estudiante stu-456 ya tiene matrícula en 2026",
  "details": null,
  "path": "/api/enrollments",
  "timestamp": "2026-02-15T10:50:00"
}
```

### Error — Servidor (500)

```json
HTTP 500 INTERNAL SERVER ERROR
{
  "success": false,
  "status": 500,
  "errorCode": "INTERNAL_ERROR",
  "message": "Error interno del servidor. Contacte al administrador.",
  "details": null,
  "path": "/api/notes/grade",
  "timestamp": "2026-02-15T10:55:00"
}
```

---

## 📏 REGLAS OBLIGATORIAS

| Regla | Descripción |
|-------|-------------|
| **R1** | Todo endpoint retorna `ApiResponse<T>` para éxito |
| **R2** | Todo error pasa por `GlobalExceptionHandler` y retorna `ErrorResponse` |
| **R3** | NUNCA se expone stacktrace al cliente (solo log interno) |
| **R4** | `errorCode` en UPPER_SNAKE_CASE (`STUDENT_NOT_FOUND`) |
| **R5** | `message` siempre en español, legible para el usuario final |
| **R6** | `details` solo se llena para errores de validación múltiple |
| **R7** | `timestamp` en formato ISO 8601 |
| **R8** | El frontend usa `success: true/false` como discriminador |
| **R9** | HTTP status code siempre coincide con el campo `status` del `ErrorResponse` |
| **R10** | Cada MS define sus propias excepciones extendiendo las bases |

---

## 🔗 RELACIÓN CON OTROS DOCUMENTOS

| Documento | Relación |
|-----------|----------|
| [01_ARQUITECTURA_HEXAGONAL](01_ARQUITECTURA_HEXAGONAL_CORRECTA.md) | ApiResponse vive en `infrastructure.common`, el dominio NO lo conoce |
| [04_API_GATEWAY](04_API_GATEWAY_Y_SERVICE_DISCOVERY.md) | El Gateway reenruta las respuestas tal cual (no las modifica) |
| [05_ARQUITECTURA_BACKEND](05_ARQUITECTURA_BACKEND_COMPLETA.md) | La estructura de carpetas incluye GlobalExceptionHandler |
| [06_ARQUITECTURA_FRONTEND](06_ARQUITECTURA_FRONTEND_COMPLETA.md) | Los tipos TypeScript deben coincidir con ApiResponse/ErrorResponse |
| [08_SEGURIDAD_KEYCLOAK](08_SEGURIDAD_KEYCLOAK.md) | El interceptor adjunta Bearer token de Keycloak |
