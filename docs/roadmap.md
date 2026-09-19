# lexic.on — Implementation Roadmap

> Living document. Check items off as they're built. Rationale for *why* things are designed this way lives in `docs/decisions.md` — this file is just the plan and current status.

## Working agreement

The user is implementing this project themselves, for learning — that's the point of the capstone. Claude's role is planning, explaining, unblocking, and reviewing on request, not writing the implementation autonomously. Help is asked for selectively (to get unstuck, or to speed up a part deliberately). Implementation proceeds in **vertical slices**: each feature slice touches every layer (Frontend, Controller, Service, Repo) *and* its own tests, and produces something end-to-end working, rather than building one architectural layer at a time across the whole app. Slice 0 and Slice 8 are deliberate exceptions — infrastructure "bookends" (walking skeleton, then CI/CD/quality-gate hardening) rather than vertical feature slices. Code reuse across slices (both reusing what an earlier slice already built, and noting when a later planned slice will need something a current slice is building) is a standing principle — see `docs/decisions.md` section 36.

## MVP scope

MVP = Slices 0–5 below. That's the point at which the core product value (read a text, see translations, track vocabulary status, browse it per language pair) works end-to-end for a real logged-in user. Slices 6–8 (Google login/account linking, admin page, CI/CD polish) are still required for the full capstone deliverable, not optional — they just come after the core product proves itself.

## Slices

- [ ] **Slice 0 — Walking skeleton**
  Spring Boot backend with one trivial endpoint, React+Vite frontend that calls it, Neon connection wired, one full deploy to Render. No real feature — the goal is retiring integration/deployment risk before building on top of it.

- [ ] **Slice 1 — Authentication (GitHub OAuth2)**
  `User` + `UserIdentity` tables, Spring Security OAuth2 login (GitHub first), protected endpoint pattern established. Done early (moved up from the original draft) specifically so `UserVocabulary` in Slice 4 can reference a real logged-in user from the start, instead of a throwaway hardcoded test-user id that would need to be ripped out later. This doesn't mean every endpoint requires login, though: the reading/translation pipeline (Slices 2-3) must stay reachable by unauthenticated guests (see decisions.md section 35) — only vocabulary tracking (Slice 4) actually needs a real user. Avatar is pulled from the OAuth2 provider where possible, with click-to-logout in the UI; the login entry point must also be reachable from the guest-facing Text page, not just a dedicated login screen.

- [ ] **Slice 2 — Core reading pipeline**
  Paste text → backend tokenizes/lemmatizes via LanguageTool → returns text + word spans + lemma. Frontend renders the text with plain clickable word spans (no status color yet — that needs Slice 4). Proves the core NLP pipeline in isolation, no translation/persistence of vocabulary yet. Must work for unauthenticated guests exactly like for logged-in users (no auth check here — see Slice 1 note). For logged-in users, only one current text is kept per user (a new paste overwrites it, with a discard warning) — no text-history feature for now (decisions.md section 35).

- [ ] **Slice 3 — Translations**
  `WordEntry` + `Translation` tables, Azure Translator integration, batch-translate-missing-words (dedup first, per `docs/decisions.md` section 4). Clicking a word shows its translation.

- [ ] **Slice 4 — Vocabulary status + stats**
  `UserVocabulary` table (tied to the real user from Slice 1; guests never get a row here — see Slice 1/2 notes). Statuses shown left-to-right as a progression: Unknown -> Review -> Known (red/yellow/green) — see `docs/decisions.md` section 35 for the naming/ordering rationale. A word gets no highlight and no `UserVocabulary` row until first clicked; clicking auto-creates the row as `Unknown`, with buttons in the same popup to change status or delete the row entirely (untrack the word again, via the same trash icon used for per-word rows in Slice 5's dictionary list view). % known/review/unknown stat bar for the text reflects only words already clicked at least once. Highlight colors should stay low-contrast/subtle so the underlying text stays easy to read.

- [ ] **Slice 5 — Dictionaries navigation**
  Sidebar (Text / Dictionaries), dynamic language-pair sub-items based on the user's actual vocabulary data, per-status word lists. Sidebar is collapsible (hamburger toggle). Each language pair gets its own nested sub-items for Unknown/Review/Known, not just the pair itself. Guests have no access to this section at all — show a prompt inviting them to register instead (decisions.md section 35). The specific-dictionary view needs a clear way back to the Dictionaries overview (decisions.md section 35 — exact UI pattern not finalized). Manually adding a whole new dictionary/language pair (an "add dictionary" action on the overview page, without having processed a text in that pair yet) is a candidate follow-up, not required for this slice — can ship later alongside manual word-adding.

- [ ] **Slice 6 — Google OAuth2 + account linking**
  Extends Slice 1: add Google as a second provider, link to the same `User` via email/verification logic.

- [ ] **Slice 7 — Roles + admin page**
  USER/ADMIN roles, backend-authorized admin page with app-level stats.

- [ ] **Slice 8 — CI/CD + quality gates**
  GitHub Actions pipeline, JaCoCo, SonarCloud (frontend/backend analyzed separately), deployment hardening. Tests themselves should be written alongside each slice as it's built, not deferred to this slice — this slice is about wiring the pipeline around tests that already exist.

## Pending input

- First round of Excalidraw wireframes (Text + Dictionaries pages) reviewed 2026-09-17 and incorporated into the slices above — full rationale in `docs/decisions.md` section 35.
- No wireframes yet for Slices 6-8 (Google login UI, admin page); not blocking, can be designed when those slices start.
