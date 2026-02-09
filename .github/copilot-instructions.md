# CodePop - Copilot Instructions

AI-powered quiz generator for coding topics. Monorepo with Spring Boot backend + React frontend.

## Build, Test, and Run Commands

### Backend (codepop-backend/)
```bash
# Run backend
./mvnw spring-boot:run

# Build
./mvnw clean install

# Run all tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=QuizServiceTest

# Skip tests during build
./mvnw clean install -DskipTests

# Code style checks
./mvnw spotless:check  # Check style violations
./mvnw spotless:apply  # Auto-fix violations
```

### Frontend (codepop-frontend/)
```bash
# Run frontend
npm run dev

# Build
npm run build

# Lint and formatting
npm run lint           # ESLint + Prettier checks
npm run format:check   # Check formatting only
npm run format:fix     # Auto-fix formatting

# Preview production build
npm run preview
```

## Code Style Enforcement

### Backend (Java)
- **Tool**: Spotless Maven plugin + Google Java Format
- **Style**: Google Java Style Guide (4-space indentation)
- **Enforcement**: Pre-commit hooks + `mvn verify` phase
- **Commands**: `mvn spotless:check` (verify), `mvn spotless:apply` (fix)

### Frontend (React)
- **Tools**: Prettier + ESLint integration
- **Style**: Standard Prettier defaults (semi-colons: true, single quotes, trailing commas: ES5, 80 char lines)
- **Enforcement**: Pre-commit hooks
- **Commands**: `npm run format:check` (verify), `npm run format:fix` (fix)

### Pre-commit Hooks
- Husky installed at project root
- Automatically runs style checks before commits
- Backend: `mvn spotless:check`
- Frontend: `npm run lint && npm run format:check`
- Bypass (emergencies only): `git commit --no-verify`

## Architecture Overview

### Request Flow
1. **Frontend** → sends quiz request for topic (e.g., "Java records")
2. **Backend** → validates topic exists in database (case-insensitive)
3. **Backend** → checks SQLite for existing questions:
   - If subtopic specified (e.g., "records"): filters to that subtopic using fuzzy LIKE matching
   - If no subtopic: returns all questions for main topic
   - Filters out `excludeQuestionIds` (session tracking)
4. **Backend** → calls LLM if < 5 questions available (QuizMaster interface)
5. **Backend** → stores new questions with subtopic in SQLite
6. **Backend** → randomizes (if > 5 available) and returns up to 5 questions
7. **Frontend** → displays quiz, tracks answers, calculates score

### Component Responsibilities
- **Frontend**: UI, session tracking (excludeQuestionIds), score calculation
- **Backend**: API mediation, topic/subtopic validation, LLM orchestration, caching
- **SQLite**: Question storage with main topics + subtopics
- **LLM (Ollama)**: Question generation via QuizMaster interface (stub implementation)

### Topic/Subtopic Architecture
- **19 main topics** stored in `topics` table: Java, Python, React, JavaScript, etc.
- **Subtopics** stored in `questions.subtopic` field: "records", "hooks", "streams", etc.
- User input "Java records" parses to: `main_topic="Java"` + `subtopic="records"`
- Fuzzy matching: "Java record" matches questions with subtopic containing "record"
- **Subtopic specified but not found**: Returns 0 questions → triggers LLM to generate new ones
- **No subtopic specified**: Returns all questions for main topic

## Key Conventions

### Backend (Java/Spring Boot)

**DTOs as Java Records**
- All DTOs (`QuizDTO`, `QuestionDTO`, `OptionDTO`) are implemented as Java records
- Immutable by default, concise syntax (88% less code than traditional classes)
- Works seamlessly with Jackson for JSON serialization
```java
public record QuizDTO(
    String topic,
    Integer totalQuestions,
    List<QuestionDTO> questions
) {}
```

**Entities with Lombok**
- All JPA entities (`Topic`, `Question`, `QuestionOption`) use Lombok annotations
- Use `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`
- **Avoid `@Data`** - includes `@EqualsAndHashCode` which causes JPA lazy loading issues
- Custom constructors allowed for domain logic
```java
@Entity
@Table(name = "topics")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Topic {
    // fields
}
```

**CORS Configuration**
- Global CORS config in `CorsConfig.java` (not `@CrossOrigin` on controllers)
- Configured via `cors.allowed-origins` property in `application.properties`
- Default: `http://localhost:5173` (Vite dev server)
- Supports multiple origins: comma-separated

**QuizMaster Interface**
- LLM integration via `QuizMaster` interface (not direct implementation)
- Current: `OllamaQuizMaster` stub (returns empty list)
- Interface contract: `generateQuestions(topic, count, existingQuestionTexts)`
- Receives existing question texts to avoid duplicates

**Repository Pattern**
- Use Spring Data JPA repositories for data access
- Case-insensitive queries via `findByNameIgnoreCase()`
- Fuzzy subtopic matching via `findByTopicAndSubtopicContainingIgnoreCase()`

