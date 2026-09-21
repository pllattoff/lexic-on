# lexic.on — Decisions Log

> Working document for the lexic.on Capstone project.
> Purpose: preserve project context, decisions, architecture ideas, and open questions independently of the chat.
>
> This file was originally `lexic-on-project-notes-updated.md` at the repo root; moved here once the project moved from pure planning into implementation. The vertical-slice implementation roadmap and MVP scope live separately in [`docs/roadmap.md`](./roadmap.md) since that's a living checklist rather than a historical decisions log. A concise `docs/architecture.md` (current-state snapshot, not history) will be added once the codebase stabilizes enough to describe.
>
> **Working style agreement:** the user is implementing this project themselves for learning purposes. Claude's role here is planning/mentoring/unblocking on request, not autonomously writing the implementation — see `docs/roadmap.md` for how work is being sequenced.

---

## 1. Project overview

**Name:** lexic.on

lexic.on is a web application for learning vocabulary while reading texts in a foreign language.

### Core idea

The user inserts a text in a foreign language and reads it directly in the application.

Words are highlighted according to the user's vocabulary status. The user can click a word to see its translation and change its status.

The application should also recognize different grammatical forms of the same word. For example:

- Freund
- Freunde
- Freunden
- Freundes

should be recognized as forms of the same lemma: **Freund**.

### Main value proposition

The user can read foreign-language texts without constantly looking up words manually, while simultaneously building a personal vocabulary.

The application should show how much of a text the user already knows.

Example:

- 70% known
- 20% learning
- 10% unknown

---

## 2. Main vocabulary statuses

Current naming, ordering, and interaction model: see section 35.

- **UNKNOWN**
- **REVIEW**
- **KNOWN**

These are user-specific statuses.

The application should not have three separate vocabulary tables. Instead, one `UserVocabulary` table stores the status and the UI shows filtered views.

---

## 3. Main user workflow

Current planned flow:

```text
User inserts text
        ↓
Backend receives raw text
        ↓
Tokenization / normalization / word-form processing
        ↓
Determine lemmas / word entries
        ↓
Check shared linguistic database
        ↓
Existing words → use existing data
Missing words → obtain translation and save
        ↓
Determine user's vocabulary status
        ↓
Return text + word metadata + statistics
        ↓
React renders highlighted/clickable words
```

The user should be able to click a word and see its translation and vocabulary status.

---

## 4. Translation strategy

A previous idea was lazy translation only when a user clicks a word.

Current conclusion: because the application needs to classify the words in the text and the user's vocabulary views should contain translations, it makes sense to translate missing words during text processing.

### Important optimization

Do not send every occurrence separately.

Example:

```text
Text contains:
treffen
treffe
Freund
Freunden
laufen
laufen
```

First normalize / resolve forms and deduplicate where possible.

Then request only missing lexical entries in a batch.

For example:

```text
["treffen", "laufen", "vergessen"]
```

rather than one API request per word.

### Azure Translator (replaces DeepL — see section 34)

Azure Translator is used for translations (originally DeepL was planned here; switched after a pricing-model change, see section 34 for the full comparison and evidence).

Important caveat: translating isolated words without sentence context can be less accurate. This is acceptable for the initial Capstone/MVP, but may be improved later.

---

## 5. External APIs / linguistic services

### WiktAPI — REJECTED (confirmed API bug, see section 33)

WiktAPI was originally planned for dictionary info, word forms, and lemma/form resolution.

Investigation (see section 33 for full evidence) found that WiktAPI's `/v1/{edition}/word/{word}` endpoint (and its `/definitions`, `/translations`, `/pronunciations`, `/forms` sub-paths) does not URL-decode the `{word}` path parameter. This causes a 404 for **any** word containing non-ASCII characters (ä, ö, ü, ß) — confirmed even for basic words like `für`, `schön`, `groß`, not just inflected forms. Since most German vocabulary contains these characters, the endpoint is effectively unusable for this project. No encoding workaround (UTF-8 NFC/NFD, Latin-1, lowercase hex) fixes it. The `/search` endpoint decodes correctly but only returns lightweight metadata (word, lang_code, lang, pos), not full entry data, so it cannot replace `/word/{word}`.

**Decision: WiktAPI is dropped entirely.** Lemmatization is now handled by LanguageTool (below); dictionary/definition data is dropped from scope (not required by the current UI concept, which only needs lemma + translation + status).

### kaikki.org raw dump — REJECTED (no longer needed)

Was considered as an offline alternative data source (self-hosted Wiktextract dump) to bypass the WiktAPI bug. No longer needed now that LanguageTool handles lemmatization directly and dictionary definitions are out of scope.

### LanguageTool — SELECTED (primary lemmatization + POS/morphology solution)

Originally considered only for grammar/spelling checking. Re-evaluated and selected as the **primary lemmatization engine**, replacing WiktAPI/kaikki.org entirely.

Used as an embedded Java library (`languagetool-core` + one `language-*` module per language), not as an external API — no network calls, no rate limits, no encoding issues.

```java
Language de = Languages.getLanguageForShortCode("de-DE");
JLanguageTool lt = new JLanguageTool(de);
AnalyzedToken.getLemma();   // resolved lemma
AnalyzedToken.getPOSTag();  // POS + morphology tag (STTS for German, Penn Treebank for English)
```

Verified with a live prototype (Java 25, Maven) against the exact examples from this document:

```text
German:  gehe -> gehen | gehst -> gehen | gegangen -> gehen (irregular)
         Häusern -> Haus | Freunden -> Freund | größeren -> groß
         gesprochen -> sprechen (irregular) | lief -> laufen (irregular)

English: went -> go | going -> go | houses -> house
         better -> good / well (comparative) | spoken -> speak | ran -> run
```

Module footprint (Maven Central, v6.8) — small enough to bundle directly in the deployed jar, no persistent storage needed:

```text
languagetool-core  ~1.9 MB
language-de        ~21 MB
language-en        ~2.8 MB
language-uk        ~1.25 MB
```

