# Grant Management Base Framework — Functional Requirements

## Overview

The Grant Management Base Framework is a configurable, metadata-driven platform that allows
administrators to define form questions, assemble them into pages, and bind those pages to grant
program rounds — all without writing code. The system dynamically renders forms to applicants
based on their role and the program round they are applying to, and saves their answers
atomically.

A **Program** is the top-level grant initiative, carrying strategic attributes such as a goal
statement and total budget. A **Program Round** is a time-bounded funding cycle within a Program,
carrying the date range, funds limit, and the set of form pages that applicants complete. This
two-level hierarchy allows a single grant program to run multiple rounds (e.g., Round 1, Round 2)
while sharing the same high-level program identity.

---

## Domain Concepts

### Programs and Rounds

A **Program** represents a named grant initiative. It has a goal, a total budget, and an optional
description. Programs group one or more funding rounds together under a single identity.

A **Program Round** is the operational unit that applicants interact with. It has a name, a start
and end date, an optional funds limit, and a status that progresses through a defined lifecycle.
Rounds belong to exactly one Program.

### Pages and Questions

A **Page** is a reusable form template that can be assigned to multiple program rounds. Each
round that uses the same page template gets its own independent set of questions — changes to one
round's page configuration do not affect any other round.

A **Question** is a reusable form field definition. Questions have a type (e.g., text, date,
file upload, yes/no), a label, and optional validation rules. Questions are defined once and can
be reused across many pages and rounds.

### Overrides

Administrators can customise how questions and pages appear within a specific round without
changing the shared definitions. A question's label and required flag can be overridden at two
levels within the same configuration record: a page-level override that applies whenever the
question appears on that page in any round, and a round-level override that applies only within
a specific round. The round-level override takes highest precedence, followed by the page-level
override, then the question's default. A question can also be excluded entirely from a specific
round.

Similarly, a page's display name and description can be overridden per round, so applicants see
round-relevant titles without altering the shared page template.

### Organizations

An **Organization** represents an entity (nonprofit, government body, business, etc.) on whose
behalf a grant application is submitted. Users belong to organizations.

---

## Functional Requirements

### 1. Question Library Management

Administrators maintain a reusable library of question definitions. Each question has a type,
a label, and — for most types — a target data location where the answer will be stored.

- Administrators can create, update, and deactivate questions.
- Deactivated questions are hidden from lists and cannot be added to new pages.
- Questions require a type and a label. Most types also require a target table and column.
- File upload questions additionally require a list of allowed file extensions and a maximum file
  size (1–100 MB).
- File attachments are uploaded separately from the answer submission. The applicant uploads a
  file first, receives a file reference, and then includes that reference as the answer value
  when submitting the page. Files are stored securely in cloud storage and scanned for viruses
  before being accepted. Infected files are automatically removed and the applicant is prompted
  to re-upload.
- Select questions (single-choice and multi-choice) require at least one option and support up
  to 200 options.
- The Yes/No question type has exactly two fixed options (Yes and No) that are managed by the
  system; administrators cannot supply custom options for this type.
- Date questions support minimum and maximum date constraints that are configured per program round — not on the question definition itself — allowing different rounds to enforce different date ranges on the same shared question.
- Multi-line text questions behave identically to single-line text questions in terms of
  validation, differing only in how they are rendered.

### 2. Question Options

For single-choice and multi-choice questions, administrators manage the list of selectable
options. Each option has a display label, a stored value, and a display order.

- Option values must be unique within a question.
- When two options share the same display order, they are sorted by their internal identifier.
- Multi-choice questions support an "Other" option that, when selected, reveals a free-text
  field for the applicant to enter a custom value.
- Options can optionally route their answer to a different data location than the question's
  default. If this per-option routing is configured, all three routing fields (target table,
  target column, and lookup value) must be provided together; partial configuration is rejected.

### 3. Conditional (Parent-Child) Questions

Administrators can define child questions that appear only when a parent question has a specific
answer. This allows forms to branch based on applicant responses.

- Only Yes/No questions may be parents. Attempting to make any other question type a parent is
  rejected.
- A parent question can have up to 50 child questions.
- Each child question specifies the parent answer value that triggers its display ("Yes" or "No").
- Yes/No questions can themselves be child questions, enabling multi-level branching. A child
  Yes/No question retains its parent capability and can trigger its own children, allowing
  forms to branch multiple levels deep.
- Circular relationships (A is parent of B, B is parent of A) are detected and rejected.
- When a question is deactivated, all its parent and child links are removed.

