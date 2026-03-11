import { Component, computed, input } from '@angular/core';
import { ChartModule } from 'primeng/chart';
import { ActivitySample } from '../../../core/models/metrics/activity-sample.model';
import { formatElapsedTime } from '../../../core/chart/chart-plugins';

@Component({
  selector: 'app-pace-chart',
  standalone: true,
  imports: [ChartModule],
  templateUrl: './pace-chart.component.html',
  styleUrl: './pace-chart.component.scss'
})
export class PaceChartComponent {

  samples = input<ActivitySample[]>([]);
  startedAt = input<string>('');

  private firstTime = computed(() => {
    const data = this.samples().filter(s => s.speed != null && s.speed > 0.5);
    if (data.length === 0) return 0;
    return this.startedAt()
      ? new Date(this.startedAt()).getTime()
      : new Date(data[0].timestamp).getTime();
  });

  private maxElapsed = computed(() => {
    const data = this.samples().filter(s => s.speed != null && s.speed > 0.5);
    if (data.length === 0) return undefined;
    const last = data[data.length - 1];
    return Math.round((new Date(last.timestamp).getTime() - this.firstTime()) / 1000);
  });

  private paceData = computed(() => {
    const data = this.samples().filter(s => s.speed != null && s.speed > 0.5);
    if (data.length === 0) return [];
    const firstTime = this.firstTime();
    return data.map(s => ({
      x: Math.round((new Date(s.timestamp).getTime() - firstTime) / 1000),
      y: Math.round(1000 / s.speed!)
    }));
  });

  chartData = computed(() => {
    const data = this.paceData();
    if (data.length === 0) return { datasets: [] };
    return {
      datasets: [{
        label: 'Pace',
        data,
        fill: false,
        borderColor: '#3B82F6',
        pointRadius: 0,
        tension: 0.4,
      }]
    };
  });

  chartOptions = computed(() => {
    const paces = this.paceData().map(s => s.y);
    const minPace = paces.length > 0 ? Math.min(...paces) : 240;
    const maxPace = paces.length > 0 ? Math.max(...paces) : 480;

    return {
      responsive: true,
      maintainAspectRatio: false,
      interaction: {
        mode: 'index',
        intersect: false,
        axis: 'x',
      },
      plugins: {
        legend: { display: false },
        tooltip: {
          callbacks: {
            title: (items: any) => formatElapsedTime(items[0].parsed.x),
            label: (ctx: any) => this.formatPace(ctx.parsed.y)
          }
        }
      },
      scales: {
        x: {
          type: 'linear',
          max: this.maxElapsed(),
          grid: { display: false },
          ticks: {
            maxTicksLimit: 8,
            maxRotation: 0,
            callback: (value: any) => formatElapsedTime(value),
          }
        },
        y: {
          reverse: true,
          title: { display: true, text: 'min/km' },
          min: Math.max(0, minPace - 20),
          max: maxPace + 20,
          ticks: {
            callback: (value: any) => this.formatPace(value)
          },
        }
      }
    };
  });

  private formatPace(secPerKm: number): string {
    if (!secPerKm) return '';
    const min = Math.floor(secPerKm / 60);
    const sec = secPerKm % 60;
    return `${min}:${sec.toString().padStart(2, '0')}`;
  }
}