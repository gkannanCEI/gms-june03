# Grant Management Base Framework — Architectural Diagrams

---

## 1. System Architecture Overview

High-level view of the technology layers, external dependencies, and how they connect.

```mermaid
graph TB
    subgraph Client["Client Layer"]
        ADMIN["Angular Admin Module\n(Admin UI)"]
        APPLICANT["Angular Applicant Module\n(Applicant UI)"]
    end

    subgraph Security["Security Layer"]
        B2C["Azure AD B2C\n(Identity Provider)"]
        JWT["JWT Bearer Token\n(Spring Security Filter)"]
    end

    subgraph Backend["Spring Boot Application"]
        direction TB
        subgraph Controllers["REST Controllers"]
            AC["AdminController\n/api/admin/**"]
            PBC["PageBuilderController\n/api/programs/**/pages"]
            ADC["ApplicationDataController\n/api/applications/**"]
        end
        subgraph Services["Service Layer"]
            PS["ProgramService"]
            PRS["ProgramRoundService"]
            PGS["PageService"]
            QS["QuestionService"]
            PBS["PageBuilderService"]
            ADS["ApplicationDataService"]
            AS["ApplicationService"]
            OS["OrganizationService"]
        end
        subgraph Repositories["Repository Layer"]
            JPA["JPA / Hibernate Repositories"]
            DDR["DynamicDataRepository\n(dynamic SQL)"]
        end
    end

    subgraph Database["Database (PostgreSQL default / Oracle)"]
        META["Metadata Tables\ngms_program, gms_program_round\ngms_page, gms_question, gms_question_option\ngms_round_page, gms_round_page_question\ngms_round_page_question_role"]
        APPDATA["Application Data Tables\n(dynamic target tables)"]
        APPLIFE["Application Lifecycle\ngms_application, gms_organization"]
    end

    ADMIN -->|JWT| JWT
    APPLICANT -->|JWT| JWT
    B2C -->|issues token| ADMIN
    B2C -->|issues token| APPLICANT
    JWT --> Controllers
    AC --> Services
    PBC --> PBS
    ADC --> ADS
    ADC --> AS
    Services --> JPA
    ADS --> DDR
    JPA --> META
    JPA --> APPLIFE
    DDR --> APPDATA
```

---

## 2. Domain Model Hierarchy

How the core domain entities relate to each other.

```mermaid
erDiagram
    PROGRAM {
        number id PK
        string program_name
        string goal
        decimal total_budget
        string status
    }
    PROGRAM_ROUND {
        number id PK
        number program_id FK
        string round_name
        date start_date
        date end_date
        decimal funds_limit
        string status
        string eligible_organization_types
    }
    PAGE {
        number id PK
        string page_name
        string page_description
        boolean active
    }
    ROUND_PAGE {
        number id PK
        number program_round_id FK
        number page_id FK
        number display_order
        string page_name_override
        string page_description_override
    }
    QUESTION {
        number id PK
        string question_type
        string label
        string target_table
        string target_column
        boolean required
        string validation_regex
        string allowed_file_types
        number max_file_size_mb
        number parent_question_id FK
        string trigger_value
        number child_display_order
        boolean active
    }
    QUESTION_OPTION {
        number id PK
        number question_id FK
        string option_label
        string option_value
        number display_order
        boolean is_other_option
        string option_target_table
        string option_target_column
        string lookup_id
    }
    ROUND_PAGE_QUESTION {
        number id PK
        number round_page_id FK
        number question_id FK
        number display_order
        string label_override
        boolean required_override
        string round_label_override
        boolean excluded
        boolean readonly
        number column_span
        string label_position
        string help_text
        string section_group
        string min_value
        string max_value
    }
    ROUND_PAGE_QUESTION_ROLE {
        number id PK
        number round_page_question_id FK
        string role_name
    }
    ORGANIZATION {
        number id PK
        string name
        string organization_type
        boolean active
    }
    APPLICATION {
        number id PK
        number program_round_id FK
        number organization_id FK
        string user_id
        string status
        boolean eligibility_warning
    }

    PROGRAM ||--o{ PROGRAM_ROUND : "has rounds"
    PROGRAM_ROUND ||--o{ ROUND_PAGE : "assigns pages"
    PAGE ||--o{ ROUND_PAGE : "used in"
    ROUND_PAGE ||--o{ ROUND_PAGE_QUESTION : "has questions"
    QUESTION ||--o{ ROUND_PAGE_QUESTION : "assigned via"
    QUESTION ||--o{ QUESTION_OPTION : "has options"
    QUESTION ||--o{ QUESTION : "parent of (RADIO_YES_NO, supports multi-level nesting)"
    ROUND_PAGE_QUESTION ||--o{ ROUND_PAGE_QUESTION_ROLE : "visible to roles"
    PROGRAM_ROUND ||--o{ APPLICATION : "receives"
    ORGANIZATION ||--o{ APPLICATION : "submits"
```

