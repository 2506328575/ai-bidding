<script setup lang="ts">
import type { ChatMessage } from '../types/qa'
import IntentBadge from './IntentBadge.vue'
import SourceCard from './SourceCard.vue'

defineProps<{ message: ChatMessage }>()
</script>

<template>
  <div class="chat-message" :class="message.role">
    <div class="bubble" :class="message.role">
      <div v-if="message.role === 'assistant' && message.intent" class="meta">
        <IntentBadge :intent="message.intent" />
      </div>
      <div class="content">{{ message.content }}</div>
      <div v-if="message.role === 'assistant' && message.sources?.length" class="sources">
        <SourceCard
          v-for="s in message.sources"
          :key="s.title"
          :source="s"
        />
      </div>
      <div v-if="message.role === 'assistant' && message.latencyMs" class="footer">
        <span>⏱ {{ message.latencyMs }}ms</span>
        <span v-if="message.tokensUsed">🎫 {{ message.tokensUsed }} tokens</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.chat-message { display: flex; margin-bottom: 16px; }
.chat-message.user { justify-content: flex-end; }
.chat-message.assistant { justify-content: flex-start; }

.bubble {
  max-width: 80%;
  padding: 12px 16px;
  border-radius: 12px;
  line-height: 1.6;
}
.bubble.user { background: #1e6fff; color: #fff; }
.bubble.assistant { background: #fff; box-shadow: 0 1px 4px rgba(0,0,0,0.08); }

.meta { margin-bottom: 8px; }
.content { white-space: pre-wrap; word-break: break-word; }
.sources { margin-top: 8px; }
.footer { margin-top: 8px; font-size: 11px; color: #aaa; display: flex; gap: 12px; }
</style>
