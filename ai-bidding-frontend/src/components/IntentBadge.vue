<script setup lang="ts">
defineProps<{ intent: string }>()

const labelMap: Record<string, string> = {
  CASE_RETRIEVAL: '案例检索',
  PRODUCT_QUERY: '产品咨询',
  POLICY_QUERY: '政策咨询',
  DATA_QUERY: '数据查询',
  GREETING: '问候'
}
</script>

<template>
  <span class="intent-badge" :class="intent.toLowerCase()">
    {{ labelMap[intent] || intent }}
  </span>
</template>

<style scoped>
.intent-badge {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}
.case_retrieval { background: #e6f0ff; color: #1e6fff; }
.product_query { background: #e6ffe6; color: #1ea81e; }
.policy_query { background: #fff3e6; color: #e67e22; }
.data_query { background: #f0e6ff; color: #8e1ea8; }
.greeting { background: #f0f0f0; color: #888; }
</style>
