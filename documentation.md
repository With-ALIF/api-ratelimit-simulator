# Smart API Rate-Limit & Abuse Simulator
## Comprehensive System Architecture, Design Specification & Technical Documentation

---

### Document Information
- **Project Name:** Smart API Rate-Limit & Abuse Simulator
- **Organization / Department:** Department of Computer Science and Engineering (CSE), Begum Rokeya University, Rangpur (BRUR)
- **Academic Course:** Object-Oriented Programming (OOP)
- **Development Team:** Runtime Crew (Group - 02)
- **Software Version:** 1.0.0
- **Document Status:** Official Production Documentation
- **Target Runtime:** Java SE 17+ (Java 21 Recommended) & JavaFX 21.0.2

---

## Executive Summary

In contemporary distributed architectures, cloud platforms, and fintech environments, public and internal Application Programming Interfaces (APIs) handle high-frequency concurrent traffic. Uncontrolled traffic poses severe threats: resource exhaustion, Denial-of-Service (DoS) conditions, automated credential stuffing, bot scraping, and degraded Quality of Service (QoS) for legitimate tenants.

The **Smart API Rate-Limit & Abuse Simulator** is an enterprise-grade desktop simulation application built with **Java 17+** and **JavaFX 21**. It replicates the internal algorithmic, stateful, and policy-driven mechanisms used by production API Gateways (such as Kong, AWS API Gateway, NGINX, and Cloudflare) to meter client access, enforce bandwidth and rate policies, detect anomalous traffic signatures, and generate granular security audit reports—**entirely in memory with zero network latency, socket overhead, or external infrastructure dependencies**.

---

