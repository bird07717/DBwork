# 公交线路与运营数据导入说明

本项目的数据分两层：

1. **线路基础数据**：线路、站点、线路站点顺序，来自 `data/real_routes/route_*.json`。
2. **运营客流数据**：车辆、司机、班次、乘车记录，来自 `scripts/generate_mock_data.py` 生成后写入 MySQL。

当前数据库表结构见 `sql/schema.sql`。

## 1. 当前 1/2/3 路调查口径

当前模拟运营数据已按以下口径调整：

| 线路 | 日客流口径 | 定位 |
| --- | ---: | --- |
| 公交1路 | 4,500-5,500 人次/天 | 核心城区干线，覆盖商圈与老镇 |
| 公交2路 | 800-1,200 人次/天 | 短途支线，服务唐闸 - 天生港区间 |
| 公交3路 | 5,500-6,500 人次/天 | 最长干线，串联核心区与天生港 |

热点站点权重写在 `scripts/generate_mock_data.py` 的 `ROUTE_PROFILES` 中：

- 1 路：唐闸古镇公交停车场、五水商圈、 市一中、端平桥、口腔医院北院。
- 2 路：唐闸古镇公交停车场、天生港、高店站。
- 3 路：天生港、环西文化广场、五水商圈、钟楼广场、市一中、天生港小学。

这些权重会影响 `ride_record.boarding_station_id` 的分布，从而影响首页热点站点、客流分析站点排行和 AI/规则调度建议。

## 2. 重建当前演示数据库

在项目根目录执行。脚本使用了现代 Python 类型标注，建议使用 **Python 3.10+**：

```bash
cd /home/bird/Projects/DBwork
python3 scripts/import_route_data.py
python3 scripts/generate_mock_data.py --end-date 2026-06-04 --days 30
```

执行效果：

- `import_route_data.py`
  - 从 `data/real_routes/route_*.json` 导入 `bus_route`、`station`、`route_station`。
  - 支持重复执行：同一 `routeCode` 会更新线路，同一站点编码会更新站点。
- `generate_mock_data.py`
  - 清空并重建 `dispatch_advice`、`ride_record`、`bus_schedule`、`bus_vehicle`、`driver`。
  - 默认按调查口径生成 30 天数据，不再强制压缩成固定 90,000 条。
  - 默认结束日期是 `2026-06-04`，与前端默认分析区间匹配。

如果只想看将要执行的 SQL，不写入数据库：

```bash
python3 scripts/import_route_data.py --dry-run
python3 scripts/generate_mock_data.py --dry-run
```

## 3. 导入其他日期的数据

例如生成 2026-06-01 到 2026-06-30 的 30 天运营数据：

```bash
python3 scripts/generate_mock_data.py --end-date 2026-06-30 --days 30
```

例如只生成最近 7 天数据：

```bash
python3 scripts/generate_mock_data.py --end-date 2026-06-30 --days 7
```

注意：当前脚本是“重建演示数据”模式，会先清空旧的班次和乘车记录，再生成新数据。这样适合课程演示和反复调参。

## 4. 控制总乘车记录数量

默认情况下，脚本直接使用调查日客流区间：

- 1 路约 4,500-5,500 人次/天
- 2 路约 800-1,200 人次/天
- 3 路约 5,500-6,500 人次/天

如果电脑性能有限，想压缩总数据量，可以加 `--ride-count`：

```bash
python3 scripts/generate_mock_data.py --end-date 2026-06-04 --days 30 --ride-count 90000
```

这会保持线路之间的相对比例，但把总乘车记录数缩放到约 90,000 条。

## 5. 导入其他线路

### 5.1 新增线路 JSON

在 `data/real_routes/` 下新增文件，例如：

```text
data/real_routes/route_8.json
```

格式示例：

```json
{
  "routeCode": "8",
  "routeName": "公交8路",
  "direction": "起点站 -> 终点站",
  "startStation": "起点站",
  "endStation": "终点站",
  "coordinateSystem": "tencent",
  "stations": [
    { "stationOrder": 1, "stationName": "起点站", "longitude": 120.800000, "latitude": 32.060000 },
    { "stationOrder": 2, "stationName": "中途站", "longitude": 120.810000, "latitude": 32.050000 },
    { "stationOrder": 3, "stationName": "终点站", "longitude": 120.820000, "latitude": 32.040000 }
  ]
}
```

然后执行：

```bash
python3 scripts/import_route_data.py
```

### 5.2 为新线路生成客流

`generate_mock_data.py` 当前只为 1/2/3 路生成运营数据。新增线路后，需要在脚本顶部的 `ROUTE_PROFILES` 中增加同名 `routeCode` 配置：

```python
"8": {
    "station_count": 3,
    "daily_passenger_range": (1500, 2200),
    "schedule_count_range": (40, 56),
    "schedule_period_shares": {"morning_peak": 0.34, "normal": 0.30, "evening_peak": 0.36},
    "demand_period_shares": {"morning_peak": 0.36, "normal": 0.25, "evening_peak": 0.39},
    "hotspots": {
        "morning_peak": [(1, 4.0), (2, 2.0)],
        "normal": [(2, 3.0), (3, 2.0)],
        "evening_peak": [(3, 4.0), (2, 2.0)],
    },
}
```

字段含义：

- `station_count`：该线路站点数量，必须和 JSON 站点数一致。
- `daily_passenger_range`：日客流区间。
- `schedule_count_range`：每天班次数区间。
- `schedule_period_shares`：班次在早高峰、平峰、晚高峰的比例。
- `demand_period_shares`：乘车需求在早高峰、平峰、晚高峰的比例。
- `hotspots`：热点上车站点，格式为 `(stationOrder, weight)`。

最后执行：

```bash
python3 scripts/generate_mock_data.py --end-date 2026-06-04 --days 30
```

## 6. 如果要导入真实刷卡/客流数据

当前项目的真实客流入口是 `ride_record` 表。最小字段要求：

| 字段 | 含义 |
| --- | --- |
| `route_id` | 线路 ID |
| `schedule_id` | 班次 ID，可为空，但有值时可用于满载率计算 |
| `boarding_station_id` | 上车站点 ID |
| `alighting_station_id` | 下车站点 ID，可为空 |
| `ride_time` | 乘车时间 |
| `period_type` | `morning_peak`、`evening_peak`、`normal` |
| `pay_type` | 数据来源，例如 `card`、`qr`、`manual`、`调查口径模拟` |

若没有真实班次数据，也可以先按线路、站点、时间导入 `ride_record`，这样站点客流和线路客流能正常统计；但满载率需要 `schedule_id` 关联 `bus_schedule` 才更准确。