(The commonly-cited "8 GB" LanguageTool figure refers to an optional n-gram dataset used only for advanced statistical grammar rules — not used here, not downloaded.)

Covers all three planned languages (de, en, uk) via separate lightweight modules, so the project is not locked into German only.

Known caveat: POS/morphology tags are raw tagset codes (e.g. `VER:1:SIN:KJ1:NON`, `VBD`) — a small mapping table is needed to show them as human-readable labels in the UI. Also, isolated single-word tagging can return multiple ambiguous lemma candidates (e.g. "Freunden" also matched a spurious verb reading); tagging full sentences (the normal use case here) lets LanguageTool's built-in disambiguator resolve this using context.

### DeepL — SUPERSEDED by Azure Translator (see section 34)

Was planned as the primary translation service. Superseded after discovering (mid-2026) that DeepL stopped offering its recurring monthly Free tier to new signups — new API customers now only get a one-time, non-renewing 1,000,000-character "Developer" allowance, after which a paid Growth plan is required. See section 34 for the full comparison against alternatives and the final decision (Azure Translator).

### spaCy

Considered as an NLP option, but it is Python-based and therefore less convenient for this Java/Spring Capstone. Superseded by the LanguageTool decision above.

---

# 6. Database decision

## Decision: PostgreSQL

The project will use **PostgreSQL** rather than MongoDB.

### Reason

The application has clearly structured relational data and several relationships between entities:

- users
- user-specific vocabulary
- words
- word forms
- translations
- texts

PostgreSQL fits this model naturally.

It also matches the planned Java/Spring Boot stack well through Spring Data JPA / Hibernate.

### Initial hosting decision

**Neon PostgreSQL** is the likely database hosting solution.

The intention is to use the free tier initially.

The expected project usage is small:

- primarily the developer/user
- potentially several additional users
- potentially a large personal vocabulary in German and English

The application is not initially intended for a large public user base.

If usage grows significantly, infrastructure can be upgraded later. The goal is not to optimize for millions of users at the Capstone stage.

---

# 7. Current database structure

Current proposed tables:

```text
AppUser
-------
id
email
role
created_at
UNIQUE(email)

UserIdentity
------------
id
user_id
provider
provider_user_id
UNIQUE(provider, provider_user_id)

WordEntry
---------
id
language
lemma
UNIQUE(language, lemma)


Translation
-----------
id
word_entry_id
target_language
translation
UNIQUE(word_entry_id, target_language, translation)


UserVocabulary
--------------
user_id
word_entry_id
target_language
status
UNIQUE(user_id, word_entry_id, target_language)
```

### Relationships

```text
AppUser
 ├── UserIdentity
 └── UserVocabulary
          │
          ▼
      WordEntry
       └── Translation
```

More explicitly:

```text
AppUser 1 ─── N UserIdentity

AppUser 1 ─── N UserVocabulary N ─── 1 WordEntry

WordEntry 1 ─── N Translation
```

`WordForm` is not used. Resolving an inflected form (e.g. "Freunden") to its lemma ("Freund") is no longer done via a stored forms table — it is computed at request time by LanguageTool (see section 5), which can lemmatize any form, including ones never seen before, without needing them pre-stored. Storing forms only made sense when lemma resolution depended on an external dictionary API; it does not apply to an embedded NLP tagger.

`UserDictionary` is not used.

A dictionary is implicit and is identified by:

```text
source_language + target_language
```

The `source_language` is taken from `WordEntry.language`.

The `target_language` is stored in `UserVocabulary.target_language`.

Only language pairs for which the user has vocabulary entries are displayed in the Dictionaries navigation.

There are no stored empty dictionaries.

---

# 8. WordEntry

`WordEntry` represents the lexical word/lemma itself.

Example:

```text
id       = 123
language = de
lemma    = Freund
```

The important point is that `Freund` is stored once as a shared linguistic entry.

It is not duplicated for every user.

---

# 9. WordForm — removed, replaced by runtime lemmatization

There is no `WordForm` table. Recognizing that different forms encountered in a text (Freund, Freunde, Freunden, Freundes) belong to the same vocabulary word — the core functionality of lexic.on — is done by LanguageTool at text-processing time (see section 5), not by looking up a stored forms table. This removed an entire class of ETL/data-maintenance work that the original WiktAPI/kaikki.org-based design would have required.

---

# 10. Translation

Multiple translations should be represented as **separate rows**, not as:

* multiple columns (`translation1`, `translation2`, ...)
* one string with separators (`"friend, boyfriend"`)

Example:

```text
WordEntry
---------
123 | de | Freund


Translation
--------------------------------
id | word_entry_id | target_language | translation
1  | 123            | uk              | друг
2  | 123            | uk              | приятель
3  | 123            | en              | friend
4  | 123            | en              | boyfriend
```

This is a normal one-to-many relational relationship:

```text
WordEntry 1 ─── N Translation
```

It allows additional translations to be added without changing the schema.

---

# 11. UserVocabulary

`UserVocabulary` stores the relationship between a user and a word, including the user's status and target language.

Example:

```text
user A → Freund → uk → KNOWN
user B → Freund → uk → REVIEW
user C → Freund → en → UNKNOWN
```

The same `WordEntry` can therefore have different statuses for different users and different target languages.

The three vocabulary sections in the UI are simply filtered views of `UserVocabulary`:

```text
Unknown → status = UNKNOWN
Review  → status = REVIEW
Known   → status = KNOWN
```

No separate tables are required for these three categories.

Suggested uniqueness constraint:

```text
UNIQUE(user_id, word_entry_id, target_language)
```

This ensures that a user has only one vocabulary entry for the same word and target language.

---

# 12. AppUser and OAuth2 authentication

## Current authentication plan

Use OAuth2 login.

GitHub is the provider already used in the course and will likely be implemented first.

Google login is also important because intended non-developer users may not have GitHub accounts.

## Important architectural decision

Do not make GitHub or Google the identity directly referenced by application data.

Instead:

```text
AppUser
-------
id
email
created_at
```

