# 执行记录

## 2026-06-06 项目验收完成与仓库收尾

- 任务：根据用户已完成的最终验收结果，更新项目状态文档并整理仓库忽略规则。
- 变更：`docs/status.md` 更新为“浏览器人工验收已完成、项目可结项”；`docs/next.md` 收口为“当前无新的实现任务”；`.gitignore` 补充本地 AI 工具目录、编辑器目录、日志、缓存和临时文件忽略规则，避免将开发过程产物上传到仓库。
- 检查：已核查当前控制文档、执行记录和仓库根目录文件；本次仅修改文档和忽略规则，未改动业务代码。
- 结果：通过，仓库进入最终 Git 提交和课程项目文档整理阶段。

## 2026-06-06 MVP v2 增强完成

- 任务：按 `docs/mvp-v2.md` 完成答辩展示和简历技术亮点增强。
- 变更：首页站点压力阈值和空数据告警补强；客流分析页新增日均客流、高峰占比、线路对比、自动分析说明和趋势平均线；AI 助手按结构化调度报告展示，并增强 DeepSeek Prompt、Python 本地规则降级和 Java 后端 AI 不可用降级文案；后台调度建议页新增指标依据、规则依据、AI 风险提示、热点站点 TOP 3 和历史详情展开；新增 `docs/demo-guide.md`、`docs/resume-highlights.md` 并更新 README、状态和当前任务文档。
- 检查：`cd frontend && npm run build` 通过，保留既有 Vite chunk 体积警告；`python3 -m py_compile ai-service/app/*.py` 通过；`cd backend && ./mvnw test` 通过；Docker MySQL/Redis healthy；后端 `/api/health` 返回数据库和 Redis 均 `up`；AI `/health` 返回 `up`；前端 `http://localhost:5173/` 返回 200；公开线路和 AI 分析接口验证通过。
- 结果：通过，进入浏览器人工验收和截图材料整理。

## 2026-06-05 收尾前初始化凭据展示清理

- 任务：移除前端页面、README 和普通说明文档中公开展示的初始化管理员凭据。
- 变更：后台登录页取消账号密码预填和演示凭据提示；README 与说明文档改为不直接展示初始化凭据；数据库初始化账号、密码哈希和认证逻辑未修改。
- 检查：已核查 README、前端源码和普通说明文档不再包含初始化管理员明文凭据；`cd frontend && npm run build` 通过，保留既有 Vite chunk 体积警告。
- 结果：通过，进入最终验收收尾。

## 2026-06-05 收尾启动命令与项目检查

- 任务：新增根目录 Makefile，提供 `make frontend`、`make backend`、`make ai` 三个启动命令，并检查当前项目收尾风险。
- 变更：新增 `Makefile`；README 补充 Makefile 快速启动方式和三个服务需分别在独立终端运行的说明。
- 检查：`make -n frontend`、`make -n backend`、`make -n ai` 均输出正确命令；`python3 -m py_compile ai-service/app/*.py` 通过；`cd frontend && npm run build` 通过，保留既有 Vite chunk 体积警告；`cd backend && ./mvnw test` 通过；未发现 `sk-` 形式密钥写入仓库文件。
- 结果：通过。

## 2026-06-05 首页统计区间与数据补充说明

- 任务：首页增加日期范围选择器，并在 README 中说明后续如何补充后台/数据库数据。
- 变更：`HomeMap.vue` 将统计区间改为可选日期范围，切换区间时清空首页缓存并重新加载总览、线路和站点统计；`styles.css` 补充日期选择器布局；README 增加后台基础数据维护、脚本生成运营客流和前端统计区间选择说明。
- 检查：`cd frontend && npm run build` 通过，保留既有 Vite chunk 体积警告。
- 结果：通过，等待用户浏览器复测。

## 2026-06-05 DeepSeek 接入 AI 助手

- 任务：将 DeepSeek 接入 AI 助手，使回答围绕当前线路、日期范围和现有客流统计数据生成调度分析。
- 变更：新增 `ai-service/app/deepseek_client.py`；AI 服务优先调用 DeepSeek，失败保留本地规则降级；AI 助手页面增加模型选择；Java 后端透传可选模型字段；更新 README 和 AI 服务环境变量示例。
- 检查：`python3 -m py_compile ai-service/app/*.py` 通过；AI 服务未配置 Key 的规则降级验证通过；`cd frontend && npm run build` 通过，保留既有 Vite chunk 体积警告；`cd backend && ./mvnw test` 通过。
- 结果：通过。真实 DeepSeek 外网调用需用户在本机配置 API Key 后复测。

## 2026-06-05 阶段 7 首页右侧面板透底修复

- 任务：修复地图态势页右侧线路/站点面板从站点间隙透出下层内容的问题。
- 变更：`styles.css` 中右侧线路状态面板改为不透明实体背景，站点列表和线路切换区增加连续底色与边框，并对面板内容做裁剪。
- 检查：`cd frontend && npm run build` 通过，保留既有 Vite chunk 体积警告。
- 结果：通过，等待用户浏览器复测。

