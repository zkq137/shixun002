import { reactive, watch } from 'vue'
import { fetchAssistantStatus, sendAssistantMessage } from '@/api/assistant'
import type { AssistantStatus } from '@/api/assistant'

export interface ChatMessage {
  role: 'user' | 'assistant' | 'system'
  content: string
  time: string
}

interface PersistedChat {
  messages: ChatMessage[]
  input: string
  conversationId: string
}

interface AssistantChatState {
  messages: ChatMessage[]
  input: string
  sending: boolean
  conversationId: string
  status: AssistantStatus | null
}

interface AssistantEndpoints {
  status: string
  chat: string
}

const STORAGE_PREFIX = 'talent-module-assistant-v1'
const LEGACY_STORAGE_KEY = 'talent-assistant-chat-v1'
const MAX_PERSISTED_MESSAGES = 80
const states = new Map<string, AssistantChatState>()
const statusLoaded = new Set<string>()

function isChatMessage(value: unknown): value is ChatMessage {
  if (typeof value !== 'object' || value === null) return false
  const message = value as Partial<ChatMessage>
  return (
    ['user', 'assistant', 'system'].includes(message.role ?? '') &&
    typeof message.content === 'string' &&
    typeof message.time === 'string'
  )
}

function storageKey(moduleKey: string) {
  return `${STORAGE_PREFIX}:${moduleKey}`
}

function restore(state: AssistantChatState, moduleKey: string) {
  try {
    const key = storageKey(moduleKey)
    const raw =
      sessionStorage.getItem(key) ??
      (moduleKey === 'employee' ? sessionStorage.getItem(LEGACY_STORAGE_KEY) : null)
    if (!raw) return
    const saved = JSON.parse(raw) as Partial<PersistedChat>
    if (Array.isArray(saved.messages)) {
      state.messages = saved.messages.filter(isChatMessage).slice(-MAX_PERSISTED_MESSAGES)
    }
    if (typeof saved.input === 'string') state.input = saved.input
    if (typeof saved.conversationId === 'string') state.conversationId = saved.conversationId
  } catch {
    sessionStorage.removeItem(storageKey(moduleKey))
  }
}

function persist(state: AssistantChatState, moduleKey: string) {
  try {
    const payload: PersistedChat = {
      messages: state.messages.slice(-MAX_PERSISTED_MESSAGES),
      input: state.input,
      conversationId: state.conversationId,
    }
    sessionStorage.setItem(storageKey(moduleKey), JSON.stringify(payload))
  } catch {
    // 当前页面内仍可继续对话。
  }
}

function stateFor(moduleKey: string) {
  const exists = states.get(moduleKey)
  if (exists) return exists

  const state = reactive<AssistantChatState>({
    messages: [],
    input: '',
    sending: false,
    conversationId: '',
    status: null,
  })
  restore(state, moduleKey)
  watch(state, () => persist(state, moduleKey), { deep: true, flush: 'sync' })
  states.set(moduleKey, state)
  return state
}

function timeNow() {
  return new Date().toLocaleTimeString('zh-CN', { hour12: false })
}

async function loadStatus(
  moduleKey: string,
  state: AssistantChatState,
  endpoints: AssistantEndpoints,
  force = false,
) {
  if (statusLoaded.has(moduleKey) && !force) return
  statusLoaded.add(moduleKey)
  try {
    state.status = await fetchAssistantStatus(endpoints.status)
  } catch {
    statusLoaded.delete(moduleKey)
    state.status = { configured: false }
  }
}

async function send(
  moduleKey: string,
  state: AssistantChatState,
  endpoints: AssistantEndpoints,
): Promise<'sent' | 'busy' | 'empty' | 'unconfigured'> {
  const text = state.input.trim()
  if (!text) return 'empty'
  if (state.sending) return 'busy'
  if (state.status && !state.status.configured) return 'unconfigured'

  state.messages.push({ role: 'user', content: text, time: timeNow() })
  state.input = ''
  state.sending = true

  try {
    const reply = await sendAssistantMessage(
      {
        query: text,
        conversationId: state.conversationId || undefined,
        user: `web-${moduleKey}`,
      },
      endpoints.chat,
    )
    state.conversationId = reply.conversationId || state.conversationId
    state.messages.push({
      role: 'assistant',
      content: reply.answer || '（Dify 没有返回内容）',
      time: timeNow(),
    })
  } catch (error) {
    const message = error instanceof Error ? error.message : '调用 AI 助手失败'
    state.messages.push({ role: 'system', content: message, time: timeNow() })
  } finally {
    state.sending = false
  }
  return 'sent'
}

function newChat(state: AssistantChatState) {
  if (state.sending) return
  state.messages = []
  state.conversationId = ''
  state.input = ''
}

export function useModuleAssistant(
  moduleKey: string,
  endpoints: AssistantEndpoints = { status: '/ai/status', chat: '/ai/chat' },
) {
  const state = stateFor(moduleKey)
  return {
    state,
    loadStatus: (force = false) => loadStatus(moduleKey, state, endpoints, force),
    send: () => send(moduleKey, state, endpoints),
    newChat: () => newChat(state),
  }
}
