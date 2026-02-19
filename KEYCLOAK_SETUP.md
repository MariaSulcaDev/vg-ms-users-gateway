# Configuración de Keycloak para SIGEI

## 1. Acceder a Keycloak Admin Console

URL: <http://localhost:8080>
Usuario: `admin`
Password: `admin`

## 2. Crear Realm "sigei"

1. Click en **Master** (arriba izquierda)
2. Click en **Create Realm**
3. Nombre: `sigei`
4. Click **Create**

## 3. Crear Client "sigei-gateway"

1. En el realm **sigei**, ir a **Clients**
2. Click **Create client**
3. Configurar:
   - **Client type**: OpenID Connect
   - **Client ID**: `sigei-gateway`
   - Click **Next**
4. Configurar:
   - **Client authentication**: ON
   - **Authorization**: OFF
   - **Authentication flow**:
     - ✅ Standard flow
     - ✅ Direct access grants
   - Click **Next**
5. Configurar URLs:
   - **Root URL**: `http://localhost:8888`
   - **Home URL**: `http://localhost:8888`
   - **Valid redirect URIs**:
     - `http://localhost:8888/*`
     - `http://localhost:9083/*`
   - **Web origins**: `*`
   - Click **Save**

## 4. Configurar Client Secret

1. En el client **sigei-gateway**, ir a la pestaña **Credentials**
2. Copiar el **Client Secret** (o regenerar uno nuevo)
3. Establecer el secret como: `sigei-gateway`
   - Si el secret generado es diferente, puedes cambiarlo manualmente o usar el generado en las variables de entorno

## 5. Crear Roles

1. En el realm **sigei**, ir a **Realm roles**
2. Crear los siguientes roles:
   - `DIRECTOR`
   - `SUBDIRECTOR`
   - `DOCENTE`
   - `AUXILIAR`
   - `PSICOLOGO`
   - `SECRETARIA`
   - `APODERADO`
   - `ADMIN`

## 6. Crear Usuario de Prueba

1. Ir a **Users**
2. Click **Add user**
3. Configurar:
   - **Username**: `testuser`
   - **Email**: `test@vallegrande.edu.pe`
   - **First name**: `Test`
   - **Last name**: `User`
   - **Email verified**: ON
   - Click **Create**
4. En la pestaña **Credentials**:
   - **Set password**: `test123`
   - **Temporary**: OFF
   - Click **Save**
5. En la pestaña **Role mapping**:
   - Click **Assign role**
   - Seleccionar roles (ej: `DIRECTOR`, `ADMIN`)
   - Click **Assign**

## 7. Obtener Token de Prueba

```bash
curl -X POST http://localhost:8080/realms/sigei/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=sigei-gateway" \
  -d "client_secret=sigei-gateway" \
  -d "grant_type=password" \
  -d "username=testuser" \
  -d "password=test123"
```

Copiar el `access_token` de la respuesta.

## 8. Probar API con Token

```bash
# Sin autenticación (dev profile)
curl http://localhost:8888/api/v1/users

# Con autenticación (prod profile)
curl http://localhost:8888/api/v1/users \
  -H "Authorization: Bearer {access_token}"
```

## Configuración de Variables de Entorno

### Gateway (prod)

```bash
KEYCLOAK_ISSUER_URI=http://localhost:8080/realms/sigei
KEYCLOAK_JWK_SET_URI=http://localhost:8080/realms/sigei/protocol/openid-connect/certs
KEYCLOAK_CLIENT_SECRET=sigei-gateway
```

### MS Users (prod)

```bash
KEYCLOAK_ISSUER_URI=http://localhost:8080/realms/sigei
KEYCLOAK_JWK_SET_URI=http://localhost:8080/realms/sigei/protocol/openid-connect/certs
```
