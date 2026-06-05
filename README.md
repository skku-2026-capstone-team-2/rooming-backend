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
- Keep nearby infrastructure and walking accessibility data synchronized for properties.
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
| Amazon S3 | Property image file storage. |
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

Current storage configuration:

- `PropertyImageStorageService` defines the storage interface.
- `S3PropertyImageStorageService` uploads and deletes images from S3.
- Default bucket name is `rooming-property-image`.
- The EC2 instance should have an IAM role that can read/write/delete objects in the bucket.
- Default region is `ap-northeast-2`; override it with `PROPERTY_IMAGE_S3_REGION` when needed.
- Set `PROPERTY_IMAGE_PUBLIC_BASE_URL` if returned image URLs should use CloudFront or another public domain.
- The database should continue storing image URLs, not image binary data.

The EC2 IAM role gives this backend permission to upload/delete images. It does not by itself make image URLs readable by browsers. For frontend display, use a public-read bucket policy for these objects or put CloudFront in front of the bucket and set `PROPERTY_IMAGE_PUBLIC_BASE_URL`.

## 3D Models

Properties store a Spline URL for 3D model metadata. Spline hosts the actual model, so this backend stores and returns the URL only.

## Infrastructure Sync

The backend maintains nearby infrastructure data for properties using TMAP POI search.

When a verified broker creates a property, the backend immediately stores nearby `Infrastructure` records and matching `InfraAccessibility` walking records for that property.

The same infrastructure sync also runs automatically:

- once after the application finishes startup,
- then once per day by scheduler, defaulting to 3:00 AM Korea time.

During each scheduled run, properties without infrastructure accessibility data are processed first. Properties that already have accessibility data are processed afterward to refresh stale POI selections.

For each property, the backend stores up to two closest infrastructures per `INFRA_CATEGORY`. If the closest POIs change over time, the refresh process creates the new selected infrastructure/accessibility records and removes obsolete property accessibility links. An infrastructure record is deleted only when no property still references it.

TMAP usage is quota-limited. If the TMAP POI API returns a quota or limit error during sync, the backend keeps any POIs and accessibility records already stored during that run, records the quota stop for the current Korean date, and stops processing remaining properties until the next day.

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
| `PROPERTY_IMAGE_S3_BUCKET` | S3 bucket for property images. Default `rooming-property-image`. |
| `PROPERTY_IMAGE_S3_REGION` | S3 bucket region. Default `ap-northeast-2`. |
| `PROPERTY_IMAGE_PUBLIC_BASE_URL` | Optional public base URL such as a CloudFront domain. |
| `INFRASTRUCTURE_SYNC_DAILY_ENABLED` | Whether scheduled infrastructure sync is enabled. Default `true`. |
| `INFRASTRUCTURE_SYNC_DAILY_CRON` | Daily infrastructure sync cron expression in Korea time. Default `0 0 3 * * *`. |
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
PROPERTY_IMAGE_S3_BUCKET=rooming-property-image
PROPERTY_IMAGE_S3_REGION=ap-northeast-2
INFRASTRUCTURE_SYNC_DAILY_ENABLED=true
INFRASTRUCTURE_SYNC_DAILY_CRON=0 0 3 * * *
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
