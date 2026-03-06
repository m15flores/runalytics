import { LapMetrics } from './lap-metrics.model';

export interface ActivityMetrics {
  activityId: string;
  userId: string;
  startedAt: string;

  totalDistance: number;        // meters
  totalDuration: number;        // seconds
  totalCalories: number;

  averagePace: number;          // sec/km
  maxPace: number;              // sec/km
  averageGAP: number;           // sec/km

  averageHeartRate: number;
  maxHeartRate: number;
  minHeartRate: number;
  hrZones: Record<string, number>;
  hrZonesPercentage: Record<string, number>;

  averageCadence: number;
  maxCadence: number;

  averagePower: number | null;
  normalizedPower: number | null;

  totalAscent: number;
  totalDescent: number;

  trainingEffect: number | null;

  laps: LapMetrics[];
  calculatedAt: string;
}
