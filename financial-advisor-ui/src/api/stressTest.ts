import apiClient from './client';
import type { StressTestResult, StressType, ScenarioComparison } from '@/types/api';

export async function runStressTest(
  type: StressType,
  parameter: number,
  durationMonths?: number
): Promise<StressTestResult> {
  const { data } = await apiClient.post<StressTestResult>('/stress-test', {
    type, parameter, durationMonths,
  });
  return data;
}

export async function compareScenarios(
  sipIncrease: number,
  prepaymentAmount: number,
  loanInterestRate: number,
  yearsRemaining: number
): Promise<ScenarioComparison> {
  const { data } = await apiClient.post<ScenarioComparison>('/scenario/compare', {
    sipIncrease, prepaymentAmount, loanInterestRate, yearsRemaining,
  });
  return data;
}
