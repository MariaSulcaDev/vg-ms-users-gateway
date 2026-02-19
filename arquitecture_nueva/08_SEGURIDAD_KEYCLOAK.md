# 08 — SEGURIDAD CON KEYCLOAK

> Autenticación y autorización centralizada con Keycloak + OAuth 2.0 / OpenID Connect

---

## 📊 ¿POR QUÉ KEYCLOAK?

| Aspecto | JWT manual (doc anterior) | Keycloak |
|---------|--------------------------|----------|
| Login / Registro | Lo codificas tú | Ya viene hecho (UI incluida) |
| Gestión de usuarios | Tu propia tabla + código | Admin Console web |
| Roles y permisos | Hardcodeado en código | Configurado en UI |
| SSO | Lo implementas tú | Incluido |
| OAuth 2.0 / OIDC | Parcial | Completo |
| MFA (2 factores) | Desarrollo +2 semanas | Un checkbox |
| Recuperar contraseña | Lo codificas tú | Ya viene hecho |
| Brute force protection | Lo codificas tú | Ya viene hecho |
| Token rotation | Lo codificas tú | Automático |

> **Keycloak reemplaza completamente el `vg-ms-users-management` para autenticación.**
> El MS de usuarios pasa a ser solo "perfil de usuario" (datos adicionales, no credentials).

---

## 🏗️ ARQUITECTURA CON KEYCLOAK

```
                    ┌───────────────────┐
                    │   Frontend React  │
                    │    Puerto 5173    │
                    └──────┬────┬───────┘
                           │    │
              ┌────────────┘    └────────────┐
              │ Login (OIDC)                 │ API calls (Bearer token)
              ▼                              ▼
    ┌──────────────────┐          ┌───────────────────┐
    │    KEYCLOAK       │          │   API GATEWAY     │
    │   Puerto 8180     │◄────────│   Puerto 8080     │
    │                   │ Valida   │                   │
    │ • Login UI        │ tokens   │ • CORS (ÚNICO)    │
    │ • Token endpoint  │          │ • Rate Limiting   │
    │ • User management │          │ • Routing         │
    │ • Roles/Permisos  │          │ • Token relay     │
    └──────────────────┘          └────────┬──────────┘
                                           │
                              ┌────────────┼────────────┐
                              ▼            ▼            ▼
                        ┌──────────┐ ┌──────────┐ ┌──────────┐
                        │ MS 9080  │ │ MS 9081  │ │ MS 9082  │
                        │          │ │          │ │          │
                        │ Sin CORS │ │ Sin CORS │ │ Sin CORS │
                        │ Sin auth │ │ Sin auth │ │ Sin auth │
                        │ filter   │ │ filter   │ │ filter   │
                        │          │ │          │ │          │
                        │ Solo lee │ │ Solo lee │ │ Solo lee │
                        │ headers  │ │ headers  │ │ headers  │
                        └──────────┘ └──────────┘ └──────────┘
```

### Flujo de autenticación

```
1. Usuario abre frontend → redirige a Keycloak login
2. Keycloak autentica → devuelve access_token + refresh_token al frontend
3. Frontend envía cada request con: Authorization: Bearer <access_token>
4. API Gateway recibe → valida token con Keycloak (via JWKS)
5. Si válido → propaga headers X-User-Id, X-User-Role, X-Institution-Id al MS
6. Si inválido → retorna 401 (nunca llega al MS)
```

---

## 🔒 CORS: SOLO EN EL GATEWAY

### ¿Por qué CORS solo en el Gateway?

```
Browser (localhost:5173)
    │
    │  CORS preflight: OPTIONS /api/v1/students
    │  Origin: http://localhost:5173
    │
    ▼
API Gateway (:8080)        ← Responde al preflight aquí
    │  Access-Control-Allow-Origin: http://localhost:5173 ✅
    │
    │  (tráfico interno, no hay CORS)
    ▼
MS Students (:9081)        ← NUNCA recibe tráfico directo del browser
```

**Regla:** CORS es una protección del **browser**. Solo aplica cuando el browser hace una request cross-origin. Los microservicios reciben tráfico del Gateway (servidor a servidor), que **no es cross-origin**.

### Configuración CORS en el Gateway

