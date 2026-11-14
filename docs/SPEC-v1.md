# Personal Financial Advisor — India V1 Specification

| Field | Value |
|-------|-------|
| **Version** | 2.0 |
| **Date** | 14 November 2026 |
| **Status** | **🔒 LOCKED** — Architecture review applied |
| **Scope** | Private, single-user, India-first holistic financial adviser |

---

## 1. Overview

Build a private, India-focused holistic financial adviser delivered as a **standalone web application**. The system combines a deterministic **Java rule engine** for precise financial calculations with an **AI/LLM layer** for interpretation, natural language advice, and opinionated recommendations. The adviser understands the user's complete financial position — assets, liabilities, goals, risk capacity, behavioural tolerance, and life stage — then provides clear, evidence-based guidance.

**Design principle:** The product is a financial decision engine, not merely a stock picker or mutual-fund recommender. Specialist investment and tax tools support the decision engine; they do not determine the household strategy.

**V1 implementation:** Standalone web application (React frontend + Spring Boot API) running locally in Docker Desktop. Reusable as the intelligence layer for a future cloud-hosted application.

---

## 2. Functional Requirements

### FR-1: Financial Profile Management

| ID | Requirement | Source |
|----|-------------|--------|
| FR-1.1 | The system SHALL maintain a persistent structured financial profile across sessions. Profile data SHALL be stored in DynamoDB as the system of record — not in conversational memory or session state. | [Source: FR-005, DL-004, NFR-001] |
| FR-1.2 | The system SHALL use a flexible schema that stores whatever financial data the user provides. The engine SHALL handle missing fields gracefully — requesting data when needed for a specific calculation rather than requiring all fields upfront. | [Source: Grill — profile_scope] |
| FR-1.3 | The system SHALL version all financial facts using snapshot-based history. Each profile update SHALL create a dated snapshot. The current snapshot is the active view; all previous snapshots SHALL be queryable for trend analysis and audit. | [Source: NFR-002, DL-017, Grill — versioning] |
| FR-1.4 | V1 SHALL support a single private user. Multi-user support is deferred to a future phase. | [Source: FR-001, DL-001] |
| FR-1.5 | V1 SHALL use INR as the default currency and apply Indian financial products, Indian tax concepts, and Indian household-planning assumptions throughout. | [Source: FR-002, DL-002] |

### FR-2: Financial Health Assessment

| ID | Requirement | Source |
|----|-------------|--------|
| FR-2.1 | The system SHALL calculate net worth as total assets minus total liabilities, with a clear breakdown of liquid assets (FDs, MFs, stocks, EPF) versus illiquid assets (land, gold, property). | [Source: Calculation §7] |
| FR-2.2 | The system SHALL calculate cash-flow health as: gross income minus (living expenses + EMIs + insurance premiums + planned investment commitments). The result is the available monthly surplus. | [Source: Calculation §7, Capability §3] |
| FR-2.3 | The system SHALL calculate emergency coverage as: emergency-designated assets ÷ mandatory monthly expenditure. This ratio SHALL be recalculated dynamically whenever mandatory outflows change (e.g., when a home loan EMI begins). | [Source: Calculation §7, DL-014] |
| FR-2.4 | The system SHALL detect concentration risk when any single asset class, sector, or instrument exceeds a configurable threshold of total portfolio value. | [Source: Capability §3] |

### FR-3: Goal Planning

