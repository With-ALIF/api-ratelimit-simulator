# Smart API Rate-Limit & Abuse Simulator

[![Java Version](https://img.shields.io/badge/Java-17%2B-blue.svg?logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
[![JavaFX](https://img.shields.io/badge/JavaFX-21.0.2-orange.svg?logo=java&logoColor=white)](https://openjfx.io/)
[![Build](https://img.shields.io/badge/Build-Maven-C71A36.svg?logo=apache-maven&logoColor=white)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/License-Academic-green.svg)]()

> A robust, interactive **JavaFX desktop application** that simulates how modern cloud platforms, fintech systems, and distributed web architectures monitor API traffic, enforce rate-limiting rules, and mitigate abusive access patterns—entirely in memory without external network dependencies.

---

## 👥 Team Information

**Team Name:** **Runtime Crew**  
**Department:** Computer Science and Engineering (CSE)  
**Institution:** Begum Rokeya University, Rangpur (BRUR)  
**Course:** Object-Oriented Programming (OOP)  

### Team Members

| Name | Role / Contribution |
| :--- | :--- |
| **Md. Abdullah - Al - Khalid Alif** | Architecture, Simulator Engine, Core UI & Integration |
| **Mithun Chandra Sarker** | Rate Limiting Policies, Evaluation Strategies |
| **Md. Asadujjaman** | Activity Tracking, Abuse Detection & Reporting |
| **Sojib Ray Shourav** | Dataset Ingestion, Testing & Traffic Generation |

---

## 📌 Project Overview

In high-throughput distributed systems, thousands of clients concurrently query web endpoints. To guarantee high availability, fair usage, and security against Denial-of-Service (DoS) attacks or credential stuffing, systems enforce **API rate-limiting and abuse detection**.

This simulator models the internal logical behavior of such protective engines:
- **Zero Network Overhead:** Operates 100% in-memory using Java objects, thread-safe collections, and the Java Time API.
- **Request Lifecycle Emulation:** Models every operation from ingestion and policy evaluation to activity recording and violation reporting.
- **Visual Analytics:** Features a modern, reactive JavaFX dashboard displaying real-time metrics, logs, tabular activity, and per-client abuse analytics.

---

## 🎯 Key Features

### 1. Multi-Mode Data Ingestion
- **Manual Interactive Form:** Submit individual requests with custom client identifiers and request categories.
- **High-Velocity Burst Generator:** Generate burst traffic (e.g., 20+ requests in a microsecond window) to simulate DDoS spikes or flash mobs.
- **Predefined Dataset Loader:** Batch load and parse `.csv` or `.txt` activity logs (`sample_data/sample_requests.csv`, `test_requests.txt`).

### 2. Pluggable Rate-Limiting Engine (Strategy Pattern)
- **Fixed Window Policy:** Tracks request counts within discrete time intervals (e.g., 10 requests / 60 seconds).
- **Sliding Window Policy:** Smooths boundary bursts by inspecting timestamps across a continuous rolling timeframe.
- **Burst Detection Policy:** Detects rapid-fire spikes occurring within sub-second thresholds.
- **Abnormal Pattern Policy:** Identifies suspicious frequency shifts and irregular access behavior.
- **Retry Abuse Policy:** Detects aggressive re-attempts following rejected calls.

### 3. Abuse Detection & Violation Classification
Assigns granular **Violation Levels** based on offending patterns:
- `NORMAL`: Regular client interaction within standard limits.
- `WARNING`: Approaching quotas or minor transient spikes.
- `CRITICAL`: Repeated threshold breaches and burst abuse.
- `BLOCKED`: Persistent abusive patterns triggering client lockout.

### 4. Rich Desktop Dashboard (JavaFX)
- **Header & Metric Cards:** Real-time counters for Total Requests, Allowed, Blocked, and Active Clients.
- **Interactive Control Panel:** Client creation, policy switching, manual request creation, and burst triggers.
- **Activity Table:** Sortable and filterable history of every incoming request with status tags.
- **System Event Log:** Live audit stream displaying policy verdicts and system actions.
- **Detailed Abuse Report Dialog:** Comprehensive per-client breakdowns with violation metrics and recommendations.

---

## 🏗️ Architecture & OOP Design

The project is structured according to SOLID principles and classical object-oriented design patterns:

```
┌─────────────────────────────────────────────────────────────┐
│                       JavaFX UI Layer                       │
│    (EnhancedDashboardView, ActivityTableView, StatsPanel)   │
└──────────────────────────────┬──────────────────────────────┘
                               │ Dispatches Actions
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                        Service Layer                        │
│   • ClientActivityTracker      • BurstTrafficGenerator      │
│   • RateLimitEnforcer          • DatasetLoader              │
│   • RateLimitAnalyzer          • EnhancedReportGenerator    │
└──────────────────────────────┬──────────────────────────────┘
                               │ Evaluates
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                    Policy Strategy Layer                    │
│   «interface» RatePolicy                                    │
│   ├── FixedWindowPolicy         ├── BurstDetectionPolicy    │
│   ├── SlidingWindowPolicy       ├── AbnormalPatternPolicy   │
│   └── RetryAbusePolicy                                      │
└──────────────────────────────┬──────────────────────────────┘
                               │ Encapsulates
                               ▼
┌───────────────────────────────────────────────────────────────────────────┐
│                         Domain Model                                      │
│   • Client                   • ServiceRequest          • RequestLog       │
│   • RequestType (Enum)       • ViolationLevel (Enum)   • AbuseReport      │
│                                                                           │
└───────────────────────────────────────────────────────────────────────────┘
```

### Applied OOP Concepts
1. **Strategy Pattern (`RatePolicy`):** Decouples rate-limiting algorithms from the evaluation engine, enabling hot-swapping between Fixed Window, Sliding Window, and Abuse policies at runtime.
2. **Encapsulation:** Enforces state isolation within domain models (`Client`, `RequestLog`), preventing direct mutations.
3. **Polymorphism:** Evaluates heterogenous policies through a unified `evaluate(Client, ServiceRequest)` contract.
4. **Rich Enums:** Type-safe enumeration of operations (`READ`, `WRITE`, `DELETE`, `LOGIN`, `PAYMENT`, `SEARCH`) and severity (`NORMAL`, `WARNING`, `CRITICAL`, `BLOCKED`).
5. **Separation of Concerns (SoC):** Distinct separation between Presentation (JavaFX), Business Logic (Services/Policies), and Data Representation (Models).

---

## 🛠️ Prerequisites & Technology Stack

| Dependency | Required Version | Description |
| :--- | :--- | :--- |
| **JDK** | Java 17 or higher (Java 21 recommended) | Modern Java language runtime |
| **JavaFX** | 21.0.2 | JavaFX Controls & FXML |
| **Apache Maven** | 3.8+ | Dependency management and build tool |
| **JUnit Jupiter** | 5.10.2 | Unit testing framework |

---

## 🚀 Getting Started

### 1. Clone the Repository
```bash
git clone https://github.com/With-ALIF/api-ratelimit-simulator.git
cd api-ratelimit-simulator
```

### 2. Build the Project
Compile classes and run automated unit tests:
```bash
mvn clean test
```

### 3. Run the Application
Launch the JavaFX dashboard:
```bash
mvn javafx:run
```

Or execute directly using the combined command:
```bash
mvn clean javafx:run
```

### 4. Package as JAR
To package the application into an executable artifact:
```bash
mvn clean package
```

---

## 🧪 Automated Testing

The project includes an automated test suite verifying dataset parsing, error handling, and burst traffic generation:

```bash
mvn test
```

Test results summary:
- `BurstTrafficGeneratorTest`: Validates high-volume request synthesis, client mapping, and timestamps.
- `DatasetLoaderTest`: Verifies parsing of CSV and TXT files, comment skipping, and invalid record filtering.

---

## 📖 How to Use the Simulator

1. **Register or Select a Client:**
   - Use the Control Panel to pick an existing client (`CLIENT_A`, `CLIENT_B`, etc.) or register a new one.
2. **Choose a Rate-Limiting Policy:**
   - Select between *Fixed Window*, *Sliding Window*, *Burst Detection*, or *Abnormal Pattern*.
3. **Generate Traffic:**
   - **Manual Mode:** Choose an action (e.g. `LOGIN`, `READ`, `PAYMENT`) and click **"Send Request"**.
   - **Burst Mode:** Click **"Generate Burst"** to issue simulated high-speed requests.
   - **Import Mode:** Load `sample_data/sample_requests.csv` to replay recorded network sessions.
4. **Monitor Analytics:**
   - Watch real-time counter changes on the top stats cards.
   - View allowed vs blocked logs with precise timestamps in the table.
5. **Inspect Abuse Reports:**
   - Open the **"Abuse Report"** dialog to review per-client request counts, block ratios, and violation severity status.

---

## 🤝 Collaboration & Academic Disclaimer

- **Asynchronous Collaboration:** Developed iteratively with modular tasks distributed across team members using version control.
- **Academic Context:** Developed for the **Object-Oriented Programming** coursework at Begum Rokeya University, Rangpur. All network and server mechanics are simulated purely through in-memory OOP abstractions for educational and demonstration purposes.

---

## 📄 License

This repository is maintained for academic coursework under Begum Rokeya University, Rangpur. Distributed for educational demonstration.