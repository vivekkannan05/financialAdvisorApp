# Intended executable tool contract

The skill can function without these tools, but the production plugin should expose deterministic tools with interfaces equivalent to the following.

## Profile
- `get_profile()` -> current structured profile with field freshness and provenance.
- `update_profile(patch, as_of, source)` -> validated profile update; never silently overwrite conflicting facts.
- `profile_completeness()` -> critical and noncritical missing/stale fields.
- `snapshot_profile(reason)` -> dated immutable planning snapshot.

## Financial health
- `assess_financial_health()` -> dimension-level status for liquidity, protection, debt, goals, investments, and material risks.
- `calculate_net_worth()` -> assets, liabilities, liquid net worth, investable assets, earmarked/protected assets.
- `calculate_cash_flow()` -> income, essential outgo, debt service, discretionary outgo, savings/investment capacity.
- `calculate_emergency_coverage()` -> protected liquid reserve divided by mandatory monthly outgo.

## Goals
- `analyse_goal(goal_id)` -> target corpus, projected corpus, funding ratio, shortfall/surplus, required contribution, risk envelope.
- `compare_goal_scenarios(goal_id, scenarios[])` -> contribution/date/target/return trade-offs.

## Risk
- `assess_risk()` -> tolerance, capacity, requirement, household constraints, material reasons.
- `assess_goal_risk(goal_id)` -> goal-specific maximum risk envelope.

## Stress testing
- `run_stress_test(scenario)` -> cash-flow and goal impact, first failure point, recovery requirements.
- supported examples: income loss, equity drawdown, interest-rate rise, large one-off expense, delayed property possession, lower return, SIP pause.

## Dashboard
- `get_dashboard_summary()` -> dimension statuses, alerts, goals, allocation, emergency runway, debt-service indicators and action list.

All calculation outputs should include assumptions, as-of date, and versioned methodology where practical.
