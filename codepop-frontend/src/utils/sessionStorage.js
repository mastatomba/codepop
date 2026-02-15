/**
 * Session storage utilities for tracking asked questions per topic
 */

const SESSION_STORAGE_KEY = 'codepop_asked_questions';

/**
 * Get all asked question IDs for a specific topic
 * @param {string} topic - The topic name
 * @returns {number[]} Array of question IDs
 */
export const getAskedQuestions = (topic) => {
  try {
    const data = sessionStorage.getItem(SESSION_STORAGE_KEY);
    if (!data) return [];

    const askedQuestions = JSON.parse(data);
    return askedQuestions[topic] || [];
  } catch (error) {
    console.error('Error reading asked questions from session storage:', error);
    return [];
  }
};

/**
 * Add question IDs to the asked questions for a topic
 * @param {string} topic - The topic name
 * @param {number[]} questionIds - Array of question IDs to add
 */
export const addAskedQuestions = (topic, questionIds) => {
  try {
    const data = sessionStorage.getItem(SESSION_STORAGE_KEY);
    const askedQuestions = data ? JSON.parse(data) : {};

    if (!askedQuestions[topic]) {
      askedQuestions[topic] = [];
    }

    // Add new IDs, avoiding duplicates
    const existingIds = new Set(askedQuestions[topic]);
    questionIds.forEach((id) => existingIds.add(id));
    askedQuestions[topic] = Array.from(existingIds);

    sessionStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(askedQuestions));
  } catch (error) {
    console.error('Error saving asked questions to session storage:', error);
  }
};

/**
 * Clear asked questions for a specific topic
 * @param {string} topic - The topic name
 */
export const clearAskedQuestions = (topic) => {
  try {
    const data = sessionStorage.getItem(SESSION_STORAGE_KEY);
    if (!data) return;

    const askedQuestions = JSON.parse(data);
    delete askedQuestions[topic];

    sessionStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(askedQuestions));
  } catch (error) {
    console.error(
      'Error clearing asked questions from session storage:',
      error
    );
  }
};

/**
 * Clear all asked questions (all topics)
 */
export const clearAllAskedQuestions = () => {
  try {
    sessionStorage.removeItem(SESSION_STORAGE_KEY);
  } catch (error) {
    console.error('Error clearing all asked questions:', error);
  }
};
