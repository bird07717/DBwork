# 城市公交客流分析与智能调度 Agent 平台数据库设计 v1

## 1. 设计目标

本数据库设计服务于第一版 MVP，重点支撑 3 条真实公交线路的地图展示、后台基础数据管理、模拟班次与乘车记录生成、客流统计分析和 AI 调度建议生成。

数据库使用 MySQL 8.0，第一版重点体现：

- ER 建模
- 主键约束
- 外键约束
- 唯一约束
- 索引设计
- 复杂 SQL 查询
- 事务处理

第一版暂不强制实现：

- 触发器
- 存储过程
- 视图

## 2. 核心实体

核心实体包括：

- 管理员
- 公交线路
- 公交站点
- 线路站点关联
- 公交车辆
- 司机
- 班次
- 乘车记录
- 调度建议
- 操作日志

核心关系：

- 一条线路可以包含多个站点
- 一个站点可以属于多条线路
- 线路和站点通过 `route_station` 建立多对多关系
- 一条线路可以有多个班次
- 一个班次对应一辆车和一名司机
- 乘车记录关联线路、班次、上车站点和下车站点
- 调度建议基于线路、时间范围、分析时段和客流统计结果生成

## 3. 表结构设计

### 3.1 `admin_user` 管理员表

用于保存后台管理员账号。

| 字段名 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT | PK, AUTO_INCREMENT | 管理员 ID |
| username | VARCHAR(50) | NOT NULL, UNIQUE | 登录账号 |
| password_hash | VARCHAR(100) | NOT NULL | 加密后的密码 |
| real_name | VARCHAR(50) | NOT NULL | 管理员姓名 |
| status | TINYINT | NOT NULL, DEFAULT 1 | 状态：1 启用，0 禁用 |
| last_login_time | DATETIME | NULL | 最后登录时间 |
| create_time | DATETIME | NOT NULL | 创建时间 |
| update_time | DATETIME | NOT NULL | 更新时间 |

### 3.2 `bus_route` 公交线路表

用于保存 3 条真实公交线路的基础信息。

| 字段名 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT | PK, AUTO_INCREMENT | 线路 ID |
| route_code | VARCHAR(30) | NOT NULL, UNIQUE | 线路编号 |
| route_name | VARCHAR(100) | NOT NULL | 线路名称 |
| direction | VARCHAR(50) | NOT NULL | 线路方向 |
| start_station_name | VARCHAR(100) | NOT NULL | 起点站名称 |
| end_station_name | VARCHAR(100) | NOT NULL | 终点站名称 |
| operation_start_time | TIME | NULL | 首班时间 |
| operation_end_time | TIME | NULL | 末班时间 |
| status | TINYINT | NOT NULL, DEFAULT 1 | 状态：1 运营，0 停用 |
| create_time | DATETIME | NOT NULL | 创建时间 |
| update_time | DATETIME | NOT NULL | 更新时间 |

### 3.3 `station` 站点表

用于保存公交站点信息和腾讯地图展示所需经纬度。

| 字段名 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT | PK, AUTO_INCREMENT | 站点 ID |
| station_code | VARCHAR(30) | NOT NULL, UNIQUE | 站点编号 |
| station_name | VARCHAR(100) | NOT NULL | 站点名称 |
| area_name | VARCHAR(100) | NULL | 所属区域 |
| longitude | DECIMAL(10,6) | NOT NULL | 经度 |
| latitude | DECIMAL(10,6) | NOT NULL | 纬度 |
| status | TINYINT | NOT NULL, DEFAULT 1 | 状态：1 启用，0 停用 |
| create_time | DATETIME | NOT NULL | 创建时间 |
| update_time | DATETIME | NOT NULL | 更新时间 |

### 3.4 `route_station` 线路站点关联表

用于描述线路经过哪些站点以及站点顺序。

