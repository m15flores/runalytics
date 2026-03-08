import { Component, ElementRef, inject, signal, viewChild } from '@angular/core';
import { TabsModule } from 'primeng/tabs';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { MetricsTabComponent } from './metrics-tab/metrics-tab.component';
import { ReportTabComponent } from './report-tab/report-tab.component';
import { CoachTabComponent } from './coach-tab/coach-tab.component';
import { ActivityService } from '../../core/services/activity.service';

type UploadStatus = 'idle' | 'uploading' | 'success' | 'error';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [TabsModule, ButtonModule, TagModule, MetricsTabComponent, ReportTabComponent, CoachTabComponent],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent {

  private activityService = inject(ActivityService);
  private fileInputRef = viewChild.required<ElementRef<HTMLInputElement>>('fileInput');

  uploadStatus = signal<UploadStatus>('idle');

  onUploadClick(): void {
    this.fileInputRef().nativeElement.click();
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;

    this.uploadStatus.set('uploading');
    this.activityService.uploadFit(file, 'demo').subscribe({
      next: () => {
        this.uploadStatus.set('success');
        input.value = '';
        setTimeout(() => this.uploadStatus.set('idle'), 4000);
      },
      error: () => {
        this.uploadStatus.set('error');
        input.value = '';
        setTimeout(() => this.uploadStatus.set('idle'), 4000);
      }
    });
  }
}