```yaml
# application.yml del API Gateway — sección CORS
spring:
  cloud:
    gateway:
      globalcors:
        cors-configurations:
          '[/**]':
            allowed-origins:
              - "${CORS_ALLOWED_ORIGINS:http://localhost:5173}"
              - "https://sigei.edu.pe"
            allowed-methods:
              - GET
              - POST
              - PUT
              - PATCH
              - DELETE
              - OPTIONS
            allowed-headers:
              - Authorization
              - Content-Type
              - X-Requested-With
              - Accept
              - Origin
              - X-Institution-Id
            exposed-headers:
              - X-Total-Count
              - X-Total-Pages
            allow-credentials: true
            max-age: 3600
```

### En cada microservicio: QUITAR toda configuración CORS

```java
// ❌ ELIMINAR de CADA microservicio:
@Configuration
public class CorsConfig {
    @Bean
    public CorsWebFilter corsWebFilter() { ... }  // BORRAR
}

// ❌ ELIMINAR de CADA microservicio:
@CrossOrigin(origins = "*")  // BORRAR

// ✅ Los microservicios NO necesitan nada de CORS
// El Gateway ya lo maneja
```

---

## ⚙️ CONFIGURACIÓN DE KEYCLOAK

### 1. Realm: `sigei`

```
Keycloak Admin Console
└── Realm: sigei
    ├── Clients
    │   ├── sigei-frontend    (public client — para React)
    │   └── sigei-gateway     (confidential — para el Gateway)
    │
    ├── Realm Roles
    │   ├── ADMIN
    │   ├── DIRECTOR
    │   ├── SUBDIRECTOR
    │   ├── DOCENTE
    │   ├── AUXILIAR
    │   ├── PSICOLOGO
    │   ├── SECRETARIA
    │   └── APODERADO
    │
    ├── Groups (por institución)
    │   ├── IE-1234567        (código modular)
    │   │   ├── directores
    │   │   ├── docentes
    │   │   └── administrativos
    │   └── IE-7654321
    │       └── ...
    │
    └── User Attributes
        ├── institutionId     (custom attribute)
        ├── institutionName
        └── employeeCode      (código de empleado MINEDU)
```

### 2. Client: `sigei-frontend` (para React)

```json
{
  "clientId": "sigei-frontend",
  "protocol": "openid-connect",
  "publicClient": true,
  "directAccessGrantsEnabled": false,
  "standardFlowEnabled": true,
  "rootUrl": "http://localhost:5173",
  "validRedirectUris": [
    "http://localhost:5173/*",
    "https://sigei.edu.pe/*"
  ],
  "webOrigins": [
    "http://localhost:5173",
    "https://sigei.edu.pe"
  ],
  "defaultClientScopes": ["openid", "profile", "email", "roles"],
  "attributes": {
    "pkce.code.challenge.method": "S256"
  }
}
```

### 3. Client: `sigei-gateway` (para el API Gateway)

```json
{
  "clientId": "sigei-gateway",
  "protocol": "openid-connect",
  "publicClient": false,
  "serviceAccountsEnabled": true,
  "directAccessGrantsEnabled": false,
  "clientAuthenticatorType": "client-secret"
}
```

### 4. Protocol Mapper — Incluir `institutionId` en el token

```
Client Scopes → roles → Mappers → Agregar:

Name: institution-id-mapper
Mapper Type: User Attribute
User Attribute: institutionId
Token Claim Name: institution_id
Claim JSON Type: String
Add to ID token: ON
Add to access token: ON
```

El token JWT resultante:

```json
{
  "sub": "user-uuid-123",
  "realm_access": {
    "roles": ["DOCENTE"]
  },
  "institution_id": "inst-uuid-456",
  "name": "María García López",
  "email": "maria.garcia@ie1234567.edu.pe",
  "preferred_username": "maria.garcia",
  "exp": 1739750400,
  "iss": "http://keycloak:8180/realms/sigei"
}
```

---

## 🔧 IMPLEMENTACIÓN — API GATEWAY CON KEYCLOAK

### pom.xml del Gateway

```xml
<dependencies>
    <!-- Gateway -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-gateway</artifactId>
    </dependency>

    <!-- OAuth2 Resource Server (valida tokens JWT) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
    </dependency>

    <!-- Eureka Client -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
    </dependency>

    <!-- Circuit Breaker -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-circuitbreaker-reactor-resilience4j</artifactId>
    </dependency>
</dependencies>
```

