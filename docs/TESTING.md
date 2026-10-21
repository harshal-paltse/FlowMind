# Testing Guide

This document describes the unit-test strategy for FlowMind, the file/package
layout, and the conventions every new test must follow.

---

## 1. Test Philosophy

FlowMind uses a **three-layer testing pyramid**:

| Layer | Location | Framework | Speed |
|---|---|---|---|
| **Unit** (pure JVM) | `src/test/` | JUnit 4 | ⚡ milliseconds |
| **Integration** | `src/androidTest/` | JUnit 4 + Espresso | 🐢 seconds |
| **UI / E2E** | `src/androidTest/` | Compose UI Test | 🐢 minutes |

The unit-test layer is the most important. It must cover all business-logic
classes in `domain/` and `data/` that have no Android framework dependencies.

---

## 2. What Is Covered

### `domain/analytics/RunAnalytics` — `RunAnalyticsTest`

| Function | Cases covered |
|---|---|
| `successRate` | empty, all-pass, all-fail, mixed (0.75) |
| `averageLatencyMs` | empty, uniform set |
| `medianLatencyMs` | empty, odd-size list, even-size list |
| `bestLatencyMs` | empty, extremes |
| `worstLatencyMs` | empty, extremes |
| `runCountByWorkflow` | grouping + descending sort, empty |
| `localInferenceRate` | empty, all-local, 50-50 split |
| `groupByDay` | same-day coalescing, multi-day split, empty |

### `domain/analytics/WorkflowSearchHelper` — `WorkflowSearchHelperTest`

| Function | Cases covered |
|---|---|
| `search` | blank query, empty list, tier-1/2/3/5 ranking, case-insensitivity |
| `searchEnabled` | disabled workflows excluded, all-disabled result |
| `matchingTemplates` | single match, deduplication, no match |

### `domain/validation/WorkflowValidator` — `WorkflowValidatorTest`

| Function | Cases covered |
|---|---|
| `validate` | happy path, MIN_NODES, MAX_NODES, unknown type, blank title, long title, duplicate title (case-insensitive), missing Trigger, missing Action |
| `validateName` | valid, blank, exceeds MAX_NAME_LENGTH |
| `validateAll` | happy path, multi-error collection, no short-circuit |

### `domain/privacy/PrivacyShield` — `PrivacyShieldTest`

SSN redaction, email redaction, safe-data pass-through.

---

## 3. Running the Tests

```bash
# All unit tests (pure JVM, no emulator required)
./gradlew test

# A single module
./gradlew :app:testDebugUnitTest

# With verbose output
./gradlew :app:testDebugUnitTest --info
```

HTML reports are generated at:
```
app/build/reports/tests/testDebugUnitTest/index.html
```

---

## 4. Writing New Tests

### 4.1 Location

Place tests under:
```
app/src/test/java/com/example/flowmind/<layer>/<package>/
```

Mirror the production source tree exactly so IntelliJ IDEA can navigate between
source and test with a single keyboard shortcut (`Ctrl+Shift+T`).

### 4.2 Naming Convention

| Item | Convention |
|---|---|
| File | `<ClassName>Test.kt` |
| Class | `<ClassName>Test` |
| Function | Backtick string describing the scenario: `` `successRate returns 0 for empty list` `` |

### 4.3 Builder Helpers

Every test class should expose a private `record(...)` / `node(...)` / `workflow(...)`
builder that sets **all** fields to sensible defaults and lets individual tests
override only the fields relevant to the scenario under test.

```kotlin
private fun record(
    success: Boolean = true,
    latencyMs: Long  = 1_000L,
    // ... other fields with defaults
) = RunRecord(...)
```

This pattern eliminates boilerplate, makes tests readable at a glance, and
shields the suite from constructor changes in the domain model.

### 4.4 Assertion Style

- Prefer **`assertEquals(expected, actual)`** over `assertTrue(actual == expected)`.
  JUnit 4 prints a diff on failure when using `assertEquals`.
- Include a **failure message** when using `assertTrue` with a complex condition:
  ```kotlin
  assertTrue("Expected bill to rank first but got: $result", result.first().name == "Bill OCR")
  ```
- Use a **delta** for float comparisons: `assertEquals(0.75f, rate, 0.001f)`.

### 4.5 What NOT to Test in Unit Tests

- Room DAOs → use `androidTest` + an in-memory database.
- ViewModels with `viewModelScope` → use `kotlinx-coroutines-test` + `TestCoroutineDispatcher`.
- Composable UI → use Compose UI Test.

---

## 5. CI Integration

The CI pipeline (`.github/workflows/`) runs `./gradlew test` on every push and
pull-request. A red unit-test suite **blocks merge** — no exceptions.

The test report is uploaded as a workflow artifact so reviewers can inspect
failures without re-running locally.
