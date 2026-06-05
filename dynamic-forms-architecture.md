# GMS Dynamic Form Rendering Architecture

This document describes the end-to-end architecture used in GMS to dynamically render form questions from a database configuration, load existing data, and save answers dynamically back to the database.

## 1. Overview

The framework allows administrators to define **Programs**, **Rounds**, **Pages**, and **Questions**. 
Questions can store answers either in an Entity-Attribute-Value (EAV) table (`gms_application_data`) or directly in domain-specific tables like `site` or `applicant_profile`. 
The UI asks the backend for a "Page", receives a JSON structure (`PageRenderDTO`) defining all fields (types, labels, dropdown options, visibility logic, column sizes), and dynamically generates an Angular Reactive Form.

## 2. Database Models & Schema

The core entities that drive dynamic form building:

*   **`ProgramRound` / `RoundPage`**: Define the sequence of pages an applicant must complete.
*   **`Question`**: The root definition of a question. It dictates the `question_type` (`TEXT`, `NUMBER`, `RADIO_YES_NO`, `SELECT_ONE`, `CHECKBOX`, `ATTACHMENT`, `LABEL`, etc.), `target_table`, and `target_column`.
*   **`RoundPageQuestion`**: Links a Question to a RoundPage. It defines layout configurations (`display_order`, `column_span`, `section_group`, `is_readonly`, `is_required`).
*   **`QuestionOption`**: Defines selectable choices (e.g., dropdown values) for `SELECT_ONE` or `RADIO` types.

### Where Answers Are Saved

The `ApplicationDataRepository` handles saving data dynamically:
1.  **Single Table (EAV)**: If a question has `targetTable` set to `gms_application_data` (or null), the answer is stored as `(application_id, question_id, value_text)`.
2.  **Domain Table Mapping**: If a question has `targetTable` = `site` and `targetColumn` = `openDate`, the repository dynamically updates the row in the `site` table for that `application_id`. 
    *(Note: To prevent SQL injection, domain tables/columns are checked against an `AllowlistService`.)*

## 3. Backend API Layer

The backend uses a standard Spring Controller-Service structure to assemble the JSON structure.

### `GET /api/programs/{pId}/rounds/{rId}/pages/{pageId}`

This endpoint returns a `PageRenderDTO` containing the page metadata and an array of `QuestionRenderDTO`s.

A `QuestionRenderDTO` looks like this:
```json
{
  "questionId": 12,
  "questionType": "TEXT",
  "label": "Homeowner First Name",
  "required": true,
  "options": [], // Used for dropdowns/radio buttons
  "displayConfig": {
    "readonly": false,
    "columnSpan": 2, // Tailwind CSS grid col-span-X
    "sectionGroup": "Homeowner Info",
    "helpText": "Enter the legal first name"
  }
}
```

### Loading and Saving Answers

*   **`GET /api/applications/{appId}/pages/{pageId}/answers`**: Retrieves all previously saved answers for the provided questions. It routes requests to either the EAV or domain tables based on the question config.
*   **`POST /api/applications/{appId}/pages/{pageId}/answers`**: Saves the answers dynamically, upserting rows into `gms_application_data` or the target domain table.

## 4. Frontend Angular Architecture

The dynamic form implementation relies on Angular's **Reactive Forms** and dynamic component rendering using a custom **Host Directive**.

### A. The Form Container (`form-page.component.ts`)

1.  **Loading the Definition**: The component calls the backend `/pages/{pageId}` API to retrieve the `PageRenderDTO`.
2.  **Building the Form**: It iterates over `data.questions` and dynamically adds a `FormControl` for each question (e.g., `controls['q_' + q.questionId] = new FormControl('', validators);`).
3.  **Loading Data**: It calls the `/answers` endpoint to fetch existing data, then calls `this.formGroup.patchValue(...)` to prepopulate the form.
4.  **Layout**: The HTML template renders questions inside a CSS Grid, grouped by `q.displayConfig.sectionGroup` if defined.

### B. Dynamic Component Rendering (`QuestionHostDirective`)

The heart of the dynamic rendering is the `appQuestionHost` directive. 

```html
<div appQuestionHost
     [question]="q"
     [control]="getControl(q.questionId)"
     [appId]="appId"
     [formGroup]="formGroup">
</div>
```

**`question-host.directive.ts`:**
This directive looks at the `questionType` (e.g., `TEXT`, `CHECKBOX`, `SELECT_ONE`) and dynamically instantiates the correct Angular Component using Angular's `ViewContainerRef.createComponent()`.

It passes the necessary `@Input()` data to the generated component:
*   `question`: The `QuestionRenderDTO` (contains the label, options, constraints).
*   `control`: The specific `FormControl` bound to this input.

### C. Specific Question Components

Each input type is represented by an individual standalone Angular component. These components just focus on rendering the HTML input and tying it to the provided `FormControl`.

**Example: `text-question.component.ts`**
```ts
@Component({
  selector: 'app-text-question',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <div class="form-group">
      <label [for]="'q_' + question.questionId" class="form-label">
        {{ question.label }}
        <span *ngIf="question.required" class="text-danger">*</span>
      </label>
      <input type="text"
             [id]="'q_' + question.questionId"
             class="form-control"
             [formControl]="control"
             [class.is-invalid]="control.invalid && control.touched" />
             
      <div *ngIf="question.displayConfig?.helpText" class="form-text">
        {{ question.displayConfig!.helpText }}
      </div>
    </div>
  `
})
export class TextQuestionComponent {
  @Input() question!: QuestionRenderDTO;
  @Input() control!: FormControl;
}
```

**Example: `select-one-question.component.ts`**
For dropdowns, it iterates over the `question.options` array provided by the backend to populate the `<select>` tag.

## 5. Adding This to Your New Application

To implement this elsewhere:

1.  **Design the Backend Schema**: Decide if you need an EAV table for purely dynamic fields, or if you will only map fields directly to domain tables.
2.  **Build the Page Render API**: Create an endpoint that converts your backend configuration into a JSON layout representation (like `QuestionRenderDTO`).
3.  **Implement the Angular Architecture**:
    *   Create the specific UI components for each input type (`TextQuestion`, `SelectQuestion`, `DateQuestion`, etc.).
    *   Create a `QuestionHostDirective` that dynamically renders the correct component based on `type`.
    *   Create a generic container component (`form-page`) that fetches the layout, generates the `FormGroup`, fetches existing values to patch the form, and submits the form back to the server.
4.  **Save Logic**: Create a backend endpoint capable of handling an array of answers (`[{ questionId: 1, value: "John" }]`) and applying those updates to the database dynamically.