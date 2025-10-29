import { z } from 'zod';
import { ProficiencyLevel, ApprovalStatus } from '../../app/enums/domain_enums';

const measurementTypes = ['QUALITY', 'QUANTITY', 'FREQUENCY', 'BINARY'] as const;

export const indicatorSchema = z.object({
  title: z.string().min(3, 'Title must be at least 3 characters'),
  description: z.string().optional(),
  observabilityLevel: z.enum(Object.values(ProficiencyLevel) as [string, ...string[]]),
  measurementType: z.enum(measurementTypes),
  weight: z.number().min(0).max(1),
  examples: z.string().optional(),
  counterExamples: z.string().optional(),
  isActive: z.boolean(),
  approvalStatus: z.enum(Object.values(ApprovalStatus) as [string, ...string[]]),
});