### 4. Page Management

Pages are reusable form templates. Administrators create pages with a name and optional
description, then assign questions to them within the context of a specific program round.

- Each (round, page) pair has its own independent list of questions. Assigning a question to a
  page in one round does not affect any other round using the same page template.
- Questions on a page have a display order. When a conflict arises, the system resequences all
  questions on that page for that round.
- A question can only be assigned once per (round, page) pair.
- Removing a question from a page within a round deletes only the mapping between that question
  and that (round, page) context. The question itself remains active and available for use in
  other pages and rounds.

### 5. Role-Based Question Visibility

Administrators can restrict which questions are visible to specific user roles on a page.

- A question with no role restrictions is visible to all users.
- A question with role restrictions is only shown to users who have at least one of the
  specified roles.
- Users with no assigned roles only see questions that have no role restrictions.
- When an administrator updates the role visibility list on an existing question-to-page assignment, the system replaces all existing role entries with the new list. Supplying an empty list removes all restrictions, making the question visible to all authenticated users.

### 6. Program Management

Administrators create and manage top-level grant programs.

- A program has a name (required), an optional description, an optional goal statement, and an
  optional total budget.
- Programs start in an Active state. They can be archived, after which no new rounds can be
  created under them.
- Administrators can view a list of all programs and drill into a program to see its rounds.

### 7. Program Round Management

Administrators create and manage funding rounds within a program.

- A round has a name, a start date, an end date, and an optional funds limit.
- Rounds start in Draft status and progress through: Draft → Active → Closed → Archived.
  No other transitions are permitted.
- The archive operation (via the delete endpoint) directly sets a round to Archived status from any non-Archived state, bypassing the sequential lifecycle transitions. This ensures rounds can always be cleaned up regardless of their current status.
- Administrators assign pages to a round in a defined display order.
- Removing a page from a round deletes only the mapping between that page and the round (along
  with all question assignments scoped to that round-page pair). The page template itself is not
  deleted and remains available for assignment to other rounds.
- A round cannot be created under an archived program.
- Administrators can retrieve a single round by ID to view its full details including assigned pages and status history.

### 8. Round-Level Question Overrides

Administrators can tailor how questions appear within a specific round without modifying the
shared question or page definitions. All configuration for a question within a (round, page)
context is stored in a single record.

- A question's label can be overridden at the page level (applies to that page across all rounds)
  or at the round level (applies only within this specific round). Both overrides are stored on
  the same configuration record.
- A question's required flag can similarly be overridden at the page level or the round level.
- A question can be excluded entirely from a specific round. Excluded questions do not appear
  in the rendered form for that round.
- The override resolution order is: round-level override (highest) → page-level override →
  question default (lowest). A missing override at any level is simply skipped.
- Overrides can only be applied to questions that are already assigned to the specified
  round-page combination.
- Administrators can retrieve the full configuration for any question in a (round, page) context,
  including display order, both levels of overrides, exclusion flag, and role visibility.

### 9. Round-Scoped Page Name and Description Overrides

Administrators can customise the display name and description of a page within a specific round.

- A page name override (up to 200 characters) replaces the canonical page name for applicants
  in that round only.
- A page description override (up to 1000 characters) replaces the canonical description.
- Setting an override to blank or null clears it, reverting to the canonical value.
- The resolved name (override if set, otherwise canonical) is what applicants see and what
  appears in page lists.
- Removing a page from a round also removes all its round-scoped overrides and question
  assignments for that round. The page template itself is unaffected.

### 10. Dynamic Form Rendering for Applicants

When an applicant opens a form page, the system assembles a fully resolved page definition
tailored to their program round and role.

- The system returns the resolved page name, description, and an ordered list of questions with
  all overrides applied.
- Questions excluded from the round or not visible to the applicant's role are omitted.
- Child questions are nested under their parent question, annotated with the trigger value that
  activates them.
- The page list for a round only shows pages that have at least one question visible to the
  applicant's role.
- The applicant-facing page endpoint only works when the round is in Active status.
- The admin preview endpoint shows all questions regardless of role restrictions, giving administrators a complete view of the full applicant experience.

### 11. Answer Validation

Before saving, the system validates each submitted answer against the rules for its question type.

- Required questions reject blank or empty answers.
- Text and multi-line text questions can have a regex pattern that the answer must match. If minimum and/or maximum character lengths are configured for the question within that round, the text length must fall within those bounds.
- Date answers must be valid dates (yyyy-MM-dd) and, if min/max dates are set, must fall within
  that range.
