# 城市公交客流分析与智能调度 Agent 平台 MVP v1

## 1. MVP 目标

第一版 MVP 的目标是做出一个可运行、可演示、可写进简历的完整闭环系统。

MVP 需要证明以下能力：

- 游客可以免登录查看公交线路、地图和客流分析
- 管理员可以登录后台维护核心数据
- 系统可以基于 3 条真实线路生成模拟运营数据
- 系统可以用图表展示客流趋势和站点客流
- AI 微服务可以基于真实统计数据生成自然语言分析和调度建议
- MySQL 数据库设计能体现 ER 建模、主外键、索引、复杂 SQL 和事务

## 2. MVP 技术形态

项目采用 Monorepo 结构。

```text
DBwork/
├── frontend/
├── backend/
├── ai-service/
├── sql/
├── scripts/
├── docs/
├── data/
└── docker-compose.yml
```

运行方式：

- 前端本地运行：`npm run dev`
- Java 后端本地运行：IDEA 或 Maven 启动
- Python AI 服务本地运行：`uvicorn app.main:app --reload`
- MySQL 和 Redis 通过 Docker Compose 运行

## 3. MVP 页面范围

### 3.1 游客前台页面

第一版游客前台只包含 4 个页面：

- 首页地图
- 线路详情
- 客流分析
- AI 助手

#### 首页地图看板

必须实现：

- 展示腾讯地图
- 通过 `VITE_TENCENT_MAP_KEY` 加载腾讯地图 JavaScript API GL
- 展示 3 条公交线路的列表
- 选择线路后在地图上显示站点 Marker
- 选择线路后在地图上绘制线路折线
- 展示基础指标：线路数量、站点数量、今日客流量
- 从线路列表中选择线路并跳转到线路详情

#### 线路详情页

必须实现：

- 展示线路名称、起点、终点、方向、运营状态
- 展示站点顺序列表
- 点击站点显示站点基础信息
- 展示该线路最近 30 天客流趋势

#### 客流分析页

必须实现：

- 按线路查看日客流趋势
- 查看早高峰、晚高峰、正常时段客流对比
- 查看站点上车客流 TOP 排行
- 使用 ECharts 展示图表

#### AI 分析助手页

必须实现：

- 输入自然语言问题
- 选择分析线路或时间范围
- 调用 AI 分析接口
- 展示 AI 返回的分析结论和调度建议

### 3.2 管理员后台页面

第一版管理员后台只包含 4 个页面：

- 管理员登录页
- 后台首页
- 基础数据管理
- 调度建议管理

#### 管理员登录页

必须实现：

- 账号密码登录
- 登录失败提示
- 登录成功后进入后台
- 未登录访问后台时跳转登录页

#### 后台首页

必须实现：

- 展示基础数据统计
- 展示今日客流概览
- 展示快捷入口

#### 基础数据管理页

MVP 将多个管理模块合并到一个后台管理页中，用菜单或 Tabs 切换，不为每个模块单独拆独立页面。

必须实现：

- 线路管理
- 站点管理
- 车辆管理
- 司机管理
- 班次管理
- 乘车记录查询

每个管理模块至少支持：

- 列表查询
- 新增
- 修改
- 删除或停用

#### 调度建议管理页

必须实现：

- 选择线路
- 选择时间范围
- 选择分析时段：早高峰、晚高峰、正常时段
- 生成调度建议
- 展示建议内容
- 保存调度建议记录

## 4. MVP 后端接口范围

### 4.1 公开接口

游客可以调用：

- `GET /api/public/routes`
- `GET /api/public/routes/{id}`
- `GET /api/public/routes/{id}/stations`
- `GET /api/public/statistics/route-flow`
- `GET /api/public/statistics/station-flow`
- `POST /api/public/ai/analyze`

### 4.2 管理员接口

管理员登录：

- `POST /api/admin/auth/login`
- `POST /api/admin/auth/logout`
- `GET /api/admin/auth/me`

基础数据管理：

- `GET /api/admin/routes`
- `POST /api/admin/routes`
- `PUT /api/admin/routes/{id}`
- `DELETE /api/admin/routes/{id}`
- `GET /api/admin/stations`
- `POST /api/admin/stations`
- `PUT /api/admin/stations/{id}`
- `DELETE /api/admin/stations/{id}`
- `GET /api/admin/vehicles`
- `POST /api/admin/vehicles`
- `PUT /api/admin/vehicles/{id}`
- `DELETE /api/admin/vehicles/{id}`
- `GET /api/admin/drivers`
- `POST /api/admin/drivers`
- `PUT /api/admin/drivers/{id}`
- `DELETE /api/admin/drivers/{id}`

班次与乘车记录：

