# AI 招投标系统 Demo — 环境搭建指南

## 前置依赖

| 工具 | 版本要求 | 安装方式 |
|------|---------|---------|
| JDK | 17+ | `sdk install java 17.0.9-tem` |
| Node.js | 20+ | `nvm install 20` 或官网下载 |
| Maven | 3.8+ | `sdk install maven` (或用项目自带 `mvnw`) |
| Python | 3.10+ | 用于 docx 模板填充脚本 |
| Git | 任意 | `git` |

## 首次克隆

```bash
git clone <你的GitHub仓库地址> ai-bidding
cd ai-bidding

# 安装文档编辑器（独立项目）
git clone https://github.com/ranuts/document.git document-editor
cd document-editor && npm install && cd ..

# 安装前端依赖
cd ai-bidding-frontend && npm install && cd ..

# 安装 Python 依赖
pip install python-docx lxml
```

## 启动全部服务

需要 **3 个终端**：

```bash
# 终端 1：后端 (Java)
export DEEPSEEK_API_KEY="sk-your-key"
export JAVA_HOME="$HOME/.sdkman/candidates/java/17.0.9-tem"
cd yudao-module-ai-bidding
./mvnw spring-boot:run    # Windows: mvnw.cmd spring-boot:run

# 终端 2：前端 (Vue)
cd ai-bidding-frontend
npm run dev

# 终端 3：文档编辑器 (OnlyOffice)
cd document-editor
npm run dev -- --port 3000
```

浏览器打开 **http://localhost:5173**

## 运行测试

```bash
# 后端测试 (63 条)
cd yudao-module-ai-bidding
./mvnw test

# 前端测试 (15 条)
cd ai-bidding-frontend
npm test
```

## 多机器同步工作流

```
家 (PC)                   GitHub                  办公室 (笔记本)
───────                  ──────                  ───────────
git pull ←─── 同步 ←─── git push ←─── 下班前     git pull ←─── 上班后
git add .                                    git add .
git commit -m "..."                           git commit -m "..."
git push          ───→ 同步 ──→              git push        ──→
```

### 每天开始工作

```bash
cd ai-bidding
git pull                       # 拉最新代码
# 如果有依赖更新：
cd ai-bidding-frontend && npm install && cd ..
cd document-editor && git pull && npm install && cd ..
# 启动服务
```

### 每天结束工作

```bash
cd ai-bidding
git add -A
git commit -m "描述你的改动"
git push
```