### application.yml del Gateway

```yaml
server:
  port: 8080

spring:
  application:
    name: api-gateway

  # ═══════════════════════════════════════════
  # KEYCLOAK — Validación de tokens JWT
  # ═══════════════════════════════════════════
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${KEYCLOAK_ISSUER_URI:http://localhost:8180/realms/sigei}
          jwk-set-uri: ${KEYCLOAK_JWKS_URI:http://localhost:8180/realms/sigei/protocol/openid-connect/certs}

  # ═══════════════════════════════════════════
  # CORS — SOLO AQUÍ, en NINGÚN microservicio
  # ═══════════════════════════════════════════
  cloud:
    gateway:
      globalcors:
        cors-configurations:
          '[/**]':
            allowed-origins:
              - "${CORS_ALLOWED_ORIGINS:http://localhost:5173}"
            allowed-methods: "*"
            allowed-headers: "*"
            allow-credentials: true
            max-age: 3600

      # Rutas (mismas del doc 04, sin cambios)
      routes:
        - id: institution-service
          uri: lb://vg-ms-institution-management
          predicates:
            - Path=/api/v1/institutions/**,/api/v1/classrooms/**
          filters:
            - TokenRelay=
            - StripPrefix=0

        - id: student-service
          uri: lb://vg-ms-students
          predicates:
            - Path=/api/v1/students/**
          filters:
            - TokenRelay=
            - StripPrefix=0

        - id: enrollment-service
          uri: lb://enrollments-service
          predicates:
            - Path=/api/v1/enrollments/**,/api/v1/academic-periods/**
          filters:
            - TokenRelay=
            - StripPrefix=0

        - id: user-service
          uri: lb://vg-ms-users-management
          predicates:
            - Path=/api/v1/users/**
          filters:
            - TokenRelay=
            - StripPrefix=0

        - id: academic-service
          uri: lb://vg-ms-academic-management
          predicates:
            - Path=/api/v1/courses/**,/api/v1/competencies/**,/api/v1/catalogs/**
          filters:
            - TokenRelay=
            - StripPrefix=0

        - id: notes-service
          uri: lb://vg-ms-notes
          predicates:
            - Path=/api/v1/notes/**,/api/v1/evaluations/**,/api/v1/report-cards/**
          filters:
            - TokenRelay=
            - StripPrefix=0

        - id: assistance-service
          uri: lb://vg-ms-assistance
          predicates:
            - Path=/api/v1/attendance/**,/api/v1/attendance-summary/**
          filters:
            - TokenRelay=
            - StripPrefix=0

        - id: disciplinary-service
          uri: lb://vg-ms-disciplinary-management
          predicates:
            - Path=/api/v1/incidents/**,/api/v1/behavior-records/**
          filters:
            - TokenRelay=
            - StripPrefix=0

        - id: psychology-service
          uri: lb://vg-ms-psychology-welfare
          predicates:
            - Path=/api/v1/psychological-evaluations/**,/api/v1/special-needs/**
          filters:
            - TokenRelay=
            - StripPrefix=0

        - id: teacher-assignment-service
          uri: lb://vg-ms-teacher-assignment
          predicates:
            - Path=/api/v1/teacher-assignments/**
          filters:
            - TokenRelay=
            - StripPrefix=0

        - id: civic-dates-service
          uri: lb://evento-microservice
          predicates:
            - Path=/api/v1/events/**,/api/v1/calendars/**
          filters:
            - TokenRelay=
            - StripPrefix=0

eureka:
  client:
    service-url:
      defaultZone: ${EUREKA_URL:http://localhost:8761/eureka/}
```

### SecurityConfig.java del Gateway