## 2026-06-05 阶段 7 首页指标口径与线路切换修复

- 任务：修复首页“公交客流调度态势”指标口径重复和线路切换卡顿问题。
- 变更：`HomeMap.vue` 左侧浮层改为当前选中线路口径，顶部总览继续保留全局口径；线路切换增加站点数据缓存、线路统计复用和请求竞态保护。
- 检查：`cd frontend && npm run build` 通过，保留既有 Vite chunk 体积警告。
- 结果：通过，等待用户浏览器复测腾讯地图显示和切换手感。

## 2026-06-05 阶段 7 状态整理与待修复 BUG 记录

- 任务：检查当前项目状态，整理用户新增的首页指标口径和线路切换卡顿问题，并清理明显无用文档文件。
- 变更：更新 `docs/status.md` 和 `docs/next.md`，记录当前进度、待处理 BUG、数据补充入口和后续修复边界；删除 Windows 下载元数据文件 `docs/assets/frontend-map-driven-reference.png:Zone.Identifier`。
- 检查：已按 `AGENTS.md` 顺序读取控制文档，并检查首页、地图组件、统计服务封装和数据生成脚本。
- 结果：文档已更新；本次未修改业务代码。

## 2026-06-05 阶段 7 用户自测修复

- 任务：修复地图站点标记样式和颜色、高峰客流统计展示不明显的问题。
- 变更：地图站点标记改为小图钉并按相对客流显示红/黄/绿；首页和线路详情接入站点客流；高峰统计按班次聚合并展示班均客流；模拟数据生成按早晚高峰加权。
- 检查：`npm run build` 通过；`./mvnw test` 在非沙箱环境通过；`python3 -m py_compile scripts/generate_mock_data.py` 通过；模拟数据重新导入 MySQL 后验证 3 条线路早晚高峰班均客流高于正常时段。
- 结果：通过。

## 2026-06-04 任务拆分阶段

- 任务：记录阶段 1 到阶段 6 的详细任务拆分，并固化任务执行闭环。
- 变更：新增 `docs/execution-workflow.md` 和 `docs/task-breakdown-v1.md`；更新 `docs/rules.md`、`docs/status.md`、`docs/next.md`、`docs/decisions.md`。
- 检查：已对 `docs/execution-workflow.md`、`docs/task-breakdown-v1.md`、`docs/rules.md`、`docs/status.md`、`docs/next.md`、`docs/decisions.md`、`docs/log.md` 进行 UTF-8 读取校验。
- 结果：通过。

## 2026-06-04 阶段 1 完成

- 任务：项目初始化与基础设施。
- 变更：创建 `frontend/`、`backend/`、`ai-service/`、`sql/`、`scripts/`，新增 `docker-compose.yml`、环境变量示例文件和 `README.md`。
- 检查：`docker compose config` 通过；MySQL 8.0 和 Redis 7 通过 WSL Docker Compose 启动并显示 healthy；README 关键内容校验通过。
- 结果：通过。

## 2026-06-04 阶段 3 完成

- 任务：Java 后端开发。
- 变更：完成公开统计接口、公开 AI 分析接口、后台统计接口、调度建议生成与查询接口、AI 服务客户端降级逻辑和本地 CORS 配置。
- 检查：`./mvnw test` 通过；后端启动成功；公开统计接口、公开 AI 降级接口、管理员登录、调度建议生成保存、OpenAPI 文档均验证成功。
- 结果：通过。

## 2026-06-04 阶段 4 完成

- 任务：Python AI 微服务开发。
- 变更：创建 FastAPI 应用、Schema、客流分析接口、调度建议接口和模板降级逻辑。
- 检查：AI 服务启动成功；`/health`、`/ai/analyze-flow`、`/ai/dispatch-advice` 调用成功；Java 后端调用 AI 服务成功。
- 结果：通过。

## 2026-06-04 阶段 5 完成

- 任务：Vue 前端开发。
- 变更：创建 Vue 3 + Vite + TypeScript 前端，实现游客 4 页和管理员 4 页，接入 Element Plus、ECharts、地图组件、登录守卫和 API 请求封装。
- 检查：`npm install` 成功；`npm run build` 通过；前端开发服务器启动成功；8 个页面路由 HTTP 访问成功。
- 结果：通过；当前环境没有浏览器二进制，地图和图表最终视觉检查需在可用浏览器环境完成。

## 2026-06-04 阶段 6 完成

- 任务：前后端与 AI 联调。
- 变更：修正空统计日期返回、清理前端构建临时产物、更新 README 启动顺序。
- 检查：Docker MySQL/Redis healthy；后端健康检查通过；AI 服务健康检查通过；前端路由可访问；游客线路、统计、AI 助手、管理员登录、基础数据 CRUD、调度建议生成、AI 不可用降级均已验证。
- 结果：通过；项目进入阶段 7 用户自测与验收。
