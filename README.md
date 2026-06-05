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

后端、AI 服务和前端是三个长期运行进程，建议分别打开三个终端执行：

```bash
make backend
make ai
make frontend
```

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

推荐在项目根目录执行：

```bash
make backend
```

等价于：

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

推荐在项目根目录执行：

```bash
make ai
```

等价于：

```bash
cd /home/bird/Projects/DBwork/ai-service
.venv/bin/uvicorn app.main:app --host 0.0.0.0 --port 8000
```

AI 健康检查：`http://localhost:8000/health`

如需启用 DeepSeek，把本地配置写入 `ai-service/.env` 或 `ai-service/.env.local`：

```text
DEEPSEEK_API_KEY=你的 DeepSeek API Key
DEEPSEEK_BASE_URL=https://api.deepseek.com
DEEPSEEK_MODEL=deepseek-v4-flash
```

AI 助手页面可选择 `deepseek-v4-flash` 或 `deepseek-v4-pro`。未配置 Key 或调用失败时，系统会自动回退到本地规则分析。

### 4. 启动 Vue 前端

首次运行先安装依赖：

```bash
cd /home/bird/Projects/DBwork/frontend
npm install
```

启动前端：

推荐在项目根目录执行：

```bash
make frontend
```

等价于：

```bash
cd /home/bird/Projects/DBwork/frontend
npm run dev -- --port 5173
```

前端地址：`http://localhost:5173`

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

## 真实线路与运营数据

第一版 MVP 使用 3 条真实公交线路：

- `data/real_routes/route_1.json`
- `data/real_routes/route_2.json`
- `data/real_routes/route_3.json`

### 后台手动加入基础数据

启动前后端后，进入前端后台：

```text
http://localhost:5173/admin/data
```

使用管理员账号登录。

可以在“基础数据”页面手动新增或维护：

- 线路
- 站点
- 车辆
- 司机
- 班次

注意：后台页面适合维护基础数据；大量乘车记录和演示客流数据建议用脚本生成。

### 用脚本重建线路和客流

在项目根目录执行：

```bash
cd /home/bird/Projects/DBwork
python3 scripts/import_route_data.py
python3 scripts/generate_mock_data.py --end-date 2026-06-04 --days 30
```

说明：

- `import_route_data.py` 会导入 `data/real_routes/route_*.json` 中的线路和站点。
- `generate_mock_data.py` 会生成车辆、司机、班次和乘车记录。
- `--end-date` 是生成数据的最后一天。
- `--days` 是向前生成多少天数据。

例如生成 2026-06-01 到 2026-06-30 的 30 天数据：

```bash
python3 scripts/generate_mock_data.py --end-date 2026-06-30 --days 30
```

注意：当前生成脚本会重建演示运营数据，适合课程演示和反复调参。

### 前端选择统计区间

首页地图态势页和客流分析页都可以选择日期范围。页面只会统计数据库中已有日期的数据。

如果页面选择 2026-06-01 到 2026-06-30，数据库也需要先生成或导入这个区间的数据，否则图表和指标会为空或偏少。

更多新线路、新日期和真实刷卡数据导入方式见 `docs/data-import-guide.md`。

## 关键文档

- `docs/requirements-v1.md`：需求文档
- `docs/mvp-v1.md`：MVP 范围
- `docs/database-design.md`：数据库设计
- `docs/map-integration.md`：腾讯地图接入
- `docs/data-import-guide.md`：线路与运营客流数据导入说明
- `docs/task-breakdown-v1.md`：阶段 1 到阶段 6 任务拆分
- `docs/execution-workflow.md`：任务执行规范
- `docs/next.md`：当前任务

## 开发阶段

阶段 1 到阶段 6 是开发阶段，完成后项目应达到可本地运行、可联调、可演示状态。

阶段 7 由用户自行测试、截图、验收和整理课程报告素材。
