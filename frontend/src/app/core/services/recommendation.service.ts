import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Recommendation } from '../models/recommendation/recommendation.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class RecommendationService {
  private readonly http = inject(HttpClient);

  getRecommendations(userId: string): Observable<Recommendation[]> {
    return this.http.get<Recommendation[]>(`${environment.coachServiceUrl}/api/recommendations/users/${userId}`);
  }
}
