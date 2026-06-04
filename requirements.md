# Requirements Document

## Introduction

The Grant Management Base Framework is a configurable, metadata-driven platform that enables
administrators to define questions, assemble them into pages, and bind pages to grant program
rounds without code changes. The system dynamically renders forms to end users based on their
role and program round context, persists answers into application-defined tables, and validates
data within a single transaction with full rollback on error.

The framework serves as the foundational layer for any grant management application, supporting
multiple programs and rounds with independent configurations, role-based page visibility, and
extensible question types. It is built on Java Spring Boot, JPA/Hibernate, and Angular, secured
by Spring Security integrated with Azure AD B2C (production) or local username/password
authentication (development). The persistence layer is database-agnostic, supporting both
PostgreSQL (default) and Oracle via Spring profiles.

A **Program** is the top-level grant initiative, carrying strategic attributes such as goal and
total budget. A **ProgramRound** is a time-bounded funding cycle within a Program, carrying the
date range, funds limit, status lifecycle, and the ordered set of pages that applicants complete.
This two-level hierarchy allows a single grant program to run multiple rounds (e.g., Round 1,
Round 2) while sharing the same high-level program identity.

The admin experience includes dedicated maintenance pages for questions, programs, program rounds,
pages, and organizations, accessible only to authenticated users with the ADMIN role.

## Glossary

- **QuestionService**: The Spring Boot service responsible for creating, updating, deleting, and
  retrieving question metadata.
- **PageService**: The Spring Boot service responsible for managing page definitions and
  question-to-page assignments within a program round context.
- **ProgramService**: The Spring Boot service responsible for managing top-level Program entities
  (goal, total budget, description) and their lifecycle.
- **ProgramRoundService**: The Spring Boot service responsible for managing ProgramRound lifecycle,
  page assignments, and round-scoped overrides within a parent Program.
- **PageBuilderService**: The runtime engine that assembles a fully-rendered page definition for
  a given program round, page, and user role, applying all override resolution.
- **ApplicationDataService**: The service responsible for persisting user-submitted answers and
  performing transactional validation.
- **AdminController**: The REST controller exposing admin endpoints for question, page, program,
  and program round management.
- **PageBuilderController**: The REST controller exposing endpoints for retrieving rendered page
  definitions.
- **ApplicationDataController**: The REST controller exposing endpoints for saving and loading
  application data.
- **DynamicDataRepository**: The repository that executes dynamic INSERT/UPDATE/SELECT operations
  against application tables using question metadata.
- **QuestionOption**: A child entity of Question representing a selectable option for SELECT_ONE,
  SELECT_MULTI, or RADIO_YES_NO question types. Each option may optionally carry its own
  targetTable, targetColumn, and lookupId for per-option answer routing.
- **Question**: A standalone metadata entity defining a form field, its type, target table/column
  mapping, validation rules, and optional hierarchical parent/child relationships to other
  Questions for conditional rendering.
- **Page**: A named container (template) that can be assigned to one or more program rounds. Each
  program round that assigns the same page template receives its own independent question list.
- **Program**: The top-level grant initiative entity carrying strategic attributes: `programName`,
  `goal`, `totalBudget`, optional `description`, and an ordered set of ProgramRounds.
- **ProgramRound**: A time-bounded funding cycle within a Program, carrying `roundName`,
  `startDate`, `endDate`, optional `fundsLimit`, `status` lifecycle, and an ordered set of pages.
  ProgramRound is the operational unit that applicants interact with. It replaces what was
  previously called "Program" in the page-assignment and override hierarchy.
- **RoundPage**: The join entity between a ProgramRound and a Page, storing display order plus
  optional round-scoped page name and description overrides.
- **RoundPageQuestion**: The join entity between a RoundPage and a Question. Stores display
  order, page-level label and required overrides, round-level label and required overrides,
  the exclusion flag, and role visibility. This is the single entity that captures all
  question-to-page configuration for a given (round, page) pair. There is no separate
  PageQuestion entity.
- **PageRenderDTO**: The API response payload returned by the PageBuilderService containing all
  resolved question metadata needed for Angular to render a form.
- **QuestionRenderDTO**: A component of PageRenderDTO representing a single question with all
  overrides resolved and child questions nested.
- **ChildQuestionRenderDTO**: A wrapper inside QuestionRenderDTO carrying the triggerValue and
  the nested child question fields.
- **PageSummaryDTO**: Lightweight page descriptor returned by getPagesForRound; pageName is the
  resolved name (override if set, else canonical).
- **SaveResult**: The API response payload returned after a save-and-validate operation,
  containing success status and field-level errors.
- **AnswerDTO**: A data transfer object representing a single user-submitted answer, containing
  questionId, value, and optional file reference.
- **UserPrincipal**: The authenticated user identity extracted from the Azure AD B2C JWT token.
- **QuestionType**: An enumeration of supported question types: TEXT, TEXT_AREA, DATE, DECIMAL,
  WHOLE_NUMBER, CURRENCY, PHONE, ZIP_CODE, ATTACHMENT, SELECT_ONE, SELECT_MULTI, CHECKBOX,
  LABEL, RADIO_YES_NO.
- **ProgramStatus**: An enumeration of top-level Program lifecycle states: ACTIVE, ARCHIVED.
- **RoundStatus**: An enumeration of ProgramRound lifecycle states: DRAFT, ACTIVE, CLOSED,
  ARCHIVED.
- **Override Priority**: The precedence order for resolving question label and required
  flag values: (1) round-level label override (RoundPageQuestion.roundLabelOverride) -- highest;
  (2) page-level overrides (RoundPageQuestion.labelOverride / requiredOverride) -- middle;
  (3) question default -- lowest. Overrides are resolved on the RoundPageQuestion record.
  A null or absent override at any level is skipped; the next lower level is used.
- **Page Name Override Priority**: The two-level precedence order for resolving page name and
  description: (1) round-scoped override (RoundPage.pageNameOverride /
  pageDescriptionOverride) -- highest; (2) canonical Page record -- default.
- **Organization**: An entity representing an organization that users belong to and on behalf of
  which grant applications are raised.
- **ProgramRoundConfig**: Request DTO used to update a RoundPage assignment, carrying
  displayOrder, pageNameOverride, and pageDescriptionOverride.
- **RoundQuestionConfig**: Request DTO used to configure a question within a (round, page)
  context: displayOrder, labelOverride (page-level), requiredOverride (page-level),
  visibleToRoles, roundLabelOverride, excluded.
- **Per-option target routing**: An optional feature of QuestionOption where targetTable,
  targetColumn, and lookupId are all set. When an applicant selects that option, the system
  writes lookupId into the option's targetTable.targetColumn instead of writing optionValue into
  the question-level target.
- **QuestionTemplate**: The Angular component responsible for rendering a single question. Each
  question type maps to a dedicated template component (e.g., TextQuestionComponent,
  DateQuestionComponent, SelectOneQuestionComponent). The template receives a
  QuestionRenderDTO and a QuestionDisplayConfig and renders either an editable input or a
  read-only display depending on the `readonly` flag.
- **QuestionDisplayConfig**: The set of layout and display attributes carried on a
  QuestionRenderDTO that control how the Angular template renders the question: `readonly`,
  `columnSpan`, `labelPosition`, `helpText`, and `sectionGroup`. These are configured per
  RoundPageQuestion and resolved by the PageBuilderService.
- **ReadonlyQuestionComponent**: A shared Angular component that renders any question type as
  a read-only label/value display. Used when `QuestionDisplayConfig.readonly = true`. Renders
  the resolved label and the current answer value (or a placeholder) without any input control.
- **Application**: An entity representing a single applicant's submission attempt for a specific
  ProgramRound. Carries status (DRAFT, SUBMITTED, WITHDRAWN), links to the applicant's user
  identity, organization, and program round. One application per (user, round) pair.
- **ApplicationService**: The Spring Boot service responsible for creating applications,
  enforcing round-active and organization-eligibility rules, preventing duplicate applications,
  and managing application status transitions.
- **QuestionOptionDTO**: API response for a single QuestionOption, including all routing fields.
- **ChildLinkDTO**: API response describing a parent-child question link: parentQuestionId,
  childQuestionId, triggerValue, childDisplayOrder.
- **UserProfileController**: The REST controller exposing the `/api/me` endpoint that returns
  the authenticated user's identity, roles, and organization membership.
- **FileScanWebhookController**: The REST controller exposing the internal
  `/api/internal/scan-results` webhook endpoint for receiving virus scan results from the
  external scanning service.

---

## Requirements

### Requirement 1: Question Definition and Management

**User Story:** As an administrator, I want to create and manage question definitions, so that I
can build a reusable library of form fields that can be assembled into pages without writing code.

#### Acceptance Criteria

1. WHEN an administrator submits a create-question request with all required fields valid, THE
   QuestionService SHALL persist a new Question entity and return a QuestionDTO containing the
   generated identifier.
2. THE QuestionService SHALL require `questionType` and `label` on every Question entity, where
   `label` must be a non-empty string of at most 255 characters.
3. THE QuestionService SHALL require `targetTable` and `targetColumn` on every Question entity
   whose `questionType` is not LABEL. Both values MUST match an entry in the system's
   registered application data table allowlist (see Requirement 21). IF either value is not
   in the allowlist, THE QuestionService SHALL return a validation error and reject the request.
4. WHEN an administrator creates a Question with `questionType` of ATTACHMENT, THE QuestionService
   SHALL accept and persist `allowedFileTypes` as a list of file extensions and `maxFileSizeMb` as
   an integer between 1 and 100 inclusive.
5. WHEN an administrator creates a Question with `questionType` other than ATTACHMENT, THE
   QuestionService SHALL ignore any submitted `allowedFileTypes` or `maxFileSizeMb` values.
6. WHEN an administrator creates a Question with `questionType` of SELECT_ONE or SELECT_MULTI,
   THE QuestionService SHALL accept and persist an ordered list of at least 1 and at most 200
   QuestionOption entries. This rule does NOT apply to RADIO_YES_NO, whose options are
   system-managed (see Requirement 22).
7. WHEN an administrator submits an update-question request for an existing Question, THE
   QuestionService SHALL apply the changes and return the updated QuestionDTO.
8. IF an update-question request references a Question identifier that does not exist, THEN THE
   QuestionService SHALL return an error indicating the Question was not found.
9. WHEN an administrator requests deletion of a Question, THE QuestionService SHALL mark the
   Question as inactive, preventing it from appearing in list results or new page assignments.
10. IF a deletion request references a Question identifier that does not exist, THEN THE
    QuestionService SHALL return an error indicating the Question was not found.
11. WHEN an administrator requests a list of questions, THE QuestionService SHALL return all
    active Question entities matching the supplied QuestionFilter criteria.
12. IF a create-question request is submitted with a missing or empty `questionType` or `label`,
    THEN THE QuestionService SHALL return a validation error and reject the request.
13. IF a create-question request for a SELECT_ONE or SELECT_MULTI Question is submitted without
    at least one QuestionOption entry, THEN THE QuestionService SHALL return a validation error
    and reject the request.

---

### Requirement 2: Question Options Management

**User Story:** As an administrator, I want to manage selectable options for radio, multi-select
questions, so that applicants are presented with a controlled set of choices.

#### Acceptance Criteria

1. WHEN an administrator adds an option to a SELECT_ONE or SELECT_MULTI question, THE
   QuestionService SHALL persist the QuestionOption with `optionLabel` (non-empty, at most 255
   characters), `optionValue` (non-empty, at most 100 characters), and `displayOrder` (>= 1).
2. WHEN options are requested for a question, THE QuestionService SHALL return them ordered by
   ascending `displayOrder`.
3. WHEN a QuestionOption is marked as `isOtherOption = true` on a SELECT_MULTI question, THE
   QuestionService SHALL persist that flag so the Angular renderer can display a free-text field.
   The `isOtherOption` flag is only valid for SELECT_MULTI questions; THE QuestionService SHALL
   return a validation error if it is set on any other question type.
4. IF an administrator attempts to add options to a Question whose `questionType` does not support
   options (i.e., any type other than SELECT_ONE or SELECT_MULTI), THEN THE QuestionService SHALL
   return a validation error and reject the request.
5. IF an administrator attempts to add a QuestionOption with an `optionValue` that already exists
   on the same Question, THEN THE QuestionService SHALL return a validation error.
6. WHEN two QuestionOptions share the same `displayOrder`, THE QuestionService SHALL break the
   tie by ordering them by ascending `id`.
