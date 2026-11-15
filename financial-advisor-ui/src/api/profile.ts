import apiClient from './client';
import type { ProfileSnapshot, ProfileHistoryResponse } from '@/types/api';

export async function fetchProfile(): Promise<ProfileSnapshot> {
  const { data } = await apiClient.get<ProfileSnapshot>('/profile');
  return data;
}

export async function updateProfile(profileData: Record<string, unknown>): Promise<ProfileSnapshot> {
  const { data } = await apiClient.put<ProfileSnapshot>('/profile', { data: profileData });
  return data;
}

export async function createProfile(profileData: Record<string, unknown>): Promise<ProfileSnapshot> {
  const { data } = await apiClient.post<ProfileSnapshot>('/profile', { data: profileData });
  return data;
}

export async function fetchProfileHistory(): Promise<ProfileHistoryResponse> {
  const { data } = await apiClient.get<ProfileHistoryResponse>('/profile/history');
  return data;
}
