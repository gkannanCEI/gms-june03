export interface Program {
  id: number;
  programName: string;
  description?: string;
  goal?: string;
  totalBudget?: number;
  status: 'ACTIVE' | 'ARCHIVED';
  rounds?: ProgramRound[];
}

export interface ProgramRound {
  id: number;
  programId: number;
  roundName: string;
  startDate: string;
  endDate: string;
  fundsLimit?: number;
  status: 'DRAFT' | 'ACTIVE' | 'CLOSED' | 'ARCHIVED';
  eligibleOrganizationTypes?: string;
}

export interface Page {
  id: number;
  pageName: string;
  pageDescription?: string;
  path?: string;
  active: boolean;
}

export interface Question {
  id: number;
  questionType: string;
  label: string;
  targetTable?: string;
  targetColumn?: string;
  required?: boolean;
  validationRegex?: string;
  allowedFileTypes?: string;
  maxFileSizeMb?: number;
  active: boolean;
  options?: QuestionOption[];
  childQuestions?: Question[];
}

export interface QuestionOption {
  id: number;
  optionLabel: string;
  optionValue: string;
  displayOrder: number;
  isOtherOption?: boolean;
  optionTargetTable?: string;
  optionTargetColumn?: string;
  lookupId?: string;
}

export interface PageRenderDTO {
  programId: number;
  roundId: number;
  pageId: number;
  pageName: string;
  pageDescription?: string;
  path?: string;
  questions: QuestionRenderDTO[];
  pageRules?: PageRuleDTO[];
}

export interface PageRuleDTO {
  id?: number;
  logic: string;
  errorMessage?: string;
  conditions: PageRuleConditionDTO[];
}

export interface PageRuleConditionDTO {
  questionId: number;
  operator: string;
  value?: string;
  value2?: string;
}

export interface VisibilityRuleDTO {
  id?: number;
  triggerQuestionId: number;
  operator: string;
  value?: string;
  logic?: string;
}

export interface QuestionRenderDTO {
  questionId: number;
  questionType: string;
  label: string;
  required: boolean;
  validationRegex?: string;
  minValue?: string;
  maxValue?: string;
  allowedFileTypes?: string;
  maxFileSizeMb?: number;
  displayOrder: number;
  displayConfig: QuestionDisplayConfig;
  options?: QuestionOptionDTO[];
  childQuestions?: ChildQuestionRenderDTO[];
  visibilityRules?: VisibilityRuleDTO[];
  formula?: string;
}

export interface QuestionDisplayConfig {
  readonly: boolean;
  columnSpan: number;
  labelPosition: string;
  helpText?: string;
  sectionGroup?: string;
}

export interface QuestionOptionDTO {
  id: number;
  optionLabel: string;
  optionValue: string;
  displayOrder: number;
  isOtherOption?: boolean;
}

export interface ChildQuestionRenderDTO {
  triggerValue: string;
  question: QuestionRenderDTO;
}

export interface PageSummaryDTO {
  pageId: number;
  pageName: string;
  path?: string;
  displayOrder: number;
  completed: boolean;
}

export interface Application {
  id: number;
  programRoundId: number;
  organizationId: number;
  userId: string;
  status: 'DRAFT' | 'SUBMITTED' | 'WITHDRAWN';
  eligibilityWarning: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface AnswerDTO {
  questionId: number;
  value: string;
  otherText?: string;
}

export interface SaveResult {
  success: boolean;
  errors: FieldError[];
}

export interface FieldError {
  questionId: number;
  message: string;
}

export interface FileReferenceDTO {
  id: string;
  originalFilename: string;
  fileExtension: string;
  fileSizeBytes: number;
  scanStatus: 'PENDING' | 'CLEAN' | 'INFECTED';
}
