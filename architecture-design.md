# Grant Management Base Framework — Architecture & Technical Design

## Technology Stack

| Layer | Technology |
|---|---|
| Backend | Java Spring Boot 3.3 |
| ORM / Persistence | JPA / Hibernate |
| Database | PostgreSQL (default) or Oracle (via Spring profile) |
| Frontend | Angular 21 |
| Security | Spring Security + Azure AD B2C (production) or HTTP Basic (local) |
| File Storage | Azure Blob Storage |

### Database Support

The application is database-agnostic. The active database is selected via Spring profile:

| Profile | Database | Flyway Migrations | Driver |
|---|---|---|---|
| `postgresql` (default) | PostgreSQL 14–18 | `db/migration/postgresql/` | `org.postgresql.Driver` |
| `oracle` | Oracle 19c+ | `db/migration/oracle/` | `oracle.jdbc.OracleDriver` |

Set via `SPRING_PROFILES_ACTIVE=postgresql` (default) or `SPRING_PROFILES_ACTIVE=oracle`.

**Important:** Flyway 11.8+ is required for PostgreSQL 18 support. The `flyway.version` property
in `pom.xml` overrides the Spring Boot BOM's default Flyway version to ensure compatibility.

The `DynamicDataRepository` uses standard SQL (SELECT + INSERT/UPDATE) rather than
database-specific MERGE or ON CONFLICT syntax, ensuring compatibility with both databases
without code changes.

---

## Domain Model Hierarchy

```
Program
└── ProgramRound (1..*)
    └── RoundPage (1..*) ← join of ProgramRound + Page
        └── RoundPageQuestion (1..*) ← join of RoundPage + Question; all config in one record
```

- **Program** — top-level initiative; no direct page or question associations.
- **ProgramRound** — operational funding cycle; owns the page assignments and status lifecycle.
- **Page** — reusable template; shared across rounds but each round gets its own independent
  RoundPageQuestion list.
- **Question** — reusable field definition; shared across pages and rounds.
- **RoundPage** — scopes a Page to a ProgramRound; stores display order and page name/description
  overrides.
- **RoundPageQuestion** — the single join entity between a RoundPage and a Question. Stores all
  question-to-page configuration for a given (round, page) pair: display order, page-level label
  and required overrides, round-level label and required overrides, the exclusion flag, role
  visibility, and the QuestionDisplayConfig fields (readonly, columnSpan, labelPosition,
  helpText, sectionGroup). There is no separate PageQuestion entity.

### JSON Serialization — Circular Reference Prevention

JPA entities use bidirectional relationships. To prevent infinite recursion during JSON
serialization, `@JsonIgnore` annotations are applied to child → parent back-references
(ProgramRound.program, RoundPage.programRound, RoundPageQuestion.roundPage,
Question.parentQuestion) and to collection fields that would trigger deep nesting
(Program.rounds, ProgramRound.roundPages, RoundPage.roundPageQuestions,
Question.childQuestions). These collections are accessed via dedicated endpoints rather than
being embedded in the parent response.

### JPA Fetch Strategy

| Entity | Field | Fetch Type | Reason |
|---|---|---|---|
| RoundPage | `page` | `EAGER` | Always needed when listing round-pages (page name displayed) |
| All other `@ManyToOne` | — | `LAZY` | Default; loaded on demand |

API responses for child entities do NOT include the full parent object. The parent ID is
available via the URL path parameters.

---

## Service Layer

### ProgramService
Manages top-level Program entities: create, update, archive, list, and get-with-rounds.

### ProgramRoundService
Manages ProgramRound lifecycle within a Program: create, update, status transitions, page
assignment, page removal, round-level question override CRUD, and round-scoped page override
persistence. Round archiving directly sets the round status to ARCHIVED without going through
the lifecycle transition guard — this allows archiving from any non-ARCHIVED state.

### PageService
Manages Page template definitions and RoundPageQuestion assignments within a (round, page)
context: create page, update page, get single page, deactivate page, assign question, update
question config (including `visibleToRoles`), remove question, list pages. The `deactivatePage()`
method uses a targeted repository query (`findByPageId(Long)`) to locate active round
assignments — not a full-table scan.

### QuestionService
Manages Question metadata: create, update, deactivate, list with filter, option management,
parent-child link management.

### PageBuilderService
Runtime assembly engine. Given a (programId, roundId, pageId, userRoles), it:
1. Loads the RoundPage and all associated RoundPageQuestions.
2. Applies Override Priority to resolve each question's effective label and required flag.
3. Applies role-visibility filtering.
4. Applies exclusion filtering.
5. Populates QuestionDisplayConfig onto each QuestionRenderDTO.
6. Nests child questions under their parent QuestionRenderDTO.
7. Returns a PageRenderDTO.

The admin preview endpoint passes an empty roles list so all questions are visible regardless
of role restrictions.

