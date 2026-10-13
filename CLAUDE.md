# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Planning docs

`docs/local/roadmap.md` (slice-by-slice status) and `docs/local/decisions.md` (architecture/decisions history and rationale) are gitignored personal planning docs — not committed, so they won't exist in a fresh clone. Claude Code reads them directly from the local working copy.

## Commands

### Backend (`backend/`)

- Run locally: `./mvnw spring-boot:run` (`mvnw.cmd` on Windows)
- Build: `./mvnw package`
- Test: `./mvnw test`

Local runs need a real Postgres (e.g. a free Neon project), Azure Translator and GitHub OAuth credentials — the required values are the placeholders in `application.properties`. Set them as environment variables, or in a gitignored `backend/src/main/resources/application-local.properties` (picked up automatically via `spring.config.import`; `application-local.properties.example` next to it is the committed template). Tests don't need any of this — they run the real Flyway migrations against an in-memory H2 database in PostgreSQL mode instead, and Azure requests are intercepted by `MockRestServiceServer` (`backend/src/test/resources/application.properties`). H2 is a stand-in until the project switches to Testcontainers with a real Postgres.

### Frontend (`frontend/`)

- Install: `npm install`
- Dev server: `npm run dev`
- Build: `npm run build`
- Lint: `npm run lint`

## Architecture

- **Backend:** Java 25, Spring Boot 4, Spring Data JPA/Hibernate, Flyway migrations, Spring Security + OAuth2, PostgreSQL (hosted on Neon), LanguageTool (embedded, tokenization/lemmatization), Azure Translator (translations).
- **Frontend:** React + TypeScript + Vite + Tailwind CSS, built to static files.
- **Integration:** in production, the frontend build output is copied into `backend/src/main/resources/static` and served by the same Spring Boot app as one deployable unit (`docs/local/decisions.md` section 29) — this is also how `.github/workflows/deploy.yml` wires the two builds together before packaging and pushing to Render.
## Testing

Full rules: `docs/local/decisions.md` section 27. In short:

* **Service** → unit tests with mocked dependencies; no Spring context, no DB. Never use `@DataJpaTest` for a service by default.
* **Classes without dependencies, converters, exceptions** → unit tests with plain JUnit.
* **Repository** → `@DataJpaTest`; test our own queries, mappings, relationships and constraints.
* **Outbound HTTP client** → `@RestClientTest` + `MockRestServiceServer`.
* **Controller** → `@SpringBootTest` + MockMvc; no separate controller unit tests.
* **Critical user flows** → full integration / E2E tests.
* An additional integration test for a service may be added only for a specific reason and never replaces its unit tests.
* Do not introduce new test levels or testing patterns without checking `decisions.md` section 27 first.
* Test names: `methodUnderTest_expectedBehavior_whenCondition`; use GIVEN/WHEN/THEN comments directly before the block they describe.
* `assertThatThrownBy()`: the lambda contains only the one call expected to throw; prepare objects/arguments (including `new ...`) before it (Sonar `java:S5778`).
