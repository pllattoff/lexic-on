# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Planning docs

`docs/local/roadmap.md` (slice-by-slice status) and `docs/local/decisions.md` (architecture/decisions history and rationale) are gitignored personal planning docs — not committed, so they won't exist in a fresh clone. Claude Code reads them directly from the local working copy.

## Commands

### Backend (`backend/`)

- Run locally: `./mvnw spring-boot:run` (`mvnw.cmd` on Windows)
- Build: `./mvnw package`
- Test: `./mvnw test`

Local runs need a real Postgres (e.g. a free Neon project), Azure Translator, GitHub OAuth and Google OAuth credentials — the required values are the placeholders in `application.properties`. Set them as environment variables, or in a gitignored `backend/src/main/resources/application-local.properties` (picked up automatically via `spring.config.import`; `application-local.properties.example` next to it is the committed template). Tests don't need any of this — they run the real Flyway migrations against an in-memory H2 database in PostgreSQL mode instead, and Azure requests are intercepted by `MockRestServiceServer` (`backend/src/test/resources/application.properties`). H2 is a stand-in until the project switches to Testcontainers with a real Postgres.

### Frontend (`frontend/`)

- Install: `npm install`
- Dev server: `npm run dev`
- Build: `npm run build`
- Lint: `npm run lint`

## Architecture

- **Backend:** Java 25, Spring Boot 4, Spring Data JPA/Hibernate, Flyway migrations, Spring Security + OAuth2, PostgreSQL (hosted on Neon), LanguageTool (embedded, tokenization/lemmatization), Azure Translator (translations).
- **Frontend:** React + TypeScript + Vite + Tailwind CSS, built to static files. Interface translations (en/de/uk) use react-i18next; the texts live in `frontend/src/locales/*.json`, and a new text needs a key in all three files. The interface language is separate from the source/target languages of the text.
- **Integration:** in production, the frontend build output is copied into `backend/src/main/resources/static` and served by the same Spring Boot app as one deployable unit (`docs/local/decisions.md` section 29) — this is also how `.github/workflows/deploy.yml` wires the two builds together before packaging and pushing to Render.

## Privacy policy

The public privacy policy is `frontend/public/privacy.html` (served at `/privacy.html`, permitted without login in `SecurityConfig`; its URL is entered in the Google OAuth consent screen). It describes what data is stored, which third-party services process it and which browser storage is used. Update the page and its "Last updated" date whenever a change affects that: a new sign-in provider, new personal data in the database, a new third-party service (translator, analytics, error tracking), a new cookie or browser storage use, or a hosting/database change.

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
