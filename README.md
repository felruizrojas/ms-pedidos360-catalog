# ms-pedidos360-catalog

Microservicio de catálogo de Pedidos360 (Spring Boot 4.1.1, Java 25, PostgreSQL, puerto 8081).
Es consumido por el BFF, que reenvía el JWT del usuario; este servicio lo valida por sí mismo
(firma, vigencia, issuer y audience).

## Variables de entorno

| Variable | Descripción | Default |
|---|---|---|
| `DB_URL` | URL JDBC de PostgreSQL | `jdbc:postgresql://localhost:5432/pedidos360_catalog` |
| `DB_USERNAME` | Usuario de BD | `postgres` |
| `DB_PASSWORD` | Contraseña de BD | `postgres` |
| `ENTRA_ISSUER_URI` | Issuer del token (Entra ID) | tenant del curso |
| `ENTRA_API_CLIENT_ID` | Audience esperada (client id de la API) | app del curso |
| `ENFORCE_ROLES` | Si es `true`, POST exige rol `Admin` u `Operador` | `false` |
| `CORS_ALLOWED_ORIGINS` | Orígenes CORS separados por coma; vacío = sin CORS | vacío |

## Levantar el servicio

```bash
./mvnw spring-boot:run
./mvnw clean verify   # tests (usan H2, no requieren PostgreSQL)
```

## Endpoints

- `GET /api/catalog/products`, `GET /api/catalog/products/{id}`: scope `access_as_user`.
- `POST /api/catalog/products`: scope `access_as_user` (+ rol `Admin`/`Operador` si `ENFORCE_ROLES=true`).
- `GET /actuator/health`: público.

Errores: `{timestamp, status, error, mensaje}` (validación agrega `detalles`).
401 = token ausente/inválido/expirado/audience incorrecta; 403 = falta scope o rol.

## Ejemplos curl

```bash
# Salud (sin token)
curl http://localhost:8081/actuator/health

# Sin token -> 401
curl -i http://localhost:8081/api/catalog/products

# Con token
TOKEN="<access token de Entra ID>"
curl -H "Authorization: Bearer $TOKEN" http://localhost:8081/api/catalog/products
curl -X POST http://localhost:8081/api/catalog/products \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"nombre":"Teclado","descripcion":"USB","precio":19990.50,"stock":5}'
```
