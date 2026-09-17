# lexic.on

lexic.on is a web app for learning vocabulary while reading texts in a foreign language. Paste a text, read it in the app, click any word to see its translation, and build a personal vocabulary as you go — words are highlighted by status (Unknown / Review / Known) and different grammatical forms of the same word are recognized as one entry.

This is a capstone project and is under active development. See [`docs/roadmap.md`](docs/roadmap.md) for current progress and [`docs/decisions.md`](docs/decisions.md) for the full history of architecture decisions and rationale.

## Stack

**Backend** (`backend/`)
- Java 25, Spring Boot
- PostgreSQL (hosted on [Neon](https://neon.tech)), Spring Data JPA / Hibernate, Flyway migrations
- Spring Security with OAuth2 login
- LanguageTool (embedded, offline) for tokenization/lemmatization
- Azure Translator for translations

**Frontend** (`frontend/`)
- React, TypeScript, Vite
- Tailwind CSS + Radix UI primitives

In production the frontend is built to static files and served by the Spring Boot backend as a single deployable app (see `docs/decisions.md` section 29).

## Local development

### Backend

Requires a PostgreSQL database (a free [Neon](https://neon.tech) project works well) and Java 25.

```bash
cd backend
./mvnw spring-boot:run   # mvnw.cmd on Windows
```

Database connection settings are not committed to the repo. Provide them either as environment variables (`DB_URL`, `DB_USER`, `DB_PASSWORD` referenced from `application.properties`) or in a git-ignored `application-local.properties` activated via `spring.profiles.active=local`.

### Frontend

```bash
cd frontend
npm install
npm run dev
```

## Repository structure

```
lexic-on/
├── backend/    Spring Boot API
├── frontend/   React + Vite SPA
└── docs/       roadmap.md (implementation plan) and decisions.md (architecture/decisions log)
```
