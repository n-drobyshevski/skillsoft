'use server'

import { revalidateTag } from 'next/cache';

export async function revalidateCompetencyTags(competencyId?: string) {
  revalidateTag('competencies');
  if (competencyId) {
    revalidateTag(`competency-${competencyId}`);
  }
}

export async function revalidateIndicatorTags(competencyId: string, indicatorId?: string) {
  revalidateTag(`indicators-${competencyId}`);
  if (indicatorId) {
    revalidateTag(`indicator-${competencyId}-${indicatorId}`);
  }
}

export async function revalidateQuestionTags(competencyId: string, behavioralIndicatorId: string, questionId?: string) {
  revalidateTag(`questions-${competencyId}-${behavioralIndicatorId}`);
  if (questionId) {
    revalidateTag(`question-${questionId}`);
  }
}