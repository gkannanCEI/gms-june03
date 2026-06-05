# Grant Management Base Framework — Environment & Infrastructure Setup

---

## 1. Database

> **Recommended for local development:** PostgreSQL is the default profile and requires the
> least setup overhead. Oracle is fully supported for production deployments but requires
> additional setup (driver download, larger runtime). All features work identically on both.

The application is database-agnostic, supporting **PostgreSQL** (default) and **Oracle** via
Spring profiles. The active database is selected by setting `SPRING_PROFILES_ACTIVE`.

### PostgreSQL (Default)

PostgreSQL 14 or later is recommended.

**Schema setup:**
```sql
CREATE DATABASE gmsdb;
CREATE USER gms_owner WITH PASSWORD 'localpassword';
GRANT ALL PRIVILEGES ON DATABASE gmsdb TO gms_owner;
```

**Connection:**
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/gmsdb
    username: gms_owner
    password: localpassword
```

**Profile activation:** `SPRING_PROFILES_ACTIVE=postgresql` (this is the default if not set)

### Oracle (Alternative)

Oracle 19c or later. Use `ojdbc11` for Java 17+.

**Schema setup:**
```sql
CREATE USER gms_owner IDENTIFIED BY <password>
  DEFAULT TABLESPACE users
  TEMPORARY TABLESPACE temp
  QUOTA UNLIMITED ON users;

GRANT CONNECT, RESOURCE TO gms_owner;
GRANT CREATE SESSION, CREATE TABLE, CREATE SEQUENCE,
      CREATE INDEX, CREATE VIEW TO gms_owner;
```

**Connection:**
```yaml
spring:
  datasource:
    url: jdbc:oracle:thin:@//db-host:1521/GMSDB
    username: gms_owner
    password: <password>
```

For Oracle RAC or TNS alias connections:
```
jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS=(PROTOCOL=TCP)(HOST=db-host)(PORT=1521))(CONNECT_DATA=(SERVICE_NAME=GMSDB)))
```

**Profile activation:** `SPRING_PROFILES_ACTIVE=oracle`

### Connection Pool (HikariCP)

Spring Boot uses HikariCP by default. Recommended pool settings (same for both databases):

```yaml
spring:
  datasource:
    hikari:
      pool-name: GmsHikariPool
      minimum-idle: 5
      maximum-pool-size: 20
      idle-timeout: 300000        # 5 minutes
      connection-timeout: 30000   # 30 seconds
      max-lifetime: 1800000       # 30 minutes
```

### JPA / Hibernate

The dialect is set automatically by the profile-specific config (`application-postgresql.yml`
or `application-oracle.yml`). Common JPA settings:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate          # never auto-create in production; use Flyway
    show-sql: false
    properties:
      hibernate:
        format_sql: false
        jdbc:
          batch_size: 50
        order_inserts: true
        order_updates: true
```

### Flyway Migrations

Migrations are stored in database-specific directories:
- `src/main/resources/db/migration/postgresql/` — PostgreSQL DDL
- `src/main/resources/db/migration/oracle/` — Oracle DDL

The active profile's `application-{profile}.yml` sets `spring.flyway.locations` to the correct
directory. Both directories contain the same logical migrations (V1, V2, V3) with
database-appropriate syntax.

**Version requirement:** Flyway 11.8+ is required for PostgreSQL 18 support. The
`flyway.version` property in `pom.xml` overrides the Spring Boot BOM's default version:

```xml
<properties>
    <flyway.version>11.8.0</flyway.version>
</properties>
```

The `flyway-database-postgresql` module must also be included as a dependency (in addition to
`flyway-core`) for PostgreSQL support in Flyway 10+.

### Environment Variables (Database)