---

## 3. Status Lifecycles

```mermaid
stateDiagram-v2
    direction LR

    state "Program Status" as PS {
        [*] --> ACTIVE : create
        ACTIVE --> ARCHIVED : archive
    }

    state "ProgramRound Status" as RS {
        [*] --> DRAFT : create
        DRAFT --> ACTIVE : activate
        ACTIVE --> CLOSED : close
        CLOSED --> ARCHIVED : archive
    }

    state "Application Status" as AS {
        [*] --> DRAFT : create
        DRAFT --> SUBMITTED : submit
        DRAFT --> WITHDRAWN : withdraw
        SUBMITTED --> WITHDRAWN : withdraw
        SUBMITTED --> DRAFT : admin reopen
    }
```

---

## 4. Applicant Journey — Request Flow

End-to-end flow from an applicant loading a form page to saving answers.

```mermaid
sequenceDiagram
    actor Applicant
    participant Angular as Angular Applicant Module
    participant B2C as Azure AD B2C
    participant AppCtrl as ApplicationDataController
    participant PBCtrl as PageBuilderController
    participant AppSvc as ApplicationService
    participant PBSvc as PageBuilderService
    participant ADSvc as ApplicationDataService
    participant DB as Database

    Applicant->>B2C: Authenticate
    B2C-->>Angular: JWT token

    Applicant->>Angular: Select program round
    Angular->>AppCtrl: POST /api/programs/{pId}/rounds/{rId}/applications
    AppCtrl->>AppSvc: createApplication(roundId, userId, orgId)
    AppSvc->>DB: Check round ACTIVE + org eligible
    AppSvc->>DB: INSERT gms_application (DRAFT)
    AppSvc-->>AppCtrl: ApplicationDTO
    AppCtrl-->>Angular: 201 + ApplicationDTO (appId)

    Angular->>PBCtrl: GET /api/programs/{pId}/rounds/{rId}/pages
    PBCtrl->>PBSvc: getPagesForRound(roundId, userRoles)
    PBSvc->>DB: Load RoundPages + filter by role visibility
    PBSvc-->>PBCtrl: List<PageSummaryDTO>
    PBCtrl-->>Angular: 200 + page list

    Angular->>PBCtrl: GET /api/programs/{pId}/rounds/{rId}/pages/{pageId}
    PBCtrl->>PBSvc: buildPage(roundId, pageId, userRoles)
    PBSvc->>DB: Load RoundPageQuestions + Questions
    PBSvc->>PBSvc: Resolve overrides, filter excluded/roles, build displayConfig
    PBSvc-->>PBCtrl: PageRenderDTO
    PBCtrl-->>Angular: 200 + PageRenderDTO

    Angular->>Angular: Render questions via QuestionHostDirective
    Applicant->>Angular: Fill in answers

    Angular->>AppCtrl: POST /api/applications/{appId}/pages/{pageId}/answers
    AppCtrl->>AppCtrl: resolveRoundPageId(appId, pageId)
    AppCtrl->>ADSvc: saveAnswers(appId, roundPageId, answers)
    ADSvc->>ADSvc: Validate all answers (type rules, required, regex)
    ADSvc->>DB: BEGIN TRANSACTION
    ADSvc->>DB: INSERT/UPDATE target tables
    ADSvc->>DB: Re-read saved rows, verify values
    alt All valid
        ADSvc->>DB: COMMIT
        ADSvc-->>AppCtrl: SaveResult { success: true }
    else Validation or mismatch error
        ADSvc->>DB: ROLLBACK
        ADSvc-->>AppCtrl: SaveResult { success: false, errors: [...] }
    end
    AppCtrl-->>Angular: 200 + SaveResult
    Angular->>Applicant: Show success or inline field errors
```

---

## 5. Admin Configuration Flow

How an administrator sets up a program round with pages and questions, including option and child-link management.

