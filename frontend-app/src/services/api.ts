import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

export const behavioralIndicatorsApi = {
  getAllIndicators: async (competencyId: string) => {
    try {
      const response = await api.get(`/competencies/${competencyId}/bi`);
      return response.data;
    } catch (error) {
      console.error('Error fetching behavioral indicators:', error);
      throw error;
    }
  },

  getIndicatorById: async (competencyId: string, indicatorId: string) => {
    try {
      const response = await api.get(`/competencies/${competencyId}/bi/${indicatorId}`);
      return response.data;
    } catch (error) {
      console.error('Error fetching behavioral indicator:', error);
      throw error;
    }
  },

  createIndicator: async (competencyId: string, data: any) => {
    try {
      const response = await api.post(`/competencies/${competencyId}/bi`, data);
      return response.data;
    } catch (error) {
      console.error('Error creating behavioral indicator:', error);
      throw error;
    }
  },

  updateIndicator: async (competencyId: string, indicatorId: string, data: any) => {
    try {
      const response = await api.put(`/competencies/${competencyId}/bi/${indicatorId}`, data);
      return response.data;
    } catch (error) {
      console.error('Error updating behavioral indicator:', error);
      throw error;
    }
  },

  deleteIndicator: async (competencyId: string, indicatorId: string) => {
    try {
      await api.delete(`/competencies/${competencyId}/bi/${indicatorId}`);
    } catch (error) {
      console.error('Error deleting behavioral indicator:', error);
      throw error;
    }
  },
};

export default api;