| Variable | Description | PostgreSQL Example | Oracle Example |
|---|---|---|---|
| `DB_URL` | JDBC connection string | `jdbc:postgresql://localhost:5432/gmsdb` | `jdbc:oracle:thin:@//db-host:1521/GMSDB` |
| `DB_USERNAME` | Schema user | `gms_owner` | `gms_owner` |
| `DB_PASSWORD` | Schema password | `localpassword` | *(from secrets manager)* |
| `SPRING_PROFILES_ACTIVE` | Database profile | `postgresql` (default) | `oracle` |

---

## 2. Azure AD B2C — Backend (Spring Boot)

### How It Works

Azure AD B2C issues JWT access tokens to Angular clients after user sign-in. The Spring Boot
application validates every inbound JWT using the B2C tenant's public signing keys (fetched
automatically from the JWKS endpoint). No client secret is needed on the backend — only the
tenant and audience values are required.

### Required B2C Tenant Information

Obtain these values from the Azure portal under your B2C tenant → App registrations:

| Value | Where to find it | Used as |
|---|---|---|
| Tenant name | B2C tenant overview | `{tenant}.onmicrosoft.com` |
| Tenant ID (GUID) | B2C tenant overview | Issuer URL component |
| Backend App (API) Client ID | App registration for the Spring Boot API | `spring.security.oauth2.resourceserver.jwt.audiences` |
| User Flow / Policy name | B2C User flows blade | Issuer URL component (e.g. `B2C_1_signin`) |

### Spring Security Configuration

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${B2C_ISSUER_URI}
          audiences: ${B2C_API_CLIENT_ID}
```

The `issuer-uri` for a B2C user flow follows this pattern:
```
https://{tenant}.b2clogin.com/{tenant}.onmicrosoft.com/{policy}/v2.0/
```

Example:
```
https://mygmstenant.b2clogin.com/mygmstenant.onmicrosoft.com/B2C_1_signin/v2.0/
```

Spring Security will auto-fetch the JWKS from `{issuer-uri}/.well-known/openid-configuration`
on startup and cache the signing keys.

### JWT Claim Mapping

B2C embeds custom attributes in the token as claims. The application reads two claims:

| Claim key | Purpose | Configured in |
|---|---|---|
| `roles` | Array of role strings (e.g. `["ADMIN"]`, `["APPLICANT"]`) | B2C custom policy or app role assignment |
| `extension_organizationId` | The user's linked organization ID (numeric string) | B2C custom attribute `organizationId` |

> **Note:** B2C custom attributes are prefixed with `extension_` in the token by default.
> If using app roles instead of custom policies, the claim key will be `roles` (standard).
> Confirm the exact claim key names by decoding a real token from your tenant using
> [jwt.ms](https://jwt.ms) before wiring up `SecurityContextProvider`.

### SecurityContextProvider Implementation

```java
@Component
public class SecurityContextProvider {

    public UserPrincipal extractPrincipal(JwtAuthenticationToken token) {
        Jwt jwt = token.getToken();
        String userId = jwt.getSubject();
        List<String> roles = jwt.getClaimAsStringList("roles");
        String orgIdClaim = jwt.getClaimAsString("extension_organizationId");
        Long organizationId = orgIdClaim != null ? Long.parseLong(orgIdClaim) : null;
        return new UserPrincipal(userId, roles, organizationId);
    }
}
```

Adjust the claim key names (`"roles"`, `"extension_organizationId"`) to match what your
specific B2C tenant and policy actually emit.

### Environment Variables (Backend Auth)

| Variable | Description | Example |
|---|---|---|
| `B2C_ISSUER_URI` | Full issuer URI including policy name | `https://tenant.b2clogin.com/tenant.onmicrosoft.com/B2C_1_signin/v2.0/` |
| `B2C_API_CLIENT_ID` | Client ID of the backend API app registration | `xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx` |

---

## 3. Spring Boot — Full application.yml Structure

