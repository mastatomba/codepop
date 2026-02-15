# Architecture Overview

## System Components

```
┌─────────────┐         ┌──────────────────┐         ┌─────────────┐
│             │         │                  │         │             │
│   React     │◄───────►│   Spring Boot    │◄───────►│   Ollama    │
│  Frontend   │  REST   │     Backend      │  HTTP   │     LLM     │
│             │         │                  │         │             │
└─────────────┘         └────────┬─────────┘         └─────────────┘
                                 │
                                 │ JPA
                                 ▼
                        ┌─────────────────┐
                        │                 │
                        │     SQLite      │
                        │    Database     │
                        │                 │
                        └─────────────────┘
```

## Component Responsibilities

### Frontend (React)
- User interface for quiz interaction
- Topic input and validation display
- Question presentation with multiple-choice options
- Answer tracking and score calculation
- Session tracking via browser sessionStorage (asked questions per topic)
- Toast notifications for errors
- Client-side routing (React Router v7)
- REST API calls via Axios
- Built with Vite for fast development

### Backend (Spring Boot)
- REST API endpoints for quiz operations
- Topic validation against database-stored topics
- Question cache management in SQLite
- Question filtering via `excludeQuestionIds` query parameter (frontend-managed)
- LLM integration via QuizMaster interface (OllamaQuizMaster implementation)
- Response parsing with markdown support
- Transaction isolation for long-running LLM calls
- Test database separation for fast, predictable tests
- Error handling and recovery

### Database (SQLite)
- Store generated questions by topic
- Question metadata (difficulty, explanations)
- No session data stored (handled by frontend sessionStorage)
- Simple schema with minimal tables

### LLM (Ollama / Cloud API)
- Generate quiz questions from prompts
- Categorize difficulty levels
- Create plausible distractors
- Generate explanations when appropriate
- Uses **delimiter-based format** for reliable parsing
- Returns structured responses with markdown support for code blocks

## Data Flow

### Quiz Request Flow

1. **User Input**
   - User enters topic: "Quiz me about Java Spring Boot"
   - Frontend validates format

2. **Session Check**
   - Frontend loads previously asked question IDs from browser sessionStorage
   - Key: `codepop_asked_questions`, format: `{ "Java Spring Boot": [1, 2, 3, ...] }`

3. **API Request**
   - Frontend sends request with excludeQuestionIds: `/api/quiz/Java%20Spring%20Boot?excludeQuestionIds=1,2,3`

4. **Topic Validation**
   - Backend checks against database-stored topics
   - Accepts if contains valid root (e.g., "Java")
   - Rejects if no valid root found

5. **Question Retrieval**
   - Backend parses user input to extract main topic and optional subtopic
   - Uses fuzzy matching to find questions (e.g., "Java record" matches subtopic "records")
   - **If subtopic specified**: Only returns questions matching that subtopic (may be 0)
   - **If no subtopic specified**: Returns all questions for main topic
   - Filters out `excludeQuestionIds` from request parameter
   - If 5+ questions available, randomizes and selects 5

6. **LLM Generation** (if needed)
   - If fewer than 5 questions available (including when subtopic has no questions), calls QuizMaster
   - Uses OllamaQuizMaster with qwen2.5-coder:7b model (temperature 0.8 for creativity)
   - **Response Format:** Uses delimiter-based format for robust parsing
     ```
     ### QUESTION 1 ###
     DIFFICULTY: easy
     QUESTION: What is the output of `print(2 + 2)`?
     OPTION: 4 [CORRECT]
     OPTION: 22
     OPTION: TypeError
     OPTION: SyntaxError
     EXPLANATION: The print function outputs the result of 2+2
     ```
   - **Why Delimiters?** More LLM-friendly than JSON (95%+ parsing success vs 75%)
   - **Code snippets**: No escaping needed - markdown code blocks work natively
   - **JSON Fallback**: Maintains backward compatibility with JSON responses
   - **Duplicate Prevention Strategy:**
     - Backend extracts question texts from ALL questions in database (not just available ones)
     - Passes complete list to LLM (no limit on number of questions shown)
     - Prompt includes: "Avoid generating questions similar to these N existing ones: [list]"
     - Prompt explicitly instructs: "Generate questions on DIFFERENT aspects of [topic] that are NOT covered above"
   - **Transaction isolation:** LLM call happens OUTSIDE any database transaction (can take 5-10 seconds)
   - Backend validates and stores in SQLite with appropriate subtopic (short write transaction < 100ms)

7. **Quiz Assembly**
   - Backend selects up to 5 questions (randomized if > 5 available)
   - Returns quiz JSON to frontend with question IDs

8. **Session Update**
   - Frontend saves question IDs to sessionStorage: `{ "Java Spring Boot": [1, 2, 3, 4, 5, 6, 7, ...] }`
   - Persists across page reloads, cleared when browser tab/window closes

9. **User Interaction**
   - Frontend displays questions one by one
   - User selects answers
   - Frontend calculates score
   - Frontend displays results with explanations

10. **Retake Same Topic**
    - Frontend loads excludeQuestionIds [1-7] from sessionStorage
    - Backend filters those out, sees < 5 available → calls LLM for new questions
    - User gets different questions

## API Endpoints

### GET /api/quiz/{topic}

Get quiz questions for a specific topic with optional subtopic filtering.

**Topic Matching Strategy:**
- Main topics (19 total): Java, Python, React, JavaScript, etc.
- User input "Java records" parses to: main_topic="Java", subtopic="records"
- Fuzzy matching: "Java record" matches questions with subtopic containing "record"
- **Subtopic specified but not found**: Returns 0 questions, LLM generates new ones for that subtopic
- **No subtopic specified**: Returns all questions for the main topic regardless of subtopic
- This allows both broad ("Java") and specific ("Java records") queries

