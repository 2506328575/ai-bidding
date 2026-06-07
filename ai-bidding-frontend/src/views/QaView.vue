<script setup lang="ts">
import { ref } from 'vue'
import { askQuestion } from '../api/qa'
import type { ChatMessage as ChatMessageType } from '../types/qa'
import ChatInput from '../components/ChatInput.vue'
import ChatMessageComponent from '../components/ChatMessage.vue'

const messages = ref<ChatMessageType[]>([])
const loading = ref(false)
const error = ref('')
const chatRef = ref<HTMLElement>()

function addMessage(m: Partial<ChatMessageType> & { role: 'user' | 'assistant'; content: string }) {
  messages.value.push({
    id: Date.now().toString() + Math.random(),
    timestamp: Date.now(),
    ...m
  })
}

async function handleSend(question: string) {
  error.value = ''
  addMessage({ role: 'user', content: question })

  loading.value = true
  try {
    const resp = await askQuestion({ question })
    addMessage({
      role: 'assistant',
      content: resp.answer,
      intent: resp.intent,
      sources: resp.sources,
      tokensUsed: resp.tokensUsed,
      latencyMs: resp.latencyMs
    })
  } catch (e: any) {
    const msg = e?.code === 'ECONNABORTED'
      ? '请求超时，请稍后重试'
      : '抱歉，系统暂时不可用，请稍后重试'
    addMessage({ role: 'assistant', content: msg })
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="qa-view">
    <header class="qa-header">
      <h1>🔍 AI 售前助手</h1>
      <span class="subtitle">基于 FA 案例库智能问答</span>
    </header>

    <div ref="chatRef" class="message-list">
      <div v-if="!messages.length" class="empty-state">
        <div class="empty-icon">👋</div>
        <p>您好！我是售前助手，可以帮您检索案例、了解产品方案</p>
      </div>

      <ChatMessageComponent
        v-for="msg in messages"
        :key="msg.id"
        :message="msg"
      />
    </div>

    <ChatInput :loading="loading" @send="handleSend" />
  </div>
</template>

<style scoped>
.qa-view {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #f5f7fa;
}

.qa-header {
  padding: 20px 24px 12px;
  background: #fff;
  border-bottom: 1px solid #eee;
}
.qa-header h1 { margin: 0; font-size: 20px; }
.subtitle { font-size: 13px; color: #999; }

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px;
}

.empty-state {
  text-align: center;
  padding: 80px 20px;
  color: #999;
}
.empty-icon { font-size: 48px; margin-bottom: 16px; }
</style>
