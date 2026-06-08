# 前端工程师 SOP

## 技术栈
- Vue 3 + TypeScript + Vite
- Element Plus (UI)
- Amis (低代码管理后台)
- Axios (HTTP)
- OnlyOffice Web 编辑器 (iframe 嵌入)

## 页面清单

| 页面 | 路由 | 说明 |
|------|------|------|
| 对话助手 | / | 统一对话入口，智能路由 |
| 方案生成 | /proposal | 需求→AI撰写→在线编辑→下载 |
| 标书填充 | /bid | 模板选择→AI填充→在线编辑→下载 |
| 模板管理 | /templates | 上传/占位符/标记/填充 |

## 组件树
```
App.vue
├── Sidebar.vue          # 左侧菜单
├── ChatView.vue         # 对话主页
│   ├── ChatMessage.vue
│   ├── ChatInput.vue
│   ├── IntentBadge.vue
│   └── SourceCard.vue
├── ProposalView.vue     # 方案生成
├── BidFillView.vue      # 标书填充
└── TemplateManageView.vue # 模板管理
```

## 开发顺序
1. 项目骨架 + App.vue (0.5天)
2. ChatView + 组件 (1.5天)
3. ProposalView (1天)
4. BidFillView (1天)
5. TemplateManageView + 编辑器集成 (1.5天)
6. 联调测试 (0.5天)

## 参照
- Java Demo 前端: ai-bidding-frontend/ (完整功能验证通过)
- Vite proxy 配置: /api→8080, /editor→3000
