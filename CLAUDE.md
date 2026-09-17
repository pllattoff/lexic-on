# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project state

This repository is currently an empty scaffold — no source code has been added yet. It contains only IDE metadata (`.idea/`, `lexic-on.iml`) and a `.gitignore`.

The `.gitignore` indicates the intended project layout:

- `backend/` — a Java project (ignored `backend/target` implies Maven or Gradle). `backend/src/main/resources/static` is ignored, suggesting the backend (likely Spring Boot) will serve the built frontend as static resources.
- `frontend/` — a Node.js project (ignored `node_modules`, `dist`, `dist-ssr` implies a Vite-based build).

Neither directory exists yet, so there are no build, lint, or test commands to document. Once code is added, update this file with the actual commands (e.g., Maven/Gradle wrapper commands for `backend/`, npm/yarn/pnpm scripts for `frontend/`) and describe how the backend and frontend interact (API routes, dev proxy setup, build integration).