7. WHEN an administrator sets `targetTable`, `targetColumn`, and `lookupId` on a QuestionOption,
   THE QuestionService SHALL persist all three fields and validate that either all three or none
   are provided (partial configuration is rejected). The `targetTable` and `targetColumn` values
   MUST also be present in the application data table allowlist; otherwise the request SHALL be
   rejected with a validation error.
8. WHEN a QuestionOption has `targetTable`, `targetColumn`, and `lookupId` all set, THE
   ApplicationDataService SHALL write `lookupId` into the option's `targetTable.targetColumn`
   when that option is selected, overriding the question-level target routing. IF a selected
   option has only a partial per-option routing configuration (i.e., not all three fields are
   set), THE ApplicationDataService SHALL fall back to the question-level target routing for
   that option.
9. Each of `targetTable`, `targetColumn`, and `lookupId` on a QuestionOption SHALL be at most
   100 characters.

---

### Requirement 3: Parent-Child Question Relationships

**User Story:** As an administrator, I want to define child questions that appear conditionally
based on a parent question's selected value, so that forms can adapt dynamically to applicant
responses.

#### Acceptance Criteria

1. WHEN an administrator links a child Question to a parent Question with a `triggerValue`, THE
   QuestionService SHALL persist the relationship on the child Question with `triggerValue` and
   `childDisplayOrder` (integer 1-999). Only RADIO_YES_NO questions may be parents; attempts to
   link a child to any other question type SHALL be rejected with a validation error.
2. THE QuestionService SHALL allow a single parent Question to have at most 50 child Questions.
3. THE QuestionService SHALL allow different parent Questions to each have their own child
   Questions.
4. IF an administrator attempts to create a circular parent-child relationship, THEN THE
   QuestionService SHALL return a validation error describing the cycle and reject the request.
5. IF an administrator submits a link-child request referencing a parent or child Question that
   does not exist, THEN THE QuestionService SHALL return a validation error.
6. WHEN a Question is deleted (marked inactive), THE QuestionService SHALL remove any parent/child
   links where that Question is either the parent or the child.
7. RADIO_YES_NO questions MAY themselves be child questions. When a RADIO_YES_NO question is
   linked as a child, it retains its parent capability — enabling multi-level branching (e.g.,
   a parent Yes/No triggers a child Yes/No, which in turn triggers its own children). The
   maximum nesting depth is not explicitly limited but circular references are still rejected
   per AC 4.

---

### Requirement 4: Page Definition and Question Assignment

**User Story:** As an administrator, I want to create pages and assign questions to them in a
defined order within a specific program round context, so that each round has an exclusive,
independently configured set of questions per page.

#### Acceptance Criteria

1. WHEN an administrator submits a valid create-page request, THE PageService SHALL persist a new
   Page entity with `pageName` and optional `pageDescription`, and return a PageDTO.
2. WHEN an administrator assigns a Question to a Page within a program round context, THE
   PageService SHALL persist a RoundPageQuestion record scoped to the (program round, page) pair
   with the specified `displayOrder` (integer 1-999).
3. WHEN an administrator assigns a Question to a Page with a `labelOverride`, THE PageService
   SHALL persist the override (at most 255 characters) as the page-level label on the
   RoundPageQuestion record.
4. WHEN an administrator assigns a Question to a Page with a `requiredOverride`, THE PageService
   SHALL persist the override on the RoundPageQuestion record so the page-specific required flag
   is used.
5. WHEN an administrator removes a Question from a Page, THE PageService SHALL delete only the
   corresponding RoundPageQuestion mapping record within that program round context. The Question
   entity itself SHALL NOT be deleted or deactivated. Other rounds using the same page template
   are unaffected.
6. WHEN an administrator requests a list of all pages, THE PageService SHALL return all active
   Page entities by default. An `includeInactive` flag SHALL return inactive pages as well.
7. IF a create-page request is submitted without a `pageName` or with a `pageName` exceeding 200
   characters, THEN THE PageService SHALL return a validation error.
8. IF an administrator attempts to assign a Question to a (program round, page) pair where that
   Question is already assigned, THEN THE PageService SHALL return a validation error.
9. WHEN a `displayOrder` conflict exists, THE PageService SHALL resequence all RoundPageQuestion
   `displayOrder` values for that (program round, page) pair.
10. Questions assigned to a page in one program round context SHALL NOT appear in any other round
    that uses the same page template. Each (program round, page) pair owns an exclusive,
    independent list of RoundPageQuestion records.
11. WHEN an administrator deactivates a Page, THE PageService SHALL set the Page's `active` flag
    to false, preventing it from appearing in default list results or being assigned to new
    program rounds. The Page entity is NOT deleted.
12. IF an administrator attempts to deactivate a Page that is currently assigned to one or more
    active ProgramRounds, THEN THE PageService SHALL return a validation error listing the
    affected rounds. Deactivation is only permitted when the page has no active round assignments.

---

### Requirement 5: Role-Based Question Visibility on Pages

**User Story:** As an administrator, I want to restrict which questions are visible to specific
user roles on a page, so that different roles see only the fields relevant to them.

#### Acceptance Criteria

1. WHEN an administrator sets role visibility on a RoundPageQuestion with a non-empty list of
   role names (each at most 100 characters, list at most 50 entries), THE PageService SHALL
   persist those role names in the `gms_round_page_question_role` table.
2. IF a RoundPageQuestion has an empty `visibleToRoles` collection, THEN THE PageBuilderService
   SHALL treat the question as visible to all roles.
3. WHEN a RoundPageQuestion has a non-empty `visibleToRoles` collection, THE PageBuilderService
   SHALL include the question only if at least one of the requesting user's roles is present.

---

### Requirement 6: Program Management

**User Story:** As an administrator, I want to create and manage top-level grant programs with a
goal and total budget, so that I can group related funding rounds under a single program identity.

#### Acceptance Criteria

1. WHEN an administrator submits a valid create-program request, THE ProgramService SHALL persist
   a new Program entity with `programName`, optional `description`, optional `goal` (text, at
   most 2000 characters), and optional `totalBudget` (decimal, precision 18 scale 2), and return
   a ProgramDTO.
2. THE ProgramService SHALL require `programName` on every Program; `programName` must be a
   non-empty string of at most 255 characters.
3. THE ProgramService SHALL set the initial `status` of a new Program to ACTIVE.
4. WHEN an administrator archives a Program, THE ProgramService SHALL transition its status to
   ARCHIVED. No further ProgramRounds may be created under an ARCHIVED Program.
5. WHEN an administrator requests a list of all programs, THE ProgramService SHALL return all
   Program entities.
6. WHEN an administrator requests a single program, THE ProgramService SHALL return the
   ProgramDTO including the list of associated ProgramRounds.
7. IF a create-program request is submitted without `programName`, THEN THE ProgramService SHALL
   return a validation error.
8. IF an administrator attempts to create a ProgramRound under an ARCHIVED Program, THEN THE
   ProgramService SHALL return a validation error.

---

### Requirement 7: Program Round Management

**User Story:** As an administrator, I want to create and manage program rounds with date ranges
and funds limits within a parent program, so that I can run multiple funding cycles under the
same grant program.

#### Acceptance Criteria

1. WHEN an administrator submits a valid create-round request, THE ProgramRoundService SHALL
   persist a new ProgramRound entity with `roundName`, `startDate`, `endDate`, and optional
   `fundsLimit`, linked to the specified parent Program, and return a ProgramRoundDTO.
2. THE ProgramRoundService SHALL require `roundName`, `startDate`, `endDate`, and `programId` on
   every ProgramRound.
3. THE ProgramRoundService SHALL set the initial `status` of a new ProgramRound to DRAFT.
4. WHEN an administrator submits a status-update request for a ProgramRound, THE
   ProgramRoundService SHALL accept only valid transitions: DRAFT -> ACTIVE, ACTIVE -> CLOSED,
   CLOSED -> ARCHIVED. Transitions to DRAFT from any other state, and any other non-sequential
   transitions, SHALL be rejected. This lifecycle validation applies only to the
   `PUT .../{roundId}/status` endpoint. The `DELETE .../{roundId}` endpoint invokes a separate
   `archiveRound()` method that sets status to ARCHIVED unconditionally from any non-ARCHIVED
   state, without going through this transition guard.
5. WHEN an administrator assigns a Page to a ProgramRound, THE ProgramRoundService SHALL persist
   a RoundPage record with the specified `displayOrder`.
6. WHEN an administrator removes a Page from a ProgramRound, THE ProgramRoundService SHALL delete
   only the RoundPage mapping record and all RoundPageQuestion mapping records scoped to that
   (round, page) pair. The Page entity itself SHALL NOT be deleted or deactivated; it remains
   available as a template for assignment to other rounds.
7. WHEN an administrator requests a list of all rounds for a program, THE ProgramRoundService
   SHALL return all ProgramRound entities for that program.
8. IF a create-round request is submitted with `startDate` after `endDate`, THEN THE
   ProgramRoundService SHALL return a validation error.
9. IF a create-round request is submitted without `roundName`, `startDate`, `endDate`, or
   `programId`, THEN THE ProgramRoundService SHALL return a validation error.
10. IF a status-update request specifies a disallowed transition, THEN THE ProgramRoundService
    SHALL return a validation error.
11. IF an administrator assigns a Page to a ProgramRound and the Page does not exist or is
    inactive, THEN THE ProgramRoundService SHALL return a validation error.
12. IF an administrator attempts to create a ProgramRound under a Program whose status is
    ARCHIVED, THEN THE ProgramRoundService SHALL return a validation error.

---

### Requirement 8: Round-Level Question Configuration Overrides

**User Story:** As an administrator, I want to override question labels and required flags at the
program round level, and exclude specific questions from a round, so that each round can present
a tailored form experience without modifying the shared question definitions.

#### Acceptance Criteria

1. WHEN an administrator applies a page-level `labelOverride` (at most 255 characters) or
   `requiredOverride` via
   `PUT /api/admin/programs/{programId}/rounds/{roundId}/pages/{pageId}/questions/{questionId}`,
   THE ProgramRoundService SHALL update the corresponding RoundPageQuestion record. The canonical
   gms_question.label SHALL NOT be modified.
2. WHEN an administrator applies a round-level `roundLabelOverride` (at most 255 characters)
   via the same endpoint, THE ProgramRoundService SHALL update the RoundPageQuestion record.
   The canonical required flag is unchanged.
3. WHEN an administrator sets `excluded = true`, THE ProgramRoundService SHALL persist the
   exclusion flag on the RoundPageQuestion record so the question is omitted from that round's
   rendered pages. The exclusion flag is stored exclusively in RoundPageQuestion.
4. THE label and required resolution order SHALL be: round-level override (highest) > page-level
   override > question default (lowest). A null or absent override at any level is skipped.
5. IF an administrator applies an override to a question not assigned to the specified
   round-page, THEN THE ProgramRoundService SHALL return a validation error.
6. WHEN an administrator requests the current configuration for a question via
   `GET /api/admin/programs/{programId}/rounds/{roundId}/pages/{pageId}/questions/{questionId}`,
   THE ProgramRoundService SHALL return the full RoundPageQuestion record including all override
   fields, exclusion flag, display order, and role visibility.

---

### Requirement 9: Page Builder -- Dynamic Page Rendering

**User Story:** As an applicant, I want the system to assemble and deliver a fully-rendered page
definition based on my program round and role, so that the Angular front end can render the
correct form without additional API calls.

#### Acceptance Criteria

1. WHEN a user requests a page via
   `GET /api/programs/{programId}/rounds/{roundId}/pages/{pageId}`, THE PageBuilderService SHALL
   return a PageRenderDTO containing the resolved page name, resolved page description, round
   identifier, program identifier, and ordered list of QuestionRenderDTOs.
2. THE PageBuilderService SHALL resolve each question's effective label using Override Priority:
   round-level override > page-level override > question default.
3. THE PageBuilderService SHALL resolve each question's effective required flag using the same
   Override Priority.
4. THE PageBuilderService SHALL resolve the effective page name using Page Name Override Priority:
   RoundPage.pageNameOverride if non-blank, otherwise the canonical page.pageName.
5. THE PageBuilderService SHALL resolve the effective page description: RoundPage
   .pageDescriptionOverride if non-blank, otherwise the canonical page.pageDescription.
6. THE PageBuilderService SHALL exclude from the PageRenderDTO any question whose
   RoundPageQuestion `visibleToRoles` is non-empty and none of the requesting user's roles are
   present.
