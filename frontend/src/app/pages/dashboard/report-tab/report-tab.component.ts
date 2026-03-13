import { Component, computed, effect, inject, input, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { catchError, of, switchMap } from 'rxjs';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { marked } from 'marked';
import { TrainingReport } from '../../../core/models/report/training-report.model';
import { ReportService } from '../../../core/services/report.service';

interface WeeklySummary {
  totalActivities?: number;
  totalKm?: number;
  totalDuration?: number;
  averagePace?: number;
  trend?: string;
}

@Component({
  selector: 'app-report-tab',
  standalone: true,
  imports: [],
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

  parsedSummary = computed<WeeklySummary | null>(() => {
    const r = this.report();
    if (!r?.summaryJson) return null;
    try {
      return JSON.parse(r.summaryJson) as WeeklySummary;
    } catch {
      return null;
    }
  });

  formattedDuration = computed(() => {
    const s = this.parsedSummary();
    if (!s?.totalDuration) return '—';
    const h = Math.floor(s.totalDuration / 3600);
    const m = Math.floor((s.totalDuration % 3600) / 60);
    return h > 0 ? `${h}h ${m}m` : `${m}m`;
  });

  formattedPace = computed(() => {
    const s = this.parsedSummary();
    if (!s?.averagePace) return '—';
    const mins = Math.floor(s.averagePace / 60);
    const secs = s.averagePace % 60;
    return `${mins}:${String(secs).padStart(2, '0')}`;
  });

  trendIcon = computed(() => {
    const trend = this.parsedSummary()?.trend;
    if (trend === 'improving') return 'pi-arrow-up-right';
    if (trend === 'declining') return 'pi-arrow-down-right';
    return 'pi-minus';
  });

  constructor() {
    effect(() => {
      this.refreshTrigger();
      this.load();
    });
  }

  formatKm(km?: number): string {
    if (km == null) return '—';
    return km.toFixed(1);
  }

  formatTrend(trend?: string): string {
    if (!trend) return 'Stable';
    return trend.charAt(0).toUpperCase() + trend.slice(1);
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
