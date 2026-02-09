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
2. Backend validates topic exists in database
3. Backend checks SQLite for existing questions
4. Backend filters out excludeQuestionIds (session tracking)
5. If < 5 questions available, backend calls QuizMaster interface
6. Backend stores new questions in SQLite
7. Backend randomizes questions (if > 5 available) and returns up to 5

**Rationale:** Cleaner separation of concerns, easier to test, better control over LLM calls and caching.

### QuizMaster Interface Pattern
**Decision:** LLM integration via interface, not direct implementation  
**Current:** `OllamaQuizMaster` stub (returns empty list)  
**Future:** Actual Ollama API implementation  
**Rationale:** 
- Allows testing without LLM dependency
- Easy to swap implementations (Ollama, GPT, Claude)
- Interface defines contract: generateQuestions(topic, count, existingQuestionTexts)
4. Backend calls LLM only if new questions needed
5. Backend stores new questions in SQLite
6. Backend returns quiz to frontend

**Rationale:** Cleaner separation of concerns, easier to test, better control over LLM calls and caching.

### Question Generation: Training Data Only
**Decision:** LLM generates questions from training knowledge, no web search  
**Rationale:** Simpler implementation for POC. Training data sufficient for coding topics. Can add web search in future if needed.

**Implementation:** QuizMaster interface receives list of existing question texts to avoid duplicates. LLM prompt includes: "Generate N new questions about X, different from: [existing questions]".

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

### Topic Validation: Database-Stored
**Decision:** Maintain topics in database, seeded at startup  
**Topics:** 19 root topics (Java, Python, React, etc.) + 2 subtopics (Java records, Javascript React)  
**Rationale:** 
- Flexible - can add topics without code changes
- Case-insensitive lookups via repository method
- Covers most popular development areas
- Prevents off-topic queries
- Easy to extend with new topics

### Question Randomization
**Decision:** Randomize question selection when > 5 questions available  
**Implementation:** `Collections.shuffle()` before selecting up to 5 questions  
**Rationale:**
- Prevents users from seeing same questions in same order
- Ensures variety across sessions
- Fair distribution of questions from database

### Explanations: Optional, per question
**Decision:** Each question can have an optional explanation (nullable field)  
**Rationale:** Flexibility for questions that need context vs those that are self-explanatory. Stored in database, not per-option.

### Session Tracking: Frontend-Managed
**Decision:** Frontend tracks excludeQuestionIds, passes to backend via query parameter  
**Backend:** Filters out excluded questions before selection/randomization  
**Rationale:**
- Stateless backend (no session storage needed for POC)
- Frontend has full control over user session
- Simple to implement
- Can migrate to backend session storage later if needed

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

### CORS Configuration
**Decision:** Global CORS configuration via `CorsConfig` class and `application.properties`  
**Rationale:**
- Centralized configuration instead of `@CrossOrigin` on each controller
- Configurable via properties file - easy to change per environment
- Supports multiple origins (comma-separated)
- Follows Spring Boot best practices for cross-cutting concerns
- Default allows `http://localhost:5173` (Vite dev server)
- Production: override with actual domain via `cors.allowed-origins` property
- More maintainable as application grows (one place to update CORS rules)

**Implementation:**
```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Value("${cors.allowed-origins:http://localhost:5173}")
    private String allowedOrigins;
    
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins.split(","))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS");
    }
}
```

### Topic/Subtopic Architecture
**Decision:** Main topics in `topics` table, subtopics as field in `questions` table  
**Rationale:**
- Topics represent 19 main technology categories (Java, Python, React, etc.)
- Subtopics are specific areas within a topic (e.g., "records" in Java, "hooks" in React)
- User input like "Java records" parses to main_topic="Java" + subtopic="records"
- Fuzzy matching allows flexible queries ("Java record" matches "records")
- **If subtopic specified but not found**: Returns 0 questions, triggering LLM to generate new ones for that specific subtopic
- **If no subtopic specified**: Returns all questions for the main topic
- Scales better: Unlimited subtopics without polluting topics table
- Easier for LLM: Can generate questions for any new subtopic dynamically

**Implementation:**
- `Question.subtopic` field with LIKE query for fuzzy matching
- `QuizService.parseTopicInput()` extracts main topic and subtopic from user input
- `QuizService.findMainTopic()` handles fuzzy matching on topic names
- `QuizService.fetchQuestions()` returns subtopic-filtered or all-topic questions based on input
  - Subtopic specified: `WHERE subtopic LIKE '%input%'` (may return 0, triggering LLM)
  - No subtopic: `WHERE topic_id = ?` (returns all for that topic)

### DTOs as Java Records
**Decision:** Use Java records for all DTOs (`QuizDTO`, `QuestionDTO`, `OptionDTO`)  
**Rationale:**
- DTOs are immutable data carriers with no business logic
- Records provide immutability by default
- Reduces boilerplate: 147 lines → 17 lines (88% reduction)
- Auto-generates `equals()`, `hashCode()`, `toString()`
- Accessor pattern: `dto.field()` instead of `dto.getField()`
- Works seamlessly with Jackson for JSON serialization/deserialization
- Demonstrates modern Java best practices (records introduced in Java 16)

**Example:**
```java
// Before (43 lines with getters/setters)
public class QuizDTO {
    private String topic;
    private Integer totalQuestions;
    private List<QuestionDTO> questions;
    // ... constructors, getters, setters
}

// After (5 lines)
public record QuizDTO(
    String topic,
    Integer totalQuestions,
    List<QuestionDTO> questions
) {}
```

### Entities with Lombok
**Decision:** Use Lombok `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor` on JPA entities  
**Rationale:**
- Eliminates ~100 lines of boilerplate getter/setter code
- Entity classes reduced from 252 → 143 lines (43% reduction)
- `@NoArgsConstructor` required by JPA for entity instantiation
- `@AllArgsConstructor` convenient for testing and object construction
- Explicitly avoided `@Data` - includes `@EqualsAndHashCode` which can cause issues with JPA lazy loading
- Custom constructors retained for domain-specific initialization logic
- Works seamlessly with JPA/Hibernate
- Lombok already included in Spring Boot starter dependencies

**Example:**
```java
// Before (69 lines with all getters/setters)
@Entity
@Table(name = "topics")
public class Topic {
    private Long id;
    private String name;
    private String category;
    private LocalDateTime createdAt;
    // ... constructors, getters, setters (40+ lines)
}

// After (41 lines)
@Entity
@Table(name = "topics")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Topic {
    private Long id;
    private String name;
    private String category;
    private LocalDateTime createdAt;
    // Custom constructor for domain logic
}
```

## Future Considerations

### Backend Enhancements
- Implement `OllamaQuizMaster` with actual LLM integration
- Add more seeded topics and questions
- Persistent session storage (server-side tracking)
- Difficulty level filtering as query parameter
- Configurable question count
- Improve subtopic extraction with NLP techniques

### Frontend Enhancements
- User accounts and progress tracking
- Quiz history and analytics
- Timer for questions
- Leaderboards
- Subtopic autocomplete/suggestions

### Infrastructure
- Move to PostgreSQL/MySQL for production scale
- Redis for session caching
- Rate limiting on API endpoints
- Metrics and monitoring
