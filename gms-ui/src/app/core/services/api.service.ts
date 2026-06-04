import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  Program, ProgramRound, Page, Question, QuestionOption,
  PageRenderDTO, PageSummaryDTO, Application, AnswerDTO,
  SaveResult, FileReferenceDTO
} from '../models';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private base = environment.apiBaseUrl;

  constructor(private http: HttpClient) {}

  // --- Programs ---
  createProgram(program: Partial<Program>): Observable<Program> {
    return this.http.post<Program>(`${this.base}/api/admin/programs`, program);
  }
  updateProgram(id: number, program: Partial<Program>): Observable<Program> {
    return this.http.put<Program>(`${this.base}/api/admin/programs/${id}`, program);
  }
  archiveProgram(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/api/admin/programs/${id}`);
  }
  listPrograms(page = 0, size = 20): Observable<any> {
    return this.http.get(`${this.base}/api/admin/programs`, {
      params: { page: page.toString(), size: size.toString() }
    });
  }
  getProgram(id: number): Observable<Program> {
    return this.http.get<Program>(`${this.base}/api/admin/programs/${id}`);
  }

  // --- Rounds ---
  createRound(programId: number, round: Partial<ProgramRound>): Observable<ProgramRound> {
    return this.http.post<ProgramRound>(`${this.base}/api/admin/programs/${programId}/rounds`, round);
  }
  updateRound(programId: number, roundId: number, round: Partial<ProgramRound>): Observable<ProgramRound> {
    return this.http.put<ProgramRound>(`${this.base}/api/admin/programs/${programId}/rounds/${roundId}`, round);
  }
  listRounds(programId: number): Observable<ProgramRound[]> {
    return this.http.get<ProgramRound[]>(`${this.base}/api/admin/programs/${programId}/rounds`);
  }
  updateRoundStatus(programId: number, roundId: number, status: string): Observable<ProgramRound> {
    return this.http.put<ProgramRound>(
      `${this.base}/api/admin/programs/${programId}/rounds/${roundId}/status`, { status });
  }

  // --- Pages ---
  createPage(page: Partial<Page>): Observable<Page> {
    return this.http.post<Page>(`${this.base}/api/admin/pages`, page);
  }
  updatePage(id: number, page: Partial<Page>): Observable<Page> {
    return this.http.put<Page>(`${this.base}/api/admin/pages/${id}`, page);
  }
  getPage(id: number): Observable<Page> {
    return this.http.get<Page>(`${this.base}/api/admin/pages/${id}`);
  }
  listPages(page = 0, size = 20, search?: string): Observable<any> {
    let params: any = { page: page.toString(), size: size.toString() };
    if (search) params.search = search;
    return this.http.get(`${this.base}/api/admin/pages`, { params });
  }

  // --- Questions ---
  createQuestion(question: Partial<Question>): Observable<Question> {
    return this.http.post<Question>(`${this.base}/api/admin/questions`, question);
  }
  updateQuestion(id: number, question: Partial<Question>): Observable<Question> {
    return this.http.put<Question>(`${this.base}/api/admin/questions/${id}`, question);
  }
  deactivateQuestion(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/api/admin/questions/${id}`);
  }
  listQuestions(page = 0, size = 20, type?: string, search?: string): Observable<any> {
    let params: any = { page: page.toString(), size: size.toString() };
    if (type) params.type = type;
    if (search) params.search = search;
    return this.http.get(`${this.base}/api/admin/questions`, { params });
  }
  getQuestion(id: number): Observable<Question> {
    return this.http.get<Question>(`${this.base}/api/admin/questions/${id}`);
  }

  // --- Applicant ---
  listActivePrograms(): Observable<any[]> {
    return this.http.get<any[]>(`${this.base}/api/programs`);
  }

  createApplication(programId: number, roundId: number): Observable<Application> {
    return this.http.post<Application>(
      `${this.base}/api/programs/${programId}/rounds/${roundId}/applications`, {});
  }
  getMyApplication(programId: number, roundId: number): Observable<Application> {
    return this.http.get<Application>(
      `${this.base}/api/programs/${programId}/rounds/${roundId}/applications/mine`);
  }
  getPageList(programId: number, roundId: number, appId?: number): Observable<PageSummaryDTO[]> {
    const params: any = {};
    if (appId != null) params['appId'] = appId.toString();
    return this.http.get<PageSummaryDTO[]>(
      `${this.base}/api/programs/${programId}/rounds/${roundId}/pages`, { params });
  }
  getRenderedPage(programId: number, roundId: number, pageId: number): Observable<PageRenderDTO> {
    return this.http.get<PageRenderDTO>(
      `${this.base}/api/programs/${programId}/rounds/${roundId}/pages/${pageId}`);
  }
  submitAnswers(appId: number, pageId: number, answers: AnswerDTO[]): Observable<SaveResult> {
    return this.http.post<SaveResult>(
      `${this.base}/api/applications/${appId}/pages/${pageId}/answers`, answers);
  }
  loadAnswers(appId: number, pageId: number, questionIds: number[]): Observable<AnswerDTO[]> {
    // Use repeated params (?questionIds=1&questionIds=2) — Spring @RequestParam List<Long>
    // binding accepts this format reliably across all proxy configurations.
    let params = new HttpParams();
    for (const id of questionIds) {
      params = params.append('questionIds', id.toString());
    }
    return this.http.get<AnswerDTO[]>(
      `${this.base}/api/applications/${appId}/pages/${pageId}/answers`, { params });
  }
  updateApplicationStatus(appId: number, status: string): Observable<Application> {
    return this.http.put<Application>(`${this.base}/api/applications/${appId}/status`, { status });
  }

  // --- Files ---
  uploadFile(appId: number, questionId: number, file: File): Observable<FileReferenceDTO> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('questionId', questionId.toString());
    return this.http.post<FileReferenceDTO>(`${this.base}/api/applications/${appId}/files`, formData);
  }
  deleteFile(appId: number, fileId: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/api/applications/${appId}/files/${fileId}`);
  }
}
