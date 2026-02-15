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
**Implementation:** `OllamaQuizMaster` using Spring AI ChatClient with `qwen2.5-coder:7b` model  
**Rationale:** 
- Allows testing without LLM dependency (test profile uses stub)
- Easy to swap implementations (Ollama, GPT, Claude)
- Interface defines contract: `generateQuestions(topic, count, existingQuestionTexts)`
- Separates LLM logic from service orchestration
- `@Profile("!test")` excludes from test environment for fast, predictable tests

**Implementation Details:**
- Temperature: 0.8 (higher creativity for quiz generation vs 0.3 default)
- Prompt includes markdown code block examples to encourage formatted questions
- Response parsing handles both wrapped JSON and raw JSON with fuzzy boundary detection
- Questions support markdown formatting in frontend (ReactMarkdown)

**Duplicate Prevention Strategy:**
- Backend passes **ALL** existing question texts to LLM (no limit)
- Previously had 10-question limit which caused duplicates when > 10 questions existed
- Removed limit so LLM sees complete history
- Prompt explicitly instructs: "Generate questions on DIFFERENT aspects of [topic] that are NOT covered above"
- With temperature 0.8, provides enough creativity for varied questions
- Note: LLM may occasionally generate similar questions for narrow topics with limited question space

### Question Generation: Training Data Only
**Decision:** LLM generates questions from training knowledge, no web search  
**Rationale:** Simpler implementation for POC. Training data sufficient for coding topics. Can add web search in future if needed.

**Implementation:** 
- QuizMaster interface receives list of **ALL** existing question texts (no limit)
- Previously limited to 10 questions, causing duplicates when database grew
- Fixed by removing limit: LLM now sees complete question history
- Prompt instructs: "Avoid generating questions similar to these N existing ones"
- Prompt adds: "Generate questions on DIFFERENT aspects of [topic] that are NOT covered above"

**Duplicate Prevention:**
1. Backend extracts ALL question texts from database for the topic (including user's excluded questions)
2. Passes complete list to `QuizMaster.generateQuestions()`
3. OllamaQuizMaster includes all questions in prompt with explicit instruction to avoid similarity
4. Temperature 0.8 provides enough creativity to generate varied questions
5. For narrow topics (e.g., "Java operators"), may still occasionally see similar questions due to limited question space

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

### Session Tracking: Browser sessionStorage
**Decision:** Use browser `sessionStorage` API to track asked questions per topic  
**Implementation:**
- Utility module: `src/utils/sessionStorage.js`
- Storage key: `codepop_asked_questions`
- Data format: `{ "topic": [questionId1, questionId2, ...] }`
- Functions: `getAskedQuestions(topic)`, `addAskedQuestions(topic, ids)`
- QuizPage loads IDs before API call, saves IDs after receiving quiz
- Passed to backend via query parameter: `?excludeQuestionIds=1,2,3`

**Rationale:**
- Persists across page reloads (user can navigate away and return)
- Automatically cleared when browser tab/window closes (fresh session each time)
- Stateless backend (no session storage on server)
- Frontend has full control over user session
- No database storage needed for session data
- Users see different questions when retaking same topic within session
- Can migrate to backend session storage or user accounts later if needed

**Alternative Considered:**
- **In-memory React state**: Cleared on page reload (poor UX)
- **localStorage**: Persists indefinitely (would require manual cleanup UI)
- **Backend session storage**: Adds complexity, requires session management

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

### Transaction Isolation Pattern
**Decision:** Separate long-running LLM calls from database transactions using static inner `@Component` class  
**Problem:** SQLite cannot handle long transactions (5-10 seconds for LLM generation causes "Unable to commit" errors)  
**Solution:** `TransactionalOperations` inner static class with Spring proxy  

**Architecture:**
```java
@Service
public class QuizService {
  private final TransactionalOperations txOps;
  
  @Component
  public static class TransactionalOperations {
    @Transactional(readOnly = true)
    public List<Question> fetchQuestions(...) { }
    
    @Transactional
    public void saveQuestions(...) { }
  }
  
  public QuizDTO getQuiz(...) {
    // NO @Transactional - orchestration only
    List<Question> questions = txOps.fetchQuestions(...); // Short read tx
    List<Question> newOnes = quizMaster.generate(...); // NO TRANSACTION
    txOps.saveQuestions(newOnes); // Short write tx
  }
}
```

**Transaction Boundaries:**
1. **Read topic**: `@Transactional(readOnly = true)` (< 10ms)
2. **Read questions**: `@Transactional(readOnly = true)` with JOIN FETCH (< 50ms)
3. **LLM generation**: NO TRANSACTION - can take 5-10 seconds
4. **Save questions**: `@Transactional` write (< 100ms)
5. **Re-fetch**: `@Transactional(readOnly = true)` (< 50ms)

**Rationale:**
- Spring can proxy static inner `@Component` independently from outer class
- `@Transactional` annotations work properly (not bypassed like internal calls)
- No circular dependencies (inner class is stateless)
- SQLite only holds locks for milliseconds, not seconds
- Clean separation: orchestration (service) vs data access (txOps)

**Why Static Inner Class Works:**
- Spring creates TWO separate proxy beans: `QuizService` and `TransactionalOperations`
- When `quizService.getQuiz()` calls `txOps.fetchQuestions()`, it goes through Spring proxy
- Transaction interceptor can apply properly
- Alternative approaches failed: self-injection (circular dependency), separate class (more boilerplate)

### Test Database Isolation
**Decision:** Separate SQLite database for integration tests  
**Implementation:**
- `application-test.properties` configures `test-codepop.db`
- `@ActiveProfiles("test")` on integration test classes
- `@TestConfiguration` provides stub `QuizMaster` (returns empty list)
- `@BeforeEach` seeds known test data
- `@Profile("!test")` on `OllamaQuizMaster` excludes from test environment

**Rationale:**
- Tests run fast (no LLM calls, ~2 seconds for 29 tests)
- Predictable results (no AI variability)
- Production database not polluted with test data
- Can reset test database easily (create-drop schema)

## React StrictMode Behavior

### Development vs Production
**Known Issue:** React StrictMode in development causes `useEffect` to run twice  
**Impact:** Can trigger duplicate API calls to backend when requesting new topics  
**Severity:** Development-only (production builds disable StrictMode)

**Modes Available:**
```bash
# Development mode (StrictMode enabled)
npm run dev  # May show duplicate LLM generation for new topics

# Production preview mode (StrictMode disabled)
npm run build && npm run preview  # No duplicate generation

# Production deployment
npm run build  # Deploy dist/ folder (StrictMode disabled)
```

**Solution Options:**
1. **Accept as dev-only** - Production unaffected, no code changes ✅ (Current)
2. **Add AbortController** - Proper React pattern, cancels duplicate requests
3. **Remove StrictMode** - Loses debugging benefits, not recommended

**Recommendation:** Accept as development quirk since production preview mode confirmed no issue exists in production builds.

## Future Considerations

### Backend Enhancements
- Add more seeded topics and questions
- Persistent session storage (server-side tracking)
- Difficulty level filtering as query parameter
- Configurable question count
- Improve subtopic extraction with NLP techniques
- Consider PostgreSQL for production (better concurrency than SQLite)

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
