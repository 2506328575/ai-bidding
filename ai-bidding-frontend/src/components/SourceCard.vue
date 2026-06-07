<script setup lang="ts">
import type { Source } from '../types/qa'
defineProps<{ source: Source }>()
</script>

<template>
  <div class="source-card">
    <span class="icon">📄</span>
    <span class="title">{{ source.title }}</span>
    <span class="relevance">匹配度: {{ source.relevance }}</span>
  </div>
</template>

<style scoped>
.source-card {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  margin-top: 6px;
  background: #f8f9fa;
  border-radius: 8px;
  font-size: 13px;
}
.title { flex: 1; color: #333; }
.relevance { color: #999; font-size: 11px; }
</style>