```java
package pe.edu.vallegrande.sigei.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
            // Desactivar CSRF (usamos JWT, no cookies de sesión)
            .csrf(ServerHttpSecurity.CsrfSpec::disable)

            // Rutas públicas vs protegidas
            .authorizeExchange(exchanges -> exchanges
                // Rutas públicas (NO requieren token)
                .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()  // CORS preflight
                .pathMatchers("/actuator/health").permitAll()
                .pathMatchers("/actuator/info").permitAll()

                // ─── Rutas por ROL ───

                // Instituciones — Solo ADMIN y DIRECTOR pueden crear/editar
                .pathMatchers(HttpMethod.POST, "/api/v1/institutions/**")
                    .hasAnyRole("ADMIN", "DIRECTOR")
                .pathMatchers(HttpMethod.PUT, "/api/v1/institutions/**")
                    .hasAnyRole("ADMIN", "DIRECTOR")
                .pathMatchers(HttpMethod.GET, "/api/v1/institutions/**")
                    .authenticated()

                // Estudiantes — CRUD según rol
                .pathMatchers(HttpMethod.POST, "/api/v1/students/**")
                    .hasAnyRole("ADMIN", "DIRECTOR", "SUBDIRECTOR", "SECRETARIA")
                .pathMatchers(HttpMethod.PUT, "/api/v1/students/**")
                    .hasAnyRole("ADMIN", "DIRECTOR", "SUBDIRECTOR", "SECRETARIA")
                .pathMatchers(HttpMethod.GET, "/api/v1/students/**")
                    .hasAnyRole("ADMIN", "DIRECTOR", "SUBDIRECTOR", "DOCENTE",
                                "AUXILIAR", "PSICOLOGO", "SECRETARIA")

                // Matrículas
                .pathMatchers(HttpMethod.POST, "/api/v1/enrollments/**")
                    .hasAnyRole("ADMIN", "DIRECTOR", "SECRETARIA")
                .pathMatchers(HttpMethod.GET, "/api/v1/enrollments/**")
                    .hasAnyRole("ADMIN", "DIRECTOR", "SUBDIRECTOR", "DOCENTE", "SECRETARIA")

                // Notas — Solo DOCENTE puede registrar
                .pathMatchers(HttpMethod.POST, "/api/v1/notes/**")
                    .hasAnyRole("ADMIN", "DIRECTOR", "DOCENTE")
                .pathMatchers(HttpMethod.PUT, "/api/v1/notes/**")
                    .hasAnyRole("ADMIN", "DOCENTE")
                .pathMatchers(HttpMethod.GET, "/api/v1/notes/**")
                    .hasAnyRole("ADMIN", "DIRECTOR", "SUBDIRECTOR", "DOCENTE", "APODERADO")

                // Asistencia
                .pathMatchers(HttpMethod.POST, "/api/v1/attendance/**")
                    .hasAnyRole("ADMIN", "DOCENTE", "AUXILIAR")
                .pathMatchers(HttpMethod.GET, "/api/v1/attendance/**")
                    .hasAnyRole("ADMIN", "DIRECTOR", "SUBDIRECTOR", "DOCENTE",
                                "AUXILIAR", "APODERADO")

                // Disciplina
                .pathMatchers("/api/v1/incidents/**", "/api/v1/behavior-records/**")
                    .hasAnyRole("ADMIN", "DIRECTOR", "SUBDIRECTOR", "DOCENTE", "AUXILIAR")

                // Psicología — Solo PSICÓLOGO y DIRECTOR
                .pathMatchers("/api/v1/psychological-evaluations/**", "/api/v1/special-needs/**")
                    .hasAnyRole("ADMIN", "DIRECTOR", "PSICOLOGO")

                // Gestión Académica
                .pathMatchers(HttpMethod.POST, "/api/v1/courses/**")
                    .hasAnyRole("ADMIN", "DIRECTOR")
                .pathMatchers(HttpMethod.GET, "/api/v1/courses/**")
                    .authenticated()

                // Asignación Docente
                .pathMatchers("/api/v1/teacher-assignments/**")
                    .hasAnyRole("ADMIN", "DIRECTOR", "SUBDIRECTOR")

                // Usuarios — Solo ADMIN
                .pathMatchers("/api/v1/users/**")
                    .hasRole("ADMIN")

                // Todo lo demás requiere autenticación
                .anyExchange().authenticated()
            )

            // Validar tokens JWT con Keycloak
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtAuthenticationConverter(keycloakJwtConverter())
                )
            )
            .build();
    }

    /**
     * Converter que extrae roles de Keycloak del claim "realm_access.roles"
     * y los mapea a Spring Security GrantedAuthority con prefijo ROLE_
     */
    @Bean
    public ReactiveJwtAuthenticationConverterAdapter keycloakJwtConverter() {
        KeycloakGrantedAuthoritiesConverter converter = new KeycloakGrantedAuthoritiesConverter();
        return new ReactiveJwtAuthenticationConverterAdapter(converter);
    }
}
```

