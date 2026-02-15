# Testing Guide

This document describes the testing strategy and practices for the CodePop project.

## Overview

CodePop has comprehensive test coverage for both backend and frontend:

- **Backend**: 29 tests (JUnit 5 + Mockito + Spring Boot Test)
- **Frontend**: 35 tests (Vitest + React Testing Library + MSW)
- **Total**: 64 automated tests

## Backend Testing

### Framework & Tools
- **JUnit 5**: Modern Java testing framework
- **Mockito**: Mocking framework for unit tests
- **Spring Boot Test**: Integration testing support
- **SQLite**: Separate test database (test-codepop.db) for integration tests
- **@TestConfiguration**: Stub QuizMaster for predictable test results

### Test Structure

```
codepop-backend/src/test/java/
├── QuizServiceTest.java              # 10 unit tests (mocks TransactionalOperations)
├── QuizControllerTest.java           # 6 unit tests (mocks QuizService)
└── QuizControllerIntegrationTest.java # 12 integration tests (separate test DB)

codepop-backend/src/test/resources/
└── application-test.properties        # Test database configuration
```

### Test Types

**Unit Tests** (`QuizServiceTest`, `QuizControllerTest`)
- Test individual components in isolation
- Mock dependencies (repositories, QuizMaster, TransactionalOperations)
- Fast execution (~200ms total for 16 unit tests)
- Focus on business logic validation
- `QuizServiceTest` mocks `TransactionalOperations` to test service orchestration

**Integration Tests** (`QuizControllerIntegrationTest`)
- Test complete API endpoints end-to-end
- Use separate SQLite database (test-codepop.db) with `@ActiveProfiles("test")`
- Stub `QuizMaster` via `@TestConfiguration` (returns empty list)
- Seed known test data in `@BeforeEach` for predictable results
- Test HTTP request/response cycle
- Validate JSON serialization
- Focus on API contracts
- Fast execution (~1.8 seconds for 12 tests) - no LLM calls

### Running Backend Tests

```bash
cd codepop-backend

# Run all tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=QuizServiceTest

# Run with coverage
./mvnw clean test jacoco:report

# Skip tests during build
./mvnw clean install -DskipTests
```

### Test Coverage

- ✅ Topic validation (valid topics, invalid topics, case-insensitive)
- ✅ Question caching (cache hits, cache misses)
- ✅ Subtopic filtering (fuzzy matching, no subtopic)
- ✅ Question exclusion (excludeQuestionIds parameter)
- ✅ Error handling (topic not found, empty results)
- ✅ API contracts (HTTP status codes, JSON structure)
- ✅ QuizMaster integration (stub implementation in tests, real OllamaQuizMaster in production)
- ✅ Transaction isolation (TransactionalOperations pattern)
- ✅ Test database isolation (separate test-codepop.db)
- ✅ JOIN FETCH for eager loading (no N+1 queries)

### Test Isolation Strategy

**Problem:** LLM calls are slow (5-10 seconds) and unpredictable for tests

**Solution:** Separate test environment with stub QuizMaster
```java
// Integration test configuration
@ActiveProfiles("test")
@TestConfiguration
static class TestConfig {
  @Bean
  @Primary
  public QuizMaster testQuizMaster() {
    return (topic, count, existing) -> Collections.emptyList();
  }
}

// Production service excluded from tests
@Service
@Primary
@Profile("!test")
public class OllamaQuizMaster implements QuizMaster { ... }
```

**Benefits:**
- Tests run fast (~2 seconds for all 29 tests)
- Predictable results (no AI variability)
- No Ollama dependency for CI/CD
- Production database not polluted with test data
- Can test both cached and empty scenarios reliably

## Frontend Testing

### Framework & Tools
- **Vitest**: Fast unit test framework (Vite-native)
- **React Testing Library**: Component testing utilities
- **jsdom**: Browser environment simulation
- **MSW**: Mock Service Worker for API mocking
- **@testing-library/user-event**: User interaction simulation

### Test Structure

```
codepop-frontend/src/
├── __tests__/
│   ├── HomePage.test.jsx              # 6 tests
│   ├── QuizPage.test.jsx              # 9 tests
│   ├── Question.test.jsx              # 7 tests
│   ├── ProgressIndicator.test.jsx     # 4 tests
│   └── ScoreBreakdown.test.jsx        # 9 tests
└── test/
    ├── setup.js                       # Test configuration
    └── mocks/
        ├── handlers.js                # MSW request handlers
        └── server.js                  # MSW server setup
```

### MSW (Mock Service Worker)

MSW intercepts network requests at the network level, providing realistic API mocking:

**Benefits:**
- Tests use actual `fetch`/`axios` calls (no mocking axios)
- More realistic than manual mocking
- Same handlers can be used in browser for development
- Type-safe with TypeScript
- No test-specific code in production components

