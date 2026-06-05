# 城市公交客流分析与智能调度 Agent 平台

这是一个面向城市公交场景的完整 Web 项目，目标是作为数据库课程项目和实习简历项目使用。

系统采用“游客免登录 + 管理员登录”的双身份模式：

- 游客：查看公交线路、地图、客流分析，并使用 AI 助手进行自然语言客流分析。
- 管理员：登录后台维护线路、站点、车辆、司机、班次和乘车记录，并生成调度建议。

## 技术栈

- 前端：Vue 3、TypeScript、Vite、Element Plus、ECharts、腾讯地图 JavaScript API GL
- Java 后端：Spring Boot、MyBatis Plus、Sa-Token、Knife4j
- AI 微服务：Python、FastAPI、大模型 API
- 数据库：MySQL 8.0
- 缓存：Redis 7
- 基础设施：Docker Compose

## 目录结构

```text
DBwork/
├── frontend/          Vue 前端
├── backend/           Spring Boot 后端
├── ai-service/        FastAPI AI 微服务
├── sql/               数据库脚本
├── scripts/           数据导入和模拟数据脚本
├── data/              真实线路数据和模拟数据
├── docs/              项目控制文档和设计文档
├── docker-compose.yml MySQL + Redis 基础设施
└── AGENTS.md          AI 开发工作流规则
```

## 当前启动方式

项目前 6 个开发阶段已完成，当前可本地启动 Docker 基础设施、Java 后端、Python AI 服务和 Vue 前端。

### 1. 启动 MySQL 和 Redis

```bash
cd /home/bird/Projects/DBwork
docker compose up -d
docker compose ps
```

基础设施端口：

- MySQL：`localhost:3307`
- Redis：`localhost:6380`

数据库开发账号：

```text
DB_NAME=bus_agent
DB_USERNAME=bus_agent
DB_PASSWORD=bus_agent123
```

### 2. 启动 Java 后端

```bash
cd /home/bird/Projects/DBwork/backend
./mvnw spring-boot:run
```

后端地址：

- 健康检查：`http://localhost:8080/api/health`
- OpenAPI：`http://localhost:8080/v3/api-docs`
- Knife4j/Swagger UI：`http://localhost:8080/swagger-ui.html`

### 3. 启动 Python AI 服务

首次运行先安装依赖：

```bash
cd /home/bird/Projects/DBwork/ai-service
python3 -m venv .venv
.venv/bin/pip install -r requirements.txt
```

启动服务：

```bash
cd /home/bird/Projects/DBwork/ai-service
.venv/bin/uvicorn app.main:app --host 0.0.0.0 --port 8000
```

AI 健康检查：`http://localhost:8000/health`

### 4. 启动 Vue 前端

首次运行先安装依赖：

```bash
cd /home/bird/Projects/DBwork/frontend
npm install
```

启动前端：

```bash
cd /home/bird/Projects/DBwork/frontend
npm run dev -- --port 5173
```

前端地址：`http://localhost:5173`

管理员账号：

```text
用户名：wjhadmin
密码：123456
```

## 环境变量

环境变量示例文件：

- `frontend/.env.example`
- `backend/.env.example`
- `ai-service/.env.example`

前端腾讯地图 Key 使用：

```text
VITE_TENCENT_MAP_KEY
```

后端本地连接 Redis 时使用：

```text
REDIS_HOST=localhost
REDIS_PORT=6380
```

后端 AI 服务地址：

```text
AI_SERVICE_BASE_URL=http://localhost:8000
```

如果未配置腾讯地图 Key，前端地图组件会显示站点走向降级视图，项目仍可演示线路、统计图表和 AI 分析。

## 真实线路数据

第一版 MVP 使用 3 条真实公交线路：

- `data/real_routes/route_1.json`
- `data/real_routes/route_2.json`
- `data/real_routes/route_3.json`

## 关键文档

- `docs/requirements-v1.md`：需求文档
- `docs/mvp-v1.md`：MVP 范围
- `docs/database-design.md`：数据库设计
- `docs/map-integration.md`：腾讯地图接入
- `docs/task-breakdown-v1.md`：阶段 1 到阶段 6 任务拆分
- `docs/execution-workflow.md`：任务执行规范
- `docs/next.md`：当前任务

## 开发阶段

阶段 1 到阶段 6 是开发阶段，完成后项目应达到可本地运行、可联调、可演示状态。

阶段 7 由用户自行测试、截图、验收和整理课程报告素材。
