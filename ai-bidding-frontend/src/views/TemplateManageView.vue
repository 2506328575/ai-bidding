<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'

interface Placeholder { name: string; description: string; filledValue?: string }
interface TemplateRecord { id: string; fileName: string; placeholders: Placeholder[] }
interface FillResult { fileId: string; fileName: string; filledFields: Record<string,string>; totalTokens: number; docxBase64: string }

const templates = ref<TemplateRecord[]>([])
const selectedId = ref('')
const selected = ref<TemplateRecord | null>(null)
const context = ref('')
const loading = ref(false)
const result = ref<FillResult | null>(null)
const showEditor = ref(false)
const editorIframe = ref<HTMLIFrameElement | null>(null)
const error = ref('')
const newPhName = ref('')
const EDITOR_ORIGIN = window.location.origin

function addPlaceholder() {
  const name = newPhName.value.trim()
  if (!name || !selected.value) return
  if (selected.value.placeholders.some((p: Placeholder) => p.name === name)) return
  selected.value.placeholders.push({ name, description: '', filledValue: '' })
  newPhName.value = ''
}

onMounted(async () => { await refreshList(); window.addEventListener('message', onEditorMsg) })
onUnmounted(() => window.removeEventListener('message', onEditorMsg))

async function refreshList() {
  const r = await fetch('/api/v1/template/list')
  templates.value = await r.json()
}

async function selectTemplate(id: string) {
  selectedId.value = id
  for (const t of templates.value) {
    if (t.id === id) { selected.value = t; break }
  }
}

const fileInput = ref<HTMLInputElement | null>(null)
async function handleUpload() {
  const file = fileInput.value?.files?.[0]
  if (!file) return
  loading.value = true; error.value = ''
  const fd = new FormData(); fd.append('file', file)
  try {
    const r = await fetch('/api/v1/template/upload', { method: 'POST', body: fd })
    if (!r.ok) throw new Error('上传失败: ' + r.status)
    const rec = await r.json()
    await refreshList()
    selectedId.value = rec.id
    selected.value = rec
  } catch (e: any) { error.value = e.message }
  finally { loading.value = false }
}

async function savePlaceholders() {
  if (!selected.value) return
  const r = await fetch('/api/v1/template/' + selected.value.id + '/placeholders', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(selected.value.placeholders)
  })
  if (!r.ok) error.value = '保存失败'
}

async function fill() {
  if (!selected.value || !context.value) return
  loading.value = true; error.value = ''; showEditor.value = false
  try {
    const r = await fetch('/api/v1/template/' + selected.value.id + '/fill', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ context: context.value })
    })
    if (!r.ok) throw new Error('填充失败: ' + r.status)
    result.value = await r.json()
    // 将填充结果回写到占位符
    for (const p of selected.value.placeholders) {
      if (result.value.filledFields[p.name]) p.filledValue = result.value.filledFields[p.name]
    }
  } catch (e: any) { error.value = e.message }
  finally { loading.value = false }
}

const markupMode = ref(false)  // 标记占位符模式

async function openMarkupEditor() {
  if (!selected.value) return
  // 获取原始模板文件并打开编辑器
  markupMode.value = true
  showEditor.value = true
  // 不需要 result，直接在 iframe ready 时加载模板原始文件
}

function openEditor() {
  markupMode.value = false
  showEditor.value = true
}

function download() {
  if (!result.value?.docxBase64) return
  const a = document.createElement('a')
  a.href = 'data:application/octet-stream;base64,' + result.value.docxBase64
  a.download = result.value.fileName
  a.click()
}

function placeholderTag(name: string) { return '{' + '{' + name + '}' + '}' }

function base64ToBytes(b64: string): Uint8Array {
  const bin = atob(b64); const bytes = new Uint8Array(bin.length)
  for (let i = 0; i < bin.length; i++) bytes[i] = bin.charCodeAt(i)
  return bytes
}

