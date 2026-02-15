# CodePop - A Dynamic AI-powered quiz generator

## Description

I want to create a dynamic AI-powered quiz generator.
The user will be presented with a prompt "Quiz me about ..".
A quiz will be dynamically generated based on the topic that is provided by the user.
A LLM will be used to research the topic and will provide a structured JSON output that the app can consume containing a couple of questions in multiple-choice answer format.
The user will be presented with the quiz.
At the end the user will get a score.

### Implementation Refinements (2026-01-25)
- **Difficulty**: Each question is categorized into 3 difficulty levels (easy, medium, hard) by the LLM and displayed to the user.
- **Question count**: Fixed at 5 questions for this POC.
- **LLM**: Use a single LLM (the quiz master), likely a local model such as Ollama, communicating via JSON.
- **Architecture**: Backend (Spring Boot) mediates all interactions. Flow: User requests quiz → Backend checks SQLite for existing questions → Backend calls LLM if new questions needed → Backend stores new questions in SQLite → Backend returns quiz to frontend. The LLM generates questions from its training knowledge (no web search).
- **Topic validation**: Maintain a hardcoded list of popular backend/frontend/mobile root topics; accept subtopics/phrases that include them for flexibility.
  - Backend: Java, Python, Node.js, C#, Go, Rust, PHP
  - Frontend: JavaScript, TypeScript, React, Vue, Angular, HTML, CSS, Svelte
  - Mobile: Swift, Kotlin, React Native, Flutter
- **Local storage**: Store generated questions per topic in SQLite. Avoid repeating questions within a session (browser session lifetime); in future sessions, repeated questions are possible. No user accounts initially—just session-based tracking.
- **Explanations**: LLM decides per question; one global explanation per question (not per answer option).
- **Error handling**: If LLM fails but local questions exist, show partial quiz with toast notification. If complete failure, show toast message to try again later.
- **Backend**: Spring Boot (Java).
- **Frontend**: React, communicating with Spring Boot backend via REST APIs. Fully implemented with:
  - Homepage with topic input
  - Quiz page with linear progression and immediate feedback
  - Score breakdown with detailed review
  - Responsive design and accessibility features
  - 35 unit tests using Vitest + React Testing Library + MSW
- **Project structure**: Monorepo with two main folders:
  - `codepop-backend/` - Spring Boot application
  - `frontend/` - React application
  - Shared `README.md` and `docs/` at root level
- **Hosting**: Backend and LLM cannot be hosted on GitHub Pages; use platforms like Render, Railway, or fly.io for backend, and GitHub Pages/Vercel/Netlify for frontend. Free tiers may have resource limits.
- **Cloud LLM**: Optionally, a free cloud LLM can be used for demos, but expect usage limits.
- **Deployment options for POC demo**:
  - Option 1 (Hybrid - Recommended): Frontend on Vercel/Netlify (free), Backend on Railway ($5 credit then ~$5-10/month) or fly.io (free tier), Use free cloud LLM API (Groq, Hugging Face, OpenAI trial, or Anthropic Claude free tier) instead of Ollama
  - Option 2 (All-in-one): Railway or fly.io for both backend and Ollama (~$10-15/month for required resources)
  - Option 3 (Local only): Run everything locally, create demo video/screenshots for GitHub README

### Boundaries

- Only coding topics are allowed as input, so user input needs to be validated before the LLM is put to work.

### Prompt

"Quiz me about **X**"

### App behavior

1. The LLM researches topic **X**
2. The LLM generates **N** multiple-choice questions
3. Each question includes:
	- 1 correct answer
	- 3 plausible distractors
4. The user will answer question 1 to **N**
4. Show a score at the end

### Quiz Master Agent

Agent responsibilities:
- research coding topic
- generate factual, verifiable questions
- avoid ambiguous or opinion-based questions
- ensure exactly one correct answer

### Quiz JSON

```json
{
  "topic": "Quantum Computing",
  "difficulty": "medium",
  "questions": [
    {
      "question": "What is a qubit?",
      "options": [
        "A classical bit with error correction",
        "A quantum bit that can exist in superposition",
        "A particle used only in quantum communication",
        "A binary digit stored in a quantum computer"
      ],
      "correct_index": 1,
      "explanation": "A qubit can exist in a superposition of 0 and 1 simultaneously."
    }
  ]
}
```

## Testing

### Backend
- **Framework**: JUnit 5 + Mockito + Spring Boot Test
- **Coverage**: 29 tests (10 unit tests for service layer, 6 unit tests for controllers, 13 integration tests)
- **Test Types**:
  - Unit tests: `QuizServiceTest`, `QuizControllerTest`
  - Integration tests: `QuizControllerIntegrationTest` (end-to-end API tests)
- **Run**: `./mvnw test`

### Frontend
- **Framework**: Vitest + React Testing Library
- **Mocking**: MSW (Mock Service Worker) for API mocking at network level
- **Coverage**: 35 tests across 5 test suites
  - HomePage (6 tests): Form validation, navigation
  - QuizPage (9 tests): Loading, error handling, complete quiz flow
  - Question (7 tests): Rendering, feedback display
  - ProgressIndicator (4 tests): Progress calculation
  - ScoreBreakdown (9 tests): Score calculation, review display
- **Run**: `npm test` (watch mode) or `npm test -- --run` (CI mode)
- **UI**: `npm run test:ui` for visual test runner

### Testing Philosophy
- **Backend**: High coverage of business logic with fast unit tests and comprehensive integration tests for API contracts
- **Frontend**: Component-level tests with realistic API mocking to ensure user flows work end-to-end without hitting real backend
