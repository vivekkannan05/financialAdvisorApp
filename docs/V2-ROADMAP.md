# Financial Advisor — V2 Roadmap

> Items identified during V1 development that require significant architectural changes or new feature areas.

## 1. Family Profiles & Linking

**Priority:** High  
**Identified during:** Insurance "covered person" discussion  

Currently the app has a single user profile. Family members (spouse, parents, children) can only be referenced via fields like `coveredPerson` on insurance or `dependents` count.

### V2 Scope
- Separate profile per family member (spouse, parents, adult children)
- **Family Link** — connect profiles under a household unit
- Family-wide dashboard: aggregated net worth, total insurance gaps, combined goals
- Per-member risk assessment (e.g., spouse's own risk profile)
- Shared goals (e.g., "Children's Education" linked to both parents)
- Auth model: invite-based linking, permission levels (view/edit)
- Privacy: each member controls their own profile visibility

### V1 Workaround (implemented)
- `coveredPerson` dropdown on insurance (Self/Spouse/Parents/Children/Family)
- `goalTag` on assets and investments to link to specific goals
- `dependents` count in personal section


## 2. Per-Goal Risk Assessment

**Priority:** High  
**Identified during:** Risk appetite explanation  

Currently the risk engine uses the **shortest goal horizon** as the overall constraint, meaning a 1-year Emergency Fund goal forces VERY_LOW risk across ALL goals — even Retirement (20 years away).

### V2 Scope
- Each goal gets its own risk assessment based on its specific horizon
- Asset allocation recommendations per goal (e.g., Emergency Fund → 100% liquid, Retirement → equity-heavy)
- Glide-path per goal: automatic de-risking as the goal date approaches
- Goal-specific portfolio tracking (which investments fund which goal)
- Dashboard shows risk level per goal, not just one overall number

### V1 Workaround (implemented)
- `goalTag` links assets/investments to specific goals
- Overall risk = MIN across all 5 dimensions (safest wins)
- Dashboard explains the constraining factor


## 3. Advanced Liquidity Model

**Priority:** Medium  
**Identified during:** MF/EPF liquidity discussion  

V1 implements a 4-tier model (Liquid/Market-Linked/Restricted/Illiquid). V2 can refine this further.

### V2 Scope
- **Withdrawal simulation**: how much can you actually access in 24h / 7d / 30d / 90d?
- **Lock-in tracker**: EPF/PPF/ELSS maturity dates, NPS withdrawal eligibility
- **Market impact estimation**: large MF redemptions may move NAV
- **Tax-aware liquidity**: LTCG vs STCG implications of liquidating each tier
- **Tiered emergency fund**: 1-month in savings, 2-months in liquid MF, 3-months in short-term FD


## 4. Tax Planning Integration

**Priority:** High  
**Identified during:** System prompt (India financial context)  

V1 mentions tax context in the AI system prompt but has no structured tax engine.

### V2 Scope
- Old vs New tax regime comparison based on actual deductions
- Section 80C utilization tracker (PPF, ELSS, EPF, life insurance, children tuition)
- Section 80D tracker (health insurance premiums — self, parents)
- HRA exemption calculator
- Capital gains tax projection (STCG/LTCG on MF/stocks)
- Tax-loss harvesting suggestions
- Annual tax summary with optimization recommendations


## 5. Real-Time Market Data

**Priority:** Low  
**Identified during:** Stress testing & scenario engine  

V1 uses static expected return rates. V2 can integrate live data.

### V2 Scope
- Live mutual fund NAV from AMFI API
- Stock portfolio valuation from NSE/BSE feeds
- Gold price (MCX/international)
- FD/RD interest rate benchmarks from RBI data
- Portfolio XIRR calculation with actual transaction dates
- Automated portfolio rebalancing alerts


## 6. Deployment & Production Hardening

**Priority:** High (before go-live)  
**Identified during:** SDLC Phase 6 (pending)  

### V2 Scope
- AWS deployment: real DynamoDB (remove local endpointOverride), IAM roles
- Multi-environment: dev → staging → production with separate configs
- CloudWatch monitoring, alerts, dashboards
- Data encryption at rest (DynamoDB encryption, KMS for secrets)
- Rate limiting & DDoS protection
- Backup & disaster recovery for profile data
- CI/CD pipeline (GitHub Actions → ECR → ECS)
- Load testing with realistic user profiles


## 7. Mobile-Responsive & PWA

**Priority:** Medium  
**Identified during:** Dashboard UI work  

### V2 Scope
- Progressive Web App (installable on phone)
- Mobile-optimized dashboard (stacked cards, swipe navigation)
- Push notifications for goal milestones, review reminders
- Offline mode for viewing last dashboard snapshot


## 8. Evaluate Database Strategy for Cross-User Queries

**Priority:** Medium (before Family Profiles)  
**Identified during:** DB design review — SQL vs NoSQL analysis  

V1's DynamoDB single-table design is optimal for the current user-scoped access patterns (key-based lookups, snapshot versioning, flexible schema). However, V2 features — especially Family Profiles & Linking (#1) and potential admin analytics — may introduce cross-user queries that DynamoDB handles less efficiently.

### V2 Scope
- **Assess query needs**: Does family aggregation (combined net worth, total insurance gaps) require cross-partition queries?
- **Option A — Stay DynamoDB**: Add a `FAMILY#<familyId>` GSI to group linked profiles; works well for family-scoped lookups
- **Option B — Hybrid**: Keep DynamoDB for profile storage (its sweet spot), add a lightweight analytics layer (e.g., Athena, Aurora Serverless) for admin reporting and cross-user aggregations
- **Option C — Migrate to SQL**: Only if V2 adds heavy relational needs (admin dashboards, multi-user comparisons, complex reporting) — high migration cost
- **Decision criteria**: number of cross-user access patterns, query complexity, operational overhead tolerance, cost at scale

### V1 Status
- DynamoDB single-table with `PK=USER#<userId>`, `SK=SNAPSHOT#<snapshotId>`, GSI for latest snapshot
- All queries are key-based (get profile, save profile, get history) — no cross-user needs
- No ad-hoc queries, no joins, no aggregations — DynamoDB is the right fit for V1


## 9. Multi-Language Support

**Priority:** Low  
**Identified during:** India financial context  

### V2 Scope
- Hindi, Tamil, Telugu, Kannada, Malayalam, Bengali translations
- AI advisor can respond in the user's preferred language
- Number formatting per locale (though Indian system is default)


---

*Last updated: 15 November 2026*  
*Items are ordered by priority, not implementation sequence.*  
*9 items total — last added: #8 (DB strategy evaluation).*
