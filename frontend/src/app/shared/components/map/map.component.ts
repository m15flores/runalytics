import { Component, AfterViewInit, OnDestroy, ElementRef, input, effect, viewChild } from '@angular/core';
import * as L from 'leaflet';
import { ActivitySample } from '../../../core/models/metrics/activity-sample.model';

@Component({
  selector: 'app-map',
  standalone: true,
  templateUrl: './map.component.html',
  styleUrl: './map.component.scss'
})
export class MapComponent implements AfterViewInit, OnDestroy {

  private readonly mapContainerRef = viewChild.required<ElementRef<HTMLDivElement>>('mapContainer');

  samples = input<ActivitySample[]>([]);

  private map: L.Map | null = null;
  private polyline: L.Polyline | null = null;
  private initialized = false;

  constructor() {
    effect(() => {
      const data = this.samples();
      if (this.initialized && data.length > 0) {
        this.drawPolyline(data);
      }
    });
  }

  ngAfterViewInit(): void {
    this.initMap();
    this.initialized = true;
    const data = this.samples();
    if (data.length > 0) {
      this.drawPolyline(data);
    }
  }

  ngOnDestroy(): void {
    this.map?.remove();
    this.map = null;
  }

  private initMap(): void {
    this.map = L.map(this.mapContainerRef().nativeElement, { zoomControl: true });

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '© <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
      maxZoom: 19,
    }).addTo(this.map);

    this.map.setView([40.4154, -3.6832], 14);
  }

  private drawPolyline(samples: ActivitySample[]): void {
    if (!this.map) return;

    const coords: L.LatLngTuple[] = samples
      .filter(s => s.latitude != null && s.longitude != null)
      .map(s => [s.latitude as number, s.longitude as number]);

    if (coords.length === 0) return;

    if (this.polyline) {
      this.polyline.remove();
    }

    this.polyline = L.polyline(coords, {
      color: '#3B82F6',
      weight: 4,
      opacity: 0.85,
    }).addTo(this.map);

    this.map.fitBounds(this.polyline.getBounds(), { padding: [20, 20] });
  }
}