**Configuration:**
```javascript
// src/test/mocks/handlers.js
http.get('/api/quiz/:topic', ({ params }) => {
  if (params.topic === 'InvalidTopic123') {
    return HttpResponse.json({ error: '...' }, { status: 404 });
  }
  return HttpResponse.json(mockQuizData);
});
```

### Running Frontend Tests

```bash
cd codepop-frontend

# Watch mode (re-runs on file changes)
npm test

# Run once (CI mode)
npm test -- --run

# Visual UI mode
npm run test:ui

# With coverage report
npm run test:coverage
```

### Test Coverage by Component

**HomePage (6 tests)**
- Renders title and form
- Displays help text
- Submit button state management
- Input validation (empty, whitespace)
- Input value updates

**QuizPage (9 tests)**
- Loading state display
- Quiz data loading from API
- Error handling (404, network errors)
- Option selection
- Submit button state
- Correct/incorrect answer feedback
- Question progression
- Complete quiz flow to results
- Results display

**Question (7 tests)**
- Question text and difficulty rendering
- All options displayed
- Feedback visibility control
- Correct answer feedback with explanation
- Incorrect answer feedback
- Missing explanation handling
- Difficulty badge styling

**ProgressIndicator (4 tests)**
- Current/total question display
- Progress bar width calculation
- Progress percentages (20%, 60%, 100%)
- Visual progress representation

**ScoreBreakdown (9 tests)**
- Quiz completion display
- Percentage calculation
- Score tier messages (Perfect, Excellent, Good, etc.)
- All questions review section
- Correct answer display for wrong questions
- Retake quiz button handler
- Explanation display
- Difficulty badges
- User answer vs correct answer comparison

### Testing Best Practices

1. **Test User Behavior, Not Implementation**
   - Use screen queries (`getByRole`, `getByText`, `getByLabelText`)
   - Avoid testing internal state or implementation details
   - Simulate real user interactions

2. **Use MSW for API Calls**
   - Don't mock axios/fetch directly
   - Define handlers for all API endpoints
   - Test both success and error scenarios

3. **Async Testing**
   - Use `waitFor` for async operations
   - Use `userEvent.setup()` for simulating user interactions
   - Avoid arbitrary `setTimeout` calls

4. **Accessibility**
   - Query by role and label when possible
   - Test keyboard navigation
   - Ensure ARIA attributes are correct

## Continuous Integration

### Pre-commit Hooks

Both backend and frontend have pre-commit hooks via Husky:

**Backend:**
```bash
# Runs before commit
mvn spotless:check
```

**Frontend:**
```bash
# Runs before commit
npm run lint && npm run format:check
```

To bypass (emergencies only):
```bash
git commit --no-verify -m "emergency fix"
```

### CI Pipeline Recommendations

```yaml
# .github/workflows/test.yml (example)
name: Test

on: [push, pull_request]

jobs:
  backend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '17'
      - name: Test Backend
        run: |
          cd codepop-backend
          ./mvnw test

  frontend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-node@v3
        with:
          node-version: '18'
      - name: Test Frontend
        run: |
          cd codepop-frontend
          npm ci
          npm test -- --run
```

## Test Maintenance

### When to Update Tests

1. **Feature Changes**: Update tests when adding/modifying features
2. **Bug Fixes**: Add test case for the bug before fixing it (TDD)
3. **Refactoring**: Tests should pass without changes if behavior is unchanged
4. **API Changes**: Update MSW handlers if backend API changes

### Common Issues

**Common Issues

**Backend:**
- Database state: Ensure `@BeforeEach` seeds known data or use `@Transactional` rollback
- Test database: Separate `test-codepop.db` configured via `@ActiveProfiles("test")`
- Mocking: Use `@MockBean` for Spring-managed beans, `@Mock` for unit test dependencies
- Integration tests slow: Use stub `QuizMaster` to avoid LLM calls (5-10 second reduction per test)
- TransactionalOperations: Mock this in unit tests to test service orchestration independently

**Frontend:**
- Async timing: Always use `waitFor` for async assertions
- MSW not intercepting: Ensure server is started in `beforeAll`
- Act warnings: Wrap state updates in `await waitFor()`
- Cleanup: MSW server resets handlers in `afterEach`

## Coverage Goals

- **Backend**: Maintain >80% line coverage
- **Frontend**: Maintain >80% component coverage
- **Focus**: Critical user paths and business logic
- **Don't obsess**: 100% coverage ≠ 100% confidence

## Resources

- [Vitest Documentation](https://vitest.dev/)
- [React Testing Library](https://testing-library.com/react)
- [MSW Documentation](https://mswjs.io/)
- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [Spring Boot Testing](https://spring.io/guides/gs/testing-web/)
