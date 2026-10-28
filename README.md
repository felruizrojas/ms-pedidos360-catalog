
# ms-pedidos360-catalog

Microservicio de catálogo de **Pedidos360** (DSY1107). Spring Boot 4.1.1 · Java 25 · PostgreSQL · puerto **8081**.
Solo lo consume el BFF, que le reenvía el JWT del usuario; este servicio **vuelve a validarlo** (defensa en profundidad).

## Qué cubre de la evaluación
| Requisito | Implementación |
|---|---|
| Microservicio Spring Boot que compila y pasa pruebas | `./mvnw clean verify` (tests con H2) |
| Integración con BD | Entidad `Producto`, `ProductoRepository` (JPA), `DB_URL/DB_USERNAME/DB_PASSWORD` |
| Filtro que valida el JWT del IDaaS | OAuth2 Resource Server: firma (JWKS de Entra ID), `exp/nbf`, `issuer`, `audience` |
| Autorización | `/api/catalog/**` exige scope `access_as_user`; con `ENFORCE_ROLES=true`, POST/PUT/DELETE exigen rol `Admin`/`Operador` |
| Coordinación con orders | Descuento de stock atómico y todo-o-nada al aceptar un pedido; reposición al cancelarlo |
| Eliminación de productos | Borrado lógico (`eliminado`, `eliminadoEn`): la fila se conserva para los pedidos ya creados |
| `.gitignore` | Excluye `target/`, IDE y `.DS_Store` |

## Endpoints
| Método | Ruta | Acceso |
|---|---|---|
| GET | `/api/catalog/products` | scope `access_as_user` |
| GET | `/api/catalog/products/{id}` | scope `access_as_user` |
| POST | `/api/catalog/products` | scope (+ rol Admin/Operador si `ENFORCE_ROLES=true`) |
| PUT | `/api/catalog/products/{id}` | scope (+ rol Admin/Operador si `ENFORCE_ROLES=true`) |
| DELETE | `/api/catalog/products/{id}` | scope (+ rol Admin/Operador si `ENFORCE_ROLES=true`) · borrado lógico, **204** sin cuerpo |
| GET | `/api/catalog/products/eliminados` | scope (+ rol Admin/Operador si `ENFORCE_ROLES=true`) · productos con borrado lógico |
| PUT | `/api/catalog/products/{id}/restaurar` | scope (+ rol Admin/Operador si `ENFORCE_ROLES=true`) · deshace el borrado lógico, **200** con el producto (idempotente; 404 si no existe) |
| POST | `/api/catalog/products/stock/decrease` | scope (+ rol Admin/Operador si `ENFORCE_ROLES=true`) · lo usa orders al **aceptar** un pedido |
| POST | `/api/catalog/products/stock/increase` | scope (+ rol Admin/Operador si `ENFORCE_ROLES=true`) · lo usa orders al **cancelar** un pedido aceptado |
| GET | `/actuator/health`, `/swagger-ui.html`, `/v3/api-docs` | público |

Errores JSON `{timestamp, status, error, mensaje}` (+ `detalles` en validación, stock insuficiente y producto eliminado). 401 = token ausente/inválido/expirado/audience incorrecta · 403 = falta scope o rol · 400 = validación o nombre duplicado · 404 = no existe o fue eliminado · 409 = stock insuficiente o producto eliminado al descontar.

### Stock (`/stock/decrease` y `/stock/increase`)
Body: `{"items":[{"productoId":1,"cantidad":2},{"productoId":4,"cantidad":1}]}` → **204** sin cuerpo.

- **Todo o nada**: una sola `@Transactional`. Cada ítem se descuenta con `UPDATE producto SET stock = stock - :cantidad WHERE id = :id AND stock >= :cantidad`; si alguna fila no se actualiza se lanza la excepción y se revierten también los ítems anteriores (no hay descuentos parciales). Dos pedidos concurrentes nunca dejan el stock negativo: la condición la evalúa la BD sobre la fila bloqueada.
- Un mismo producto repetido en `items` se suma antes de descontar; las filas se procesan en orden de id (evita deadlocks entre transacciones concurrentes).
- Stock insuficiente → **409**:
  ```json
  {"timestamp":"...","status":409,"error":"Conflict",
   "mensaje":"Stock insuficiente para \"Pan Amasado\" (id 1): solicitado 5, disponible 2",
   "detalles":{"productoId":1,"solicitado":5,"disponible":2}}
  ```
