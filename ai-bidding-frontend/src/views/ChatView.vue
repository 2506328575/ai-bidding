<script setup lang="ts">
import { ref, nextTick } from 'vue'
defineEmits<{ nav: [key: string] }>()

interface ChatMsg {
  id: string; role: 'user' | 'assistant'; content: string
  type?: string; data?: any; timestamp: number
}

const messages = ref<ChatMsg[]>([])
const input = ref('')
const loading = ref(false)
const chatRef = ref<HTMLElement | null>(null)

function addMsg(role: 'user'|'assistant', content: string, type?: string, data?: any) {
  messages.value.push({ id: Date.now().toString(36), role, content, type, data, timestamp: Date.now() })
  nextTick(() => { chatRef.value?.scrollTo({ top: chatRef.value.scrollHeight, behavior: 'smooth' }) })
}

async function send() {
  const q = input.value.trim(); if (!q || loading.value) return
  input.value = ''; addMsg('user', q); loading.value = true
  try {
    const r = await fetch('/api/v1/chat', {
      method: 'POST', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ message: q })
    })
    const resp = await r.json()
    if (resp.type === 'qa_result') {
      addMsg('assistant', resp.data.answer, 'qa', resp.data)
    } else {
      addMsg('assistant', resp.text || JSON.stringify(resp.data), resp.type)
    }
  } catch (e: any) {
    addMsg('assistant', '抱歉，系统暂时不可用：' + e.message, 'error')
  } finally { loading.value = false }
}

function handleKey(e: KeyboardEvent) {
  if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); send() }
}

// 快捷按钮
function quickSend(text: string) { input.value = text; send() }
</script>

<template>
  <div class="chat-app">
    <header class="chat-header">
      <h1>🤖 AI 售前助手</h1>
      <span class="subtitle">统一对话入口 · 智能路由 · 案例检索 / 方案生成 / 标书填充</span>
    </header>

    <div ref="chatRef" class="chat-messages">
      <div v-if="!messages.length" class="welcome">
        <div class="welcome-icon">👋</div>
        <h2>您好！我是 AI 售前助手</h2>
        <p>可以直接告诉我您的需求，例如：</p>
        <div class="quick-actions">
          <button @click="quickSend('有没有制造业的ERP案例？')">🔍 检索案例</button>
          <button @click="quickSend('你们的系统支持国产化部署吗？')">💡 技术问答</button>
          <button @click="$emit('nav','proposal')">📄 方案生成</button>
          <button @click="$emit('nav','bid')">📋 标书填充</button>
        </div>
      </div>

      <div v-for="msg in messages" :key="msg.id" class="msg-row" :class="msg.role">
        <div class="avatar">{{ msg.role === 'user' ? '👤' : '🤖' }}</div>
        <div class="bubble" :class="msg.role">
          <div class="msg-content" v-html="renderContent(msg.content)" />
          <!-- QA 来源卡片 -->
          <div v-if="msg.type === 'qa' && msg.data?.sources?.length" class="sources">
            <div v-for="s in msg.data.sources" :key="s.title" class="src-card">
              📄 {{ s.title }}
            </div>
          </div>
          <!-- 加载动画 -->
          <div v-if="loading && msg.role === 'assistant' && msg === messages[messages.length-1]" class="typing">
            <span /><span /><span />
          </div>
        </div>
      </div>
    </div>

    <div class="chat-input-bar">
      <input v-model="input" placeholder="输入您的问题或需求... (Enter 发送)"
        :disabled="loading" @keydown="handleKey" />
      <button :disabled="!input.trim() || loading" @click="send">
        {{ loading ? '...' : '发送' }}
      </button>
    </div>
  </div>
</template>

<script lang="ts">
// Markdown 简单渲染
export function renderContent(text: string): string {
  if (!text) return ''
  return text
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/\n/g, '<br/>')
    .replace(/^- (.+)$/gm, '• $1')
}
</script>

<style scoped>
.chat-app { display: flex; flex-direction: column; height: 100vh; background: #f5f7fa; }
.chat-header { padding: 16px 24px; background: #fff; border-bottom: 1px solid #eee; text-align: center; }
.chat-header h1 { margin: 0; font-size: 20px; }
.subtitle { font-size: 12px; color: #999; }

.chat-messages { flex: 1; overflow-y: auto; padding: 20px 24px; }

.welcome { text-align: center; padding: 60px 20px; }
.welcome-icon { font-size: 56px; }
.welcome h2 { margin: 12px 0 4px; }
.welcome p { color: #999; margin: 0 0 20px; }
.quick-actions { display: flex; gap: 10px; justify-content: center; flex-wrap: wrap; }
.quick-actions button {
  padding: 10px 18px; border: 1px solid #ddd; border-radius: 20px;
  background: #fff; cursor: pointer; font-size: 13px; transition: 0.2s;
}
.quick-actions button:hover { background: #e6f0ff; border-color: #1e6fff; }

.msg-row { display: flex; gap: 10px; margin-bottom: 20px; }
.msg-row.user { flex-direction: row-reverse; }
.avatar { width: 32px; height: 32px; display: flex; align-items: center; justify-content: center; font-size: 18px; flex-shrink: 0; }

.bubble { max-width: 75%; padding: 12px 18px; border-radius: 16px; line-height: 1.7; font-size: 14px; }
.bubble.user { background: #1e6fff; color: #fff; border-bottom-right-radius: 4px; }
.bubble.assistant { background: #fff; box-shadow: 0 1px 4px rgba(0,0,0,0.08); border-bottom-left-radius: 4px; }

.sources { margin-top: 10px; display: flex; flex-wrap: wrap; gap: 6px; }
.src-card { background: #f0f5ff; padding: 4px 12px; border-radius: 12px; font-size: 12px; color: #1e6fff; }

.chat-input-bar { display: flex; gap: 10px; padding: 16px 24px; background: #fff; border-top: 1px solid #eee; }
.chat-input-bar input { flex: 1; padding: 12px 16px; border: 1px solid #ddd; border-radius: 24px; font-size: 14px; outline: none; }
.chat-input-bar input:focus { border-color: #1e6fff; }
.chat-input-bar button { padding: 10px 24px; background: #1e6fff; color: #fff; border: none; border-radius: 24px; cursor: pointer; font-size: 14px; }
.chat-input-bar button:disabled { background: #ccc; cursor: not-allowed; }

.typing { display: flex; gap: 4px; padding: 4px 0; }
.typing span { width: 6px; height: 6px; background: #ccc; border-radius: 50%; animation: bounce 1.4s infinite; }
.typing span:nth-child(2) { animation-delay: 0.2s; }
.typing span:nth-child(3) { animation-delay: 0.4s; }
@keyframes bounce { 0%,80%,100% { transform: scale(0); } 40% { transform: scale(1); } }
</style>