- Numeric types (decimal, whole number, currency) validate that the value is parseable in the
  expected format. Additionally, if minimum and/or maximum values are configured for the question within that round, the numeric value must fall within that range (inclusive).
- Phone numbers must match a standard format; ZIP codes must be 5-digit or ZIP+4 format.
- File upload answers are validated against the allowed file types and maximum file size. The
  file must have been uploaded and virus-scanned before the answer can be submitted. Files
  flagged as infected are rejected.
- Single-choice and multi-choice answers must be one of the question's defined option values.
- Multi-choice questions with an "Other" option selected require a non-empty free-text value.
- Checkbox answers must be "true" or "false".
- Label questions accept and ignore any submitted value.
- Answers referencing unknown or inactive questions produce a field-level error.

### 12. Atomic Form Submission

Form submissions are saved atomically — either all answers are persisted or none are.

- All save operations run within a single transaction.
- After persisting all answers, the system re-reads each saved value and verifies it matches
  what was submitted. A mismatch triggers a rollback.
- If any validation error exists, the entire submission is rolled back.
- The system returns a result indicating success or failure, with field-level error details on
  failure.
- Previously saved answers for a page can be retrieved at any time.
- Multi-choice answers (where multiple options are selected) are stored as a JSON array string
  in a single column. When the "Other" free-text option is selected, the custom text is stored
  with a distinguishing prefix so it can be separated from standard option values on read-back.
  All other question types store a single plain string value.

### 13. Admin Portal and Authentication

Administrators access the system through a dedicated admin portal.

- The portal provides maintenance screens for questions, pages, programs, program rounds, and
  organizations.
- Access to the admin portal and all admin API endpoints is restricted to users with the ADMIN
  role.
- Unauthenticated requests are rejected; authenticated non-admin requests are rejected.
- The system supports two authentication modes, configurable per environment:
  - **Azure AD B2C (production)**: Users are redirected to Azure AD B2C for sign-in. No login
    screen is rendered by the application. Tokens are managed by MSAL.
  - **Local (development/testing)**: The application displays a login screen where users enter
    a username and password. Credentials are validated against a local users table with
    encrypted passwords. A "Sign Out" button in the navigation bar ends the session.
- In local mode, two seed accounts are provided out of the box: an administrator account and
  an applicant account, both with a default password. The login screen shows these credentials
  as a development convenience hint.
- In local mode, credentials are stored in the browser session and sent as HTTP Basic authentication on every subsequent API request via an interceptor. The frontend calls a profile endpoint to obtain the authenticated user's roles, which determine whether admin or applicant navigation is shown.

### 14. Admin Screens

#### Question Maintenance
- A searchable, paginated question list supports filtering by type and free-text search on label.
- The create/edit form shows a type dropdown first; changing the type immediately shows or hides
  relevant sections without a page reload.
- Common fields (label, target table, target column, required) are always visible. Type-specific
  sections appear only when relevant: options for select types, file constraints for attachments,
  a regex field with a live test input for text types.
- The options section for select types supports drag-and-drop reordering. An "Advanced Routing"
  panel per option reveals the optional per-option target routing fields, keeping the default
  view clean.
- The Yes/No type shows an informational banner explaining that its options are system-managed,
  and a child questions section where administrators can link child questions, set the trigger
  value ("Yes" or "No") for each, set the child display order, and remove existing links. A
  count badge shows how many of the 50-child limit are used.
- Validation errors appear inline next to the relevant fields. A sticky save bar with Save and
  Cancel buttons is always visible without scrolling.
- Deactivated questions are hidden by default with a "Show inactive" toggle to reveal them.

#### Page Maintenance
- A searchable page list shows name, description (truncated), and status. A "Used in Rounds"
  section on each page shows which rounds currently use it, so administrators understand the
  impact before editing.
- The page form has a live character counter on the name field and shows inline errors before
  the save request is made.
- Question assignment within a round context uses a drag-and-drop ordered list. An "Add
  Question" button opens a search-and-select dialog filtered to questions not already assigned.
- Each assigned question row shows the resolved label (with visual distinction when an override
  is active), a type badge, and an expand toggle to reveal the full configuration. An Edit icon
  opens an inline configuration form; a Remove icon prompts a confirmation before deleting the
  mapping.
- When a Yes/No question is assigned, its child questions appear indented beneath it with a
  "Yes →" or "No →" trigger label. A tooltip explains that child links are managed on the
  Question maintenance screen.