- Producto inexistente → **404** (y tampoco se aplica ningún cambio). `items` vacío o `cantidad <= 0` → **400** con `detalles`.
- Producto eliminado → **409** (sin cambios parciales):
  ```json
  {"timestamp":"...","status":409,"error":"Conflict",
   "mensaje":"Producto 'Pan Amasado' ya no está disponible",
   "detalles":{"productoId":1,"motivo":"Producto 'Pan Amasado' ya no está disponible"}}
  ```
- `/stock/increase` **sí** funciona con productos eliminados, para que cancelar un pedido aceptado no pierda unidades.

### Borrado lógico (`DELETE /api/catalog/products/{id}`)
- Marca `eliminado=true` y `eliminadoEn=<ahora>`; la fila sigue en la BD porque los pedidos guardan su `productoId` y su nombre.
- Después del borrado, el producto no aparece en el listado y `GET`/`PUT`/`DELETE` sobre él responden **404**, igual que si no existiera.
- Efecto en el stock: `/stock/decrease` responde **409** (orders no puede aceptar un pedido que lo incluya) y `/stock/increase` sigue sumando.
- El nombre sigue ocupado (`unique`): crear otro producto con ese nombre responde **400** con `detalles.nombre = "Existe un producto eliminado con este nombre."`.

## Decisiones de diseño
- **Filtro explícito en el repositorio** (`findAllByEliminadoFalse`, `findByIdAndEliminadoFalse`, `AND p.eliminado = false` en el descuento) en vez de `@SQLDelete`/`@SQLRestriction`, para que la reposición de stock siga viendo los productos eliminados.
- **Sin Flyway**: la columna la agrega `ddl-auto: update`; `columnDefinition = "boolean default false"` permite agregarla a una tabla con filas existentes.
- **Eliminado en el descuento → 409** (no 404): el producto existió y el pedido lo referencia, así que es un conflicto de negocio. Su `detalles` es un objeto para que orders lo reenvíe tal cual.
- **Restaurar productos queda fuera de alcance**: no hay endpoint para deshacer el borrado, y el nombre de un producto eliminado no se puede reutilizar.

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
| | |
|---|---|
| URL | http://localhost:8081/swagger-ui.html |
| Spec JSON | `curl http://localhost:8081/v3/api-docs` |

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

# Stock (con ENFORCE_ROLES=true el token debe tener rol Admin u Operador)
curl -i -X POST $BASE/api/catalog/products/stock/decrease \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"items":[{"productoId":1,"cantidad":2},{"productoId":4,"cantidad":1}]}'     # 204 (baja el stock de ambos)

curl -i -X POST $BASE/api/catalog/products/stock/decrease \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"items":[{"productoId":1,"cantidad":1},{"productoId":4,"cantidad":99999}]}' # 409 "detalles" (el producto 1 tampoco cambia)

curl -i -X POST $BASE/api/catalog/products/stock/decrease \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"items":[{"productoId":99999,"cantidad":1}]}'                               # 404

curl -i -X POST $BASE/api/catalog/products/stock/increase \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"items":[{"productoId":1,"cantidad":2},{"productoId":4,"cantidad":1}]}'     # 204 (repone)

# Borrado lógico (con ENFORCE_ROLES=true el token debe tener rol Admin u Operador)
curl -i -X DELETE -H "Authorization: Bearer $TOKEN" $BASE/api/catalog/products/4  # 204 (repetirlo → 404)
curl -i -H "Authorization: Bearer $TOKEN" $BASE/api/catalog/products/4            # 404
```

#evaluacion 2