import axios from "axios";

const API_BASE_URL = "http://localhost:8080/api";

const api = axios.create({
	baseURL: API_BASE_URL,
	headers: {
		"Content-Type": "application/json",
	},
});

export const competenciesApi = {
	getAllCompetencies: async () => {
		try {
			const response = await api.get("/competencies");
			return response.data;
		} catch (error) {
			console.error("Error fetching competencies:", error);
			return error;
		}
	},
	getCompetencyById: async (competencyId: string) => {
		try {
			const response = await api.get(`/competencies/${competencyId}`);
			return response.data;
		} catch (error) {
			console.error("Error fetching competency:", error);
			throw error;
		}
	},
	createCompetency: async (data: any) => {
		try {
			const response = await api.post("/competencies", data);
			return response.data;
		} catch (error) {
			console.error("Error creating competency:", error);
			throw error;
		}
	},
	updateCompetency: async (competencyId: string, data: any) => {
		try {
			const response = await api.put(`/competencies/${competencyId}`, data);
			return response.data;
		} catch (error) {
			console.error("Error updating competency:", error);
			throw error;
		}
	},
	deleteCompetency: async (competencyId: string) => {
		try {
			await api.delete(`/competencies/${competencyId}`);
		} catch (error) {
			console.error("Error deleting competency:", error);
			throw error;
		}
	},
};

export const behavioralIndicatorsApi = {
	getAllIndicators: async (competencyId: string) => {
		try {
			const response = await api.get(`/competencies/${competencyId}/bi`);
			return response.data;
		} catch (error) {
			console.error("Error fetching behavioral indicators:", error);
			throw error;
		}
	},

	getIndicatorById: async (competencyId: string, indicatorId: string) => {
		try {
			const response = await api.get(
				`/competencies/${competencyId}/bi/${indicatorId}`,
			);
			return response.data;
		} catch (error) {
			console.error("Error fetching behavioral indicator:", error);
			throw error;
		}
	},

	createIndicator: async (competencyId: string, data: any) => {
		try {
			const response = await api.post(`/competencies/${competencyId}/bi`, data);
			return response.data;
		} catch (error) {
			console.error("Error creating behavioral indicator:", error);
			throw error;
		}
	},

	updateIndicator: async (
		competencyId: string,
		indicatorId: string,
		data: any,
	) => {
		try {
			const response = await api.put(
				`/competencies/${competencyId}/bi/${indicatorId}`,
				data,
			);
			return response.data;
		} catch (error) {
			console.error("Error updating behavioral indicator:", error);
			throw error;
		}
	},

	deleteIndicator: async (competencyId: string, indicatorId: string) => {
		try {
			await api.delete(`/competencies/${competencyId}/bi/${indicatorId}`);
		} catch (error) {
			console.error("Error deleting behavioral indicator:", error);
			throw error;
		}
	},
};

export const assessmentQuestionsApi = {
	getAllQuestions: async (
		competencyId: string,
		behavioralIndicatorId: string,
	) => {
		try {
			const response = await api.get(
				`/api/competencies/${competencyId}/bi/${behavioralIndicatorId}/questions`,
			);
			return response.data;
		} catch (error) {
			console.error("Error fetching assessment questions:", error);
			throw error;
		}
	},
	createQuestion: async (
		competencyId: string,
		behavioralIndicatorId: string,
		data: any,
	) => {
		try {
			const response = await api.post(
				`/competencies/${competencyId}/bi/${behavioralIndicatorId}/questions`,
				data,
			);
			return response.data;
		} catch (error) {
			console.error("Error creating assessment question:", error);
			throw error;
		}
	},
	deleteQuestion: async (
		competencyId: string,
		behavioralIndicatorId: string,
		questionId: string,
	) => {
		try {
			await api.delete(
				`/competencies/${competencyId}/bi/${behavioralIndicatorId}/questions/${questionId}`,
			);
		} catch (error) {
			console.error("Error deleting assessment question:", error);
			throw error;
		}
	},
	getQuestionById: async (
		competencyId: string,
		behavioralIndicatorId: string,
		questionId: string,
	) => {
		try {
			const response = await api.get(
				`/competencies/${competencyId}/bi/${behavioralIndicatorId}/questions/${questionId}`,
			);
			return response.data;
		} catch (error) {
			console.error("Error fetching assessment question:", error);
			throw error;
		}
	},
};

export default api;
