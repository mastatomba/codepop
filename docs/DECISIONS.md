# Design Decisions

This document outlines key architectural and design decisions made for CodePop.

## Technology Stack

### Backend: Spring Boot (Java)
**Decision:** Use Spring Boot instead of Python/FastAPI  
**Rationale:** Team has stronger Java expertise; Spring Boot provides mature ecosystem for REST APIs, database access, and LLM integration through Spring AI.

### Frontend: React
**Decision:** React over Thymeleaf or other frameworks  
**Rationale:** Better suited for interactive quiz experience with smooth transitions, real-time feedback, and toast notifications. Cleaner separation between frontend and backend.

### Build Tool: Vite
**Decision:** Vite over Create React App or other bundlers  
**Rationale:** 
- Extremely fast dev server startup and HMR
- Optimized production builds
- Modern tooling with ESM support
- Minimal configuration needed for POC
- Create React App is being deprecated

### Routing: React Router v7
**Decision:** React Router over Next.js or other frameworks  
**Rationale:** 
- Next.js is overkill for this POC (adds SSR, API routes, complex routing)
- React Router provides simple client-side routing
- Perfect for SPA consuming existing REST API
- Lightweight and focused on routing only

### Database: SQLite
**Decision:** SQLite for local storage  
**Rationale:** Simple file-based database, perfect for POC. No separate database server needed. Easy to deploy and backup.

### LLM: Ollama (local) with cloud API fallback
**Decision:** Primary support for local Ollama, with option for cloud APIs  
**Rationale:** 
- Ollama avoids cloud costs and API limits for development
- Cloud APIs (Groq, Hugging Face) available as fallback for demos
- Spring AI provides unified interface for both

## Architecture

### Mediated Backend Pattern
**Decision:** Backend mediates all interactions; LLM has no direct access to database  
**Flow:**
1. User requests quiz via frontend
2. Backend validates topic against allowed list
3. Backend checks SQLite for existing questions
4. Backend calls LLM only if new questions needed
5. Backend stores new questions in SQLite
6. Backend returns quiz to frontend

**Rationale:** Cleaner separation of concerns, easier to test, better control over LLM calls and caching.

### Question Generation: Training Data Only
**Decision:** LLM generates questions from training knowledge, no web search  
**Rationale:** Simpler implementation for POC. Training data sufficient for coding topics. Can add web search in future if needed.

## Features

### Fixed Question Count: 5 questions
**Decision:** Fixed at 5 questions per quiz for POC  
**Rationale:** Keeps quizzes short and focused. Reduces LLM load. Can make configurable later.

### Difficulty Levels: 3 categories
**Decision:** Easy, Medium, Hard  
**Rationale:** Simple and clear. More granular levels (5+) would be harder for LLM to consistently categorize.

### Session-Based Tracking
**Decision:** Browser session lifetime, no user accounts  
**Rationale:** Minimal complexity for POC. Users can retake quizzes across sessions. User accounts can be added later if needed.

### Topic Validation: Hardcoded List
**Decision:** Maintain hardcoded list of 19 root topics across backend/frontend/mobile  
**Topics:** Java, Python, Node.js, C#, Go, Rust, PHP, JavaScript, TypeScript, React, Vue, Angular, HTML, CSS, Svelte, Swift, Kotlin, React Native, Flutter  
**Rationale:** 
- Simple validation without extra LLM calls
- Covers most popular development areas
- Accepts subtopics naturally (e.g., "Java Spring Boot")
- Prevents off-topic queries

### Explanations: Optional, per question
**Decision:** LLM decides whether to include explanation; one global explanation per question (not per answer)  
**Rationale:** Flexibility for LLM to explain difficult concepts. Single explanation is cleaner than per-answer explanations.

## Error Handling

### Graceful Degradation
**Decision:** Show partial quiz if LLM fails but local questions exist; show retry message if complete failure  
**Rationale:** Better user experience than complete failure. Toast notifications keep user informed without blocking UI.

## Project Structure

### Monorepo
**Decision:** Single repository with `codepop-backend/` and `codepop-frontend/` folders  
**Rationale:** 
- Easier to manage for POC
- Shared documentation and issues
- Simpler deployment setup
- Version frontend and backend together
- Clear separation between frontend and backend code

## Future Considerations

- User accounts and progress tracking
- Configurable question count
- More granular difficulty levels
- Web search for question generation
- Support for non-coding topics
- Mobile app versions