- Drag-and-drop reordering persists immediately on drop.

#### Program and Round Maintenance
- The program list shows name, status, goal (truncated), total budget, and round count. Archive
  requires a confirmation dialog.
- The program detail view shows program attributes at the top and a Rounds tab below. The round
  list shows name, status, dates, funds limit, and page count.
- The round detail view uses three tabs — Pages, Settings, and Preview — keeping all round
  configuration in one place without navigating away.
- The Pages tab shows assigned pages as a drag-and-drop ordered list. Each page row shows the
  resolved name (with a tooltip showing the canonical name when an override is active), a
  question count badge, and action buttons for Configure, Override Name, and Remove.
- "Override Name" opens an inline panel with a side-by-side comparison of the canonical name
  and the current override, with a "Clear Override" button to revert.
- "Configure" expands an inline question configuration panel for that (round, page) pair,
  identical in structure to the Page Maintenance question panel.
- Each question row in the round-page panel shows "Page" and "Round" override badges so
  administrators can see at a glance which overrides are active. The edit form has clearly
  labelled sections for page-level overrides, round-level overrides, exclusion, and role
  visibility, with a helper note explaining the resolution order.
- For DATE questions, the configuration panel also displays optional minimum date and maximum date pickers (stored per round-page-question), with client-side validation that minimum is not after maximum.
- For each question, a "Visibility Rules" section allows administrators to add conditional
  display rules. Each rule specifies a trigger question, an operator, and a comparison value.
  Rules determine whether the question is shown or hidden to applicants based on other answers.
- A "Formula" field allows administrators to define calculated field expressions using question
  ID references and arithmetic operators (e.g., `q_5 + q_6 * 2`).
- A "Page Rules" section at the bottom of the configuration page allows administrators to
  define cross-field validation rules with conditions and error messages that appear when
  conditions are not met during submission.
- Yes/No questions show their child questions indented with trigger labels. A collapse toggle
  hides children to reduce visual clutter when not needed.
- The Settings tab shows the current round status and a transition button labelled for the next
  valid step (e.g., "Activate Round"). The button is disabled with a tooltip when no transition
  is available. A status history log shows when each transition occurred.
- The Preview tab renders the live form for each page in the round, with a page selector
  dropdown. The preview shows exactly what applicants will see.
- Removing a page from a round shows a confirmation dialog explaining that the page template
  and questions are not deleted, only the round assignment.
- All save operations show inline success notifications. Failures show field-level errors inline
  without losing unsaved changes in other sections.

### 15. Applicant Form Rendering

Applicants fill out forms through a dynamic interface.

- The applicant's entry point is an Application Dashboard page that shows the program name,
  round name, application status, and a list of form pages with completion indicators. From
  here the applicant navigates into individual pages to fill in answers.
- Before rendering a form page, the system ensures an application exists for the current round. If no application exists, one is created automatically. The resulting application ID is used in all subsequent answer submissions and file uploads for that round.
- If the applicant's organization becomes ineligible after a round configuration change, a
  prominent warning banner appears on the dashboard and all form pages. The form becomes
  read-only and the Submit button is disabled until the issue is resolved by an administrator.
- Each question type renders as the appropriate control: single-line text, multi-line textarea,
  date picker, radio buttons, checkboxes, file upload, or read-only label.
- Date inputs have min/max attributes set from the question's constraints.
- Child questions appear immediately below their parent when the trigger value is selected,
  and are hidden otherwise. When hidden, any previously entered values are cleared so they
  are not submitted.
- Questions are laid out in a 12-column grid. Administrators configure how many columns each
  question occupies, allowing questions to sit side-by-side or span the full width as needed.
- A question can be configured as read-only, in which case it displays the saved answer as
  plain text with no input control. Read-only questions are not included in the submission.
- The label for each question can be positioned above the input, to the left of it, or hidden
  (still accessible to screen readers).
- Optional help text can be configured per question and appears below the input to guide
  applicants.
- Related questions can be grouped into named sections with a visible section heading, making
  long forms easier to navigate.
- On submission, field-level errors are displayed inline below each input. On success, the
  applicant is navigated to the next page or shown a completion message.

### 16. Organization Management

Administrators manage the organizations that users belong to.

- Organizations have a name (unique, up to 255 characters), a type, and an optional description.
- Supported organization types are: Nonprofit, Government, Business, Educational, and Other.
- These same types are used to restrict which organizations are eligible to apply to a given
  program round.
- Administrators can create, update, deactivate, and list organizations.