(Named `AppUser`/`app_user` rather than `User`/`user` — see section 38 for why.)

and:

```text
UserIdentity
------------
id
user_id
provider
provider_user_id
```

Example:

```text
AppUser #42

UserIdentity:
42 | github | 12345678
42 | google | 1092837465
```

This allows one application user to have multiple OAuth identities.

The rest of the application refers to:

```text
AppUser.id
```

not to a GitHub or Google ID.

Therefore:

```text
AppUser #42
 ├── GitHub identity
 ├── Google identity
 ├── Text
 └── UserVocabulary
```

## Account linking

The intention is that GitHub and Google can eventually be linked to the same application user.

Email can be used as part of the account-linking logic, subject to appropriate verification/safety checks.

The provider-specific identifier should also be stored:

```text
(provider, provider_user_id)
```

and should be unique.

Suggested database constraints:

```text
UNIQUE(provider, provider_user_id)
UNIQUE(email)
```

The exact account-linking UX/security flow is still an implementation detail to be finalized.

---

# 13. Frontend / backend architecture

Current planned stack:

### Backend

- Java
- Spring Boot
- REST API
- PostgreSQL
- Spring Data JPA / Hibernate
- OAuth2 / Spring Security

### Frontend

- React
- TypeScript
- Vite
- Tailwind CSS for styling, plus Radix UI primitives (or a shadcn/ui-style wrapper over them) for interactive widgets that are fiddly to hand-roll — popovers, dropdowns, tabs. Chosen over a full component kit (MUI/Ant Design) so the app isn't fighting an opinionated pre-made visual style to match the custom dark-themed mockups from section 35. The user is picking this up as they go rather than knowing it upfront.

The backend is responsible for:

- processing texts
- linguistic/word lookup
- translations
- vocabulary state
- database access
- authentication

The frontend is responsible for:

- displaying the text
- highlighting words
- showing statistics
- word interaction
- vocabulary views

---

# 14. Text rendering concept

The backend can return the original text together with word positions/metadata.

Example:

```json
{
  "text": "Ich treffe mich mit meinen Freunden.",
  "words": [
    {
      "start": 29,
      "end": 37,
      "lemma": "Freund",
      "status": "KNOWN"
    }
  ]
}
```

React can then render the text and make the relevant spans clickable.

This avoids requiring a complex text editor for the MVP.

---

# 15. Shared linguistic data vs. user data

A central architectural principle:

### Shared data

```text
WordEntry
Translation
```

These describe language/linguistic information that can be reused by all users.

### User-specific data

```text
AppUser
UserIdentity
Text
UserVocabulary
```

This separation avoids duplicating the same word and translation for every user.

---

# 16. Project naming

Current project name:

**lexic.on**

The name is intended as the user-facing brand.

Technical naming may need to use a format supported by the specific platform, for example:

```text
lexic-on
```

or another available variant.

This is still a practical naming/deployment detail rather than a core architectural decision.

---

# 17. Deployment

Current planned deployment direction:

```text
React frontend
      ↓
    Render

Spring Boot backend
      ↓
    Render

PostgreSQL
      ↓
     Neon
```

External services:

```text
Spring Boot
 ├── LanguageTool (embedded library, no network call)
 └── Azure Translator API
```

The initial goal is to stay within free tiers where practical.

---

# 18. Product copy / description

Current preferred German short description:

> **lexic.on** – Keine Lust mehr, bei fremdsprachigen Texten jedes zweite Wort nachzuschlagen? Mit lexic.on kannst du Texte in einer Fremdsprache lesen, unbekannte Wörter direkt anklicken, Übersetzungen bekommen und deinen persönlichen Wortschatz aufbauen. Die App erkennt auch verschiedene Wortformen und zeigt dir, wie viel vom Text du schon kennst. 🚀

Possible shorter opening alternatives:

- Genug davon, bei fremdsprachigen Texten jedes zweite Wort nachzuschlagen?
- Fremdsprachige Texte lesen, ohne ständig Wörter nachzuschlagen?
- Mehr lesen, weniger nachschlagen – mit lexic.on.
- Lies fremdsprachige Texte und lerne dabei genau die Wörter, die dir noch fehlen.

---

# 19. Current development stage

The project has moved from pure planning into early implementation. Per `docs/roadmap.md`, Slice 0 (walking skeleton) is in progress; no vertical slice has shipped end-to-end yet.

Current progress:

- project idea, core workflow, and data model defined (sections 1-15)
- UI sketched in Excalidraw and reviewed (section 35)
- PostgreSQL + Flyway migrations decided; Neon selected as hosting
- backend scaffold exists: Spring Boot app boots, Spring Security/OAuth2-client/Flyway/Lombok dependencies added, a `SecurityConfig` stub is in place (not yet configured with an actual OAuth2 login flow)
- CI/CD pipeline exists end-to-end (GitHub Actions -> Docker Hub -> Render, see `.github/workflows/deploy.yml`) — it only triggers on push to `main`; a separate `pull_request`-triggered workflow with tests + SonarCloud/JaCoCo is planned for Slice 2 (see section 37)
- frontend is still the default Vite scaffold (no real UI built yet)
- no database entities, migrations, or REST endpoints exist yet

---

# 20. Open questions

These still need to be decided:

### Database / linguistic model

- ~~How exactly to resolve an inflected form to its lemma?~~ RESOLVED: LanguageTool (embedded, offline), see section 5/33.
- Which word types should be stored (articles, prepositions, etc.)?
- How to handle words with multiple meanings?
- Whether translations should have additional metadata such as part of speech or preferred translation.
- Exact indexes and foreign-key constraints.
- Whether `Text` should store the processed word information or whether it should be generated dynamically.

### Text processing

- Exact tokenization approach.
- ~~Lemmatization / morphology solution.~~ RESOLVED: LanguageTool.
- How to map LanguageTool's raw POS/morphology tags (STTS for German, Penn Treebank for English) to human-readable labels for the UI.
- Whether to cache per-word lemmatization results (to avoid re-tagging repeated words across texts) or always compute on the fly — deferred as an optimization detail.
- How punctuation and special characters are handled.
- How words with the same spelling but different meanings are handled.
- Whether context should influence translation.

