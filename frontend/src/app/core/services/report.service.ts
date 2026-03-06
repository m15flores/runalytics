import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { TrainingReport } from '../models/report/training-report.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ReportService {
  private readonly http = inject(HttpClient);

  getReports(userId: string): Observable<TrainingReport[]> {
    return this.http.get<TrainingReport[]>(`${environment.reportServiceUrl}/api/reports/users/${userId}`);
  }

  getReport(userId: string, weekNumber: number, year: number): Observable<TrainingReport> {
    return this.http.get<TrainingReport>(`${environment.reportServiceUrl}/api/reports/users/${userId}/${weekNumber}/${year}`);
  }
}
