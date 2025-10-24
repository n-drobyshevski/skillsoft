export type CompetencyCategory = 
  | 'COGNITIVE'
  | 'INTERPERSONAL'
  | 'LEADERSHIP'
  | 'ADAPTABILITY'
  | 'EMOTIONAL_INTELLIGENCE'
  | 'COMMUNICATION'
  | 'COLLABORATION'
  | 'CRITICAL_THINKING'
  | 'TIME_MANAGEMENT';

export type ProficiencyLevel = 
  | 'NOVICE'
  | 'DEVELOPING'
  | 'PROFICIENT'
  | 'ADVANCED'
  | 'EXPERT';

export type ApprovalStatus = 
  | 'DRAFT'
  | 'PENDING_REVIEW'
  | 'APPROVED'
  | 'REJECTED'
  | 'ARCHIVED'
  | 'UNDER_REVISION';

export type IndicatorMeasurementType = 
  | 'QUALITY'
  | 'QUANTITY'
  | 'FREQUENCY'
  | 'BINARY';

export type QuestionType = 
  | 'MULTIPLE_CHOICE'
  | 'SINGLE_CHOICE'
  | 'TRUE_FALSE'
  | 'OPEN_ENDED'
  | 'SCENARIO_BASED'
  | 'LIKERT_SCALE'
  | 'SITUATIONAL_JUDGMENT';

export type DifficultyLevel = 
  | 'BASIC'
  | 'INTERMEDIATE'
  | 'ADVANCED'
  | 'EXPERT';

export interface StandardCodeMapping {
  code: string;
  name: string;
  confidence: 'LOW' | 'MODERATE' | 'HIGH' | 'VERIFIED';
}

export interface StandardCodes {
  ESCO?: StandardCodeMapping;
  ONET?: StandardCodeMapping;
  BIG_FIVE?: StandardCodeMapping;
  [key: string]: StandardCodeMapping | undefined;
}

export interface BehavioralIndicator {
  id: string;
  title: string;
  description?: string;
  observabilityLevel: ProficiencyLevel;
  measurementType: IndicatorMeasurementType;
  weight: number;
  examples?: string;
  counterExamples?: string;
  isActive: boolean;
  approvalStatus: ApprovalStatus;
  orderIndex: number;
}

export interface AssessmentQuestion {
  id: string;
  behavioralIndicatorId: string;
  questionText: string;
  questionType: QuestionType;
  answerOptions?: Array<{
    text?: string;
    label?: string;
    value?: number;
    score?: number;
    correct?: boolean;
    explanation?: string;
  }>;
  scoringRubric: string;
  timeLimit?: number;
  difficultyLevel: DifficultyLevel;
  isActive: boolean;
  orderIndex: number;
}

export interface Competency {
  id: string;
  name: string;
  description?: string;
  category: CompetencyCategory;
  level: ProficiencyLevel;
  standardCodes?: StandardCodes;
  isActive: boolean;
  approvalStatus: ApprovalStatus;
  behavioralIndicators?: BehavioralIndicator[];
  version: number;
  createdAt: string;
  lastModified: string;
}
