# 真实公交线路数据

本目录保存第一版 MVP 使用的 3 条真实公交线路与站点数据。

数据格式：

- `routeCode`：线路编号
- `routeName`：线路名称
- `direction`：线路方向
- `startStation`：起点站
- `endStation`：终点站
- `coordinateSystem`：坐标系，当前按腾讯地图可用经纬度处理
- `stations`：站点顺序、站点名称、经度、纬度

文件清单：

| 文件 | 线路 | 定位 | 当前日客流口径 |
| --- | --- | --- | ---: |
| `route_1.json` | 公交1路 | 核心城区干线，覆盖商圈与老镇 | 4,500-5,500 人次/天 |
| `route_2.json` | 公交2路 | 短途支线，服务唐闸 - 天生港区间 | 800-1,200 人次/天 |
| `route_3.json` | 公交3路 | 最长干线，串联核心区与天生港 | 5,500-6,500 人次/天 |

运营客流、热点站点和班次模拟规则见：

```text
scripts/generate_mock_data.py
docs/data-import-guide.md
```
