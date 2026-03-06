export type RecommendationType =
  | 'INJURY_PREVENTION'
  | 'OVERTRAINING'
  | 'ZONE_COMPLIANCE'
  | 'CARDIAC_DRIFT'
  | 'TRAINING_VOLUME'
  | 'RECOVERY'
  | 'WORKOUT_QUALITY'
  | 'PACE'
  | 'CADENCE'
  | 'HEART_RATE'
  | 'GOAL_PROGRESS'
  | 'NUTRITION';

export type Priority = 'HIGH' | 'MEDIUM' | 'LOW';

export type TrainingVerdict = 'VALID' | 'PARTIALLY_VALID' | 'INVALID';

export type TrainingPhase =
  | 'AEROBIC_BASE'
  | 'QUALITY_BLOCK'
  | 'TRANSITION'
  | 'TAPER'
  | 'RECOVERY';

export interface Recommendation {
  id: string;
  userId: string;
  reportId: string;
  type: RecommendationType;
  priority: Priority;
  content: string;
  rationale: string;
  verdict: TrainingVerdict;
  weekInCycle: number;
  trainingPhase: TrainingPhase;
  applied: boolean;
  createdAt: string;
  expiresAt: string;
}
