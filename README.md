# 天气数据分析系统

基于 Spring Boot + Vue 3 的天气数据采集、展示与分析平台，集成和风天气 API，支持多城市天气实时监控与历史数据对比分析。

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端框架 | Spring Boot 3.2.5 |
| ORM | MyBatis-Plus 3.5.6 |
| 数据库 | MySQL |
| 认证 | JWT (jjwt 0.12) |
| 前端框架 | Vue 3 + TypeScript |
| 构建工具 | Vite 5 |
| UI 组件库 | Element Plus |
| 图表 | ECharts 6 |
| 状态管理 | Pinia |
| 路由 | Vue Router 4 |

## 功能模块

- **用户认证** — 注册、登录、JWT Token 鉴权
- **仪表盘** — 关注城市天气概览、实时数据总览
- **天气查询** — 实时天气、7 天预报、历史数据查询
- **数据分析** — 单城市统计、多城市对比分析
- **城市管理** — 关注城市列表，支持搜索添加
- **定时采集** — 每 30 分钟自动拉取实时天气，每日 8 点拉取预报

## 项目结构

```
weather-analysis-system/
├── weather-server/                 # 后端 Spring Boot
│   ├── src/main/java/com/weather/
│   │   ├── config/                 # 拦截器、定时任务、异常处理
│   │   ├── controller/             # REST 控制器
│   │   ├── dto/                    # 请求/响应 DTO
│   │   ├── entity/                 # 实体类
│   │   ├── mapper/                 # MyBatis Mapper
│   │   ├── service/                # 业务逻辑
│   │   └── util/                   # JWT、和风天气 API 客户端
│   └── src/main/resources/
│       └── application.yml         # 应用配置
├── weather-web/                    # 前端 Vue 3
│   └── src/
│       ├── api/                    # API 请求封装
│       ├── components/             # 公共组件
│       ├── router/                 # 路由配置
│       ├── stores/                 # Pinia 状态管理
│       └── views/                  # 页面视图
└── .gitignore
```

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.8+
- Node.js 18+
- MySQL 8.0+

### 数据库初始化

运行 `sql/weather_analysis.sql` 即可创建数据库和表结构。

### 环境变量

前往 [和风天气](https://www.qweather.com/) 注册并获取 API Key，然后设置环境变量：

```bash
# Linux / macOS
export QWATHER_API_KEY=your_api_key

# Windows (PowerShell)
$env:QWATHER_API_KEY="your_api_key"

# Windows (CMD)
set QWATHER_API_KEY=your_api_key
```

### 后端启动

```bash
cd weather-server
mvn spring-boot:run
```

服务启动在 `http://localhost:8080`

### 前端启动

```bash
cd weather-web
npm install
npm run dev
```

开发服务器启动在 `http://localhost:3000`，API 请求自动代理到后端。

### 修改数据库配置

编辑 `weather-server/src/main/resources/application.yml`，按实际环境修改数据库连接信息：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/weather_analysis
    username: root
    password: 123456
```

## API 接口

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
| GET/POST | `/api/cities/*` | 城市管理 | 是 |
