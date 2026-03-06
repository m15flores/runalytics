import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ActivityService {
  private readonly http = inject(HttpClient);

  uploadFit(file: File): Observable<{ activityId: string }> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<{ activityId: string }>(`${environment.activityServiceUrl}/activities`, formData);
  }
}
