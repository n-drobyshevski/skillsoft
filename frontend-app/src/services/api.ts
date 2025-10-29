import { cache } from 'react';
import { revalidateCompetencyTags, revalidateIndicatorTags, revalidateQuestionTags } from '@/app/actions';
import { AssessmentQuestion, BehavioralIndicator, Competency } from '../../app/interfaces/domain-interfaces';

// const API_BASE_URL = "http://outstanding-presence.railway.internal:8080/api";
// const API_BASE_URL = "https://backend-production-263e.up.railway.app/api";
const API_BASE_URL = "https://" + process.env.NEXT_PUBLIC_API_URL + "/api";

// Input types for API operations
interface CompetencyInput {
    name: string;
    description?: string;
    category?: string;
    level?: string;
    [key: string]: unknown;
}

interface IndicatorInput {
    title: string;
    description?: string;
    competencyId?: string;
    level?: string;
    weight?: number;
    [key: string]: unknown;
}

interface QuestionInput {
    questionText: string;
    questionType: string;
    difficulty?: string;
    behavioralIndicatorId?: string;
    options?: string[];
    correctAnswer?: string;
    [key: string]: unknown;
}


// const API_BASE_URL = "https://localhost:8080/api";
// Types for API responses and errors
export interface ApiError extends Error {
    status?: number;
    code?: string;
}

interface ErrorResponse {
    message?: string;
    code?: string;
}

// Helper function to handle responses with proper error typing
async function handleResponse<T>(response: Response): Promise<T> {
    if (!response.ok) {
        const error: ApiError = new Error('API request failed for url: ' + response.url);
        error.status = response.status;
        
        try {
            const errorData = await response.json() as ErrorResponse;
            error.message = errorData.message || `HTTP error! status: ${response.status}`;
            error.code = errorData.code;
        } catch {
            error.message = `HTTP error! status: ${response.status}`;
        }
        throw error;
    }
    return response.json() as Promise<T>;
}

// API fetch wrapper with caching and revalidation
async function fetchApi<T>(
    endpoint: string,
    options: RequestInit & {
        tags?: string[];
        revalidate?: false | 0 | number;
        cache?: RequestCache;
    } = {}
): Promise<T> {
    const { tags = [], revalidate, cache = 'force-cache', ...fetchOptions } = options;
    // Note: Removed console.log for production security
    const response = await fetch(`${API_BASE_URL}${endpoint}`, {
        ...fetchOptions,
        headers: {
            'Content-Type': 'application/json',
            ...fetchOptions.headers,
        },
        next: {
            tags,
            revalidate,
        },
        cache,
        mode: 'cors',
        credentials: 'include',
    });

    return handleResponse<T>(response);
}

// Cached competencies fetcher
const getCompetenciesCached = cache(async () : Promise<Competency[] | null> => {
    return fetchApi('/competencies', {
        tags: ['competencies'],
        revalidate: 60, // Revalidate every minute
    });
});

export const competenciesApi = {
    getAllCompetencies: getCompetenciesCached,

    getCompetencyById: async (competencyId: string) : Promise<Competency | null> => {
        return fetchApi(`/competencies/${competencyId}`, {
            tags: [`competency-${competencyId}`],
            revalidate: 60,
        });
    },

    createCompetency: async (data: CompetencyInput): Promise<Competency> => {
        const result = await fetchApi<Competency>('/competencies', {
            method: 'POST',
            body: JSON.stringify(data),
            cache: 'no-store',
        });
        await revalidateCompetencyTags();
        return result;
    },

    updateCompetency: async (competencyId: string, data: CompetencyInput): Promise<Competency> => {
        const result = await fetchApi<Competency>(`/competencies/${competencyId}`, {
            method: 'PUT',
            body: JSON.stringify(data),
            cache: 'no-store',
        });
        await revalidateCompetencyTags(competencyId);
        return result;
    },

    deleteCompetency: async (competencyId: string) => {
        await fetchApi(`/competencies/${competencyId}`, {
            method: 'DELETE',
            cache: 'no-store',
        });
        await revalidateCompetencyTags(competencyId);
    },
};