```mermaid
sequenceDiagram
    actor Admin
    participant Angular as Angular Admin Module
    participant AC as AdminController
    participant PS as ProgramService
    participant PRS as ProgramRoundService
    participant PGS as PageService
    participant QS as QuestionService
    participant DB as Database

    Note over Admin,DB: ── Question Library Setup ──

    Admin->>Angular: Create Question (e.g. SELECT_MULTI)
    Angular->>AC: POST /api/admin/questions
    AC->>QS: createQuestion(type, label, targetTable, targetColumn)
    QS->>DB: INSERT gms_question
    AC-->>Angular: 201 + QuestionDTO

    Admin->>Angular: Add options to question
    loop For each option
        Angular->>AC: POST /api/admin/questions/{id}/options
        AC->>QS: addOption(questionId, label, value, displayOrder)
        QS->>DB: INSERT gms_question_option
        AC-->>Angular: 201 + QuestionOptionDTO
    end

    Admin->>Angular: Reorder options via drag-and-drop
    Angular->>AC: PUT /api/admin/questions/{id}/options/reorder
    AC->>QS: reorderOptions(questionId, orderedOptionIds)
    QS->>DB: UPDATE gms_question_option display_order
    AC-->>Angular: 200 + List<QuestionOptionDTO>

    Admin->>Angular: Create RADIO_YES_NO question (parent)
    Angular->>AC: POST /api/admin/questions
    AC->>QS: createQuestion(RADIO_YES_NO, ...)
    QS->>DB: INSERT gms_question (auto-injects Yes/No options)
    AC-->>Angular: 201 + QuestionDTO

    Admin->>Angular: Link child question to RADIO_YES_NO parent
    Angular->>AC: POST /api/admin/questions/{parentId}/children
    AC->>QS: linkChild(parentId, childId, triggerValue, childDisplayOrder)
    QS->>DB: UPDATE gms_question SET parent_question_id, trigger_value
    AC-->>Angular: 201 + ChildLinkDTO

    opt Update child link triggerValue or displayOrder
        Admin->>Angular: Edit triggerValue or childDisplayOrder
        Angular->>AC: PUT /api/admin/questions/{parentId}/children/{childId}
        AC->>QS: updateChildLink(parentId, childId, triggerValue, childDisplayOrder)
        QS->>DB: UPDATE gms_question SET trigger_value, child_display_order
        AC-->>Angular: 200 + ChildLinkDTO
    end

    Note over Admin,DB: ── Program & Round Setup ──

    Admin->>Angular: Create Program
    Angular->>AC: POST /api/admin/programs
    AC->>PS: createProgram(name, goal, budget)
    PS->>DB: INSERT gms_program (ACTIVE)
    AC-->>Angular: 201 + ProgramDTO

    Admin->>Angular: Create Program Round
    Angular->>AC: POST /api/admin/programs/{pId}/rounds
    AC->>PRS: createRound(programId, name, dates, fundsLimit, eligibleOrgTypes)
    PRS->>DB: INSERT gms_program_round (DRAFT)
    AC-->>Angular: 201 + ProgramRoundDTO

    Note over Admin,DB: ── Round-Page-Question Configuration ──

    Admin->>Angular: Assign Page to Round
    Angular->>AC: POST /api/admin/programs/{pId}/rounds/{rId}/pages
    AC->>PRS: assignPage(roundId, pageId, displayOrder)
    PRS->>DB: INSERT gms_round_page
    AC-->>Angular: 200

    Admin->>Angular: Override page name for this round
    Angular->>AC: PUT /api/admin/programs/{pId}/rounds/{rId}/pages/{pgId}
    AC->>PRS: updateRoundPage(roundPageId, pageNameOverride, pageDescOverride)
    PRS->>DB: UPDATE gms_round_page
    AC-->>Angular: 200

    Admin->>Angular: Assign Question to Round-Page
    Angular->>AC: POST /api/admin/programs/{pId}/rounds/{rId}/pages/{pgId}/questions
    AC->>PGS: assignQuestion(roundPageId, questionId)
    PGS->>DB: INSERT gms_round_page_question (defaults)
    AC-->>Angular: 200

    Admin->>Angular: Configure overrides + display config
    Angular->>AC: PUT /api/admin/programs/{pId}/rounds/{rId}/pages/{pgId}/questions/{qId}
    AC->>PRS: updateRoundPageQuestion(id, overrides, displayConfig, visibleToRoles)
    PRS->>DB: UPDATE gms_round_page_question
    PRS->>DB: REPLACE gms_round_page_question_role rows
    AC-->>Angular: 200

    Note over Admin,DB: ── Activate Round ──

    Admin->>Angular: Activate Round
    Angular->>AC: PUT /api/admin/programs/{pId}/rounds/{rId}/status
    AC->>PRS: transitionStatus(roundId, ACTIVE)
    PRS->>DB: UPDATE gms_program_round.status = ACTIVE
    AC-->>Angular: 200 + ProgramRoundDTO
```

---

## 6. PageBuilderService — Override Resolution

Internal logic of how the PageBuilderService assembles a PageRenderDTO.

