import { Directive, Input, ViewContainerRef, OnInit, Type } from '@angular/core';
import { AbstractControl } from '@angular/forms';
import { QuestionRenderDTO } from '../../core/models';
import { TextQuestionComponent } from './text-question/text-question.component';
import { TextAreaQuestionComponent } from './text-area-question/text-area-question.component';
import { DateQuestionComponent } from './date-question/date-question.component';
import { SelectOneQuestionComponent } from './select-one-question/select-one-question.component';
import { SelectMultiQuestionComponent } from './select-multi-question/select-multi-question.component';
import { CheckboxQuestionComponent } from './checkbox-question/checkbox-question.component';
import { RadioYesNoQuestionComponent } from './radio-yes-no-question/radio-yes-no-question.component';
import { AttachmentQuestionComponent } from './attachment-question/attachment-question.component';
import { LabelQuestionComponent } from './label-question/label-question.component';
import { ReadonlyQuestionComponent } from './readonly-question/readonly-question.component';
import { NumberQuestionComponent } from './number-question/number-question.component';

const COMPONENT_MAP: Record<string, Type<any>> = {
  'TEXT': TextQuestionComponent,
  'TEXT_AREA': TextAreaQuestionComponent,
  'DATE': DateQuestionComponent,
  'DECIMAL': NumberQuestionComponent,
  'WHOLE_NUMBER': NumberQuestionComponent,
  'CURRENCY': NumberQuestionComponent,
  'PHONE': TextQuestionComponent,
  'ZIP_CODE': TextQuestionComponent,
  'SELECT_ONE': SelectOneQuestionComponent,
  'SELECT_MULTI': SelectMultiQuestionComponent,
  'CHECKBOX': CheckboxQuestionComponent,
  'RADIO_YES_NO': RadioYesNoQuestionComponent,
  'ATTACHMENT': AttachmentQuestionComponent,
  'LABEL': LabelQuestionComponent,
};

@Directive({ selector: '[appQuestionHost]', standalone: true })
export class QuestionHostDirective implements OnInit {
  @Input() question!: QuestionRenderDTO;
  @Input() control!: AbstractControl;
  @Input() currentValue?: string;
  /**
   * GAP-23: appId is now an explicit @Input so it can be forwarded to
   * AttachmentQuestionComponent.  Without it, file uploads always fail
   * because AttachmentQuestionComponent.onFileSelected() guards on appId.
   */
  @Input() appId?: number;
  /** Forwarded to RadioYesNoQuestionComponent so child controls come from the shared FormGroup. */
  @Input() formGroup?: any;

  constructor(private viewContainerRef: ViewContainerRef) {}

  ngOnInit() {
    this.viewContainerRef.clear();

    let componentType: Type<any>;
    if (this.question.displayConfig?.readonly) {
      componentType = ReadonlyQuestionComponent;
    } else {
      componentType = COMPONENT_MAP[this.question.questionType] || TextQuestionComponent;
    }

    const componentRef = this.viewContainerRef.createComponent(componentType);
    componentRef.instance.question = this.question;
    componentRef.instance.control = this.control;

    if (this.currentValue !== undefined) {
      componentRef.instance.currentValue = this.currentValue;
    }

    // GAP-23: Inject appId into AttachmentQuestionComponent (and any other
    // component that declares an appId @Input).
    if (this.appId !== undefined) {
      (componentRef.instance as any)['appId'] = this.appId;
    }

    // GAP-21: Inject the parent FormGroup into RadioYesNoQuestionComponent so
    // child controls are retrieved from the shared form rather than isolated locals.
    if (this.formGroup !== undefined) {
      (componentRef.instance as any)['formGroup'] = this.formGroup;
    }
  }
}
