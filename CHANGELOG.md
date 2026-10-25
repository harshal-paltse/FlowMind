# Changelog

All notable changes to **FlowMind** are documented here.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/)
and this project adheres to [Semantic Versioning](https://semver.org/).

---

## [Unreleased]

### Added
- `RunAnalytics.p95LatencyMs()` — 95th-percentile latency via the nearest-rank method; primary SLO indicator for dashboard alerting
- `RunAnalytics.failureRate()` — complement of `successRate()` guaranteed to sum to 1.0, enabling dual-metric pie charts
- `RunAnalytics.StreakInfo` data class — bundles `successStreak`, `failureStreak`, and `longestSuccessStreak` into a single coherent value
- `RunAnalytics.streakInfo()` — single-pass forward/backward scan that populates `StreakInfo`; O(n) with no allocations beyond the result object
- `AnalyticsSummary` data class — top-level immutable value object bundling all commonly needed metrics (total, rates, latency percentiles, local rate, streak)
- `List<RunRecord>.toAnalyticsSummary()` — convenience extension that delegates to all `RunAnalytics` helpers and returns an `AnalyticsSummary` in one call; intended for `stateFlow.map { }` in ViewModels
- `WorkflowValidator.MAX_DESCRIPTION_LENGTH` constant (250 chars) — enforces a DB-safe cap on the optional description field
- `WorkflowValidator.validateDescription()` — blank descriptions are allowed (field is optional); only non-blank strings exceeding `MAX_DESCRIPTION_LENGTH` produce an error
- `WorkflowValidator.ValidationSummary` data class — carries `isValid`, `errors`, and `warnings` so the builder UI can render advisory hints alongside hard errors without a separate call
- `WorkflowValidator.validateSummary()` — combines name, description, and node validation; additionally emits advisory warnings when no AI/Transform node is present or the node count exceeds 20
- `WorkflowSearchHelper` — pure, stateless object providing ranked fuzzy search across workflow names, template IDs, and node JSON; tiers: exact name (100) → starts-with (80) → contains (60) → template ID (40) → node body (20)
- `NotificationHelper` — centralised notification dispatcher with three typed channels (`flowmind_runs`, `flowmind_scheduler`, `flowmind_alerts`); exposes `notifyRunComplete`, `notifyScheduledTrigger`, `notifyAlert`, and `cancelAll`; `POST_NOTIFICATIONS` `SecurityException` is silently swallowed for Android 13+ compat
- `WorkflowExtensions` — `nodeTypes()`, `hasTrigger()`, `hasAction()`, `summarise()` new helpers; `nodeTypes()` extracts the type prefix (Trigger/AI/Transform/Action) of every node; `summarise()` returns a compact one-liner e.g. `"3 nodes • Trigger → AI → Action • Active"`
- `DryRunSimulator` — node-type-aware `simulatedOutputFor()` produces realistic mock output per node category (OCR %, STT transcript, LLM token count, etc.); `simulationSummary()` top-level extension formats a `SimulationState.Success` into a display string; default pipeline updated to reflect real node types
- `ModelDownloader` — `DownloadState.Paused` sealed subtype; thread-safe `pause()`, `resume()`, `cancel()` controls backed by `AtomicBoolean`; download loop polls pause flag every 500 ms and emits `Paused` states; `_isCancelled` check added between retry attempts
- `PdfGenerator.generateRunSummaryReport()` — new overload that renders a list of `RunRecord`s as a paginated A4 table (columns: #, Workflow, Status, Latency, Inference) with alternating row shading, colour-coded status cells, and a summary footer (total runs, success count, avg latency)
- `RunRecordRepository` — `clearAll()` wipes all records; `countByTemplate(templateId)` returns per-template run count; `mostRecentRun()` fetches the latest record; `totalRunCount()` fast COUNT without loading all rows
- `RunRecordExtensions.kt` — `statusLabel()`, `latencyFormatted()`, `runAtFormatted()`, `inferenceLabel()`, `isFast()`, `shareSummary()` extension functions on `RunRecord`
- `RunAnalytics` singleton — pure, framework-free analytics helpers: `successRate`, `averageLatencyMs`, `medianLatencyMs`, `bestLatencyMs`, `worstLatencyMs`, `runCountByWorkflow`, `localInferenceRate`, `groupByDay`
- `WorkflowDao.findById()`, `findByName()`, `updateEnabled()`, `countWorkflows()` targeted Room queries
- `WorkflowRepository.findById()`, `findByName()`, `setEnabled()`, `count()` — exposing new DAO queries
- `DeviceFitScorer.isCharging()` — battery/cellular penalties now waived or halved when device is plugged in
- `docs/ARCHITECTURE.md` — comprehensive architecture reference covering layers, data flows, DI, persistence, background work, privacy/security, and AI/ML pipeline
- `WorkflowValidator.MIN_NODES`, `MAX_NAME_LENGTH` constants
- `WorkflowValidator.validateName()` — standalone name validator for the builder UI
- Duplicate step-title guard in `WorkflowValidator.validate()`
- Action-node presence check in `WorkflowValidator.validate()` — prevents trigger-only workflows
- Blank-title guard in `WorkflowValidator.validate()`
- Full KDoc added to `DeviceFitScore` data class and `DeviceFitScorer` class

- `updatedAt` and `isEnabled` fields to `Workflow` entity for lifecycle tracking
- `errorMessage` and `userNote` fields to `RunRecord` for richer failure diagnostics
- **Whisper Tiny** speech-to-text model to `ModelRegistry`
- Helper functions `findById`, `filterByAccelerator`, `filterByRam` to `ModelRegistry`
- Cancellation support and `totalNodes` progress tracking in `DryRunSimulator`
- Node JSON parsing with graceful fallback in `DryRunSimulator`
- `totalRuns` and `bestLatencyMs` fields to `InsightsData`
- `exportCsv()` function with `SharedFlow` output in `ReportsViewModel`
- Duplicate workflow name guard in `BuilderViewModel.save()`
- `update()` method in `BuilderViewModel` for editing existing workflows
- FCM push token diagnostic check in `DiagnosticsViewModel`
- `retry()` and `reset()` in `DiagnosticsViewModel` for re-running checks
- `compositeScore`, `isCpuCompatible`, `summary` computed properties on `ModelCandidate`
- `CONTRIBUTING.md` with setup guide, branching strategy and PR checklist
- **`DonutChart`** animated composable with sweep-in animation and legend to `CustomCharts`
- Gradient vertical fill on `SimpleBarChart` bars for polished UI
- `chartHeight` parameter on `SimpleBarChart` for flexible sizing
- Multi-page support in `PdfGenerator` — long reports automatically overflow to new A4 pages
- Branded title styling (deep indigo) and page-number footer in `PdfGenerator`
- `PAGE_WIDTH`, `PAGE_HEIGHT`, `MARGIN_*` constants extracted to `PdfGenerator.Companion`
- Real SHA-256 file integrity verification replacing simulation stub in `ModelDownloader`
- Retry with exponential back-off (up to `maxRetries`) in `ModelDownloader`
- `maxRetries` constructor parameter on `ModelDownloader`
- `rotateDatabasePassphrase()` for on-demand key rotation in `SecurityManager`
- `storeToken()`, `retrieveToken()`, `clearToken()` helpers in `SecurityManager`
- `clearAllSecrets()` secure wipe for sign-out / account-deletion flows in `SecurityManager`
- Lazy initialisation of `MasterKey` and `EncryptedSharedPreferences` in `SecurityManager`
- Full KDoc, `sendEmailSummary`, `sendWorkflowExport`, and `ping` to `FirebaseFunctionsManager`
- Full KDoc to `ScheduleViewModel` (`ScheduleUiEvent`, `scheduleOnce`, `scheduleRepeating`, `cancelSchedule`, `sendTestNotification`)
- Full KDoc to `RunViewModel` (class-level, `runWorkflow`, `reset`, `state`)
- `ReportsViewModel.csvExport` now documented with `@property` KDoc
- Per-check configurable timeout (`checkTimeoutMs = 5 s`) in `DiagnosticsViewModel` using `withTimeoutOrNull`
- `lastRunTimestamp: Long?` field on `DiagState` recording epoch-ms of last completed run
- `clearChecks()` in `DiagnosticsViewModel` — resets state to PENDING without incrementing retry counter
- Full KDoc to `AuthViewModel`, `SignInState` sealed class and all its variants
- `displayName: String?` accessor on `AuthViewModel` forwarding to repository
- `signOut()` in `AuthViewModel` — delegates to repository and resets state to `SignInState.Idle`
- `signInState` now backed by `asStateFlow()` for safe external exposure

### Changed
- **`DashboardScreen`** — "My Automations" list now features a live search bar backed by `WorkflowSearchHelper`; typing filters by name/node content, a clear (✕) button resets the query, and an empty-results state with a "Clear search" action is shown when no workflows match
- Standardised KDoc across all domain models, ViewModels, and utility classes
- `SecurityManager.encryptedPrefs` converted from `val` initialised in-body to lazy delegate

---

## [1.0.0] — 2026-10-01

### Added
- Initial release of FlowMind
- Workflow Builder with drag-and-drop node canvas
- Dashboard screen with live stats
- Reports screen with run history and analytics
- Live Input screen for real-time audio/camera capture
- Diagnostics screen for health checks
- Schedule screen for time-based workflow triggers
- Workflow Lab (dry-run simulator)
- Firebase Auth integration
- Cloudflare AI Workers backend (`/v1/infer`)
- Room persistence for workflows, run records and ML model catalogue
- FCM push notifications for trigger delivery
- Quick Settings tile for one-tap workflow execution
- Home-screen widget showing last run status
- On-device OCR via ML Kit
- Privacy Shield for data anonymisation before cloud calls
- WorkManager-based background scheduler

---

[Unreleased]: https://github.com/harshal-paltse/FlowMind/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/harshal-paltse/FlowMind/releases/tag/v1.0.0
