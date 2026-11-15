/** TypeScript types matching the Spring Boot API response contracts */

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresInMinutes: number;
}

export interface ErrorResponse {
  error: string;
  message: string;
  details?: FieldDetail[];
  timestamp: string;
}

export interface FieldDetail {
  field: string;
  reason: string;
}

/** Dashboard — 10 dimension health view (FR-10.x) */
export interface DashboardResponse {
  userId: string;
  snapshotId: string;
  snapshotDate: string;
  dimensions: DimensionStatus[];
  summary: FinancialSummary;
  timestamp: string;
}

export interface DimensionStatus {
  index: number;
  name: string;
  status: string;
  description: string;
  actions: string[];
}

export interface FinancialSummary {
  netWorth: number;
  liquidAssets: number;
  marketLinkedAssets: number;
  restrictedAssets: number;
  illiquidAssets: number;
  totalLiabilities: number;
  monthlySurplus: number;
  emergencyCoverageMonths: number;
  riskLevel: string;
  goalCount: number;
  goalsOnTrack: number;
  goalsAtRisk: number;
}

/** Chat advisory (FR-12.5) */
export interface ChatResponse {
  message: string;
  source: 'ai' | 'rule-engine' | 'guard';
  aiAvailable: boolean;
  validationWarnings?: string[];
  engineData?: Record<string, unknown>;
  timestamp: string;
}

/** Profile */
export interface ProfileSnapshot {
  userId: string;
  snapshotId: string;
  snapshotDate: string;
  isLatest: boolean;
  data: Record<string, unknown>;
}

export interface ProfileHistoryResponse {
  userId: string;
  snapshotCount: number;
  snapshots: SnapshotSummary[];
}

export interface SnapshotSummary {
  snapshotId: string;
  snapshotDate: string;
  isLatest: boolean;
}

/** Stress test (FR-7.x) */
export type StressType = 'INCOME_LOSS' | 'MARKET_FALL' | 'INTEREST_RATE_CHANGE' | 'LARGE_EXPENSE';

export interface StressTestResult {
  scenario: { type: StressType; parameter: number; description: string };
  portfolioValueBefore: number;
  portfolioValueAfter: number;
  portfolioImpact: number;
  emergencyCoverageBefore: number;
  emergencyCoverageAfter: number;
  goalImpacts: GoalImpact[];
  warnings: string[];
}

export interface GoalImpact {
  goalId: string;
  goalName: string;
  fundingPercentBefore: number;
  fundingPercentAfter: number;
  wasViable: boolean;
  stillViable: boolean;
  status: string;
}

/** Scenario comparison (FR-8.x) */
export interface ScenarioComparison {
  scenarioAName: string;
  scenarioBName: string;
  scenarioA: ScenarioOutcome;
  scenarioB: ScenarioOutcome;
  recommendation: string;
  tradeoffs: string[];
}

export interface ScenarioOutcome {
  description: string;
  goalFundingPercents: Record<string, number>;
  projectedCorpus: number;
  totalInterestSaved: number;
  netWorthImpact: number;
  advantages: string[];
  disadvantages: string[];
}
