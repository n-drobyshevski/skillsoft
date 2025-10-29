'use server'

import { revalidatePath } from 'next/cache';

export async function revalidateCompetencyTags(competencyId?: string) {
  try {
    revalidatePath('/competencies');
    revalidatePath('/');
    if (competencyId) {
      revalidatePath(`/competencies/${competencyId}`);
    }
  } catch (error) {
    console.error('Error revalidating competency paths:', error);
  }
}

export async function revalidateIndicatorTags(competencyId: string, indicatorId?: string) {
  try {
    revalidatePath(`/competencies/${competencyId}`);
    revalidatePath('/behavioral-indicators');
    if (indicatorId) {
      revalidatePath(`/behavioral-indicators/${indicatorId}`);
    }
  } catch (error) {
    console.error('Error revalidating indicator paths:', error);
  }
}

export async function revalidateQuestionTags(questionId: string, competencyId?: string, behavioralIndicatorId?: string ) {
  try {
    revalidatePath('/assessment-questions');
    if (questionId) {
      revalidatePath(`/assessment-questions/${questionId}`);
    }
    if (competencyId) {
      revalidatePath(`/competencies/${competencyId}`);
    }
    if (behavioralIndicatorId) {
      revalidatePath(`/behavioral-indicators/${behavioralIndicatorId}`);
    }
  } catch (error) {
    console.error('Error revalidating question paths:', error);
  }
}