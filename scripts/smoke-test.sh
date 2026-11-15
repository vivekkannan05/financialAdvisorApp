#!/usr/bin/env bash
# Full-stack smoke test for Financial Advisor (IT-10, IT-06, IT-01, IT-02).
#
# Prerequisites: API running on localhost:8080 with DynamoDB Local.
# Usage: ./scripts/smoke-test.sh
set -euo pipefail

API="http://localhost:8080/api/v1"
USER="admin"
PASS="changeme"
PASS_COUNT=0
FAIL_COUNT=0

green() { printf "\033[32m✓ %s\033[0m\n" "$1"; }
red()   { printf "\033[31m✗ %s\033[0m\n" "$1"; }
header(){ printf "\n\033[1;34m── %s ──\033[0m\n" "$1"; }

check() {
  local desc="$1" expected_status="$2" actual_status="$3"
  if [ "$actual_status" -eq "$expected_status" ]; then
    green "$desc (HTTP $actual_status)"
    PASS_COUNT=$((PASS_COUNT + 1))
  else
    red "$desc — expected $expected_status, got $actual_status"
    FAIL_COUNT=$((FAIL_COUNT + 1))
  fi
}

# ── Health check ──
header "1. Actuator Health (IT-09)"
STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/actuator/health)
check "Actuator /health reachable" 200 "$STATUS"

# ── Auth flow (IT-06) ──
header "2. Authentication (IT-06)"
LOGIN_RESP=$(curl -s -w "\n%{http_code}" -X POST "$API/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$USER\",\"password\":\"$PASS\"}")
LOGIN_STATUS=$(echo "$LOGIN_RESP" | tail -1)
LOGIN_BODY=$(echo "$LOGIN_RESP" | sed '$d')
check "POST /auth/login" 200 "$LOGIN_STATUS"

TOKEN=$(echo "$LOGIN_BODY" | python3 -c "import sys,json; print(json.load(sys.stdin)['accessToken'])" 2>/dev/null || echo "")
if [ -z "$TOKEN" ]; then
  red "Could not extract access token — remaining tests will fail"
  exit 1
fi
green "JWT token obtained"

REFRESH=$(echo "$LOGIN_BODY" | python3 -c "import sys,json; print(json.load(sys.stdin)['refreshToken'])" 2>/dev/null || echo "")

# Token refresh
REFRESH_STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$API/auth/refresh" \
  -H "Content-Type: application/json" \
  -d "{\"refreshToken\":\"$REFRESH\"}")
check "POST /auth/refresh" 200 "$REFRESH_STATUS"

# ── Protected endpoint without token (should 401) ──
header "3. Auth Protection"
UNAUTH_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$API/profile")
check "GET /profile without token returns 401" 401 "$UNAUTH_STATUS"

AUTH="-H \"Authorization: Bearer $TOKEN\""

# ── Profile CRUD (IT-01) ──
header "4. Profile CRUD (IT-01)"
CREATE_STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$API/profile" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"data":{"income":{"monthlySalary":200000},"expenses":{"monthlyHousehold":88000},"assets":{"emergencyFD":{"value":900000,"goalTag":"emergency","locked":true},"mutualFunds":{"value":344698}},"liabilities":{},"insurance":{"termLife":{"type":"term","cover":10000000,"premiumYearly":18000}},"goals":[{"id":"retirement","name":"Retirement","targetAmountToday":50000000,"targetYear":2050}],"riskProfile":{"behaviouralTolerance":"moderate","age":32,"retirementAge":60}}}')
check "POST /profile (create)" 201 "$CREATE_STATUS"

GET_STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
  -H "Authorization: Bearer $TOKEN" "$API/profile")
check "GET /profile (read)" 200 "$GET_STATUS"

# Idempotent update — same data (NFR-3.6)
UPDATE_STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X PUT "$API/profile" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"data":{"income":{"monthlySalary":200000},"expenses":{"monthlyHousehold":88000},"assets":{"emergencyFD":{"value":900000,"goalTag":"emergency","locked":true},"mutualFunds":{"value":344698}},"liabilities":{},"insurance":{"termLife":{"type":"term","cover":10000000,"premiumYearly":18000}},"goals":[{"id":"retirement","name":"Retirement","targetAmountToday":50000000,"targetYear":2050}],"riskProfile":{"behaviouralTolerance":"moderate","age":32,"retirementAge":60}}}')
check "PUT /profile (idempotent, same data = 200)" 200 "$UPDATE_STATUS"

HISTORY_STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
  -H "Authorization: Bearer $TOKEN" "$API/profile/history")
check "GET /profile/history" 200 "$HISTORY_STATUS"

# ── Dashboard (IT-02) ──
header "5. Dashboard (IT-02)"
DASH_STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
  -H "Authorization: Bearer $TOKEN" "$API/health/dashboard")
check "GET /health/dashboard" 200 "$DASH_STATUS"

# ── Chat (IT-03/04) ──
header "6. Chat Advisory (IT-03)"
CHAT_STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$API/chat" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"message":"What is my net worth?"}')
check "POST /chat" 200 "$CHAT_STATUS"

# ── Stress Test (IT-05) ──
header "7. Stress Test (IT-05)"
STRESS_STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$API/stress-test" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"type":"MARKET_FALL","parameter":0.30}')
check "POST /stress-test" 200 "$STRESS_STATUS"

# ── Scenario Compare ──
header "8. Scenario Compare"
SCENARIO_STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$API/scenario/compare" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"sipIncrease":10000,"prepaymentAmount":500000,"loanInterestRate":0.085,"yearsRemaining":20}')
check "POST /scenario/compare" 200 "$SCENARIO_STATUS"

# ── Periodic Review ──
header "9. Periodic Review (FR-9)"
REVIEW_STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
  -H "Authorization: Bearer $TOKEN" "$API/review")
check "GET /review" 200 "$REVIEW_STATUS"

# ── CORS check (IT-11) ──
header "10. CORS (IT-11)"
CORS_STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
  -H "Origin: http://localhost:5173" \
  -H "Authorization: Bearer $TOKEN" "$API/profile")
check "GET /profile with allowed origin" 200 "$CORS_STATUS"

# ── Summary ──
echo ""
echo "════════════════════════════════════"
printf "  Passed: \033[32m%d\033[0m  Failed: \033[31m%d\033[0m\n" "$PASS_COUNT" "$FAIL_COUNT"
echo "════════════════════════════════════"

[ "$FAIL_COUNT" -eq 0 ] && exit 0 || exit 1
