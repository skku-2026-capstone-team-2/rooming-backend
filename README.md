# Rooming Backend API

This repository contains the Rooming backend API server. It is responsible for authentication, user profile APIs, broker property APIs, seeker target-place APIs, property read APIs, route/location integration, and recommendation result persistence.

## API Contract

The shared frontend/backend contract is maintained here:

```text
src/main/resources/static/openapi.yaml
```

When the server is running, the same contract is served at:

```text
/openapi.yaml
/swagger-ui.html
```

Local URLs:

```text
http://localhost:8080/openapi.yaml
http://localhost:8080/swagger-ui.html
```

## Responsibilities

- Authenticate seeker and broker users through Google OAuth.
- Issue and validate JWT access tokens.
- Store the auth token in an HttpOnly cookie for browser clients.
- Serve current seeker/broker profile APIs.
- Store broker verification information and verification documents.
- Allow verified brokers to register properties.
- Store property metadata, Spline URLs, and property image URLs.
- Receive property image uploads through a storage abstraction.
- Manage seeker target places.
- Call TMAP and ODSAY for route and location data.
- Call the Rooming AI server for recommendations.
- Save recommendation snapshots and favorite state.

## Stack

- Java 21
- Spring Boot 3.5
- Spring Security OAuth2
- JWT
- Spring Data JPA
- PostgreSQL
- Hibernate Spatial / location support
- Gradle Kotlin DSL
- Springdoc OpenAPI

## External Dependencies

This backend expects these external services/configurations:

| Dependency | Used for |
| --- | --- |
| PostgreSQL | Main application database. |
| Google OAuth | Seeker/broker login. |
| TMAP API | Walking routes and nearby infrastructure search. |
| ODSAY API | Public transit routes. |
| Rooming AI server | Recommendation generation through `/recommend`. |
| Image storage | Property image file storage. Currently dummy-backed until S3 is configured. |
| Frontend origin | CORS and OAuth redirect target. |

## Authentication

The backend uses stateless JWT authentication.

After Google OAuth succeeds, the backend creates a JWT and stores it in an HttpOnly cookie:

```text
ROOMING_ACCESS_TOKEN
```

The JWT contains the user id and account type:

```text
SEEKER
BROKER
```

Authenticated API requests can use either:

```http
Authorization: Bearer <jwt>
```

or the cookie. Browser clients should normally use cookie auth:

```js
await fetch(`${API_URL}/api/v1/user/seeker/me`, {
  credentials: "include",
});
```

For production cross-site cookies, keep:

```text
AUTH_COOKIE_SECURE=true
AUTH_COOKIE_SAME_SITE=None
```

## Property Images

Property creation does not receive image files. The flow is:

1. A verified broker creates a property.
2. The broker uploads image files for that property.
3. The backend stores files through `PropertyImageStorageService`.
4. The backend stores only returned image URLs in `Property.imageUrls`.
5. The frontend loads images by URL.

Upload endpoint:

```http
POST /api/v1/properties/{id}/images
Content-Type: multipart/form-data
```

Multipart field name:

```text
images
```

Frontend example:

```js
const formData = new FormData();
files.forEach(file => formData.append("images", file));

await fetch(`${API_URL}/api/v1/properties/${propertyId}/images`, {
  method: "POST",
  credentials: "include",
  body: formData,
});
```

Do not manually set `Content-Type` for multipart requests; the browser must set the boundary.

Only the broker who registered the property can upload or delete images for that property.

Current storage status:

- `PropertyImageStorageService` defines the storage interface.
- `DummyPropertyImageStorageService` returns dummy URLs and does not connect to AWS.
- When S3 is ready, replace the dummy implementation with an S3-backed implementation.
- The database should continue storing image URLs, not image binary data.

## 3D Models

Properties store a Spline URL for 3D model metadata. Spline hosts the actual model, so this backend stores and returns the URL only.

## Environment Variables

