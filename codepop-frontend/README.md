# CodePop Frontend 🎯

React-based frontend for the CodePop AI-powered quiz application.

## 🛠️ Tech Stack

- **Vite** - Fast build tool with instant HMR
- **React 19** - UI library
- **React Router v7** - Client-side routing
- **Axios** - HTTP client for REST API calls
- **Vitest** - Unit testing framework
- **React Testing Library** - Component testing utilities
- **MSW (Mock Service Worker)** - API mocking for tests

## 🚀 Getting Started

### Prerequisites

- Node.js 18+ (LTS recommended)
- npm or yarn
- CodePop backend running on `http://localhost:8080`

### Installation

```bash
# Install dependencies
npm install

# Start development server
npm run dev
```

The app will be available at `http://localhost:5173`

### Available Scripts

```bash
npm run dev          # Start dev server with hot reload
npm run build        # Build for production
npm run preview      # Preview production build locally
npm test             # Run tests in watch mode
npm test -- --run    # Run tests once (CI mode)
npm run test:ui      # Open Vitest UI (visual test runner)
npm run test:coverage # Run tests with coverage report
npm run lint         # Run ESLint (includes Prettier checks)
npm run format:check # Check code formatting (Prettier)
npm run format:fix   # Auto-fix code formatting
```

**Code Style Enforcement:**

- **Tools**: Prettier + ESLint integration
- **Style**: Standard Prettier (semi-colons, single quotes, 80 char lines, trailing commas ES5)
- **Runs automatically**: On pre-commit hooks
- **IDE Integration**: Install Prettier extension for VS Code/WebStorm to auto-format on save

## 📁 Project Structure

```
codepop-frontend/
├── src/
│   ├── components/      # Reusable UI components
│   │   ├── Question.jsx
│   │   ├── QuestionOption.jsx
│   │   ├── ProgressIndicator.jsx
│   │   └── ScoreBreakdown.jsx
│   ├── pages/           # Route-level page components
│   │   ├── HomePage.jsx
│   │   └── QuizPage.jsx
│   ├── services/        # API service layer (axios)
│   │   └── api.js
│   ├── utils/           # Utility modules
│   │   └── sessionStorage.js  # Session tracking for asked questions
│   ├── __tests__/       # Unit tests
│   │   ├── HomePage.test.jsx
│   │   ├── QuizPage.test.jsx
│   │   ├── Question.test.jsx
│   │   ├── ProgressIndicator.test.jsx
│   │   └── ScoreBreakdown.test.jsx
│   ├── test/            # Test configuration
│   │   ├── setup.js
│   │   └── mocks/
│   │       ├── handlers.js  # MSW request handlers
│   │       └── server.js    # MSW server setup
│   ├── App.jsx          # Main app component with routing
│   └── main.jsx         # Application entry point
├── public/              # Static assets
├── vite.config.js       # Vite configuration (includes proxy to backend)
└── vitest.config.js     # Vitest test configuration
```

## 🔌 API Integration

The frontend communicates with the Spring Boot backend via REST API:

- **Base URL**: `/api` (proxied to `http://localhost:8080/api` in development)
- **Primary Endpoint**: `GET /api/quiz/{topic}?excludeQuestionIds=1,2,3` - Fetch quiz questions

API calls are centralized in `src/services/api.js` using Axios.

### Session Tracking

The app tracks which questions have been asked per topic using **browser sessionStorage**:

- **Utility Module**: `src/utils/sessionStorage.js`
- **Storage Key**: `codepop_asked_questions`
- **Format**: `{ "Java": [1, 2, 3], "React hooks": [4, 5, 6] }`
- **Behavior**:
  - Persists across page reloads (unlike React state)
  - Cleared when browser tab/window closes (unlike localStorage)
  - When retaking same topic, previously asked questions are excluded
  - Backend generates new questions if < 5 available after exclusion

**Functions:**

- `getAskedQuestions(topic)` - Retrieve asked question IDs for a topic
- `addAskedQuestions(topic, ids)` - Save question IDs after quiz fetch

## ⚙️ Configuration

### Vite Proxy

The dev server is configured to proxy API requests to the backend:

```javascript
// vite.config.js
server: {
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true,
    }
  }
}
```

This allows frontend code to call `/api/quiz/java` which forwards to `http://localhost:8080/api/quiz/java`.

## 🧪 Testing

The project uses **Vitest** with **React Testing Library** for comprehensive unit testing.

### Test Coverage

- **35 tests** across 5 test suites
- **100% component coverage** - all pages and components tested
- **MSW integration** - realistic API mocking at network level

### Running Tests

```bash
# Watch mode (re-runs on file changes)
npm test

# Run once (CI mode)
npm test -- --run

# Visual UI mode
npm run test:ui

# With coverage report
npm run test:coverage
```

### Test Structure

Tests use MSW (Mock Service Worker) to mock backend API calls:

- **src/test/mocks/handlers.js** - Defines mock API responses
- **src/test/mocks/server.js** - Configures MSW server
- **src/test/setup.js** - Test environment setup

Example test output:

```
✓ src/__tests__/HomePage.test.jsx (6 tests)
✓ src/__tests__/QuizPage.test.jsx (9 tests)
✓ src/__tests__/Question.test.jsx (7 tests)
✓ src/__tests__/ProgressIndicator.test.jsx (4 tests)
✓ src/__tests__/ScoreBreakdown.test.jsx (9 tests)

Test Files  5 passed (5)
Tests  35 passed (35)
```

## 🚧 Development Status

**Phase 1**: Project setup ✅  
**Phase 2**: Core components & routing ✅  
**Phase 3**: API integration ✅  
**Phase 4**: UI polish ✅  
**Phase 5**: Unit testing ✅

**Status**: MVP Complete - Full quiz flow with comprehensive test coverage

## 📝 License

MIT License - see the [LICENSE](../LICENSE) file for details.
