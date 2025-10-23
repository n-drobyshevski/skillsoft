import type { Competency } from '../types/competency';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

export class CompetencyService {
  private static async fetchWithErrorHandling<T>(url: string, options?: RequestInit): Promise<T> {
    try {
      const response = await fetch(url, {
        headers: {
          'Content-Type': 'application/json',
          ...options?.headers,
        },
        ...options,
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error(`API Error: ${error}`);
      throw error;
    }
  }

  static async getAllCompetencies(): Promise<Competency[]> {
    return this.fetchWithErrorHandling<Competency[]>(`${API_BASE_URL}/competencies`);
  }

  static async getCompetencyById(id: string): Promise<Competency> {
    return this.fetchWithErrorHandling<Competency>(`${API_BASE_URL}/competencies/${id}`);
  }

  static async searchCompetencies(query: string): Promise<Competency[]> {
    const searchParams = new URLSearchParams({ q: query });
    return this.fetchWithErrorHandling<Competency[]>(`${API_BASE_URL}/competencies/search?${searchParams}`);
  }
}