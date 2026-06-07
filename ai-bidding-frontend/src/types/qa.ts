export interface Source {
  title: string
  relevance: string
}

export interface QaResponse {
  answer: string
  intent: string
  sources: Source[]
  tokensUsed: number
  latencyMs: number
}

export interface QaRequest {
  question: string
}

export interface ChatMessage {
  id: string
  role: 'user' | 'assistant'
  content: string
  intent?: string
  sources?: Source[]
  tokensUsed?: number
  latencyMs?: number
  timestamp: number
}
