# 腾讯地图接入设计 v1

## 1. API Key

当前开发阶段使用的腾讯地图 API Key：

```text
77MBZ-JJGCQ-MGF5A-BJCUR-4LKZO-JVB2W
```

前端统一通过环境变量读取：

```text
VITE_TENCENT_MAP_KEY=77MBZ-JJGCQ-MGF5A-BJCUR-4LKZO-JVB2W
```

后续创建前端项目时，应将真实 Key 放入 `frontend/.env.local`，并在 `frontend/.env.example` 中只保留变量名示例。生产环境需要在腾讯位置服务控制台配置域名白名单。

## 2. 地图 SDK

第一版使用腾讯地图 JavaScript API GL。

SDK 加载地址：

```html
<script src="https://map.qq.com/api/gljs?v=1.exp&key=YOUR_API_KEY"></script>
<link rel="stylesheet" href="https://map.qq.com/api/gljs/qqmap.css">
```

在 Vue + Vite 项目中，`YOUR_API_KEY` 应由 `import.meta.env.VITE_TENCENT_MAP_KEY` 注入，不在源码中硬编码。

## 3. 首页地图初始化

首页地图默认展示南通公交线路区域。

默认地图参数：

```javascript
const map = new TMap.Map(mapContainer, {
  center: new TMap.LatLng(32.045, 120.82),
  zoom: 14,
  mapTypeId: TMap.MapTypeId.ROADMAP,
  minZoom: 10,
  maxZoom: 18,
  disableDefaultUI: false
});
```

需要添加地图控制栏：

```javascript
const controlBar = new TMap.ControlBar({
  position: TMap.ControlPosition.TOP_RIGHT
});
map.addControl(controlBar);
```

## 4. 线路和站点展示

公交线路数据来源于：

```text
data/real_routes/route_1.json
data/real_routes/route_2.json
data/real_routes/route_3.json
```

前端接口返回站点数据后，需要转换为腾讯地图坐标对象：

```javascript
const routeCoordinates = stations.map(station =>
  new TMap.LatLng(station.latitude, station.longitude)
);
```

### 4.1 站点 Marker

使用 `TMap.MultiMarker` 展示站点。

核心实现方式：

```javascript
const markerLayer = new TMap.MultiMarker({
  map,
  geometries: stations.map(station => ({
    id: String(station.stationOrder),
    styleId: "stationStyle",
    position: new TMap.LatLng(station.latitude, station.longitude),
    properties: {
      name: station.stationName,
      order: station.stationOrder
    }
  })),
  styles: {
    stationStyle: new TMap.MarkerStyle({
      width: 30,
      height: 30,
      anchor: { x: 15, y: 30 },
      src: "https://mapapi.qq.com/web/lbs/javascriptGL/demo/img/marker.png"
    })
  }
});
```

### 4.2 线路折线

使用 `TMap.MultiPolyline` 绘制公交线路。

核心实现方式：

```javascript
const polylineLayer = new TMap.MultiPolyline({
  map,
  geometries: [{
    id: route.routeCode,
    styleId: "busRouteStyle",
    paths: routeCoordinates
  }],
  styles: {
    busRouteStyle: new TMap.PolylineStyle({
      color: "#007AFF",
      width: 6,
      borderWidth: 2,
      borderColor: "#FFFFFF",
      lineCap: "round"
    })
  }
});
```

## 5. 站点信息窗口

点击站点 Marker 时显示站点信息。

核心实现方式：

```javascript
const infoWindow = new TMap.InfoWindow({
  map,
  offset: { x: 0, y: -30 },
  visible: false
});

markerLayer.on("click", event => {
  const { name, order } = event.geometry.properties;
  infoWindow.setPosition(event.geometry.position);
  infoWindow.setContent(`
    <div class="info-window">
      <h4>公交站点</h4>
      <p><strong>站点：</strong>${name}</p>
      <p><strong>站序：</strong>${order}</p>
    </div>
  `);
  infoWindow.show();
});

map.on("click", () => {
  infoWindow.hide();
});
```

## 6. 坐标系统

当前真实线路数据按腾讯地图可用经纬度处理，文档中标记为：

```text
coordinateSystem: "tencent"
```

如果后续接入其他来源坐标，需要统一转换到腾讯地图使用的 GCJ-02 坐标系。

可参考转换方式：

```javascript
function wgs84ToGcj02(lat, lng) {
  return TMap.CoordTransform.wgs84ToGcj02(new TMap.LatLng(lat, lng));
}

function bd09ToGcj02(lat, lng) {
  return TMap.CoordTransform.bd09ToGcj02(new TMap.LatLng(lat, lng));
}
```

## 7. 验收清单

地图功能完成后需要验证：

- `YOUR_API_KEY` 已替换为环境变量读取的真实 Key。
- 地图可以正常显示，不出现空白页。
- 3 条公交线路可以切换展示。
- 站点 Marker 显示在对应位置。
- 线路折线完整连接所有站点。
- 点击站点 Marker 可以打开信息窗口。
- 点击地图空白处可以关闭信息窗口。
- 浏览器控制台没有腾讯地图 SDK 加载错误。
- 生产环境配置了腾讯地图 API Key 域名白名单。

## 8. 官方资源

- 腾讯位置服务官网：https://lbs.qq.com/
- JavaScript API GL 文档：https://lbs.qq.com/webApi/javascriptGL/glDoc/glDocIndex
- 示例中心：https://lbs.qq.com/webApi/javascriptGL/demo/index.html
- 常见问题：https://lbs.qq.com/FAQ/web_faq.html