| 字段名 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT | PK, AUTO_INCREMENT | 关联 ID |
| route_id | BIGINT | NOT NULL, FK | 线路 ID，关联 `bus_route.id` |
| station_id | BIGINT | NOT NULL, FK | 站点 ID，关联 `station.id` |
| station_order | INT | NOT NULL | 站点在线路中的顺序 |
| distance_from_start | DECIMAL(8,2) | NULL | 距起点距离，单位 km |
| create_time | DATETIME | NOT NULL | 创建时间 |

唯一约束：

- `uk_route_station_order(route_id, station_order)`
- `uk_route_station(route_id, station_id)`

### 3.5 `bus_vehicle` 车辆表

用于保存模拟车辆信息。

| 字段名 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT | PK, AUTO_INCREMENT | 车辆 ID |
| vehicle_code | VARCHAR(30) | NOT NULL, UNIQUE | 车辆编号 |
| plate_no | VARCHAR(20) | NOT NULL, UNIQUE | 车牌号 |
| capacity | INT | NOT NULL | 车辆核载人数 |
| status | TINYINT | NOT NULL, DEFAULT 1 | 状态：1 可用，2 运行中，0 停用 |
| create_time | DATETIME | NOT NULL | 创建时间 |
| update_time | DATETIME | NOT NULL | 更新时间 |

### 3.6 `driver` 司机表

用于保存模拟司机信息。

| 字段名 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT | PK, AUTO_INCREMENT | 司机 ID |
| employee_no | VARCHAR(30) | NOT NULL, UNIQUE | 工号 |
| driver_name | VARCHAR(50) | NOT NULL | 司机姓名 |
| phone | VARCHAR(20) | NOT NULL | 手机号 |
| status | TINYINT | NOT NULL, DEFAULT 1 | 状态：1 可用，2 出车中，0 停用 |
| create_time | DATETIME | NOT NULL | 创建时间 |
| update_time | DATETIME | NOT NULL | 更新时间 |

### 3.7 `bus_schedule` 班次表

用于保存每天约 100 条模拟班次。

| 字段名 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT | PK, AUTO_INCREMENT | 班次 ID |
| route_id | BIGINT | NOT NULL, FK | 线路 ID，关联 `bus_route.id` |
| vehicle_id | BIGINT | NOT NULL, FK | 车辆 ID，关联 `bus_vehicle.id` |
| driver_id | BIGINT | NOT NULL, FK | 司机 ID，关联 `driver.id` |
| schedule_date | DATE | NOT NULL | 班次日期 |
| depart_time | DATETIME | NOT NULL | 发车时间 |
| period_type | VARCHAR(20) | NOT NULL | 时段类型：morning_peak、evening_peak、normal |
| status | TINYINT | NOT NULL, DEFAULT 1 | 状态：1 已计划，2 已完成，0 已取消 |
| create_time | DATETIME | NOT NULL | 创建时间 |
| update_time | DATETIME | NOT NULL | 更新时间 |

建议唯一约束：

- `uk_vehicle_depart(vehicle_id, depart_time)`
- `uk_driver_depart(driver_id, depart_time)`

### 3.8 `ride_record` 乘车记录表

用于保存 30 天约 1 万条模拟乘车记录，是客流统计和 AI 分析的核心数据来源。

| 字段名 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT | PK, AUTO_INCREMENT | 乘车记录 ID |
| route_id | BIGINT | NOT NULL, FK | 线路 ID，关联 `bus_route.id` |
| schedule_id | BIGINT | NULL, FK | 班次 ID，关联 `bus_schedule.id` |
| boarding_station_id | BIGINT | NOT NULL, FK | 上车站点 ID，关联 `station.id` |
| alighting_station_id | BIGINT | NULL, FK | 下车站点 ID，关联 `station.id` |
| ride_time | DATETIME | NOT NULL | 乘车时间 |
| period_type | VARCHAR(20) | NOT NULL | 时段类型：morning_peak、evening_peak、normal |
| pay_type | VARCHAR(20) | NOT NULL, DEFAULT 'mock' | 支付方式或数据来源 |
| create_time | DATETIME | NOT NULL | 创建时间 |

### 3.9 `dispatch_advice` 调度建议表

