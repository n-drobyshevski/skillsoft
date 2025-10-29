import { z } from 'zod';
import { CompetencyCategory, ProficiencyLevel, ApprovalStatus } from '../enums/domain_enums';

export const competencySchema = z.object({
  name: z.string().min(3, 'Name must be at least 3 characters'),
  description: z.string().optional(),
  category: z.enum(Object.values(CompetencyCategory) as [string, ...string[]]),
  level: z.enum(Object.values(ProficiencyLevel) as [string, ...string[]]),
  isActive: z.boolean(),
  approvalStatus: z.enum(Object.values(ApprovalStatus) as [string, ...string[]]),
});