### 17. Application Lifecycle

Applicants create and manage their grant applications through a defined lifecycle.

- An applicant starts an application for a specific program round. The system creates one
  application per applicant per round — attempting to start a second application for the same
  round returns the existing one.
- An application can only be created when the round is Active and the applicant's organization
  type is eligible for that round.
- Applications start in Draft status. The applicant fills in form pages and saves answers
  progressively. Answers can be updated at any time while the application is in Draft.
- When ready, the applicant submits the application, transitioning it to Submitted status.
  Submitted applications cannot be modified unless explicitly reopened by an administrator.
- An applicant can withdraw a Draft or Submitted application, transitioning it to Withdrawn.
- The applicant can retrieve their current application and all previously saved answers at
  any time.
- If no application exists for the authenticated user and the specified round, the system returns a not-found response. The frontend handles this by creating an application automatically before loading the page list.

### 18. Organization Eligibility Enforcement

Program rounds can restrict which organization types are eligible to apply.

- When configuring a round, administrators can select one or more eligible organization types.
  Leaving the selection empty means all organization types are eligible.
- When an applicant attempts to start an application, the system checks their organization's
  type against the round's eligibility list. Ineligible organizations receive a clear error
  message explaining why they cannot apply.
- The round's eligibility information is visible to applicants before they start an application
  so they can determine eligibility upfront.
- If eligibility rules are tightened on an active round, existing submitted applications are
  not affected. Draft applications from newly ineligible organizations are flagged with a
  warning but not automatically withdrawn.

### 19. Page Lifecycle

Pages are reusable templates that can be deactivated when no longer needed.

- Administrators can deactivate a page, which hides it from default lists and prevents it from
  being assigned to new rounds.
- A page cannot be deactivated while it is assigned to any active program round. The system
  returns a clear error listing the affected rounds.
- Deactivated pages can be revealed in the admin list via a "Show inactive" toggle.
- Administrators can retrieve a single page by ID to view its full details and see which rounds currently use it.

### 20. Error Handling

When an unhandled error occurs, the system displays a professional error page rather than
exposing raw technical details to the user. The error page is served for all unhandled exceptions
and HTTP errors.

### 21. Bulk Round Setup

Administrators can create a fully configured program round in a single operation, useful for
quickly bootstrapping a round with pages and questions from an external definition.

- The request specifies a program ID, round name, and a list of pages each containing a list
  of questions.
- The system creates a new program round with the specified name, a start date of today, an
  end date three months from today, and a funds limit of $1,000.
- Each page in the request is created as a new page and assigned to the round in the order
  provided. If an active page with the same name already exists, it is reused rather than
  creating a duplicate.
- Each question in a page is created as a new question and assigned to the round-page in order.
  If an active question with the same label and type already exists in the question bank, it is
  reused rather than creating a duplicate.
- Questions include a label, type, required flag, and optional validation regex, min value,
  and max value.
- All questions use the default target table (`gms_application_data`) and target column
  (`value_text`).
- For select-type questions (SELECT_ONE, SELECT_MULTI), options are provided in the request
  with a label and value for each.
- RADIO_YES_NO questions without explicit options automatically receive the standard Yes/No
  option pair.
- The round is created in DRAFT status. The administrator can activate it separately when ready.

### 21. File Attachment Handling

Applicants can upload file attachments as part of their grant application.

- Files are uploaded through a dedicated upload endpoint, separate from the answer submission.
  This allows large files to be uploaded independently and provides immediate feedback on
  validation failures.
- Each uploaded file is validated against the question's allowed file types and maximum size
  before being stored. Invalid files are rejected immediately.
- After upload, files are automatically scanned for viruses. While scanning is in progress,
  the file shows a "Scanning..." status. If a virus is detected, the file is removed and the
  applicant is notified to upload a clean file.
- The answer for a file upload question is a reference to the uploaded file (not the file
  content itself). This reference is validated at submission time to ensure the file exists,
  belongs to the correct application and question, and has passed the virus scan.
- Files are stored securely in cloud storage. Downloads are served via short-lived signed URLs
  that expire after 5 minutes, ensuring files cannot be accessed without proper authorization.
- Only the applicant who owns the application (or an administrator) can download attached files.
- Applicants can remove an uploaded file before final submission, which deletes both the stored
  file and its metadata.
- In read-only mode, file attachments display the original filename as text. Administrators
  viewing submitted applications see the filename as a download link.

### 22. Admin Application Management

Administrators can view and manage applications submitted to program rounds.