用于保存管理员生成的 AI 调度建议。

| 字段名 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT | PK, AUTO_INCREMENT | 建议 ID |
| route_id | BIGINT | NOT NULL, FK | 线路 ID，关联 `bus_route.id` |
| start_date | DATE | NOT NULL | 分析开始日期 |
| end_date | DATE | NOT NULL | 分析结束日期 |
| period_type | VARCHAR(20) | NOT NULL | 分析时段 |
| avg_load_rate | DECIMAL(5,2) | NULL | 平均满载率 |
| passenger_count | INT | NOT NULL, DEFAULT 0 | 统计客流量 |
| advice_level | VARCHAR(20) | NOT NULL | 建议级别：low、medium、high |
| advice_content | TEXT | NOT NULL | 建议内容 |
| ai_summary | TEXT | NULL | AI 生成的说明 |
| create_time | DATETIME | NOT NULL | 创建时间 |

### 3.10 `operation_log` 操作日志表

用于记录管理员关键操作。

| 字段名 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT | PK, AUTO_INCREMENT | 日志 ID |
| admin_id | BIGINT | NULL, FK | 管理员 ID，关联 `admin_user.id` |
| operation_type | VARCHAR(50) | NOT NULL | 操作类型 |
| operation_content | VARCHAR(255) | NOT NULL | 操作内容 |
| request_path | VARCHAR(255) | NULL | 请求路径 |
| create_time | DATETIME | NOT NULL | 创建时间 |

## 4. 索引设计

必须建立的索引：

| 表名 | 索引名 | 字段 | 说明 |
| --- | --- | --- | --- |
| bus_route | uk_route_code | route_code | 线路编号唯一 |
| station | uk_station_code | station_code | 站点编号唯一 |
| route_station | uk_route_station_order | route_id, station_order | 保证同一线路站点顺序唯一 |
| route_station | idx_station_id | station_id | 查询某站点所属线路 |
| bus_schedule | idx_schedule_route_date | route_id, schedule_date | 按线路和日期查询班次 |
| bus_schedule | idx_schedule_depart_time | depart_time | 按发车时间查询班次 |
| ride_record | idx_ride_route_time | route_id, ride_time | 按线路和时间统计客流 |
| ride_record | idx_ride_boarding_time | boarding_station_id, ride_time | 按站点和时间统计上车人数 |
| ride_record | idx_ride_period | period_type, ride_time | 按时段统计高峰客流 |
| dispatch_advice | idx_advice_route_date | route_id, start_date, end_date | 查询线路调度建议 |

## 5. 复杂 SQL 场景

第一版至少实现以下复杂 SQL：

### 5.1 线路日客流趋势

```sql
SELECT
    DATE(ride_time) AS stat_date,
    COUNT(*) AS passenger_count
FROM ride_record
WHERE route_id = ?
  AND ride_time BETWEEN ? AND ?
GROUP BY DATE(ride_time)
ORDER BY stat_date;
```

### 5.2 站点上车客流排行

```sql
SELECT
    s.station_name,
    COUNT(*) AS boarding_count
FROM ride_record rr
JOIN station s ON rr.boarding_station_id = s.id
WHERE rr.route_id = ?
  AND rr.ride_time BETWEEN ? AND ?
GROUP BY s.id, s.station_name
ORDER BY boarding_count DESC
LIMIT 10;
```

### 5.3 早晚高峰客流对比

```sql
SELECT
    period_type,
    COUNT(*) AS passenger_count
FROM ride_record
WHERE route_id = ?
  AND ride_time BETWEEN ? AND ?
GROUP BY period_type
ORDER BY passenger_count DESC;
```

### 5.4 满载率计算

```sql
SELECT
    bs.route_id,
    bs.id AS schedule_id,
    bv.capacity,
    COUNT(rr.id) AS passenger_count,
    ROUND(COUNT(rr.id) / bv.capacity * 100, 2) AS load_rate
FROM bus_schedule bs
JOIN bus_vehicle bv ON bs.vehicle_id = bv.id
LEFT JOIN ride_record rr ON rr.schedule_id = bs.id
WHERE bs.route_id = ?
  AND bs.depart_time BETWEEN ? AND ?
GROUP BY bs.route_id, bs.id, bv.capacity
ORDER BY load_rate DESC;
```

