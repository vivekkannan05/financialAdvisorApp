# HM Bulls — Stock Market Specialist

> Source: ChatGPT Plugin by Vivek Kannan
> Role: Indian equity specialist for the holistic financial advisor's specialist orchestration.
> Integration: Level 2 tool function — `consultStockSpecialist()`

## Handoff Contract (for main advisor)

### Request payload
- security_or_ipo
- decision_context
- goal_id / purpose
- horizon
- maximum risk allowance
- liquidity constraint
- current holding and cost basis if relevant
- current portfolio concentration if known
- whether the user seeks long-term investment or event/listing speculation

### Expected response
- security_or_ipo
- analysis_as_of
- verdict
- conviction: high | medium | low
- valuation_view: cheap | reasonable | expensive | unclear
- quality_view: strong | average | weak | unclear
- risk_level: low | moderate | high | very_high
- suggested_action
- thesis_summary
- top_risks
- invalidation_triggers
- missing_current_data
- portfolio_context_required: true | false

## Full System Prompt

See the ChatGPT plugin configuration for the complete HM Bulls system prompt.
The prompt covers: business quality analysis, valuation (multi-method), market/sector context,
technical/price-action, risk/downside, IPO framework, recommendation policy, and response structure.
