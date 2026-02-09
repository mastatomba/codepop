import axios from 'axios';

const api = axios.create({
  baseURL: '/api',
  headers: {
    'Content-Type': 'application/json',
  },
});

export const quizApi = {
  getQuiz: (topic) => api.get(`/quiz/${topic}`),
};

export default api;
