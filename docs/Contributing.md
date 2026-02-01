# Contributing Guide

Thank you for considering contributing to MedDocs! This guide explains our conventions, workflow, and best practices.

---

## Table of Contents
- [Getting Started](#getting-started)
- [Development Setup](#development-setup)
- [Branching Strategy](#branching-strategy)
- [Commit Messages](#commit-messages)
- [Code Style](#code-style)
- [Architecture Guidelines](#architecture-guidelines)
- [Pull Request Process](#pull-request-process)
- [Testing](#testing)
- [Documentation](#documentation)
- [Issue Reporting](#issue-reporting)

---

## Getting Started

1. Fork the repository
2. Clone your fork locally
3. Set up the development environment (see below)
4. Create a feature branch
5. Make your changes
6. Submit a pull request

---

## Development Setup

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17 or 21
- Android SDK with API level 28+
- Git

### Setup Steps
```powershell
# Clone your fork
git clone https://github.com/YOUR_USERNAME/meddocsapp.git
cd meddocsapp

# Open in Android Studio
# File → Open → Select project folder

# Sync Gradle
# File → Sync Project with Gradle Files

# Build
.\gradlew.bat assembleDebug
```

### Verify Setup
```powershell
# Run tests
.\gradlew.bat test

# Check for lint issues
.\gradlew.bat lint
```

---

## Branching Strategy

### Branch Naming
- `main` - Protected, production-ready code
- `develop` - Integration branch for features
- `feature/<short-name>` - New features (e.g., `feature/export-pdf`)
- `fix/<short-name>` - Bug fixes (e.g., `fix/crash-on-share`)
- `docs/<short-name>` - Documentation updates
- `refactor/<short-name>` - Code refactoring

### Workflow
1. Create branch from `main` or `develop`
2. Make focused, small commits
3. Keep branch up to date with base
4. Submit PR when ready

---

## Commit Messages

Use [Conventional Commits](https://www.conventionalcommits.org/) format:

```
<type>(<scope>): <description>

[optional body]

[optional footer]
```

### Types
| Type | Description |
|------|-------------|
| `feat` | New feature |
| `fix` | Bug fix |
| `docs` | Documentation only |
| `style` | Formatting, no code change |
| `refactor` | Code change without feature/fix |
| `test` | Adding/updating tests |
| `chore` | Build, config, dependencies |

### Examples
```
feat(patient): add quick discharge action
fix(repository): handle null recycle bin dao
docs(readme): update installation steps
refactor(viewmodel): extract file operations
test(dao): add search query tests
chore(deps): update Room to 2.6.1
```

### Scope (optional)
Use component names: `patient`, `file`, `dashboard`, `compare`, `share`, `recycle`, `logger`, `avatar`

---

## Code Style

### Kotlin Conventions
- Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use meaningful variable/function names
- Prefer `val` over `var` when possible
- Use data classes for models
- Use sealed classes for state

### Formatting
- 4-space indentation
- Max line length: 120 characters
- Use Android Studio's auto-format (Ctrl+Alt+L)

### Naming
```kotlin
// Classes: PascalCase
class PatientRepository

// Functions/variables: camelCase
fun insertPatient(patient: Patient)
val patientList: List<Patient>

// Constants: SCREAMING_SNAKE_CASE
const val DEFAULT_RETENTION_DAYS = 7

// XML resources: snake_case
activity_patient_detail.xml
ic_add_patient.xml
```

### Documentation
- Add KDoc to public classes and functions
- Explain non-obvious logic with comments
- Use `// TODO:` for pending work
- Use `// FIXME:` for known issues

```kotlin
/**
 * Repository for patient data operations.
 *
 * Coordinates between DAOs and provides business logic
 * for patient management including recycle bin support.
 *
 * @param patientDao DAO for patient table operations
 * @param patientFileDao DAO for file operations
 * @param recycleBinDao DAO for recycle bin (optional)
 */
class PatientRepository(...)
```

---

## Architecture Guidelines

### MVVM Pattern
Follow the established architecture:

```
UI (Activity/Fragment)
    ↓ observes LiveData
ViewModel
    ↓ calls suspend functions
Repository
    ↓ executes queries
DAO (Room)
```

### Rules
1. **UI Layer**: No business logic, only observe and render
2. **ViewModel**: Expose UI state, launch coroutines in `viewModelScope`
3. **Repository**: Single source of truth, coordinate DAOs
4. **DAO**: Pure database operations, return Flow for reads

### Adding New Features

1. **Entity**: Create `@Entity` data class if new table needed
2. **DAO**: Add queries to existing DAO or create new one
3. **Migration**: Add database migration if schema changes
4. **Repository**: Add business methods
5. **ViewModel**: Expose to UI via LiveData
6. **UI**: Create layouts and wire up
7. **Tests**: Add unit tests for repository/viewmodel
8. **Logging**: Use `AppLogger` for debug output

### Dependencies
- Inject dependencies via constructor
- Use `ViewModelProvider.Factory` for ViewModels
- Access repository via `MedDocsApplication`

---

## Pull Request Process

### Before Submitting
- [ ] Code compiles without errors
- [ ] All tests pass (`.\gradlew.bat test`)
- [ ] No new lint warnings
- [ ] Self-reviewed the diff
- [ ] Updated documentation if needed
- [ ] Commit messages follow conventions

### PR Description Template
```markdown
## Description
Brief description of changes

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation update

## Testing
Describe how you tested the changes

## Screenshots (if UI changes)
Before/After screenshots

## Checklist
- [ ] Code follows style guidelines
- [ ] Self-reviewed
- [ ] Tests added/updated
- [ ] Documentation updated
```

### Review Process
1. Submit PR against `main` or `develop`
2. Automated checks run (build, tests)
3. Maintainer reviews code
4. Address feedback with new commits
5. Squash and merge when approved

---

## Testing

### Unit Tests
Located in `app/src/test/java/com/example/meddocsapp/`

```powershell
# Run all unit tests
.\gradlew.bat test

# Run specific test class
.\gradlew.bat test --tests "PatientRepositoryTest"
```

### Writing Tests
- Use fake DAOs (see `PatientRepositoryTest.kt`)
- Test repository business logic
- Test ViewModel state changes
- Use descriptive test names

```kotlin
@Test
fun `discharge patient updates status and date`() = runBlocking {
    // Arrange
    val patient = Patient(name = "Test", bedNumber = "1", status = "Active")
    
    // Act
    repository.dischargePatient(patient, dischargeDate)
    
    // Assert
    val updated = repository.allPatients.first().first()
    assertEquals("Discharged", updated.status)
}
```

### Instrumented Tests
Located in `app/src/androidTest/java/`

```powershell
# Run on connected device
.\gradlew.bat connectedAndroidTest
```

---

## Documentation

### What to Document
- New features in README.md
- API changes in docs/API-Usage.md
- Architecture changes in docs/Architecture.md
- All changes in CHANGELOG.md

### Documentation Style
- Use Markdown formatting
- Include code examples
- Keep language clear and concise
- Update table of contents if adding sections

---

## Issue Reporting

### Bug Reports
Include:
- Steps to reproduce
- Expected vs actual behavior
- Device/Android version
- Screenshots/screen recordings
- Logs (enable Developer Mode)

### Feature Requests
Include:
- Clear description of the feature
- Use case / problem it solves
- Proposed implementation (optional)
- Mockups if UI-related

### Labels
- `bug` - Something isn't working
- `enhancement` - New feature request
- `documentation` - Documentation improvements
- `good first issue` - Good for newcomers
- `help wanted` - Extra attention needed

---

## Code of Conduct

- Be respectful and inclusive
- Provide constructive feedback
- Help others learn and grow
- Focus on the code, not the person

---

## Questions?

Open an issue with the `question` label or reach out to maintainers.

Thank you for contributing! 🎉
