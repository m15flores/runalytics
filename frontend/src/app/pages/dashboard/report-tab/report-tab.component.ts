import { Component, effect, inject, input, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { catchError, of, switchMap } from 'rxjs';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { marked } from 'marked';
import { CardModule } from 'primeng/card';
import { TrainingReport } from '../../../core/models/report/training-report.model';
import { ReportService } from '../../../core/services/report.service';

@Component({
  selector: 'app-report-tab',
  standalone: true,
  imports: [CardModule],
  templateUrl: './report-tab.component.html',
  styleUrl: './report-tab.component.scss'
})
export class ReportTabComponent {

  private reportService = inject(ReportService);
  private sanitizer = inject(DomSanitizer);
  private http = inject(HttpClient);

  refreshTrigger = input<number>(0);
  report = signal<TrainingReport | null>(null);
  renderedMarkdown = signal<SafeHtml>('');

  constructor() {
    effect(() => {
      this.refreshTrigger();
      this.load();
    });
  }

  private load(): void {
    this.reportService.getReports('demo').pipe(
      switchMap(reports => reports.length > 0
        ? of(reports)
        : this.http.get<TrainingReport[]>('assets/demo-data/reports.json')),
      catchError(() => this.http.get<TrainingReport[]>('assets/demo-data/reports.json'))
    ).subscribe(reports => {
      if (reports.length > 0) {
        const r = reports[0];
        this.report.set(r);
        const html = marked.parse(r.markdownContent) as string;
        this.renderedMarkdown.set(this.sanitizer.bypassSecurityTrustHtml(html));
      }
    });
  }
}
