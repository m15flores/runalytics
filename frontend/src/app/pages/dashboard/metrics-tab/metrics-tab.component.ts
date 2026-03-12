import { Component, inject, input, signal, effect } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { catchError } from 'rxjs/operators';
import { CardModule } from 'primeng/card';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ActivityMetrics } from '../../../core/models/metrics/activity-metrics.model';
import { ActivitySample } from '../../../core/models/metrics/activity-sample.model';
import { MetricsService } from '../../../core/services/metrics.service';
import { MapComponent } from '../../../shared/components/map/map.component';
import { HrChartComponent } from '../../../shared/components/hr-chart/hr-chart.component';
import { PaceChartComponent } from '../../../shared/components/pace-chart/pace-chart.component';
import { CadenceChartComponent } from '../../../shared/components/cadence-chart/cadence-chart.component';
import { ElevationChartComponent } from '../../../shared/components/elevation-chart/elevation-chart.component';

@Component({
  selector: 'app-metrics-tab',
  standalone: true,
  imports: [CardModule, TableModule, TagModule, MapComponent, HrChartComponent, PaceChartComponent, CadenceChartComponent, ElevationChartComponent],
  templateUrl: './metrics-tab.component.html',
  styleUrl: './metrics-tab.component.scss'
})
export class MetricsTabComponent {

  private readonly metricsService = inject(MetricsService);
  private readonly http = inject(HttpClient);

  metrics = input<ActivityMetrics | null>(null);
  samples = signal<ActivitySample[]>([]);

  constructor() {
    effect(() => {
      const m = this.metrics();
      if (m) {
        this.loadSamples(m.activityId);
      }
    });
  }

  private loadSamples(activityId: string): void {
    this.metricsService.getSamples(activityId).pipe(
      catchError(() => this.http.get<ActivitySample[]>('assets/demo-data/samples.json'))
    ).subscribe({
      next: data => this.samples.set(data),
      error: () => {}
    });
  }

  formatPace(secPerKm: number | null): string {
    if (secPerKm == null) return '—';
    const min = Math.floor(secPerKm / 60);
    const sec = secPerKm % 60;
    return `${min}:${sec.toString().padStart(2, '0')}`;
  }

  formatDistance(meters: number): string {
    return (meters / 1000).toFixed(2);
  }

  formatDuration(seconds: number): string {
    const h = Math.floor(seconds / 3600);
    const m = Math.floor((seconds % 3600) / 60);
    const s = seconds % 60;
    if (h > 0) return `${h}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
    return `${m}:${s.toString().padStart(2, '0')}`;
  }

  intensitySeverity(intensity: string): 'success' | 'info' | 'secondary' | 'warn' {
    switch (intensity) {
      case 'warmup':   return 'info';
      case 'cooldown': return 'secondary';
      case 'active':   return 'success';
      default:         return 'secondary';
    }
  }
}