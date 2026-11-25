# API REST con Autenticación por API Key (Spring Boot + Oracle)

Servicio REST desarrollado en **Spring Boot** con **Spring Security** y **Spring Data JPA**, que implementa autenticación stateless mediante la generación y validación temporal de **API Keys** almacenadas en **Oracle Database**.

---

## Arquitectura y Flujo de Autenticación

1. El cliente envía sus credenciales (`userName` y `password`) al endpoint público `POST /api/noauth/apiKey`.
2. El servidor valida el usuario en la tabla `PERSON`, genera una API Key aleatoria segura (`SecureRandom` + Base64URL sin padding) y la registra en `HISTORY_LOG` junto con la fecha y hora de creación (`CREATED_AT`).
3. Para consumir endpoints protegidos bajo `/api/auth/**`, el cliente envía la llave generada en el encabezado HTTP **`X-API-KEY`** (o como parámetro de consulta `X-API-KEY`).
4. El filtro `ApiKeyFilter` intercepta la petición, comprueba que la llave exista en `HISTORY_LOG` y verifica que **no haya transcurrido más de 1 minuto** desde su creación.

---

## Estructura de la Base de Datos (Oracle)

El modelo relacional consta de dos tablas principales (`PERSON` e `HISTORY_LOG`) y dos secuencias para la generación de identificadores:

### 1. Tabla `PERSON`
Almacena los usuarios habilitados para solicitar API Keys.

| Columna | Tipo | Restricciones | Descripción |
| :--- | :--- | :--- | :--- |
| `ID` | `NUMBER` | `PRIMARY KEY` | Identificador único del usuario (generado con `person_seq`). |
| `USERNAME` | `VARCHAR2(250)` | `NOT NULL` | Nombre de usuario para iniciar sesión. |
| `PASSWORD` | `VARCHAR2(500)` | `NOT NULL` | Contraseña del usuario. |

### 2. Tabla `HISTORY_LOG`
Registra cada API Key generada, el usuario al que pertenece y el instante exacto de creación para controlar su expiración.

| Columna | Tipo | Restricciones | Descripción |
| :--- | :--- | :--- | :--- |
| `ID` | `NUMBER` | `PRIMARY KEY` | Identificador único del registro (generado con `log_seq`). |
| `PERSON_ID` | `NUMBER` | `NOT NULL`, `FK -> PERSON(ID)` | Usuario propietario de la API Key. |
| `API_KEY` | `VARCHAR2(500)` | `NOT NULL`, `UNIQUE` | Token único generado para autenticación. |
| `CREATED_AT` | `TIMESTAMP` | `NOT NULL` | Fecha y hora de emisión de la API Key (vigencia de 1 minuto). |

### Script DDL y Datos de Prueba

```sql
CREATE SEQUENCE person_seq
    START WITH 1
    INCREMENT BY 1
    NOCACHE
    NOCYCLE;

CREATE TABLE person (
    id       NUMBER        PRIMARY KEY,
    username VARCHAR2(250) NOT NULL,
    password VARCHAR2(500) NOT NULL
);

CREATE SEQUENCE log_seq
    START WITH 1
    INCREMENT BY 1
    NOCACHE
    NOCYCLE;

CREATE TABLE history_log (
    id           NUMBER        PRIMARY KEY,
    person_id    NUMBER        NOT NULL,
    api_key      VARCHAR2(500) NOT NULL UNIQUE,
    created_at   TIMESTAMP     NOT NULL,
    CONSTRAINT fk_history_person FOREIGN KEY (person_id) REFERENCES person(id)
);

-- Datos iniciales de prueba
INSERT INTO person (id, username, password) VALUES (person_seq.NEXTVAL, 'test', 'test');
INSERT INTO person (id, username, password) VALUES (person_seq.NEXTVAL, 'abraham', 'admin');

COMMIT;

SELECT * FROM person;
SELECT * FROM history_log;
```

> **Nota sobre Oracle SQL Developer:** Recuerda ejecutar `COMMIT;` después de realizar inserciones manuales en la tabla `PERSON` para confirmar la transacción y permitir que la aplicación lea los registros.

---

## Documentación de Endpoints

**URL Base:** `http://localhost:8585/api`

### 1. Generar API Key (Ruta Pública)

Valida las credenciales del usuario y devuelve una nueva API Key válida por **1 minuto**.

* **Método:** `POST`
* **Endpoint:** `/noauth/apiKey`
* **URL completa:** `http://localhost:8585/api/noauth/apiKey`
* **Autenticación:** No requerida.
* **Headers:** `Content-Type: application/json`

