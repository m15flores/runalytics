import { Component, effect, inject, input, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { catchError, of, switchMap } from 'rxjs';
import { CardModule } from 'primeng/card';
import { TagModule } from 'primeng/tag';
import { Recommendation } from '../../../core/models/recommendation/recommendation.model';
import { RecommendationService } from '../../../core/services/recommendation.service';

@Component({
  selector: 'app-coach-tab',
  standalone: true,
  imports: [CardModule, TagModule],
  templateUrl: './coach-tab.component.html',
  styleUrl: './coach-tab.component.scss'
})
export class CoachTabComponent {

  private recommendationService = inject(RecommendationService);
  private http = inject(HttpClient);

  refreshTrigger = input<number>(0);
  recommendations = signal<Recommendation[]>([]);

  constructor() {
    effect(() => {
      this.refreshTrigger();
      this.load();
    });
  }

  private load(): void {
    this.recommendationService.getRecommendations('demo').pipe(
      switchMap(data => data.length > 0
        ? of(data)
        : this.http.get<Recommendation[]>('assets/demo-data/recommendations.json')),
      catchError(() => this.http.get<Recommendation[]>('assets/demo-data/recommendations.json'))
    ).subscribe(data => this.recommendations.set(data));
  }

  verdictSeverity(verdict: string): 'success' | 'warn' | 'danger' | 'secondary' {
    switch (verdict) {
      case 'VALID':            return 'success';
      case 'PARTIALLY_VALID': return 'warn';
      case 'INVALID':         return 'danger';
      default:                return 'secondary';
    }
  }

  prioritySeverity(priority: string): 'danger' | 'warn' | 'info' {
    switch (priority) {
      case 'HIGH':   return 'danger';
      case 'MEDIUM': return 'warn';
      default:       return 'info';
    }
  }

  formatType(type: string): string {
    return type.replace(/_/g, ' ');
  }
}
