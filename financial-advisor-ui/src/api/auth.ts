import apiClient from './client';
import type { AuthResponse } from '@/types/api';
import axios from 'axios';

export async function login(username: string, password: string): Promise<AuthResponse> {
  try {
    const { data } = await apiClient.post<AuthResponse>('/auth/login', { username, password });
    localStorage.setItem('accessToken', data.accessToken);
    localStorage.setItem('refreshToken', data.refreshToken);
    return data;
  } catch (err) {
    if (axios.isAxiosError(err) && err.response?.data?.message) {
      throw new Error(err.response.data.message);
    }
    throw err;
  }
}

export function logout(): void {
  localStorage.removeItem('accessToken');
  localStorage.removeItem('refreshToken');
}

export function isAuthenticated(): boolean {
  return !!localStorage.getItem('accessToken');
}
