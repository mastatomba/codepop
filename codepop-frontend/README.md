# CodePop Frontend 🎯

React-based frontend for the CodePop AI-powered quiz application.

## 🛠️ Tech Stack

- **Vite** - Fast build tool with instant HMR
- **React 19** - UI library
- **React Router v7** - Client-side routing
- **Axios** - HTTP client for REST API calls

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
npm run dev      # Start dev server with hot reload
npm run build    # Build for production
npm run preview  # Preview production build locally
npm run lint     # Run ESLint
```

## 📁 Project Structure

```
codepop-frontend/
├── src/
│   ├── components/    # Reusable UI components
│   ├── pages/         # Route-level page components
│   ├── services/      # API service layer (axios)
│   ├── utils/         # Helper functions
│   ├── App.jsx        # Main app component with routing
│   └── main.jsx       # Application entry point
├── public/            # Static assets
└── vite.config.js     # Vite configuration (includes proxy to backend)
```

## 🔌 API Integration

The frontend communicates with the Spring Boot backend via REST API:

- **Base URL**: `/api` (proxied to `http://localhost:8080/api` in development)
- **Primary Endpoint**: `GET /api/quiz/{topic}` - Fetch quiz questions

API calls are centralized in `src/services/api.js` using Axios.

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

## 🚧 Development Status

**Phase 1**: Project setup ✅  
**Phase 2**: Core components & routing (in progress)  
**Phase 3**: API integration (planned)  
**Phase 4**: UI polish (planned)

## 📝 License

MIT License - see the [LICENSE](../LICENSE) file for details.