```yaml
# ── Server ──────────────────────────────────────────────────────────────────
server:
  port: ${SERVER_PORT:8081}
  error:
    whitelabel:
      enabled: false

# ── Datasource ───────────────────────────────────────────────────────────────
spring:
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:postgresql}

  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    hikari:
      pool-name: GmsHikariPool
      minimum-idle: 5
      maximum-pool-size: 20
      idle-timeout: 300000
      connection-timeout: 30000
      max-lifetime: 1800000

  # ── JPA ──────────────────────────────────────────────────────────────────
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false

  # ── Security / OAuth2 ────────────────────────────────────────────────────
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${B2C_ISSUER_URI}
          audiences: ${B2C_API_CLIENT_ID}

# ── Logging ──────────────────────────────────────────────────────────────────
logging:
  level:
    root: WARN
    com.yourorg.gms: INFO
    org.springframework.security: WARN
    org.hibernate.SQL: WARN

# ── Application-specific ─────────────────────────────────────────────────────
gms:
  security:
    auth-mode: ${GMS_AUTH_MODE:local}
    role-claim-key: ${GMS_ROLE_CLAIM_KEY:roles}
    org-id-claim-key: ${GMS_ORG_ID_CLAIM_KEY:extension_organizationId}
  cors:
    allowed-origins: ${GMS_CORS_ORIGINS:http://localhost:42001}
  dynamic-data:
    allowed-targets:
      - table: gms_application_data
        columns: [value_text]
      # Add domain-specific tables here, e.g.:
      # - table: gms_applicant_profile
      #   columns: [first_name, last_name, date_of_birth, phone, zip_code]
      # - table: gms_project_details
      #   columns: [project_title, project_description, requested_amount, start_date]
  file-storage:
    connection-string: ${AZURE_STORAGE_CONNECTION_STRING}
    container-name: gms-attachments
    sas-expiry-minutes: 5
    max-file-size-mb: 100    # hard ceiling; per-question limit may be lower
```

> **Note:** Database-specific settings (`driver-class-name`, `hibernate.dialect`,
> `flyway.locations`, `connection-test-query`) are defined exclusively in profile-specific
> YAML files (`application-postgresql.yml`, `application-oracle.yml`) and must not appear
> in the base `application.yml`.

### All Required Environment Variables

| Variable | Required | Default | Description |
|---|---|---|---|
| `DB_URL` | Yes | — | Oracle JDBC URL |
| `DB_USERNAME` | Yes | — | Oracle schema user |
| `DB_PASSWORD` | Yes | — | Oracle schema password |
| `B2C_ISSUER_URI` | Yes (b2c mode) | — | Azure AD B2C issuer URI (includes policy name) |
| `B2C_API_CLIENT_ID` | Yes (b2c mode) | — | Backend API app registration client ID |
| `SERVER_PORT` | No | `8081` | HTTP port |
| `GMS_AUTH_MODE` | No | `local` | Authentication mode: `b2c` (JWT) or `local` (HTTP Basic against gms_user table). Drives conditional bean selection in SecurityConfig. |
| `GMS_ROLE_CLAIM_KEY` | No | `roles` | JWT claim key for role array (b2c mode only). Used by SecurityContextProvider. |
| `GMS_ORG_ID_CLAIM_KEY` | No | `extension_organizationId` | JWT claim key for organization ID (b2c mode only). Used by SecurityContextProvider. |
| `GMS_CORS_ORIGINS` | No | `http://localhost:42001` | Comma-separated allowed CORS origins. Applied to all paths by SecurityConfig. |
| `SPRING_PROFILES_ACTIVE` | No | `postgresql` | Active Spring profile. Controls database dialect, Flyway migration locations, and profile-specific YAML overrides. Valid values: `postgresql`, `oracle`. |
| `AZURE_STORAGE_CONNECTION_STRING` | Yes | — | Azure Blob Storage connection string for file attachments |

> **Dynamic data allowlist** (`gms.dynamic-data.allowed-targets`) is configured directly in
> `application.yml` (or a profile-specific override) rather than as an environment variable,
> because it is a structured list of table/column pairs. In containerised deployments, mount
> a profile-specific `application-prod.yml` as a ConfigMap or volume rather than trying to
> express the list as a single env var.