The app imports an optional local `.env` file:

```yaml
spring.config.import: optional:file:.env[.properties]
```

Common configuration:

| Variable | Purpose |
| --- | --- |
| `DB_URL` | Full JDBC URL. Overrides host/port/name pieces. |
| `DB_HOST` | PostgreSQL host when `DB_URL` is not set. |
| `DB_PORT` | PostgreSQL port when `DB_URL` is not set. |
| `DB_NAME` | PostgreSQL database name when `DB_URL` is not set. |
| `DB_USERNAME` | PostgreSQL username. |
| `DB_PASSWORD` | PostgreSQL password. |
| `JPA_DDL_AUTO` | Hibernate DDL mode. Default is `update`. |
| `SECRET_HASH_KEY` | JWT signing secret. |
| `GOOGLE_CLIENT_ID` | Google OAuth client id. |
| `GOOGLE_CLIENT_SECRET` | Google OAuth client secret. |
| `OAUTH2_SUCCESS_REDIRECT_URI` | Redirect URL after OAuth success. |
| `FRONTEND_ALLOWED_ORIGINS` | Comma-separated CORS origins. |
| `AUTH_COOKIE_SECURE` | Whether auth cookies require HTTPS. Default `true`. |
| `AUTH_COOKIE_SAME_SITE` | Cookie SameSite value. Default `None`. |
| `OAUTH2_REDIRECT_ACCESS_TOKEN` | Include token in redirect fragment. Default `false`. |
| `TMAP_API_KEY` | TMAP API key. |
| `ODSAY_API_KEY` | ODSAY API key. |
| `ROOMING_AI_BASE_URL` | AI server base URL. |
| `SERVER_PORT` | Spring server port. Default `8080`. |

Example local `.env`:

```properties
DB_HOST=localhost
DB_PORT=5432
DB_NAME=rooming_db
DB_USERNAME=rooming
DB_PASSWORD=rooming_password
SECRET_HASH_KEY=replace-with-a-long-jwt-secret
GOOGLE_CLIENT_ID=replace-with-google-client-id
GOOGLE_CLIENT_SECRET=replace-with-google-client-secret
TMAP_API_KEY=replace-with-tmap-key
ODSAY_API_KEY=replace-with-odsay-key
ROOMING_AI_BASE_URL=http://localhost:8000
FRONTEND_ALLOWED_ORIGINS=http://localhost:5173
AUTH_COOKIE_SECURE=false
AUTH_COOKIE_SAME_SITE=Lax
```

Do not commit real secrets.

## Local Development

Prerequisites:

- Java 21
- PostgreSQL
- Google OAuth credentials
- TMAP and ODSAY API keys
- Reachable AI server when testing recommendations

Run:

```bash
./gradlew bootRun
```

Windows:

```powershell
.\gradlew.bat bootRun
```

## Build

Build the Spring Boot jar:

```bash
./gradlew bootJar
```

Generated artifact:

```text
build/libs/rooming-0.0.1-SNAPSHOT.jar
```

Run:

```bash
java -jar build/libs/rooming-0.0.1-SNAPSHOT.jar
```

## Testing

Run all tests:

```bash
./gradlew test
```

Compile only:

```bash
./gradlew compileJava
```

Some Spring context tests require a reachable PostgreSQL configuration because JPA initializes during startup.

Live external API tests are disabled by default. To enable them:

```bash
./gradlew test -Drooming.live-external-api=true
```

## Project Structure

```text
src/main/java/com/rooming
  common/             shared response DTOs and exception handling
  security/           OAuth2, JWT, and Spring Security
  domain/broker/      broker profile, office, verification, property posting
  domain/seeker/      seeker profile and target places
  domain/property/    property reads, images, and 3D metadata
  domain/locations/   TMAP/ODSAY clients, routes, infrastructure access
  domain/recommendation/ AI recommendation integration and saved results

src/main/resources
  application.yml
  static/openapi.yaml
```