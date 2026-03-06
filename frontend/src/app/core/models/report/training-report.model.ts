export interface TrainingReport {
  id: string;
  userId: string;
  weekNumber: number;
  year: number;
  markdownContent: string;
  summaryJson: string;
  createdAt: string;
  triggerActivityId: string;
  athleteName: string;
  currentGoal: string;
}
