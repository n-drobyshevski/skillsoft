import React from "react";
import {
  BrainCircuit,
  Award,
  GraduationCap,
  Users,
  Heart,
  Clock,
  LightbulbIcon,
  UsersRound,
  ListTodo,
  Gavel,
  ChartColumn,
  Pencil,
  BadgeQuestionMark,
  Binary,
  MessageSquare
} from "lucide-react";
import { ProficiencyLevel } from "./enums/domain_enums";

// Common color constants to avoid duplication
const COMMON_COLORS = {
  NOVICE: "border-red-500/20 text-red-700 bg-red-50/90 dark:bg-red-950/90 dark:text-red-200 dark:border-red-400/30",
  DEVELOPING: "border-amber-500/20 text-amber-700 bg-amber-50/90 dark:bg-amber-950/90 dark:text-amber-200 dark:border-amber-400/30",
  PROFICIENT: "border-emerald-500/20 text-emerald-700 bg-emerald-50/90 dark:bg-emerald-950/90 dark:text-emerald-200 dark:border-emerald-400/30",
  ADVANCED: "border-blue-500/20 text-blue-700 bg-blue-50/90 dark:bg-blue-950/90 dark:text-blue-200 dark:border-blue-400/30",
  EXPERT: "border-violet-500/20 text-violet-700 bg-violet-50/90 dark:bg-violet-950/90 dark:text-violet-200 dark:border-violet-400/30",
} as const;

const LEVEL_COLORS_DARK = {
  NOVICE: "border-red-600/30 text-red-700 bg-red-50/90 dark:bg-red-950/90 dark:text-red-200 dark:border-red-400/30",
  DEVELOPING: "border-amber-600/30 text-amber-800 bg-amber-50/90 dark:bg-amber-950/90 dark:text-amber-200 dark:border-amber-400/30",
  PROFICIENT: "border-emerald-600/30 text-emerald-700 bg-emerald-50/90 dark:bg-emerald-950/90 dark:text-emerald-200 dark:border-emerald-400/30",
  ADVANCED: "border-blue-600/30 text-blue-700 bg-blue-50/90 dark:bg-blue-950/90 dark:text-blue-200 dark:border-blue-400/30",
  EXPERT: "border-violet-600/30 text-violet-700 bg-violet-50/90 dark:bg-violet-950/90 dark:text-violet-200 dark:border-violet-400/30",
} as const;
// Helper functions
const competencyCategoryToIcon = (category: string) => {
  const iconMap = new Map([
    ['COGNITIVE', BrainCircuit],
    ['INTERPERSONAL', Users],
    ['LEADERSHIP', GraduationCap],
    ['ADAPTABILITY', Binary],
    ['EMOTIONAL_INTELLIGENCE', Heart],
    ['COMMUNICATION', MessageSquare],
    ['COLLABORATION', UsersRound],
    ['CRITICAL_THINKING', LightbulbIcon],
    ['TIME_MANAGEMENT', Clock],
  ]);
  
  const IconComponent = iconMap.get(category) || Award;
  return React.createElement(IconComponent, { className: "h-4 w-4" });
};

const approvalStatusToColor = (status: string): string => {
  const colorMap = new Map([
    ['DRAFT', 'status-draft'],
    ['PENDING_REVIEW', 'status-pending-review'],
    ['APPROVED', 'status-approved'],
    ['REJECTED', 'status-rejected'],
    ['ARCHIVED', 'status-archived'],
    ['UNDER_REVISION', 'status-under-revision'],
  ]);
  
  return colorMap.get(status) || 'status-draft';
};

const competencyProficiencyLevelToColor = (level: ProficiencyLevel): string => {
  const colorMap = new Map([
    [ProficiencyLevel.NOVICE, COMMON_COLORS.NOVICE],
    [ProficiencyLevel.DEVELOPING, COMMON_COLORS.DEVELOPING],
    [ProficiencyLevel.PROFICIENT, COMMON_COLORS.PROFICIENT],
    [ProficiencyLevel.ADVANCED, COMMON_COLORS.ADVANCED],
    [ProficiencyLevel.EXPERT, COMMON_COLORS.EXPERT],
  ]);
  
  return colorMap.get(level) || COMMON_COLORS.NOVICE;
};

const levelToNumber = (level: string): number => {
  const levelMap = new Map([
    ['NOVICE', 1],
    ['DEVELOPING', 2],
    ['PROFICIENT', 3],
    ['ADVANCED', 4],
    ['EXPERT', 5],
  ]);
  
  return levelMap.get(level) || 1;
};

const levelToColor = (level: string): string => {
  const colorMap = new Map([
    ['NOVICE', LEVEL_COLORS_DARK.NOVICE],
    ['DEVELOPING', LEVEL_COLORS_DARK.DEVELOPING],
    ['PROFICIENT', LEVEL_COLORS_DARK.PROFICIENT],
    ['ADVANCED', LEVEL_COLORS_DARK.ADVANCED],
    ['EXPERT', LEVEL_COLORS_DARK.EXPERT],
  ]);
  
  return colorMap.get(level) || LEVEL_COLORS_DARK.NOVICE;
};


const questionTypeToIcon = (category: string) => {
  const iconMap = new Map([
    ['MULTIPLE_CHOICE', ListTodo],
    ['SITUATIONAL_JUDGMENT', Gavel],
    ['LIKERT_SCALE', ChartColumn],
    ['OPEN_ENDED', Pencil],
    ['TRUE_FALSE', BadgeQuestionMark],
  ]);
  
  const IconComponent = iconMap.get(category) || BadgeQuestionMark;
  return React.createElement(IconComponent, { className: "h-4 w-4" });
};

const questionDifficultyToColor = (difficulty: string): string => {
  const colorMap = new Map([
    ['BEGINNER', COMMON_COLORS.PROFICIENT], // Green for beginner
    ['INTERMEDIATE', COMMON_COLORS.DEVELOPING], // Amber for intermediate
    ['ADVANCED', COMMON_COLORS.NOVICE], // Red for advanced
  ]);
  
  return colorMap.get(difficulty) || COMMON_COLORS.PROFICIENT;
};

// Helper functions
const biLevelToColor = (level: string): string => {
	const colorMap = new Map([
		['NOVICE', COMMON_COLORS.NOVICE],
		['DEVELOPING', COMMON_COLORS.DEVELOPING],
		['PROFICIENT', COMMON_COLORS.PROFICIENT],
		['ADVANCED', COMMON_COLORS.ADVANCED],
		['EXPERT', COMMON_COLORS.EXPERT],
	]);
	
	return colorMap.get(level) || COMMON_COLORS.NOVICE;
};
export { competencyCategoryToIcon, competencyProficiencyLevelToColor, levelToNumber, levelToColor, questionTypeToIcon, questionDifficultyToColor, biLevelToColor, approvalStatusToColor };