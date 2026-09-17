# lexic.on — Implementation Roadmap

> Living document. Check items off as they're built. Rationale for *why* things are designed this way lives in `docs/decisions.md` — this file is just the plan and current status.

## Working agreement

The user is implementing this project themselves, for learning — that's the point of the capstone. Claude's role is planning, explaining, unblocking, and reviewing on request, not writing the implementation autonomously. Help is asked for selectively (to get unstuck, or to speed up a part deliberately). Implementation proceeds in **vertical slices**: each slice touches every layer (DB → backend → frontend) and produces something end-to-end working, rather than building one architectural layer at a time across the whole app.

## MVP scope

MVP = Slices 0–5 below. That's the point at which the core product value (read a text, see translations, track vocabulary status, browse it per language pair) works end-to-end for a real logged-in user. Slices 6–8 (Google login/account linking, admin page, CI/CD polish) are still required for the full capstone deliverable, not optional — they just come after the core product proves itself.

## Slices

- [ ] **Slice 0 — Walking skeleton**
  Spring Boot backend with one trivial endpoint, React+Vite frontend that calls it, Neon connection wired, one full deploy to Render. No real feature — the goal is retiring integration/deployment risk before building on top of it.

- [ ] **Slice 1 — Authentication (GitHub OAuth2)**
  `User` + `UserIdentity` tables, Spring Security OAuth2 login (GitHub first), protected endpoint pattern established. Done early (moved up from the original draft) specifically so `UserVocabulary` in Slice 4 can reference a real logged-in user from the start, instead of a throwaway hardcoded test-user id that would need to be ripped out later.

- [ ] **Slice 2 — Core reading pipeline**
  Paste text → backend tokenizes/lemmatizes via LanguageTool → returns text + word spans + lemma. Frontend renders the text with plain clickable word spans (no status color yet — that needs Slice 4). Proves the core NLP pipeline in isolation, no translation/persistence of vocabulary yet.

- [ ] **Slice 3 — Translations**
  `WordEntry` + `Translation` tables, Azure Translator integration, batch-translate-missing-words (dedup first, per `docs/decisions.md` section 4). Clicking a word shows its translation.

- [ ] **Slice 4 — Vocabulary status + stats**
  `UserVocabulary` table (tied to the real user from Slice 1). Statuses shown left-to-right as a progression: Unknown -> Review -> Known (red/yellow/green) — see `docs/decisions.md` section 35 for the naming/ordering rationale. A word gets no highlight and no `UserVocabulary` row until first clicked; clicking auto-creates the row as `Unknown`, with buttons in the same popup to change status or delete the row entirely (untrack the word again). % known/review/unknown stat bar for the text reflects only words already clicked at least once.

- [ ] **Slice 5 — Dictionaries navigation**
  Sidebar (Text / Dictionaries), dynamic language-pair sub-items based on the user's actual vocabulary data, per-status word lists.

- [ ] **Slice 6 — Google OAuth2 + account linking**
  Extends Slice 1: add Google as a second provider, link to the same `User` via email/verification logic.

- [ ] **Slice 7 — Roles + admin page**
  USER/ADMIN roles, backend-authorized admin page with app-level stats.

- [ ] **Slice 8 — CI/CD + quality gates**
  GitHub Actions pipeline, JaCoCo, SonarCloud (frontend/backend analyzed separately), deployment hardening. Tests themselves should be written alongside each slice as it's built, not deferred to this slice — this slice is about wiring the pipeline around tests that already exist.

## Pending input

- Excalidraw UI sketches — to be provided, will sharpen the frontend side of Slices 2, 4, 5 once reviewed.
