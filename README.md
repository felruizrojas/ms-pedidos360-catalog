# ms-pedidos360-catalog

Microservicio de catálogo de **Pedidos360** (DSY1107). Spring Boot 4.1.1 · Java 25 · PostgreSQL · puerto **8081**.
Solo lo consume el BFF, que le reenvía el JWT del usuario; este servicio **vuelve a validarlo** (defensa en profundidad).

## Qué cubre de la evaluación
| Requisito | Implementación |
|---|---|
| Microservicio Spring Boot que compila y pasa pruebas | `./mvnw clean verify` (tests con H2) |
| Integración con BD cloud | Entidad `Producto`, `ProductoRepository` (JPA), `DB_URL/DB_USERNAME/DB_PASSWORD`. En AWS: PostgreSQL en Docker en la EC2 |
| Filtro que valida el JWT del IDaaS | OAuth2 Resource Server: firma (JWKS de Entra ID), `exp/nbf`, `issuer`, `audience` |
| Autorización | `/api/catalog/**` exige scope `access_as_user`; con `ENFORCE_ROLES=true`, POST exige rol `Admin`/`Operador` |
| `.gitignore` | Excluye `target/`, IDE y `.DS_Store` |
| Despliegue | GitHub Actions: `verify` → SCP del jar → `systemctl restart pedidos360-catalog` en EC2 |

## Endpoints
| Método | Ruta | Acceso |
|---|---|---|
| GET | `/api/catalog/products` | scope `access_as_user` |
| GET | `/api/catalog/products/{id}` | scope `access_as_user` |
| POST | `/api/catalog/products` | scope (+ rol Admin/Operador si `ENFORCE_ROLES=true`) |
| GET | `/actuator/health`, `/swagger-ui.html`, `/v3/api-docs` | público |

Errores JSON `{timestamp, status, error, mensaje}` (+ `detalles` en validación). 401 = token ausente/inválido/expirado/audience incorrecta · 403 = falta scope o rol · 400 = validación o nombre duplicado · 404 = no existe.

## Variables de entorno
| Variable | Default |
|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/pedidos360_catalog` |
| `DB_USERNAME` / `DB_PASSWORD` | `postgres` / `postgres` |
| `ENTRA_ISSUER_URI` | `https://login.microsoftonline.com/0bfad962-b91d-465a-b769-8e71565efe7d/v2.0` |
| `ENTRA_API_CLIENT_ID` (audience) | `d80d5009-1b8a-49b2-a3da-08a8bbd00936` |
| `ENFORCE_ROLES` | `false` |
| `CORS_ALLOWED_ORIGINS` | vacío (sin CORS: el navegador nunca llama directo al catálogo) |

## Ejecutar
```bash
docker run -d --name pg-catalog -p 5432:5432 -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=pedidos360_catalog postgres:16
./mvnw spring-boot:run      # carga productos semilla (productos-seed.json) si la tabla está vacía
./mvnw clean verify         # tests (H2, no requieren PostgreSQL ni Azure)
```

## Swagger (OpenAPI)
| | Local | AWS |
|---|---|---|
| URL | http://localhost:8081/swagger-ui.html | puerto 8081 cerrado a Internet; usar túnel SSH ↓ |
| Comando | `open http://localhost:8081/swagger-ui.html` | `ssh -i <llave>.pem -N -L 8081:localhost:8081 ec2-user@52.71.122.5` y luego `open http://localhost:8081/swagger-ui.html` |
| Spec JSON | `curl http://localhost:8081/v3/api-docs` | ídem, con el túnel abierto |

Documenta el contrato propio del microservicio (el BFF expone el mismo contrato hacia el front).

## Pruebas manuales (curl)

`TOKEN`: access token de Entra ID con scope `api://d80d5009-1b8a-49b2-a3da-08a8bbd00936/access_as_user`.
Se obtiene iniciando sesión en el frontend → DevTools → Network → cualquier llamada a `/api/...` → header `Authorization` (copiar lo que va después de `Bearer `).

```bash
BASE=http://localhost:8081
TOKEN="<access token>"

curl -i $BASE/actuator/health                                             # 200 {"status":"UP"}
curl -i $BASE/api/catalog/products                                        # 401 sin token
curl -i -H "Authorization: Bearer abc.def.ghi" $BASE/api/catalog/products # 401 token inválido
curl -i -H "Authorization: Bearer $TOKEN" $BASE/api/catalog/products      # 200 lista JSON
curl -i -H "Authorization: Bearer $TOKEN" $BASE/api/catalog/products/1    # 200 producto
curl -i -H "Authorization: Bearer $TOKEN" $BASE/api/catalog/products/99999 # 404

curl -i -X POST $BASE/api/catalog/products \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"nombre":"Teclado","descripcion":"Mecanico USB","precio":19990,"stock":5}'   # 201 (repetirlo → 400 nombre duplicado)

curl -i -X POST $BASE/api/catalog/products \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"nombre":"","precio":-1,"stock":-5}'                                         # 400 con "detalles"
```

**AWS:** el puerto 8081 **no está expuesto a Internet** (solo lo alcanza el BFF dentro de la EC2), por lo que no hay curl público distinto.
Los **mismos comandos** sirven ejecutándolos por SSH dentro de la instancia (`BASE=http://localhost:8081`).
Desde fuera, el catálogo se prueba a través del API Gateway (ver README del BFF).