**Parameters:**
- `topic` (path variable, required) - Topic name, optionally with subtopic (e.g., "Java", "Java records", "React hooks")
- `excludeQuestionIds` (query parameter, optional) - Comma-separated list of question IDs to exclude from the quiz

**Response:** Returns a `QuizDTO` with up to 5 questions

**Examples:**
```bash
GET /api/quiz/Java                      # All Java questions (any subtopic)
GET /api/quiz/Java%20records            # Only Java "records" subtopic
GET /api/quiz/Java%20streams            # Java "streams" subtopic (LLM generates if none exist)
GET /api/quiz/React%20hooks             # Only React "hooks" subtopic
GET /api/quiz/Java?excludeQuestionIds=1,2  # Java questions excluding 1 and 2
```

**Status Codes:**
- `200 OK` - Quiz successfully retrieved
- `400 Bad Request` - Topic not found in database
- `500 Internal Server Error` - Unexpected error

## Database Schema

### topics
- `id` BIGINT PRIMARY KEY AUTO_INCREMENT
- `name` VARCHAR(100) UNIQUE NOT NULL
- `category` VARCHAR(50) - 'backend', 'frontend', 'mobile'
- `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP

### questions
- `id` BIGINT PRIMARY KEY AUTO_INCREMENT
- `topic_id` BIGINT FOREIGN KEY REFERENCES topics(id)
- `question_text` TEXT NOT NULL
- `subtopic` VARCHAR(100) - Optional subtopic for filtering (e.g., "records", "hooks")
- `difficulty` VARCHAR(20) NOT NULL - 'EASY', 'MEDIUM', 'HARD'
- `explanation` TEXT NULL - Optional explanation
- `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP

### question_options
- `id` BIGINT PRIMARY KEY AUTO_INCREMENT
- `question_id` BIGINT FOREIGN KEY REFERENCES questions(id)
- `option_text` TEXT NOT NULL
- `is_correct` BOOLEAN NOT NULL - Exactly one true per question
- Each question has 4-6 options

## Technology Integration

### Vite Build Tool
- Fast HMR (Hot Module Replacement)
- Optimized production builds
- Development proxy to backend API
- ES modules support

### React Router v7
- Client-side routing
- Route structure: `/` (home), `/quiz/:topic` (quiz page), `/results` (results)
- Declarative routing configuration

### Axios HTTP Client
- REST API communication with backend
- Centralized API service layer (`src/services/api.js`)
- `quizApi.getQuiz(topic, excludeQuestionIds)` handles query parameter serialization
- Request/response interceptors
- Error handling

### Browser Session Storage
- Native browser API for session-scoped data storage
- Data persists across page reloads (unlike React state)
- Automatically cleared when tab/window closes (unlike localStorage)
- Utility module: `src/utils/sessionStorage.js`
- Stores asked question IDs per topic for exclusion on retake

### Spring AI - Ollama
- Auto-configuration for Ollama ChatClient
- Uses qwen2.5-coder:7b model with temperature override (0.8 for quiz generation)
- **Delimiter-based response format** for robust parsing (95%+ success rate)
- Prompt engineering for structured output with markdown support
- Handles multi-line code snippets without escaping issues
- JSON fallback for backward compatibility
- Response cleanup and validation
- Excluded from test environment via @Profile("!test")

### Spring Data JPA
- Repository pattern for data access
- Entity mapping for questions and sessions
- Query methods for filtering by topic/difficulty

### SQLite JDBC Driver
- Lightweight database connectivity
- File-based storage (codepop.db for production, test-codepop.db for tests)
- No server setup required
- **Transaction limitations:** Cannot handle long-running transactions (5+ seconds)
- **Solution:** TransactionalOperations pattern isolates LLM calls from database transactions

## Deployment Architecture

### Development
- Frontend: Vite dev server on localhost:5173 (StrictMode enabled - may see duplicate API calls)
- Backend: Spring Boot on localhost:8080
- Ollama: Local instance on localhost:11434 with qwen2.5-coder:7b model
- Database: Local SQLite file (codepop.db)
- Test Database: Separate test-codepop.db for integration tests
- Frontend proxies `/api` requests to backend
- **Note:** React StrictMode causes double-rendering in dev - use `npm run build && npm run preview` to test without StrictMode

### Production Preview (Local)
- Frontend: `npm run build && npm run preview` (port 4173)
- Backend: Spring Boot on localhost:8080
- **StrictMode disabled** - no duplicate API calls
- Tests production build locally before deployment

### Production (Option 1: Hybrid)
- Frontend: Vercel/Netlify (static hosting)
- Backend: Railway/fly.io (containerized)
- LLM: Cloud API (Groq/Hugging Face)
- Database: SQLite file in backend container

### Production (Option 2: Local Demo)
- All components run locally
- Demo video/screenshots for GitHub
- Setup instructions in README

## Security Considerations

- No user authentication (POC scope)
- Rate limiting on quiz generation (prevent abuse)
- Input validation on all endpoints
- Sanitize LLM outputs before storage
- CORS configuration for frontend access
- No sensitive data in database

## Scalability Notes

Current architecture is optimized for POC/demo:
- SQLite suitable for single-user or low traffic
- **SQLite limitation:** Cannot handle long-running write transactions (LLM calls take 5-10 seconds)
- **Mitigation:** TransactionalOperations pattern isolates LLM from transactions
- Session data grows linearly with users
- LLM calls are rate-limited by provider (Ollama local instance)

For production scale:
- **Migrate to PostgreSQL/MySQL** - Better concurrency and transaction handling
- Add Redis for session caching
- Queue LLM requests with message broker (async job processing)
- Load balance backend instances
- CDN for frontend assets
- Consider serverless LLM APIs for scalability (vs local Ollama)
