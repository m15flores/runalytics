import { Component, computed, input } from '@angular/core';
import { ChartModule } from 'primeng/chart';
import { ActivitySample } from '../../../core/models/metrics/activity-sample.model';
import { formatElapsedTime } from '../../../core/chart/chart-plugins';

@Component({
  selector: 'app-elevation-chart',
  standalone: true,
  imports: [ChartModule],
  templateUrl: './elevation-chart.component.html',
  styleUrl: './elevation-chart.component.scss'
})
export class ElevationChartComponent {

  samples = input<ActivitySample[]>([]);
  startedAt = input<string>('');

  private firstTime = computed(() => {
    const data = this.samples().filter(s => s.altitude != null);
    if (data.length === 0) return 0;
    return this.startedAt()
      ? new Date(this.startedAt()).getTime()
      : new Date(data[0].timestamp).getTime();
  });

  private maxElapsed = computed(() => {
    const data = this.samples().filter(s => s.altitude != null);
    if (data.length === 0) return undefined;
    const last = data[data.length - 1];
    return Math.round((new Date(last.timestamp).getTime() - this.firstTime()) / 1000);
  });

  chartData = computed(() => {
    const data = this.samples().filter(s => s.altitude != null);
    if (data.length === 0) return { datasets: [] };
    const firstTime = this.firstTime();
    return {
      datasets: [{
        label: 'Elevation',
        data: data.map(s => ({
          x: Math.round((new Date(s.timestamp).getTime() - firstTime) / 1000),
          y: Math.round(s.altitude!)
        })),
        fill: true,
        borderColor: '#10B981',
        backgroundColor: 'rgba(16, 185, 129, 0.15)',
        pointRadius: 0,
        tension: 0.4,
      }]
    };
  });

  chartOptions = computed(() => ({
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
          label: (ctx: any) => `${Math.round(ctx.parsed.y)} m`
        }
      }
    },
    scales: {
      x: {
        type: 'linear',
        max: this.maxElapsed(),
        ticks: {
          maxTicksLimit: 8,
          maxRotation: 0,
          callback: (value: any) => formatElapsedTime(value),
        },
        grid: { display: false },
      },
      y: {
        title: { display: true, text: 'm' },
      }
    }
  }));
}