### Authentication

- Exact GitHub OAuth2 implementation.
- Google OAuth2 implementation.
- Exact account-linking flow between GitHub and Google.
- What happens if OAuth providers return different/missing email information.

### Application features

- User registration/profile details.
- ~~Whether users can edit/delete texts.~~ PARTIALLY RESOLVED: no in-place edit; a text is replaced wholesale via "add new text" (with a discard warning) — see section 35. No standalone delete-without-replacing action decided.
- ~~Whether vocabulary status changes automatically or only manually.~~ RESOLVED: hybrid — first click auto-tracks the word as UNKNOWN, then the user manually changes/removes the status — see section 35.
- How statistics are calculated.
- Whether vocabulary history/progress should be stored.

---

# 21. Important current decisions — quick reference

```text
Project:
    lexic.on

Database:
    PostgreSQL

Initial PostgreSQL hosting:
    Neon

Backend:
    Java + Spring Boot

Frontend:
    React + TypeScript + Vite

Authentication:
    OAuth2
    GitHub initially
    Google planned

Core shared linguistic entities:
    WordEntry
    Translation

Core user entities:
    AppUser
    UserIdentity
    Text
    UserVocabulary

Vocabulary statuses (UI order, see section 35):
    UNKNOWN -> REVIEW -> KNOWN

Translation:
    one translation = one Translation row

Vocabulary categories:
    filtered views of UserVocabulary,
    not separate tables

Identity:
    application data references AppUser.id,
    not GitHub/Google IDs directly

Main external services:
    Azure Translator (translation, F0 free tier)

Lemmatization / POS / morphology:
    LanguageTool (embedded Java library, offline — not an external API)

Deployment direction:
    Render + Neon
```

---

## 22. Documentation strategy — UPDATED, see also section 32

This file is intentionally a **working project context / decisions document**, now living at `docs/decisions.md`.

Split so far:

```text
CLAUDE.md        — operational guide for AI assistants working in the repo (commands, architecture map); thin until real code exists
docs/decisions.md — this file: history of decisions and rationale
docs/roadmap.md   — vertical-slice implementation plan + MVP scope (living checklist)
```

`README.md` was added at the repo root (project pitch + local setup instructions for both modules) once there was a real backend/frontend to describe.

Still pending (create once the codebase stabilizes enough to describe concisely, rather than pre-creating empty files):

```text
docs/architecture.md — current-state architecture snapshot (not history)
```

`database.md`, `deployment.md`, `testing.md` are not planned as separate files unless a section here grows large enough to need it — avoid pre-splitting into empty files.

When a decision changes, update the relevant section and the quick-reference section.

---

# 23. Repository structure

## Monorepo

The project will use a **monorepo**: frontend and backend are stored in the same Git repository.

Planned structure:

```text
lexic-on/
├── backend/
├── frontend/
├── .github/
│   └── workflows/
├── docs/
├── README.md
└── CLAUDE.md
```

The monorepo is a repository organization decision. It does not by itself determine the deployment architecture.

## Frontend deployment

The React frontend will be built into static files.

The planned deployment architecture is to serve these static frontend files together with the Spring Boot backend, so the application can be deployed as one application on Render.

---

# 24. CI/CD and deployment

## GitHub Actions

**GitHub Actions** will be used for the CI/CD pipeline.

The pipeline should cover the main project checks and deployment process, including:

- frontend build
- backend build
- backend tests
- quality checks
- SonarCloud analysis
- creation of the deployable application
- deployment to Render

The exact workflow configuration is still to be implemented.

---

# 25. SonarCloud

SonarCloud will be integrated through **GitHub Actions**.

The frontend and backend should be analyzed separately because they use different technologies and have different quality/coverage considerations.

## Backend

Backend SonarCloud analysis should include:

- Java code analysis
- test results
- JaCoCo coverage

## Frontend

Frontend SonarCloud analysis should cover the React/TypeScript code.

Frontend test coverage is **not currently part of the project requirements** and is not planned as a SonarCloud coverage target at this stage.

---

# 26. User roles and administration

The application will have at least two roles:

```text
USER
ADMIN
```

## USER

Normal users can use the main lexic.on functionality, including:

- reading texts
- viewing translations
- managing vocabulary
- changing vocabulary statuses
- viewing their own vocabulary/statistics

## ADMIN

Administrators will have access to a protected administration page.

The Admin Page should provide application-level data/statistics, for example information about:

- users
- texts
- vocabulary usage
- general application activity

The exact admin statistics and functionality are still open.

The admin page must be protected by backend authorization, not only hidden in the frontend.

---

# 27. Backend technology and development standards

Current backend requirements:

- **Java 25**
- Spring Boot
- Spring Security
- Spring Data JPA / Hibernate
- PostgreSQL
- Lombok
- REST API
- OAuth2
- unit tests
- integration tests
- JaCoCo

## Testing

The project should contain both:

- unit tests
- integration tests

Tests should use the **GIVEN-WHEN-THEN** structure where it is practical and improves readability.

Example:

```text
GIVEN a user with a known vocabulary word
WHEN the user processes a text containing that word
THEN the word should be returned with KNOWN status
```

This is a preferred testing style, not a requirement to force the structure into every test.

---

# 28. Dependency management principle

Before adding a dependency, first check whether **Spring Boot already provides a starter or supported solution** for the required functionality.

Preferred approach:

```text
Need functionality
      ↓
Check Spring Boot starters / Boot-supported dependencies
      ↓
Use the Boot-supported solution if available
      ↓
Only add a lower-level/direct Spring dependency
when there is a clear reason
```

The project should avoid adding lower-level Spring dependencies independently when the required functionality is already provided through a Spring Boot starter or Boot-managed dependency.

Spring Boot should manage dependency versions where possible.

This principle is intended to keep the dependency tree simpler, follow standard Spring Boot conventions, and avoid unnecessary dependency/version management.

