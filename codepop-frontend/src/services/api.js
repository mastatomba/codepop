import axios from 'axios';

const api = axios.create({
  baseURL: '/api',
  headers: {
    'Content-Type': 'application/json',
  },
});

export const quizApi = {
  getQuiz: async (topic, excludeQuestionIds = []) => {
    try {
      const params =
        excludeQuestionIds.length > 0
          ? { excludeQuestionIds: excludeQuestionIds.join(',') }
          : {};
      const response = await api.get(`/quiz/${encodeURIComponent(topic)}`, {
        params,
      });
      return response.data;
    } catch (error) {
      if (error.response?.status === 404) {
        throw new Error(
          `Topic not found: "${topic}". Please try a different topic.`
        );
      }
      throw new Error(
        error.response?.data?.error || 'Failed to fetch quiz. Please try again.'
      );
    }
  },
};

export default api;