7. THE PageBuilderService SHALL exclude from the PageRenderDTO any question whose
   RoundPageQuestion has `excluded = true`.
8. THE PageBuilderService SHALL include child questions nested under their parent
   QuestionRenderDTO, each annotated with the `triggerValue` that activates them.
9. THE PageBuilderService SHALL apply the same role-visibility and exclusion rules to child
   questions as to top-level questions.
10. THE PageBuilderService SHALL return questions in ascending `displayOrder`; ties broken by
    ascending `questionId`.
11. THE PageRenderDTO SHALL contain no duplicate questions identified by `questionId`.
12. IF the requested round-page combination does not exist or the ProgramRound status is not
    ACTIVE, THEN THE PageBuilderController SHALL return HTTP 404. This restriction applies only
    to the applicant-facing endpoint; the admin preview endpoint (Requirement 12 AC 36) bypasses
    the status check and works for rounds in any status including DRAFT, CLOSED, and ARCHIVED.
13. WHEN a user requests the list of pages for a round via
    `GET /api/programs/{programId}/rounds/{roundId}/pages`, THE PageBuilderService SHALL return
    only pages with at least one question visible to the requesting user's role. The `pageName`
    in each PageSummaryDTO SHALL be the resolved name.
14. IF the requesting user has no assigned roles, THEN THE PageBuilderService SHALL include only
    questions whose `visibleToRoles` collection is empty.
15. EACH QuestionRenderDTO SHALL include a `displayConfig` object (QuestionDisplayConfig)
    populated from the RoundPageQuestion record, containing:
    - `readonly` (boolean, default false): when true, the Angular template SHALL render the
      question as a read-only display using ReadonlyQuestionComponent instead of an input
      control.
    - `columnSpan` (integer 1–12, default 12): the number of grid columns the question occupies
      within the page layout grid.
    - `labelPosition` (enum: ABOVE, LEFT, HIDDEN; default ABOVE): controls where the question
      label is rendered relative to the input.
    - `helpText` (string, optional, max 500 characters): supplementary guidance text rendered
      below the input control.
    - `sectionGroup` (string, optional, max 100 characters): a logical grouping label. Questions
      sharing the same `sectionGroup` value SHALL be rendered within a visually distinct section
      with the group label as a heading.
16. THE PageBuilderService SHALL propagate `displayConfig` to child QuestionRenderDTOs using the
    child's own RoundPageQuestion record. Child questions MAY have different display configs
    from their parent.

**User Story:** As an applicant, I want the system to validate my answers against the question
type rules before saving, so that I receive immediate, field-level feedback on invalid input.

#### Acceptance Criteria

1. WHEN an answer is submitted for a `required = true` question with a null, empty, or
   whitespace-only value, THE ApplicationDataService SHALL add a field-level error.
2. WHEN an answer is submitted for a TEXT or TEXT_AREA question with a non-null `validationRegex`,
   THE ApplicationDataService SHALL validate the value against the regex. For TEXT_AREA, the regex
   is evaluated with DOTALL mode to match across line breaks.
3. WHEN an answer is submitted for a DATE question, THE ApplicationDataService SHALL validate the
   value is a valid ISO-8601 date string (yyyy-MM-dd). When `minValue` or `maxValue` are set on
   the RoundPageQuestion, the submitted date must fall within the range [minValue, maxValue]
   (inclusive). The min/max constraints are configured per (round, page, question) — not on the
   question definition itself.
4. WHEN an answer is submitted for a DECIMAL question, THE ApplicationDataService SHALL validate
   that the value is parseable as a Java BigDecimal.
5. WHEN an answer is submitted for a WHOLE_NUMBER question, THE ApplicationDataService SHALL
   validate that the value is parseable as a Java Long.
6. WHEN an answer is submitted for a CURRENCY question, THE ApplicationDataService SHALL validate
   the value matches `^\d{1,12}(\.\d{1,2})?$`.
7. WHEN an answer is submitted for a PHONE question, THE ApplicationDataService SHALL validate
   the value matches `^\+?[0-9\-\(\)\s]{7,15}$`.
8. WHEN an answer is submitted for a ZIP_CODE question, THE ApplicationDataService SHALL validate
   the value matches `^\d{5}(-\d{4})?$`.
9. WHEN an answer is submitted for an ATTACHMENT question, THE ApplicationDataService SHALL
   validate that the file extension is in `allowedFileTypes` and size does not exceed
   `maxFileSizeMb`.
10. WHEN an answer is submitted for a SELECT_ONE question, THE ApplicationDataService SHALL
    validate that the submitted value is one of the question's defined option values.
11. WHEN an answer is submitted for a SELECT_MULTI question, THE ApplicationDataService SHALL
    validate that each selected value is a defined option value.
12. WHEN an answer is submitted for a SELECT_MULTI question with the "Other" option selected,
    THE ApplicationDataService SHALL require a non-empty `otherText` value.
13. WHEN an answer is submitted for a CHECKBOX question, THE ApplicationDataService SHALL validate
    that the value is "true" or "false" (case-insensitive).
14. WHEN an answer is submitted for a LABEL question, THE ApplicationDataService SHALL accept and
    ignore any submitted value.
15. WHEN an answer references an unknown or inactive `questionId`, THE ApplicationDataService
    SHALL add a field-level error.

---

### Requirement 11: Transactional Save and Post-Save Validation

**User Story:** As an applicant, I want my form submission to be saved atomically, so that either
all answers are persisted correctly or none are, preventing partial or corrupt data.

#### Acceptance Criteria

1. WHEN the ApplicationDataService saves answers, it SHALL execute all operations within a single
   database transaction.
2. WHEN all answers pass validation, THE ApplicationDataService SHALL persist each answer into the
   target table and column identified by question metadata, applying per-option target routing
   where applicable.
3. WHEN a target table row does not yet exist for the given `applicationId`, THE
   DynamicDataRepository SHALL INSERT.
4. WHEN a target table row already exists, THE DynamicDataRepository SHALL UPDATE only the
   submitted columns.
5. WHEN all answers have been persisted, THE ApplicationDataService SHALL re-read each saved row
   and verify stored values equal submitted values.
6. IF re-read values do not match, THE ApplicationDataService SHALL add a field-level error and
   roll back the transaction.
7. IF any validation error exists, THE ApplicationDataService SHALL roll back and return a
   SaveResult with `success = false`.
8. WHEN all validation passes, THE ApplicationDataService SHALL commit and return a SaveResult
   with `success = true`.
9. WHEN an applicant requests previously saved data for a page, THE ApplicationDataService SHALL
   return the current values from application tables.
10. IF a database error occurs, THE ApplicationDataService SHALL roll back, log the error, and
    return a SaveResult with `success = false`.
11. IF the post-save re-read finds no row, THE ApplicationDataService SHALL treat this as a
    verification failure and roll back.

---

### Requirement 12: Admin REST API

**User Story:** As an administrator, I want a RESTful API for managing questions, pages, programs,
and program rounds, so that the Angular admin module can perform all configuration operations.

#### Acceptance Criteria

1. `POST /api/admin/questions` -- create question, return HTTP 201 + QuestionDTO.
2. `PUT /api/admin/questions/{id}` -- update question, return HTTP 200 + QuestionDTO.
3. `DELETE /api/admin/questions/{id}` -- deactivate question (soft-delete, sets active=false),
   return HTTP 204.
4. `GET /api/admin/questions` -- list questions, return HTTP 200 + paginated response. Supports
   query parameters: `type` (filter by questionType), `search` (free-text on label),
   `includeInactive` (boolean, default false), `page` (zero-based page index, default 0),
   `size` (page size, default 20, max 100), `sort` (field name + direction, e.g.
   `label,asc` or `createdAt,desc`; default `label,asc`). Response body follows the standard
   paginated envelope: `{ content: List<QuestionDTO>, totalElements, totalPages, page, size }`.
5. `GET /api/admin/questions/{id}` -- get single question with full detail including options and
   child question links, return HTTP 200 + QuestionDTO.
6. `POST /api/admin/questions/{id}/options` -- add a QuestionOption to a SELECT_ONE or
   SELECT_MULTI question, return HTTP 201 + QuestionOptionDTO.
7. `PUT /api/admin/questions/{id}/options/{optionId}` -- update a QuestionOption (label, value,
   displayOrder, isOtherOption, per-option routing fields), return HTTP 200 + QuestionOptionDTO.
8. `DELETE /api/admin/questions/{id}/options/{optionId}` -- remove a QuestionOption, return
   HTTP 204.
9. `PUT /api/admin/questions/{id}/options/reorder` -- reorder all options for a question by
   submitting an ordered list of optionIds, return HTTP 200 + List<QuestionOptionDTO>.
10. `POST /api/admin/questions/{parentId}/children` -- link a child question to a RADIO_YES_NO
    parent, supplying childQuestionId, triggerValue ("Yes"/"No"), and childDisplayOrder. Return
    HTTP 201 + ChildLinkDTO.
11. `PUT /api/admin/questions/{parentId}/children/{childId}` -- update triggerValue or
    childDisplayOrder for an existing child link, return HTTP 200 + ChildLinkDTO.
12. `DELETE /api/admin/questions/{parentId}/children/{childId}` -- unlink a child question from
    its parent, return HTTP 204. The child Question entity is NOT deleted.
13. `GET /api/admin/questions/{parentId}/children` -- list all child questions linked to a
    RADIO_YES_NO parent, return HTTP 200 + List<ChildLinkDTO>.
14. `POST /api/admin/pages` -- create page, return HTTP 201 + PageDTO.
15. `PUT /api/admin/pages/{id}` -- update page (name, description), return HTTP 200 + PageDTO.
16. `DELETE /api/admin/pages/{id}` -- deactivate page (soft-delete, sets active=false). Only
    permitted if the page is not currently assigned to any active ProgramRound. Return HTTP 204.
    If the page is assigned to one or more active rounds, return HTTP 409 with a list of
    affected rounds.
17. `GET /api/admin/pages` -- list pages, return HTTP 200 + paginated response. Supports
    query parameters: `includeInactive` (boolean, default false), `search` (free-text on
    pageName), `page` (default 0), `size` (default 20, max 100), `sort` (default
    `pageName,asc`). Response follows the standard paginated envelope.
17a. `GET /api/admin/pages/{id}` -- retrieve a single page by ID with full detail including
    page name, description, and active status. Return HTTP 200 + PageDTO. Return HTTP 404 if
    no page exists with the given ID.
18. `POST /api/admin/programs` -- create program, return HTTP 201 + ProgramDTO.
19. `PUT /api/admin/programs/{id}` -- update program (name, description, goal, totalBudget),
    return HTTP 200 + ProgramDTO.
20. `DELETE /api/admin/programs/{id}` -- archive program (status -> ARCHIVED), return HTTP 204.
21. `GET /api/admin/programs` -- list all programs, return HTTP 200 + List<ProgramDTO>.
22. `GET /api/admin/programs/{id}` -- get program with rounds, return HTTP 200 + ProgramDTO.
23. `POST /api/admin/programs/{programId}/rounds` -- create program round, return HTTP 201 +
    ProgramRoundDTO.
24. `PUT /api/admin/programs/{programId}/rounds/{roundId}` -- update round (name, dates,
    fundsLimit), return HTTP 200 + ProgramRoundDTO.
25. `DELETE /api/admin/programs/{programId}/rounds/{roundId}` -- soft-delete (archive) round,
    return HTTP 204.
26. `GET /api/admin/programs/{programId}/rounds` -- list rounds for a program, return HTTP 200 +
    List<ProgramRoundDTO>.
27. `PUT /api/admin/programs/{programId}/rounds/{roundId}/status` -- update round status,
    return HTTP 200 + ProgramRoundDTO.
28. `POST /api/admin/programs/{programId}/rounds/{roundId}/pages` -- assign page to round,
    return HTTP 200.
29. `GET /api/admin/programs/{programId}/rounds/{roundId}/pages` -- list pages assigned to a
    round, return HTTP 200 + List<RoundPageDTO>.
30. `DELETE /api/admin/programs/{programId}/rounds/{roundId}/pages/{pageId}` -- remove page from
    round: deletes the RoundPage mapping record and all scoped RoundPageQuestion mapping records.
    The Page entity is NOT deleted. Return HTTP 204.
31. `PUT /api/admin/programs/{programId}/rounds/{roundId}/pages/{pageId}` -- update round-scoped
    page overrides (pageNameOverride, pageDescriptionOverride, displayOrder), return HTTP 200.
    Does NOT modify the canonical gms_page record.