### Spring Profiles

The `spring.profiles.active` property selects the active Spring profile, which controls
database dialect, Flyway migration locations, and profile-specific YAML overrides.

| Property | `spring.profiles.active` |
|---|---|
| Environment variable | `SPRING_PROFILES_ACTIVE` |
| Default | `postgresql` |
| Valid values | `postgresql`, `oracle` |
| Configured in | `application.yml` |

**What the active profile controls:**
- **Database dialect** — `application-postgresql.yml` sets the Hibernate dialect to PostgreSQL;
  `application-oracle.yml` sets it to Oracle.
- **Flyway migration locations** — Each profile points `spring.flyway.locations` to the
  database-appropriate migration directory (`db/migration/postgresql/` or `db/migration/oracle/`).
- **Connection test query** — Oracle uses `SELECT 1 FROM DUAL`; PostgreSQL uses `SELECT 1`.

### Application Configuration Properties (gms.* namespace)

The following `gms.*` properties control application-level behavior. They are set in
`application.yml` and can be overridden via environment variables.

#### gms.security.auth-mode

Selects the authentication strategy. This property drives `@ConditionalOnProperty` annotations
in `SecurityConfig` to activate either the B2C JWT filter chain or the local HTTP Basic filter
chain.

| Attribute | Value |
|---|---|
| Property key | `gms.security.auth-mode` |
| Environment variable | `GMS_AUTH_MODE` |
| Default | `local` (in application.yml); `b2c` in production |
| Valid values | `b2c`, `local` |
| Used by | `SecurityConfig` (conditional bean selection), `SecurityContextProvider` (principal extraction mode) |

- **`b2c`** — Activates `b2cFilterChain`: validates Azure AD B2C JWTs via the configured
  issuer URI. Requires `B2C_ISSUER_URI` and `B2C_API_CLIENT_ID` to be set.
- **`local`** — Activates `localFilterChain`: authenticates via HTTP Basic against the
  `gms_user` table using BCrypt passwords. B2C variables are not required.

#### gms.security.role-claim-key

Specifies the JWT claim key from which user roles are extracted during B2C authentication.

| Attribute | Value |
|---|---|
| Property key | `gms.security.role-claim-key` |
| Environment variable | `GMS_ROLE_CLAIM_KEY` |
| Default | `roles` |
| Expected format | String — the name of a JWT claim containing a JSON array of role strings |
| Used by | `SecurityContextProvider.extractFromJwt()` |

The value must match the claim key emitted by your Azure AD B2C policy. Standard B2C app
roles use `roles`; custom policies may use a different key.

#### gms.security.org-id-claim-key

Specifies the JWT claim key from which the user's organization ID is extracted.

| Attribute | Value |
|---|---|
| Property key | `gms.security.org-id-claim-key` |
| Environment variable | `GMS_ORG_ID_CLAIM_KEY` |
| Default | `extension_organizationId` |
| Expected format | String — the name of a JWT claim containing a numeric organization ID |
| Used by | `SecurityContextProvider.extractFromJwt()` |

B2C custom attributes are prefixed with `extension_` by default. If your tenant uses a
different prefix or attribute name, update this property accordingly.

#### gms.cors.allowed-origins

Configures the list of origins permitted to make cross-origin requests to the API.

| Attribute | Value |
|---|---|
| Property key | `gms.cors.allowed-origins` |
| Environment variable | `GMS_CORS_ORIGINS` |
| Default | `http://localhost:42001` |
| Expected format | Comma-separated list of origin URLs (e.g. `https://app.example.com,https://admin.example.com`) |
| Used by | `SecurityConfig.corsConfigurationSource()` |

The value is split on commas and applied to all paths (`/**`). In production, set this to
the actual frontend domain(s). Multiple origins are supported for multi-tenant or
multi-environment deployments.

---

## 4. Angular — Environment Configuration

### Azure AD B2C — Frontend App Registration

