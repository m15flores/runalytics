import { Component, OnInit, signal, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { CardModule } from 'primeng/card';
import { TagModule } from 'primeng/tag';
import { Recommendation } from '../../../core/models/recommendation/recommendation.model';

@Component({
  selector: 'app-coach-tab',
  standalone: true,
  imports: [CardModule, TagModule],
  templateUrl: './coach-tab.component.html',
  styleUrl: './coach-tab.component.scss'
})
export class CoachTabComponent implements OnInit {

  private http = inject(HttpClient);

  recommendations = signal<Recommendation[]>([]);

  ngOnInit(): void {
    this.http.get<Recommendation[]>('assets/demo-data/recommendations.json')
      .subscribe(data => this.recommendations.set(data));
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
