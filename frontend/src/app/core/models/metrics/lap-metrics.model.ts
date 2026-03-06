export interface LapMetrics {
  lapNumber: number;
  lapName: string;
  intensity: string;
  distance: number;         // meters
  duration: number;         // seconds
  averagePace: number;      // sec/km
  maxPace: number;          // sec/km
  averageHeartRate: number;
  maxHeartRate: number;
  minHeartRate: number;
  averageCadence: number;
  totalAscent: number;
  totalDescent: number;
}
