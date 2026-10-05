# Contributing to FlowMind

Thank you for your interest in contributing! This document outlines the process for reporting bugs, suggesting features, and submitting pull requests.

---

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Setup](#development-setup)
- [Branching Strategy](#branching-strategy)
- [Commit Convention](#commit-convention)
- [Pull Request Checklist](#pull-request-checklist)
- [Reporting Issues](#reporting-issues)

---

## Code of Conduct

All contributors are expected to be respectful and inclusive. Harassment of any kind will not be tolerated.

---

## Getting Started

1. **Fork** the repository to your GitHub account.
2. **Clone** your fork locally:
   ```bash
   git clone https://github.com/<your-username>/FlowMind.git
   cd FlowMind
   ```
3. Open the project in **Android Studio Hedgehog** or later.
4. Sync Gradle and make sure the project builds before making changes.

---

## Development Setup

| Requirement | Version |
|-------------|---------|
| Android Studio | Hedgehog+ |
| Kotlin | 1.9+ |
| Gradle | 8.x |
| Min SDK | 26 |
| Target SDK | 34 |

Copy `local.properties.example` to `local.properties` and fill in your keys:

```properties
CLOUDFLARE_ACCOUNT_ID=your_account_id
CLOUDFLARE_API_TOKEN=your_api_token
```

---

## Branching Strategy

| Branch | Purpose |
|--------|---------|
| `main` | Stable, production-ready code |
| `feature/<name>` | New features |
| `fix/<name>` | Bug fixes |
| `chore/<name>` | Maintenance, refactoring |

Always branch off `main` and target `main` in PRs.

---

## Commit Convention

We follow [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(scope): short description

[optional body]
```

**Types:** `feat`, `fix`, `chore`, `docs`, `refactor`, `test`, `perf`

**Examples:**
```
feat(domain): add Whisper Tiny model to ModelRegistry
fix(builder): prevent duplicate workflow names
docs: add CONTRIBUTING.md
```

---

## Pull Request Checklist

Before submitting a PR, confirm:

- [ ] The code compiles and all existing tests pass
- [ ] New features include unit tests where applicable
- [ ] KDoc comments are added to all public classes and functions
- [ ] No hardcoded credentials or secrets are included
- [ ] The PR description clearly explains what and why

---

## Reporting Issues

Use the [GitHub Issues](https://github.com/harshal-paltse/FlowMind/issues) tracker. Please include:

- A clear, descriptive title
- Steps to reproduce
- Expected vs. actual behaviour
- Device info (Android version, RAM)
- Relevant logs / screenshots

---

*Happy coding! 🚀*
