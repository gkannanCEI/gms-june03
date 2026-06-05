# GMS Database Schema and Architecture

This document outlines the complete database schema for the Grant Management System (GMS). The application primarily uses PostgreSQL, and the schema relies on relational models combined with Entity-Attribute-Value (EAV) patterns for dynamic form capabilities.

## 1. Architectural Overview

The GMS application enables administrators to create programs, configure multiple funding rounds, and assign sets of pages with dynamically rendered questions to those rounds. The dynamic data entered by applicants is either stored in a central EAV table or routed to predefined, explicit domain tables based on question configuration.

## 2. Core Tables

### 2.1. Program & Round Configurations

*   **`gms_program`**: Represents an overarching funding program.
    *   `id` (BIGINT, PK): Unique identifier.
    *   `program_name` (VARCHAR): Name of the program.
    *   `description` / `goal` (VARCHAR): Text details.
    *   `total_budget` (NUMERIC): Overall budget pool.
    *   `status` (VARCHAR): Current status.

*   **`gms_program_round`**: Specific application cycles under a program.
    *   `id` (BIGINT, PK): Unique identifier.
    *   `program_id` (BIGINT, FK): Links to `gms_program`.
    *   `round_name` (VARCHAR): Identifies the round.
    *   `start_date` / `end_date` (DATE): Open and close dates.
    *   `funds_limit` (NUMERIC): Budget constraint for this round.
    *   `status` (VARCHAR): Active/Inactive.
    *   `eligible_organization_types` (VARCHAR): Filters for applicants.

### 2.2. Page & Question Meta-Data

These tables define the dynamic form structure.

*   **`gms_page`**: Reusable page definitions.
    *   `id` (BIGINT, PK): Unique identifier.
    *   `page_name` (VARCHAR): Logical name of the page.
    *   `page_description` (VARCHAR): Display text.
    *   `active` (BOOLEAN): If the page is active.

*   **`gms_question`**: Reusable question definitions.
    *   `id` (BIGINT, PK): Unique identifier.
    *   `question_type` (VARCHAR): Data type/UI control (e.g., `TEXT`, `NUMBER`, `RADIO_YES_NO`, `SELECT_ONE`).
    *   `label` (VARCHAR): Primary question text.
    *   `target_table` (VARCHAR): Target domain table where data is stored (can be `NULL` for EAV storage).
    *   `target_column` (VARCHAR): Target column in the domain table.
    *   `required` (BOOLEAN): Baseline requirement status.
    *   `validation_regex` (VARCHAR): Rule for front/backend validation.
    *   `allowed_file_types` / `max_file_size_mb`: File upload configurations.
    *   `parent_question_id` (BIGINT, FK): Allows nesting of questions.
    *   `external_id` (VARCHAR): Identifier used for import/integration.

*   **`gms_question_option`**: Lookup values for selection-based questions.
    *   `id` (BIGINT, PK): Unique identifier.
    *   `question_id` (BIGINT, FK): Links to `gms_question`.
    *   `option_label` (VARCHAR): Text displayed to the user.
    *   `option_value` (VARCHAR): Value stored in DB.
    *   `display_order` (INTEGER): Order in UI list.

### 2.3. Linking Configuration (Round -> Page -> Question)

*   **`gms_round_page`**: Assigns pages to a specific round.
    *   `id` (BIGINT, PK): Unique identifier.
    *   `program_round_id` (BIGINT, FK): Links to `gms_program_round`.
    *   `page_id` (BIGINT, FK): Links to `gms_page`.
    *   `display_order` (INTEGER): Sequence of pages.

*   **`gms_round_page_question`**: Assigns questions to a round-page with specific override capabilities.
    *   `id` (BIGINT, PK): Unique identifier.
    *   `round_page_id` (BIGINT, FK): Links to `gms_round_page`.
    *   `question_id` (BIGINT, FK): Links to `gms_question`.
    *   `display_order` (INTEGER): Order within the page.
    *   `label_override` / `required_override` (VARCHAR / BOOLEAN): Overrides baseline question configs for this specific page mapping.
    *   `readonly` / `excluded` (BOOLEAN): Layout/behavior flags.
    *   `column_span` (INTEGER): CSS Grid sizing.
    *   `min_value` / `max_value` (VARCHAR): Constraints depending on question type.
    *   `formula` (VARCHAR): Calculation script/formula for calculated fields.

*   **`gms_round_page_question_role`**: Defines RBAC (Role-Based Access Control) for specific questions on a page.
    *   `id` (BIGINT, PK)
    *   `round_page_question_id` (BIGINT, FK)
    *   `role_name` (VARCHAR)

## 3. Dynamic Logic & Rules

*   **`gms_visibility_rule`**: Conditional show/hide logic based on answers to other questions.
    *   `round_page_question_id` (BIGINT, FK): The question being hidden/shown.
    *   `trigger_question_id` (BIGINT, FK): The question being evaluated.
    *   `operator` (VARCHAR): Evaluator (e.g., `=`, `>`, `!=`).
    *   `value` (VARCHAR): Target value.
    *   `logic` (VARCHAR): `AND` / `OR` chaining.

*   **`gms_page_rule`** & **`gms_page_rule_condition`**: Cross-field validations applied on form submission.

## 4. Operational & Transactional Data

*   **`gms_organization`**: Organization entity that users belong to.
    *   `id` (BIGINT, PK)
    *   `name` / `organization_type` (VARCHAR).

*   **`gms_user`**: Identity table.
    *   `id` (BIGINT, PK)
    *   `username` / `password_hash` (VARCHAR): Credentials.
    *   `roles` (VARCHAR): Permissions (e.g., `ADMIN`, `APPLICANT`).
    *   `organization_id` (BIGINT, FK).

*   **`gms_application`**: Represents an active application entry by a user/org into a program round.
    *   `id` (BIGINT, PK)
    *   `program_round_id` (BIGINT, FK)
    *   `organization_id` (BIGINT, FK)
    *   `user_id` (VARCHAR)
    *   `status` (VARCHAR): e.g., `DRAFT`, `SUBMITTED`.

### 4.1. Data Storage Paths (The Core Magic)

As mentioned in the dynamic rendering architecture, answers are split between two paths based on the `gms_question` definition:

#### Path A: The Entity-Attribute-Value (EAV) Store
*   **`gms_application_data`**: Universal storage for dynamically created questions without explicit columns.
    *   `id` (BIGINT, PK)
    *   `application_id` (BIGINT, FK)
    *   `question_id` (BIGINT, FK)
    *   `value_text` (VARCHAR): The raw string value submitted by the applicant.

#### Path B: Domain-Specific Tables
When a question defines a `target_table` and `target_column`, data bypasses the EAV store and is injected directly into relational tables.
*   **`site`** (Example of a dynamic target domain table):
    *   `id` (BIGINT, PK)
    *   `application_id` (BIGINT, FK)
    *   `"openDate"` (VARCHAR): Explicit column dynamically populated.
    *   `"ADDRESS-1"` (VARCHAR).

## 5. Summary Flow
When a user submits an application:
1. The backend parses `question_id` to fetch the definition from `gms_question`.
2. If `target_table` = `null` or `gms_application_data`, the answer string is UPSERTED into `gms_application_data`.
3. If `target_table` = `site`, the backend checks the `AllowlistService` to confirm `site` and the target column are safe. Then, it generates dynamic SQL to UPSERT the value directly into the `site` table keyed on the applicant's `application_id`.