Register a separate **Single Page Application (SPA)** in Azure AD B2C for the Angular client:

| Setting | Value |
|---|---|
| Platform | Single-page application |
| Redirect URIs | `http://localhost:42001` (dev), `https://your-app-domain.com` (prod) |
| Logout URL | `https://your-app-domain.com/logout` |
| Implicit grant | Disabled (use auth code + PKCE) |
| API permissions | Add the backend API scope (e.g. `https://{tenant}.onmicrosoft.com/gms-api/access_as_user`) |

### environment.ts (Development)

```typescript
export const environment = {
  production: false,
  authMode: 'local' as 'b2c' | 'local',    // 'local' for dev, 'b2c' for production
  apiBaseUrl: 'http://localhost:8081',
  b2c: {
    clientId: 'ANGULAR_APP_CLIENT_ID',          // SPA app registration client ID
    authority: 'https://TENANT.b2clogin.com/TENANT.onmicrosoft.com/B2C_1_signin',
    knownAuthorities: ['TENANT.b2clogin.com'],
    redirectUri: 'http://localhost:42001',
    postLogoutRedirectUri: 'http://localhost:42001',
    scopes: [
      'https://TENANT.onmicrosoft.com/gms-api/access_as_user'
    ]
  }
};
```

> **Note:** In `authMode: 'local'`, the `b2c` configuration block is ignored at runtime.
> The placeholder values above are only used when `authMode` is set to `'b2c'` (production).

### environment.prod.ts (Production)

```typescript
export const environment = {
  production: true,
  apiBaseUrl: '${API_BASE_URL}',                // injected at build or runtime
  b2c: {
    clientId: '${B2C_ANGULAR_CLIENT_ID}',
    authority: '${B2C_AUTHORITY}',
    knownAuthorities: ['${B2C_KNOWN_AUTHORITY}'],
    redirectUri: '${APP_BASE_URL}',
    postLogoutRedirectUri: '${APP_BASE_URL}',
    scopes: ['${B2C_SCOPE}']
  }
};
```

Use Angular's `fileReplacements` in `angular.json` to swap `environment.ts` for
`environment.prod.ts` on production builds. For runtime injection (e.g. Docker), serve a
`/assets/config.json` endpoint and load it in `APP_INITIALIZER`.

### MSAL Angular Setup (app.module.ts)

```typescript
import { MsalModule, MsalInterceptor } from '@azure/msal-angular';
import { PublicClientApplication, InteractionType } from '@azure/msal-browser';

@NgModule({
  imports: [
    MsalModule.forRoot(
      new PublicClientApplication({
        auth: {
          clientId: environment.b2c.clientId,
          authority: environment.b2c.authority,
          knownAuthorities: environment.b2c.knownAuthorities,
          redirectUri: environment.b2c.redirectUri,
        },
        cache: { cacheLocation: 'sessionStorage' }
      }),
      { interactionType: InteractionType.Redirect, authRequest: { scopes: environment.b2c.scopes } },
      { interactionType: InteractionType.Redirect, protectedResourceMap: new Map([
          [environment.apiBaseUrl + '/api', environment.b2c.scopes]
        ])
      }
    )
  ],
  providers: [
    { provide: HTTP_INTERCEPTORS, useClass: MsalInterceptor, multi: true }
  ]
})
export class AppModule {}
```

`MsalInterceptor` automatically attaches the Bearer token to all requests matching the
`protectedResourceMap` pattern.

### Angular Environment Variables Summary

| Value | Dev source | Prod source |
|---|---|---|
| `apiBaseUrl` | `environment.ts` | Build-time replacement or runtime config |
| `authMode` | `environment.ts` | Build-time replacement or runtime config |
| `b2c.clientId` | `environment.ts` | CI/CD secret → `environment.prod.ts` |
| `b2c.authority` | `environment.ts` | CI/CD secret → `environment.prod.ts` |
| `b2c.knownAuthorities` | `environment.ts` | CI/CD secret → `environment.prod.ts` |
| `b2c.redirectUri` | `environment.ts` | App base URL |
| `b2c.scopes` | `environment.ts` | Backend API scope URI |