32. `POST /api/admin/programs/{programId}/rounds/{roundId}/pages/{pageId}/questions` -- assign
    question to page within round context, return HTTP 200.
33. `GET /api/admin/programs/{programId}/rounds/{roundId}/pages/{pageId}/questions/{questionId}`
    -- get full RoundPageQuestion config (displayOrder, page-level overrides, round-level
    overrides, exclusion flag, visibleToRoles, displayConfig), return HTTP 200.
34. `PUT /api/admin/programs/{programId}/rounds/{roundId}/pages/{pageId}/questions/{questionId}`
    -- update all question config for this (round, page) context: displayOrder, labelOverride
    (page-level), requiredOverride (page-level), roundLabelOverride, excluded, visibleToRoles,
    readonly, columnSpan, labelPosition, helpText, sectionGroup. Does NOT modify the canonical
    gms_question record. Return HTTP 200.
35. `DELETE /api/admin/programs/{programId}/rounds/{roundId}/pages/{pageId}/questions/{questionId}`
    -- remove question from page within round context: deletes RoundPageQuestion mapping only.
    Question entity is NOT deleted. Return HTTP 204.
36. `GET /api/admin/programs/{programId}/rounds/{roundId}/pages/{pageId}/preview` -- admin
    preview of the rendered page with full override resolution regardless of round status, return
    HTTP 200 + PageRenderDTO. The preview endpoint SHALL pass an empty roles list to
    PageBuilderService, ensuring all questions are visible regardless of their `visibleToRoles`
    configuration. This gives administrators a complete view of the full applicant experience.
37. IF a request references a non-existent resource, THE AdminController SHALL return HTTP 404.
38. IF a request body fails validation, THE AdminController SHALL return HTTP 400 with field-level
    error details.
39. IF a destructive action is blocked due to active dependencies (e.g. deactivating a page
    assigned to active rounds), THE AdminController SHALL return HTTP 409 with a response body:
    `{ "error": "CONFLICT", "message": "<description>", "affectedResources": [ { "type": "ProgramRound", "id": <roundId>, "name": "<roundName>" }, ... ] }`.
    This allows the admin UI to display which resources are blocking the action.

---

### Requirement 13: Admin Maintenance Pages and Role-Based Access

**User Story:** As an administrator, I want a dedicated admin portal with maintenance pages, so
that I can manage the system through a secure UI.

#### Acceptance Criteria

1. WHEN an authenticated ADMIN user navigates to `/admin-dashboard.html`, they SHALL be allowed
   access.
2. WHEN a non-ADMIN user accesses `/maintenance/**` or `/api/admin/**`, the system SHALL return
   HTTP 403.
3. WHEN an unauthenticated request accesses a protected resource, the system SHALL return HTTP 401.
4. THE admin portal SHALL include links to question, program, program round, page, and
   organization maintenance pages accessible only to ADMIN users.
5. THE system SHALL use JWT role claim extraction to determine access rights.

---

### Requirement 14: Round-Scoped Page Name and Description Overrides

**User Story:** As an administrator, I want to override the display name and description of a
page within a specific program round, so that applicants see a round-relevant title and
description without altering the shared page template used by other rounds.

#### Acceptance Criteria

1. WHEN an administrator sets a `pageNameOverride` (at most 200 characters) via
   `PUT /api/admin/programs/{programId}/rounds/{roundId}/pages/{pageId}`, THE ProgramRoundService
   SHALL persist the override in `gms_round_page.page_name_override`. The canonical
   `gms_page.pageName` SHALL remain unchanged.
2. WHEN an administrator sets a `pageDescriptionOverride` (at most 1000 characters), THE
   ProgramRoundService SHALL persist it in `gms_round_page.page_description_override`. The
   canonical `gms_page.pageDescription` SHALL remain unchanged.
3. WHEN the PageBuilderService resolves the page name for a PageRenderDTO, it SHALL use
   `RoundPage.pageNameOverride` if non-blank; otherwise fall back to `page.pageName`.
4. WHEN the PageBuilderService resolves the page description, it SHALL use
   `RoundPage.pageDescriptionOverride` if non-blank; otherwise fall back to
   `page.pageDescription`.
5. WHEN the PageSummaryDTO is built for `GET /api/programs/{programId}/rounds/{roundId}/pages`,
   the `pageName` field SHALL contain the resolved name.
6. WHEN the RoundPageDTO is returned, it SHALL include canonical `pageName` / `pageDescription`,
   the override fields `pageNameOverride` / `pageDescriptionOverride`, and computed
   `resolvedPageName` / `resolvedPageDescription` fields for UI convenience.
7. IF an administrator sets `pageNameOverride` to a blank string or null, THE ProgramRoundService
   SHALL clear the override and the page SHALL revert to its canonical name.
8. WHEN a Page is removed from a ProgramRound, THE ProgramRoundService SHALL delete only the
   RoundPage mapping record (including all stored overrides) and all RoundPageQuestion mapping
   records scoped to that (round, page) pair. The Page entity itself SHALL NOT be deleted or
   deactivated.
9. WHEN a Page that was previously removed from a ProgramRound is reassigned to the same round,
   THE ProgramRoundService SHALL create a fresh RoundPage record with default values. Previously
   deleted overrides and RoundPageQuestion configurations SHALL NOT be restored; the
   administrator must reconfigure the page assignment from scratch.

---

### Requirement 15: Authentication and Authorization via Azure AD B2C

**User Story:** As a system operator, I want all API endpoints secured by Azure AD B2C JWT
tokens, so that only authenticated and authorized users can access the system.

#### Acceptance Criteria

1. THE Spring Security Filter Chain SHALL validate every inbound API request for a valid JWT
   Bearer token before allowing access to any protected endpoint.
2. IF a request is received without a JWT Bearer token, THEN return HTTP 401.
3. IF a request is received with an expired or invalid JWT Bearer token, THEN return HTTP 401.
4. THE SecurityContextProvider SHALL extract user identity and role claims from the JWT token.
5. THE AdminController endpoints SHALL be accessible only to users with the ADMIN role claim;
   others receive HTTP 403.
6. THE PageBuilderController and ApplicationDataController endpoints SHALL be accessible to
   authenticated users with APPLICANT role or higher.
7. Static resources (`/favicon.ico`, `/static/**`, public HTML pages) SHALL be accessible without
   authentication.
8. THE system SHALL support mapping the authenticated user's role and organization membership into
   the runtime request context.
9. THE user's organization ID SHALL be carried as a custom claim in the Azure AD B2C JWT token
   (claim key: `extension_organizationId`). This claim is populated during user registration or
   profile update in B2C and contains the numeric ID of the user's `gms_organization` record.
   THE SecurityContextProvider SHALL extract this claim and include it in the `UserPrincipal`.
   There is no separate user-to-organization mapping table — the token is the authoritative
   source of the user's organization membership.
10. IF the `extension_organizationId` claim is absent or null in the JWT, THE
    SecurityContextProvider SHALL set `organizationId = null` on the UserPrincipal. Endpoints
    that require organization context (e.g. application creation) SHALL return HTTP 403 with a
    message indicating the user has no organization assigned.
11. THE user's roles SHALL be carried as a `roles` claim (string array) in the JWT. Supported
    role values are: `ADMIN`, `APPLICANT`. A user with no roles claim is treated as having no
    roles and can only access public resources.
12. THE system SHALL support a configurable authentication mode controlled by the
    `gms.security.auth-mode` property (environment variable `GMS_AUTH_MODE`):
    - `b2c` (default): Azure AD B2C JWT validation as described in ACs 1–11 above.
    - `local`: HTTP Basic authentication against a `gms_user` database table with BCrypt-hashed
      passwords. This mode is intended for local development and testing without requiring an
      Azure AD B2C tenant.
    The two modes are mutually exclusive; only one SecurityFilterChain is active at a time.
13. WHEN `auth-mode = local`, THE system SHALL authenticate requests using HTTP Basic
    credentials validated against the `gms_user` table. THE `LocalUserDetailsService` SHALL
    load the user by username, verify the BCrypt password hash, and map the user's `roles`
    column (comma-separated) to Spring Security authorities (ROLE_ADMIN, ROLE_APPLICANT).
14. WHEN `auth-mode = local`, THE SecurityContextProvider SHALL extract the UserPrincipal from
    the authenticated username by looking up the `gms_user` table. The user's `organization_id`
    FK provides the organization context (equivalent to the B2C `extension_organizationId`
    claim in production mode).
15. THE System SHALL persist local users in the `gms_user` table:

    | Column | Type | Constraints |
    |---|---|---|
    | id | INTEGER | PK (auto-generated identity) |
    | username | VARCHAR(100) | NOT NULL, UNIQUE |
    | password_hash | VARCHAR(255) | NOT NULL; BCrypt hash |
    | display_name | VARCHAR(255) | |
    | roles | VARCHAR(200) | NOT NULL; comma-separated (e.g. "ADMIN" or "APPLICANT") |
    | organization_id | INTEGER | FK → gms_organization; nullable |
    | active | BOOLEAN/NUMBER(1) | NOT NULL; default 1 |
    | created_at | TIMESTAMP | NOT NULL |
    | updated_at | TIMESTAMP | NOT NULL |

    The migration SHALL seed two default users for development:
    - `admin` / `password123` (BCrypt hash) — roles: ADMIN
    - `applicant` / `password123` (BCrypt hash) — roles: APPLICANT
16. WHEN `auth-mode = local`, THE Angular Applicant Module SHALL display a login screen at
    `/login` with username and password fields. On successful authentication, credentials are
    stored in session storage and attached to all subsequent API requests via an HTTP
    interceptor. A "Sign Out" button in the navigation bar clears the session and redirects
    to the login screen.
17. WHEN `auth-mode = b2c`, THE Angular Applicant Module SHALL use MSAL (Microsoft
    Authentication Library) to redirect the user to Azure AD B2C for sign-in. No login screen
    is rendered by the application; authentication is handled entirely by the B2C redirect flow.

---

### Requirement 16: Angular Admin Module -- Question Maintenance Screen

**User Story:** As an administrator, I want an Angular screen to create and manage question
definitions, so that I can build the question library through a user interface without needing
to understand the underlying data model.

#### Acceptance Criteria

1. THE Admin Module SHALL provide a question list screen displaying all active questions in a
   paginated, searchable table with columns for label, type, target table, and active status.
   The list SHALL support filtering by question type and free-text search on label.
2. THE question list SHALL provide an "Add Question" button that opens a create form, and an
   "Edit" action per row that opens the same form pre-populated with the question's current
   values.
3. WHEN an administrator opens the create/edit form, THE Admin Module SHALL render a `questionType`
   dropdown as the first field. Changing the type SHALL immediately show or hide the relevant
   sections below without requiring a page reload.
4. THE form SHALL always display: `label` (text input), `targetTable` (text input), `targetColumn`
   (text input), and `required` (checkbox). For LABEL type questions, `targetTable` and
   `targetColumn` SHALL be hidden as they are not applicable.
5. WHEN `questionType` is TEXT or TEXT_AREA, THE Admin Module SHALL display an optional
   `validationRegex` field with a helper tooltip explaining regex syntax and a live test input
   so the administrator can verify the pattern before saving.
6. WHEN `questionType` is TEXT_AREA, THE Admin Module SHALL render the preview area as a
   `<textarea>` so the administrator can see how the field will appear to applicants.
7. WHEN `questionType` is SELECT_ONE or SELECT_MULTI, THE Admin Module SHALL display an Options
   section with a drag-and-drop list of current options and an "Add Option" button. Each option
   row SHALL show `optionLabel`, `optionValue`, and a remove button. An expandable "Advanced
   Routing" panel per option SHALL reveal the optional `targetTable`, `targetColumn`, and
   `lookupId` fields. The section SHALL enforce at least one option before saving.
8. WHEN `questionType` is SELECT_MULTI, each option row SHALL additionally show an "Other
   (free text)" toggle for `isOtherOption`. Only one option may have this toggle enabled; the
   Admin Module SHALL enforce this and show an inline error if a second is attempted.
9. WHEN `questionType` is ATTACHMENT, THE Admin Module SHALL display a `allowedFileTypes` tag
   input (comma-separated file extensions) and a `maxFileSizeMb` number input (1–100) with
   helper text showing the valid range.
