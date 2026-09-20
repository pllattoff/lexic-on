# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project state

lexic.on is past the empty-scaffold stage, but no vertical feature slice has shipped end-to-end yet — see `docs/roadmap.md` for the current slice-by-slice status and `docs/decisions.md` for the full architecture/decisions history and rationale.

Currently in place: a Spring Boot backend scaffold (Spring Security/OAuth2-client/Flyway/Lombok dependencies added, a `SecurityConfig` stub not yet wired to a real login flow), a default Vite+React frontend template (no real UI yet), and a working CI/CD pipeline (GitHub Actions → Docker Hub → Render). No database entities, migrations, or real REST endpoints exist yet.

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
- **Integration:** in production, the frontend build output is copied into `backend/src/main/resources/static` and served by the same Spring Boot app as one deployable unit (`docs/decisions.md` section 29) — this is also how `.github/workflows/deploy.yml` wires the two builds together before packaging and pushing to Render.
- Full architecture rationale and decision history: `docs/decisions.md`. Implementation plan and current slice status: `docs/roadmap.md`.