#### Request Body (`application/json`)

```json
{
    "userName": "test",
    "password": "test"
}
```

#### Respuesta Exitosa (`200 OK`)

```json
{
    "apiKey": "C6Y-2B4nd5JphJquiAUYNPttCqiOn05N0BWEHpJyuco"
}
```

---

### 2. Endpoint Protegido de Prueba

Valida la vigencia de la API Key mediante el filtro de seguridad y retorna un saludo.

* **Método:** `GET`
* **Endpoint:** `/auth/hola`
* **URL completa:** `http://localhost:8585/api/auth/hola`
* **Autenticación:** Requerida (`X-API-KEY` en Header o Query Param).

#### Opciones de Envío de la API Key

* **Opción A — Por Encabezado HTTP (Recomendado):**
  * **Header:** `X-API-KEY: C6Y-2B4nd5JphJquiAUYNPttCqiOn05N0BWEHpJyuco`
* **Opción B — Por Parámetro de Consulta (Alternativo):**
  * **URL:** `http://localhost:8585/api/auth/hola?X-API-KEY=C6Y-2B4nd5JphJquiAUYNPttCqiOn05N0BWEHpJyuco`

#### Respuesta Exitosa (`200 OK`)

```text
holaMundo
```

---

## Manejo de Errores

El servicio valida tanto las peticiones de inicio de sesión como cada solicitud protegida que pasa por `ApiKeyFilter`. A continuación se detallan todos los posibles códigos y mensajes de error:

| Código HTTP | Estado | Endpoint / Origen | Mensaje Retornado | Causa |
| :--- | :--- | :--- | :--- | :--- |
| `400` | `Bad Request` | `POST /api/noauth/apiKey` | `Usuario con falta de credenciales` | El usuario existe en la base de datos pero su contraseña está nula o vacía. |
| `400` | `Bad Request` | `POST /api/noauth/apiKey` | *(Error de deserialización JSON)* | El cuerpo de la petición está vacío o mal formado. |
| `401` | `Unauthorized` | `POST /api/noauth/apiKey` | `Usuario o Contraseña Invalidos` | El `userName` no existe en `PERSON` o el `password` no coincide. |
| `401` | `Unauthorized` | Rutas protegidas (`/api/auth/**`) | `Error(401): No envio el apiKey` | No se envió `X-API-KEY` ni en los encabezados ni en los parámetros de consulta, o se envió vacío. |
| `401` | `Unauthorized` | Rutas protegidas (`/api/auth/**`) | `Error(401): ApiKey no valida` | La API Key enviada no existe en la tabla `HISTORY_LOG` o está mal escrita. |
| `401` | `Unauthorized` | Rutas protegidas (`/api/auth/**`) | `Error(401): La apiKey no tiene fecha de creacion` | El registro en `HISTORY_LOG` tiene el campo `CREATED_AT` en `NULL`. |
| `401` | `Unauthorized` | Rutas protegidas (`/api/auth/**`) | `Error(401): La apiKey ha expirado` | Ya transcurrió más de **1 minuto** desde `CREATED_AT` al momento de realizar la petición. |
| `404` | `Not Found` | Filtro de seguridad | `Error(404): La ruta de la peticion esta vacia` | La URI de la solicitud llegó nula o vacía. |
| `404` | `Not Found` | Cualquier ruta inexistente | *(Respuesta estándar 404)* | La ruta solicitada no coincide con ningún controlador registrado (por ejemplo, escribir mal `/api/noauth/apiKey` o `/api/auth/hola`). |
| `405` | `Method Not Allowed` | Endpoints existentes | *(Respuesta estándar 405)* | Se utilizó un método HTTP incorrecto (por ejemplo, `GET` en `/api/noauth/apiKey` o `POST` en `/api/auth/hola`). |
| `500` | `Internal Server Error` | `POST /api/noauth/apiKey` | `Error en el servidor: <detalle>` | Fallo inesperado en el servidor o problema de comunicación con la base de datos. |

---

## Flujo de Prueba en Postman

1. Realizar `POST http://localhost:8585/api/noauth/apiKey` con `{"userName": "test", "password": "test"}` para obtener una `apiKey` (`200 OK`).
2. Antes de que transcurra **1 minuto**, realizar `GET http://localhost:8585/api/auth/hola` enviando el encabezado `X-API-KEY` con la llave generada para recibir `holaMundo` (`200 OK`).
3. Esperar más de **1 minuto** y repetir la petición `GET` para verificar la respuesta `401 Unauthorized` (`Error(401): La apiKey ha expirado`).