function onEditorMsg(event: MessageEvent) {
  if (event.origin !== EDITOR_ORIGIN) return
  const { type, payload } = event.data || {}
  if (!type?.startsWith('document:')) return
  switch (type) {
    case 'document:ready':
      if (markupMode.value && selected.value) {
        // 标记模式：加载原始模板到编辑器
        fetch('/api/v1/template/' + selected.value.id + '/file')
          .then(r => r.arrayBuffer())
          .then(buf => {
            editorIframe.value?.contentWindow?.postMessage({
              id: Date.now().toString(36), type: 'document:open-buffer',
              payload: { buffer: new Uint8Array(buf),
                fileName: selected.value!.fileName, readonly: false }
            }, EDITOR_ORIGIN)
          })
      } else if (result.value?.docxBase64) {
        editorIframe.value?.contentWindow?.postMessage({
          id: Date.now().toString(36), type: 'document:open-buffer',
          payload: { buffer: base64ToBytes(result.value.docxBase64),
            fileName: result.value.fileName || 'filled.docx', readonly: false }
        }, EDITOR_ORIGIN)
      }
      break
    case 'document:saved':
      if (markupMode.value && (payload?.file as Blob)) {
        // 标记模式保存 → 重新上传提取占位符
        const form = new FormData()
        form.append('file', payload.file as Blob, selected.value?.fileName || 'marked.docx')
        fetch('/api/v1/template/upload', { method: 'POST', body: form })
          .then(r => r.json())
          .then(rec => {
            refreshList()
            selectedId.value = rec.id; selected.value = rec
            markupMode.value = false; showEditor.value = false
            error.value = ''
          })
          .catch(e => { error.value = '重新提取占位符失败: ' + e.message })
        return
      }
      const blob = payload?.file as Blob
      if (blob) {
        const url = URL.createObjectURL(blob)
        const a = document.createElement('a'); a.href = url
        a.download = payload?.fileName || 'filled.docx'; a.click()
        URL.revokeObjectURL(url)
      }
      break
    case 'document:error': error.value = '编辑错误: ' + (payload?.message || ''); break
  }
}
</script>

<template>
  <div class="tmpl-view">
    <header class="header">
      <h1>🗂️ 模板管理</h1>
      <span class="subtitle">上传模板 → 管理占位符 → AI 填充 → 在线预览编辑</span>
    </header>

    <div class="layout">
      <!-- 左侧：上传 + 模板列表 + 占位符 -->
      <div class="left-panel">
        <h3>📤 上传模板</h3>
        <input ref="fileInput" type="file" accept=".docx" @change="handleUpload" />
        <p v-if="error" class="error">{{ error }}</p>

        <h3 class="mt">📋 已上传模板</h3>
        <div v-if="!templates.length" class="empty">暂无模板</div>
        <div v-for="t in templates" :key="t.id" class="tmpl-item"
          :class="{ active: selectedId === t.id }" @click="selectTemplate(t.id)">
          <span>📄 {{ t.fileName }}</span>
          <span class="badge">{{ t.placeholders.length }} 占位符</span>
        </div>
      </div>

      <!-- 中间：占位符管理 -->
      <div class="mid-panel" v-if="selected">
        <h3>🏷️ 占位符 ({{ selected.placeholders.length }})</h3>
        <div v-if="selected.placeholders.length === 0" class="notice">
          ⚠️ 模板中未检测到 <code>{{　}}</code> 占位符。<br/>
          <el-button type="primary" size="small" class="mt" @click="openMarkupEditor">
            ✏️ 在编辑器中添加占位符
          </el-button>
          <span class="hint">→ 在文档中需要填充的位置输入 <code>{{　字段名　}}</code> → 保存 → 自动提取</span>
          <br/><br/>
          或在此处手动添加占位符名称：
        </div>
        <div v-for="(p, i) in selected.placeholders" :key="p.name" class="ph-row">
          <code>{{ placeholderTag(p.name) }}</code>
          <input v-model="p.description" :placeholder="'含义描述'" class="ph-desc" />
          <span v-if="p.filledValue" class="filled-preview">{{ p.filledValue?.substring(0, 40) }}...</span>
          <span class="ph-del" @click="selected.placeholders.splice(i,1)" title="删除">×</span>
        </div>
        <!-- 手动添加占位符 -->
        <div class="add-ph-row">
          <input v-model="newPhName" placeholder="新占位符名称 (如: 技术方案)" class="ph-desc"
            @keyup.enter="addPlaceholder" />
          <el-button size="small" @click="addPlaceholder" :disabled="!newPhName.trim()">+ 添加</el-button>
        </div>
        <el-button size="small" class="mt" @click="savePlaceholders">💾 保存占位符定义</el-button>

        <!-- 填充 -->
        <h3 class="mt">🤖 AI 填充</h3>
        <el-input v-model="context" type="textarea" :rows="4"
          placeholder="项目背景/需求上下文（如：制造业ERP升级，预算100-300万，需包含报价明细）" />
        <el-button type="primary" class="mt" :loading="loading" :disabled="!context" @click="fill">
          {{ loading ? 'AI 生成中...' : '🚀 AI 填充模板' }}
        </el-button>
      </div>

      <!-- 右侧：结果 + 预览 -->
      <div class="right-panel" v-if="result && !showEditor">
        <div class="toolbar">
          <h3>✅ 填充结果</h3>
          <el-button type="primary" @click="openEditor">✏️ 在线编辑</el-button>
          <el-button type="success" @click="download">⬇ 下载</el-button>
        </div>
        <div v-for="(v, k) in result.filledFields" :key="k" class="field-result">
          <strong>{{ k }}</strong>
          <pre>{{ v }}</pre>
        </div>
        <p class="footer">🎫 {{ result.totalTokens }} tokens</p>
      </div>

      <!-- 在线编辑器 -->
      <div class="right-panel editor-panel" v-if="showEditor && result">
        <div class="editor-toolbar">
          <h3>✏️ 在线编辑 — {{ result.fileName }}</h3>
          <el-button @click="showEditor = false">← 返回</el-button>
          <el-button type="success" @click="download">⬇ 下载</el-button>
        </div>
        <iframe ref="editorIframe" src="/editor/?embed=1" class="doc-editor-iframe" />
      </div>
    </div>
  </div>
