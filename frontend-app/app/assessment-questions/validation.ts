import { z } from 'zod';
import { DifficultyLevel } from '../../app/enums/domain_enums';

const questionTypes = [
  'MULTIPLE_CHOICE',
  'SINGLE_CHOICE',
  'TRUE_FALSE',
  'OPEN_ENDED',
  'SCENARIO_BASED',
  'LIKERT_SCALE',
  'SITUATIONAL_JUDGMENT',
] as const;

export const questionSchema = z.object({
  questionText: z.string().min(10, 'Question text must be at least 10 characters'),
  questionType: z.enum(questionTypes),
  answerOptions: z.array(z.object({
    text: z.string().optional(),
    label: z.string().optional(),
    value: z.number().optional(),
    score: z.number().optional(),
    correct: z.boolean().optional(),
    explanation: z.string().optional(),
  })).optional(),
  scoringRubric: z.string(),
  timeLimit: z.preprocess((val) => Number(val), z.number().optional()),
  difficultyLevel: z.enum(Object.values(DifficultyLevel) as [string, ...string[]]),
  isActive: z.boolean(),
  orderIndex: z.preprocess((val) => Number(val), z.number()),
});
