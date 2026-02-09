# CodePop Backend

Spring Boot backend service for CodePop - an AI-powered quiz generator for coding topics.

## Tech Stack

- **Java 17+**
- **Spring Boot 4.0.2** - REST API framework
- **Spring Data JPA** - Database access
- **Spring AI - Ollama** - LLM integration (interface ready, implementation pending)
- **SQLite** - Local database for question storage
- **Hibernate** - ORM with SQLite dialect

## Project Structure

```
codepop-backend/
├── src/main/java/nl/schoutens/codepop/
│   ├── entity/          # Domain entities (Topic, Question, QuestionOption)
│   ├── dto/             # Data Transfer Objects for API responses
│   ├── repository/      # Spring Data JPA repositories
│   ├── service/         # Business logic layer
│   ├── controller/      # REST API endpoints
│   └── config/          # Configuration and data seeding
├── src/test/java/       # Unit and integration tests
├── src/main/resources/
│   └── application.properties  # Application configuration
└── codepop.db           # SQLite database (auto-generated)
```

## Prerequisites

- JDK 17 or higher
- Maven 3.6+ (or use included `./mvnw`)
- Ollama running locally (optional - for future LLM integration)

## Running the Application

```bash
# Using Maven wrapper (recommended)
./mvnw spring-boot:run

# Or with Maven installed
mvn spring-boot:run
```

The backend will start on `http://localhost:8080`

## Configuration

Edit `src/main/resources/application.properties`:

```properties
# Database
spring.datasource.url=jdbc:sqlite:codepop.db
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

# CORS - Allow frontend to call backend API
cors.allowed-origins=http://localhost:5173
# For production, change to: cors.allowed-origins=https://yourdomain.com
# For multiple origins: cors.allowed-origins=http://localhost:5173,https://yourdomain.com
```

CORS is configured globally in `CorsConfig.java` - no need for `@CrossOrigin` annotations on individual controllers.


## API Endpoints

### GET /api/quiz/{topic}

Get a quiz for a specific topic with optional subtopic filtering.

**Topic Matching:**
- The API supports both main topics and subtopics
- Main topics: Java, Python, React, JavaScript, etc. (19 main topics)
- Input like "Java records" parses to: main topic = "Java", subtopic = "records"
- Fuzzy matching: "Java record" matches questions with subtopic "records"
- **If subtopic specified but not found**: Returns 0 questions, LLM will generate new ones
- **If no subtopic specified**: Returns all questions for main topic

**Parameters:**
- `topic` (path variable) - Topic name, optionally with subtopic (e.g., "Java", "Java records", "React hooks")
- `excludeQuestionIds` (query parameter, optional) - Comma-separated list of question IDs to exclude

**Example Requests:**
```bash
# Get quiz for all Java questions (any subtopic)
curl http://localhost:8080/api/quiz/Java

# Get quiz for Java records subtopic only
curl http://localhost:8080/api/quiz/Java%20records

# Get quiz for Java streams subtopic (LLM generates if none exist)
curl http://localhost:8080/api/quiz/Java%20streams

# Get React hooks subtopic
curl http://localhost:8080/api/quiz/React%20hooks

# Get quiz excluding questions 1 and 2
curl "http://localhost:8080/api/quiz/Java%20records?excludeQuestionIds=1,2"
```

**Response (200 OK):**
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
        {
          "id": 1,
          "text": "record",
          "isCorrect": true
        },
        {
          "id": 2,
          "text": "class",
          "isCorrect": false
        }
      ]
    }
  ]
}
```

**Error Response (404 Not Found):**
```json
{
  "error": "Topic not found: InvalidTopic",
  "status": "404"
}
```

## Database Schema

### topics
- `id` - Primary key
- `name` - Unique topic name (case-insensitive queries)
- `category` - backend, frontend, or mobile
- `created_at` - Timestamp

### questions
- `id` - Primary key
- `topic_id` - Foreign key to topics
- `question_text` - The question
- `subtopic` - Optional subtopic for filtering (e.g., "records", "hooks")
- `difficulty` - EASY, MEDIUM, or HARD
- `explanation` - Optional explanation (nullable)
- `created_at` - Timestamp

### question_options
- `id` - Primary key
- `question_id` - Foreign key to questions
- `option_text` - Answer option text
- `is_correct` - Boolean (exactly one true per question)

## Seeded Data

The application automatically seeds the database on first startup:

**Topics (19 main topics):**
- Backend: Java, Python, Node.js, C#, Go, Rust, PHP
- Frontend: JavaScript, TypeScript, React, Vue, Angular, HTML, CSS, Svelte
- Mobile: Swift, Kotlin, React Native, Flutter

**Questions (6 total):**
- 3 questions for Java with subtopic "records" (Easy, Medium, Hard)
- 1 question for React with subtopic "hooks" (Easy)
- 1 question for React with subtopic "lifecycle" (Medium)
- 1 question for React with subtopic "fundamentals" (Hard)

Each question has 4-6 answer options with exactly one correct answer.

## Code Quality Notes

**DTOs as Java Records:**
All DTO classes (`QuizDTO`, `QuestionDTO`, `OptionDTO`) are implemented as Java records for:
- Immutability by default
- Concise syntax (5-7 lines instead of 40+)
- Auto-generated `equals()`, `hashCode()`, `toString()`
- Better practice for data transfer objects
- Works seamlessly with Jackson JSON serialization

**Entities with Lombok:**
All JPA entity classes (`Topic`, `Question`, `QuestionOption`) use Lombok annotations to reduce boilerplate:
- `@Getter` / `@Setter` - Generates accessor methods (eliminated ~100 lines of code)
- `@NoArgsConstructor` - Required by JPA for entity instantiation
- `@AllArgsConstructor` - Convenient for testing and construction
- Avoided `@Data` on entities - includes `@EqualsAndHashCode` which can cause issues with JPA lazy loading
- Custom constructors retained for domain-specific logic
- Result: 252 lines → 143 lines (43% reduction)

## Development

### Build and Test
```bash
# Build the project
./mvnw clean install

# Run all tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=QuizServiceTest

# Skip tests during build
./mvnw clean install -DskipTests
```

### Test Coverage
- **QuizServiceTest**: 7 unit tests covering service layer logic
  - Topic validation
  - Question filtering and exclusion
  - Randomization (when > 5 questions available)
  - LLM integration interface calls
  - Error handling

### Code Quality
```bash
# Compile and check for errors
./mvnw compile

# Clean build artifacts
./mvnw clean
```

## Features

✅ **Implemented:**
- REST API endpoint for quiz retrieval
- Topic validation (case-insensitive)
- Question randomization (when > 5 available)
- Question exclusion for session tracking
- Flexible 4-6 answer options per question
- Difficulty levels (EASY, MEDIUM, HARD)
- Optional explanations
- CORS configuration for frontend
- SQLite persistence with auto-schema generation
- Data seeding on startup
- Comprehensive unit tests

🚧 **Pending:**
- OllamaQuizMaster LLM integration (interface ready)
- Additional topics and questions
- Session persistence (currently frontend tracks excluded IDs)

## LLM Integration

The backend includes a `QuizMaster` interface for question generation:

```java
public interface QuizMaster {
    List<Question> generateQuestions(
        String topic, 
        int count, 
        List<String> existingQuestionTexts
    );
}
```

**Current Implementation:**
- `OllamaQuizMaster` - Stub implementation (returns empty list)

**Future Implementation:**
- Ollama API integration
- Prompt engineering to avoid duplicate questions
- Automatic saving of generated questions to database

## License

MIT License - see the [LICENSE](../LICENSE) file for details.