```mermaid
flowchart TD
    A([Request: programId, roundId, pageId, userRoles]) --> B{Round ACTIVE?\nor admin preview?}
    B -- No --> ERR[Return HTTP 404]
    B -- Yes --> C[Load RoundPage]
    C --> D[Resolve page name\nRoundPage.pageNameOverride\nor Page.pageName]
    D --> E[Load all RoundPageQuestions\nfor this RoundPage]
    E --> F[For each RoundPageQuestion]

    F --> G{excluded = true?}
    G -- Yes --> SKIP[Skip question]
    G -- No --> H{visibleToRoles\nnon-empty?}
    H -- Yes --> I{User has\nmatching role?}
    I -- No --> SKIP
    I -- Yes --> J[Resolve label\nroundLabelOverride\n→ labelOverride\n→ Question.label]
    H -- No --> J

    J --> K[Resolve required flag\nrequiredOverride\n→ requiredOverride\n→ Question.required]
    K --> L[Build QuestionDisplayConfig\nreadonly, columnSpan\nlabelPosition, helpText\nsectionGroup]
    L --> M{Is RADIO_YES_NO\nwith children?}
    M -- Yes --> N[Load child RoundPageQuestions\nApply same rules recursively\nNest in childQuestions]
    M -- No --> O[Add to ordered list]
    N --> O

    O --> P{More questions?}
    P -- Yes --> F
    P -- No --> Q[Sort by displayOrder ASC\ntie-break by questionId ASC]
    Q --> R([Return PageRenderDTO])
```

---

## 7. Angular Component Architecture

How the Angular Applicant Module renders a page using the component-per-type model.

```mermaid
flowchart TD
    PR[PageRenderDTO\nfrom API] --> PRC[PageComponent\nbuilds ReactiveForm group]
    PRC --> GRID[12-column CSS Grid]

    GRID --> SG{sectionGroup\nset?}
    SG -- Yes --> SEC[Section Container\nwith heading]
    SG -- No --> DIRECT[Direct grid cell]
    SEC --> QH
    DIRECT --> QH

    QH[QuestionHostDirective\nmaps questionType → Component]

    QH --> RO{displayConfig\n.readonly?}
    RO -- Yes --> ROC[ReadonlyQuestionComponent\nlabel + saved value display]
    RO -- No --> TC[Type-specific Component\ne.g. TextQuestionComponent\nDateQuestionComponent\nSelectOneQuestionComponent\netc.]

    TC --> LP{labelPosition}
    LP -- ABOVE --> LA[label above input]
    LP -- LEFT --> LL[label left of input]
    LP -- HIDDEN --> LH[sr-only label]

    TC --> HT{helpText\nnon-blank?}
    HT -- Yes --> HTR[small element\naria-describedby linked]
    HT -- No --> NOHL[ ]

    TC --> VAL[Validation errors\nfrom AbstractControl\nbelow input]

    TC --> RYN{RADIO_YES_NO?}
    RYN -- Yes --> CHILD[Evaluate triggerValue\nfor each child]
    CHILD --> MATCH{Selected value\nmatches triggerValue?}
    MATCH -- Yes --> SHOW[Render child via\nQuestionHostDirective]
    MATCH -- No --> HIDE[Hide child\nreset form control to null]

    TC --> ATT{ATTACHMENT?}
    ATT -- Yes --> ATTC[AttachmentQuestionComponent\nreceives appId via\nQuestionHostDirective\nfor file upload API calls]
```

---

## 8. Organization Eligibility and Application Creation

Decision flow when an applicant attempts to start an application.

```mermaid
flowchart TD
    START([Applicant: Start Application\nfor programId + roundId]) --> AUTH{JWT valid?\nAPPLICANT role?}
    AUTH -- No --> E401[HTTP 401 / 403]
    AUTH -- Yes --> ROUND{ProgramRound\nstatus = ACTIVE?}
    ROUND -- No --> E404[HTTP 404\nRound not available]
    ROUND -- Yes --> ORGLOAD[Load applicant's\nOrganization from UserPrincipal]
    ORGLOAD --> ELIG{eligibleOrganizationTypes\nempty?}
    ELIG -- Yes\n(all eligible) --> DUP
    ELIG -- No --> CHECK{Org type in\neligible list?}
    CHECK -- No --> E403[HTTP 403\nOrganization not eligible]
    CHECK -- Yes --> DUP{Application already\nexists for this\nuser + round?}
    DUP -- Yes --> RETURN[Return existing\nApplicationDTO]
    DUP -- No --> CREATE[INSERT gms_application\nstatus = DRAFT]
    CREATE --> RESP[HTTP 201 + ApplicationDTO\nwith applicationId]
```

---
