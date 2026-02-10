import { http, HttpResponse } from 'msw';

const BASE_URL = '/api';

export const handlers = [
  http.get(`${BASE_URL}/quiz/:topic`, ({ params }) => {
    const { topic } = params;

    if (topic === 'InvalidTopic123') {
      return HttpResponse.json(
        {
          error: 'Topic not found: InvalidTopic123',
          status: '404',
        },
        { status: 404 }
      );
    }

    const mockQuizData = {
      topic: decodeURIComponent(topic),
      totalQuestions: 3,
      questions: [
        {
          id: 1,
          text: 'What is a test question?',
          difficulty: 'EASY',
          explanation: 'This is a test explanation.',
          options: [
            { id: 1, text: 'Correct Answer', isCorrect: true },
            { id: 2, text: 'Wrong Answer 1', isCorrect: false },
            { id: 3, text: 'Wrong Answer 2', isCorrect: false },
            { id: 4, text: 'Wrong Answer 3', isCorrect: false },
          ],
        },
        {
          id: 2,
          text: 'What is another test question?',
          difficulty: 'MEDIUM',
          explanation: 'Another test explanation.',
          options: [
            { id: 5, text: 'Wrong Answer', isCorrect: false },
            { id: 6, text: 'Correct Answer', isCorrect: true },
            { id: 7, text: 'Wrong Answer', isCorrect: false },
          ],
        },
        {
          id: 3,
          text: 'What is a hard test question?',
          difficulty: 'HARD',
          explanation: null,
          options: [
            { id: 8, text: 'Wrong Answer', isCorrect: false },
            { id: 9, text: 'Wrong Answer', isCorrect: false },
            { id: 10, text: 'Correct Answer', isCorrect: true },
          ],
        },
      ],
    };

    return HttpResponse.json(mockQuizData);
  }),
];