## 6. 事务场景

第一版至少体现以下事务：

### 6.1 新增班次

新增班次时需要在一个事务内完成：

- 校验线路存在且启用
- 校验车辆存在且可用
- 校验司机存在且可用
- 写入 `bus_schedule`
- 记录 `operation_log`

如果任一步失败，事务回滚。

### 6.2 生成模拟运营数据

生成模拟数据时需要在一个事务或分批事务内完成：

- 生成车辆和司机
- 生成 30 天班次
- 生成约 1 万条乘车记录
- 记录生成日志

如果生成失败，需要避免产生明显不完整的数据。

### 6.3 保存调度建议

保存调度建议时需要在一个事务内完成：

- 查询统计数据
- 计算规则结果
- 调用 AI 微服务生成说明
- 写入 `dispatch_advice`
- 记录 `operation_log`

## 7. 真实线路数据状态

用户已提供 3 条真实公交线路、站点顺序和站点经纬度，已整理到：

- `data/real_routes/route_1.json`
- `data/real_routes/route_2.json`
- `data/real_routes/route_3.json`

数据概要：

| 线路编号 | 线路名称 | 起点站 | 终点站 | 站点数 |
| --- | --- | --- | --- | --- |
| 1 | 公交1路 | 环西文化广场① | 唐闸古镇公交停车场 | 27 |
| 2 | 公交2路 | 天生港 | 唐闸古镇公交停车场 | 17 |
| 3 | 公交3路 | 天生港 | 环西文化广场① | 32 |

## 8. 已确定初始化与规则配置

### 8.1 管理员初始化账号

第一版初始化 1 个管理员账号：

| 配置项 | 值 |
| --- | --- |
| 用户名 | 初始化管理员账号 |
| 显示姓名 | 王家豪 |
| 初始密码 | 初始化管理员密码 |

实现时密码不得明文保存到数据库，应在初始化脚本或后端启动初始化逻辑中写入加密后的 `password_hash`。

### 8.2 车辆默认容量

第一版模拟车辆默认容量：

```text
50 人
```

`bus_vehicle.capacity` 默认按 50 生成，满载率计算以该字段为准。

### 8.3 高峰时段

早高峰：

```text
07:00-09:00
核心高峰：07:30-08:30
```

晚高峰：

```text
17:00-19:00
核心高峰：17:30-18:30
```

时段分类建议：

- `morning_peak`：07:00-09:00
- `evening_peak`：17:00-19:00
- `normal`：除早晚高峰外的其他运营时段

核心高峰用于图表强调和 AI 分析说明，第一版不需要单独建表，可在统计逻辑中按时间范围计算。

### 8.4 调度建议默认阈值

第一版采用默认调度阈值：

- 高峰满载率大于等于 85%：建议增加班次。
- 高峰满载率 60% 到 85%：建议维持并观察。
- 正常时段满载率小于 30%：建议减少班次或延长发车间隔。
- 客流高于近 7 日均值 30%：标记为异常增长。

## 9. 待用户提供的信息

当前数据库设计第一版所需关键业务参数已补齐。后续如果要生成更贴近真实业务的模拟数据，还可以继续补充以下可选信息：

1. 3 条线路各自的运营首班和末班时间。
2. 3 条线路之间的客流强弱差异偏好。
3. 需要重点模拟的热门站点。

腾讯地图 API Key 已提供，前端统一使用环境变量 `VITE_TENCENT_MAP_KEY`，具体接入方式见 `docs/map-integration.md`。

## 10. 下一步

下一步应基于本文档继续产出：

- `sql/schema.sql`
- `sql/init_admin.sql`
- `data/real_routes/*.json`
- `scripts/import_route_data.py`
- `scripts/generate_mock_data.py`