| ID | Requirement | Source |
|----|-------------|--------|
| FR-3.1 | The system SHALL calculate future goal values using inflation-adjusted projections. Each goal SHALL have: target amount (today's value), target date, assumed inflation rate, and computed future value. | [Source: Capability §3] |
| FR-3.2 | The system SHALL assess goal funding status by computing: current goal-tagged assets + projected future contributions (with assumed return rates) versus required future corpus. | [Source: Calculation §7] |
| FR-3.3 | The system SHALL quantify the shortfall per goal and compute the required monthly contribution to close each gap within the goal's time horizon. | [Source: Capability §3] |
| FR-3.4 | The system SHALL track goal progress over time using versioned snapshots, showing funding percentage at each snapshot date. | [Source: Capability §3, FR-1.3] |
| FR-3.5 | The system SHALL NOT force goals into priority labels (must-have / important / nice-to-have). When available cash flow cannot fund all goals, the system SHALL quantify the shortfall per goal and explain trade-offs explicitly. | [Source: §5, DL-006] |
| FR-3.6 | The system SHALL assign risk separately per goal. Near-term goals (< 3 years) SHALL receive low-risk allocation regardless of the user's overall risk tolerance. | [Source: §5, DL-009] |
| FR-3.7 | The system SHALL enforce that the same rupee is never simultaneously counted toward multiple goals or purposes (e.g., emergency reserve AND investable capital). If the user wishes to reallocate, it must be an explicit action. | [Source: §5, EC-6] |

### FR-4: Risk Engine

| ID | Requirement | Source |
|----|-------------|--------|
| FR-4.1 | The system SHALL use a dynamic risk model. It SHALL NOT assign a permanent conservative/moderate/aggressive label that applies to all goals. | [Source: §4, DL-007] |
| FR-4.2 | The risk model SHALL evaluate five dimensions for each assessment context: (1) **Risk tolerance** — psychological willingness to accept volatility, (2) **Risk capacity** — financial ability to withstand loss (liquidity, liabilities, income stability, commitments), (3) **Risk requirement** — return needed to meet a specific goal, (4) **Goal horizon** — time until the money is needed, (5) **Life stage** — age, years to retirement, earning period, accumulated corpus, debt position. | [Source: §4] |
| FR-4.3 | **Core risk rule:** When risk dimensions conflict, the safer constraint SHALL win. Risk tolerance determines comfort; risk capacity determines financial acceptability; goal horizon determines where risk is permitted. | [Source: §4] |
| FR-4.4 | Risk assessment SHALL be re-evaluated whenever material changes occur: income change, new/removed liabilities, goal additions/removals, life events, or market conditions that affect capacity. | [Source: §4, DL-007] |

### FR-5: Cash Flow & Allocation

| ID | Requirement | Source |
|----|-------------|--------|
| FR-5.1 | The system SHALL analyse income, mandatory expenses, EMIs, insurance premiums, and investment commitments to determine available monthly surplus. | [Source: Capability §3] |
| FR-5.2 | The system SHALL recommend how to allocate the monthly surplus across goals, based on goal priority (user-defined or shortfall-based), time horizon, and risk profile per goal. | [Source: Capability §3] |
| FR-5.3 | Credit card usage SHALL be treated as a spending/payment mechanism, not as revolving debt, when statements are paid in full. The system SHALL reclassify to debt if a revolving balance is detected. | [Source: DL-012] |

### FR-6: Portfolio Strategy

| ID | Requirement | Source |
|----|-------------|--------|
| FR-6.1 | The system SHALL recommend asset allocation and goal tagging BEFORE recommending specific investment products. Strategy determines allocation; allocation determines products. | [Source: Capability §3] |
| FR-6.2 | Each asset SHALL have a role or goal tag (emergency reserve, education, retirement, furnishing, etc.) where possible. Untagged assets SHALL be flagged for assignment. | [Source: §5] |
| FR-6.3 | Retirement investments SHALL follow a glide-path approach. De-risking SHALL consider funded status, income, liabilities, and surplus wealth — NOT age alone. | [Source: §5, DL-010] |
| FR-6.4 | The emergency fund (currently ₹9,00,000 FD) SHALL be treated as protected capital. It SHALL NOT be included in investable capital calculations unless the user explicitly reallocates it. | [Source: §5, DL-011] |
| FR-6.5 | The apartment (≈₹2 crore purchase, ≈₹1.8 crore expected mortgage, ≈₹20 lakh furnishing) SHALL be modeled as a major liability/commitment, not simply as a net-worth asset. Its impact on liquidity and risk capacity SHALL be prominently factored. | [Source: DL-013] |

### FR-7: Stress Testing

| ID | Requirement | Source |
|----|-------------|--------|
| FR-7.1 | The system SHALL model an **income loss** scenario: partial or full income loss for configurable duration. Show impact on EMI serviceability, emergency coverage, and goal funding. | [Source: Capability §3] |
| FR-7.2 | The system SHALL model a **market fall** scenario: configurable equity drawdown (e.g., -20%, -40%). Show impact on portfolio value, goal funding status, and whether rebalancing is needed. | [Source: Capability §3] |
| FR-7.3 | The system SHALL model an **interest rate change** scenario: configurable rate increase/decrease. Show impact on EMI amounts, debt serviceability, and fixed-income returns. | [Source: Capability §3] |
| FR-7.4 | The system SHALL model a **large unplanned expense** scenario: configurable amount. Show impact on emergency coverage, liquidity, and goal timeline. | [Source: Capability §3] |
| FR-7.5 | For each stress scenario, the system SHALL show whether key goals remain viable and which goals are most at risk. | [Source: Calculation §7] |

### FR-8: Scenario Analysis

| ID | Requirement | Source |
|----|-------------|--------|
| FR-8.1 | The system SHALL compare alternative financial decisions side by side: higher SIP amount, mortgage prepayment vs investing, later retirement, lower goal target, higher equity allocation, etc. Each comparison SHALL show impact on goal funding, risk, and timeline. | [Source: Capability §3] |
| FR-8.2 | The system SHALL balance expected wealth growth AND probability of achieving goals. It SHALL NOT optimise solely for maximum return or solely for maximum certainty. | [Source: DL-008] |

### FR-9: Periodic Review

| ID | Requirement | Source |
|----|-------------|--------|
| FR-9.1 | The system SHALL recalculate plan assumptions and projections whenever the user updates material inputs (income, expenses, assets, liabilities, goals, or market conditions). | [Source: Capability §3] |
| FR-9.2 | The system SHALL flag material drift: when actual values deviate significantly from plan assumptions (e.g., expenses rose 20%, portfolio underperformed by 15%). | [Source: Capability §3] |

### FR-10: Dashboard

| ID | Requirement | Source |
|----|-------------|--------|
| FR-10.1 | The system SHALL provide a multi-dimensional financial health dashboard. A single composite score is optional and SHALL NOT hide dimension-level problems. | [Source: §11, DL-019] |
| FR-10.2 | The dashboard SHALL display these 10 health dimensions: (1) Liquidity status, (2) Insurance status, (3) Debt exposure, (4) Retirement trajectory, (5) Education trajectory, (6) Portfolio risk, (7) Income concentration, (8) Emergency coverage, (9) Housing commitment, (10) Key warnings and recommended next actions. | [Source: §11] |
| FR-10.3 | The dashboard SHALL be an interactive web interface with drill-down capability per dimension. Each dimension SHALL show: current status, trend (from snapshots), and recommended actions. | [Source: Grill — v1_target, react_detail] |

### FR-11: Specialist Delegation

| ID | Requirement | Source |
|----|-------------|--------|
| FR-11.1 | The system SHALL delegate stock/IPO evaluation to a specialist analysis tool, but ONLY after the holistic adviser has determined that direct equity is appropriate for the user's plan. | [Source: §8] |
| FR-11.2 | The system SHALL delegate mutual fund analysis to a specialist tool, but ONLY after the adviser has determined the required asset class, goal fit, and allocation. | [Source: §8] |
| FR-11.3 | The system SHALL delegate Indian tax analysis (capital gains, DTAA, residential status) to a specialist tool when tax considerations affect a recommendation. | [Source: §8] |
| FR-11.4 | The holistic adviser SHALL retain strategy ownership. Specialist outputs are inputs to a broader decision, not the final household recommendation. | [Source: §8, DL-015] |
| FR-11.5 | Specialist tools SHALL use explicit as-of dates for all data (market prices, NAV, tax rules). Stale analysis SHALL be flagged. | [Source: §9, DL-016] |

### FR-12: AI Advisory Layer

| ID | Requirement | Source |
|----|-------------|--------|
| FR-12.1 | The adviser SHALL be opinionated when evidence supports a conclusion. It SHALL recommend a specific course of action rather than presenting neutral options. | [Source: FR-004, DL-005] |
| FR-12.2 | The adviser SHALL actively challenge unsafe or inconsistent user choices (e.g., high-risk allocation for a near-term goal, inadequate insurance, emergency fund depletion). | [Source: FR-004] |
| FR-12.3 | Computation SHALL be hybrid: Java rule engine performs all precise financial calculations (compounding, projections, risk scores, stress tests). LLM provides interpretation, natural language explanations, and conversational advice. | [Source: Grill — computation_model] |
| FR-12.4 | The rule engine SHALL validate all LLM financial assertions. The LLM SHALL NOT fabricate financial products, return rates, tax rules, or calculation results. Any LLM output referencing specific financial facts SHALL be verified against the profile data and rule engine outputs. | [Source: EC-4, Grill — error_handling] |
| FR-12.5 | The system SHALL provide a conversational chat interface for financial queries, planning discussions, and what-if questions, in addition to the structured dashboard. | [Source: Grill — v1_target] |

---

## 3. Non-Functional Requirements

### NFR-1: Security

| ID | Requirement | Source |
|----|-------------|--------|
| NFR-1.1 | The system SHALL authenticate users via JWT tokens with username/password credentials. Token expiry, refresh, and invalidation SHALL be implemented. | [Source: Grill — auth_model] |
| NFR-1.2 | The system SHALL implement full encryption: data at rest (DynamoDB encryption), data in transit (TLS for all API calls), and field-level encryption for sensitive financial data (account balances, income figures, asset values). <!-- SPEC-BINDING: NFR-1.2 = ENCRYPTION_STRATEGY --> | [Source: Grill — data_encryption] |
| NFR-1.3 | The system SHALL NOT store PAN, Aadhaar, passwords, OTPs, broker credentials, or full bank-account numbers in the financial profile. | [Source: NFR-003, DL-018] |
| NFR-1.4 | Future external data integrations SHALL use secure connectors (OAuth 2.0 or equivalent). The system SHALL NOT store external-service credentials directly. | [Source: NFR-004] |
| NFR-1.5 | User passwords SHALL be hashed using bcrypt (cost factor 12) or Argon2id before storage. Plaintext passwords SHALL never be stored or logged. <!-- SPEC-BINDING: NFR-1.5 = PASSWORD_HASH_ALGORITHM --> | [Source: Arch Review S-1] |
| NFR-1.6 | User chat input to the LLM SHALL be sandboxed. System prompts encoding financial adviser rules and persona SHALL be protected from user-prompt overrides. Spring AI advisors SHALL enforce persona boundaries to prevent prompt injection attacks. | [Source: Arch Review S-2] |
| NFR-1.7 | All API inputs SHALL be validated for type, range, and format. Financial amounts SHALL be validated as non-negative BigDecimal values. Invalid input SHALL be rejected with a descriptive error before reaching the business logic layer. | [Source: Arch Review S-3] |
| NFR-1.8 | CORS SHALL be configured to allow only the frontend origin. In Docker Compose local development, this is `http://localhost:5173` (Vite default). For cloud deployment, the allowed origin SHALL be configurable. <!-- SPEC-BINDING: NFR-1.8 = CORS_ALLOWED_ORIGINS --> | [Source: Arch Review S-4] |
| NFR-1.9 | JWT secrets SHALL be stored in environment variables for local Docker development. For cloud deployment, secrets SHALL be migrated to AWS Secrets Manager or equivalent secure vault. | [Source: Arch Review S-5] |

### NFR-2: Data Integrity

| ID | Requirement | Source |
|----|-------------|--------|
| NFR-2.1 | DynamoDB SHALL be the system of record for all financial facts. No financial data SHALL depend solely on LLM conversation history or session state. | [Source: NFR-001, DL-004] |
| NFR-2.2 | All profile data SHALL be versioned using dated snapshots. The snapshot key SHALL include a timestamp. Current state = latest snapshot. Historical queries SHALL return the snapshot nearest to or before the requested date. <!-- SPEC-BINDING: NFR-2.2 = SNAPSHOT_VERSIONING_STRATEGY --> | [Source: NFR-002, DL-017] |
| NFR-2.3 | The data layer SHALL handle partial profiles without errors. Missing fields SHALL be treated as "not yet provided" — the engine SHALL request them when needed for a specific calculation. | [Source: Grill — profile_scope] |

### NFR-3: Reliability

| ID | Requirement | Source |
|----|-------------|--------|
| NFR-3.1 | The system SHALL refuse to provide financial advice when required input data is missing or insufficient for the requested analysis. It SHALL explicitly ask the user for the missing information. <!-- SPEC-BINDING: NFR-3.1 = STRICT_DATA_VALIDATION --> | [Source: Grill — error_handling] |
| NFR-3.2 | The system SHALL NOT make silent assumptions about financial data. If data is ambiguous or incomplete, the system SHALL request clarification rather than proceeding with assumptions. | [Source: Grill — error_handling] |
| NFR-3.3 | If the LLM service is unavailable, the system SHALL fall back to rule-engine-only results. The response SHALL indicate that AI interpretation is temporarily unavailable while providing the computed financial data. | [Source: EC-4] |
| NFR-3.4 | LLM API calls SHALL have a configurable timeout. On timeout, the system SHALL fall back to rule-engine-only response with a notice. <!-- SPEC-BINDING: NFR-3.4 = LLM_TIMEOUT_SECONDS = 30 --> | [Source: Arch Review R-1] |
| NFR-3.5 | All financial calculations SHALL use `BigDecimal` with `RoundingMode.HALF_UP` and a scale of 2 for INR amounts. This is the standard rounding mode for Indian financial calculations. Intermediate calculations MAY use higher precision; final displayed values SHALL be rounded to 2 decimal places. <!-- SPEC-BINDING: NFR-3.5 = FINANCIAL_ROUNDING_MODE = HALF_UP, FINANCIAL_SCALE = 2 --> | [Source: Arch Review R-2] |
| NFR-3.6 | Profile snapshot creation SHALL be idempotent. If the submitted profile data is identical to the current latest snapshot, no new snapshot SHALL be created. This prevents duplicate history entries from repeated saves. | [Source: Arch Review R-3] |
| NFR-3.7 | DynamoDB Local data SHALL be persisted to a Docker volume so that profile data survives container restarts. Data loss on container restart is unacceptable for financial records. | [Source: Arch Review R-4] |
| NFR-3.8 | On API startup, the system SHALL verify that Ollama is reachable and the configured LLM model is loaded. If not, the system SHALL log a warning and operate in rule-engine-only mode until the model becomes available. | [Source: Arch Review R-5] |

### NFR-4: Explainability

| ID | Requirement | Source |
|----|-------------|--------|
| NFR-4.1 | Every material recommendation SHALL explain: (a) the reasoning, (b) the assumptions used, (c) the risks of following or ignoring the recommendation, and (d) the trade-offs involved. | [Source: NFR-005] |
| NFR-4.2 | All market-dependent or law-dependent analysis SHALL state the effective date of the data or rules used. | [Source: NFR-006] |

### NFR-5: Operational Excellence

| ID | Requirement | Source |
|----|-------------|--------|
| NFR-5.1 | The application SHALL use structured JSON logging via SLF4J + Logback. Financial calculation logs SHALL include input parameters and result summaries for auditability but SHALL NOT log sensitive raw values (full account balances, income). | [Source: Arch Review O-1] |
| NFR-5.2 | The system SHALL expose Spring Boot Actuator health endpoints (`/actuator/health`) reporting the status of DynamoDB, Ollama, and the application itself. Docker Compose SHALL use these health checks for service dependency ordering. | [Source: Arch Review O-2] |
| NFR-5.3 | All API errors SHALL return a consistent JSON format: `{ "error": "ERROR_CODE", "message": "human-readable description", "details": [...], "timestamp": "ISO-8601" }`. The frontend SHALL rely on this format for error display. | [Source: Arch Review O-3] |
| NFR-5.4 | All API paths SHALL be prefixed with `/api/v1/`. This allows future breaking changes via `/api/v2/` without disrupting existing clients. <!-- SPEC-BINDING: NFR-5.4 = API_VERSION_PREFIX = /api/v1 --> | [Source: Arch Review O-4] |
| NFR-5.5 | Docker Compose SHALL use `depends_on` with health checks for startup ordering: `api` depends on `dynamodb-local` (healthy) and `ollama` (started). `frontend` depends on `api` (healthy). | [Source: Arch Review O-5] |

### NFR-6: Data Freshness

| ID | Requirement | Source |
|----|-------------|--------|
| NFR-6.1 | All financial analysis SHALL state its effective/as-of date. The system SHALL track when each data point was last updated. | [Source: NFR-006, §9] |
| NFR-6.2 | The system SHALL detect stale data (configurable staleness threshold per data type) and flag it before generating any advice that depends on that data. Stale-data-dependent advice SHALL be blocked until the user acknowledges or refreshes the data. <!-- SPEC-BINDING: NFR-6.2 = STALENESS_THRESHOLD_DAYS --> | [Source: EC-1] |

---

## 4. Interface Definitions

### 4.1 Financial Profile Schema (DynamoDB)

The profile uses a flexible schema. Core fields are documented below; additional fields are accepted and stored without schema changes.

```json
{
  "userId": "string",
  "snapshotId": "string (ULID or ISO timestamp)",
  "snapshotDate": "2026-11-14T00:00:00Z",
  "isLatest": true,
  "personalInfo": {
    "age": 40,
    "retirementAge": null,
    "dependents": ["spouse", "child1", "child2", "mother"]
  },
  "income": {
    "monthlySalary": null,
    "otherIncome": []
  },
  "expenses": {
    "monthlyHousehold": 88000,
    "breakdown": {
      "family": 60000,
      "religious": 3000,
      "suja": 5000,
      "amma": 5000,
      "kids": 10000,
      "personal": 5000
    }
  },
  "assets": {
    "emergencyFD": { "value": 900000, "goalTag": "emergency", "locked": true },
    "epf": { "value": 500000, "goalTag": "retirement" },
    "mutualFunds": { "value": 344698, "goalTag": null },
    "indianStocks": { "value": 159611, "goalTag": null },
    "land": { "value": 500000, "goalTag": null, "liquid": false },
    "gold": { "quantity": "100g", "purity": "22K", "value": null, "goalTag": null, "liquid": false },
    "apartment": { "purchasePrice": 20000000, "paidPercent": 5, "goalTag": null, "liquid": false }
  },
  "liabilities": {
    "homeLoan": { "principal": 18000000, "status": "expected", "emiStartDate": null },
    "creditCard": { "monthlySpend": 25000, "paidInFull": true, "isDebt": false }
  },
  "insurance": {
    "termLife": { "user": { "cover": 35000000, "premium": 11000, "premiumFrequency": "monthly", "remainingYears": 10 } },
    "termLifeSpouse": { "cover": 10000000, "premium": 3000, "premiumFrequency": "monthly", "remainingYears": 10 },
    "healthFamily": { "cover": 1000000, "premium": 12000, "premiumFrequency": "yearly" },
    "healthParents": { "cover": 1000000, "premium": 40000, "premiumFrequency": "yearly" }
  },
  "goals": [
    { "id": "retirement", "name": "Retirement", "targetDate": null, "targetAmountToday": null, "inflationRate": null },
    { "id": "education", "name": "Children Education", "targetDate": null, "targetAmountToday": null, "inflationRate": null },
    { "id": "furnishing", "name": "Apartment Furnishing", "targetDate": null, "targetAmountToday": 2000000, "inflationRate": null }
  ],
  "riskProfile": {
    "behaviouralTolerance": "growth-oriented",
    "notes": "Likely to continue SIPs and add investment during major market falls"
  },
  "planningPreference": "balance-growth-and-certainty",
  "metadata": {
    "createdAt": "2026-11-14T00:00:00Z",
    "updatedAt": "2026-11-14T00:00:00Z",
    "source": "manual-entry"
  }
}
```

**Notes:**
- Fields with `null` values are "not yet provided" — the engine will request them when needed.
- `goalTag` links assets to specific goals. Untagged assets are flagged for assignment.
- `locked: true` on emergency FD prevents automatic reallocation (FR-6.4).
- Additional fields added by the user are preserved in the flexible schema.

### 4.2 API Endpoints (Spring Boot)

All paths use the `/api/v1/` prefix per NFR-5.4.

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/auth/login` | Authenticate, return JWT |
| POST | `/api/v1/auth/refresh` | Refresh JWT token |
| GET | `/api/v1/profile` | Get current profile snapshot |
| GET | `/api/v1/profile/history` | Get all profile snapshots |
| PUT | `/api/v1/profile` | Update profile (creates new snapshot; idempotent per NFR-3.6) |
| GET | `/api/v1/health` | Financial health assessment |
| GET | `/api/v1/health/dashboard` | Dashboard data (all 10 dimensions) |
| GET | `/api/v1/goals` | List all goals with funding status |
| POST | `/api/v1/goals` | Add/update a goal |
| POST | `/api/v1/stress-test` | Run a stress scenario |
| POST | `/api/v1/scenario/compare` | Compare alternative decisions |
| POST | `/api/v1/chat` | Conversational advisory (Spring AI) |
| GET | `/api/v1/review` | Periodic review / drift detection |
| GET | `/actuator/health` | Spring Boot Actuator health check (DynamoDB, Ollama, app status) |

### 4.3 Dashboard Response Schema

```json
{
  "asOfDate": "2026-11-14",
  "dimensions": {
    "liquidity": { "status": "healthy|warning|critical", "value": "...", "trend": "up|flat|down", "detail": "..." },
    "insurance": { "status": "...", "coverageRatio": "...", "gaps": [] },
    "debtExposure": { "status": "...", "debtToIncomeRatio": "...", "detail": "..." },
    "retirementTrajectory": { "status": "...", "fundedPercent": "...", "shortfall": "..." },
    "educationTrajectory": { "status": "...", "fundedPercent": "...", "shortfall": "..." },
    "portfolioRisk": { "status": "...", "concentrationFlags": [], "detail": "..." },
    "incomeConcentration": { "status": "...", "singleSourceRisk": true, "detail": "..." },
    "emergencyCoverage": { "status": "...", "monthsCovered": "...", "threshold": "..." },
    "housingCommitment": { "status": "...", "emiToIncomeRatio": "...", "detail": "..." },
    "warnings": [{ "severity": "high|medium|low", "message": "...", "action": "..." }]
  },
  "overallScore": null
}
```

---

### 4.4 Standard Error Response Format (NFR-5.3)

```json
{
  "error": "INSUFFICIENT_DATA",
  "message": "Cannot calculate emergency coverage: monthly income is not provided.",
  "details": [
    { "field": "income.monthlySalary", "reason": "required for emergency coverage calculation" }
  ],
  "timestamp": "2026-11-14T18:30:00Z"
}
```

Common error codes: `INSUFFICIENT_DATA`, `STALE_DATA`, `UNAUTHORIZED`, `VALIDATION_ERROR`, `LLM_UNAVAILABLE`, `CALCULATION_ERROR`.

---

## 5. Architecture Guidance

These architectural decisions were validated during the Phase 3 architecture review. They guide implementation without over-engineering for the project's single-user, local-Docker scale.

### 5.1 Application Structure — Monolith (Not Microservices)

The application SHALL be a single Spring Boot monolith. Separate microservices for individual engines (health, goals, risk, stress) are explicitly rejected — they share the same profile data, same database, and same JVM. The cost of inter-service communication, Docker complexity, and operational overhead is not justified for a single-user application.

### 5.2 Key Abstractions (KISS-filtered)

| Abstraction | Justification | Pattern |
|-------------|--------------|---------|
| **Financial math utility** | Compounding, present value, future value, annuity formulas are reused across 4+ engines. Single source of truth prevents rounding inconsistencies. | Static utility class with `BigDecimal` methods |
| **Profile service** | Centralises DynamoDB CRUD, snapshot versioning, and field-level encryption. Used by every engine. | Spring `@Service` with injected DynamoDB client |
| **Risk evaluator composite** | 5 risk dimensions with independent logic, composed by "safer constraint wins" rule. Each evaluator is independently testable. | Sealed interface `RiskDimension` + composite `RiskAssessor` |
| **Specialist delegation** | 3 specialist tools invoked via LLM function calling. Use Spring AI's `@Description` annotated functions — no custom plugin architecture needed. | Spring AI function calling |

### 5.3 Rejected Patterns

| Pattern | Reason for Rejection |
|---------|---------------------|
| Microservices | Over-engineering for single-user monolith (C-5) |
| Event-driven profile updates | Single user, synchronous workflows — event bus adds complexity without benefit (C-6) |
| Abstract factory for engines | One implementation per engine — factory pattern is YAGNI (C-7) |

---

## 6. Edge Cases

| ID | Scenario | Expected Behaviour | Source |
|----|----------|-------------------|--------|
| EC-1 | **Stale market data** — stock/MF prices or NAV are outdated beyond the configured staleness threshold. | The system SHALL flag which data is stale, state the last-known date, and refuse to generate advice that depends on the stale data until the user acknowledges or refreshes. | [Source: §9, NFR-5.2] |
| EC-2 | **Conflicting goals** — available monthly surplus cannot fund all goals simultaneously. | The system SHALL quantify the shortfall per goal, show the impact of different allocation choices, and present trade-offs explicitly. It SHALL NOT silently prioritise one goal over another. | [Source: §5, DL-006] |
| EC-3 | **Emergency fund breach** — a recommendation or user action would reduce emergency reserves below the safe threshold (configurable, default = 6 months of mandatory expenses). | The system SHALL block the recommendation and explain why. It SHALL show the projected emergency coverage after the action and the minimum safe threshold. | [Source: DL-011, DL-014] |
| EC-4 | **LLM hallucination** — the LLM generates a response referencing financial products, return rates, tax rules, or calculation results that are not verified. | The rule engine SHALL validate all LLM financial assertions against profile data, known product databases, and calculation outputs. Unverifiable claims SHALL be stripped or flagged. If the LLM is unavailable, the system falls back to rule-engine-only output. | [Source: Grill — error_handling] |
| EC-5 | **Large life event** — job loss, medical emergency, major market crash, or other event that invalidates current plan assumptions. | The system SHALL trigger a full plan recalculation, flag every assumption that has changed, and present the revised financial health dashboard with before/after comparison. | [Source: Grill — edge_cases] |
| EC-6 | **Double-counting** — the same rupee is allocated to both emergency reserve and investable capital, or to two different goals simultaneously. | The system SHALL enforce mutual exclusivity of rupee allocation. Each rupee has exactly one purpose/goal tag. The system SHALL reject or warn on any operation that would double-count. | [Source: §5] |

---

## 7. Technology Stack

| Component | Choice | Version | Rationale |
|-----------|--------|---------|-----------|
| Backend language | Java | 25 LTS | Latest LTS; BigDecimal for financial precision, virtual threads for concurrent LLM calls, records for data models. [Source: Grill — stack] |
| Backend framework | Spring Boot | Latest stable (prefer 4.x if Spring AI compatible) | Production-grade, Spring AI integration, DynamoDB support via AWS SDK v2. [Source: Grill — stack] |
| AI/LLM framework | Spring AI | Latest stable | Chat client, function calling for specialist delegation, RAG, structured output. [Source: Grill — stack] |
| Frontend | React + Vite + TypeScript + Tailwind CSS | Latest stable | Interactive dashboard, fast build tooling, type safety, utility-first styling. [Source: Grill — react_detail] |
| Database | DynamoDB (Local via LocalStack for dev) | Latest | Flexible schema, AWS-native, snapshot versioning, migrate to cloud DynamoDB later. [Source: Grill — database] |
| LLM (local dev) | Ollama | Latest | Run LLMs locally (Llama 3, Mistral), free, private — no financial data sent externally. [Source: Grill — llm_strategy] |
| LLM (production) | Configurable (OpenAI / Anthropic / Ollama) | Latest | Cloud LLM option for better quality; configurable via Spring AI model abstraction. [Source: Grill — llm_strategy] |
| AWS emulator | LocalStack | Latest | Local AWS service emulation (DynamoDB, Secrets Manager, etc.) for dev. [Source: Grill — infra_local] |
| Build tool | Maven or Gradle | Latest | Java project build, dependency management. |
| Containerisation | Docker + Docker Compose | Latest | Full-stack local development environment. [Source: Grill — infra_local] |

---

## 8. Acceptance Criteria

### AC-1: Profile Management

| ID | Criteria |
|----|----------|
| AC-1.1 | **Given** a user provides financial data, **When** the profile is saved, **Then** a new dated snapshot is created in DynamoDB and the previous snapshot remains queryable. |
| AC-1.2 | **Given** a profile with missing fields, **When** the user requests a calculation that requires those fields, **Then** the system asks for the specific missing data instead of failing silently or assuming values. |
| AC-1.3 | **Given** multiple profile snapshots exist, **When** the user queries history, **Then** all snapshots are returned in chronological order with their effective dates. |

### AC-2: Financial Health

| ID | Criteria |
|----|----------|
| AC-2.1 | **Given** the user's profile with assets and liabilities, **When** net worth is calculated, **Then** the result separates liquid and illiquid assets, and the total equals assets minus liabilities. |
| AC-2.2 | **Given** the user's income and expense data, **When** cash-flow health is calculated, **Then** the monthly surplus equals income minus (expenses + EMIs + insurance + investments). |
| AC-2.3 | **Given** emergency assets of ₹9,00,000 and monthly expenses of ₹88,000, **When** emergency coverage is calculated, **Then** the result is approximately 10.2 months. |
| AC-2.4 | **Given** a future home loan EMI starts, **When** emergency coverage is recalculated, **Then** the coverage ratio decreases to reflect the higher mandatory outflow. |

### AC-3: Goal Planning

| ID | Criteria |
|----|----------|
| AC-3.1 | **Given** a goal with today's value and target date, **When** future value is calculated, **Then** the result applies the specified inflation rate compounded to the target date. |
| AC-3.2 | **Given** two goals competing for the same surplus, **When** allocation is requested, **Then** the system shows the shortfall for each goal under different allocation scenarios. |
| AC-3.3 | **Given** the emergency FD is tagged as "emergency" with locked=true, **When** the system calculates investable capital, **Then** the FD amount is excluded. |

### AC-4: Risk Engine

| ID | Criteria |
|----|----------|
| AC-4.1 | **Given** a user with growth-oriented tolerance but a furnishing goal in 2 years, **When** risk is assessed for that goal, **Then** the system recommends low-risk allocation despite the user's overall tolerance. |
| AC-4.2 | **Given** a material change in income or liabilities, **When** risk is re-evaluated, **Then** the risk capacity dimension reflects the change, potentially altering recommendations. |

### AC-5: Stress Testing

| ID | Criteria |
|----|----------|
| AC-5.1 | **Given** a stress scenario of 40% equity market fall, **When** the test runs, **Then** the system shows revised portfolio value, impacted goal funding percentages, and which goals are now at risk. |
| AC-5.2 | **Given** a stress scenario of 6-month income loss, **When** the test runs, **Then** the system shows how many months the emergency fund covers and whether EMI obligations can be met. |

### AC-6: Dashboard

| ID | Criteria |
|----|----------|
| AC-6.1 | **Given** the user's complete profile, **When** the dashboard loads, **Then** all 10 health dimensions are displayed with status (healthy/warning/critical), current value, and trend. |
| AC-6.2 | **Given** a critical issue in one dimension (e.g., emergency coverage < 3 months), **When** an overall score is shown, **Then** the critical dimension remains visually prominent and is not hidden by the aggregate score. |

### AC-7: AI Advisory

| ID | Criteria |
|----|----------|
| AC-7.1 | **Given** the user asks a financial question, **When** the LLM generates advice, **Then** all numerical claims in the response are verified against rule engine calculations. |
| AC-7.2 | **Given** the LLM service is down, **When** the user requests advice, **Then** the system provides rule-engine-computed data with a notice that AI interpretation is temporarily unavailable. |
| AC-7.3 | **Given** incomplete profile data, **When** the user asks for advice that requires the missing data, **Then** the system refuses to advise and lists the specific fields needed. |

### AC-8: Security

| ID | Criteria |
|----|----------|
| AC-8.1 | **Given** a valid username/password, **When** the user logs in, **Then** a JWT is issued with an appropriate expiry time. |
| AC-8.2 | **Given** an API request without a valid JWT, **When** the request is processed, **Then** it is rejected with 401 Unauthorized. |
| AC-8.3 | **Given** sensitive financial data is stored, **When** the data is written to DynamoDB, **Then** field-level encryption is applied to balance, income, and asset-value fields. |

---

## 9. Test Plan

### Unit Tests (No infrastructure dependency)

| ID | Test | Validates | FR/NFR |
|----|------|-----------|--------|
| UT-01 | Net worth calculation with mixed liquid/illiquid assets | Correct sum and breakdown | FR-2.1 |
| UT-02 | Cash-flow surplus calculation | Income - expenses - EMIs - insurance - investments | FR-2.2 |
| UT-03 | Emergency coverage ratio | Emergency assets ÷ monthly mandatory expenses | FR-2.3 |
| UT-04 | Emergency coverage recalculation after EMI starts | Ratio decreases with higher outflows | FR-2.3, DL-014 |
| UT-05 | Goal future value with inflation | Compounding at specified rate to target date | FR-3.1 |
| UT-06 | Goal shortfall calculation | Required corpus minus (current + projected contributions) | FR-3.3 |
| UT-07 | Double-counting prevention | Same rupee cannot be tagged to two goals | FR-3.7, EC-6 |
| UT-08 | Risk assessment: near-term goal override | Short-horizon goal gets low-risk despite high tolerance | FR-4.3, DL-009 |
| UT-09 | Risk assessment: safer constraint wins | When tolerance says "high" but capacity says "low", capacity wins | FR-4.3 |
| UT-10 | Credit card: paid-in-full not counted as debt | Card with full payment = spending mechanism | FR-5.3, DL-012 |
| UT-11 | Credit card: revolving balance counted as debt | Card with unpaid balance = liability | FR-5.3 |
| UT-12 | Emergency fund exclusion from investable capital | FD with locked=true excluded from investment pool | FR-6.4, DL-011 |
| UT-13 | Stress test: market fall impact on goals | Goal funding percentage decreases proportionally | FR-7.2, FR-7.5 |
| UT-14 | Stress test: income loss impact on emergency coverage | Coverage months recalculated under stress | FR-7.1, FR-7.5 |
| UT-15 | Scenario comparison: SIP increase vs mortgage prepay | Both paths show different goal/debt outcomes | FR-8.1 |
| UT-16 | Missing field handling — engine requests data | Calculation refuses gracefully on missing input | NFR-3.1 |
| UT-17 | Snapshot versioning: new snapshot preserves old | After update, both old and new snapshots exist | FR-1.3, NFR-2.2 |
| UT-18 | Stale data detection | Data beyond staleness threshold is flagged | NFR-5.2, EC-1 |
| UT-19 | LLM output validation — reject unverified claims | Rule engine strips/flags unverifiable financial assertions | FR-12.4, EC-4 |
| UT-20 | JWT token generation and validation | Valid credentials produce valid token; invalid credentials are rejected | NFR-1.1 |
| UT-21 | Field-level encryption round-trip | Encrypted field is recoverable with correct key | NFR-1.2 |
| UT-22 | Password hashing — bcrypt/Argon2id | Password is hashed, plaintext never stored | NFR-1.5 |
| UT-23 | Input validation — negative financial amount rejected | Negative BigDecimal values rejected with error | NFR-1.7 |
| UT-24 | Financial rounding — HALF_UP with scale 2 | All INR calculations round correctly | NFR-3.5 |
| UT-25 | Idempotent snapshot — identical data no new snapshot | Duplicate save does not create new snapshot | NFR-3.6 |
| UT-26 | LLM timeout triggers fallback | Mock LLM delay > timeout → rule-engine-only response | NFR-3.4 |
| UT-27 | Standard error response format | All error responses match the specified JSON schema | NFR-5.3 |
| UT-28 | Prompt injection protection | Adversarial chat input does not override system prompt persona | NFR-1.6 |

### Integration Tests (With LocalStack / DynamoDB Local / Ollama)

| ID | Test | Validates | FR/NFR |
|----|------|-----------|--------|
| IT-01 | Profile CRUD with DynamoDB | Create, read, update, snapshot versioning | FR-1.1, FR-1.3 |
| IT-02 | Dashboard endpoint returns all 10 dimensions | Full health assessment pipeline | FR-10.1, FR-10.2 |
| IT-03 | Chat endpoint with Ollama — financial question | LLM response validated by rule engine | FR-12.3, FR-12.4 |
| IT-04 | Chat endpoint with Ollama down — fallback | Rule-engine-only response returned | NFR-3.3 |
| IT-05 | Stress test endpoint — full scenario execution | Input scenario → computed impact on goals | FR-7.1 to FR-7.5 |
| IT-06 | Auth flow — login, access protected endpoint, token refresh | JWT lifecycle works end to end | NFR-1.1, AC-8.1 |
| IT-07 | Sensitive data encryption in DynamoDB | Field-level encryption verified at storage level | NFR-1.2 |
| IT-08 | Profile history query — multiple snapshots | Returns chronological snapshots | FR-1.3 |
| IT-09 | Actuator health endpoint — all dependencies | Reports DynamoDB and Ollama status correctly | NFR-5.2 |
| IT-10 | Docker Compose full startup — all services healthy | `docker-compose up` brings all services online with correct ordering | NFR-5.5, IR-1 |
| IT-11 | CORS — frontend origin allowed, others blocked | API accepts requests from Vite dev server, rejects other origins | NFR-1.8 |
| IT-12 | DynamoDB data persists across container restart | Stop and restart containers; profile data still present | NFR-3.7 |

### Test Data

- **Golden file set:** Pre-calculated expected results for the user baseline profile (Section 6 of requirements). Covers net worth, cash flow, emergency coverage, goal projections.
- **Progression to cloud:** Add spreadsheet verification (Excel cross-check) and property-based testing (mathematical invariants) when moving to higher environments.

---

## 10. Infrastructure

### 9.1 Docker Compose (Local Development)

| Service | Image | Purpose |
|---------|-------|---------|
| `api` | Custom (Dockerfile) | Spring Boot backend |
| `frontend` | Custom (Dockerfile) | React dev server (Vite) |
| `dynamodb-local` | `amazon/dynamodb-local` | DynamoDB emulation |
| `localstack` | `localstack/localstack` | AWS service emulation (Secrets Manager, etc.) |
| `ollama` | `ollama/ollama` | Local LLM inference |

### 9.2 Infrastructure Requirements

| ID | Requirement | Source |
|----|-------------|--------|
| IR-1 | Docker Compose SHALL bring up all services with a single `docker-compose up` command. | [Source: Grill — infra_local] |
| IR-2 | DynamoDB Local SHALL be seeded with the profile table schema on startup. | [Source: FR-1.1] |
| IR-3 | Ollama SHALL pull a default model (e.g., Llama 3.1 8B) on first startup. | [Source: Grill — llm_strategy] |
| IR-4 | Environment variables SHALL be configurable via `.env` file for: LLM model, DynamoDB endpoint, encryption keys. | [Source: NFR-1.2] |
| IR-5 | CI/CD: Local builds initially (Maven/Gradle + npm). GitHub Actions to be added when moving to cloud. | [Source: Grill — cicd] |

### 9.3 Config Variables

| Variable | Default | Owner | Source |
|----------|---------|-------|--------|
| `DYNAMODB_ENDPOINT` | `http://dynamodb-local:8000` | Infra | IR-2 |
| `OLLAMA_BASE_URL` | `http://ollama:11434` | Infra | IR-3 |
| `LLM_MODEL` | `llama3.1:8b` | App | IR-3 |
| `LLM_PROVIDER` | `ollama` | App | Grill — llm_strategy |
| `LLM_TIMEOUT_SECONDS` | `30` | App | NFR-3.4 |
| `JWT_SECRET` | (generated) | Security | NFR-1.1, NFR-1.9 |
| `JWT_EXPIRY_MINUTES` | `60` | Security | NFR-1.1 |
| `ENCRYPTION_KEY` | (generated) | Security | NFR-1.2 |
| `PASSWORD_HASH_ALGORITHM` | `bcrypt` | Security | NFR-1.5 |
| `BCRYPT_COST_FACTOR` | `12` | Security | NFR-1.5 |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | Security | NFR-1.8 |
| `STALENESS_THRESHOLD_DAYS` | `7` | App | NFR-6.2 |
| `EMERGENCY_MONTHS_THRESHOLD` | `6` | App | EC-3 |
| `API_VERSION_PREFIX` | `/api/v1` | App | NFR-5.4 |
| `FINANCIAL_ROUNDING_MODE` | `HALF_UP` | App | NFR-3.5 |
| `FINANCIAL_SCALE` | `2` | App | NFR-3.5 |

---

## 11. User Profile Baseline (V1 Planning Reference)

These values are the initial V1 planning baseline from the requirements document. They should be stored with an effective date (2026-11-14) so future values can replace them without losing history.

| Profile Item | Current Baseline |
|-------------|-----------------|
| Age | 40 years |
| Monthly household expenditure | ₹88,000 (Family ₹60K + Religious ₹3K + Suja ₹5K + Amma ₹5K + Kids ₹10K + Personal ₹5K) |
| Emergency reserve / FD | ₹9,00,000 |
| EPF | ₹5,00,000 |
| Mutual funds | ₹3,44,698 |
| Indian stocks | ₹1,59,611 |
| Land | ≈₹5,00,000 |
| Gold | ≈100g of 22K gold |
| Term insurance (user) | ₹3.5 crore cover; ≈₹11,000/month premium for next 10 years |
| Term insurance (spouse) | ₹1 crore cover; ≈₹3,000/month premium for next 10 years |
| Health insurance (family) | ₹10 lakh cover; ≈₹12,000/year premium |
| Health insurance (parents) | ₹10 lakh cover; ≈₹40,000/year premium |
| Credit card | ≈₹25,000/month, paid in full |
| Apartment | ≈₹2 crore purchase; 5% paid, 5% pending, ≈90% mortgage |
| Home loan (expected) | ≈₹1.8 crore |
| Future furnishing | ≈₹20 lakh around possession |
| Behavioural risk | Growth-oriented; likely to continue SIPs and add during major falls |
| Planning preference | Balance expected wealth growth and probability of goals |

---

## 12. Out of Scope for V1

| Item | Rationale |
|------|-----------|
| Automated trade or investment execution | Advisory-only in V1 (FR-003, DL-003) |
| Multi-user household accounts or public use | Single-user private product (FR-001, DL-001) |
| Global/cross-border financial planning | India-first; global deferred to Phase 4 (DL-002) |
| Financial-independence / early-retirement optimisation | Deferred; retirement remains a long-term goal (§6) |
| Real-time integrations with banks, Groww, Zerodha, MF Central | Deferred to Phase 2 data integrations (DL-006) |
| Final UI/visual design of the dashboard | V1 focuses on functional correctness; visual polish is iterative |
| Risk model numerical calibration | Qualitative framework in V1; numerical thresholds deferred (OD-003) |
| Goal optimisation method (Monte Carlo vs deterministic) | Deferred to design phase (OD-004) |

---

## 13. Open Design Decisions (Carried from Decision Log)

These items were explicitly deferred during discovery and will be resolved during the design phase (Phase 2: /spec-to-design-doc) or later.

| ID | Topic | What Remains |
|----|-------|-------------|
| OD-001 | Persistent storage technology | DynamoDB Local locked for dev. Cloud hosting decision deferred. |
| OD-002 | Dashboard visual design | Dimensions defined; exact widgets, charts, and layout deferred to frontend build. |
| OD-003 | Risk model calibration | Qualitative framework locked. Numerical thresholds and scoring to be designed. |
| OD-004 | Goal optimisation method | Deterministic scenarios for V1. Probability simulation may be added later. |
| OD-005 | Education goal assumptions | Continue current targets; refine by college type, inflation, timing. |
| OD-006 | Future external data providers | Deferred to Phase 2 roadmap. |
| OD-007 | Global phase | Deferred to Phase 4 roadmap. |

---

## 14. Phase Roadmap

| Phase | Scope |
|-------|-------|
| **Phase 1 — India V1** | Private holistic adviser, persistent profile, goal/risk/cash-flow engines, stress testing, dashboard, specialist handoffs, AI advisory layer, Docker Desktop. |
| **Phase 2 — Data integrations** | Connect investment/banking providers (Groww, Zerodha, MF Central). Automate profile refresh with secure authorisation. |
| **Phase 3 — Dedicated application** | Cloud deployment. Reuse same data model and decision engine. Richer dashboards, scenario tools, document storage. |
| **Phase 4 — Global planning** | Multi-country residency, taxation, foreign assets, FX, cross-border retirement, international investment planning. |

---

## 15. Decision Log Reference

All 20 finalized decisions (DL-001 through DL-020) from the Discovery Decision Log are incorporated into this specification. Key decision-to-requirement mappings:

| Decision | Key Requirement Impact |
|----------|----------------------|
| DL-001 Single user | FR-1.4 |
| DL-002 India-first | FR-1.5 |
| DL-003 Advisory-only | Out of Scope §11 |
| DL-004 Persistent profile | FR-1.1, NFR-2.1 |
| DL-005 Opinionated adviser | FR-12.1, FR-12.2 |
| DL-006 No goal priority labels | FR-3.5 |
| DL-007 Dynamic risk | FR-4.1, FR-4.4 |
| DL-008 Balance growth and certainty | FR-8.2 |
| DL-009 Goal-specific risk | FR-3.6, FR-4.3 |
| DL-010 Retirement glide-path | FR-6.3 |
| DL-011 Emergency fund protected | FR-6.4 |
| DL-012 Credit card not debt | FR-5.3 |
| DL-013 Housing as liability | FR-6.5 |
| DL-014 Dynamic emergency coverage | FR-2.3 |
| DL-015 Adviser owns strategy | FR-11.4 |
| DL-016 Specialist data freshness | FR-11.5, NFR-5.1 |
| DL-017 Version financial facts | FR-1.3, NFR-2.2 |
| DL-018 No sensitive identifiers | NFR-1.3 |
| DL-019 Multidimensional dashboard | FR-10.1 |
| DL-020 Preserve for future app | FR-006 |

---

## Spec Change Log

| Date | NFR/FR | Old Value | New Value | Reason | Changed By |
|------|--------|-----------|-----------|--------|------------|
| 2026-11-14 | NFR-1.5 to NFR-1.9 | (none) | Added | Architecture review — security gaps (S-1 to S-5) | Agent |
| 2026-11-14 | NFR-3.4 to NFR-3.8 | (none) | Added | Architecture review — reliability gaps (R-1 to R-5) | Agent |
| 2026-11-14 | NFR-5.1 to NFR-5.5 | (none) | Added | Architecture review — operational excellence (O-1 to O-5) | Agent |
| 2026-11-14 | NFR-5→NFR-6 | Data Freshness was NFR-5 | Renumbered to NFR-6 | Operational Excellence inserted as NFR-5 | Agent |
| 2026-11-14 | API paths | `/api/...` | `/api/v1/...` | API versioning per NFR-5.4 | Agent |
| 2026-11-14 | Section 5 | (none) | Architecture Guidance added | Arch review: monolith, key abstractions, rejected patterns | Agent |
| 2026-11-14 | UT-22 to UT-28 | (none) | Added | Tests for architecture review findings | Agent |
| 2026-11-14 | IT-09 to IT-12 | (none) | Added | Integration tests for arch review findings | Agent |
| 2026-11-14 | Spec version | 1.0 | 2.0 | Architecture review applied | Agent |

---

## Development Phases

| Phase | Description | Spec References | Dependencies |
|-------|-------------|-----------------|-------------|
| **DP-1: Foundation** | Project scaffold, Docker Compose (with health checks, volume persistence, startup ordering), DynamoDB table setup, JWT auth (bcrypt password hashing), CORS, API versioning, structured logging, Actuator health endpoints, standard error response format | IR-1 to IR-5, NFR-1.1, NFR-1.5, NFR-1.8, NFR-1.9, NFR-2.1, NFR-5.1 to NFR-5.5 | None |
| **DP-2: Profile Engine** | Profile CRUD, snapshot versioning (idempotent), flexible schema, field-level encryption, input validation | FR-1.x, NFR-1.2, NFR-1.7, NFR-2.2, NFR-3.6 | DP-1 |
| **DP-3: Financial Calculations** | Net worth, cash flow, emergency coverage, goal projections, risk engine. All using BigDecimal with HALF_UP rounding. Financial math utility library. | FR-2.x, FR-3.x, FR-4.x, FR-5.x, NFR-3.5 | DP-2 |
| **DP-4: Stress & Scenario** | Stress testing engine, scenario comparison, trade-off analysis | FR-7.x, FR-8.x | DP-3 |
| **DP-5: AI Advisory Layer** | Spring AI integration, Ollama setup (with availability check), chat interface (with prompt injection protection), LLM validation, LLM timeout + fallback, specialist delegation via function calling | FR-11.x, FR-12.x, NFR-1.6, NFR-3.3, NFR-3.4, NFR-3.8 | DP-3 |
| **DP-6: Dashboard & Frontend** | React dashboard, 10 dimensions, drill-down, trend visualization | FR-10.x | DP-3, DP-4 |
| **DP-7: Integration & Polish** | End-to-end testing, golden-file validation, periodic review, drift detection, Docker Compose full-stack validation | FR-9.x, Test Plan (UT-01 to UT-28, IT-01 to IT-12) | DP-1 to DP-6 |