10. WHEN `questionType` is DATE, THE Admin Module SHALL NOT display date range pickers on the
    question creation/edit form. Date min/max constraints are configured per round at the
    RoundPageQuestion level — see Requirement 18 for the configuration panel description.
11. WHEN `questionType` is RADIO_YES_NO, THE Admin Module SHALL display an informational banner
    stating "Yes and No options are managed automatically by the system." The options section
    SHALL be hidden. A "Child Questions" section SHALL appear below, showing a table of currently
    linked child questions with columns for child label, trigger value ("Yes" / "No"), and
    display order. An "Add Child Question" button SHALL open a search-and-select dialog filtered
    to non-RADIO_YES_NO, non-child questions, with a trigger value dropdown and display order
    input. Each row SHALL have a remove button to unlink the child. The section SHALL show a
    count badge (e.g., "3 / 50 children") to indicate proximity to the 50-child limit.
12. THE form SHALL display a sticky save bar at the bottom with a "Save" button and a "Cancel"
    button. WHEN an administrator saves a question, THE Admin Module SHALL call the
    AdminController API, show an inline success notification on success, or display field-level
    validation errors inline next to the relevant fields on failure.
13. THE question list SHALL provide a "Deactivate" action per row with a confirmation dialog
    stating "This question will be removed from all future page assignments. Existing data is
    unaffected." Deactivated questions SHALL be hidden from the default list view but accessible
    via a "Show inactive" toggle.

---

### Requirement 17: Angular Admin Module -- Page Maintenance Screen

**User Story:** As an administrator, I want an Angular screen to create and manage reusable page
templates, so that I can define the structure of form pages independently of any specific program
round.

#### Acceptance Criteria

1. THE Admin Module SHALL provide a page list screen displaying all active pages in a searchable
   table with columns for page name, description (truncated), and active status. A "Show
   inactive" toggle SHALL reveal deactivated pages.
2. THE page list SHALL provide an "Add Page" button and an "Edit" action per row.
3. WHEN an administrator opens the page create/edit form, THE Admin Module SHALL display a
   `pageName` field (required, max 200 characters with a live character counter) and an optional
   `pageDescription` textarea. The form SHALL show an inline error if `pageName` is blank or
   exceeds the limit before the save request is made.
4. THE page form SHALL include a "Save" button and a "Cancel" button. On save success, the Admin
   Module SHALL show an inline success notification. On failure, field-level errors SHALL appear
   inline.
5. THE page form SHALL include a read-only "Used in Rounds" section listing the program rounds
   that currently have this page assigned, so administrators can understand the impact of
   changes before editing.
6. WHEN an administrator views a page within a round context (accessed from the Round Maintenance
   screen), THE Admin Module SHALL display the question assignment panel for that (round, page)
   pair. This panel SHALL show the currently assigned questions in a drag-and-drop ordered list.
7. THE question assignment panel SHALL provide an "Add Question" button that opens a
   search-and-select dialog showing all active questions not already assigned to this
   (round, page) pair. The dialog SHALL support free-text search on label and filtering by
   question type.
8. WHEN a question is selected for assignment, THE Admin Module SHALL show an inline
   configuration form with fields for `displayOrder` (pre-filled with the next available order),
   `labelOverride` (optional, max 255 characters), `requiredOverride` (optional checkbox), and
   a `visibleToRoles` multi-select. The administrator SHALL be able to confirm or cancel before
   the assignment is saved.
9. EACH assigned question row in the panel SHALL show the resolved label (override if set,
   otherwise the canonical label, visually distinguished), the question type badge, display
   order, and an expand toggle to reveal the full configuration (overrides, role visibility).
   An "Edit" icon SHALL open the inline configuration form for that question. A "Remove" icon
   SHALL prompt a confirmation before deleting the RoundPageQuestion mapping.
10. WHEN a RADIO_YES_NO question is assigned to the page, THE Admin Module SHALL display its
    linked child questions indented beneath it with a branching indicator showing the trigger
    value ("Yes" / "No") for each child. Child questions SHALL appear as separate assignable
    rows that can be independently configured. A helper tooltip SHALL explain that child question
    links are defined on the Question maintenance screen.
11. THE question assignment panel SHALL show a drag handle on each row to allow reordering.
    Dropping a question to a new position SHALL immediately update `displayOrder` values and
    persist the change.

---

### Requirement 18: Angular Admin Module -- Program and Program Round Maintenance Screen

**User Story:** As an administrator, I want an Angular screen to create programs and manage their
rounds, pages, and question configurations, so that I can set up grant programs through a guided,
step-by-step interface without needing to navigate between multiple disconnected screens.

#### Acceptance Criteria

1. THE Admin Module SHALL provide a program list screen displaying all programs in a table with
   columns for name, status (ACTIVE / ARCHIVED), goal (truncated), total budget, and number of
   rounds. An "Add Program" button SHALL open a create form. An "Edit" action per row SHALL open
   the program edit form. An "Archive" action SHALL prompt a confirmation dialog before
   archiving.
2. WHEN an administrator opens the program create/edit form, THE Admin Module SHALL display
   `programName` (required), `description` (optional textarea), `goal` (optional textarea, max
   2000 characters with a live character counter), and `totalBudget` (optional currency input).
   The form SHALL show inline validation errors before the save request is made.
3. THE program detail view SHALL display the program's attributes at the top and a "Rounds" tab
   below listing all rounds for that program in a table with columns for round name, status,
   start date, end date, funds limit, and number of pages. An "Add Round" button SHALL open the
   round create form.
4. WHEN an administrator opens the round create/edit form, THE Admin Module SHALL display
   `roundName` (required), `startDate` (required date picker), `endDate` (required date picker),
   and `fundsLimit` (optional currency input). The Admin Module SHALL validate client-side that
   `startDate` is not after `endDate` and show an inline error before saving.
5. THE round detail view SHALL use a tabbed layout with tabs: "Pages", "Settings", and "Preview".
   This keeps page configuration, round settings, and the live preview in one place without
   requiring navigation away.
6. THE "Pages" tab SHALL display the pages assigned to this round as an ordered, drag-and-drop
   list. Each page row SHALL show the resolved page name (with a tooltip showing the canonical
   name if an override is set), a question count badge, an expand toggle, and action buttons for
   "Configure", "Override Name", and "Remove".
7. AN "Add Page" button on the "Pages" tab SHALL open a search-and-select dialog showing all
   active pages not already assigned to this round. After selection, the page is added to the
   bottom of the list with a default display order.
8. WHEN an administrator clicks "Override Name" on a page row, THE Admin Module SHALL show an
   inline edit panel with `pageNameOverride` (max 200 characters) and `pageDescriptionOverride`
   (max 1000 characters) fields, a character counter for each, and a side-by-side comparison
   showing the canonical name alongside the current override value. A "Clear Override" button
   SHALL reset both fields to blank, reverting to the canonical values.
9. WHEN an administrator clicks "Configure" on a page row, THE Admin Module SHALL expand an
   inline question configuration panel (or navigate to a dedicated page-within-round view for
   complex configurations). This panel SHALL show the questions assigned to this (round, page)
   pair in drag-and-drop order, identical in structure to the question assignment panel described
   in Requirement 17 AC 6–11.
10. EACH question row in the round-page configuration panel SHALL show a two-level override
    indicator: a "Page" badge if a page-level override is set, and a "Round" badge if a
    round-level override is set, so administrators can see at a glance which overrides are active.
    Clicking "Edit" on a question row SHALL open an inline form with clearly labelled sections:
    "Page-Level Overrides" (labelOverride, requiredOverride), "Round-Level Overrides"
    (roundLabelOverride), "Exclude from this round" (excluded checkbox), and "Role Visibility"
    (multi-select). A helper note SHALL explain the override resolution order: round overrides
    take precedence over page overrides, which take precedence over the question default.
11. WHEN a RADIO_YES_NO question is present in the round-page configuration panel, THE Admin
    Module SHALL display its child questions indented beneath it with a "Yes →" or "No →"
    trigger label. Each child question row SHALL be independently configurable with the same
    override form as any other question. A collapsed/expanded toggle SHALL allow the
    administrator to hide child questions to reduce visual clutter when not needed.
12. THE "Settings" tab SHALL display the round status with a status transition button (e.g.,
    "Activate Round", "Close Round", "Archive Round") that is labelled according to the next
    valid transition. The button SHALL be disabled with a tooltip if no valid transition is
    available. A status history log SHALL show when each transition occurred.
13. THE "Preview" tab SHALL render the live preview for each page in the round by calling
    `GET /api/admin/programs/{programId}/rounds/{roundId}/pages/{pageId}/preview`. A page
    selector dropdown SHALL allow switching between pages. The preview SHALL render exactly as
    applicants would see it, including resolved labels, required indicators, and conditional
    child question visibility.
14. WHEN an administrator removes a page from a round, THE Admin Module SHALL show a confirmation
    dialog stating "This will remove the page and all its question configurations from this
    round. The page template and questions will not be deleted." The remove action SHALL only
    proceed after confirmation.
15. WHEN an administrator saves any change (program, round, page override, question config), THE
    Admin Module SHALL show an inline success notification. On failure, field-level errors SHALL
    appear inline next to the relevant fields without losing unsaved changes in other sections.

---

### Requirement 19: Angular Applicant Module -- Dynamic Form Rendering

**User Story:** As an applicant, I want to fill out grant application forms that are dynamically
rendered based on my program round and role, so that I only see the fields relevant to me.

#### Acceptance Criteria

1. WHEN the Applicant Module loads a page, it SHALL call
   `GET /api/programs/{programId}/rounds/{roundId}/pages/{pageId}` and render each
   QuestionRenderDTO. The page title SHALL be the `pageName` from the PageRenderDTO (already
   resolved).
2. SELECT_ONE questions SHALL render as radio buttons.
3. SELECT_MULTI questions SHALL render as checkboxes with a free-text field for `isOtherOption`.
4. CHECKBOX questions SHALL render as a single checkbox control.
5. ATTACHMENT questions SHALL render as a file upload control.
6. LABEL questions SHALL render as read-only display text with no input.
7. TEXT questions SHALL render as a single-line `<input type="text">` control.
8. TEXT_AREA questions SHALL render as a multi-line `<textarea>` control.
9. DATE questions SHALL render as `<input type="date">` with `min` and `max` attributes
   populated from `QuestionRenderDTO.minValue` and `maxValue` when present. Both fields are
   optional strings in yyyy-MM-dd format sourced from the RoundPageQuestion record.
10. WHEN a parent question's selected value matches a child question's `triggerValue`, the child
    SHALL be displayed immediately below the parent.
11. WHEN there is no matching triggerValue, all child questions of that parent SHALL be hidden.
12. WHEN an applicant submits a page, the module SHALL call
    `POST /api/applications/{appId}/pages/{pageId}/answers` and display field-level errors.
13. WHEN `success = true` is received, the module SHALL navigate to the next page or display a
    completion message.
14. THE Applicant Module SHALL lay out questions using a 12-column grid. Each question SHALL
    occupy the number of columns specified by `QuestionRenderDTO.displayConfig.columnSpan`
    (1–12). Questions with smaller column spans SHALL flow side-by-side within the same row
    until the row is full, then wrap to the next row.
15. WHEN `QuestionRenderDTO.displayConfig.readonly = true`, THE Applicant Module SHALL render
    the question using ReadonlyQuestionComponent regardless of question type. The component
    SHALL display the resolved label and the current saved answer value (or an em-dash
    placeholder if no answer exists). No input control SHALL be rendered and the field SHALL
    NOT be included in the answer submission payload.
16. WHEN `QuestionRenderDTO.displayConfig.labelPosition = ABOVE`, THE Applicant Module SHALL
    render the label above the input control. WHEN `labelPosition = LEFT`, the label SHALL be
    rendered to the left of the input in a two-column layout within the question's grid cell.
    WHEN `labelPosition = HIDDEN`, the label SHALL be rendered as a visually hidden element
    (accessible to screen readers but not visible) and the input SHALL occupy the full cell.
17. WHEN `QuestionRenderDTO.displayConfig.helpText` is non-blank, THE Applicant Module SHALL
    render the help text as a small descriptive paragraph immediately below the input control
    and above any validation error message.
18. WHEN two or more consecutive QuestionRenderDTOs share the same non-blank
    `displayConfig.sectionGroup` value, THE Applicant Module SHALL wrap them in a visually
    distinct section container with the `sectionGroup` value rendered as a section heading.
    A new section container SHALL begin whenever the `sectionGroup` value changes or becomes
    blank.
