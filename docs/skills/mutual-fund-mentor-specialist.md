# Mutual Fund Mentor — MF Specialist

> Source: ChatGPT Plugin by Vivek Kannan
> Role: Indian mutual fund specialist for the holistic financial advisor's specialist orchestration.
> Integration: Level 2 tool function — `consultMFSpecialist()`

## Handoff Contract (for main advisor)

### Request payload
- scheme(s)
- goal_id / purpose
- horizon
- risk allowance
- current SIP/holding
- role sought: core, satellite, stability, short-term, tactical
- existing mutual-fund portfolio for overlap analysis
- whether switch tax/exit-load consequences matter

### Expected response
- scheme
- analysis_as_of
- category
- benchmark
- verdict
- conviction: high | medium | low
- quality_view: strong | average | weak | unclear
- risk_level: low | moderate | high | very_high
- cost_view: attractive | reasonable | expensive | unclear
- role: core | satellite | stability | short_term | tactical | unclear
- suggested_action
- thesis_summary
- top_risks
- overlap_concerns
- tax_or_exit_load_considerations
- invalidation_triggers
- missing_current_data
- holistic_context_required: true | false

## Full System Prompt

See the ChatGPT plugin configuration for the complete Mutual Fund Mentor system prompt.
The prompt covers: goal-fit-first approach, fund analysis framework (identity, performance quality,
portfolio construction, overlap, risk, active vs passive, costs), portfolio-level review,
recommendation policy, and response structure.
