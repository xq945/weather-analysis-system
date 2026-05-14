# 天气数据分析系统

基于 Spring Boot + Vue 3 的天气数据采集、展示与分析平台，集成和风天气 API，支持多城市天气实时监控、历史数据对比分析、AI 智能问答与分析报告生成。

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端框架 | Spring Boot 3.2.5 |
| ORM | MyBatis-Plus 3.5.6 |
| 数据库 | MySQL |
| 认证 | JWT (jjwt 0.12) |
| 向量数据库 | Qdrant |
| AI Embedding | 阿里云百炼 DashScope (text-embedding-v4) |
| AI 对话 | DeepSeek API |
| 前端框架 | Vue 3 + TypeScript |
| 构建工具 | Vite 5 |
| UI 组件库 | Element Plus |
| 图表 | ECharts 6 |
| 状态管理 | Pinia |
| 路由 | Vue Router 4 |

## 功能模块

- **用户认证** — 注册、登录、JWT Token 鉴权，支持管理员/普通用户权限
- **用户管理** — 管理员可查看所有用户，管理用户状态与权限
- **仪表盘** — 关注城市天气概览、实时数据总览
- **天气查询** — 实时天气、7 天预报、历史数据查询
- **数据分析** — 单城市统计、多城市对比分析
- **城市管理** — 关注城市列表，支持搜索添加，支持切换查看全部/我的城市
- **定时采集** — 每 30 分钟自动拉取实时天气，每日 8 点拉取预报
- **AI 智能助手** — 基于 RAG（检索增强生成）的自然语言问答，支持流式 SSE 输出，检索不到时自动降级调用大模型
- **分析报告生成** — 自动生成 Markdown 格式天气分析报告，向量化存储于 Qdrant

## 系统架构

```
┌──────────────┐     ┌──────────────────────────────────────────────────┐
│  前端 Vue 3   │────▶│                后端 Spring Boot                  │
│  localhost:3000│    │               localhost:8080                     │
└──────────────┘     │                                                  │
                     │  ┌───────────┐  ┌──────────┐  ┌───────────────┐  │
                     │  │ ChatService│─▶│Qdrant检索 │  │ EmbeddingService│  │
                     │  └─────┬─────┘  └──────────┘  └───────┬───────┘  │
                     │        │                              │          │
                     │        ▼                              ▼          │
                     │  ┌───────────┐                 ┌───────────┐      │
                     │  │ LlmService│                 │ DashScope  │      │
                     │  │ (DeepSeek)│                 │ Embedding  │      │
                     │  └───────────┘                 └───────────┘      │
                     │                                                  │
                     └──────────────────────────────────────────────────┘
                                      │
                    ┌─────────────────┼──────────────────┐
                    ▼                 ▼                   ▼
              ┌──────────┐    ┌────────────┐    ┌──────────────┐
              │  MySQL    │    │   Qdrant   │    │  和风天气 API  │
              │ 天气/用户/│    │  向量数据库  │    │  实时天气/预报  │
              │  报告数据  │    │  报告chunks │    │              │
              └──────────┘    └────────────┘    └──────────────┘
```

### AI 智能助手完整流程

```
用户提问 "北京这几天温度变化大吗？"
    │
    ├─→ 识别城市 → "北京"
    │
    ├─→ EmbeddingService.embed(question)
    │    └─→ 调用 DashScope text-embedding-v4 生成 1024 维向量
    │
    ├─→ RetrieverService.search(vector, city="北京", dateRange, topK=10)
    │    └─→ Qdrant 向量检索 + 元数据过滤（city match + date range）
    │         ├─ 命中阈值 ≥ 0.6 → 返回检索片段
    │         └─ 未命中 → 空结果
    │
    ├─→ 构建 System Prompt
    │    ├─ 有检索结果 → 拼接上下文片段 + 引用来源
    │    └─ 无检索结果 → 使用默认提示词（降级到普通对话）
    │
    ├─→ LlmService.chatStream(systemPrompt, question, emitter, onDone)
    │    └─→ 调用 DeepSeek API (deepseek-chat, stream=true)
    │         └─ 通过 SSE 逐字返回 delta 事件到前端
    │
    └─→ LLM 流结束 → 发送 done 事件（含引用报告来源列表 → 前端显示标签）
```

## 项目结构

```
weather-analysis-system/
├── weather-server/                 # 后端 Spring Boot
│   ├── src/main/java/com/weather/
│   │   ├── config/                 # 拦截器、定时任务、异常处理、Qdrant 配置
│   │   ├── controller/             # REST 控制器（Auth/City/Weather/User/Chat/Report）
│   │   ├── dto/                    # 请求/响应 DTO（ChatRequest/ChatResponse/SearchResult/Report）
│   │   ├── entity/                 # 实体类（User/FollowedCity/WeatherData/WeatherReport）
│   │   ├── mapper/                 # MyBatis Mapper
│   │   ├── service/                # 业务逻辑层
│   │   │   ├── ChatService.java    # RAG 编排核心（向量化→检索→Prompt→LLM）
│   │   │   ├── EmbeddingService.java # DashScope Embedding API 调用
│   │   │   ├── LlmService.java     # DeepSeek LLM API 调用（流式/非流式）
│   │   │   ├── RetrieverService.java # Qdrant 向量检索与写入
│   │   │   └── ReportService.java  # 分析报告生成与向量同步
│   │   └── util/                   # JWT、和风天气 API 客户端、天气文本翻译
│   └── src/main/resources/
│       ├── application.yml         # 应用主配置
│       ├── application-local.yml         # 本地开发配置（不提交 Git）
│       └── application-local.yml.example  # 本地开发配置模板
├── weather-web/                    # 前端 Vue 3
│   └── src/
│       ├── api/                    # API 请求封装（auth/city/weather/user/chat/report）
│       ├── components/             # 公共组件
│       ├── router/                 # 路由配置（含权限守卫）
│       ├── stores/                 # Pinia 状态管理（auth/cities）
│       └── views/                  # 页面视图
│           └── AssistantView.vue   # 天气助手对话页面
├── deploy/                          # 部署配置
│   └── nginx.conf                   # Nginx 配置（宝塔面板适用）
├── sql/                             # 数据库脚本
│   ├── weather_analysis.sql         # 完整建表脚本
│   └── weather_report.sql           # 分析报告表（已合并入主脚本）
└── docs/                            # 设计文档
    └── qdrant-rag-design.md         # RAG 检索增强生成设计
```

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.8+
- Node.js 18+
- MySQL 8.0+
- Docker（运行 Qdrant 向量数据库）

