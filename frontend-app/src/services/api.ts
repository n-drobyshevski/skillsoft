import { cache } from 'react';
import { revalidateCompetencyTags, revalidateIndicatorTags, revalidateQuestionTags } from '@/app/actions';
import { AssessmentQuestion, BehavioralIndicator, Competency } from '../../app/interfaces/domain-interfaces';

const API_BASE_URL = "https://backend-production-a6b6.up.railway.app/api";

// const API_BASE_URL = "https://localhost:8080/api";
// Types
export interface ApiError extends Error {
    status?: number;
    code?: string;
}

// Helper function to handle responses
async function handleResponse<T>(response: Response): Promise<T> {
    if (!response.ok) {
        const error: ApiError = new Error('API request failed');
        error.status = response.status;
        try {
            const errorData = await response.json();
            error.message = errorData.message || `HTTP error! status: ${response.status}`;
            error.code = errorData.code;
        } catch {
            error.message = `HTTP error! status: ${response.status}`;
        }
        throw error;
    }
    return response.json();
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

    createCompetency: async (data: any) => {
        const result = await fetchApi('/competencies', {
            method: 'POST',
            body: JSON.stringify(data),
            cache: 'no-store',
        });
        await revalidateCompetencyTags();
        return result;
    },

    updateCompetency: async (competencyId: string, data: any) => {
        const result = await fetchApi(`/competencies/${competencyId}`, {
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

  getIndicatorById: async (competencyId: string, indicatorId: string) : Promise<BehavioralIndicator | null> => {
    return fetchApi(`/competencies/${competencyId}/bi/${indicatorId}`, {
      tags: [`indicator-${competencyId}-${indicatorId}`],
      revalidate: 60,
    });
  },

  createIndicator: async (competencyId: string, data: any) => {
    const result = await fetchApi(`/competencies/${competencyId}/bi`, {
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
    data: any
  ) => {
    const result = await fetchApi(
      `/competencies/${competencyId}/bi/${indicatorId}`,
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
    await fetchApi(`/competencies/${competencyId}/bi/${indicatorId}`, {
      method: "DELETE",
      cache: "no-store",
    });
    await revalidateIndicatorTags(competencyId, indicatorId);
  },
};

// Cached assessment questions fetcher
const getQuestionsCached = cache(async (competencyId: string, behavioralIndicatorId: string) : Promise<AssessmentQuestion[] | null> => {
    return fetchApi(
        `/competencies/${competencyId}/bi/${behavioralIndicatorId}/questions`,
        {
            tags: [`questions-${competencyId}-${behavioralIndicatorId}`],
            revalidate: 60,
        }
    );
});
// Cached ALL assessment questions fetcher
const getAllQuestionsCached = cache(async () : Promise<AssessmentQuestion[] | null> => {
    return fetchApi(
        `/assessment-questions`,
        {
            tags: [`questions-all`],
            revalidate: 60,
        }
    );
});
export const assessmentQuestionsApi = {
  getQuestions: getQuestionsCached,
  getAllQuestions: getAllQuestionsCached,

  getQuestionById: async (
    competencyId: string,
    behavioralIndicatorId: string,
    questionId: string
  ) : Promise<AssessmentQuestion | null> => {
    return fetchApi(
      `/competencies/${competencyId}/bi/${behavioralIndicatorId}/questions/${questionId}`,
      {
        tags: [`question-${questionId}`],
        revalidate: 60,
      }
    );
  },

  createQuestion: async (
    competencyId: string,
    behavioralIndicatorId: string,
    data: any
  ) => {
    const result = await fetchApi(
      `/competencies/${competencyId}/bi/${behavioralIndicatorId}/questions`,
      {
        method: "POST",
        body: JSON.stringify(data),
        cache: "no-store",
      }
    );
    await revalidateQuestionTags(competencyId, behavioralIndicatorId);
    return result;
  },

  deleteQuestion: async (
    competencyId: string,
    behavioralIndicatorId: string,
    questionId: string
  ) => {
    await fetchApi(
      `/competencies/${competencyId}/bi/${behavioralIndicatorId}/questions/${questionId}`,
      {
        method: "DELETE",
        cache: "no-store",
      }
    );
    await revalidateQuestionTags(
      competencyId,
      behavioralIndicatorId,
      questionId
    );
  },
};
