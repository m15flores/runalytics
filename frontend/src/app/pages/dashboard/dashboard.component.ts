import { Component } from '@angular/core';
import { TabsModule } from 'primeng/tabs';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { MetricsTabComponent } from './metrics-tab/metrics-tab.component';
import { ReportTabComponent } from './report-tab/report-tab.component';
import { CoachTabComponent } from './coach-tab/coach-tab.component';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [TabsModule, ButtonModule, TagModule, MetricsTabComponent, ReportTabComponent, CoachTabComponent],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent {
  onUploadClick(): void {
    // TODO: trigger file input
  }
}