---

# 29. Updated deployment architecture

The current intended architecture is:

```text
LOCAL DEVELOPMENT

React
  ↓
Spring Boot
  ↓
Neon PostgreSQL


PRODUCTION

React
  ↓
build static files
  ↓
Spring Boot
  ↓
Render
  ↓
Neon PostgreSQL

Spring Boot
  ├── LanguageTool (embedded library, no network call)
  └── Azure Translator API
```

The important deployment decision is that the React application is built as static assets and served together with the Spring Boot application.

Neon remains the external PostgreSQL database.

---

# 30. Updated development / project decisions

The following are now considered current project decisions:

```text
Repository:
    Monorepo

Repository structure:
    frontend/
    backend/
    .github/
    docs/

Frontend:
    React + TypeScript + Vite
    Tailwind CSS + Radix UI (shadcn/ui-style) for interactive widgets
    Built as static files
    Served together with Spring Boot

Backend:
    Java 25
    Spring Boot
    Lombok
    REST API
    Spring Security / OAuth2
    Spring Data JPA / Hibernate

Database:
    PostgreSQL
    Neon
    Schema managed by Flyway migrations (versioned SQL in src/main/resources/db/migration),
    not Hibernate ddl-auto — spring.jpa.hibernate.ddl-auto=validate

Authentication:
    OAuth2
    GitHub initially
    Google planned

Roles:
    USER
    ADMIN

Testing:
    Unit tests
    Integration tests
    GIVEN-WHEN-THEN where practical
    JaCoCo for backend

Quality:
    SonarCloud
    GitHub Actions
    Backend and frontend analyzed separately
    Frontend coverage currently not required

CI/CD:
    GitHub Actions

Deployment:
    Render + Neon

External services:
    Azure Translator (translation, F0 free tier)

Lemmatization / POS / morphology:
    LanguageTool (embedded Java library, offline)
```

---

# 31. Updated open questions

The following questions remain open:

### Deployment / CI/CD

- Exact GitHub Actions workflow structure.
- Exact Render deployment configuration.
- How the React build output will be integrated into the Spring Boot application.
- Exact production environment variables and secrets.
- Whether development and production should use separate Neon databases/projects/branches.

### SonarCloud

- Exact SonarCloud project configuration.
- Exact frontend/backend project separation.
- Exact JaCoCo report configuration and paths.
- Final quality gates.

### Administration

- Which statistics should be shown on the Admin Page?
- Whether admin statistics should be calculated dynamically or partly stored.
- Whether additional admin actions are required.

### Testing

- Exact unit-test scope.
- Exact integration-test scope.
- Test database strategy.
- Whether Testcontainers should be used for PostgreSQL integration tests. Interim state (not a resolution of this question): tests currently run against an in-memory H2 database (`MODE=PostgreSQL`, Flyway disabled — see `backend/src/test/resources/application.properties`), explicitly marked as a temporary stand-in until real Flyway migrations exist and/or the project switches to Testcontainers with a real Postgres instance.

### Dependency management

- Exact Spring Boot starters required by the final implementation.
- Which third-party dependencies are justified beyond Spring Boot's provided solutions.

---

# 32. Documentation strategy — see section 22 for the current, updated version

