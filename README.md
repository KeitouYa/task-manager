# Task Manager

A Spring Boot REST API for a personal task manager, with an AI-powered endpoint that turns plain language into a structured task.

## Setup

Requires Java 17.

```bash
git clone <repo>
cd task-manager
# macOS / Linux
./mvnw spring-boot:run
# Windows
.\mvnw.cmd spring-boot:run
```

Server starts on `http://localhost:8081`. UI at `/`, H2 console at `/h2-console` (JDBC URL `jdbc:h2:mem:taskdb`, user `sa`, blank password).

## OpenAI API key

The AI endpoint (`POST /tasks/suggest`) needs `OPENAI_API_KEY` in the environment.

```bash
# macOS / Linux
export OPENAI_API_KEY=sk-...
# Windows (PowerShell)
$env:OPENAI_API_KEY = "sk-..."
```

If the key is missing or blank, the app still boots and CRUD still works. Only `/tasks/suggest` fails — with a clean `503 Service Unavailable` and message `"OpenAI API key not configured. Set the OPENAI_API_KEY environment variable."`

## API

| Method | Path | Description |
|---|---|---|
| `POST` | `/tasks` | Create a task |
| `GET` | `/tasks` | List all tasks |
| `GET` | `/tasks/{id}` | Get one task |
| `PUT` | `/tasks/{id}` | Update a task (full replace) |
| `DELETE` | `/tasks/{id}` | Delete a task |
| `POST` | `/tasks/suggest` | AI: natural language → task preview (not persisted) |

### Create a task

```bash
curl -X POST http://localhost:8081/tasks \
  -H 'Content-Type: application/json' \
  -d '{"title":"Write tests","description":"service layer","dueDate":"2026-05-22","priority":"HIGH","status":"TODO"}'
```

### AI suggest

Takes a plain-language description and returns a structured task preview. The result is **not** persisted — the UI shows it as a preview, and the user clicks "Use this" to fill the create form.

Request:

```bash
curl -X POST http://localhost:8081/tasks/suggest \
  -H 'Content-Type: application/json' \
  -d '{"text":"remind me to submit the quarterly report before Friday"}'
```

Response:

```json
{
  "id": null,
  "title": "Submit the quarterly report",
  "description": null,
  "dueDate": "2026-05-22",
  "priority": "MEDIUM",
  "status": "TODO"
}
```

The model resolves relative dates using today's date and defaults priority to `MEDIUM` and status to `TODO` when not specified.

## Tests

```bash
./mvnw test
```

Runs 11 tests (5 service unit, 1 suggest service unit, 2 OpenAI client unit, 1 controller slice, 1 CRUD integration, 1 context smoke). No env vars required — no test hits OpenAI.

## What I'd do with more time

- **Test the missing-API-key → 503 path** end-to-end. The code does it; no test guards it.
- **Add `POST /tasks/split`** — a second AI endpoint that takes a large goal and returns a list of sub-tasks. Helps users who struggle to break down big work into actionable pieces.
