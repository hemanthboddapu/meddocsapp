# Contributing Guide

Thanks for considering contributing to MedDocs! This guide explains our conventions and workflow.

## Branching Strategy
- `main` is protected. Create feature branches: `feature/<short-name>` and bugfix branches: `fix/<short-name>`.
- Keep branches focused and small.

## Commit Messages
Use Conventional Commits:
- `feat:` add a new user-facing feature
- `fix:` bug fixes
- `docs:` documentation changes
- `refactor:` code changes that don’t fix a bug or add a feature
- `test:` add or adjust tests
Examples: `feat(patient): add discharge action`, `fix(repository): handle empty query`

## Code Style
- Kotlin style; consider ktlint or Android Studio inspections.
- MVVM: UI logic in Activities/Fragments; business/data logic in ViewModel/Repository.
- Room: Use `suspend` for writes; return `Flow<...>` for reactive reads.
- Coroutines: Prefer `Dispatchers.IO` for DB and file operations.

## PR Process
- Open a PR against `main`.
- Include a clear description and screenshots for UI changes.
- Ensure the build is green and tests pass.
- Request review from a project maintainer.

## Testing
- Unit tests live under `app/src/test` (see `PatientViewModelTest.kt`).
- Add tests for new repository and view model behaviors.
- For database changes, also add instrumented tests under `app/src/androidTest`.

## Issue Reporting
Please include:
- Steps to reproduce
- Expected vs actual behavior
- Logs or screenshots if applicable

## Release & Versioning
- Tags on `main` mark releases.
- Follow semantic versioning where applicable.