(Superseded by the update in section 22 — kept here for history rather than deleted, per this file's own decisions-log nature.)


## UI Navigation

Desktop version uses a visible left sidebar.

Main sections:
- Text
- Dictionaries

Dictionaries contains language-pair sub-items, displayed as nested navigation items:
- English ↔ Deutsch
- English ↔ Українська
- Deutsch ↔ Українська

Language pairs should be displayed dynamically based on the user's existing vocabulary data, rather than being hardcoded.

The application is implemented as a single React SPA with separate routes/pages, for example:
- / → Text
- /dictionaries → Dictionaries
- /dictionaries/{language-pair} → specific dictionary


# 33. WiktAPI investigation outcome and final lemmatization decision

This section documents how the WiktAPI question raised above (inconsistent `404`s on German words) was resolved, so future sessions don't need to re-investigate it.

## Root cause: confirmed API bug, not a data-coverage gap

Tested directly against `api.wiktapi.dev` using Node.js `fetch` (to rule out any local shell/encoding artifacts). Findings:

- `GET /v1/{edition}/word/{word}` (and its `/definitions`, `/translations`, `/pronunciations`, `/forms` sub-paths) **does not URL-decode the `{word}` path parameter** before doing its lookup.
- This means **any** word containing non-ASCII characters fails, regardless of encoding scheme tried: standard UTF-8 percent-encoding, Unicode NFD-normalized percent-encoding, Latin-1 percent-encoding, and lowercase-hex percent-encoding were all tested — all returned `404`, with the error message showing the literal undecoded string was used as the lookup key (e.g. `"No entries found for \"f%C3%BCr\""`). Sending raw unencoded UTF-8 bytes in the path returns `400 Bad Request`.
- This is **not limited to rare inflected forms** — it fails for extremely common base words like `für`, `schön`, `groß`, since German vocabulary overwhelmingly contains ä/ö/ü/ß.
- The `/search?q=...` endpoint *does* decode its query-string parameter correctly (found `Häusern`, `größeren`, `Straße`, `groß` without issue), confirming the underlying data exists — the bug is specifically in path-parameter handling. However, `/search` only returns lightweight metadata (`word`, `lang_code`, `lang`, `pos`), not full entry data, so it cannot substitute for `/word/{word}`.

**Conclusion:** WiktAPI cannot be used to retrieve full dictionary data for essentially any real German word via a standards-compliant HTTP client. It is dropped from the project entirely (not used even as a fallback).

## Final decision

- **Lemmatization + POS/morphology:** LanguageTool, embedded as a Java library (`languagetool-core` + `language-de`/`language-en`/`language-uk`). Runs offline, no external API, no rate limits, no encoding issues. Verified directly against this document's test examples (Freunden, Häusern, gegangen, gesprochen, größeren, and English equivalents) — see section 5 for the full results and module sizes.
- **kaikki.org raw dump:** considered as an offline alternative, ultimately not needed — dropped from scope along with dictionary definitions, since the UI concept only requires lemma + translation + vocabulary status, not definitions.
- **Translations:** ~~DeepL~~ → **Azure Translator** (switched after a DeepL pricing-model change; see section 34 for the full comparison and live verification).
- **`WordForm` table:** removed from the schema (section 7/9) — no longer needed since lemma resolution happens at request time via LanguageTool instead of via a precomputed forms table.

---

# 34. Translation provider: DeepL → Azure Translator

## Why DeepL was dropped as the default choice

DeepL was originally planned as the translation provider (section 4). Re-checked its current pricing during this project (2026) and found DeepL had changed its offering: the old recurring "API Free" plan (500,000 characters/month, forever) is **no longer available to new signups**. New API customers instead get a **"Developer" plan: 1,000,000 characters total, one-time, non-renewing** — once used up, only a paid "Growth" plan continues service. This doesn't block the project outright (given the "translate once, cache forever" strategy from section 4, actual lifetime usage for a personal-scale vocabulary is well under 1M characters), but it removes the "free forever, resets monthly" safety margin the original plan assumed.

## Alternatives compared

| Service | Free allowance | Renewal | Notes |
|---|---|---|---|
| DeepL (current) | 1,000,000 characters | one-time only | best raw translation quality, but budget doesn't reset |
| Google Cloud Translation | 500,000 characters/month | recurring, permanent | requires a GCP project with billing enabled |
| **Azure Translator (F0)** | **2,000,000 characters/month** | **recurring, permanent** | hard-capped — returns an error instead of billing you if exceeded, no overage risk |
| Amazon Translate | 2,000,000 characters/month | recurring, but only for 12 months | reverts to paid after a year |
| LibreTranslate (self-hosted, Argos Translate) | unlimited (own server) | — | Python-based NMT engine, noticeably lower translation quality than DeepL/Google/Azure, adds a separate service to host/maintain — rejected as not worth the complexity/quality trade-off for this project |

## Decision: Azure Translator (F0 free tier)

Selected for the largest **recurring** free allowance (2M characters/month) combined with hard-capped, no-overage-risk behavior — safer for a project without billing oversight than a service that would otherwise start charging.

### Setup notes (for future reference — the Azure Portal UI/branding around this changed in 2026)

- Azure rebranded "Azure AI Foundry" to **"Microsoft Foundry"** in Jan 2026, and Cognitive Services (including Translator) is now presented under the umbrella name **"Foundry Tools"**. Searching "Translator" in the portal marketplace can be confusing because Azure now promotes the more general "Foundry resource" (for LLM-based translation) ahead of the classic standalone "Translator" resource that has the free F0 tier. Use the direct resource-creation link to skip the ambiguity: `https://portal.azure.com/#create/Microsoft.CognitiveServicesTextTranslation`.
- **Region "Global" cannot be used when creating a new Resource Group at the same time** ("The location 'Global' is not available while creating a new resource group") — the resource group itself needs a real physical region even though the Translator resource supports "Global". Fix: just pick a real region for the whole resource (e.g. North Europe); this only means requests need an extra `Ocp-Apim-Subscription-Region` header — no other downside.
- New/trial Azure subscriptions can additionally hit `RequestDisallowedByAzure: The selected region is currently not accepting new customers` for popular regions (e.g. West Europe) — this is a temporary capacity-allocation restriction for new subscriptions, unrelated to this project. Fix: try a different region (North Europe worked).
- Azure account creation requires a non-prepaid card + phone verification for identity purposes (temporary ~$1 hold, no actual charge unless you explicitly move to pay-as-you-go) — same category of friction as DeepL/Google, not a differentiator.
- Networking: choose **"All networks, including the internet, can access this resource"** — the API key is what protects access; restricting by IP/VNet would just block Render (no static egress IP on free tier) and local testing for no benefit.
- Identity (managed identity): leave **Off** — only relevant if the Translator resource itself needed to authenticate to other Azure resources, which it doesn't here.

### Live verification (resource created in North Europe, F0 tier)

Request format used (regional resource, so the region header is required):

```
POST https://api.cognitive.microsofttranslator.com/translate?api-version=3.0&from={src}&to={dst}
Ocp-Apim-Subscription-Key: <key>
Ocp-Apim-Subscription-Region: northeurope
Content-Type: application/json

[{"Text": "..."}]
```

Tested against the same sentences used for the LanguageTool verification (section 5/33), across all three planned languages:

```text
DE → EN:  "Ich gehe jeden Tag mit meinen Freunden spazieren. Gestern sind wir
           zu den größeren Häusern gelaufen und haben mit unseren Nachbarn
           gesprochen. Wir haben schöne Blumen gesehen."
       →  "I go for a walk with my friends every day. Yesterday we walked to
           the bigger houses and talked to our neighbors. We saw beautiful
           flowers."

EN → DE:  "He went for a run this morning and spoke to his friends about the
           houses they had seen. It was a better day than yesterday."
       →  "Heute Morgen war er joggen gegangen und sprach mit seinen Freunden
           über die Häuser, die sie gesehen hatten. Es war ein besserer Tag
           als gestern."

EN → UK:  (same source text)
       →  "Сьогодні вранці він пішов на пробіжку і розповів друзям про
           будинки, які вони бачили. День був кращий, ніж учора."
```

All three translations are natural and semantically accurate — good enough for a vocabulary-learning app (not literary translation). Confirms Azure Translator works correctly for all three planned language pairs (de/en/uk) end-to-end, including the free F0 tier and the region-header requirement.

## Additional live verification of LanguageTool on full sentences (not just isolated words)

Also re-ran the LanguageTool lemmatization check (section 5/33) on full sentences instead of isolated words, to confirm sentence context improves disambiguation as expected:

```text
German ("Ich gehe jeden Tag mit meinen Freunden spazieren. Gestern sind wir
zu den größeren Häusern gelaufen und haben mit unseren Nachbarn gesprochen.
Wir haben schöne Blumen gesehen."):
  gehe→gehen, Freunden→Freund, größeren→groß, Häusern→Haus,
  gelaufen→[gelaufen, laufen], gesprochen→[gesprochen, sprechen],
  gesehen→[gesehen, sehen], schöne→schön, Blumen→Blume, Nachbarn→Nachbar

English ("He went for a run this morning and spoke to his friends about the
houses they had seen. It was a better day than yesterday."):
  went→go, friends→friend, houses→house, had→have, seen→see, better→[good, well]
```

Confirms context helps (compare to the isolated-word test in section 5, where forms like "Freunden" returned several spurious candidate lemmas that disappeared once given sentence context). But it is **not a complete fix**: German past participles (`gelaufen`, `gesprochen`, `gesehen`) still return two lemma candidates each even with full sentence context (a structural ambiguity in the German tagset between the perfect-tense-verb reading and the predicate-adjective reading), and in the English test the tagger mis-tagged `spoke` as a noun (`NN`) instead of a verb (`VBD`) despite unambiguous verb-slot context — though the correct lemma `speak` was still present among the candidates. **Conclusion carried forward:** the app-level lemma-matching logic needs a simple disambiguation rule (e.g., prefer a candidate lemma that differs from the surface token over one that equals it) rather than assuming a single unambiguous reading is always returned.

---

# 35. UI wireframes (Excalidraw, 2026-09-17): status naming/ordering, vocabulary population model, interface language

First round of Excalidraw wireframes reviewed, covering the Text page (paste/process/highlight/click-word popup) and the Dictionaries page (per-language-pair summary cards + per-word list). This resolved several open items and superseded the original status naming from section 2.

Source files: `docs/local/wireframes/lexicon-2026-09-17.excalidraw` (+ `.png` export). `docs/local/` is gitignored (not committed), so these only exist in the local working copy — open them directly to see the actual sketch rather than relying solely on this write-up.

## Interface language

English only for now. Additional interface languages may be added later, but this is explicitly not a near-term priority — no i18n abstraction is being built ahead of need.

## Vocabulary status naming and order — supersedes section 2

The three statuses, in their current naming and order:

```text
Unknown -> Review -> Known
(red)      (yellow)   (green)
```

This order is deliberately left-to-right as a "progression" (a word moves from not-known toward known) and must be used consistently everywhere statuses are shown as a row: the word popup, the per-language-pair stat bar, and the status tabs/filters inside a specific dictionary view.

**Decision: the backend enum is named `UNKNOWN`/`REVIEW`/`KNOWN`**, matching the UI directly — no separate label-mapping layer, since there is no i18n abstraction yet.

## When a word enters `UserVocabulary` (lazy, click-triggered)

A word encountered in a processed text is **not** automatically inserted into `UserVocabulary` just by appearing in the text. Two states exist per word, per user:

- **Not in the user's vocabulary at all** — the word has never been clicked. No highlight is shown. No `UserVocabulary` row exists. Not counted in the Known/Review/Unknown stats.
- **In the user's vocabulary** — the word has an explicit `UserVocabulary` row with one of the three statuses, and is highlighted accordingly.

The transition from the first state to the second happens **automatically on click**: clicking a word to see its translation creates a `UserVocabulary` row with status `Unknown` (the click itself is treated as evidence the word wasn't known well enough to skip). The same popup that shows the translation also lets the user immediately correct the status to `Review` or `Known` if the click was just curiosity rather than genuine unfamiliarity.

**Undo / remove from vocabulary:** the popup also needs a delete/remove action (reusing the same trash icon already used for per-word rows in the Dictionaries list view, section on "2.a — Displaying a specific dictionary") to fully remove the `UserVocabulary` row again, returning the word to the untracked state (no highlight) — for the case where the user clicked a word by mistake or out of curiosity and doesn't want it tracked at all. This is distinct from changing status: status change keeps the row (moves it between Unknown/Review/Known); delete removes the row entirely.

**Consequence for the "% of text known" statistic:** this stat only reflects words the user has already clicked at least once, not the entire text from the first read. This is intentional — the personal dictionary builds up progressively as the user reads and interacts with words — not a bug to fix later.

## Other details captured from the wireframes (lower priority, no decision needed yet)

- No text-history/save feature for now: only one active/current text per user is kept (the "add new text" action warns the current text will be discarded). May be revisited as a separate feature later, but out of scope for the initial slices.
- Guest (unauthenticated) users can paste text and click words to see translations, but have no access to the Dictionaries section; a prompt should invite them to register for full functionality.
- Avatar pulled from the OAuth2 provider where possible, with click-to-logout; login must be reachable pre-registration too.
- Sidebar is collapsible (hamburger toggle).
- Under Dictionaries, each language pair gets its own nested sub-items in the sidebar for Unknown/Review/Known (not just the language pair itself) — partly for navigation convenience, partly so the sidebar doesn't look sparse.
- Manually adding a word to the dictionary (without it appearing in a processed text) is a possible future feature, not required now. Rough shape if/when it's built: user types a word, the translation service is called, the translation is shown, and a "Save" action adds it to `UserVocabulary`. Not designed in detail yet.
- The `+` in the Dictionaries overview page header is an "add dictionary" button — lets a user manually start tracking a new language pair without having processed any text in it yet. Same status as manually adding a word above: possible future feature, not required for the MVP slices, can be deferred.
- The specific-dictionary view needs a back button (a left-arrow icon next to the language-pair title, not a remove/delete action) to return to the Dictionaries overview list. The exact placement/pattern isn't finalized — the actual requirement is just that navigating back out of a specific dictionary must be convenient, however it ends up implemented.
- Layout must be responsive down to mobile widths.
- Word highlighting should stay subtle/low-contrast so the underlying text remains easy to read, even though the wireframe itself uses fairly strong colors as a placeholder.

---

# 36. Code reuse principle

Applies in both directions across slices:

- **Backward:** before writing new logic in a slice — a service method, a repository query, a React component, a validation rule — check whether an earlier slice already built something that does the same job (or close to it), and reuse or extend it rather than writing a parallel copy.
- **Forward:** when planning or writing the guide for a slice, check `docs/roadmap.md` for whether a later, already-planned slice will need the same piece of logic/data/component. If so, leave a short cross-reference note in both slices' roadmap entries (e.g. Slice 5 already does this: "the same trash icon used for per-word rows in Slice 6's dictionary list view"). This is not speculative — it's grounded in a later slice that's already on the roadmap, not a guess about the future — so it doesn't conflict with the project's general "don't design for hypothetical requirements" default. The point is only to note the connection so the earlier slice's code is shaped with that known reuse in mind, not to build the abstraction ahead of time.

Neither direction is a mandate to abstract preemptively: two pieces of code that only coincidentally look similar today, but represent different concerns, should stay separate rather than being forced under one shared abstraction "just in case."

---

# 37. CI quality gate moved up from the tail of the roadmap (2026-09-20)

Slice 0 was left with one known gap on completion: no `pull_request`-triggered GitHub Actions workflow exists yet, so tests only run (as part of `mvn package` in `deploy.yml`) after code is already merged to `main`, not as a merge gate. The original roadmap had deferred fixing this all the way to the last slice (CI/CD + quality gates), bundled with SonarCloud/JaCoCo wiring.

Decision: don't backfill Slice 0's gap before starting Slice 1 — the user needs to demo authentication (Slice 1) imminently, and the gap doesn't block that. Instead, the CI quality gate (PR-triggered test run + SonarCloud/JaCoCo, previously the last item on the roadmap) is moved up to immediately follow Slice 1, becoming the new Slice 2 — earlier than originally planned, but not as early as Slice 0, since a PR quality gate is more meaningful once there's real feature code (auth) for it to check, rather than against a near-empty scaffold. All slices after the original Slice 1 shift up by one number accordingly (old Slice 2 -> 3, ... old Slice 7 -> 8); the old Slice 8 is retired as a separate entry.

The old Slice 8 description also mentioned "deployment hardening" alongside the CI/tests/SonarCloud work. That phrase was never given concrete content anywhere in this document (no rollback strategy, secrets rotation, staging environment, or similar has been decided) — it's dropped rather than carried forward, since there's nothing specific to move.

---

# 38. `AppUser` renamed from `User` (2026-09-20)

While writing the Slice 1 implementation guide, the entity/table previously called `User`/`user` (section 12/21) was renamed to `AppUser`/`app_user`. Two independent reasons converged on the same rename:

- `user` is a reserved word in PostgreSQL and needs quoting or a different name regardless.
- The user asked for a clearer name that signals "our own application user record," as distinct from a GitHub/Google identity — which matters here specifically because this entity's whole reason for existing (section 12) is to *not* be a raw OAuth identity, so a name that could be mistaken for one undercuts the point.

The foreign-key columns on `UserIdentity` and `UserVocabulary` keep the name `user_id` (referencing `app_user.id`) — renaming the column too wasn't necessary to resolve either concern above, and `user_id` reads fine in context on a table that isn't itself named `user`.

---

# 39. Backend package structure: layer-based, not feature-based (2026-09-20)

While planning Slice 1, feature/domain packages (e.g. `com.lexicon.backend.auth`, `.vocabulary`) were briefly considered, reasoning that they'd mirror the project's vertical-slice delivery order and give better encapsulation as the codebase grows. That reasoning doesn't apply here: "vertical slices" (section 36, `docs/roadmap.md`) describes the *order* features are built in, not how backend code is packaged, and the project's actual scale (a handful of entities total) never grows large enough for the layer-package "dumping ground" concern to bite.

**Decision: classic layer-based packages**, matching what the user's bootcamp taught and has always used:

```text
com.lexicon.backend.model       — JPA entities
com.lexicon.backend.repository  — Spring Data repositories
com.lexicon.backend.service     — business logic
com.lexicon.backend.controller  — REST controllers
com.lexicon.backend.enums       — enums (e.g. a future vocabulary-status enum)
com.lexicon.backend.security    — Spring Security wiring (`SecurityConfig`, from Slice 0) and OAuth2-specific classes (custom `OAuth2UserService`, custom `OAuth2User` principal) — kept here rather than forced into `service`, since they're Spring Security infrastructure classes, not ordinary business-logic services
```

This is the convention for the whole backend going forward, not just Slice 1.

---

# 40. CSRF token can be stale right after a fresh login; client retries once (2026-09-20)

While testing Slice 1's logout locally (reproduced live via browser automation, with and without React StrictMode — ruled StrictMode out as the cause), a repeatable pattern emerged: immediately after a fresh GitHub login, the *first* `POST /logout` fails with `403` even though the `XSRF-TOKEN` cookie the frontend reads and sends matches what's in the browser's cookie jar. The rejected request itself re-syncs the cookie, and an immediate second attempt with the freshly-read value always succeeds. This reproduced consistently across multiple independent tests.

The likely cause is Spring Security's CSRF token rotation on successful authentication (invalidating the pre-login token, a deliberate hardening measure) racing with how quickly the cookie becomes available to the very next request — this wasn't nailed down to full certainty (no way to inspect the raw `Set-Cookie` traffic from JS), but the reproducible fail-once-then-succeed pattern is solid enough to act on.

**Decision:** the frontend retries the logout request once on a `403` response, rather than chasing the exact root cause further. See `frontend/src/components/AccountMenu.tsx`'s `handleLogout`. This is a client-side mitigation, not a security weakening — CSRF protection itself (see the `SpaCsrfTokenRequestHandler` in `SecurityConfig`, documented in the Slice 1 guide) is untouched; it only smooths over a timing quirk specific to the first state-changing request right after login. Worth remembering for Slice 5: any vocabulary-status write endpoint hit immediately after a fresh login could hit the same one-time 403, so the same retry-once pattern (or a small shared fetch helper implementing it) should be reused there rather than re-discovered.