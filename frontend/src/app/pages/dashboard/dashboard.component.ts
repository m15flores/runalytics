import { Component, ElementRef, OnInit, effect, inject, signal, viewChild } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { EMPTY, catchError, filter, switchMap, take, takeUntil, timer } from 'rxjs';
import { TabsModule } from 'primeng/tabs';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';
import { MetricsTabComponent } from './metrics-tab/metrics-tab.component';
import { ReportTabComponent } from './report-tab/report-tab.component';
import { CoachTabComponent } from './coach-tab/coach-tab.component';
import { ActivityService } from '../../core/services/activity.service';
import { MetricsService } from '../../core/services/metrics.service';
import { ActivityMetrics } from '../../core/models/metrics/activity-metrics.model';
import { environment } from '../../../environments/environment';
import { resizeAllCharts } from '../../core/chart/chart-plugins';

type UploadStatus = 'idle' | 'uploading' | 'success' | 'error';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [TabsModule, ButtonModule, TagModule, TooltipModule, MetricsTabComponent, ReportTabComponent, CoachTabComponent],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent implements OnInit {

  private activityService = inject(ActivityService);
  private metricsService = inject(MetricsService);
  private http = inject(HttpClient);
  private fileInputRef = viewChild.required<ElementRef<HTMLInputElement>>('fileInput');

  readonly allowUpload = environment.allowUpload;

  uploadStatus = signal<UploadStatus>('idle');
  latestMetrics = signal<ActivityMetrics | null>(null);
  refreshCounter = signal<number>(0);
  activeTab = signal<string>('0');

  constructor() {
    effect(() => {
      if (this.activeTab() === '0') {
        // Defer resize to let PrimeNG finish showing the tab panel before resizing
        requestAnimationFrame(() => resizeAllCharts());
      }
    });
  }

  ngOnInit(): void {
    this.loadLatestMetrics();
  }

  onUploadClick(): void {
    this.fileInputRef().nativeElement.click();
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;

    this.uploadStatus.set('uploading');
    this.activityService.uploadFit(file, 'demo').subscribe({
      next: () => {
        this.uploadStatus.set('success');
        input.value = '';
        setTimeout(() => this.uploadStatus.set('idle'), 4000);
        this.pollForNewMetrics();
      },
      error: () => {
        this.uploadStatus.set('error');
        input.value = '';
        setTimeout(() => this.uploadStatus.set('idle'), 4000);
      }
    });
  }

  private loadLatestMetrics(): void {
    this.metricsService.getLatestMetrics('demo').pipe(
      catchError(() => this.http.get<ActivityMetrics>('assets/demo-data/metrics.json'))
    ).subscribe({
      next: data => this.latestMetrics.set(data),
      error: () => {}
    });
  }

  private pollForNewMetrics(): void {
    const previousId = this.latestMetrics()?.activityId;

    timer(1000, 2000).pipe(
      switchMap(() => this.metricsService.getLatestMetrics('demo').pipe(
        catchError(() => EMPTY)
      )),
      filter(data => data.activityId !== previousId),
      take(1),
      takeUntil(timer(30000))
    ).subscribe(data => {
      this.latestMetrics.set(data);
      setTimeout(() => this.refreshCounter.update(n => n + 1), 3000);
    });
  }
}
