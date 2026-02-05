# CodePop Backend

Spring Boot backend service for CodePop - an AI-powered quiz generator for coding topics.

## Tech Stack

- **Java 17+**
- **Spring Boot** - REST API framework
- **Spring Data JPA** - Database access
- **Spring AI - Ollama** - LLM integration
- **SQLite** - Local database for question storage

## Project Structure

- `src/main/java` - Java source code
- `src/main/resources` - Configuration files
- `codepop.db` - SQLite database (auto-generated)

## Prerequisites

- JDK 17 or higher
- Maven 3.6+ (or use included `./mvnw`)
- Ollama running locally (for LLM integration)

## Running the Application

```bash
# Using Maven wrapper
./mvnw spring-boot:run

# Or with Maven installed
mvn spring-boot:run
```

The backend will start on `http://localhost:8080`

## Configuration

Edit `src/main/resources/application.properties` for:
- Server port
- Database location
- Ollama API endpoint

## API Endpoints

*Coming soon*

## Development

```bash
# Build the project
./mvnw clean install

# Run tests
./mvnw test
```
