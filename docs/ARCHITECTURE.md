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
- Toast notifications for errors
- Client-side routing (React Router v7)
- REST API calls via Axios
- Built with Vite for fast development

### Backend (Spring Boot)
- REST API endpoints for quiz operations
- Topic validation against hardcoded list
- Question cache management in SQLite
- LLM integration and prompt engineering
- Response parsing and validation
- Session tracking
- Error handling and recovery

### Database (SQLite)
- Store generated questions by topic
- Question metadata (difficulty, explanations)
- Session data (questions shown per session)
- Simple schema with minimal tables

### LLM (Ollama / Cloud API)
- Generate quiz questions from prompts
- Categorize difficulty levels
- Create plausible distractors
- Generate explanations when appropriate
- Return structured JSON responses

## Data Flow

### Quiz Request Flow

1. **User Input**
   - User enters topic: "Quiz me about Java Spring Boot"
   - Frontend validates format and sends to backend

2. **Topic Validation**
   - Backend checks against hardcoded root topics list
   - Accepts if contains valid root (e.g., "Java")
   - Rejects if no valid root found

3. **Question Retrieval**
   - Backend checks SQLite for existing questions on topic
   - Filters out questions already shown in current session
   - If 5 questions available, skip to step 5

4. **LLM Generation** (if needed)
   - Backend builds prompt with topic and requirements
   - Calls LLM API with structured output request
   - LLM returns JSON with questions, options, answers, difficulty
   - Backend validates and stores in SQLite

5. **Quiz Assembly**
   - Backend selects 5 questions (cached + new)
   - Marks questions as "shown" for current session
   - Returns quiz JSON to frontend

6. **User Interaction**
   - Frontend displays questions one by one
   - User selects answers
   - Frontend calculates score
   - Frontend displays results with explanations

## API Endpoints (Planned)

### POST /api/quiz/generate
Request quiz for a topic
```json
{
  "topic": "React hooks"
}
```

### GET /api/quiz/:quizId
Retrieve quiz details

### POST /api/quiz/:quizId/submit
Submit answers and get score

### GET /api/topics
Get list of supported root topics

## Database Schema (Planned)

### questions table
- id (primary key)
- topic (indexed)
- question_text
- difficulty (easy/medium/hard)
- created_at

### question_options table
- id (primary key)
- question_id (foreign key)
- option_text
- is_correct
- explanation (optional)

### sessions table
- id (primary key)
- session_token
- created_at
- expires_at

### session_questions table
- session_id (foreign key)
- question_id (foreign key)
- shown_at

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
- Centralized API service layer
- Request/response interceptors
- Error handling

### Spring AI - Ollama
- Auto-configuration for Ollama client
- Prompt templates for structured output
- JSON response parsing
- Retry and timeout handling

### Spring Data JPA
- Repository pattern for data access
- Entity mapping for questions and sessions
- Query methods for filtering by topic/difficulty

### SQLite JDBC Driver
- Lightweight database connectivity
- File-based storage (codepop.db)
- No server setup required

## Deployment Architecture

### Development
- Frontend: Vite dev server on localhost:5173
- Backend: Spring Boot on localhost:8080
- Ollama: Local instance on localhost:11434
- Database: Local SQLite file
- Frontend proxies `/api` requests to backend

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
- Session data grows linearly with users
- LLM calls are rate-limited by provider

For production scale:
- Migrate to PostgreSQL/MySQL
- Add Redis for session caching
- Queue LLM requests with message broker
- Load balance backend instances
- CDN for frontend assets
