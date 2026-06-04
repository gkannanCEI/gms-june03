import { Injectable } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { VisibilityRuleDTO } from '../../core/models';

@Injectable({ providedIn: 'root' })
export class VisibilityRuleEvaluatorService {

  /**
   * Evaluates visibility rules for a question.
   * @param rules - The visibility rules to evaluate
   * @param formGroup - The form group containing all question controls
   * @param questionMap - Map of questionId → formControlKey (e.g. 'q_123')
   * @returns true if the question should be VISIBLE
   */
  evaluateRules(rules: VisibilityRuleDTO[], formGroup: FormGroup, questionMap: Map<number, string>): boolean {
    if (!rules || rules.length === 0) {
      return true; // No rules means always visible
    }

    // Determine logic mode from the first rule (all rules in a set share the same logic)
    const logic = rules[0].logic?.toUpperCase() || 'AND';

    if (logic === 'OR') {
      // OR logic: at least one rule must pass
      return rules.some(rule => {
        const controlKey = questionMap.get(rule.triggerQuestionId);
        if (!controlKey) return false;
        return this.evaluateRule(rule, formGroup, controlKey);
      });
    }

    // AND logic (default): all rules must pass
    return rules.every(rule => {
      const controlKey = questionMap.get(rule.triggerQuestionId);
      if (!controlKey) return false;
      return this.evaluateRule(rule, formGroup, controlKey);
    });
  }

  private evaluateRule(rule: VisibilityRuleDTO, formGroup: FormGroup, controlKey: string): boolean {
    const control = formGroup.get(controlKey);
    const value = control ? control.value : null;
    const strValue = value != null ? String(value) : '';

    switch (rule.operator) {
      case 'IS_NOT_EMPTY':
        return value != null && value !== undefined && strValue !== '';

      case 'IS_EMPTY':
        return value == null || value === undefined || strValue === '';

      case 'EQUALS':
        return strValue === (rule.value ?? '');

      case 'NOT_EQUALS':
        return strValue !== (rule.value ?? '');

      case 'LESS_THAN': {
        const numVal = parseFloat(strValue);
        const ruleVal = parseFloat(rule.value ?? '');
        if (isNaN(numVal) || isNaN(ruleVal)) return false;
        return numVal < ruleVal;
      }

      case 'GREATER_THAN': {
        const numVal = parseFloat(strValue);
        const ruleVal = parseFloat(rule.value ?? '');
        if (isNaN(numVal) || isNaN(ruleVal)) return false;
        return numVal > ruleVal;
      }

      case 'CONTAINS':
        return strValue.includes(rule.value ?? '');

      default:
        return true; // Unknown operator - default to visible
    }
  }
}