- `GET /api/admin/schedules`
- `POST /api/admin/schedules`
- `PUT /api/admin/schedules/{id}`
- `DELETE /api/admin/schedules/{id}`
- `GET /api/admin/ride-records`

统计与调度：

- `GET /api/admin/statistics/route-flow`
- `GET /api/admin/statistics/station-flow`
- `GET /api/admin/statistics/peak-summary`
- `POST /api/admin/dispatch-advice/generate`
- `GET /api/admin/dispatch-advice`

模拟数据：

- `POST /api/admin/mock-data/generate`

## 5. MVP AI 服务范围

Python FastAPI AI 微服务第一版只提供两个核心接口。

```text
POST /ai/analyze-flow
POST /ai/dispatch-advice
```

### 5.1 客流分析接口

输入：

- 用户问题
- 线路信息
- 时间范围
- 后端统计结果

输出：

- 分析摘要
- 关键数据说明
- 客流变化判断
- 可读的自然语言结论

### 5.2 调度建议接口

输入：

- 线路信息
- 分析时段
- 班次数量
- 客流量
- 车辆容量
- 满载率
- 站点客流排行

输出：

- 调度建议
- 建议原因
- 风险提示
- 建议优先级

## 6. MVP 数据库范围

MVP 必须包含以下表：

- `admin_user`
- `bus_route`
- `station`
- `route_station`
- `bus_vehicle`
- `driver`
- `bus_schedule`
- `ride_record`
- `dispatch_advice`
- `operation_log`

MVP 必须体现：

- 每张核心表有主键
- 线路编号、站点编号、车牌号、司机工号、管理员账号有唯一约束
- 线路与站点通过 `route_station` 建立多对多关系
- 班次关联线路、车辆和司机
- 乘车记录关联线路、班次、上车站点和下车站点
- `ride_record` 针对线路、站点、乘车时间建立索引
- 班次保存、车辆状态更新等关键业务使用事务

## 7. MVP 数据规模

真实数据：

- 3 条真实公交线路
- 真实站点名称和经纬度

模拟数据：

- 30 辆公交车
- 30 名司机
- 30 天运营数据
- 每天约 100 条班次
- 总计约 1 万条乘车记录
- 车辆默认容量为 50 人
- 早高峰为 07:00-09:00，核心高峰为 07:30-08:30
- 晚高峰为 17:00-19:00，核心高峰为 17:30-18:30

## 8. MVP 验收标准

### 8.1 游客验收

- 游客无需登录即可打开首页
- 首页能展示腾讯地图
- 腾讯地图 API Key 通过环境变量读取，不在业务源码中硬编码
- 选择线路后能展示站点和线路折线
- 游客能查看线路详情和站点顺序
- 游客能查看客流趋势图表
- 游客能输入自然语言问题并获得 AI 分析结果

### 8.2 管理员验收

- 管理员可以登录后台
- 未登录不能访问后台页面
- 管理员可以维护线路、站点、车辆、司机、班次数据
- 管理员可以查询乘车记录
- 管理员可以生成模拟数据
- 管理员可以生成并保存调度建议

### 8.3 数据库验收

- 建表 SQL 可以在 Docker MySQL 中成功执行
- 初始化数据可以成功导入
- 3 条线路和站点关系正确
- 模拟数据规模符合要求
- 复杂统计 SQL 可以返回正确结果
- 外键和唯一约束能阻止明显非法数据

### 8.4 AI 验收

- AI 回答必须基于后端统计数据
- AI 结果中需要包含关键指标或数据依据
- AI 可以区分早晚高峰模板和正常时段模板
- AI 调度建议采用默认阈值：高峰满载率大于等于 85% 建议加车，正常时段满载率小于 30% 建议减班或延长间隔，客流高于近 7 日均值 30% 标记为异常增长
- AI 不应声称使用了系统中不存在的数据

## 9. MVP 暂不实现内容

第一版暂不实现：

- 真实车辆 GPS 定位
- 乘客实名注册
- 司机端页面
- 真实刷卡支付数据接入
- 多 Agent 协作
- 自动生成完整路径规划
- 生产服务器部署
- 复杂权限角色体系

## 10. 建议开发顺序

1. 创建项目目录结构和 Docker Compose
2. 设计 MySQL 表结构和 ER 图
3. 导入 3 条真实公交线路和站点数据
4. 编写模拟车辆、司机、班次和乘车记录生成脚本
5. 实现 Java 后端基础 CRUD 和统计接口
6. 实现 Vue 前台地图、线路详情和客流图表
7. 实现管理员登录和后台管理页面
8. 实现 Python AI 微服务
9. 联调 AI 分析与调度建议
10. 整理项目文档、截图和演示说明