#### authMode Property

The `authMode` property in `environment.ts` controls the frontend authentication strategy,
mirroring the backend's `gms.security.auth-mode` setting.

| Value | Behavior |
|---|---|
| `'local'` | Shows a login screen at `/login`; uses HTTP Basic auth against the backend |
| `'b2c'` | Uses MSAL/Azure AD B2C redirect flow; no login screen (redirects to B2C) |

Both frontend and backend must use the same auth mode. In development, `authMode: 'local'`
is the default.

### Proxy Configuration (Development)

During local development, `proxy.conf.json` proxies API requests from the Angular dev server
to the Spring Boot backend, avoiding CORS issues without requiring the backend CORS
configuration to be active.

```json
{
  "/api": {
    "target": "http://localhost:8081",
    "secure": false,
    "changeOrigin": true
  }
}
```

| Setting | Purpose |
|---|---|
| `target` | Backend URL (must match `server.port` in application.yml) |
| `secure` | Set to `false` for local HTTP (no TLS) |
| `changeOrigin` | Rewrites the `Host` header to match the target |

Activate with: `ng serve --proxy-config proxy.conf.json`

---

## 5. CI/CD Pipeline

### Recommended Pipeline Stages

```
┌─────────────┐   ┌─────────────┐   ┌──────────────┐   ┌─────────────┐
│   Build     │──▶│    Test     │──▶│   Package    │──▶│   Deploy    │
│             │   │             │   │              │   │             │
│ mvn package │   │ mvn verify  │   │ Docker build │   │ az webapp   │
│ ng build    │   │ ng test     │   │ or JAR copy  │   │ or kubectl  │
└─────────────┘   └─────────────┘   └──────────────┘   └─────────────┘
```

### Backend Build

```bash
mvn clean package -DskipTests=false -Dspring.profiles.active=ci
```

The CI profile should use an in-memory H2 database (with Oracle compatibility mode) or a
dedicated CI Oracle schema for integration tests.

> **Oracle migration parity:** If Oracle is a supported deployment target, the CI pipeline
> SHOULD include a Flyway validation step (`flyway:info` or `flyway:validate` with the Oracle
> profile) to confirm that all migrations in `db/migration/oracle/` exist and are valid. This
> catches migration parity drift between the PostgreSQL and Oracle migration sets.

### Frontend Build

```bash
npm ci
ng build --configuration production \
  --base-href / \
  --output-path dist/gms-ui
```

Pass B2C values as build-time environment replacements or use `fileReplacements` in
`angular.json` pointing to a CI-populated `environment.prod.ts`.

### Secrets Management

Never commit secrets to source control. Use one of:
- **Azure Key Vault** — reference secrets in App Service configuration as
  `@Microsoft.KeyVault(SecretUri=...)` references.
- **GitHub Actions / Azure DevOps secrets** — inject as environment variables during the
  pipeline run.
- **Kubernetes Secrets** — mount as environment variables in the pod spec.

### Deployment Targets

#### Azure App Service (recommended for initial deployment)

```bash
# Backend JAR
az webapp deploy \
  --resource-group gms-rg \
  --name gms-api \
  --src-path target/gms-api.jar \
  --type jar

# Frontend static files (Azure Static Web Apps or App Service)
az staticwebapp deploy \
  --app-name gms-ui \
  --source dist/gms-ui
```

Set all environment variables (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `B2C_ISSUER_URI`,
`B2C_API_CLIENT_ID`) as App Service Application Settings — they are injected as environment
variables at runtime and kept out of the codebase.

#### Azure Kubernetes Service (AKS)

Use a `Deployment` manifest with environment variables sourced from a `Secret`:

```yaml
env:
  - name: DB_URL
    valueFrom:
      secretKeyRef:
        name: gms-secrets
        key: db-url
  - name: DB_PASSWORD
    valueFrom:
      secretKeyRef:
        name: gms-secrets
        key: db-password
  - name: B2C_ISSUER_URI
    valueFrom:
      secretKeyRef:
        name: gms-secrets
        key: b2c-issuer-uri
  - name: B2C_API_CLIENT_ID
    valueFrom:
      secretKeyRef:
        name: gms-secrets
        key: b2c-api-client-id
```

#### On-Premises / VM

Set environment variables in the systemd service unit or the shell profile of the service
account running the JAR. Use a `.env` file loaded by the startup script — never committed
to source control.

---

## 6. Database Migration

Use **Flyway** (recommended) or Liquibase to manage schema changes.

### Flyway Setup

```xml
<properties>
    <flyway.version>11.8.0</flyway.version>
</properties>

<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-postgresql</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-oracle</artifactId>
</dependency>
```

```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    schemas: GMS_OWNER
    baseline-on-migrate: true
```

Place migration scripts in `src/main/resources/db/migration/` following the naming convention:

```
V1__create_program_tables.sql
V2__create_question_tables.sql
V3__create_round_page_tables.sql
V4__create_application_tables.sql
V5__create_organization_table.sql
```

Flyway runs automatically on application startup and applies any pending migrations in order.

> **Oracle V4 migration gap:** The `V4__move_min_max_to_rpq.sql` migration currently exists
> only for PostgreSQL. If using the Oracle profile against a schema that predates V4, create
> the equivalent Oracle DDL at `db/migration/oracle/V4__move_min_max_to_rpq.sql`:
> `ALTER TABLE gms_round_page_question ADD (min_value VARCHAR2(100)); ALTER TABLE gms_round_page_question ADD (max_value VARCHAR2(100)); ALTER TABLE gms_question DROP COLUMN min_date; ALTER TABLE gms_question DROP COLUMN max_date;`

---

## 7. Local Development Setup

### Prerequisites

| Tool | Minimum version |
|---|---|
| Java | 17 |
| Maven | 3.9 |
| Node.js | 20 LTS |
| Angular CLI | 21 |
| Oracle DB | 19c (or Oracle Free 23ai for local) |

### Backend — Local Run

Create a `.env` file (gitignored) in the project root:

```bash
DB_URL=jdbc:oracle:thin:@//localhost:1521/XEPDB1
DB_USERNAME=gms_owner
DB_PASSWORD=localpassword
GMS_AUTH_MODE=local
GMS_CORS_ORIGINS=http://localhost:42001
AZURE_STORAGE_CONNECTION_STRING=DefaultEndpointsProtocol=https;AccountName=devaccount;AccountKey=...
```

When `GMS_AUTH_MODE=local`, the B2C variables (`B2C_ISSUER_URI`, `B2C_API_CLIENT_ID`) are not
required. The system authenticates via HTTP Basic against the `gms_user` table.

For B2C mode (connecting to a real Azure AD B2C tenant), add:
```bash
GMS_AUTH_MODE=b2c
B2C_ISSUER_URI=https://TENANT.b2clogin.com/TENANT.onmicrosoft.com/B2C_1_signin/v2.0/
B2C_API_CLIENT_ID=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
```

Load with a tool like `dotenv-java` or export manually, then:

```bash
mvn spring-boot:run
```

**Default local users** (seeded by Flyway migration V3):
- `admin` / `password123` — ADMIN role
- `applicant` / `password123` — APPLICANT role

### Frontend — Local Run

Ensure `environment.ts` has `authMode: 'local'` (this is the default for development).
The app will show a login screen at `http://localhost:42001/login`.

```bash
cd gms-ui
npm install
ng serve --proxy-config proxy.conf.json
```

`proxy.conf.json` proxies `/api` to the local backend to avoid CORS issues during development:

```json
{
  "/api": {
    "target": "http://localhost:8081",
    "secure": false,
    "changeOrigin": true
  }
}
```

---
