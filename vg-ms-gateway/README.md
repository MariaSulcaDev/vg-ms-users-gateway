# Gateway SIGEI

API Gateway para el sistema SIGEI con integración de Keycloak y Resilience4j.

## Servicios Integrados

- **vg-ms-users-management**: `/api/users/**`

## Configuración

### Perfiles

- **dev**: Sin autenticación (permitAll), URL directa a localhost
- **prod**: Autenticación completa con Keycloak, URLs por variables de entorno

### Variables de Entorno (prod)

```bash
SERVER_PORT=8888
KEYCLOAK_ISSUER_URI=http://localhost:8080/realms/sigei
KEYCLOAK_JWK_SET_URI=http://localhost:8080/realms/sigei/protocol/openid-connect/certs
KEYCLOAK_CLIENT_SECRET=sigei-gateway
USERS_SERVICE_URL=http://vg-ms-users-management:9083
```

### Ejecución

```bash
# Desarrollo
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Producción
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

## Características

- **Circuit Breaker (Resilience4j)**: Fallback automático cuando un servicio no responde
- **CORS**: Configurado globalmente
- **Keycloak**: Validación de JWT con extracción de roles de realm

## Endpoints

- **Actuator**: `http://localhost:8888/actuator`
- **Gateway Routes**: `http://localhost:8888/actuator/gateway/routes`
- **Fallback Users**: `http://localhost:8888/fallback/users`