// Cached behavioral indicators fetcher
const getIndicatorsCached = cache(async (competencyId: string) : Promise<BehavioralIndicator[] | null> => {
    return fetchApi(`/competencies/${competencyId}/bi`, {
        tags: [`indicators-${competencyId}`],
        revalidate: 60,
    });
});
const getAllIndicatorsCached = cache(
  async (): Promise<BehavioralIndicator[] | null> => {
    return fetchApi(`/behavioral-indicators`, {
      tags: [`indicators-all`],
      revalidate: 60,
    });
  }
);

export const behavioralIndicatorsApi = {
  getIndicators: getIndicatorsCached,
  getAllIndicators: getAllIndicatorsCached,

  getIndicatorById: async (indicatorId: string) : Promise<BehavioralIndicator | null> => {
    return fetchApi(`/behavioral-indicators/${indicatorId}`, {
      tags: [`indicator-${indicatorId}`],
      revalidate: 60,
    });
  },

  createIndicator: async (competencyId: string, data: IndicatorInput): Promise<BehavioralIndicator> => {
    const result = await fetchApi<BehavioralIndicator>(`/behavioral-indicators`, {
      method: "POST",
      body: JSON.stringify(data),
      cache: "no-store",
    });
    await revalidateIndicatorTags(competencyId);
    return result;
  },

  updateIndicator: async (
    competencyId: string,
    indicatorId: string,
    data: IndicatorInput
  ): Promise<BehavioralIndicator> => {
    const result = await fetchApi<BehavioralIndicator>(
      `/behavioral-indicators/${indicatorId}`,
      {
        method: "PUT",
        body: JSON.stringify(data),
        cache: "no-store",
      }
    );
    await revalidateIndicatorTags(competencyId, indicatorId);
    return result;
  },

  deleteIndicator: async (competencyId: string, indicatorId: string) => {
    await fetchApi(`/behavioral-indicators/${indicatorId}`, {
      method: "DELETE",
      cache: "no-store",
    });
    await revalidateIndicatorTags(competencyId, indicatorId);
  },
};

// Cached assessment questions fetcher
const getIndicatorQuestionsCached = cache(async (competencyId: string, behavioralIndicatorId: string) : Promise<AssessmentQuestion[] | null> => {
    return fetchApi(
      `/behavioral-indicators/${behavioralIndicatorId}/questions`,
      {
        tags: [`questions-${competencyId}-${behavioralIndicatorId}`],
        revalidate: 60,
      }
    );
});
// Cached ALL assessment questions fetcher
const getAllQuestionsCached = cache(async () : Promise<AssessmentQuestion[] | null> => {
    return fetchApi(
        `/questions`,
        {
            tags: [`questions-all`],
            revalidate: 60,
        }
    );
});
export const assessmentQuestionsApi = {
  getIndicatorQuestions: getIndicatorQuestionsCached,
  getAllQuestions: getAllQuestionsCached,

  getQuestionById: async (
    questionId: string
  ) : Promise<AssessmentQuestion | null> => {
    return fetchApi(
      `/questions/${questionId}`,
      {
        tags: [`question-${questionId}`],
        revalidate: 60,
      }
    );
  },

  createQuestion: async (
    competencyId: string,
    behavioralIndicatorId: string,
    data: QuestionInput
  ): Promise<AssessmentQuestion> => {
    const result = await fetchApi<AssessmentQuestion>(
      `/questions`,
      {
        method: "POST",
        body: JSON.stringify(data),
        cache: "no-store",
      }
    );
    await revalidateQuestionTags(competencyId, behavioralIndicatorId);
    return result;
  },

  updateQuestion: async (
    questionId: string,
    data: QuestionInput,
    competencyId: string,
    behavioralIndicatorId: string,
  ): Promise<AssessmentQuestion> => {
    const result = await fetchApi<AssessmentQuestion>(
      `/questions/${questionId}`,
      {
        method: "PUT",
        body: JSON.stringify(data),
        cache: "no-store",
      }
    );
    await revalidateQuestionTags(
      questionId,
      competencyId,
      behavioralIndicatorId
    );
    return result;
  },

  deleteQuestion: async (
    competencyId: string,
    behavioralIndicatorId: string,
    questionId: string
  ) => {
    await fetchApi(
      `/questions/${questionId}`,
      {
        method: "DELETE",
        cache: "no-store",
      }
    );
    await revalidateQuestionTags(
      questionId,
      competencyId,
      behavioralIndicatorId,
    );
  },
};
