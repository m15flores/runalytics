import { Chart } from 'chart.js';

// Shared state across all chart instances on the page
let hoveredXValue: number | null = null;
const chartRegistry = new Set<any>();

function syncTooltip(chart: any, xValue: number): void {
  const xScale = chart.scales['x'];
  if (!xScale) return;

  const pixelX = xScale.getPixelForValue(xValue);
  if (pixelX < xScale.left || pixelX > xScale.right) {
    chart.tooltip.setActiveElements([], { x: 0, y: 0 });
    return;
  }

  // Find nearest data index for the given time value
  const dataset = chart.data.datasets[0];
  if (!dataset?.data?.length) return;

  let nearestIdx = 0;
  let minDist = Infinity;
  (dataset.data as any[]).forEach((point: any, i: number) => {
    const dist = Math.abs(point.x - xValue);
    if (dist < minDist) {
      minDist = dist;
      nearestIdx = i;
    }
  });

  chart.tooltip.setActiveElements(
    [{ datasetIndex: 0, index: nearestIdx }],
    { x: pixelX, y: chart.scales['y'].bottom }
  );
}

export function registerChartPlugins(): void {
  Chart.register({
    id: 'vertical-crosshair',

    afterInit: (chart: any) => {
      chartRegistry.add(chart);

      // Fix: when a parent tab is hidden (display:none), the canvas loses its
      // dimensions. ResizeObserver fires when the canvas becomes visible again,
      // allowing Chart.js to recalculate the correct size.
      const observer = new ResizeObserver(() => {
        if (chart.canvas.offsetWidth > 0 && chart.canvas.offsetHeight > 0) {
          chart.resize();
        }
      });
      observer.observe(chart.canvas);
      chart._resizeObserver = observer;
    },

    beforeDestroy: (chart: any) => {
      chart._resizeObserver?.disconnect();
      chartRegistry.delete(chart);
    },

    afterEvent: (chart: any, args: any) => {
      const type = args.event.type;
      const xScale = chart.scales['x'];
      if (!xScale) return;

      if (type === 'mousemove' && args.event.x != null) {
        hoveredXValue = xScale.getValueForPixel(args.event.x);
      } else if (type === 'mouseout') {
        hoveredXValue = null;
      }

      // Sync tooltip + crosshair on all other charts
      chartRegistry.forEach((c: any) => {
        if (c === chart) return;
        if (hoveredXValue !== null) {
          syncTooltip(c, hoveredXValue);
        } else {
          c.tooltip.setActiveElements([], { x: 0, y: 0 });
        }
        c.update('none');
      });
    },

    afterDraw: (chart: any) => {
      const active = chart.tooltip?._active;
      if (!active?.length) return;

      const pixelX = active[0].element.x;
      const ctx = chart.ctx;
      const top = chart.scales['y'].top;
      const bottom = chart.scales['y'].bottom;

      ctx.save();
      ctx.beginPath();
      ctx.moveTo(pixelX, top);
      ctx.lineTo(pixelX, bottom);
      ctx.lineWidth = 1;
      ctx.strokeStyle = 'rgba(120, 120, 120, 0.5)';
      ctx.setLineDash([4, 4]);
      ctx.stroke();
      ctx.restore();
    }
  });
}

export function formatElapsedTime(seconds: number): string {
  const m = Math.floor(seconds / 60);
  const s = Math.round(seconds % 60);
  return `${m}:${s.toString().padStart(2, '0')}`;
}