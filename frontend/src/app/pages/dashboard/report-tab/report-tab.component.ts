import { Component, OnInit, signal, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { marked } from 'marked';
import { CardModule } from 'primeng/card';
import { TrainingReport } from '../../../core/models/report/training-report.model';

@Component({
  selector: 'app-report-tab',
  standalone: true,
  imports: [CardModule],
  templateUrl: './report-tab.component.html',
  styleUrl: './report-tab.component.scss'
})
export class ReportTabComponent implements OnInit {

  private http = inject(HttpClient);
  private sanitizer = inject(DomSanitizer);

  report = signal<TrainingReport | null>(null);
  renderedMarkdown = signal<SafeHtml>('');

  ngOnInit(): void {
    this.http.get<TrainingReport[]>('assets/demo-data/reports.json')
      .subscribe(reports => {
        if (reports.length > 0) {
          const r = reports[0];
          this.report.set(r);
          const html = marked.parse(r.markdownContent) as string;
          this.renderedMarkdown.set(this.sanitizer.bypassSecurityTrustHtml(html));
        }
      });
  }
}