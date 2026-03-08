import { Component, OnInit, signal, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { CardModule } from 'primeng/card';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ActivityMetrics } from '../../../core/models/metrics/activity-metrics.model';

@Component({
  selector: 'app-metrics-tab',
  standalone: true,
  imports: [CardModule, TableModule, TagModule],
  templateUrl: './metrics-tab.component.html',
  styleUrl: './metrics-tab.component.scss'
})
export class MetricsTabComponent implements OnInit {

  private http = inject(HttpClient);

  metrics = signal<ActivityMetrics | null>(null);

  ngOnInit(): void {
    this.http.get<ActivityMetrics>('assets/demo-data/metrics.json')
      .subscribe(data => this.metrics.set(data));
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
