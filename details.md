# Smart API Rate-Limit & Abuse Simulator

In modern software systems, many users or systems access the same service simultaneously. To keep services reliable, secure, and fair, the system intetrnally keeps track of how requests are made and checks whether the usage is normal or abnormal.

This project is a **JavaFX-based desktop application** that simulates how modern web systems internally protect APIs without using real networking or servers.

Build a JavaFX-based desktop application that simulates how modern web systems protect APIs by:

- Tracking client request activity 
- Applying rate-limiting rules 
- Detecting abnormal or abusive usage patterns 
- Generating a usage and violation report per client 

---
 
This project models concepts used in `cloud platforms, fintech systems, and large-scale web services`, without requiring real networking or servers. The project simulates service request behavior using manually generated or `file-based data`. No real service, API, or network communication is required or expected. In this project, you are NOT building a real online service, login system, or payment system. You are building a simulation that models how such systems internally record requests and analyze usage patterns.

## 🧠 Concept Overview

This simulator models concepts commonly used in:

- Cloud platforms  
- Fintech systems  
- Large-scale web services  

---

## 🛠 Tools & Technologies

- **Java 21+**  
- **JavaFX** (Graphical User Interface)  
- **Java Collections** (`Map`, `List`)  
- **Java Time API** (`LocalDateTime`, `Duration`)  
- **Optional:** File I/O (`Import/Export request logs`)

---

## Core OOP Concepts

- Domain Modeling (`Client, RequestLog, RatePolicy`) 
- Encapsulation of counters and thresholds 
- Polymorphism (`different rate-limit policies`) 
- Strategy-like design (`policy evaluation`) 
- Use of Enums (`RequestType, ViolationLevel`)

## Suggested Class Struncture

- Client (`clientId, name`) 
- RequestLog (`timestamp, requestType`) 
- RatePolicy (interface: `evaluate(Client)`) 
- FixedWindowPolicy implements RatePolicy 
- SlidingWindowPolicy implements RatePolicy 
- AbuseReport (`violations, severity`)

## Featur Must Implement

- Register API clients 
- Record simulated API requests 
- Apply rate-limit policies 
- Detect excessive requests within time windows 
- Generate per-client abuse report