import apiClient from './client';
import type { ChatResponse } from '@/types/api';

export async function sendMessage(message: string): Promise<ChatResponse> {
  const { data } = await apiClient.post<ChatResponse>('/chat', { message });
  return data;
}
