import { ApprovalStatus, CompetencyCategory, DifficultyLevel, ProficiencyLevel  } from "../enums/domain_enums";
import { IndicatorMeasurementType, QuestionType } from "../types/competency";


export interface StandardCodeMapping {
  code: string;
  name: string;
  confidence: "LOW" | "MODERATE" | "HIGH" | "VERIFIED";
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

export interface DashboardStats {
  totalCompetencies: number;
  totalBehavioralIndicators: number;
  totalAssessmentQuestions: number;
  competenciesByCategory: { [key: string]: number };
  competenciesByLevel: { [key: string]: number };
  averageIndicatorsPerCompetency: number;
}