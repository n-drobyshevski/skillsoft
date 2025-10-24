export type CompetencyCategory =
	| "COGNITIVE"
	| "INTERPERSONAL"
	| "LEADERSHIP"
	| "ADAPTABILITY"
	| "EMOTIONAL_INTELLIGENCE"
	| "COMMUNICATION"
	| "COLLABORATION"
	| "CRITICAL_THINKING"
	| "TIME_MANAGEMENT";

export type ProficiencyLevel =
	| "NOVICE"
	| "DEVELOPING"
	| "PROFICIENT"
	| "ADVANCED"
	| "EXPERT";

export type ApprovalStatus =
	| "DRAFT"
	| "PENDING_REVIEW"
	| "APPROVED"
	| "REJECTED"
	| "ARCHIVED"
	| "UNDER_REVISION";

export type IndicatorMeasurementType =
	| "QUALITY"
	| "QUANTITY"
	| "FREQUENCY"
	| "BINARY";

export type QuestionType =
	| "MULTIPLE_CHOICE"
	| "SINGLE_CHOICE"
	| "TRUE_FALSE"
	| "OPEN_ENDED"
	| "SCENARIO_BASED"
	| "LIKERT_SCALE"
	| "SITUATIONAL_JUDGMENT";

export type DifficultyLevel = "BASIC" | "INTERMEDIATE" | "ADVANCED" | "EXPERT";
