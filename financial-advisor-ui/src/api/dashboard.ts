import apiClient from './client';
import type { DashboardResponse } from '@/types/api';

export async function fetchDashboard(): Promise<DashboardResponse> {
  const { data } = await apiClient.get<DashboardResponse>('/health/dashboard');
  return data;
}