**Seeded Data**
- Database auto-seeds on first startup
- 19 main topics (Backend: Java, Python, etc.; Frontend: React, Vue, etc.; Mobile: Swift, Kotlin, etc.)
- 6 sample questions (3 Java records, 3 React with different subtopics)

### Frontend (React/Vite)

**API Service Layer**
- Centralized in `src/services/api.js` using Axios
- Vite proxy forwards `/api/*` to `http://localhost:8080/api/*` in development

**Session Tracking**
- Frontend tracks `excludeQuestionIds` (shown questions)
- Passes to backend via query parameter: `/api/quiz/{topic}?excludeQuestionIds=1,2,3`
- Backend filters out excluded questions before randomization

**React Router v7**
- Routes: `/` (home), `/quiz/:topic` (quiz page), `/results` (results)
- Client-side routing only (SPA consuming REST API)

## API Endpoints

### GET /api/quiz/{topic}
Retrieve quiz questions for a topic with optional subtopic filtering.

**Parameters:**
- `topic` (path): Topic name with optional subtopic (e.g., "Java", "Java records", "React hooks")
- `excludeQuestionIds` (query): Comma-separated IDs to exclude

**Examples:**
```bash
# All Java questions (any subtopic)
GET /api/quiz/Java

# Java records subtopic only
GET /api/quiz/Java%20records

# React hooks subtopic (generates if none exist)
GET /api/quiz/React%20hooks

# Exclude questions 1 and 2
GET /api/quiz/Java?excludeQuestionIds=1,2
```

**Response (200):**
```json
{
  "topic": "Java records",
  "totalQuestions": 3,
  "questions": [
    {
      "id": 1,
      "text": "What keyword is used to define a record in Java?",
      "difficulty": "EASY",
      "explanation": "Java records use the 'record' keyword...",
      "options": [
        {"id": 1, "text": "record", "isCorrect": true},
        {"id": 2, "text": "class", "isCorrect": false}
      ]
    }
  ]
}
```

**Error (404):**
```json
{
  "error": "Topic not found: InvalidTopic",
  "status": "404"
}
```

## Database Schema

### topics
- `id` - Primary key
- `name` - Unique topic name (case-insensitive lookups)
- `category` - "backend", "frontend", or "mobile"
- `created_at` - Timestamp

### questions
- `id` - Primary key
- `topic_id` - Foreign key to topics
- `question_text` - The question
- `subtopic` - Optional subtopic for filtering (e.g., "records", "hooks")
- `difficulty` - "EASY", "MEDIUM", or "HARD"
- `explanation` - Optional explanation (nullable)
- `created_at` - Timestamp

### question_options
- `id` - Primary key
- `question_id` - Foreign key to questions
- `option_text` - Answer text
- `is_correct` - Boolean (exactly one true per question, 4-6 options total)

## Project Structure

```
codepop/
├── codepop-backend/          # Spring Boot backend
│   ├── src/main/java/nl/schoutens/codepop/
│   │   ├── entity/           # JPA entities (Topic, Question, QuestionOption)
│   │   ├── dto/              # Data Transfer Objects (Java records)
│   │   ├── repository/       # Spring Data JPA repositories
│   │   ├── service/          # Business logic (QuizService, QuizMaster interface)
│   │   ├── controller/       # REST controllers (QuizController)
│   │   └── config/           # Configuration (CorsConfig, DataSeeder)
│   ├── src/test/java/        # Unit and integration tests
│   └── pom.xml               # Maven dependencies
├── codepop-frontend/         # React frontend
│   ├── src/
│   │   ├── components/       # Reusable UI components
│   │   ├── pages/            # Route-level components
│   │   ├── services/         # API service layer (Axios)
│   │   ├── App.jsx           # Main app with React Router
│   │   └── main.jsx          # Entry point
│   ├── package.json          # npm dependencies
│   └── vite.config.js        # Vite config (includes proxy to backend)
└── docs/                     # Architecture and design decisions
```

## Development Notes

**Prerequisites:**
- Backend: JDK 17+, Maven 3.6+ (or use `./mvnw`)
- Frontend: Node.js 18+, npm
- Optional: Ollama running locally (for future LLM integration)

**Database:**
- SQLite file: `codepop-backend/codepop.db` (auto-generated)
- Schema auto-updates via `spring.jpa.hibernate.ddl-auto=update`

**Ports:**
- Backend: `http://localhost:8080`
- Frontend: `http://localhost:5173`
- Ollama: `http://localhost:11434` (if using local LLM)

**Current Status:**
- ✅ Backend MVP complete (API, validation, caching, tests)
- ✅ Frontend project setup complete
- 🚧 Frontend components and routing in progress
- ⏳ API integration pending
- ⏳ OllamaQuizMaster implementation pending

**Test Coverage:**
- 29 total `@Test` annotations across unit and integration tests
- `QuizServiceTest`: 10 unit tests (service layer logic)
- `QuizControllerTest`: 6 unit tests (controller layer)
- `QuizControllerIntegrationTest`: 12 integration tests (end-to-end API)
