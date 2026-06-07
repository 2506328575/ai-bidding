import axios from 'axios'
import type { QaRequest, QaResponse } from '../types/qa'

const apiClient = axios.create({
  baseURL: '/api/v1',
  timeout: 30000,
  headers: { 'Content-Type': 'application/json' }
})

export async function askQuestion(request: QaRequest): Promise<QaResponse> {
  const { data } = await apiClient.post<QaResponse>('/qa/ask', request)
  return data
}