19. THE Applicant Module SHALL provide an Application Dashboard page as the entry point for an
    applicant's interaction with a program round. This page SHALL:
    - Display the program name, round name, and application status (DRAFT / SUBMITTED /
      WITHDRAWN).
    - Show the list of pages for the round (from `GET /api/programs/{pId}/rounds/{rId}/pages`)
      with a completion indicator per page (based on whether saved answers exist).
    - Allow the applicant to navigate to any page to fill in or review answers.
    - Display a "Submit Application" button when all required pages have saved answers. The
      button SHALL call `PUT /api/applications/{appId}/status` with status SUBMITTED.
20. WHEN the ApplicationDTO returned by `GET .../applications/mine` has
    `eligibilityWarning = true`, THE Applicant Module SHALL display a prominent warning banner
    at the top of the Application Dashboard page with the message: "Your organization type is
    no longer eligible for this program round. Your application has been preserved but cannot
    be submitted. Please contact the program administrator for assistance." The "Submit
    Application" button SHALL be disabled while the warning is active.
21. THE warning banner SHALL also appear at the top of every form page when the applicant
    navigates into a page from the dashboard while `eligibilityWarning = true`. The form SHALL
    remain read-only (all questions rendered via ReadonlyQuestionComponent) to prevent edits
    that cannot be submitted.

---

### Requirement 30: Angular Question Component Architecture

**User Story:** As a front-end developer, I want each question type to be implemented as a
dedicated Angular component that accepts a standard input contract, so that the form rendering
engine is extensible, testable, and consistent across all question types.

#### Acceptance Criteria

1. THE Angular application SHALL implement a dedicated question component for each supported
   question type: TextQuestionComponent, TextAreaQuestionComponent, DateQuestionComponent,
   DecimalQuestionComponent, WholeNumberQuestionComponent, CurrencyQuestionComponent,
   PhoneQuestionComponent, ZipCodeQuestionComponent, AttachmentQuestionComponent,
   SelectOneQuestionComponent, SelectMultiQuestionComponent, CheckboxQuestionComponent,
   LabelQuestionComponent, and RadioYesNoQuestionComponent.
2. EACH question component SHALL accept two inputs:
   - `question: QuestionRenderDTO` — the fully resolved question definition including label,
     options, validation constraints, child questions, and `displayConfig`.
   - `control: AbstractControl` — the reactive form control bound to this question's answer
     value, provided by the parent page form group.
3. THE Applicant Module SHALL use a `QuestionHostDirective` (or equivalent dynamic component
   loader) that maps `QuestionRenderDTO.questionType` to the corresponding component class and
   instantiates it at runtime. Adding a new question type SHALL require only registering a new
   component in the type-to-component map without modifying the page rendering logic.
4. EACH question component SHALL check `question.displayConfig.readonly` on initialisation. IF
   `readonly = true`, the component SHALL delegate rendering to ReadonlyQuestionComponent by
   emitting a `readonlyMode` output or by the host directive substituting
   ReadonlyQuestionComponent in place of the type-specific component.
5. ReadonlyQuestionComponent SHALL accept `question: QuestionRenderDTO` and `value: string`
   inputs. It SHALL render the resolved label and the display value. For SELECT_ONE and
   SELECT_MULTI types, it SHALL resolve the option label(s) from the options list rather than
   displaying the raw stored value. For ATTACHMENT, it SHALL render the file name as a
   non-clickable text label.
6. EACH question component SHALL apply the `displayConfig.columnSpan` value as a CSS class or
   inline style on its host element (e.g., `col-span-{n}` in a Tailwind grid or equivalent).
   The component SHALL NOT hard-code layout widths.
7. EACH question component SHALL render the label element using `displayConfig.labelPosition`:
   ABOVE renders a `<label>` block above the control; LEFT renders a `<label>` inline to the
   left using a two-column flex or grid layout within the component; HIDDEN renders the label
   with `class="sr-only"` (screen-reader only).
8. EACH question component SHALL render `displayConfig.helpText` as a `<small>` or `<p>`
   element with a `aria-describedby` association to the input control when non-blank.
9. EACH question component SHALL emit a `valueChange` output event carrying the new answer
   value whenever the user modifies the input. The parent page component SHALL listen to this
   event to update the reactive form and trigger conditional child question visibility.
10. RadioYesNoQuestionComponent SHALL render two radio buttons (Yes / No) and, after a selection
    is made, SHALL evaluate each child question's `triggerValue` against the selected value.
    Matching child questions SHALL be rendered inline below the parent using the same
    QuestionHostDirective. Non-matching child questions SHALL be hidden and their form controls
    SHALL be reset to null so stale values are not submitted.
