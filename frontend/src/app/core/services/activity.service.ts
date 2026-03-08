import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ActivityService {
  private readonly http = inject(HttpClient);

  uploadFit(file: File, userId: string): Observable<{ userId: string }> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('userId', userId);
    formData.append('device', 'garmin');
    formData.append('source', 'fit');
    return this.http.post<{ userId: string }>(`${environment.activityServiceUrl}/activities/fit`, formData);
  }
}
