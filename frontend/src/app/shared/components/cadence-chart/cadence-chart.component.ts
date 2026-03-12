import { Component, computed, input } from '@angular/core';
import { ChartModule } from 'primeng/chart';
import { ActivitySample } from '../../../core/models/metrics/activity-sample.model';
import { formatElapsedTime } from '../../../core/chart/chart-plugins';

@Component({
  selector: 'app-cadence-chart',
  standalone: true,
  imports: [ChartModule],
  templateUrl: './cadence-chart.component.html',
  styleUrl: './cadence-chart.component.scss'
})
export class CadenceChartComponent {

  samples = input<ActivitySample[]>([]);
  startedAt = input<string>('');

  private firstTime = computed(() => {
    const data = this.samples().filter(s => s.cadence != null);
    if (data.length === 0) return 0;
    return this.startedAt()
      ? new Date(this.startedAt()).getTime()
      : new Date(data[0].timestamp).getTime();
  });

  private maxElapsed = computed(() => {
    const data = this.samples().filter(s => s.cadence != null);
    if (data.length === 0) return undefined;
    const last = data[data.length - 1];
    return Math.round((new Date(last.timestamp).getTime() - this.firstTime()) / 1000);
  });

  chartData = computed(() => {
    const data = this.samples().filter(s => s.cadence != null);
    if (data.length === 0) return { datasets: [] };
    const firstTime = this.firstTime();
    return {
      datasets: [{
        label: 'Cadence',
        data: data.map(s => ({
          x: Math.round((new Date(s.timestamp).getTime() - firstTime) / 1000),
          y: s.cadence
        })),
        fill: true,
        borderColor: '#8B5CF6',
        backgroundColor: 'rgba(139, 92, 246, 0.1)',
        pointRadius: 0,
        tension: 0.3,
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
          label: (ctx: any) => `${ctx.parsed.y} spm`
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
        title: { display: true, text: 'spm' },
      }
    }
  }));
}