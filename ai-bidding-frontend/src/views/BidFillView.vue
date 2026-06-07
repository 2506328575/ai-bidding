<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'

interface Template {
  name: string; label: string; size: string;
}

const templates = ref<Template[]>([])
const form = ref({ industry: '', projectInfo: '', templateName: '' })
const loading = ref(false)
const result = ref<any>(null)
const error = ref('')
const showEditor = ref(false)
const fileUrl = ref('')
const editorLoading = ref(false)

onMounted(async () => {
  try {
    const r = await fetch('/api/v1/bid/templates')
    templates.value = await r.json()
    if (templates.value.length) form.value.templateName = templates.value[0].name
  } catch (e) { /* ignore */ }
})

async function generate() {
  if (!form.value.industry) return
  loading.value = true; error.value = ''; showEditor.value = false
  try {
    const resp = await fetch('/api/v1/bid/fill', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(form.value)
    })
    if (!resp.ok) throw new Error('生成失败: ' + resp.status)
    result.value = await resp.json()
    fileUrl.value = '/api/v1/bid/file/' + result.value.fileId
  } catch (e: any) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

const editorIframe = ref<HTMLIFrameElement | null>(null)
const EDITOR_ORIGIN = window.location.origin

function base64ToBytes(b64: string): Uint8Array {
  const bin = atob(b64)
  const bytes = new Uint8Array(bin.length)
  for (let i = 0; i < bin.length; i++) bytes[i] = bin.charCodeAt(i)
  return bytes
}

function openEditor() {
  showEditor.value = true
  editorLoading.value = true
  nextTick(() => {
    // wait for iframe ready event, then send file
  })
}

function onEditorMessage(event: MessageEvent) {
  if (event.origin !== EDITOR_ORIGIN) return
  const { type, payload } = event.data || {}
  if (!type?.startsWith('document:')) return

  console.log('[Editor]', type, payload)
  switch (type) {
    case 'document:ready':
      // Editor initialized — send the docx bytes
      sendToEditor('document:open-buffer', {
        buffer: base64ToBytes(result.value.docxBase64),
        fileName: result.value.fileName || 'bid.docx',
        readonly: false
      })
      editorLoading.value = false
      break
    case 'document:opened':
      console.log('Document opened:', payload?.fileName)
      break
    case 'document:saved':
      // User saved in editor — download
      const blob = payload?.file as Blob
      if (blob) {
        const url = URL.createObjectURL(blob)
        const a = document.createElement('a')
        a.href = url
        a.download = payload?.fileName || result.value?.fileName || 'bid.docx'
        a.click()
        URL.revokeObjectURL(url)
      }
      break
    case 'document:error':
      error.value = '编辑操作失败: ' + (payload?.message || '未知错误')
      break
  }
}

function sendToEditor(type: string, payload: any) {
  const id = Date.now().toString(36) + Math.random().toString(36).slice(2)
  editorIframe.value?.contentWindow?.postMessage({ id, type, payload }, EDITOR_ORIGIN)
}

onMounted(() => window.addEventListener('message', onEditorMessage))
onUnmounted(() => window.removeEventListener('message', onEditorMessage))

function download() {
  if (!result.value?.docxBase64) return
  const link = document.createElement('a')
  link.href = 'data:application/octet-stream;base64,' + result.value.docxBase64
  link.download = result.value.fileName || 'bid_filled.docx'
  link.click()
}
</script>

<template>
  <div class="bid-fill-view">
    <header class="header">
      <h1>📋 标书模板填充</h1>
      <span class="subtitle">基于已有 Word 模板，AI 自动生成匹配内容</span>
    </header>

    <div class="layout">
      <div class="form-panel">
        <h3>需求背景</h3>
        <el-select v-model="form.templateName" class="mt">
          <el-option v-for="t in templates" :key="t.name" :value="t.name" :label="t.label + ' (' + t.size + ')'" />
        </el-select>
        <el-input v-model="form.industry" placeholder="行业 (如: 人力资源、制造业)" class="mt" />
        <el-input v-model="form.projectInfo" placeholder="项目概况 (如: ERP系统升级，预算100-300万)" type="textarea" :rows="4" class="mt" />
        <el-button type="primary" class="mt" :loading="loading" :disabled="!form.industry" @click="generate">
          {{ loading ? 'AI 生成中...' : '填充标书' }}
        </el-button>
        <p v-if="error" class="error">{{ error }}</p>
      </div>

      <div class="result-panel">
        <div v-if="!result && !loading" class="empty">👈 选择模板 + 输入需求 → 点击「填充标书」</div>
        <div v-if="result && !showEditor">
          <div class="toolbar">
            <h2>填充结果</h2>
            <el-button type="primary" @click="openEditor">✏️ 在线编辑</el-button>
            <el-button type="success" @click="download">⬇ 下载 Word</el-button>
          </div>
          <div v-for="s in result.sections" :key="s.title" class="section">
            <h3>📝 {{ s.title }}</h3>
            <pre>{{ s.content }}</pre>
          </div>
          <div class="footer">🎫 {{ result.totalTokensUsed }} tokens &nbsp; ⏱ {{ result.totalLatencyMs }}ms</div>
        </div>

        <div v-if="showEditor && fileUrl" class="editor-panel">
          <div class="editor-toolbar">
            <h2>✏️ 在线编辑 — {{ result?.fileName }}</h2>
            <el-button @click="showEditor = false">← 返回预览</el-button>
            <el-button type="success" @click="download">⬇ 下载</el-button>
          </div>
          <iframe
            ref="editorIframe"
            src="/editor/?embed=1"
            class="doc-editor-iframe"
            @load="editorLoading = false"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.bid-fill-view { min-height: 100vh; background: #f5f7fa; }
.header { padding: 20px 24px 12px; background: #fff; border-bottom: 1px solid #eee; }
.header h1 { margin: 0; font-size: 20px; }
.subtitle { font-size: 13px; color: #999; }
.layout { display: grid; grid-template-columns: 350px 1fr; gap: 24px; padding: 24px; }
.form-panel { background: #fff; padding: 20px; border-radius: 8px; height: fit-content; }
.form-panel h3 { margin: 0 0 12px; }
.mt { margin-top: 12px; }
.result-panel { background: #fff; padding: 24px; border-radius: 8px; overflow-y: auto; max-height: calc(100vh - 130px); }
.empty { text-align: center; padding: 80px; color: #999; }
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.toolbar h2 { margin: 0; }
.section { margin-bottom: 20px; padding-bottom: 16px; border-bottom: 1px solid #f0f0f0; }
.section h3 { margin: 0 0 8px; }
.section pre { margin: 0; white-space: pre-wrap; font-size: 13px; line-height: 1.7; background: #f8f9fa; padding: 12px; border-radius: 6px; }
.footer { font-size: 12px; color: #999; text-align: center; margin-top: 20px; }
.error { color: #e74c3c; margin-top: 8px; }

.editor-panel { display: flex; flex-direction: column; height: calc(100vh - 140px); }
.editor-toolbar { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; }
.editor-toolbar h2 { margin: 0; flex: 1; }
.doc-editor-iframe { flex: 1; border: 1px solid #ddd; border-radius: 8px; width: 100%; min-height: 600px; }
</style>