### KeycloakGrantedAuthoritiesConverter.java

```java
package pe.edu.vallegrande.sigei.gateway.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Convierte los roles de Keycloak (realm_access.roles) a
 * GrantedAuthorities de Spring Security.
 *
 * Token de Keycloak:
 * {
 *   "realm_access": {
 *     "roles": ["DIRECTOR", "default-roles-sigei"]
 *   }
 * }
 *
 * Se convierte a: ROLE_DIRECTOR
 */
public class KeycloakGrantedAuthoritiesConverter
        implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = extractAuthorities(jwt);
        return new JwtAuthenticationToken(jwt, authorities, jwt.getClaimAsString("preferred_username"));
    }

    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null) {
            return Collections.emptyList();
        }

        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) realmAccess.get("roles");
        if (roles == null) {
            return Collections.emptyList();
        }

        return roles.stream()
            .filter(role -> !role.startsWith("default-roles-"))  // Ignorar roles default
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
            .collect(Collectors.toList());
    }
}
```

### Filtro para propagar datos del token a los microservicios

```java
package pe.edu.vallegrande.sigei.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Extrae datos del JWT validado y los propaga como headers
 * a los microservicios downstream.
 *
 * Los MS NO necesitan validar el token. Solo leen los headers.
 */
@Component
public class UserContextPropagationFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
            .filter(ctx -> ctx.getAuthentication() instanceof JwtAuthenticationToken)
            .map(ctx -> (JwtAuthenticationToken) ctx.getAuthentication())
            .map(auth -> {
                Jwt jwt = auth.getToken();

                ServerHttpRequest request = exchange.getRequest().mutate()
                    // Datos del usuario extraídos del token
                    .header("X-User-Id", jwt.getSubject())
                    .header("X-User-Name", jwt.getClaimAsString("preferred_username"))
                    .header("X-User-Email", jwt.getClaimAsString("email"))
                    .header("X-User-FullName", jwt.getClaimAsString("name"))
                    .header("X-Institution-Id",
                        jwt.getClaimAsString("institution_id") != null
                            ? jwt.getClaimAsString("institution_id") : "")
                    .header("X-User-Roles", String.join(",", extractRoles(jwt)))
                    .build();

                return exchange.mutate().request(request).build();
            })
            .defaultIfEmpty(exchange)
            .flatMap(chain::filter);
    }

    private List<String> extractRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null) return List.of();

        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) realmAccess.get("roles");
        return roles != null ? roles.stream()
            .filter(r -> !r.startsWith("default-roles-"))
            .toList() : List.of();
    }

    @Override
    public int getOrder() {
        return 1;  // Después del filtro de seguridad
    }
}
```

---

## 🔧 IMPLEMENTACIÓN — MICROSERVICIOS (lean)

### Lo que cada MS necesita (muy poco)

```yaml
# application.yml de CADA microservicio
# NO necesita configuración de Keycloak, OAuth, ni CORS
# Solo confía en los headers del Gateway

# (Opcional) Si quieres doble validación:
# spring:
#   security:
#     oauth2:
#       resourceserver:
#         jwt:
#           issuer-uri: ${KEYCLOAK_ISSUER_URI}
```

### Helper para leer el contexto del usuario en cada MS

```java
package pe.edu.vallegrande.sigei.common.security;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;

/**
 * Lee los headers propagados por el API Gateway.
 * Cada MS usa esto en vez de validar tokens.
 */
public record UserContext(
    String userId,
    String username,
    String email,
    String fullName,
    String institutionId,
    List<String> roles
) {
    public static UserContext fromRequest(ServerHttpRequest request) {
        return new UserContext(
            getHeader(request, "X-User-Id"),
            getHeader(request, "X-User-Name"),
            getHeader(request, "X-User-Email"),
            getHeader(request, "X-User-FullName"),
            getHeader(request, "X-Institution-Id"),
            parseRoles(getHeader(request, "X-User-Roles"))
        );
    }

    public boolean hasRole(String role) {
        return roles.contains(role);
    }

    public boolean isFromInstitution(String instId) {
        return this.institutionId.equals(instId);
    }

    private static String getHeader(ServerHttpRequest request, String name) {
        String value = request.getHeaders().getFirst(name);
        return value != null ? value : "";
    }

    private static List<String> parseRoles(String roles) {
        if (roles == null || roles.isBlank()) return List.of();
        return Arrays.asList(roles.split(","));
    }
}
```

