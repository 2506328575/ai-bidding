<script setup lang="ts">
import { ref } from 'vue'

const form = ref({ industry: '', scale: '', painPoints: '', budget: '' })
const loading = ref(false)
const result = ref<any>(null)
const error = ref('')

async function generate() {
  if (!form.value.industry || !form.value.painPoints) return
  loading.value = true
  error.value = ''
  try {
    const resp = await fetch('/api/v1/proposal/generate', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(form.value)
    })
    if (!resp.ok) throw new Error('生成失败: ' + resp.status)
    result.value = await resp.json()
  } catch (e: any) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

function download() {
  if (!result.value?.docxBase64) return
  const link = document.createElement('a')
  link.href = 'data:application/octet-stream;base64,' + result.value.docxBase64
  link.download = 'proposal.docx'
  link.click()
}
</script>

<template>
  <div class="proposal-view">
    <header class="proposal-header">
      <h1>📄 方案生成</h1>
      <span class="subtitle">输入客户需求，AI 自动生成售前技术方案</span>
    </header>

    <div class="layout">
      <!-- 输入区 -->
      <div class="form-panel">
        <h3>客户需求</h3>
        <el-input v-model="form.industry" placeholder="行业 (如: 制造业、金融)" />
        <el-input v-model="form.scale" placeholder="规模 (如: 200-500人、中大型)" class="mt" />
        <el-input
          v-model="form.painPoints"
          placeholder="核心痛点 (如: ERP系统老旧需升级)"
          type="textarea"
          :rows="3"
          class="mt"
        />
        <el-input v-model="form.budget" placeholder="预算范围 (如: 100-300万)" class="mt" />
        <el-button
          type="primary"
          class="mt"
          :loading="loading"
          :disabled="!form.industry || !form.painPoints"
          @click="generate"
        >
          {{ loading ? 'AI 生成中...' : '生成方案' }}
        </el-button>
        <p v-if="error" class="error">{{ error }}</p>
      </div>

      <!-- 结果区 -->
      <div class="result-panel">
        <div v-if="!result && !loading" class="empty">
          👈 输入需求后点击「生成方案」
        </div>

        <div v-if="result" class="proposal-result">
          <div class="toolbar">
            <h2>{{ result.title }}</h2>
            <el-button type="success" @click="download">⬇ 下载 Word</el-button>
          </div>

          <!-- 大纲 -->
          <div class="outline">
            <h3>📋 方案大纲</h3>
            <pre>{{ result.outline }}</pre>
          </div>

          <!-- 各章节内容 -->
          <div v-for="sec in result.sections" :key="sec.title" class="section">
            <h3>📝 {{ sec.title }}</h3>
            <p>{{ sec.content }}</p>
          </div>

          <div class="footer-info">
            ⏱ {{ result.totalLatencyMs }}ms &nbsp; 🎫 {{ result.totalTokensUsed }} tokens
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.proposal-view {
  min-height: 100vh;
  background: #f5f7fa;
}
.proposal-header {
  padding: 20px 24px 12px;
  background: #fff;
  border-bottom: 1px solid #eee;
}
.proposal-header h1 { margin: 0; font-size: 20px; }
.subtitle { font-size: 13px; color: #999; }

.layout {
  display: grid;
  grid-template-columns: 350px 1fr;
  gap: 24px;
  padding: 24px;
  max-height: calc(100vh - 100px);
}

.form-panel {
  background: #fff;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.08);
  height: fit-content;
}
.form-panel h3 { margin: 0 0 16px; }
.mt { margin-top: 12px; }

.result-panel {
  background: #fff;
  padding: 24px;
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.08);
  overflow-y: auto;
}
.empty {
  text-align: center;
  padding: 80px;
  color: #999;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 2px solid #1e6fff;
}
.toolbar h2 { margin: 0; font-size: 18px; }

.outline {
  background: #f0f5ff;
  padding: 16px;
  border-radius: 8px;
  margin-bottom: 24px;
}
.outline pre { margin: 0; white-space: pre-wrap; font-size: 13px; line-height: 1.8; }

.section {
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}
.section h3 { margin: 0 0 12px; color: #1e6fff; }
.section p { margin: 0; line-height: 1.8; white-space: pre-wrap; }

.footer-info { font-size: 12px; color: #999; margin-top: 20px; text-align: center; }
.error { color: #e74c3c; margin-top: 8px; }
</style>