## Table of Contents
1. [System Architecture & Design Principles](#1-system-architecture--design-principles)
2. [Domain Model Specification](#2-domain-model-specification)
3. [Algorithmic Rate Limiting & Detection Engine](#3-algorithmic-rate-limiting--detection-engine)
4. [Service Layer & Engine Implementation](#4-service-layer--engine-implementation)
5. [User Interface Architecture (JavaFX)](#5-user-interface-architecture-javafx)
6. [Data Ingestion & Traffic Simulation](#6-data-ingestion--traffic-simulation)
7. [Reporting, Analytics & Diagnostic Export](#7-reporting-analytics--diagnostic-export)
8. [Setup, Build & Execution Guide](#8-setup-build--execution-guide)
9. [Verification & Automated Test Suite](#9-verification--automated-test-suite)
10. [Team Contributions & Academic Context](#10-team-contributions--academic-context)
11. [Request Lifecycle & Admission Control: When Operations are Allowed or Blocked](#11-request-lifecycle--admission-control-when-operations-are-allowed-or-blocked)

---

## 1. System Architecture & Design Principles

The application is structured according to **Clean Architecture** and **Layered Architectural Patterns**, strictly decoupling presentation, application logic, detection strategies, and in-memory persistence.

### 1.1 Architectural Layers

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│                             PRESENTATION LAYER (JavaFX)                          │
│   • MainApp                      • EnhancedDashboardView    • TopBarView         │
│   • ControlPanelView             • ActivityTableView        • StatsPanel         │
│   • LogPanel                     • ReportViewDialog         • ReportDialogHelper │
└────────────────────────────────────────┬─────────────────────────────────────────┘
                                         │ User Events / Ingestion Commands
                                         ▼
┌──────────────────────────────────────────────────────────────────────────────────┐
│                            APPLICATION SERVICE LAYER                             │
│   • DashboardActionHandler       • RateLimitEnforcer        • RateLimitAnalyzer  │
│   • ClientActivityTracker        • RequestLogger            • DatasetLoader      │
│   • BurstTrafficGenerator        • EnhancedReportGenerator  • ReportGenerator    │
└────────────────────────────────────────┬─────────────────────────────────────────┘
                                         │ Policy Evaluation
                                         ▼
┌──────────────────────────────────────────────────────────────────────────────────┐
│                          POLICY STRATEGY ENGINE LAYER                            │
│   «interface» RatePolicy                                                         │
│   ├── FixedWindowPolicy                    ├── SlidingWindowPolicy               │
│   ├── BurstDetectionPolicy                 ├── AbnormalPatternPolicy             │
│   └── RetryAbusePolicy                                                           │
└────────────────────────────────────────┬─────────────────────────────────────────┘
                                         │ Operates Upon
                                         ▼
┌──────────────────────────────────────────────────────────────────────────────────┐
│                               DOMAIN MODEL LAYER                                 │
│   • Client                       • ServiceRequest           • RequestLog         │
│   • AbuseReport                  • RequestType (Enum)       • ViolationLevel (Enum)│
└──────────────────────────────────────────────────────────────────────────────────┘
```

### 1.2 Object-Oriented Principles & Design Patterns

1. **Strategy Pattern (`RatePolicy`):**
   - Encapsulates rate-limiting algorithms within an interchangeable policy family (`FixedWindowPolicy`, `SlidingWindowPolicy`, `BurstDetectionPolicy`, `AbnormalPatternPolicy`, `RetryAbusePolicy`).
   - The analysis engine (`RateLimitAnalyzer`) iterates across polymorphic policies without coupling to specific concrete implementations.

2. **Single Responsibility Principle (SRP):**
   - Ingestion (`DatasetLoader`, `BurstTrafficGenerator`), filtering (`RateLimitEnforcer`), analytics (`RateLimitAnalyzer`), logging (`RequestLogger`), metrics tracking (`ClientActivityTracker`), and presentation (`EnhancedDashboardView`) reside in distinct, single-purpose classes.

3. **Immutability and Defensive Encapsulation:**
   - Domain objects (`Client`, `ServiceRequest`) are immutable once instantiated.
   - State containers (`RequestLog`, `ClientActivityTracker`, `AbuseReport`) return unmodifiable views via `Collections.unmodifiableList()` and `Collections.unmodifiableMap()`, preventing external mutations from corrupting internal state.

4. **Monotonic Severity Escalation:**
   - `AbuseReport` enforces non-downgradable violation escalation (`NORMAL` $\to$ `WARNING` $\to$ `CRITICAL`). A critical finding cannot be accidentally overwritten by a subsequent lenient policy check.

5. **Reactive UI Updates:**
   - High-performance decoupled notification using JavaFX event dispatching and a 1-second background `Timeline` polling loop for real-time quota replenishment and risk indicators.

---

## 2. Domain Model Specification

The domain layer models the entities participating in the simulated API ecosystem.

### 2.1 Class Diagram (Domain Model)

```
┌─────────────────────────┐             ┌─────────────────────────┐
│         Client          │             │     ServiceRequest      │
├─────────────────────────┤             ├─────────────────────────┤
│ - clientId: String      │             │ - clientId: String      │
│ - name: String          │             │ - requestType: RequestType
├─────────────────────────┤             │ - timestamp: LocalDateTime
│ + getClientId(): String │             ├─────────────────────────┤
│ + getName(): String     │             │ + getFormattedTime(): Str│
│ + equals(Object): bool  │             │ + toString(): String    │
│ + hashCode(): int       │             └────────────┬────────────┘
└─────────────────────────┘                          │ 1..*
                                                     ▼
┌─────────────────────────┐             ┌─────────────────────────┐
│       AbuseReport       │             │       RequestLog        │
├─────────────────────────┤             ├─────────────────────────┤
│ - clientId: String      │             │ - clientId: String      │
│ - level: ViolationLevel │             │ - requests: List<Req>   │
│ - violations: List<Str> │             ├─────────────────────────┤
├─────────────────────────┤             │ + addRequest(req): void │
│ + addViolation(str)     │             │ + getRequests(): List   │
│ + setLevel(lvl): void   │             │ + getRequestCount(): int│
│ + isCritical(): bool    │             │ + clear(): void         │
└─────────────────────────┘             └─────────────────────────┘
```

### 2.2 Domain Entities

#### `Client` (`com.async_alpha.api_simulator.model.Client`)
- **Purpose:** Represents an identifiable consumer of the API (e.g., microservice, mobile client, web browser, external partner).
- **Invariants:**
  - `clientId`: Non-null, non-empty, trimmed unique identifier.
  - `name`: Fallback to `clientId` if omitted or empty.
- **Identity:** Equality and hash codes are based strictly on `clientId`.

#### `ServiceRequest` (`com.async_alpha.api_simulator.model.ServiceRequest`)
- **Purpose:** Represents an individual, timestamped invocation of an API endpoint.
- **Attributes:**
  - `clientId`: Identifies the client making the request.
  - `requestType`: Enumerated operation type.
  - `timestamp`: Temporal mark of request arrival (`java.time.LocalDateTime`).
- **Formatting:** Provides standardized `HH:mm:ss` representation via `getFormattedTime()`.

#### `RequestLog` (`com.async_alpha.api_simulator.model.RequestLog`)
- **Purpose:** Maintains the sequential ledger of requests originating from a specific client.
- **Features:**
  - Thread-safe defensive copying through `Collections.unmodifiableList(requests)`.
  - Utility methods: `getRequestCount()`, `isEmpty()`, and `clear()`.

#### `RequestType` (`com.async_alpha.api_simulator.model.RequestType`)
Enumerates operations mapped to real-world software API actions:
| Enum Constant | Display Name | Semantic Real-World Operation |
| :--- | :--- | :--- |
| `READ` | Read | Data fetch, query, status check, or login attempt |
| `WRITE` | Write | New record insertion, transaction creation, or payment submission |
| `UPDATE` | Update | Profile modification, settings update, state transition |
| `DELETE` | Delete | Resource removal, account deactivation, batch cancellation |

#### `ViolationLevel` (`com.async_alpha.api_simulator.model.ViolationLevel`)
Represents the severity state of client access behavior:
| Level | Display Name | Operational Meaning |
| :--- | :--- | :--- |
| `NORMAL` | Normal | Traffic conforms to operational thresholds; zero intervention. |
| `WARNING` | Warning | Suspicious patterns, anomalous spikes, or near-capacity consumption; monitoring advised. |
| `CRITICAL` | Critical | Severe rate limit exhaustion, burst flooding, or bot activity; rate limiting or lockout mandated. |

#### `AbuseReport` (`com.async_alpha.api_simulator.model.AbuseReport`)
- **Purpose:** Collects audit findings and violation flags discovered during policy inspection.
- **Key Logic:**
  - `setLevel(ViolationLevel newLevel)`: Enforces non-downgrade guarantees. Once a report reaches `CRITICAL`, it cannot transition down to `WARNING` or `NORMAL`. Once it reaches `WARNING`, it cannot revert to `NORMAL`.

---

## 3. Algorithmic Rate Limiting & Detection Engine

The simulator includes five specialized algorithms modeling industry rate-limiting and fraud-prevention systems.

```
                   «interface»
                   RatePolicy
       +evaluate(RequestLog, AbuseReport): void
                        ▲
   ┌───────────┬────────┴───┬─────────────┬──────────────┐
   │           │            │             │              │
FixedWindow  SlidingWindow BurstDetect AbnormalPattern RetryAbuse
  Policy       Policy       Policy       Policy         Policy
```

### 3.1 Fixed Window Policy (`FixedWindowPolicy`)
- **Algorithm:** Divides time into fixed, discrete windows of duration $W$ (default: 10 seconds). For a request arriving at time $T_{now}$, it counts all requests in the interval $[T_{now} - W, T_{now}]$.
- **Threshold:** Maximum allowed requests $M$ (default: 5 requests).
- **Evaluation:**
  $$\text{Count} = \sum_{r \in \text{Requests}} \mathbb{I}(T_{now} - W \le r.timestamp \le T_{now})$$
  If $\text{Count} > M$, a violation is generated and `ViolationLevel` is set to `WARNING`.
- **Characteristics:**
  - Low memory footprint.
  - Vulnerable to window boundary spikes (e.g., 5 requests at second 9 and 5 requests at second 11 result in 10 requests in 2 seconds without tripping the isolated window boundary).

### 3.2 Sliding Window Policy (`SlidingWindowPolicy`)
- **Algorithm:** Overcomes the boundary limitation of Fixed Window by dynamically calculating a continuous sliding window for every pair of requests.
- **Evaluation:**
  - For every request $i$, calculates the duration to subsequent requests $j$ ($j > i$):
    $$\Delta t = \text{Duration}(r_i.timestamp, r_j.timestamp)$$
  - Maintains an incremental counter for all requests falling within $\Delta t \le W$.
  - If the counter exceeds $M$, it immediately flags a sliding window breach and escalates severity directly to `CRITICAL`.
- **Characteristics:**
  - Guarantees strict rate limits across any arbitrary interval.
  - Prevents micro-burst exploitation across time borders.

### 3.3 Burst Detection Policy (`BurstDetectionPolicy`)
- **Algorithm:** Identifies high-frequency spikes concentrated in short windows (default: $\ge 4$ requests within $3$ seconds).
- **Escalation Rules:**
  - $\ge 1$ burst event detected: Escalates to `WARNING`.
  - $\ge 3$ distinct burst clusters detected: Escalates to `CRITICAL`.
- **Use Case:** Mitigates Denial of Service (DoS), rapid credential stuffing, and flash-mob scrapers.

### 3.4 Abnormal Pattern Policy (`AbnormalPatternPolicy`)
Employs three statistical heuristics to uncover automated scripts and bots:
1. **Off-Hours Activity Detection:**
   - Inspects the local hour of each request.
   - Requests occurring between **02:00:00 and 04:59:59** exceeding threshold (default: $> 3$) trigger an off-hours warning.
2. **Request Type Imbalance (Scraping Detection):**
   - Calculates the categorical distribution across `READ`, `WRITE`, `UPDATE`, and `DELETE`.
   - If total requests $\ge 10$ and any single request type exceeds **90.0%** of total traffic, flags potential automated data harvesting/scraping.
3. **Uniform Interval Analysis (Bot Detection):**
   - Calculates the inter-arrival delta between consecutive requests:
     $$\delta_k = |r_{k+1}.timestamp - r_k.timestamp|$$
   - Compares successive intervals: $|\delta_k - \delta_{k+1}| \le 1\text{ second}$.
   - If uniform intervals account for **$> 70.0\%$** of total requests (with $\ge 10$ samples), flags programmatic bot activity and triggers `CRITICAL`.

### 3.5 Retry Abuse Policy (`RetryAbusePolicy`)
- **Algorithm:** Evaluates client re-attempt patterns following rejected calls or fast repetitions.
- **Checks:**
  1. **Sub-second Rapid Retries:** Flags clients executing $> 5$ requests where consecutive delta $< 1000\text{ ms}$ (`CRITICAL`).
  2. **Consecutive Fast Succession:** Flags chains of $\ge 8$ requests arriving with inter-arrival intervals $\le 2\text{ seconds}$ (`WARNING`).

---

## 4. Service Layer & Engine Implementation

```
┌──────────────────────────────────────────────────────────────────────┐
│                         SERVICE LAYER                                │
├──────────────────────────┬───────────────────────────────────────────┤
│ RateLimitEnforcer        │ Real-time request admission & quotas      │
│ RateLimitAnalyzer        │ Multi-policy retrospective abuse auditing │
│ ClientActivityTracker    │ Historical counters, ledger & rates       │
│ RequestLogger            │ Central in-memory request store           │
│ DatasetLoader            │ Multi-syntax file/text parser             │
│ BurstTrafficGenerator    │ Synthetic high-velocity traffic engine    │
│ EnhancedReportGenerator  │ Diagnostic formatter (ASCII, HTML, table) │
└──────────────────────────┴───────────────────────────────────────────┘
```

### 4.1 RateLimitEnforcer (`com.async_alpha.api_simulator.service.RateLimitEnforcer`)
The real-time gatekeeper for simulated requests.
- `shouldBlock(ServiceRequest request)`: Checks if recent requests within `timeWindow` (10s) meet or exceed `maxRequests` (5).
- `processRequest(ServiceRequest request)`:
  - Evaluates `shouldBlock()`.
  - If allowed: logs the request into `RequestLogger`.
  - Returns a `RequestResult(request, blocked, remainingQuota)`.
- `getRemainingQuota(String clientId)`: Returns the available requests left in the current sliding window.
- `getTimeUntilReset(String clientId)`: Calculates the exact `java.time.Duration` until the oldest request in the window expires, restoring quota.

### 4.2 ClientActivityTracker (`com.async_alpha.api_simulator.service.ClientActivityTracker`)
Maintains per-client statistics:
- Total Requests, Allowed Requests, Blocked Requests.
- Success Rate calculation:
  $$\text{Success Rate} = \left(\frac{\text{Allowed Requests}}{\text{Total Requests}}\right) \times 100\%$$
- Stores chronological `ActivityRecord` instances tracking timestamps, types, and admission decisions (`ALLOWED` vs `BLOCKED`).

### 4.3 RateLimitAnalyzer (`com.async_alpha.api_simulator.service.RateLimitAnalyzer`)
Orchestrates offline and retrospective abuse evaluations:
- Extracts recent traffic slices (default: last 10 seconds).
- Passes the filtered log to each registered `RatePolicy`.
- Returns an aggregated `AbuseReport`.

### 4.4 BurstTrafficGenerator (`com.async_alpha.api_simulator.service.BurstTrafficGenerator`)
Generates high-speed request bursts for stress-testing policies:
- Parameters: `clientId`, `requestCount` (default: 20), `totalDuration` (default: 10s).
- Linearly calculates millisecond offsets across the duration window.
- Alternates `RequestType` values across iterations (`READ`, `WRITE`, `UPDATE`, `DELETE`) to simulate real-world diverse traffic.

### 4.5 DatasetLoader (`com.async_alpha.api_simulator.service.DatasetLoader`)
Robust parser capable of loading batch traffic logs from external files or strings.
- **Delimiter Support:** Detects and splits on commas (`,`), semicolons (`;`), or whitespace (`\s+`).
- **Header & Comment Handling:** Skips empty lines, comment lines starting with `#` or `//`, and header lines containing `timestamp` or `clientId`.
- **Timestamp Formats Handled:**
  1. Standard ISO-8601: `yyyy-MM-dd'T'HH:mm:ss`
  2. Standard Date-Time: `yyyy-MM-dd HH:mm:ss`
  3. Time Only (Full): `HH:mm:ss` (paired with current date)
  4. Time Only (Short): `HH:mm` (paired with current date)
- **Error Fault Tolerance:** Collects parsing errors into a `List<String> errors` without crashing execution.

---

## 5. User Interface Architecture (JavaFX)

The application provides a responsive, dark-mode desktop user interface built with JavaFX 21 controls and custom CSS styling.

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                    TopBarView (Branding, Team Attribution)                   │
├──────────────────────┬───────────────────────────────────────────────────────┤
│                      │                                                       │
│   ControlPanelView   │                 ActivityTableView                     │
│   • Client Selection │                 • Time, Client, Type, Status          │
│   • Type Filter      │                 • Color-Coded Badges                  │
│   • Action Buttons   │                 • Dynamic Filtering                   │
│   • Quota Badge      │                                                       │
│   • Risk Badge       │                                                       │
│   • StatsPanel       │                                                       │
│                      │                                                       │
├──────────────────────┴───────────────────────────────────────────────────────┤
│                      LogPanel (Real-Time Terminal Audit Log)                 │
└──────────────────────────────────────────────────────────────────────────────┘
```

### 5.1 Presentation Components

| UI Component | Implementation Class | Responsibilities |
| :--- | :--- | :--- |
| **Main Window** | `MainApp` | Boots JavaFX lifecycle, attaches `main.css`, sets stage dimensions ($900 \times 600$). |
| **Root Container** | `EnhancedDashboardView` | Coordinates panels via `BorderPane`, manages the 1-second auto-refresh timer, handles single-client vs all-clients views. |
| **Top Bar** | `TopBarView` | Displays application title, system subtitle, and team banner. |
| **Control Panel** | `ControlPanelView` | Provides client picker, request type selector, status filter, action triggers, and embedded metrics. |
| **Activity Table** | `ActivityTableView` | Virtualized `TableView` displaying chronological request activity with custom colored cell renderers. |
| **Live Statistics** | `StatsPanel` | Compact grid showing Total, Allowed, Blocked counts, and success rate with color indicators. |
| **System Console** | `LogPanel` | Monospaced auto-scrolling log area displaying timestamped system events. |
| **Report Viewer** | `ReportViewDialog` | Modal dialog displaying monospaced ASCII security audits, clipboard copying, and file exports. |
| **Dialog Helper** | `ReportDialogHelper` | Centralizes alerts, report popups, and native `FileChooser` dialogs. |

### 5.2 Single-Client vs Multi-Client (`ALL_CLIENTS`) Mode
- Selecting an individual client (`CLIENT_A`, `CLIENT_B`, etc.) isolates metrics, tables, and quota indicators to that specific tenant.
- Selecting `ALL_CLIENTS` aggregates metrics across all registered clients, sorts combined activity logs in descending chronological order, and enables fleet-wide burst tests and comparison reports.

---

## 6. Data Ingestion & Traffic Simulation

The application supports three ingestion workflows:

```
                  ┌─────────────────────────────────────────┐
                  │          INGESTION WORKFLOWS            │
                  └────────────────────┬────────────────────┘
                                       │
         ┌─────────────────────────────┼─────────────────────────────┐
         ▼                             ▼                             ▼
┌──────────────────┐          ┌──────────────────┐          ┌──────────────────┐
│   Manual Mode    │          │  Burst Generator │          │  Dataset Import  │
│ Interactive GUI  │          │ Synthetic Spikes │          │ CSV / TXT Batch  │
└──────────────────┘          └──────────────────┘          └──────────────────┘
```

### 6.1 Mode A: Manual Interactive Submission
1. Select the target client from the **Client** combo box.
2. Select the target operation (`READ`, `WRITE`, `UPDATE`, `DELETE`).
3. Click **"Send Request"**.
4. The request is processed by `RateLimitEnforcer`.
5. If allowed: quota decrements, table updates, and success log is recorded.
6. If blocked: quota reaches 0, table displays a bold red `BLOCKED` status, and warning is logged.

### 6.2 Mode B: High-Velocity Burst Simulation
1. Select a client or `ALL_CLIENTS`.
2. Click **"Simulate Burst (20 Req / 10s)"**.
3. The `BurstTrafficGenerator` synthesizes 20 requests distributed over a 10-second window.
4. The first 5 requests pass (consuming quota); the subsequent 15 requests are blocked.
5. Risk status escalates from `NORMAL` to `WARNING` or `CRITICAL`.

### 6.3 Mode C: Predefined Dataset File Import
1. Click **"Load Dataset (.txt/.csv)"**.
2. A native file chooser opens in the `sample_data/` directory.
3. Select `sample_requests.csv` or `test_requests.txt`.
4. The `DatasetLoader` parses records, registers new clients dynamically, runs requests through the enforcer, and refreshes the dashboard.

#### Dataset File Format Specification
Files may be formatted as comma-delimited, semicolon-delimited, or space-delimited text:
```csv
# Timestamp, Client ID, Request Type
10:00:00, CLIENT_A, READ
10:00:05, CLIENT_A, READ
10:00:10, CLIENT_A, WRITE
10:02:00, CLIENT_B, READ
10:02:01, CLIENT_B, WRITE
```

---

## 7. Reporting, Analytics & Diagnostic Export

The reporting subsystem generates multi-format diagnostic outputs.

### 7.1 Text/ASCII Violation Report Structure
Generated via `EnhancedReportGenerator.generateViolationReport()`:
```
====================================================================
                        API SECURITY REPORT
                  Full Violation & Usage Analysis

  Generated: 05 Sep 2026 • 19:13:17      [ CRITICAL ]

  CLIENT INFORMATION
  ------------------------------------------------------------------
  CLIENT ID           CLIENT_A
  REPORT DATE         05 Sep 2026 • 19:13:17
  SEVERITY            ● CRITICAL

  ------------------------------------------------------------------
  USAGE OVERVIEW
  TOTAL REQUESTS     80
  ALLOWED            8          10.0%
  BLOCKED            72         90.0% limit exceeded

  SUCCESS RATE
  10.0%   █░░░░░░░░░
  Last Activity: 09:11:35

  REQUEST TYPE DISTRIBUTION
  READ
  80 requests 100.0%
  ██████████

  SECURITY STATUS
  ------------------------------------------------------------------
                     1 VIOLATION(S) DETECTED

    1. Sliding window abuse detected: 6 requests within 10s window (limit: 5)

  ------------------------------------------------------------------
  RECOMMENDATION
  ------------------------------------------------------------------
  ✖  CRITICAL
  Immediate action required! Rate limit threshold breach detected.
  Consider blocking this client temporarily or rotating API credentials.
  Low success rate: Check client integration and error handling.
  ------------------------------------------------------------------
```

### 7.2 Multi-Client Full Comparison Matrix
Generated via `EnhancedReportGenerator.generateAllClientsFullReport()`:
```
======================================================================
                    ALL CLIENTS - FULL REPORT
                 API RATE-LIMIT & ABUSE SIMULATOR
======================================================================

Generated: 05 Sep 2026 • 19:13:17

OVERALL
----------------------------------------------------------------------
 Requests      Allowed       Blocked       Success Rate
 320           32 (10%)      288 (90%)     10.0%
----------------------------------------------------------------------

CLIENT PERFORMANCE
----------------------------------------------------------------------
 Client       Requests     Allowed      Blocked      Success   Last Act.
----------------------------------------------------------------------
 CLIENT_A     80           8 (10%)      72 (90%)     10.0%     09:11:35
 CLIENT_B     80           8 (10%)      72 (90%)     10.0%     09:11:35
 CLIENT_C     80           8 (10%)      72 (90%)     10.0%     09:11:35
 CLIENT_D     80           8 (10%)      72 (90%)     10.0%     09:11:35
----------------------------------------------------------------------
 TOTAL        320          32 (10%)     288 (90%)    10.0%     -
----------------------------------------------------------------------

RATE-LIMIT STATUS
----------------------------------------------------------------------
 Allowed :  █░░░░░░░░░   10.0%
 Blocked :  █████████░   90.0%
----------------------------------------------------------------------

SUMMARY
----------------------------------------------------------------------
 Total Clients          :     4
 Total Requests         :   320
 Total Allowed          :    32
 Total Blocked          :   288
 Overall Success Rate   :  10.0%
 Overall Block Rate     :  90.0%
 Average Requests/Client:    80
----------------------------------------------------------------------
                         END OF REPORT
======================================================================
```

### 7.3 Multi-Client HTML Comparative Report
The system can also generate an HTML-based tabular report via `generateComparisonReport()`:
- Styled with CSS variables matching the dark theme (`#090d16`, `#1e293b`, `#38bdf8`).
- Highlights success rates using color-coded CSS classes (`.ok`, `.warn`, `.danger`).

---

## 8. Setup, Build & Execution Guide

### 8.1 System Requirements

| Component | Minimum Specification | Recommended Specification |
| :--- | :--- | :--- |
| **Operating System** | Windows 10/11, macOS 12+, Ubuntu 20.04+ | Windows 11 / macOS Sonoma / Ubuntu 22.04 LTS |
| **JDK Runtime** | OpenJDK 17 | OpenJDK 21 LTS (Temurin / Oracle) |
| **Build Tool** | Apache Maven 3.8.0+ | Apache Maven 3.9.6+ |
| **Display Resolution**| $1024 \times 768$ | $1920 \times 1080$ or higher |

### 8.2 Maven Project Configuration (`pom.xml`)
The build configuration uses standard Maven coordinates and the official OpenJFX plugin:
```xml
<dependencies>
    <dependency>
        <groupId>org.openjfx</groupId>
        <artifactId>javafx-controls</artifactId>
        <version>21.0.2</version>
    </dependency>
    <dependency>
        <groupId>org.openjfx</groupId>
        <artifactId>javafx-fxml</artifactId>
        <version>21.0.2</version>
    </dependency>
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <version>5.10.2</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### 8.3 CLI Build and Run Instructions

#### Step 1: Clone or Navigate to the Workspace
```powershell
git clone https://github.com/With-ALIF/api-ratelimit-simulator.git
```

#### Step 2: Compile & Run Automated Tests
```powershell
mvn clean test
```

#### Step 3: Run the Desktop Application
```powershell
mvn javafx:run
```

#### Step 4: Package Executable JAR
```powershell
mvn clean package
```

---

## 9. Verification & Automated Test Suite

Automated tests are implemented using **JUnit Jupiter (JUnit 5)**.

### 9.1 Test Classes & Coverage

```
src/test/java/com/async_alpha/api_simulator/service/
├── BurstTrafficGeneratorTest.java
├── DatasetLoaderTest.java
└── EnhancedReportGeneratorTest.java
```

| Test Class | Targeted Component | Test Objectives |
| :--- | :--- | :--- |
| `BurstTrafficGeneratorTest` | `BurstTrafficGenerator` | Validates that burst counts match requested quotas, client IDs match, and timestamps increment monotonically. |
| `DatasetLoaderTest` | `DatasetLoader` | Tests parsing of comma/semicolon/space-separated datasets, comment/header skipping, and malformed line handling. |
| `EnhancedReportGeneratorTest`| `EnhancedReportGenerator` | Tests full multi-client report formatting, ASCII progress bar rendering, rate metrics calculation, and structural completeness. |

### 9.2 Running Tests
Execute the entire test suite via Maven:
```powershell
mvn test
```

Sample output:
```
[INFO] Running com.async_alpha.api_simulator.service.BurstTrafficGeneratorTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.async_alpha.api_simulator.service.DatasetLoaderTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.async_alpha.api_simulator.service.EnhancedReportGeneratorTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] -------------------------------------------------------
[INFO] Results:
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

---

## 10. Team Contributions & Academic Context

### 10.1 Academic Affiliation
- **Institution:** Begum Rokeya University, Rangpur (BRUR)
- **Department:** Department of Computer Science and Engineering (CSE)
- **Course:** Object-Oriented Programming (OOP)
- **Project Group:** Group - 02
- **Team Name:** **Runtime Crew**

### 10.2 Team Member Role Allocations

| Team Member | Role & Architectural Responsibilities |
| :--- | :--- |
| **Md. Abdullah - Al - Khalid Alif** | Lead Architecture, Simulator Engine Design, JavaFX Dashboard Integration, Report Subsystems |
| **Mithun Chandra Sarker** | Rate-Limiting Policy Implementation (`FixedWindowPolicy`, `SlidingWindowPolicy`), Evaluation Strategies |
| **Md. Asadujjaman** | Activity Tracking Engine, Anomaly Detection (`AbnormalPatternPolicy`, `RetryAbusePolicy`), Abuse Reporting |
| **Sojib Ray Shourav** | Dataset Loader Service (`DatasetLoader`), Synthetic Burst Generator (`BurstTrafficGenerator`), Automated Test Suites |

### 10.3 Academic Disclaimer
This project is engineered strictly for academic and pedagogical evaluation in Object-Oriented Programming. All rate-limiting, queuing, abuse detection, and traffic synthesis are executed purely in memory utilizing Java collections and the Java Time API. No external network connections, server sockets, or live third-party services are created or required.

---

## 11. Request Lifecycle & Admission Control: When Operations are Allowed or Blocked

In this simulator, all API interactions—whether performing data retrieval (`READ`), record creation (`WRITE`), state modification (`UPDATE`), or resource deletion (`DELETE`)—pass through a centralized admission control pipeline governed by `RateLimitEnforcer`, `RequestLogger`, and the pluggable `RatePolicy` strategy suite.

```
Incoming Request (READ / WRITE / UPDATE / DELETE)
                     │
                     ▼
       ┌───────────────────────────┐
       │     RateLimitEnforcer     │
       │   inspects recent window  │
       │     (Default: 10 sec)     │
       └─────────────┬─────────────┘
                     │
            Recent count >= 5 ?
            /                 \
          YES                  NO
          /                     \
         ▼                       ▼
   ┌───────────┐           ┌───────────┐
   │  BLOCKED  │           │  ALLOWED  │
   │ Quota = 0 │           │ Quota = N │
   └─────┬─────┘           └─────┬─────┘
         │                       │
         │ (Logged in            │ (Stored in
         │  ClientActivity)      │  RequestLogger & Activity)
         └───────────┬───────────┘
                     ▼
       ┌───────────────────────────┐
       │    RateLimitAnalyzer      │
       │  Evaluates Abuse Policies │
       │  (Burst, Pattern, Retry)  │
       └─────────────┬─────────────┘
                     ▼
       Updated Risk Level (NORMAL / WARNING / CRITICAL)
```

---

### 11.1 The Core Admission Principle

The simulator enforces rate-limiting at the **Client Identity level** using a sliding time window:

$$\text{Window Duration } (\Delta t) = 10 \text{ seconds}, \quad \text{Maximum Allowed Requests } (M) = 5 \text{ requests}$$

#### 1. When Any Request is **ALLOWED**:
A request ($r_{new}$) of type `READ`, `WRITE`, `UPDATE`, or `DELETE` is **ALLOWED** if and only if:
$$\text{RecentRequestsCount} = \sum_{r \in \text{Log}(client)} \mathbb{I}(T_{new} - 10\text{s} \le r.timestamp \le T_{new}) < 5$$
- **System Actions on ALLOWED:**
  1. The request status is marked as `ALLOWED` (rendered in green in the UI).
  2. The remaining quota decreases: $\text{RemainingQuota} = 5 - \text{RecentRequestsCount} - 1$.
  3. The request is persisted in the in-memory `RequestLogger`.
  4. The client's `allowedRequests` counter is incremented in `ClientActivityTracker`.
  5. The real-time dashboard log displays `✅ [HH:mm:ss] CLIENT_X - ALLOWED | Quota: N/5`.

#### 2. When Any Request is **BLOCKED**:
A request ($r_{new}$) of type `READ`, `WRITE`, `UPDATE`, or `DELETE` is **BLOCKED** if:
$$\text{RecentRequestsCount} \ge 5$$
- **System Actions on BLOCKED:**
  1. The request status is marked as `BLOCKED` (rendered in bold red in the UI).
  2. Remaining quota is pinned to `0`.
  3. The request is **NOT** added to the successful `RequestLogger` (preventing an artificial infinite extension of the rate window), but **IS** recorded in `ClientActivityTracker` for security analysis and success rate calculations.
  4. The real-time console prints `⛔ [HH:mm:ss] CLIENT_X - BLOCKED | Quota: 0/5 ⚠️ RATE LIMIT EXCEEDED`.
  5. The client must wait until `getTimeUntilReset()` reaches $0$ seconds (as the oldest recorded request in the window expires).

---

### 11.2 Operation-Specific Rules & Abuse Detection

While the volume threshold applies across all requests, each operation type has distinct behavioral implications evaluated by the secondary anomaly policies:

#### 1. `READ` Operations (Data Fetch, Queries, Login Attempts)
- **When Allowed:** Sent when client quota $> 0$ (fewer than 5 requests in the last 10 seconds).
- **When Blocked:** Sent when 5 requests have already occurred within the 10-second window.
- **Abuse Policy Trigger (`AbnormalPatternPolicy`):**
  - If a client issues $\ge 10$ total requests and $> 90\%$ of them are `READ`, the engine flags an **Imbalance Anomaly** representing automated data harvesting or credential spraying:
    $$\frac{\text{Count}(\text{READ})}{\text{Total Requests}} > 0.90 \implies \text{WARNING: Potential Scraping}$$

#### 2. `WRITE` Operations (Record Creation, Payment Submission)
- **When Allowed:** Sent when client quota $> 0$.
- **When Blocked:** Sent when client quota $= 0$.
- **Abuse Policy Trigger (`BurstDetectionPolicy`):**
  - High-speed `WRITE` flooding (e.g., automated transaction bombing) where $\ge 4$ requests arrive within $3$ seconds triggers a burst violation:
    $$\text{Count}(r \in [T, T + 3\text{s}]) \ge 4 \implies \text{Burst Violation Detected}$$
  - $\ge 1$ burst sets risk level to `WARNING`; $\ge 3$ bursts escalate directly to `CRITICAL`.

#### 3. `UPDATE` Operations (Profile Edits, State Transitions)
- **When Allowed:** Sent when client quota $> 0$.
- **When Blocked:** Sent when client quota $= 0$.
- **Abuse Policy Trigger (`RetryAbusePolicy`):**
  - Rapid, repetitive state modification attempts (e.g., trying to overwrite locked resources or brute-force state changes) where $\ge 8$ consecutive requests have inter-arrival deltas $\le 2$ seconds trigger:
    $$\text{ConsecutiveFastRequests} \ge 8 \implies \text{WARNING: Excessive Consecutive Requests}$$

#### 4. `DELETE` Operations (Resource Removal, Account Deletion)
- **When Allowed:** Sent when client quota $> 0$.
- **When Blocked:** Sent when client quota $= 0$.
- **Abuse Policy Trigger (`AbnormalPatternPolicy` - Bot Interval Detection):**
  - Because `DELETE` is a high-impact operation, automated scripts deleting resources in a timed loop are detected when consecutive time intervals between requests are uniform ($|\delta_k - \delta_{k+1}| \le 1\text{s}$):
    $$\text{UniformIntervalRatio} > 70\% \implies \text{CRITICAL: Bot-like Automated Script Detected}$$

---

### 11.3 Comprehensive Decision & Policy Matrix

| Request Type | Condition for `ALLOWED` | Condition for `BLOCKED` | Associated Anomaly Detection Policy | Security Consequence on Abuse |
| :--- | :--- | :--- | :--- | :--- |
| **`READ`** | Total requests in last 10s $< 5$ | Total requests in last 10s $\ge 5$ | `AbnormalPatternPolicy` (Type Imbalance $> 90\%$) | Flagged as scraping / data harvesting (`WARNING`) |
| **`WRITE`** | Total requests in last 10s $< 5$ | Total requests in last 10s $\ge 5$ | `BurstDetectionPolicy` ($\ge 4$ requests in $3$ seconds) | Flash flood / DoS attack mitigation (`CRITICAL` if $\ge 3$ bursts) |
| **`UPDATE`** | Total requests in last 10s $< 5$ | Total requests in last 10s $\ge 5$ | `RetryAbusePolicy` ($\ge 8$ rapid consecutive calls) | Resource churn / aggressive retry lockout (`WARNING`) |
| **`DELETE`** | Total requests in last 10s $< 5$ | Total requests in last 10s $\ge 5$ | `AbnormalPatternPolicy` (Uniform interval $> 70\%$) | Programmatic bot script mitigation (`CRITICAL`) |
| **Any (`READ`/`WRITE`/`UPDATE`/`DELETE`)** | Sub-second retry delta $\ge 1000\text{ ms}$ | Sub-second retry delta $< 1000\text{ ms}$ ($> 5$ times) | `RetryAbusePolicy` (Rapid retry loop) | Client lockout recommended (`CRITICAL`) |
| **Any (`READ`/`WRITE`/`UPDATE`/`DELETE`)** | Timestamp outside 02:00–05:00 AM | $> 3$ requests occurring between 02:00 and 05:00 AM | `AbnormalPatternPolicy` (Off-Hours Activity) | Nocturnal anomaly surveillance (`WARNING`) |

---

### 11.4 Quota Replenishment Lifecycle

```
Second:   0s   1s   2s   3s   4s   5s   6s   7s   8s   9s  10s  11s  12s
Event:   REQ1 REQ2 REQ3 REQ4 REQ5 REQ6  --   --   --   --   --  REQ7  --
Verdict:  OK   OK   OK   OK   OK  BLOCK                       OK
Quota:    4    3    2    1    0     0                          1
                                                               ^
                                              (REQ1 expires after 10s,
                                               restoring 1 slot of quota)
```

1. **Step 1 (Quota Consumption):** At seconds $0, 1, 2, 3, 4$, five requests (`REQ1` to `REQ5`) arrive. Each is **ALLOWED**. Quota decrements from $4$ down to $0$.
2. **Step 2 (Rate Limit Enforcement):** At second $5$, `REQ6` arrives. Because 5 requests were already logged in the last 10 seconds, `REQ6` is **BLOCKED**.
3. **Step 3 (Window Eviction & Recovery):** At second $11$, more than 10 seconds have elapsed since `REQ1` (which occurred at second $0$). `REQ1` falls outside the active sliding window $[1\text{s}, 11\text{s}]$. The active count drops to 4, restoring 1 request slot.
4. **Step 4 (Subsequent Admission):** `REQ7` arriving at second $11$ is immediately **ALLOWED**.
