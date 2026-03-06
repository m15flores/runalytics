import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ActivityMetrics } from '../models/metrics/activity-metrics.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class MetricsService {
  private readonly http = inject(HttpClient);

  getActivityMetrics(activityId: string): Observable<ActivityMetrics> {
    return this.http.get<ActivityMetrics>(`${environment.metricsServiceUrl}/activities/${activityId}/metrics`);
  }
}
