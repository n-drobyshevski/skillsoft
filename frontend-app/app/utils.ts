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
// Helper functions
 const competencyCategoryToIcon = (category: string) => {
    const icons = {
      COGNITIVE: BrainCircuit,
      INTERPERSONAL: Users,
      LEADERSHIP: GraduationCap,
      ADAPTABILITY: Binary,
      EMOTIONAL_INTELLIGENCE: Heart,
      COMMUNICATION: MessageSquare,
      COLLABORATION: UsersRound,
      CRITICAL_THINKING: LightbulbIcon,
      TIME_MANAGEMENT: Clock,
    };
    const IconComponent = icons[category as keyof typeof icons] || Award;
    return React.createElement(IconComponent, { className: "h-4 w-4" });
  };
  const competencyProficiencyLevelToColor = (level: ProficiencyLevel): string => {
    const colors: { [key in ProficiencyLevel]: string } = {
      [ProficiencyLevel.NOVICE]:
        "border-red-500/20 text-red-700 bg-red-50/90 dark:bg-red-950/90 dark:text-red-200 dark:border-red-400/30",
      [ProficiencyLevel.DEVELOPING]:
        "border-amber-500/20 text-amber-700 bg-amber-50/90 dark:bg-amber-950/90 dark:text-amber-200 dark:border-amber-400/30",
      [ProficiencyLevel.PROFICIENT]:
        "border-emerald-500/20 text-emerald-700 bg-emerald-50/90 dark:bg-emerald-950/90 dark:text-emerald-200 dark:border-emerald-400/30",
      [ProficiencyLevel.ADVANCED]:
        "border-blue-500/20 text-blue-700 bg-blue-50/90 dark:bg-blue-950/90 dark:text-blue-200 dark:border-blue-400/30",
      [ProficiencyLevel.EXPERT]:
        "border-violet-500/20 text-violet-700 bg-violet-50/90 dark:bg-violet-950/90 dark:text-violet-200 dark:border-violet-400/30",
    };
    return colors[level];
  };

const levelToNumber = (level: string): number => {
  const levels: { [key: string]: number } = {
    NOVICE: 1,
    DEVELOPING: 2,
    PROFICIENT: 3,
    ADVANCED: 4,
    EXPERT: 5,
  };
  return levels[level] || 1;
};

const levelToColor = (level: string): string => {
  const colors: { [key: string]: string } = {
    NOVICE:
      "border-red-600/30 text-red-700 bg-red-50/90 dark:bg-red-950/90 dark:text-red-200 dark:border-red-400/30",
    DEVELOPING:
      "border-amber-600/30 text-amber-800 bg-amber-50/90 dark:bg-amber-950/90 dark:text-amber-200 dark:border-amber-400/30",
    PROFICIENT:
      "border-emerald-600/30 text-emerald-700 bg-emerald-50/90 dark:bg-emerald-950/90 dark:text-emerald-200 dark:border-emerald-400/30",
    ADVANCED:
      "border-blue-600/30 text-blue-700 bg-blue-50/90 dark:bg-blue-950/90 dark:text-blue-200 dark:border-blue-400/30",
    EXPERT:
      "border-violet-600/30 text-violet-700 bg-violet-50/90 dark:bg-violet-950/90 dark:text-violet-200 dark:border-violet-400/30",
  };
  return colors[level] || colors["NOVICE"];
};


const questionTypeToIcon = (category: string) => {
  const icons = {
    MULTIPLE_CHOICE: ListTodo,
    SITUATIONAL_JUDGMENT: Gavel,
    LIKERT_SCALE: ChartColumn,
    OPEN_ENDED: Pencil,
    TRUE_FALSE: BadgeQuestionMark,
  };
  const IconComponent =
    icons[category as keyof typeof icons] || BadgeQuestionMark;
  return React.createElement(IconComponent, { className: "h-4 w-4" });
};

const questionDifficultyToColor = (difficulty: string): string => {
  const colors: { [key: string]: string } = {
    BEGINNER:
      "border-emerald-500/20 text-emerald-700 bg-emerald-50/90 dark:bg-emerald-950/90 dark:text-emerald-200 dark:border-emerald-400/30",
    INTERMEDIATE:
      "border-amber-500/20 text-amber-700 bg-amber-50/90 dark:bg-amber-950/90 dark:text-amber-200 dark:border-amber-400/30",
    ADVANCED:
      "border-red-500/20 text-red-700 bg-red-50/90 dark:bg-red-950/90 dark:text-red-200 dark:border-red-400/30",
  };
  return colors[difficulty] || colors["BEGINNER"];
};

// Helper functions
const biLevelToColor = (level: string): string => {
	const colors: { [key: string]: string } = {
		NOVICE:
			"border-red-500/20 text-red-700 bg-red-50/90 dark:bg-red-950/90 dark:text-red-200 dark:border-red-400/30",
		DEVELOPING:
			"border-amber-500/20 text-amber-700 bg-amber-50/90 dark:bg-amber-950/90 dark:text-amber-200 dark:border-amber-400/30",
		PROFICIENT:
			"border-emerald-500/20 text-emerald-700 bg-emerald-50/90 dark:bg-emerald-950/90 dark:text-emerald-200 dark:border-emerald-400/30",
		ADVANCED:
			"border-blue-500/20 text-blue-700 bg-blue-50/90 dark:bg-blue-950/90 dark:text-blue-200 dark:border-blue-400/30",
		EXPERT:
			"border-violet-500/20 text-violet-700 bg-violet-50/90 dark:bg-violet-950/90 dark:text-violet-200 dark:border-violet-400/30",
	};
	return colors[level] || colors["NOVICE"];
};
export { competencyCategoryToIcon, competencyProficiencyLevelToColor, levelToNumber, levelToColor, questionTypeToIcon, questionDifficultyToColor, biLevelToColor };