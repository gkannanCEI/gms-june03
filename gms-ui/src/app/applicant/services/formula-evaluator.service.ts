import { Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class FormulaEvaluatorService {

  /**
   * Evaluates a formula string with field references.
   * Formula format: "q_123 + q_456 - q_789"
   * Supports: +, -, *, / operations and parentheses.
   * Field references use format `q_<questionId>`.
   *
   * @param formula - The formula expression to evaluate
   * @param values - Map of questionId → string value
   * @returns The result as a string, or null if evaluation fails
   */
  evaluate(formula: string, values: Map<number, string>): string | null {
    if (!formula || formula.trim() === '') {
      return null;
    }

    try {
      // Replace q_<id> references with numeric values from the map
      const resolved = formula.replace(/q_(\d+)/g, (match, idStr) => {
        const questionId = parseInt(idStr, 10);
        const val = values.get(questionId);
        if (val == null || val === '') {
          return '0';
        }
        const numVal = parseFloat(val);
        if (isNaN(numVal)) {
          return '0';
        }
        return String(numVal);
      });

      // Validate that the resolved expression only contains safe characters
      // Allow: digits, decimal points, +, -, *, /, (, ), whitespace
      if (!/^[\d\s+\-*/().]+$/.test(resolved)) {
        return null;
      }

      // Evaluate the arithmetic expression safely using Function constructor
      // This is safe because we've validated the input contains only arithmetic characters
      const result = this.safeEval(resolved);
      if (result == null || isNaN(result) || !isFinite(result)) {
        return null;
      }

      // Return with reasonable precision
      return Number.isInteger(result) ? String(result) : result.toFixed(2);
    } catch {
      return null;
    }
  }

  /**
   * Safely evaluates an arithmetic expression using a recursive descent parser.
   * Only supports: numbers, +, -, *, /, parentheses.
   */
  private safeEval(expr: string): number | null {
    const tokens = this.tokenize(expr);
    if (tokens === null) return null;

    let pos = 0;

    const parseExpression = (): number | null => {
      let left = parseTerm();
      if (left === null) return null;

      while (pos < tokens.length && (tokens[pos] === '+' || tokens[pos] === '-')) {
        const op = tokens[pos++];
        const right = parseTerm();
        if (right === null) return null;
        left = op === '+' ? left + right : left - right;
      }
      return left;
    };

    const parseTerm = (): number | null => {
      let left = parseFactor();
      if (left === null) return null;

      while (pos < tokens.length && (tokens[pos] === '*' || tokens[pos] === '/')) {
        const op = tokens[pos++];
        const right = parseFactor();
        if (right === null) return null;
        if (op === '/') {
          if (right === 0) return null; // division by zero
          left = left / right;
        } else {
          left = left * right;
        }
      }
      return left;
    };

    const parseFactor = (): number | null => {
      // Handle unary minus
      if (pos < tokens.length && tokens[pos] === '-') {
        pos++;
        const factor = parseFactor();
        if (factor === null) return null;
        return -factor;
      }

      // Handle unary plus
      if (pos < tokens.length && tokens[pos] === '+') {
        pos++;
        return parseFactor();
      }

      // Handle parentheses
      if (pos < tokens.length && tokens[pos] === '(') {
        pos++; // consume '('
        const result = parseExpression();
        if (result === null) return null;
        if (pos >= tokens.length || tokens[pos] !== ')') return null;
        pos++; // consume ')'
        return result;
      }

      // Handle number
      if (pos < tokens.length) {
        const num = parseFloat(tokens[pos]);
        if (!isNaN(num)) {
          pos++;
          return num;
        }
      }

      return null;
    };

    const result = parseExpression();
    if (pos !== tokens.length) return null; // Unconsumed tokens
    return result;
  }

  private tokenize(expr: string): string[] | null {
    const tokens: string[] = [];
    let i = 0;
    const s = expr.trim();

    while (i < s.length) {
      // Skip whitespace
      if (s[i] === ' ' || s[i] === '\t') {
        i++;
        continue;
      }

      // Operators and parentheses
      if ('+-*/()'.includes(s[i])) {
        tokens.push(s[i]);
        i++;
        continue;
      }

      // Numbers (including decimals)
      if (s[i] >= '0' && s[i] <= '9' || s[i] === '.') {
        let num = '';
        while (i < s.length && (s[i] >= '0' && s[i] <= '9' || s[i] === '.')) {
          num += s[i];
          i++;
        }
        tokens.push(num);
        continue;
      }

      // Unknown character
      return null;
    }

    return tokens;
  }
}
