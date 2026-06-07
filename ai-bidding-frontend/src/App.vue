<script setup lang="ts">
import { ref } from 'vue'
import ChatView from './views/ChatView.vue'
import BidFillView from './views/BidFillView.vue'
import ProposalView from './views/ProposalView.vue'
import TemplateManageView from './views/TemplateManageView.vue'

const menu = [
  { key: 'chat', label: '💬 对话助手', icon: '💬' },
  { key: 'bid', label: '📋 标书填充', icon: '📋' },
  { key: 'proposal', label: '📄 方案生成', icon: '📄' },
  { key: 'tmpl', label: '🗂️ 模板管理', icon: '🗂️' },
]
const active = ref('chat')
const collapsed = ref(false)
</script>

<template>
  <div class="app-shell">
    <aside class="sidebar" :class="{ collapsed }">
      <div class="logo" @click="collapsed = !collapsed">🤖<span v-if="!collapsed"> AI 售前</span></div>
      <nav>
        <button v-for="m in menu" :key="m.key"
          :class="{ active: active === m.key }"
          @click="active = m.key"
          :title="m.label">
          <span class="nav-icon">{{ m.icon }}</span>
          <span v-if="!collapsed" class="nav-label">{{ m.label }}</span>
        </button>
      </nav>
    </aside>
    <main class="main-content">
      <ChatView v-if="active === 'chat'" @nav="(k: string) => active = k" />
      <BidFillView v-if="active === 'bid'" />
      <ProposalView v-if="active === 'proposal'" />
      <TemplateManageView v-if="active === 'tmpl'" />
    </main>
  </div>
</template>

<style>
body { margin: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; }
</style>

<style scoped>
.app-shell { display: flex; height: 100vh; background: #f5f7fa; }
.sidebar {
  width: 200px; background: #1a1a2e; color: #fff;
  display: flex; flex-direction: column; transition: width 0.2s; flex-shrink: 0;
}
.sidebar.collapsed { width: 56px; }
.sidebar.collapsed .nav-label { display: none; }
.sidebar.collapsed .logo span { display: none; }

.logo {
  padding: 20px 16px; font-size: 18px; font-weight: 700;
  cursor: pointer; user-select: none; border-bottom: 1px solid #2a2a4e;
}

nav { flex: 1; padding: 8px; display: flex; flex-direction: column; gap: 2px; }
nav button {
  display: flex; align-items: center; gap: 10px;
  width: 100%; padding: 10px 12px; border: none; border-radius: 8px;
  background: transparent; color: #aab; font-size: 14px; cursor: pointer;
  text-align: left; transition: 0.15s;
}
nav button:hover { background: #2a2a4e; color: #fff; }
nav button.active { background: #1e6fff; color: #fff; }
.nav-icon { font-size: 18px; width: 24px; text-align: center; flex-shrink: 0; }
.nav-label { white-space: nowrap; }

.main-content { flex: 1; overflow: hidden; }
</style>
