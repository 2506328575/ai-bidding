<script setup lang="ts">
import { ref } from 'vue'

defineProps<{ loading: boolean }>()
const emit = defineEmits<{ send: [question: string] }>()

const input = ref('')

function handleSend() {
  const q = input.value.trim()
  if (!q) return
  emit('send', q)
  input.value = ''
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    handleSend()
  }
}
</script>

<template>
  <div class="chat-input">
    <el-input
      v-model="input"
      placeholder="输入您的问题... Enter 发送 / Shift+Enter 换行"
      :disabled="loading"
      :rows="2"
      type="textarea"
      resize="none"
      data-testid="chat-input"
      @keydown="handleKeydown"
    />
    <el-button
      type="primary"
      :disabled="!input.trim() || loading"
      :loading="loading"
      data-testid="send-button"
      @click="handleSend"
    >
      发送
    </el-button>
  </div>
</template>

<style scoped>
.chat-input {
  display: flex;
  gap: 12px;
  align-items: flex-end;
  padding: 16px;
  background: #fff;
  border-top: 1px solid #eee;
}
.chat-input :deep(.el-textarea) { flex: 1; }
</style>
