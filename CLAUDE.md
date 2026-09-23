# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project state

lexic.on is past the empty-scaffold stage, but no vertical feature slice has shipped end-to-end yet — see `docs/local/roadmap.md` for the current slice-by-slice status and `docs/local/decisions.md` for the full architecture/decisions history and rationale. Both are gitignored personal planning docs (not committed, so they won't exist in a fresh clone) — Claude Code reads them directly from the local working copy.

Currently in place: GitHub OAuth2 login/logout wired end-to-end on the backend (session-based, CSRF-protected — `SecurityConfig`/`AuthController`), a minimal real frontend UI for it (`AccountMenu`: login link or avatar + click-to-logout), and a complete CI/CD setup — `deploy.yml` (push to `main` → Docker Hub → Render) plus PR-triggered `ci-backend.yml`/`ci-frontend.yml` (tests, build/lint, SonarCloud). No `AppUser`/`UserIdentity` persistence exists yet, though, so there are still no database entities or Flyway migrations — auth is session-only for now.

## Commands

### Backend (`backend/`)

- Run locally: `./mvnw spring-boot:run` (`mvnw.cmd` on Windows)
- Build: `./mvnw package`
- Test: `./mvnw test`

Local runs need `DB_URL`, `DB_USER`, `DB_PASSWORD` for a real Postgres connection (e.g. a free Neon project) — set as environment variables, or in a gitignored `backend/src/main/resources/application-local.properties` (picked up automatically via `spring.config.import`, see `application.properties`). Tests don't need any of this — they run against an in-memory H2 database instead (`backend/src/test/resources/application.properties`), which is an explicitly temporary stand-in (see that file's comment) until real Flyway migrations exist and/or the project switches to Testcontainers.

### Frontend (`frontend/`)

- Install: `npm install`
- Dev server: `npm run dev`
- Build: `npm run build`
- Lint: `npm run lint`

## Architecture

- **Backend:** Java 25, Spring Boot, Spring Data JPA/Hibernate, Flyway migrations, Spring Security + OAuth2, PostgreSQL (hosted on Neon).
- **Frontend:** React + TypeScript + Vite + Tailwind CSS, built to static files.
- **Integration:** in production, the frontend build output is copied into `backend/src/main/resources/static` and served by the same Spring Boot app as one deployable unit (`docs/local/decisions.md` section 29) — this is also how `.github/workflows/deploy.yml` wires the two builds together before packaging and pushing to Render.
- Full architecture rationale and decision history: `docs/local/decisions.md`. Implementation plan and current slice status: `docs/local/roadmap.md`.