### Uso en un Controller de microservicio

```java
@RestController
@RequestMapping("/api/v1/students")
public class StudentController {

    private final FindStudentUseCase findUseCase;

    @GetMapping
    public Flux<StudentResponse> findStudents(
            ServerHttpRequest request,
            @RequestParam(required = false) String status) {

        UserContext user = UserContext.fromRequest(request);

        // Filtrar por institución del usuario (multi-tenancy)
        return findUseCase.findByInstitution(user.institutionId())
            .filter(s -> status == null || s.getStatus().name().equals(status))
            .map(mapper::toResponse);
    }

    @PostMapping
    public Mono<StudentResponse> create(
            ServerHttpRequest request,
            @RequestBody CreateStudentRequest body) {

        UserContext user = UserContext.fromRequest(request);

        // Inyectar institution del usuario logueado
        body.setInstitutionId(user.institutionId());

        return createUseCase.create(mapper.toDomain(body))
            .map(mapper::toResponse);
    }
}
```

---

## 🔧 IMPLEMENTACIÓN — FRONTEND REACT CON KEYCLOAK

### Instalar dependencia

```bash
npm install keycloak-js @react-keycloak/web
```

### keycloak.ts — Configuración del cliente

```typescript
// src/core/auth/keycloak.ts
import Keycloak from 'keycloak-js';

const keycloak = new Keycloak({
  url: import.meta.env.VITE_KEYCLOAK_URL || 'http://localhost:8180',
  realm: import.meta.env.VITE_KEYCLOAK_REALM || 'sigei',
  clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID || 'sigei-frontend',
});

export default keycloak;
```

### KeycloakProvider.tsx

```typescript
// src/core/auth/KeycloakProvider.tsx
import { ReactKeycloakProvider } from '@react-keycloak/web';
import keycloak from './keycloak';
import { LoadingScreen } from '@/shared/components/feedback/LoadingScreen';

interface Props {
  children: React.ReactNode;
}

export function KeycloakProvider({ children }: Props) {
  return (
    <ReactKeycloakProvider
      authClient={keycloak}
      initOptions={{
        onLoad: 'login-required',     // Redirige a login si no autenticado
        checkLoginIframe: false,       // Mejor rendimiento
        pkceMethod: 'S256',            // Seguridad PKCE
      }}
      LoadingComponent={<LoadingScreen message="Autenticando..." />}
      onTokens={({ token }) => {
        if (token) {
          // Guardar token para axios interceptor
          localStorage.setItem('access_token', token);
        }
      }}
    >
      {children}
    </ReactKeycloakProvider>
  );
}
```

### useAuth.ts — Hook de autenticación

```typescript
// src/core/auth/useAuth.ts
import { useKeycloak } from '@react-keycloak/web';
import { useMemo } from 'react';

export type UserRole =
  | 'ADMIN' | 'DIRECTOR' | 'SUBDIRECTOR' | 'DOCENTE'
  | 'AUXILIAR' | 'PSICOLOGO' | 'SECRETARIA' | 'APODERADO';

export interface AuthUser {
  id: string;
  username: string;
  email: string;
  fullName: string;
  roles: UserRole[];
  institutionId: string;
}

export function useAuth() {
  const { keycloak, initialized } = useKeycloak();

  const user = useMemo<AuthUser | null>(() => {
    if (!keycloak.authenticated || !keycloak.tokenParsed) return null;

    const token = keycloak.tokenParsed;
    return {
      id: token.sub ?? '',
      username: token.preferred_username ?? '',
      email: token.email ?? '',
      fullName: token.name ?? '',
      roles: (token.realm_access?.roles ?? [])
        .filter((r: string) => !r.startsWith('default-roles-')) as UserRole[],
      institutionId: token.institution_id ?? '',
    };
  }, [keycloak.authenticated, keycloak.tokenParsed]);

  return {
    isAuthenticated: !!keycloak.authenticated,
    isLoading: !initialized,
    user,
    token: keycloak.token,

    login: () => keycloak.login(),
    logout: () => keycloak.logout({ redirectUri: window.location.origin }),

    hasRole: (roles: UserRole[]) =>
      user ? roles.some(r => user.roles.includes(r)) : false,

    refreshToken: () => keycloak.updateToken(30),
  };
}
```