- A filterable, paginated list shows all applications for a given round, with filtering by
  status (Draft, Submitted, Withdrawn) and by eligibility warning flag.
- Each entry displays the application ID, user, status, warning indicator, and creation date.
- Administrators can reopen a submitted application, transitioning it back to Draft so the
  applicant can make changes.

### 23. Navigation Structure

- Unauthenticated users are directed to a login screen (local mode) or Azure AD B2C (production).
- After authentication, administrators land on the program list as their default view.
- After authentication, applicants land on the applicant home page showing available programs
  and active rounds.
- The admin portal provides navigation to programs, rounds, questions, pages, and organizations.
- Applicants see a "Programs" link in the navigation bar leading to their home page, from which
  they select a program round, view the round's pages, and fill in forms.
- Any unrecognized route redirects to the login screen.

### 24. Applicant Home Page

Non-admin users see a landing page that lists all active programs with their currently active
rounds.

- The page displays each active program's name and description.
- Under each program, active rounds are shown as clickable links with the round name and
  date range.
- Only programs that have at least one active round are displayed. Programs with no active
  rounds are hidden.
- Clicking a round link navigates the applicant to the round's application dashboard, where
  they see the list of form pages and can begin or resume their application.

### 25. Visibility Rules (Conditional Question Display)

Questions can be configured to appear or hide based on the values of other questions on the
same page.

- A visibility rule specifies a trigger question, an operator, and an optional comparison value.
- Supported operators: IS_NOT_EMPTY, IS_EMPTY, EQUALS, NOT_EQUALS, LESS_THAN, GREATER_THAN,
  CONTAINS.
- Multiple visibility rules on a single question are combined with AND or OR logic.
- When rules evaluate to false, the question is hidden from the applicant and its value is
  excluded from submission.
- Visibility rules are evaluated client-side in real-time as the applicant fills in the form.
  The `VisibilityRuleEvaluatorService` subscribes to form value changes and re-evaluates all
  rules on every change, supporting AND/OR logic across multiple rules per question. When a
  trigger question's control has no matching form key, the rule evaluates to false (hide).
- On submission, the server re-evaluates visibility rules and skips validation for questions
  whose rules evaluate to false, ensuring hidden fields don't block submission.
- Visibility rules are configured per (round, page, question) — different rounds can have
  different conditional logic for the same shared question.

### 26. Page Rules (Cross-Field Validation)

Pages can have rules that validate relationships between multiple fields before submission.

- A page rule specifies one or more conditions (each referencing a question, operator, and
  value) combined with AND or OR logic.
- If a page rule's conditions are not met at submission time, the system returns a page-level
  error with the configured error message.
- Page rules are evaluated server-side during answer submission, after individual field
  validation passes.
- The Angular applicant module displays page-level errors as banners above the form, distinct
  from field-level inline errors.
- Page rules are configured per (round, page) — different rounds can have different cross-field
  validation for the same shared page.

### 27. Calculated Fields

Questions can be configured as calculated fields whose values are derived from formulas
referencing other questions on the same page.

- A calculated question has a formula expression stored as a string (e.g., field references
  and arithmetic operators).
- The Angular applicant module evaluates the formula client-side and displays the computed
  value as a read-only field. The `FormulaEvaluatorService` uses a safe recursive descent
  parser (no eval) supporting +, -, *, /, parentheses, and field references (`q_<questionId>`).
  Division by zero returns null. Missing field values default to 0. Results are rounded to
  2 decimal places for non-integer values.
- Calculated fields are not editable by applicants and are not included in the answer
  submission payload.
- The server computes and stores the calculated value during answer save for reporting purposes.
- Supported formula operations: addition, subtraction, multiplication, division, and field
  references by question ID.

### 28. External Form Import

The system supports importing form definitions from external form builder exports.

- The import endpoint accepts a JSON structure with sections (mapped to pages) and fields
  (mapped to questions), including visibility rules, page rules, and options.
- Each imported field's external UUID is stored on the question record for future reference
  and re-import capability.
- Type mapping converts external types (text, textarea, number, email, date, select, radio,
  checkbox) to GMS question types.
- Validation constraints (min, max, minLength, maxLength) are mapped to the appropriate
  round-page-question min/max values.
- Visibility rules referencing external field IDs are resolved to GMS question IDs using the
  mapping built during import.
- Page rules are similarly resolved and stored.
- If no program ID is specified, the import defaults to program ID 1. Round names are
  auto-generated with a timestamp.