</template>

<style scoped>
.tmpl-view { min-height: 100vh; background: #f5f7fa; }
.header { padding: 20px 24px 12px; background: #fff; border-bottom: 1px solid #eee; }
.header h1 { margin: 0; font-size: 20px; }
.subtitle { font-size: 13px; color: #999; }
.layout { display: grid; grid-template-columns: 250px 1fr 1fr; gap: 20px; padding: 20px; }

.left-panel, .mid-panel, .right-panel {
  background: #fff; padding: 20px; border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
}
.mt { margin-top: 16px; }
.empty { color: #999; padding: 20px; }
.tmpl-item { padding: 10px 12px; border-radius: 6px; cursor: pointer; margin: 4px 0; display: flex; justify-content: space-between; }
.tmpl-item:hover { background: #f0f5ff; }
.tmpl-item.active { background: #e6f0ff; font-weight: 600; }
.badge { font-size: 11px; background: #1e6fff; color: #fff; padding: 2px 8px; border-radius: 10px; }

.ph-row { display: flex; align-items: center; gap: 8px; margin: 6px 0; padding: 6px; background: #f8f9fa; border-radius: 4px; }
.ph-row code { font-size: 12px; color: #1e6fff; min-width: 80px; }
.ph-desc { flex: 1; border: 1px solid #ddd; padding: 4px 8px; border-radius: 4px; font-size: 12px; }
.filled-preview { font-size: 11px; color: #999; max-width: 120px; overflow: hidden; white-space: nowrap; }

.toolbar { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; }
.toolbar h3 { margin: 0; flex: 1; }
.field-result { margin: 10px 0; padding: 8px; background: #f8f9fa; border-radius: 4px; }
.field-result strong { color: #1e6fff; }
.field-result pre { margin: 4px 0 0; font-size: 12px; white-space: pre-wrap; }
.footer { font-size: 11px; color: #999; text-align: center; margin-top: 12px; }
.error { color: #e74c3c; font-size: 12px; }
.notice { background: #fff8e1; padding: 12px; border-radius: 6px; font-size: 13px; line-height: 1.8; }
.notice code { background: #ffe082; padding: 1px 4px; border-radius: 3px; }
.hint { font-size: 11px; color: #999; display: block; margin-top: 4px; }
.add-ph-row { display: flex; gap: 8px; margin-top: 8px; }
.ph-del { cursor: pointer; color: #e74c3c; font-weight: bold; padding: 0 4px; }
.ph-del:hover { color: #c0392b; }
.editor-panel { height: calc(100vh - 120px); display: flex; flex-direction: column; }
.editor-toolbar { display: flex; align-items: center; gap: 10px; margin-bottom: 8px; }
.doc-editor-iframe { flex: 1; border: 1px solid #ddd; border-radius: 6px; min-height: 500px; }
</style>
