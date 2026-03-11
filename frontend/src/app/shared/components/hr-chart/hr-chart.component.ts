import { Component, computed, input } from '@angular/core';
import { ChartModule } from 'primeng/chart';
import { ActivitySample } from '../../../core/models/metrics/activity-sample.model';
import { formatElapsedTime } from '../../../core/chart/chart-plugins';

@Component({
  selector: 'app-hr-chart',
  standalone: true,
  imports: [ChartModule],
  templateUrl: './hr-chart.component.html',
  styleUrl: './hr-chart.component.scss'
})
export class HrChartComponent {

  samples = input<ActivitySample[]>([]);

  chartData = computed(() => {
    const data = this.samples().filter(s => s.heartRate != null);
    if (data.length === 0) return { datasets: [] };
    const firstTime = new Date(data[0].timestamp).getTime();
    return {
      datasets: [{
        label: 'Heart Rate',
        data: data.map(s => ({
          x: Math.round((new Date(s.timestamp).getTime() - firstTime) / 1000),
          y: s.heartRate
        })),
        fill: true,
        borderColor: '#EF4444',
        backgroundColor: 'rgba(239, 68, 68, 0.1)',
        pointRadius: 0,
        tension: 0.3,
      }]
    };
  });

  chartOptions = {
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
          label: (ctx: any) => `${ctx.parsed.y} bpm`
        }
      }
    },
    scales: {
      x: {
        type: 'linear',
        ticks: {
          maxTicksLimit: 8,
          maxRotation: 0,
          callback: (value: any) => formatElapsedTime(value),
        },
        grid: { display: false },
      },
      y: {
        title: { display: true, text: 'bpm' },
        min: 60,
      }
    }
  };
}