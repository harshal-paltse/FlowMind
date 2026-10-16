# FlowMind — Architecture Overview

> **Version:** 1.1 (October 2026)  
> **Author:** Harshal Paltse  
> **Audience:** Contributors, code reviewers, and future maintainers

---

## Table of Contents

1. [High-Level Design](#1-high-level-design)
2. [Module Structure](#2-module-structure)
3. [Layer Responsibilities](#3-layer-responsibilities)
4. [Key Data Flows](#4-key-data-flows)
5. [Dependency Injection](#5-dependency-injection)
6. [Persistence](#6-persistence)
7. [Background Work](#7-background-work)
8. [Privacy & Security](#8-privacy--security)
9. [AI / ML Pipeline](#9-ai--ml-pipeline)
10. [Design Decisions & Trade-offs](#10-design-decisions--trade-offs)

---

## 1. High-Level Design

FlowMind is a single-module Android application built on **Clean Architecture** with a strict
unidirectional data flow:

```
UI (Compose screens)
    ↕  StateFlow / SharedFlow
ViewModel  (ui/screens/)
    ↕  suspend functions / Flow
Repository  (data/)
    ↕  Room DAOs / Network clients
Data Sources  (Room DB · Firebase · Cloudflare Workers · WorkManager)
```

The domain layer (`domain/`) is **framework-free** — it holds models, validation rules, analytics
helpers, and manager classes that do not import `android.*` unless absolutely necessary for
device-sensor reads.

---

## 2. Module Structure

```
app/
└── src/main/java/com/example/flowmind/
    ├── auth/           Firebase Auth wrappers and AuthRepository
    ├── data/           Repositories + Room DAOs + WorkflowSeeder
    │   └── db/         AppDatabase, WorkflowDao, RunRecordDao, ModelDao
    ├── di/             Hilt modules (@Provides / @Binds)
    ├── domain/
    │   ├── analytics/  Pure analytics helpers (RunAnalytics)
    │   ├── audio/      VoiceToWorkflow NLP mapper
    │   ├── manager/    DeviceFitScorer, ModelDownloader
    │   ├── models/     Room entities + extension functions
    │   ├── network/    CloudflareApiClient, FirebaseFunctionsManager
    │   ├── pdf/        PdfGenerator
    │   ├── privacy/    PrivacyShield (PII redaction)
    │   ├── simulator/  DryRunSimulator
    │   └── validation/ WorkflowValidator
    ├── fcm/            FCM service + notification helpers
    ├── ml/             ML Kit OCR wrappers
    ├── ui/
    │   ├── components/ Reusable Compose composables (CustomCharts, etc.)
    │   ├── navigation/ NavGraph
    │   ├── screens/    One ViewModel + Screen file per feature
    │   ├── theme/      Material3 theme tokens
    │   ├── tile/       Quick Settings tile (TileService)
    │   └── widget/     Home-screen app widget (GlanceAppWidget)
    └── worker/         WorkManager workers (ScheduledWorkflowWorker, etc.)
```

---

## 3. Layer Responsibilities

### UI Layer — `ui/`
- **Compose screens** are stateless; they read `StateFlow` from the ViewModel and emit events.
- **ViewModels** own all UI state. They call repositories on `viewModelScope` (no coroutine leaks).
- **No business logic** lives in screens or composables.

### Domain Layer — `domain/`
- **Models** are plain Kotlin data classes (with Room annotations where needed).
- **Extension files** (`WorkflowExtensions.kt`, `RunRecordExtensions.kt`) add derived properties
  without polluting entity classes with logic that Room might misinterpret as columns.
- **Validators** (`WorkflowValidator`) return a `sealed class ValidationResult` — never throw.
- **Analytics** (`RunAnalytics`) are pure functions over in-memory lists — trivially unit-testable.

### Data Layer — `data/`
- One **Repository** per aggregate root (`WorkflowRepository`, `RunRecordRepository`).
- Repositories expose `Flow<List<T>>` for reactive UI updates and `suspend` functions for writes.
- DAOs are injected through Hilt; callers never reference DAOs directly.

---

## 4. Key Data Flows

### Workflow Execution
```
User taps Run
  → RunViewModel.runWorkflow()
  → PrivacyShield.sanitise(input)       ← strips PII before cloud calls
  → CloudflareApiClient.infer()          ← POST /v1/infer (with sanitised payload)
  → RunRecord persisted via RunRecordRepository.insert()
  → ResultScreen collects RunViewModel.state (StateFlow)
```

### Background Schedule
```
ScheduleViewModel.scheduleRepeating()
  → WorkManager.enqueue(PeriodicWorkRequest)
  → ScheduledWorkflowWorker.doWork()
    → loads Workflow from WorkflowRepository.findById()
    → executes steps (same pipeline as foreground run)
    → posts FCM-style notification on completion
```

### Model Download
```
ModelCandidatesScreen selects a model
  → ModelDownloader.download(url, destFile)
    → retries with exponential back-off (up to maxRetries)
    → verifies SHA-256 post-download
    → updates ModelCandidate.isDownloaded via ModelDao
  → DeviceFitScorer.scoreModels() re-evaluated on next visit
```

---

## 5. Dependency Injection

Hilt is used throughout. Key binding points:

| Module | Provides |
|--------|----------|
| `DatabaseModule` | `AppDatabase`, all DAOs |
| `RepositoryModule` | `WorkflowRepository`, `RunRecordRepository` |
| `NetworkModule` | `CloudflareApiClient`, `FirebaseFunctionsManager` |
| `SecurityModule` | `SecurityManager` (singleton) |

ViewModels are injected with `@HiltViewModel` and `@Inject constructor`.

---

## 6. Persistence

| Store | Technology | Scope |
|-------|-----------|-------|
| Workflows | Room (`workflows` table) | App lifetime |
| Run records | Room (`run_records` table) | App lifetime |
| ML models catalogue | Room (`models` table) | App lifetime |
| Auth token | `EncryptedSharedPreferences` | App lifetime |
| Database encryption passphrase | Android Keystore | Device lifetime |

Room's `@TypeConverter` converts `TriggerType` enum ↔ String.  
Database migrations use `Room.databaseBuilder(...).fallbackToDestructiveMigration()` during early
development; production releases will supply explicit `Migration` objects.

---

## 7. Background Work

All deferrable background tasks run through **WorkManager**:

- `ScheduledWorkflowWorker` — triggered by time-based schedules.
- `ModelDownloadWorker` (planned) — downloads large model files with network constraints.

FCM push messages use `FlowMindFcmService` to receive trigger payloads and enqueue an immediate
`OneTimeWorkRequest`.

---

## 8. Privacy & Security

- **`PrivacyShield`** runs before every cloud API call. It strips names, emails, phone numbers,
  credit-card numbers, and IP addresses using compiled `Regex` patterns.
- **`SecurityManager`** wraps `EncryptedSharedPreferences` (AES-256-GCM) for token storage and
  provides `rotateDatabasePassphrase()` for on-demand key rotation.
- Cloud calls are rejected entirely when `PrivacyShield.isSafeForCloud()` returns `false`.

---

## 9. AI / ML Pipeline

```
Input (text / image / audio)
  → PrivacyShield.sanitise()
  → [On-device path]  ML Kit OCR / TFLite model via DeviceFitScorer-selected candidate
  → [Cloud path]      CloudflareApiClient → Cloudflare AI Worker → LLM / Vision model
  → Result JSON parsed and displayed on ResultScreen
```

Model selection follows this priority:
1. User's explicit choice on ModelCandidatesScreen.
2. Highest `DeviceFitScore` (storage + RAM + battery + charging + connectivity checks).
3. Fall back to cloud if all on-device models are rejected.

---

## 10. Design Decisions & Trade-offs

| Decision | Rationale |
|----------|-----------|
| Single-module app | Simpler build graph for a hackathon-scale project; modularise later. |
| Cloudflare Workers over direct LLM SDK | Edge inference, no server to maintain, pay-per-request. |
| Room over DataStore for workflows | Relational queries (by name, by date) are easier with SQL. |
| `sealed class ValidationResult` instead of exceptions | Errors are part of the expected domain flow, not exceptional conditions. |
| Extension files for model helpers | Keeps Room entities clean; prevents accidental column mapping. |
| Pure `RunAnalytics` object | Zero Android dependencies → fully unit-testable with plain JUnit. |

---

*For contribution guidelines see [CONTRIBUTING.md](./CONTRIBUTING.md).*  
*For the release history see [CHANGELOG.md](./CHANGELOG.md).*