11. EACH question component SHALL display field-level validation errors from the bound
    `AbstractControl` below the input (and below `helpText` if present). Error messages SHALL
    be human-readable strings mapped from validator keys (e.g., `required` → "This field is
    required", `pattern` → "Value does not match the required format").
12. THE question component library SHALL be implemented as a standalone Angular feature module
    (or set of standalone components) so it can be imported independently by both the Applicant
    Module and the Admin Module's Live Preview feature.

---

### Requirement 20: Data Persistence -- Metadata Tables

**User Story:** As a system architect, I want the metadata schema to reflect the Program ->
ProgramRound -> RoundPage hierarchy, so that the database structure accurately represents the
domain model.

#### Acceptance Criteria

1. THE System SHALL persist Program entities in the `gms_program` table with columns:
   `program_name` (VARCHAR 255, NOT NULL), `description` (VARCHAR 2000), `goal` (VARCHAR 2000),
   `total_budget` (NUMBER precision 18 scale 2), `status` (VARCHAR 20, values: ACTIVE, ARCHIVED),
   `created_at`, and `updated_at`.
2. THE System SHALL persist ProgramRound entities in the `gms_program_round` table with columns:
   `round_name` (VARCHAR 255, NOT NULL), `start_date`, `end_date`, `funds_limit` (NUMBER
   precision 18 scale 2), `status` (VARCHAR 20, values: DRAFT, ACTIVE, CLOSED, ARCHIVED),
   `program_id` (FK to gms_program), `eligible_organization_types`, `created_at`, and
   `updated_at`.
3. THE System SHALL persist RoundPage entities in the `gms_round_page` table with FKs to
   `gms_program_round` and `gms_page`, plus `display_order`, `page_name_override` (VARCHAR 200),
   and `page_description_override` (VARCHAR 1000) columns.
4. THE System SHALL persist RoundPageQuestion entities in the `gms_round_page_question` table
   with FKs to `gms_round_page` and `gms_question`, and columns for `display_order`,
   `label_override` (page-level, VARCHAR 255), `required_override` (page-level),
   `round_label_override` (round-level, VARCHAR 255), `excluded` (NOT NULL, default 0),
   `readonly` (NUMBER(1), default 0),
   `column_span` (NUMBER, default 12, values 1–12), `label_position` (VARCHAR 10, values:
   ABOVE, LEFT, HIDDEN; default ABOVE), `help_text` (VARCHAR 500), and `section_group`
   (VARCHAR 100). Unique constraint: (round_page_id, question_id). This single table captures
   all question-to-page configuration for a (round, page) pair. There is no separate
   gms_page_question table. The excluded flag exists solely here.
5. THE System SHALL persist Question entities in the `gms_question` table with columns for
   `question_type`, `label`, `target_table`, `target_column`, `required`, `validation_regex`,
   `allowed_file_types`, `max_file_size_mb`, `parent_question_id`, `trigger_value`,
   `child_display_order`, `active`, `created_at`, and `updated_at`. The `min_date` and
   `max_date` columns have been removed — min/max constraints are now stored on
   `gms_round_page_question` as `min_value` and `max_value`.
6. THE System SHALL persist QuestionOption entities in the `gms_question_option` table with a FK
   to `gms_question`, and with columns `option_target_table` (VARCHAR 100), `option_target_column`
   (VARCHAR 100), and `lookup_id` (VARCHAR 100) for per-option target routing.
7. THE System SHALL persist Question parent/child hierarchy in `gms_question` via an optional
   `parent_question_id` FK on child Questions, plus `trigger_value` and `child_display_order`.
8. THE System SHALL persist Page entities in the `gms_page` table with `active`, `created_at`,
   and `updated_at` columns.
10. THE System SHALL persist role visibility entries in the `gms_round_page_question_role` table
    with a FK to `gms_round_page_question`.
11. ALL tables with an integer primary key SHALL use the database's native identity generation
    mechanism (e.g., `GENERATED ALWAYS AS IDENTITY` in Oracle, `BIGINT GENERATED ALWAYS AS
    IDENTITY` in PostgreSQL). JPA entities SHALL use
    `@GeneratedValue(strategy = GenerationType.IDENTITY)`. This eliminates the need for manually
    managed sequences and ensures IDs are assigned by the database on INSERT. The
    `gms_file_attachment` table is the sole exception — it uses a UUID (36-character string)
    primary key generated by the application layer. Exact DDL type names differ by database —
    see the Flyway migration scripts in `db/migration/postgresql/` and `db/migration/oracle/`
    for database-specific syntax.
12. THE System SHALL persist status transition history in the `gms_status_history` table:

    | Column | Type | Constraints |
    |---|---|---|
    | id | INTEGER | PK (auto-generated identity) |
    | entity_type | VARCHAR(50) | NOT NULL; values: PROGRAM, PROGRAM_ROUND, APPLICATION |
    | entity_id | INTEGER | NOT NULL; the ID of the entity whose status changed |
    | from_status | VARCHAR(20) | Previous status (null for initial creation) |
    | to_status | VARCHAR(20) | NOT NULL; new status |
    | changed_by | VARCHAR(255) | NOT NULL; userId of the actor |
    | changed_at | TIMESTAMP | NOT NULL |
    | reason | VARCHAR(500) | Optional; e.g. "Reopened by admin" |

    THE ProgramService, ProgramRoundService, and ApplicationService SHALL insert a row into
    this table on every status transition. The admin UI's "status history log" reads from
    this table.

---

### Requirement 21: Data Persistence -- Application Data Tables

**User Story:** As a system architect, I want the application data repository to use safe,
parameterized dynamic SQL against a known set of pre-existing application tables, so that
answers are persisted correctly without SQL injection risk.

#### Acceptance Criteria

1. THE DynamicDataRepository SHALL construct a parameterized SQL INSERT using `targetTable` and
   `targetColumn` from the resolved answer target (after per-option routing). Table and column
   names are identifiers and CANNOT be parameterized via JDBC `?` placeholders; they MUST
   instead be validated against the application data table allowlist before being interpolated
   into the SQL string.
2. THE DynamicDataRepository SHALL construct a parameterized SQL UPDATE targeting only submitted
   columns. Column names SHALL be validated against the allowlist before interpolation.
3. THE DynamicDataRepository SHALL use parameterized `?` placeholders exclusively for all
   answer VALUES in INSERT and UPDATE statements to prevent SQL injection on data values.
4. THE DynamicDataRepository SHALL query the target table by `application_id` to determine
   INSERT vs UPDATE.
5. THE DynamicDataRepository SHALL participate in the caller's active Spring transaction.
6. THE system SHALL maintain an application data table allowlist: a configuration-driven
   registry of permitted `(tableName, columnName)` pairs. This allowlist is loaded at
   application startup from a configuration source (e.g. `application.yml` or a dedicated
   `gms_allowed_target` database table) and cached for the lifetime of the application.
7. WHEN a `targetTable` or `targetColumn` value is resolved at answer-save time and is NOT
   present in the allowlist, THE DynamicDataRepository SHALL throw a security exception,
   roll back the transaction, and log the violation. This is a defence-in-depth check
   supplementing the validation performed at question-creation time.
8. Application data tables are pre-existing tables created as part of the application schema.
   They are NOT created at runtime by the framework. Each application data table MUST contain
   at minimum the following columns:
   - `application_id` (NUMBER, FK to `gms_application`) — identifies which application the
     row belongs to.
   - One or more answer columns whose names match the `targetColumn` values configured on
     questions. Answer columns store values as VARCHAR2 to accommodate all question types
     (numbers, dates, and text are all stored as strings; type conversion is the
     responsibility of the consuming layer).
   - `created_at` (TIMESTAMP) and `updated_at` (TIMESTAMP) audit columns.
   The `application_id` column is the lookup key used by DynamicDataRepository to determine
   INSERT vs UPDATE.
9. THE system SHALL ship with a reference application data table `gms_application_data` for
   use in simple single-table configurations. Its schema is:

   | Column | Type | Constraints |
   |---|---|---|
   | id | NUMBER | PK |
   | application_id | NUMBER | FK → gms_application, NOT NULL |
   | question_id | NUMBER | FK → gms_question, NOT NULL |
   | value_text | VARCHAR2(4000) | Stores all answer values as strings |
   | created_at | TIMESTAMP | NOT NULL |
   | updated_at | TIMESTAMP | NOT NULL |
   | UNIQUE | | (application_id, question_id) |

   For multi-table configurations (where different questions route to different domain tables),
   implementors define their own tables following the minimum column contract in AC 8 above
   and register them in the allowlist.
10. THE allowlist configuration entry for each permitted target SHALL specify:
    - `tableName`: the exact Oracle table name (case-insensitive match).
    - `allowedColumns`: the list of column names permitted for that table.
    Example `application.yml` allowlist entry:
    ```yaml
    gms:
      dynamic-data:
        allowed-targets:
          - table: gms_application_data
            columns: [value_text]
          - table: gms_applicant_profile
            columns: [first_name, last_name, date_of_birth, phone, zip_code]
          - table: gms_project_details
            columns: [project_title, project_description, requested_amount, start_date]
    ```
11. THE system supports two deployment patterns for application data storage. The choice is
    made at implementation time and configured via the allowlist:
    - **Single-table (EAV) pattern**: All answers are stored in `gms_application_data` with one
      row per (application, question) pair. The `value_text` column holds the answer value.
      This is the simplest configuration and is suitable for programs with moderate data
      volumes.
    - **Multi-table (domain) pattern**: Answers are distributed across domain-specific tables
      (e.g. `gms_applicant_profile`, `gms_project_details`). Each table has one row per
      application and multiple typed columns. Questions target specific columns in specific
      tables. This pattern provides better query performance for reporting and avoids the
      EAV anti-pattern at scale.
    Both patterns may coexist — some questions can target `gms_application_data` while others
    target domain tables. The DynamicDataRepository handles both transparently.
12. WHEN an answer is submitted for a SELECT_MULTI question, THE ApplicationDataService SHALL
    store the selected values as a JSON array string in the target column. Example: if the
    applicant selects options "A", "B", and "C", the stored value SHALL be `["A","B","C"]`.
    This applies regardless of whether the single-table or multi-table pattern is used.
13. WHEN an answer is submitted for a SELECT_MULTI question with `isOtherOption` selected, THE
    ApplicationDataService SHALL store the value as a JSON array where the "Other" option's
    value is replaced by the applicant's free-text entry prefixed with `OTHER:`. Example:
    `["A","OTHER:Custom value entered by applicant"]`.
14. WHEN the ApplicationDataService reads back a SELECT_MULTI answer (for pre-population or
    post-save verification), it SHALL parse the JSON array string and return individual values
    to the Angular client as a string array in the AnswerDTO.
15. FOR all other question types (TEXT, TEXT_AREA, DATE, DECIMAL, WHOLE_NUMBER, CURRENCY, PHONE,
    ZIP_CODE, CHECKBOX, RADIO_YES_NO, SELECT_ONE, LABEL), THE ApplicationDataService SHALL
    store the answer as a single plain string value in the target column. No JSON wrapping is
    applied.

---

### Requirement 22: Error Handling and Custom Error Pages

**User Story:** As a system operator, I want the application to display a professional error page
for unhandled exceptions, so that users are not exposed to raw stack traces.

#### Acceptance Criteria

1. WHEN an application error occurs, THE Spring Boot application SHALL forward to `/error`.
2. THE AppErrorController SHALL serve `src/main/resources/static/error.html`.
3. THE error page SHALL display a professional error message with styling.
4. THE Spring Boot whitelabel error page SHALL be disabled via
   `server.error.whitelabel.enabled: false`.
5. THE error page SHALL be served for all unhandled exceptions and HTTP errors.
6. THE AppErrorController SHALL support extracting error status codes and messages from request
   attributes.

---

### Requirement 23: RadioYesNo Question Type

**User Story:** As an administrator, I want a dedicated Yes/No radio-button question type with
fixed options and exclusive parent-child support, so that I can build branching forms without
manually configuring options.

#### Acceptance Criteria

1. THE QuestionService SHALL support `RADIO_YES_NO` with exactly two fixed options: Yes
   (optionValue="Yes", displayOrder=1) and No (optionValue="No", displayOrder=2).
2. WHEN a `RADIO_YES_NO` question is created or updated, THE QuestionService SHALL auto-inject
   Yes/No QuestionOption entries and SHALL reject any caller-supplied options.
3. THE `RADIO_YES_NO` type SHALL require `targetTable` and `targetColumn`.
4. WHEN an answer is submitted for a `RADIO_YES_NO` question, THE ApplicationDataService SHALL
   validate the value is "Yes" or "No" (case-insensitive).
5. `RADIO_YES_NO` is the ONLY question type that may be a parent in a parent-child relationship.
   Attempts to add children to any other question type SHALL be rejected with a validation error.
6. WHEN linking a child to a `RADIO_YES_NO` parent, `triggerValue` MUST be "Yes" or "No"
   (case-insensitive); normalised to canonical casing on persist.
7. `RADIO_YES_NO` questions MAY be child questions. When linked as a child, a RADIO_YES_NO
   question retains its parent capability, enabling multi-level conditional branching. Circular
   references are still rejected.
8. WHEN rendering, child questions SHALL be nested in `childQuestions` with `triggerValue`.
9. Angular SHALL show children matching the selected value and hide all others.
10. THE Admin UI SHALL include `RADIO_YES_NO` in the type dropdown with an info banner explaining
    system-managed options.
11. THE QuestionFilter SHALL support filtering by `questionType = RADIO_YES_NO`.
12. WHEN the PageBuilderService renders a `RADIO_YES_NO` question, the Yes and No options SHALL
    always be present in the resolved options list regardless of any page or round override.

---

### Requirement 24: Date Question Type (DATE)

**User Story:** As an administrator, I want a dedicated date-picker question type, so that
applicants can enter dates that are validated against business rules configured per round.

#### Acceptance Criteria

1. THE QuestionService SHALL support `DATE` question type, rendered as `<input type="date">`.
2. DATE questions do NOT store min/max date constraints at the question level. Date range
   constraints are configured per (round, page, question) as `min_value` and `max_value` on the
   `RoundPageQuestion` record — see Requirement 8 for the round-level configuration and
   Requirement 9 AC 3 for runtime validation.
3. WHEN an answer is submitted for a DATE question, THE ApplicationDataService SHALL validate the
   value is a valid ISO-8601 date string. Invalid format SHALL produce a field-level error.
4. IF `min_value` is set on the RoundPageQuestion, THE ApplicationDataService SHALL reject
   answers before `min_value` with a field-level error.
5. IF `max_value` is set on the RoundPageQuestion, THE ApplicationDataService SHALL reject
   answers after `max_value` with a field-level error.
6. THE QuestionRenderDTO and ChildQuestionRenderDTO SHALL expose `minValue` and `maxValue`
   (sourced from RoundPageQuestion) so the Angular front end can set the `min` and `max`
   attributes on the date input.
7. DATE questions SHALL NOT use options. The QuestionService SHALL clear any options from DATE
   questions and SHALL NOT validate them as required.
8. DATE question answers are stored as ISO-8601 strings (yyyy-MM-dd) in the `value_text` column
   of `gms_application_data`.

---

### Requirement 25: Multi-Line Text Area Question Type (TEXT_AREA)

**User Story:** As an administrator, I want a multi-line text area question type distinct from the
single-line text input, so that applicants can enter narrative or paragraph responses.

#### Acceptance Criteria

1. THE QuestionService SHALL support `TEXT_AREA` question type, rendered as `<textarea>`.
2. `TEXT_AREA` questions SHALL behave identically to `TEXT` in terms of persistence and
   validation (optional `validationRegex`), with the distinction being purely in the rendered
   HTML control: `TEXT` renders as `<input type="text">` and `TEXT_AREA` renders as `<textarea>`.
3. WHEN a `validationRegex` is set on a TEXT_AREA question, THE ApplicationDataService SHALL
   evaluate it with DOTALL mode so that `.` matches newline characters in multi-line input.
4. TEXT_AREA questions SHALL NOT use options. The QuestionService SHALL clear any options.
5. TEXT_AREA questions require `targetTable` and `targetColumn` (same as TEXT).

---

### Requirement 26: Organization Management

**User Story:** As an administrator, I want to manage organizations and their types, so that users
can belong to organizations and applications can be raised on behalf of organizations.

#### Acceptance Criteria

1. WHEN an administrator creates an organization, THE OrganizationService SHALL persist an
   Organization entity with name, type, and optional description.
2. THE OrganizationService SHALL require `name` and `organizationType`; `name` is unique and at
   most 255 characters.
3. THE OrganizationService SHALL support organization types: NONPROFIT, GOVERNMENT, BUSINESS,
   EDUCATIONAL, OTHER. These same values are used in the `eligible_organization_types` column on
   `gms_program_round` to restrict which organization types may apply to a given round.
4. THE AdminController SHALL expose `POST /api/admin/organizations` (HTTP 201),
   `PUT /api/admin/organizations/{id}` (HTTP 200), `DELETE /api/admin/organizations/{id}`
   (HTTP 204), and `GET /api/admin/organizations` (HTTP 200).

---

### Requirement 27: Application Lifecycle

**User Story:** As an applicant, I want to create and manage my grant application for a specific
program round, so that I can track my submission and resume it across sessions.

#### Acceptance Criteria

1. WHEN an applicant initiates an application for a ProgramRound, THE ApplicationService SHALL
   create a new Application entity linked to the authenticated user's identity, the user's
   organization, and the specified ProgramRound, and return an ApplicationDTO containing the
   generated `applicationId`.
2. THE ApplicationService SHALL require that the ProgramRound is in ACTIVE status before an
   application can be created. IF the round is not ACTIVE, THE ApplicationService SHALL return
   a validation error.
3. THE ApplicationService SHALL require that the applicant's organization type is included in
   the ProgramRound's `eligibleOrganizationTypes` list before an application can be created.
   IF the organization is not eligible, THE ApplicationService SHALL return a validation error.
4. THE ApplicationService SHALL prevent an applicant from creating more than one Application
   per (user, ProgramRound) pair. IF a duplicate is attempted, THE ApplicationService SHALL
   return the existing ApplicationDTO rather than creating a new record.
5. AN Application SHALL carry the following attributes: `applicationId`, `programRoundId`,
   `organizationId`, `userId`, `status` (DRAFT, SUBMITTED, WITHDRAWN), and `createdAt`,
   `updatedAt` timestamps.
6. THE ApplicationService SHALL set the initial `status` of a new Application to DRAFT.
7. WHEN an applicant submits a completed application, THE ApplicationService SHALL transition
   the Application status from DRAFT to SUBMITTED. Answers may no longer be modified after
   submission unless an administrator explicitly reopens the application (see AC 13).
8. WHEN an applicant withdraws an application, THE ApplicationService SHALL transition the
   Application status from DRAFT or SUBMITTED to WITHDRAWN.
9. WHEN an applicant requests their application for a round, THE ApplicationService SHALL return
   the ApplicationDTO including current status and the `applicationId` needed for answer
   submission.
10. THE ApplicationDataController SHALL expose:
    - `POST /api/programs/{programId}/rounds/{roundId}/applications` -- create application,
      return HTTP 201 + ApplicationDTO.
    - `GET /api/programs/{programId}/rounds/{roundId}/applications/mine` -- get the
      authenticated user's application for this round, return HTTP 200 + ApplicationDTO or
      HTTP 404 if none exists.
    - `PUT /api/applications/{appId}/status` -- update application status (SUBMITTED,
      WITHDRAWN), return HTTP 200 + ApplicationDTO.
    - `POST /api/applications/{appId}/pages/{pageId}/answers` -- save answers for a page,
      return HTTP 200 + SaveResult.
    - `GET /api/applications/{appId}/pages/{pageId}/answers` -- retrieve saved answers for a
      page, return HTTP 200 + List<AnswerDTO>.
11. IF a request references an `applicationId` that does not belong to the authenticated user
    (and the user does not have the ADMIN role), THE ApplicationDataController SHALL return
    HTTP 403.
12. THE System SHALL persist Application entities in the `gms_application` table with columns:
    `id` (INTEGER, PK, auto-generated identity), `program_round_id` (FK to
    gms_program_round), `organization_id` (FK to gms_organization), `user_id` (VARCHAR 255,
    the Azure AD B2C subject claim), `status` (VARCHAR 20: DRAFT, SUBMITTED, WITHDRAWN),
    `eligibility_warning` (BOOLEAN, default false), `created_at`, `updated_at`. Unique
    constraint: (program_round_id, user_id). The `id` column is the primary key referenced
    as `appId` in API paths and as `applicationId` in DTOs.
13. THE AdminController SHALL expose:
    - `GET /api/admin/programs/{programId}/rounds/{roundId}/applications` -- list all
      applications for a round (paginated: `page`, `size`, `sort`). Supports filter parameters:
      `status` (DRAFT, SUBMITTED, WITHDRAWN), `eligibilityWarning` (boolean, filters to only
      flagged applications when true). Return HTTP 200.
    - `PUT /api/admin/applications/{appId}/reopen` -- transition a SUBMITTED application back
      to DRAFT, allowing the applicant to modify answers again. Return HTTP 200 +
      ApplicationDTO. Only users with the ADMIN role may call this endpoint. IF the application
      is not in SUBMITTED status, return HTTP 400.
14. WHEN an administrator reopens an application, THE ApplicationService SHALL transition the
    status from SUBMITTED to DRAFT and record the reopen event in the application's audit
    history.

---

### Requirement 28: Organization Eligibility Enforcement

**User Story:** As a system operator, I want program rounds to restrict applications to eligible
organization types, so that only qualifying organizations can apply to a given round.

#### Acceptance Criteria

1. WHEN an administrator configures a ProgramRound, THE ProgramRoundService SHALL accept an
   optional `eligibleOrganizationTypes` list (subset of: NONPROFIT, GOVERNMENT, BUSINESS,
   EDUCATIONAL, OTHER). An empty or absent list means all organization types are eligible.
   The list SHALL be stored as a comma-separated string in the `eligible_organization_types`
   column (e.g. "NONPROFIT,GOVERNMENT"). The ProgramRoundService SHALL parse this string on
   read and return it as a list in the ProgramRoundDTO.
2. WHEN an applicant attempts to create an Application for a ProgramRound with a non-empty
   `eligibleOrganizationTypes` list, THE ApplicationService SHALL verify that the applicant's
   organization type is in the list. IF not, THE ApplicationService SHALL return HTTP 403 with
   a message indicating the organization type is not eligible for this round.
3. WHEN the PageBuilderService returns the list of pages for a round via
   `GET /api/programs/{programId}/rounds/{roundId}/pages`, THE PageBuilderController SHALL
   first verify that the requesting user has an active Application for that round. IF no
   application exists, THE PageBuilderController SHALL return HTTP 403.
4. THE ProgramRoundDTO SHALL include the `eligibleOrganizationTypes` list so the Angular
   Applicant Module can display eligibility information before the applicant starts an
   application.
5. WHEN an administrator updates the `eligibleOrganizationTypes` on an ACTIVE ProgramRound,
   THE ProgramRoundService SHALL apply the change immediately. Existing Applications already
   in SUBMITTED status SHALL NOT be affected. DRAFT Applications from organizations that are
   no longer eligible SHALL have their `eligibility_warning` flag set to 1. The
   ApplicationDTO SHALL include this flag so the admin UI can surface a warning indicator.
   The applicant SHALL see a banner on their application page explaining that their
   organization type is no longer eligible and they should contact the program administrator.
6. THE admin round create/edit form SHALL include a multi-select control for
   `eligibleOrganizationTypes` showing all supported organization types. Leaving the selection
   empty SHALL mean all types are eligible, and the UI SHALL display "All organization types
   eligible" when nothing is selected.

---

### Requirement 29: File Attachment Upload, Storage, and Retrieval

**User Story:** As an applicant, I want to upload file attachments as part of my grant
application, so that I can provide supporting documents required by the program.

#### Acceptance Criteria

1. THE system SHALL provide a dedicated file upload endpoint separate from the answer submission
   endpoint:
   `POST /api/applications/{appId}/files` — accepts a single file as `multipart/form-data`
   with a `questionId` form field identifying which ATTACHMENT question the file belongs to.
   Returns HTTP 201 + FileReferenceDTO containing the generated `fileId` (UUID).
2. THE ApplicationDataService SHALL validate the uploaded file BEFORE storing it:
   - File extension MUST be in the question's `allowedFileTypes` list (case-insensitive match).
   - File size MUST NOT exceed the question's `maxFileSizeMb` value.
   IF validation fails, THE endpoint SHALL return HTTP 400 with a descriptive error and NOT
   store the file.
3. WHEN a file passes validation, THE FileStorageService SHALL store the file in Azure Blob
   Storage under a container named `gms-attachments`. The blob path SHALL follow the pattern:
   `{applicationId}/{questionId}/{fileId}.{extension}` to ensure uniqueness and enable
   per-application access scoping.
4. THE FileStorageService SHALL generate a UUID (`fileId`) for each uploaded file. This UUID
   is the sole reference used throughout the system — raw blob URLs are never exposed to
   clients.
5. AFTER successful storage, THE FileStorageService SHALL submit the file for asynchronous
   virus scanning via Azure Defender for Storage (or an equivalent scanning service). The file
   metadata record SHALL be created with `scanStatus = PENDING`.
6. WHEN the virus scan completes, THE system SHALL update the file metadata via the internal
   webhook endpoint (see Requirement 32):
   - If clean: set `scanStatus = CLEAN`.
   - If infected: set `scanStatus = INFECTED`, delete the blob from storage, and mark the
     file as unavailable.
7. THE AnswerDTO `fileReference` field SHALL contain the `fileId` (UUID string). When an
   applicant submits answers for a page, the answer value for an ATTACHMENT question SHALL be
   the `fileId` returned by the upload endpoint. THE ApplicationDataService SHALL validate
   that the referenced `fileId` exists, belongs to the same application, targets the same
   question, and has `scanStatus = CLEAN` before accepting the answer.
8. THE system SHALL provide a file download endpoint:
   `GET /api/applications/{appId}/files/{fileId}` — returns the file content with appropriate
   `Content-Type` and `Content-Disposition` headers.
9. THE file download endpoint SHALL verify:
   - The requesting user owns the application (or has ADMIN role).
   - The file's `scanStatus = CLEAN`.
   IF either check fails, return HTTP 403 or HTTP 404 respectively.
10. THE FileStorageService SHALL generate a short-lived SAS (Shared Access Signature) URL for
    the blob and redirect the client, rather than streaming the file through the application
    server. The SAS URL SHALL expire after 5 minutes and be scoped to read-only access on the
    specific blob.
11. THE system SHALL persist file metadata in the `gms_file_attachment` table:

    | Column | Type | Constraints |
    |---|---|---|
    | id | VARCHAR2(36) | PK; UUID |
    | application_id | NUMBER | FK → gms_application, NOT NULL |
    | question_id | NUMBER | FK → gms_question, NOT NULL |
    | original_filename | VARCHAR2(500) | NOT NULL |
    | file_extension | VARCHAR2(20) | NOT NULL |
    | file_size_bytes | NUMBER | NOT NULL |
    | blob_path | VARCHAR2(1000) | NOT NULL; Azure Blob path |
    | scan_status | VARCHAR2(20) | NOT NULL; values: PENDING, CLEAN, INFECTED |
    | uploaded_at | TIMESTAMP | NOT NULL |
    | scanned_at | TIMESTAMP | |

12. THE Admin Module's question maintenance screen SHALL display `allowedFileTypes` as a tag
    input and `maxFileSizeMb` as a number input when `questionType = ATTACHMENT`.
13. THE Angular Applicant Module's AttachmentQuestionComponent SHALL:
    - Display a file picker control restricted to the question's `allowedFileTypes`.
    - Show a progress indicator during upload.
    - Display the uploaded file name and a "Remove" button after successful upload.
    - Show a "Scanning..." indicator while `scanStatus = PENDING`.
    - Show an error if the scan result is INFECTED and prompt re-upload.
    - On form submission, include the `fileId` as the answer value for the ATTACHMENT question.
14. WHEN an applicant removes a file (before final submission), THE system SHALL:
    - Delete the blob from Azure Blob Storage.
    - Delete the `gms_file_attachment` metadata record.
    - Clear the `fileId` from the answer.
    Endpoint: `DELETE /api/applications/{appId}/files/{fileId}` — returns HTTP 204.
15. THE ReadonlyQuestionComponent SHALL render ATTACHMENT answers as the original filename
    (non-clickable text label) when in readonly mode. Admins viewing submitted applications
    SHALL see the filename as a download link.

---

### Requirement 31: User Profile Endpoint

**User Story:** As an authenticated user, I want a lightweight endpoint that returns my identity,
roles, and organization membership, so that the Angular front end can determine my access level
without relying on indirect heuristics.

#### Acceptance Criteria

1. THE system SHALL provide a `GET /api/me` endpoint that returns the authenticated user's
   profile information as a JSON object containing `userId` (string), `roles` (string array),
   and `organizationId` (string or empty string if not assigned).
2. THE `/api/me` endpoint SHALL require authentication. Unauthenticated requests SHALL receive
   HTTP 401 (handled by the Spring Security filter chain before reaching the controller).
3. THE `/api/me` endpoint SHALL be accessible to any authenticated user regardless of role
   (ADMIN, APPLICANT, or no roles).
4. THE UserProfileController SHALL extract the user's identity from the SecurityContextProvider
   and return the UserPrincipal fields without exposing sensitive token details.
5. THE Angular AuthService SHALL use the `/api/me` endpoint to determine the current user's
   roles and organization context on application initialization, replacing any indirect role
   detection mechanisms.

---

### Requirement 32: File Scan Webhook Endpoint

**User Story:** As a system integrator, I want a dedicated internal webhook endpoint for the
virus scanning service to report scan results, so that file scan status is updated asynchronously
without polling.

#### Acceptance Criteria

1. THE system SHALL provide a `POST /api/internal/scan-results` endpoint that accepts scan
   result notifications from the virus scanning service (Azure Defender for Storage or
   equivalent).
2. THE endpoint SHALL accept a JSON request body with fields: `fileId` (UUID string identifying
   the scanned file) and `scanStatus` (string: "CLEAN" or "INFECTED").
3. IF `fileId` or `scanStatus` is missing from the request body, THE endpoint SHALL return
   HTTP 400.
4. IF `scanStatus` is not one of "CLEAN" or "INFECTED", THE endpoint SHALL return HTTP 400.
5. WHEN a valid scan result is received, THE FileStorageService SHALL update the corresponding
   `gms_file_attachment` record's `scan_status` and `scanned_at` fields. If the status is
   INFECTED, the blob SHALL be deleted from storage.
6. THE `/api/internal/**` path pattern SHALL be accessible without authentication (permitAll)
   to allow the external scanning service to deliver results. In production, this endpoint
   SHOULD be secured via network-level controls (e.g., Azure Event Grid validation, IP
   allowlisting, or a shared secret header) rather than JWT authentication.
7. THE endpoint SHALL return HTTP 200 on successful processing.
8. Files that remain in `PENDING` scan status for longer than a configurable timeout (default:
   24 hours) SHALL be considered scan failures. The system SHOULD provide an admin-visible
   mechanism (e.g., a query or endpoint) to identify stale uploads that have not received a
   scan callback, so operators can investigate misconfigured or unreachable scan webhooks.
   Note: automatic timeout enforcement is not currently implemented — this AC documents the
   intended future behaviour.

---

### Requirement 33: API Documentation via OpenAPI (Swagger UI)

**User Story:** As a developer or integrator, I want interactive API documentation available at
a well-known URL, so that I can explore and test the system's REST endpoints without reading
source code.

#### Acceptance Criteria

1. THE system SHALL expose OpenAPI 3.0 specification at `/v3/api-docs/**` and interactive
   Swagger UI at `/swagger-ui/**` and `/swagger-ui.html`.
2. THE `/v3/api-docs/**`, `/swagger-ui/**`, and `/swagger-ui.html` paths SHALL be accessible
   without authentication (permitAll) in both `b2c` and `local` authentication modes.
3. THE OpenAPI specification SHALL be auto-generated from the Spring Boot controllers using
   the `springdoc-openapi-starter` library. No manual specification maintenance is required.
4. THE Swagger UI SHALL list all public and admin REST endpoints with their request/response
   schemas, enabling developers to test API calls directly from the browser.

---
