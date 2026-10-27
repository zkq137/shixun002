import { get, post } from './request'

export interface AssistantStatus {
  configured: boolean
  baseUrl?: string | null
}

export interface AssistantReply {
  answer: string
  conversationId?: string | null
  messageId?: string | null
  createdAt?: number | null
}

/** 查询 Dify 是否已配置 */
export function fetchAssistantStatus(endpoint = '/ai/status') {
  return get<AssistantStatus>(endpoint)
}

/** 发送一条消息给 Dify 工作流 */
export function sendAssistantMessage(body: {
  query: string
  conversationId?: string
  user?: string
}, endpoint = '/ai/chat') {
  // 智能体可能串联 LLM 和多个后端请求，单独放宽等待时间。
  return post<AssistantReply>(endpoint, body, { timeout: 120_000 })
}