### 数据库初始化

运行 `sql/weather_analysis.sql` 即可创建数据库和表结构。

### 启动 Qdrant

```bash
docker run -d --name qdrant -p 6333:6333 qdrant/qdrant
```

### 修改本地配置

复制配置模板并填入真实值：

```bash
cp weather-server/src/main/resources/application-local.yml.example \
   weather-server/src/main/resources/application-local.yml
```

编辑 `application-local.yml`，按实际环境修改数据库连接、API Key 等信息：

```yaml
spring:
  datasource:
    username: 你的数据库用户名
    password: 你的数据库密码

jwt:
  secret: 你的JWT密钥

qweather:
  api-key: 你的和风天气API Key

# 阿里云百炼 API（用于文本 Embedding 向量化）
embedding:
  api-key: 你的百炼API Key (sk-xxxx)

# DeepSeek API（用于 LLM 对话）
llm:
  api-key: 你的DeepSeek API Key (sk-xxxx)
```

### 环境变量

所有配置均可通过环境变量覆盖：

```bash
# Linux / macOS
export DASHSCOPE_API_KEY=your_dashscope_key
export DEEPSEEK_API_KEY=your_deepseek_key
export QWATHER_API_KEY=your_qweather_key

# Windows (PowerShell)
$env:DASHSCOPE_API_KEY="your_dashscope_key"
```

### 后端启动

```bash
cd weather-server
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

服务启动在 `http://localhost:8080`

### 前端启动

```bash
cd weather-web
npm install
npm run dev
```

开发服务器启动在 `http://localhost:3000`，API 请求自动代理到后端。

## API 接口

### 基础接口

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/api/auth/register` | 用户注册 | 否 |
| POST | `/api/auth/login` | 用户登录 | 否 |
| GET | `/api/auth/me` | 当前用户信息 | 是 |
| GET | `/api/weather/overview` | 关注城市概览 | 是 |
| POST | `/api/weather/fetch` | 手动拉取天气数据 | 是 |
| GET | `/api/weather/now` | 实时天气 | 是 |
| GET | `/api/weather/forecast` | 7 天预报 | 是 |
| GET | `/api/weather/history` | 历史天气 | 是 |
| GET | `/api/weather/statistics` | 天气统计 | 是 |
| GET | `/api/weather/compare` | 城市对比 | 是 |
| GET | `/api/cities` | 我的关注城市列表 | 是 |
| GET | `/api/cities/all` | 所有城市列表 | 是 |
| POST | `/api/cities` | 添加关注城市 | 是 |
| DELETE | `/api/cities/{id}` | 删除关注城市 | 是 |
| GET | `/api/users` | 用户列表（管理员） | 是 |
| PUT | `/api/users/{id}/status` | 修改用户状态（管理员） | 是 |
| PUT | `/api/users/{id}/permission` | 修改用户权限（管理员） | 是 |

### 报告与 AI 接口

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/api/admin/report/generate` | 生成天气分析报告 | 是 |
| GET | `/api/report/list` | 报告列表 | 是 |
| GET | `/api/report/{reportId}` | 报告详情 | 是 |
| DELETE | `/api/admin/report/{reportId}` | 删除报告 | 是 |
| POST | `/api/admin/report/sync/{id}` | 重新同步向量库 | 是 |
| POST | `/api/chat/ask` | AI 问答（SSE 流式/非流式） | 是 |

### AI 问答接口详情

#### 请求格式

```json
POST /api/chat/ask
Content-Type: application/json
Authorization: Bearer <token>

{
  "question": "北京这几天温度变化大吗？",
  "city": "北京",
  "dateRange": {
    "start": "2026-05-07",
    "end": "2026-05-14"
  },
  "stream": true
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| question | string | 是 | 用户问题 |
| city | string | 否 | 指定城市，为空自动从问题中提取 |
| dateRange.start | string | 否 | 开始日期 yyyy-MM-dd，默认 7 天前 |
| dateRange.end | string | 否 | 结束日期 yyyy-MM-dd，默认今天 |
| stream | boolean | 否 | 是否流式输出，默认 true |

#### 流式响应格式 (SSE)

```
event:delta
data:北京最近一周的

event:delta
data:温度变化较为平稳...

event:done
data:[{"city":"北京","date":"2026-05-13","section":"概况"},{"city":"北京","date":"2026-05-12","section":"趋势分析"}]
```

| 事件 | 说明 |
|------|------|
| `delta` | LLM 增量输出文本，前端逐字追加渲染 |
| `done` | 流结束，data 为 JSON 数组，包含引用的天气报告来源 |

#### 非流式响应格式

当 `stream: false` 时，同样走 SSE 协议（单个 delta + done），由前端兼容解析。
