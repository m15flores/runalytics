import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ActivityMetrics } from '../models/metrics/activity-metrics.model';
import { ActivitySample } from '../models/metrics/activity-sample.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class MetricsService {
  private readonly http = inject(HttpClient);

  getActivityMetrics(activityId: string): Observable<ActivityMetrics> {
    return this.http.get<ActivityMetrics>(`${environment.metricsServiceUrl}/activities/${activityId}/metrics`);
  }

  getLatestMetrics(userId: string): Observable<ActivityMetrics> {
    return this.http.get<ActivityMetrics>(`${environment.metricsServiceUrl}/activities/users/${userId}/latest`);
  }

  getSamples(activityId: string): Observable<ActivitySample[]> {
    return this.http.get<ActivitySample[]>(`${environment.metricsServiceUrl}/activities/${activityId}/samples`);
  }
}