### ApplicationDataService
Handles answer validation and transactional persistence. Resolves `roundPageId` from the
`(applicationId, pageId)` pair before loading RoundPageQuestion constraints. Delegates dynamic
SQL to DynamicDataRepository. Performs post-save re-read verification.

### AllowlistService
Loads the application data table allowlist from `application.yml` at startup, normalises all
table and column names to uppercase, and exposes `isPermitted(table, column)` used by both
`QuestionService` (at question creation) and `DynamicDataRepository` (at answer save).

### FileStorageService
Handles file upload to Azure Blob Storage, SAS URL generation for downloads, blob deletion,
and virus scan status management. Validates file extension and size against question metadata
before storing.

### OrganizationService
Manages Organization entities: create, update, deactivate, list.

### ApplicationService
Manages Application lifecycle: create application (with round-active and org-eligibility
validation), prevent duplicates, status transitions (DRAFT → SUBMITTED, DRAFT/SUBMITTED →
WITHDRAWN), retrieval by applicationId, and retrieval of a user's application for a given
round. Enforces that `appId` belongs to the authenticated user on all answer operations.

### UserProfileService
Provides the `/api/me` endpoint that returns the authenticated user's `userId` and `roles`
list.

### BulkRoundSetupService
Creates a fully configured program round in a single transaction: creates the round (with
today's start date, 3-month end date, $1000 fund limit), creates new pages, creates new
questions (with default target `gms_application_data.value_text`), and wires up all the
RoundPage and RoundPageQuestion mappings. Handles option creation for select-type questions
and auto-injects Yes/No options for RADIO_YES_NO questions. Before creating a new question,
checks for an existing active question with the same label and type — if found, reuses it
rather than creating a duplicate. Similarly, pages with the same name are reused if they
already exist.

---

## Override Resolution Rules

### Question Label and Required Flag

Resolution order (highest to lowest):
1. RoundPageQuestion.roundLabelOverride (round-level label override)
2. RoundPageQuestion.labelOverride / requiredOverride (page-level overrides)
3. Question.label / Question.required (question default)

A null or absent override at any level is skipped; the next lower level is used.

### Question Exclusion

A question is excluded from a rendered page if RoundPageQuestion.excluded = true.

### Page Name and Description

Resolution order (highest to lowest):
1. RoundPage.pageNameOverride / pageDescriptionOverride (if non-blank)
2. Page.pageName / Page.pageDescription (canonical)

Setting an override to blank or null clears it and reverts to the canonical value.

---

## Question Types

| Type | Rendered As | Options | Requires targetTable/Column |
|---|---|---|---|
| TEXT | Single-line text input | No | Yes |
| TEXT_AREA | Multi-line textarea | No | Yes |
| DATE | Date input | No | Yes |
| DECIMAL | Numeric input | No | Yes |
| WHOLE_NUMBER | Numeric input | No | Yes |
| CURRENCY | Text input | No | Yes |
| PHONE | Text input | No | Yes |
| ZIP_CODE | Text input | No | Yes |
| ATTACHMENT | File upload control | No | Yes |
| SELECT_ONE | Radio buttons | Yes (1–200) | Yes |
| SELECT_MULTI | Checkboxes | Yes (1–200) | Yes |
| CHECKBOX | Single checkbox | No | Yes |
| LABEL | Read-only text | No | No |
| RADIO_YES_NO | Radio buttons (Yes/No) | System-managed | Yes |

**RADIO_YES_NO specifics:**
- Exactly two fixed options: Yes (displayOrder=1), No (displayOrder=2).
- Options are auto-injected on create/update; caller-supplied options are rejected.
- This is the ONLY type that may be a parent in a parent-child relationship.
- RADIO_YES_NO questions MAY be child questions, enabling multi-level branching.
- triggerValue on child links must be "Yes" or "No".

---

## Answer Validation Rules

| Question Type | Validation Rule |
|---|---|
| Any required question | Null, empty, or whitespace-only value → field error |
| TEXT | Optional `validationRegex` match |
| TEXT_AREA | Optional `validationRegex` match with DOTALL mode |
| DATE | ISO-8601 format (yyyy-MM-dd); if min_value/max_value are set on the RoundPageQuestion record, the date must fall within [min_value, max_value] inclusive |
| DECIMAL | Parseable as BigDecimal |
| WHOLE_NUMBER | Parseable as Long |
| CURRENCY | Matches `^\d{1,12}(\.\d{1,2})?$` |
| PHONE | Matches `^\+?[0-9\-\(\)\s]{7,15}$` |
| ZIP_CODE | Matches `^\d{5}(-\d{4})?$` |
| ATTACHMENT | File extension in allowedFileTypes; size ≤ maxFileSizeMb |
| SELECT_ONE | Value must be one of the defined option values |
| SELECT_MULTI | Each selected value must be a defined option value |
| SELECT_MULTI + "Other" | otherText must be non-empty when "Other" is selected |
| CHECKBOX | Value must be "true" or "false" (case-insensitive) |
| RADIO_YES_NO | Value must be "Yes" or "No" (case-insensitive) |
| LABEL | Any value accepted and ignored |
| Unknown/inactive questionId | Field-level error |

Date min/max constraints are configured per (round, page, question) on the
`gms_round_page_question` record — not on the question definition itself. This allows
different rounds to impose different date ranges on the same question.

---

## Transactional Save Flow

1. Resolve `roundPageId` from `(applicationId, pageId)` to load RoundPageQuestion constraints.
2. Validate all answers (field-level errors collected, not thrown).
3. If any validation error exists → rollback, return failure with errors.
4. For each answer, resolve the target table/column (applying per-option routing if applicable).
5. For each target table row: INSERT if no row exists for `applicationId`, UPDATE otherwise.
6. Re-read each saved row and verify stored values equal submitted values.
7. If any mismatch or missing row → rollback, return failure.
8. If database error at any point → rollback, log error, return failure.
9. On full success → commit, return success.

All steps execute within a single `@Transactional` boundary.

---

## Per-Option Target Routing

QuestionOption may carry `targetTable`, `targetColumn`, and `lookupId`. When all three are set,
the system writes `lookupId` into the option's target location instead of writing the option
value into the question-level target.

Rules:
- All three fields must be provided together; partial configuration is rejected.
- Each field is at most 100 characters.
- If a selected option has incomplete routing, the system falls back to the question-level
  target routing for that option.

---

## Dynamic SQL Safety Model

JDBC parameterization covers values only — table and column names must be interpolated directly
into SQL strings. The framework addresses this with a two-layer defence:

**Layer 1 — Allowlist Validation at Question Creation:** When an administrator creates or
updates a Question with a `targetTable` and `targetColumn`, both values are validated against
the allowlist before persisting. Invalid values are rejected immediately.

**Layer 2 — Runtime Allowlist Check:** At answer-save time, `DynamicDataRepository` re-validates
the resolved target pair against the same allowlist before constructing any SQL. If the pair is
not in the allowlist, the repository throws a security exception and rolls back.

The allowlist is loaded at startup from `application.yml` and cached. Each entry specifies a
table name and its permitted column names.

---

## Application Data Tables

Application data tables are **pre-existing tables** created as part of the schema setup.
The framework never creates tables at runtime.

### Minimum Column Contract

Every application data table registered in the allowlist MUST contain:

| Column | Type | Purpose |
|---|---|---|
| `application_id` | NUMBER (FK → gms_application) | Lookup key for INSERT vs UPDATE |
| *(answer columns)* | VARCHAR2 | One column per targetColumn value; all answer types stored as strings |
| `created_at` | TIMESTAMP | Audit |
| `updated_at` | TIMESTAMP | Audit |

### Deployment Patterns

| Pattern | When to use | Characteristics |
|---|---|---|
| **Single-table (EAV)** | Simple programs, rapid prototyping | All answers in `gms_application_data`; one row per (application, question) |
| **Multi-table (domain)** | Production at scale, reporting needs | Answers in domain tables; one row per application per table |

Both patterns are handled transparently by `DynamicDataRepository`.

### Multi-Select Answer Storage

SELECT_MULTI answers are stored as a JSON array string. The `OTHER:` prefix convention
distinguishes free-text "Other" entries from standard option values. All other question types
store a single plain string value.

---

## REST API Reference

All admin endpoints are under `/api/admin/**` and require the ADMIN role.
Applicant-facing endpoints are under `/api/programs/**` and require APPLICANT role or higher.

### Authentication Endpoints

| Method | Path | Description |
|---|---|---|
| GET | `/api/me` | Returns the authenticated user's identity and roles |

### Question Endpoints

| Method | Path | Description |
|---|---|---|
| POST | `/api/admin/questions` | Create question |
| PUT | `/api/admin/questions/{id}` | Update question |
| DELETE | `/api/admin/questions/{id}` | Deactivate question (soft-delete) |
| GET | `/api/admin/questions` | List questions (paginated, filterable) |
| GET | `/api/admin/questions/{id}` | Get question with options and child links |
| POST | `/api/admin/questions/{id}/options` | Add option |
| PUT | `/api/admin/questions/{id}/options/{optionId}` | Update option |
| DELETE | `/api/admin/questions/{id}/options/{optionId}` | Remove option |
| PUT | `/api/admin/questions/{id}/options/reorder` | Reorder all options |
| POST | `/api/admin/questions/{parentId}/children` | Link child question |
| PUT | `/api/admin/questions/{parentId}/children/{childId}` | Update child link |
| DELETE | `/api/admin/questions/{parentId}/children/{childId}` | Unlink child question |
| GET | `/api/admin/questions/{parentId}/children` | List child questions |

### Page Endpoints

| Method | Path | Description |
|---|---|---|
| POST | `/api/admin/pages` | Create page |
| PUT | `/api/admin/pages/{id}` | Update page |
| DELETE | `/api/admin/pages/{id}` | Deactivate page; 409 if assigned to active rounds |
| GET | `/api/admin/pages` | List pages (paginated, filterable) |
| GET | `/api/admin/pages/{id}` | Get single page |

### Program Endpoints

| Method | Path | Description |
|---|---|---|
| POST | `/api/admin/programs` | Create program |
| PUT | `/api/admin/programs/{id}` | Update program |
| DELETE | `/api/admin/programs/{id}` | Archive program |
| GET | `/api/admin/programs` | List all programs |
| GET | `/api/admin/programs/{id}` | Get program with rounds |

### Program Round Endpoints

| Method | Path | Description |
|---|---|---|
| POST | `/api/admin/programs/{programId}/rounds` | Create round |
| PUT | `/api/admin/programs/{programId}/rounds/{roundId}` | Update round |
| DELETE | `/api/admin/programs/{programId}/rounds/{roundId}` | Archive round |
| GET | `/api/admin/programs/{programId}/rounds` | List rounds |
| GET | `/api/admin/programs/{programId}/rounds/{roundId}` | Get single round with full detail |
| PUT | `/api/admin/programs/{programId}/rounds/{roundId}/status` | Transition round status |

### Round-Page Assignment Endpoints

| Method | Path | Description |
|---|---|---|
| POST | `/api/admin/programs/{programId}/rounds/{roundId}/pages` | Assign page to round |
| GET | `/api/admin/programs/{programId}/rounds/{roundId}/pages` | List pages in round |
| DELETE | `/api/admin/programs/{programId}/rounds/{roundId}/pages/{pageId}` | Remove page from round |
| PUT | `/api/admin/programs/{programId}/rounds/{roundId}/pages/{pageId}` | Update round-scoped page overrides |

### Round-Page-Question Endpoints

| Method | Path | Description |
|---|---|---|
| POST | `.../rounds/{roundId}/pages/{pageId}/questions` | Assign question to page within round |
| GET | `.../rounds/{roundId}/pages/{pageId}/questions` | List all question configs for a round-page |
| GET | `.../rounds/{roundId}/pages/{pageId}/questions/{questionId}` | Get full question config |
| PUT | `.../rounds/{roundId}/pages/{pageId}/questions/{questionId}` | Update question config (includes visibilityRules and formula) |
| DELETE | `.../rounds/{roundId}/pages/{pageId}/questions/{questionId}` | Remove question from page |
| GET | `.../rounds/{roundId}/pages/{pageId}/preview` | Admin preview of rendered page |
| GET | `.../rounds/{roundId}/pages/{pageId}/rules` | Get page rules for a round-page |
| PUT | `.../rounds/{roundId}/pages/{pageId}/rules` | Replace all page rules for a round-page |

### Applicant-Facing Endpoints

| Method | Path | Description |
|---|---|---|
| GET | `/api/programs` | List active programs with their active rounds |
| POST | `/api/programs/{programId}/rounds/{roundId}/applications` | Create application |
| GET | `/api/programs/{programId}/rounds/{roundId}/applications/mine` | Get user's application |
| GET | `/api/programs/{programId}/rounds/{roundId}/pages` | List pages visible to user |
| GET | `/api/programs/{programId}/rounds/{roundId}/pages/{pageId}` | Get rendered page |
| PUT | `/api/applications/{appId}/status` | Update application status |
| POST | `/api/applications/{appId}/pages/{pageId}/answers` | Submit answers |
| GET | `/api/applications/{appId}/pages/{pageId}/answers` | Retrieve saved answers |

### Admin Application Management Endpoints

| Method | Path | Description |
|---|---|---|
| GET | `/api/admin/programs/{programId}/rounds/{roundId}/applications` | List applications (paginated, filterable) |
| PUT | `/api/admin/applications/{appId}/reopen` | Reopen a submitted application |

### Bulk Setup Endpoints

| Method | Path | Description |
|---|---|---|
| POST | `/api/admin/programs/bulk-round-setup` | Create a round with pages and questions in one call |
| POST | `/api/admin/programs/import-form` | Import external form JSON (sections/fields) into a round with visibility rules and page rules |

### Internal Endpoints

| Method | Path | Description |
|---|---|---|
| POST | `/api/internal/scan-results` | Receive virus scan result webhook (fileId + scanStatus) |

### Organization Endpoints

| Method | Path | Description |
|---|---|---|
| POST | `/api/admin/organizations` | Create organization |
| PUT | `/api/admin/organizations/{id}` | Update organization |
| DELETE | `/api/admin/organizations/{id}` | Deactivate organization |
| GET | `/api/admin/organizations` | List organizations |

### File Attachment Endpoints

| Method | Path | Description |
|---|---|---|
| POST | `/api/applications/{appId}/files` | Upload file (multipart/form-data) |
| GET | `/api/applications/{appId}/files/{fileId}` | Download file (302 redirect to SAS URL) |
| DELETE | `/api/applications/{appId}/files/{fileId}` | Remove file |

### Error Responses

- `404` — resource not found
- `400` — request body validation failure, with field-level error details
- `401` — missing or invalid/expired token
- `403` — authenticated user lacks required role
- `409` — action blocked by active dependencies; response includes affected resources

---

## Database Schema

### ID Generation Strategy

All tables use `GENERATED ALWAYS AS IDENTITY` with JPA `GenerationType.IDENTITY`. The sole
exception is `gms_file_attachment` which uses a VARCHAR2(36) UUID PK generated by the
application layer.

### Pagination Convention

All paginated list endpoints use: `page` (zero-based, default 0), `size` (default 20, max 100),
`sort` (field,direction). Response envelope: `{ content, totalElements, totalPages, page, size }`

### `gms_program`

| Column | Type | Constraints |
|---|---|---|
| id | NUMBER | PK |
| program_name | VARCHAR2(255) | NOT NULL |
| description | VARCHAR2(2000) | |
| goal | VARCHAR2(2000) | |
| total_budget | NUMBER(18,2) | |
| status | VARCHAR2(20) | NOT NULL; values: ACTIVE, ARCHIVED |
| created_at | TIMESTAMP | NOT NULL |
| updated_at | TIMESTAMP | NOT NULL |

### `gms_program_round`

| Column | Type | Constraints |
|---|---|---|
| id | NUMBER | PK |
| program_id | NUMBER | FK → gms_program, NOT NULL |
| round_name | VARCHAR2(255) | NOT NULL |
| start_date | DATE | NOT NULL |
| end_date | DATE | NOT NULL |
| funds_limit | NUMBER(18,2) | |
| status | VARCHAR2(20) | NOT NULL; values: DRAFT, ACTIVE, CLOSED, ARCHIVED |
| eligible_organization_types | VARCHAR2(500) | Comma-separated list; empty means all eligible |
| created_at | TIMESTAMP | NOT NULL |
| updated_at | TIMESTAMP | NOT NULL |

### `gms_page`

| Column | Type | Constraints |
|---|---|---|
| id | NUMBER | PK |
| page_name | VARCHAR2(200) | NOT NULL |
| page_description | VARCHAR2(1000) | |
| active | NUMBER(1) | NOT NULL |
| created_at | TIMESTAMP | NOT NULL |
| updated_at | TIMESTAMP | NOT NULL |

### `gms_round_page`

| Column | Type | Constraints |
|---|---|---|
| id | NUMBER | PK |
| program_round_id | NUMBER | FK → gms_program_round, NOT NULL |
| page_id | NUMBER | FK → gms_page, NOT NULL |
| display_order | NUMBER | NOT NULL |
| page_name_override | VARCHAR2(200) | |
| page_description_override | VARCHAR2(1000) | |

### `gms_question`

| Column | Type | Constraints |
|---|---|---|
| id | NUMBER | PK |
| question_type | VARCHAR2(50) | NOT NULL |
| label | VARCHAR2(255) | NOT NULL |
| target_table | VARCHAR2(100) | Required unless type = LABEL |
| target_column | VARCHAR2(100) | Required unless type = LABEL |
| required | NUMBER(1) | |
| validation_regex | VARCHAR2(500) | TEXT and TEXT_AREA only |
| allowed_file_types | VARCHAR2(500) | ATTACHMENT only |
| max_file_size_mb | NUMBER | ATTACHMENT only; 1–100 |
| parent_question_id | NUMBER | FK → gms_question (self); child questions only |
| trigger_value | VARCHAR2(100) | Child questions only |
| child_display_order | NUMBER | Child questions only; 1–999 |
| active | NUMBER(1) | NOT NULL |
| external_id | VARCHAR(100) | External UUID from imported form definitions |
| created_at | TIMESTAMP | NOT NULL |
| updated_at | TIMESTAMP | NOT NULL |

> **Note:** Date range constraints are stored on `gms_round_page_question` as `min_value` and
> `max_value`, not on `gms_question`.

### `gms_question_option`

| Column | Type | Constraints |
|---|---|---|
| id | NUMBER | PK |
| question_id | NUMBER | FK → gms_question, NOT NULL |
| option_label | VARCHAR2(255) | NOT NULL |
| option_value | VARCHAR2(100) | NOT NULL; unique per question |
| display_order | NUMBER | NOT NULL; >= 1 |
| is_other_option | NUMBER(1) | SELECT_MULTI only |
| option_target_table | VARCHAR2(100) | Per-option routing; all-or-none with option_target_column and lookup_id |
| option_target_column | VARCHAR2(100) | |
| lookup_id | VARCHAR2(100) | |

### `gms_round_page_question`

| Column | Type | Constraints |
|---|---|---|
| id | NUMBER | PK |
| round_page_id | NUMBER | FK → gms_round_page, NOT NULL |
| question_id | NUMBER | FK → gms_question, NOT NULL |
| display_order | NUMBER | NOT NULL; 1–999 |
| label_override | VARCHAR2(255) | Page-level label override |
| required_override | NUMBER(1) | Page-level required override |
| round_label_override | VARCHAR2(255) | Round-level label override |
| excluded | NUMBER(1) | NOT NULL; default 0 |
| readonly | NUMBER(1) | NOT NULL; default 0 |
| column_span | NUMBER | NOT NULL; default 12; values 1–12 |
| label_position | VARCHAR2(10) | NOT NULL; default ABOVE; values: ABOVE, LEFT, HIDDEN |
| help_text | VARCHAR2(500) | Optional guidance text |
| section_group | VARCHAR2(100) | Optional grouping label |
| min_value | VARCHAR2(100) | Polymorphic min constraint |
| max_value | VARCHAR2(100) | Polymorphic max constraint |
| formula | VARCHAR(1000) | Calculated field expression (e.g., q_5 + q_6); null if not calculated |
| UNIQUE | | (round_page_id, question_id) |

**`min_value` / `max_value` interpretation by question type:**

| Question Type | min_value meaning | max_value meaning |
|---|---|---|
| DATE | Minimum date (yyyy-MM-dd) | Maximum date (yyyy-MM-dd) |
| DECIMAL, CURRENCY | Minimum numeric value | Maximum numeric value |
| WHOLE_NUMBER | Minimum integer | Maximum integer |
| TEXT, TEXT_AREA | Minimum character length | Maximum character length |
| PHONE, ZIP_CODE | Minimum character length | Maximum character length |
| All other types | Not applicable | Not applicable |

### `gms_round_page_question_role`

| Column | Type | Constraints |
|---|---|---|
| id | NUMBER | PK |
| round_page_question_id | NUMBER | FK → gms_round_page_question, NOT NULL |
| role_name | VARCHAR2(100) | NOT NULL |

### `gms_visibility_rule`

| Column | Type | Constraints |
|---|---|---|
| id | BIGINT | PK |
| round_page_question_id | BIGINT | FK → gms_round_page_question, NOT NULL, ON DELETE CASCADE |
| trigger_question_id | BIGINT | FK → gms_question, NOT NULL |
| operator | VARCHAR(30) | NOT NULL; values: IS_NOT_EMPTY, IS_EMPTY, EQUALS, NOT_EQUALS, LESS_THAN, GREATER_THAN, CONTAINS |
| value | VARCHAR(255) | Comparison value (null for IS_NOT_EMPTY/IS_EMPTY) |
| logic | VARCHAR(5) | AND or OR (default AND) |
| created_at | TIMESTAMP | NOT NULL |

### `gms_page_rule`

| Column | Type | Constraints |
|---|---|---|
| id | BIGINT | PK |
| round_page_id | BIGINT | FK → gms_round_page, NOT NULL, ON DELETE CASCADE |
| logic | VARCHAR(5) | AND or OR (default AND) |
| error_message | VARCHAR(500) | Message shown when rule conditions fail |
| created_at | TIMESTAMP | NOT NULL |

### `gms_page_rule_condition`

| Column | Type | Constraints |
|---|---|---|
| id | BIGINT | PK |
| page_rule_id | BIGINT | FK → gms_page_rule, NOT NULL, ON DELETE CASCADE |
| question_id | BIGINT | FK → gms_question, NOT NULL |
| operator | VARCHAR(30) | NOT NULL |
| value | VARCHAR(255) | |
| value2 | VARCHAR(255) | |

### `gms_application`

| Column | Type | Constraints |
|---|---|---|
| id | NUMBER | PK |
| program_round_id | NUMBER | FK → gms_program_round, NOT NULL |
| organization_id | NUMBER | FK → gms_organization, NOT NULL |
| user_id | VARCHAR2(255) | NOT NULL; Azure AD B2C subject claim |
| status | VARCHAR2(20) | NOT NULL; values: DRAFT, SUBMITTED, WITHDRAWN |
| eligibility_warning | NUMBER(1) | NOT NULL; default 0 |
| created_at | TIMESTAMP | NOT NULL |
| updated_at | TIMESTAMP | NOT NULL |
| UNIQUE | | (program_round_id, user_id) |

### `gms_status_history`

| Column | Type | Constraints |
|---|---|---|
| id | NUMBER | PK |
| entity_type | VARCHAR2(50) | NOT NULL; values: PROGRAM, PROGRAM_ROUND, APPLICATION |
| entity_id | NUMBER | NOT NULL |
| from_status | VARCHAR2(20) | Previous status (null for initial creation) |
| to_status | VARCHAR2(20) | NOT NULL |
| changed_by | VARCHAR2(255) | NOT NULL |
| changed_at | TIMESTAMP | NOT NULL |
| reason | VARCHAR2(500) | Optional |

### `gms_application_data` (Reference Answer Table)

| Column | Type | Constraints |
|---|---|---|
| id | NUMBER | PK |
| application_id | NUMBER | FK → gms_application, NOT NULL |
| question_id | NUMBER | FK → gms_question, NOT NULL |
| value_text | VARCHAR2(4000) | All answer values stored as strings |
| created_at | TIMESTAMP | NOT NULL |
| updated_at | TIMESTAMP | NOT NULL |
| UNIQUE | | (application_id, question_id) |

### `gms_organization`

| Column | Type | Constraints |
|---|---|---|
| id | NUMBER | PK |
| name | VARCHAR2(255) | NOT NULL; unique |
| organization_type | VARCHAR2(20) | NOT NULL; values: NONPROFIT, GOVERNMENT, BUSINESS, EDUCATIONAL, OTHER |
| description | VARCHAR2(2000) | |
| active | NUMBER(1) | NOT NULL; default 1 |
| created_at | TIMESTAMP | NOT NULL |
| updated_at | TIMESTAMP | NOT NULL |

### `gms_file_attachment`

| Column | Type | Constraints |
|---|---|---|
| id | VARCHAR2(36) | PK; UUID |
| application_id | NUMBER | FK → gms_application, NOT NULL |
| question_id | NUMBER | FK → gms_question, NOT NULL |
| original_filename | VARCHAR2(500) | NOT NULL |
| file_extension | VARCHAR2(20) | NOT NULL |
| file_size_bytes | NUMBER | NOT NULL |
| blob_path | VARCHAR2(1000) | NOT NULL |
| scan_status | VARCHAR2(20) | NOT NULL; PENDING, CLEAN, INFECTED |
| uploaded_at | TIMESTAMP | NOT NULL |
| scanned_at | TIMESTAMP | |

### `gms_user` (Local Mode Only)

| Column | Type | Constraints |
|---|---|---|
| id | NUMBER | PK |
| username | VARCHAR2(100) | NOT NULL, UNIQUE |
| password_hash | VARCHAR2(255) | NOT NULL; BCrypt |
| display_name | VARCHAR2(255) | |
| roles | VARCHAR2(200) | NOT NULL; comma-separated |
| organization_id | NUMBER | FK → gms_organization; nullable |
| active | NUMBER(1) | NOT NULL; default 1 |
| created_at | TIMESTAMP | NOT NULL |
| updated_at | TIMESTAMP | NOT NULL |

---

## Security Architecture

### Authentication Modes

| Mode | Value | Mechanism | Use case |
|---|---|---|---|
| **Azure AD B2C** | `b2c` | JWT Bearer token validation | Production |
| **Local** | `local` | HTTP Basic auth against `gms_user` table | Development / testing |

Only one SecurityFilterChain bean is active at a time, selected via `@ConditionalOnProperty`.

### Authorization

| Path Pattern | Required Role |
|---|---|
| `/api/admin/**` | ROLE_ADMIN |
| `/maintenance/**` | ROLE_ADMIN |
| `/admin-dashboard.html` | ROLE_ADMIN |
| `/api/programs/**` | ROLE_APPLICANT or ROLE_ADMIN |
| `/api/applications/**` | ROLE_APPLICANT or ROLE_ADMIN |
| `/api/me` | Any authenticated user |
| `/api/internal/**` | No authentication (webhook) |
| `/favicon.ico`, `/static/**`, public HTML | No authentication required |
| `/swagger-ui/**`, `/v3/api-docs/**` | No authentication required |

### SecurityContextProvider

Unified component that works in both modes:
- **B2C mode**: extracts userId (JWT subject), roles (JWT claim), organizationId (JWT
  extension_organizationId claim).
- **Local mode**: looks up the authenticated username in `gms_user` table, extracts userId,
  roles, organizationId.

Returns a `UserPrincipal(userId, roles, organizationId)` record used by all services.

---

## DTO Reference

| DTO | Description |
|---|---|
| `ProgramDTO` | Program entity response; includes list of ProgramRoundDTOs |
| `ProgramRoundDTO` | Round entity response; includes eligibleOrganizationTypes list |
| `PageDTO` | Page template response |
| `RoundPageDTO` | Round-scoped page response: `id` (Long), `pageId` (Long), `pageName` (canonical), `pageDescription` (canonical), `pageNameOverride` (nullable), `pageDescriptionOverride` (nullable), `resolvedPageName`, `resolvedPageDescription`, `displayOrder` (Integer) |
| `QuestionDTO` | Question metadata response; includes options and child links |
| `QuestionOptionDTO` | Single question option; includes per-option routing fields |
| `ChildLinkDTO` | Parent-child link descriptor |
| `PageRenderDTO` | Fully resolved page for rendering |
| `QuestionRenderDTO` | Single question with all overrides applied; includes displayConfig |
| `ChildQuestionRenderDTO` | Wrapper for a child question with triggerValue and displayConfig |
| `QuestionDisplayConfig` | Layout attributes: readonly, columnSpan, labelPosition, helpText, sectionGroup |
| `VisibilityRuleDTO` | Conditional display rule: triggerQuestionId, operator (IS_NOT_EMPTY, IS_EMPTY, EQUALS, NOT_EQUALS, LESS_THAN, GREATER_THAN, CONTAINS), value, logic (AND/OR) |
| `PageRuleDTO` | Page-level cross-field validation: logic (AND/OR), errorMessage, conditions[] |
| `PageRuleConditionDTO` | Single condition in a page rule: questionId, operator, value, value2 |
| `PageSummaryDTO` | Lightweight page descriptor for page lists |
| `ApplicationDTO` | Application entity response: `id` (Long), `programRoundId` (Long), `organizationId` (Long), `userId` (String), `status` (DRAFT/SUBMITTED/WITHDRAWN), `eligibilityWarning` (boolean), `createdAt` (ISO-8601), `updatedAt` (ISO-8601) |
| `SaveResult` | Answer save response; contains `success` (boolean) and `errors` (array of `{questionId, message}` objects) |
| `AnswerDTO` | Single submitted answer |
| `FileReferenceDTO` | Returned after file upload; contains fileId, filename, size, scanStatus |
| `UserPrincipal` | Authenticated user identity |
| `UserProfileDTO` | Response from GET /api/me |
| `BulkRoundSetupRequest` | Request DTO for bulk round creation; contains programId, roundName, and nested pages with questions and options |

---

## File Attachment Architecture

### Storage Backend

Files are stored in Azure Blob Storage in a container named `gms-attachments`. The application
generates short-lived SAS URLs for downloads rather than serving file content directly.

### Upload Flow

1. Applicant uploads file via multipart POST
2. FileStorageService validates extension + size
3. Blob stored at `{appId}/{questionId}/{fileId}.{ext}`
4. `gms_file_attachment` record created with scanStatus=PENDING
5. Async virus scan triggered
6. Returns FileReferenceDTO with fileId

### Download Flow

1. Verify ownership (user owns app OR has ADMIN role)
2. Verify scanStatus = CLEAN
3. Generate SAS URL (read-only, 5-minute expiry)
4. HTTP 302 redirect to SAS URL

### Virus Scanning

Azure Defender for Storage scans blobs asynchronously. Results:
- **CLEAN** → file available for download and answer submission
- **INFECTED** → blob deleted, metadata marked INFECTED, answer referencing it rejected

---

## Angular Question Component Architecture

### Component-per-Type Model

Each question type maps to a dedicated Angular component. A `QuestionHostDirective` dynamically
instantiates the correct component at runtime based on the question type.

| Question Type | Component |
|---|---|
| TEXT | TextQuestionComponent |
| TEXT_AREA | TextAreaQuestionComponent |
| DATE | DateQuestionComponent |
| DECIMAL | DecimalQuestionComponent |
| WHOLE_NUMBER | WholeNumberQuestionComponent |
| CURRENCY | CurrencyQuestionComponent |
| PHONE | PhoneQuestionComponent |
| ZIP_CODE | ZipCodeQuestionComponent |
| ATTACHMENT | AttachmentQuestionComponent |
| SELECT_ONE | SelectOneQuestionComponent |
| SELECT_MULTI | SelectMultiQuestionComponent |
| CHECKBOX | CheckboxQuestionComponent |
| LABEL | LabelQuestionComponent |
| RADIO_YES_NO | RadioYesNoQuestionComponent |

### Layout Attributes (QuestionDisplayConfig)

| Attribute | Type | Default | Behaviour |
|---|---|---|---|
| `readonly` | boolean | false | Substitutes ReadonlyQuestionComponent |
| `columnSpan` | 1–12 | 12 | CSS grid column span |
| `labelPosition` | ABOVE / LEFT / HIDDEN | ABOVE | Label placement relative to input |
| `helpText` | string (max 500) | — | Guidance text below input, aria-describedby linked |
| `sectionGroup` | string (max 100) | — | Groups consecutive matching questions in a named section |

### Grid Layout

The page renders questions in a 12-column CSS grid. Questions with columnSpan < 12 flow
side-by-side until the row is full, then wrap. Section group containers span the full 12
columns and contain their own inner grid.

---

## API Versioning

The current API uses no explicit version prefix. All endpoints are effectively v1. If breaking
changes are required in future, new endpoints SHALL be versioned as `/api/v2/...`. The current
unversioned paths imply `/api/v1/` behaviour. Backwards compatibility SHOULD be maintained for
at least one major version cycle. Deprecated endpoints SHOULD return an `X-Deprecated` response
header with a sunset date before removal.

---

## Concurrent Edit Handling

`RoundPageQuestion` and `Question` updates are currently last-write-wins. No optimistic locking
is implemented. If two administrators simultaneously configure the same round's questions, the
second save silently overwrites the first.

For future hardening, add `@Version private Long version;` to entities requiring conflict
detection. When a version conflict occurs, JPA will throw `OptimisticLockException`; the
`GlobalExceptionHandler` should map this to HTTP 409 with a message instructing the
administrator to refresh and reapply their changes.