### main.tsx — Integración

```typescript
// src/main.tsx
import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { KeycloakProvider } from '@/core/auth/KeycloakProvider';
import { AppRouter } from '@/router/AppRouter';
import '@/shared/styles/index.css';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      staleTime: 30_000,
    },
  },
});

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <KeycloakProvider>
      <QueryClientProvider client={queryClient}>
        <AppRouter />
      </QueryClientProvider>
    </KeycloakProvider>
  </StrictMode>
);
```

### interceptors.ts — Actualizado para Keycloak

```typescript
// src/core/api/interceptors.ts
import keycloak from '@/core/auth/keycloak';

export function setupInterceptors(client: AxiosInstance): void {
  client.interceptors.request.use(async (config) => {
    // Auto-refresh token si va a expirar en <30s
    if (keycloak.authenticated) {
      try {
        await keycloak.updateToken(30);
      } catch {
        keycloak.login();
        return Promise.reject('Token refresh failed');
      }
      config.headers.Authorization = `Bearer ${keycloak.token}`;
    }
    return config;
  });

  client.interceptors.response.use(
    (response) => response,
    (error) => {
      if (error.response?.status === 401) {
        keycloak.login();
      }
      return Promise.reject(error);
    }
  );
}
```

### Variables de entorno

```bash
# .env.development
VITE_API_URL=http://localhost:8080
VITE_KEYCLOAK_URL=http://localhost:8180
VITE_KEYCLOAK_REALM=sigei
VITE_KEYCLOAK_CLIENT_ID=sigei-frontend

# .env.production
VITE_API_URL=https://api.sigei.edu.pe
VITE_KEYCLOAK_URL=https://auth.sigei.edu.pe
VITE_KEYCLOAK_REALM=sigei
VITE_KEYCLOAK_CLIENT_ID=sigei-frontend
```

---

## 🐳 DOCKER — Keycloak para desarrollo

```yaml
# docker-compose.yml (desarrollo)
services:
  keycloak-db:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: keycloak
      POSTGRES_USER: keycloak
      POSTGRES_PASSWORD: ${KC_DB_PASSWORD:-keycloak_dev}
    volumes:
      - keycloak_data:/var/lib/postgresql/data
    ports:
      - "5433:5432"

  keycloak:
    image: quay.io/keycloak/keycloak:25.0
    command: start-dev --import-realm
    environment:
      KC_DB: postgres
      KC_DB_URL: jdbc:postgresql://keycloak-db:5432/keycloak
      KC_DB_USERNAME: keycloak
      KC_DB_PASSWORD: ${KC_DB_PASSWORD:-keycloak_dev}
      KC_HOSTNAME: localhost
      KC_HTTP_PORT: 8180
      KEYCLOAK_ADMIN: admin
      KEYCLOAK_ADMIN_PASSWORD: ${KC_ADMIN_PASSWORD:-admin}
    ports:
      - "8180:8180"
    depends_on:
      - keycloak-db
    volumes:
      - ./keycloak/realms:/opt/keycloak/data/import

volumes:
  keycloak_data:
```

---

## 📋 RESUMEN — ¿Quién hace qué?

| Responsabilidad | Keycloak | Gateway | Microservicio |
|----------------|----------|---------|---------------|
| Login / UI de autenticación | ✅ | | |
| Emitir tokens JWT | ✅ | | |
| Gestionar usuarios | ✅ | | |
| Definir roles | ✅ | | |
| **CORS** | | **✅ ÚNICO** | ❌ NO |
| Validar tokens JWT | | ✅ | ❌ NO* |
| Autorización por ruta + rol | | ✅ | |
| Propagar headers de usuario | | ✅ | |
| Leer headers de usuario | | | ✅ |
| Filtrar datos por institución | | | ✅ |
| Rate limiting | | ✅ | |
| Circuit breaker | | ✅ | |

> *Los MS confían en el Gateway. Opcionalmente pueden validar tokens también (defensa en profundidad), pero no es necesario en la mayoría de casos.

---

> **Archivos de arquitectura actualizados:**
>
> - `00` a `07` — Documentación previa
> - `08_SEGURIDAD_KEYCLOAK.md` — **Este documento** (Keycloak + CORS centralizado)
