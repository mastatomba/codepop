# CodePop 🎯

> A dynamic AI-powered quiz generator for coding topics - pop quiz yourself on anything from Java to React!

## 📖 Description

CodePop generates interactive multiple-choice quizzes on coding topics using AI. Simply enter "Quiz me about **X**" where X is your chosen coding topic, and get 5 dynamically generated questions with instant feedback and scoring.

## ✨ Features

- **AI-Powered Question Generation**: Uses LLM (Ollama or cloud API) to create factual, verifiable quiz questions
- **Smart Topic Validation**: Supports 19 coding topics across backend, frontend, and mobile development
- **Intelligent Caching**: Stores generated questions in SQLite to avoid repetition within a session
- **Difficulty Levels**: Each question categorized as easy, medium, or hard
- **Graceful Degradation**: Shows partial quiz if LLM fails, with helpful notifications

## 🛠️ Tech Stack

**Backend:**
- Java 17+
- Spring Boot
- Spring Data JPA
- Spring AI - Ollama
- SQLite

**Frontend:**
- React *(coming soon)*
- REST API communication

## 🎯 Supported Topics

**Backend:** Java, Python, Node.js, C#, Go, Rust, PHP  
**Frontend:** JavaScript, TypeScript, React, Vue, Angular, HTML, CSS, Svelte  
**Mobile:** Swift, Kotlin, React Native, Flutter

*Subtopics supported too (e.g., "Java Spring Boot", "React hooks")*

## 🚀 Getting Started

### Prerequisites
- JDK 17 or higher
- Maven 3.6+
- Ollama installed and running locally (or cloud LLM API key)

### Running Locally

```bash
# Clone the repository
git clone https://github.com/mastatomba/codepop.git
cd codepop

# Run the backend
cd codepop-backend
./mvnw spring-boot:run
```

The backend will start on `http://localhost:8080`

## 📁 Project Structure

```
codepop/
├── codepop-backend/    # Spring Boot backend
├── frontend/           # React frontend (coming soon)
├── IDEA.md            # Detailed concept and design decisions
└── README.md          # This file
```

## 🎨 How It Works

1. User enters a coding topic
2. Backend validates topic against supported list
3. Backend checks SQLite for existing questions
4. If needed, LLM generates new questions with explanations
5. Questions stored in database for future use
6. User answers 5 multiple-choice questions
7. Score displayed at the end

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🚧 Status

**Current:** Backend development in progress  
**Next:** Frontend implementation, LLM integration

---

*Built as a POC to explore AI-powered educational tools for developers*