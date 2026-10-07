import api from './api';

// Auth
export const authService = {
  register: (data) => api.post('/auth/register', data),
  login: (data) => api.post('/auth/login', data),
  getMe: () => api.get('/auth/me'),
};

// Topics
export const topicService = {
  getAll: () => api.get('/topics'),
  getById: (id) => api.get(`/topics/${id}`),
  create: (data) => api.post('/topics', data),
  update: (id, data) => api.put(`/topics/${id}`, data),
  delete: (id) => api.delete(`/topics/${id}`),
  markAsLearned: (id, data) => api.post(`/topics/${id}/learn`, data),
  getTodayQueue: () => api.get('/topics/revision/today'),
  recalculatePriorities: () => api.post('/topics/recalculate-priorities'),
};

// Revision
export const revisionService = {
  getSession: (topicId) => api.get(`/revision/${topicId}`),
  complete: (topicId, data, quizScore) => {
    const params = quizScore !== undefined ? { quizScore } : {};
    return api.post(`/revision/${topicId}/complete`, data, { params });
  },
  getHistory: (topicId) => api.get(`/revision/${topicId}/history`),
  getAll: () => api.get('/revision'),
};

// Quiz
export const quizService = {
  getQuestions: (topicId, limit = 10) => api.get(`/quiz/${topicId}/questions`, { params: { limit } }),
  getAllQuestions: (topicId) => api.get(`/quiz/${topicId}/all-questions`),
  submit: (data) => api.post('/quiz/submit', data),
  getHistory: (topicId) => api.get(`/quiz/${topicId}/history`),
  addQuestion: (topicId, data) => api.post(`/quiz/${topicId}/questions`, data),
};

// Analytics
export const analyticsService = {
  getDashboardStats: () => api.get('/analytics'),
  getSubjectPerformance: () => api.get('/analytics/subjects'),
  getWeakTopics: () => api.get('/analytics/weak-topics'),
  getRecommendations: () => api.get('/analytics/recommendations'),
  getQuizPerformance: (days = 30) => api.get('/analytics/quiz-performance', { params: { days } }),
  getRevisionActivity: (days = 30) => api.get('/analytics/revision-activity', { params: { days } }),
  getMistakes: () => api.get('/analytics/mistakes'),
  getMistakeSummary: () => api.get('/analytics/mistakes/summary'),
};
