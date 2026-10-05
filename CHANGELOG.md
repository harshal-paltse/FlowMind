# Changelog

All notable changes to **FlowMind** are documented here.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/)
and this project adheres to [Semantic Versioning](https://semver.org/).

---

## [Unreleased]

### Added
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

### Changed
- Standardised KDoc across all domain models and ViewModels

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
