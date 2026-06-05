<template>
  <div class="map-panel" :class="`route-status-${routeStatus}`">
    <div ref="mapRef" class="map-canvas"></div>
    <div v-if="!mapReady" class="fallback-map">
      <div class="fallback-road road-main"></div>
      <div class="fallback-road road-vertical"></div>
      <div class="fallback-road road-diagonal"></div>
      <div class="fallback-district district-a">商业区</div>
      <div class="fallback-district district-b">居住区</div>
      <div class="fallback-district district-c">换乘区</div>
      <div class="fallback-line"></div>
      <span
        v-for="(station, index) in stations"
        :key="station.id"
        class="fallback-station"
        :class="[`flow-${flowLevel(station)}`, { selected: station.id === selectedStationId }]"
        :title="station.stationName"
        :style="{ left: `${8 + (index / Math.max(stations.length - 1, 1)) * 84}%`, top: `${44 + (index % 5) * 3}%` }"
        @click="$emit('select-station', station.id)"
      ></span>
      <div v-if="selectedStation" class="fallback-station-card" :class="`status-${flowLevel(selectedStation).replace('selected-', '')}`">
        <span>当前站点</span>
        <strong>{{ selectedStation.stationName }}</strong>
        <em>近 7 日上车 {{ Number(selectedStation.boardingCount || 0).toLocaleString('zh-CN') }} 人次</em>
      </div>
      <div class="fallback-map-legend">
        <span><i class="legend-high"></i>高客流</span>
        <span><i class="legend-normal"></i>关注</span>
        <span><i class="legend-low"></i>平稳</span>
      </div>
      <div class="map-status">{{ mapStatus }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'

export interface StationPoint {
  id: number
  stationName: string
  longitude: number
  latitude: number
  boardingCount?: number
  statusLevel?: 'high' | 'normal' | 'low'
}

const props = withDefaults(
  defineProps<{
    stations: StationPoint[]
    selectedStationId?: number
    routeStatus?: 'high' | 'normal' | 'low'
  }>(),
  { selectedStationId: 0, routeStatus: 'low' }
)
const emit = defineEmits<{ (event: 'select-station', stationId: number): void }>()
const mapRef = ref<HTMLDivElement>()
const mapReady = ref(false)
const loading = ref(false)
const mapError = ref('')
let map: any
let markerLayer: any
let polylineLayer: any
let mapLoader: Promise<void> | null = null

const flowThresholds = computed(() => {
  const counts = props.stations
    .map((item) => Number(item.boardingCount ?? 0))
    .filter((count) => count > 0)
    .sort((a, b) => a - b)
  if (!counts.length) {
    return { high: 0, low: 0, separated: false }
  }
  const high = counts[Math.floor((counts.length - 1) * 0.67)] || 0
  const low = counts[Math.floor((counts.length - 1) * 0.33)] || 0
  return { high, low, separated: high > low }
})

const selectedStation = computed(() => props.stations.find((station) => station.id === props.selectedStationId))

const mapStatus = computed(() => {
  if (loading.value) return '腾讯地图加载中'
  return mapError.value || '腾讯地图未加载，正在显示线路降级视图'
})

function loadTencentMap() {
  const key = import.meta.env.VITE_TENCENT_MAP_KEY
  if (!key || key.includes('replace-with')) {
    return Promise.reject(new Error('未配置腾讯地图 Key'))
  }
  if (window.TMap) {
    return Promise.resolve()
  }
  if (mapLoader) {
    return mapLoader
  }
  mapLoader = new Promise<void>((resolve, reject) => {
    const existing = document.getElementById('tencent-map-gl')
    if (existing) {
      waitForTMap(resolve, reject)
      return
    }
    const script = document.createElement('script')
    script.id = 'tencent-map-gl'
    script.src = `https://map.qq.com/api/gljs?v=1.exp&key=${encodeURIComponent(key)}`
    script.onload = () => waitForTMap(resolve, reject)
    script.onerror = () => reject(new Error('腾讯地图脚本加载失败，请检查网络、Key 或域名白名单'))
    document.head.appendChild(script)
  })
  return mapLoader
}

function waitForTMap(resolve: () => void, reject: (error: Error) => void, count = 0) {
  if (window.TMap) {
    resolve()
    return
  }
  if (count > 50) {
    reject(new Error('腾讯地图脚本已返回，但 TMap 未初始化'))
    return
  }
  window.setTimeout(() => waitForTMap(resolve, reject, count + 1), 100)
}

async function draw() {
  if (!props.stations.length) return
  await nextTick()
  try {
    loading.value = true
    mapError.value = ''
    await loadTencentMap()
    if (!mapRef.value || !window.TMap) return
    mapReady.value = true
    const centerStation = props.stations[Math.floor(props.stations.length / 2)]
    const center = new window.TMap.LatLng(centerStation.latitude, centerStation.longitude)
    if (!map) {
      map = new window.TMap.Map(mapRef.value, { center, zoom: 13 })
    } else {
      map.setCenter(center)
    }
    const geometries = props.stations.map((station) => ({
      id: String(station.id),
      styleId: flowLevel(station),
      position: new window.TMap.LatLng(station.latitude, station.longitude),
      properties: { title: markerTitle(station) }
    }))
    markerLayer?.setMap(null)
    markerLayer = new window.TMap.MultiMarker({
      map,
      styles: {
        high: markerStyle(window.TMap, '#d93b30'),
        normal: markerStyle(window.TMap, '#f2b84b'),
        low: markerStyle(window.TMap, '#49a66a'),
        'selected-high': markerStyle(window.TMap, '#d93b30', true),
        'selected-normal': markerStyle(window.TMap, '#f2b84b', true),
        'selected-low': markerStyle(window.TMap, '#49a66a', true)
      },
      geometries
    })
    if (typeof markerLayer.on === 'function') {
      markerLayer.on('click', (event: any) => {
        const stationId = Number(event.geometry?.id)
        if (stationId) emit('select-station', stationId)
      })
    }
    polylineLayer?.setMap(null)
    polylineLayer = new window.TMap.MultiPolyline({
      map,
      styles: { line: new window.TMap.PolylineStyle({ color: levelColor(props.routeStatus), width: 6 }) },
      geometries: [{ id: 'route', styleId: 'line', paths: geometries.map((item) => item.position) }]
    })
  } catch (error) {
    mapReady.value = false
    mapError.value = (error as Error).message
  } finally {
    loading.value = false
  }
}

onMounted(draw)
watch(() => [props.stations, props.selectedStationId, props.routeStatus], draw, { deep: true })

function markerStyle(TMap: any, color: string, selected = false) {
  return new TMap.MarkerStyle({
    width: selected ? 26 : 20,
    height: selected ? 36 : 28,
    anchor: { x: selected ? 13 : 10, y: selected ? 36 : 28 },
    src: pinSvg(color)
  })
}

function pinSvg(color: string) {
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="20" height="28" viewBox="0 0 24 34"><path fill="${color}" stroke="white" stroke-width="2" d="M12 1C6.1 1 1.5 5.7 1.5 11.6c0 8.1 10.5 21.2 10.5 21.2s10.5-13.1 10.5-21.2C22.5 5.7 17.9 1 12 1Z"/><circle cx="12" cy="11.5" r="4.2" fill="white"/></svg>`
  return `data:image/svg+xml;charset=utf-8,${encodeURIComponent(svg)}`
}

function flowLevel(station: StationPoint) {
  const level = station.statusLevel
  if (station.id === props.selectedStationId && level) return `selected-${level}`
  if (level) return level
  const count = Number(station.boardingCount ?? 0)
  const thresholds = flowThresholds.value
  if (count <= 0) return station.id === props.selectedStationId ? 'selected-low' : 'low'
  if (!thresholds.separated) return station.id === props.selectedStationId ? 'selected-normal' : 'normal'
  const fallbackLevel = count >= thresholds.high ? 'high' : count <= thresholds.low ? 'low' : 'normal'
  return station.id === props.selectedStationId ? `selected-${fallbackLevel}` : fallbackLevel
}

function markerTitle(station: StationPoint) {
  const count = Number(station.boardingCount ?? 0)
  return `${station.stationName}，上车客流 ${count} 人次`
}

function levelColor(level: 'high' | 'normal' | 'low') {
  if (level === 'high') return '#d93b30'
  if (level === 'normal') return '#f2b84b'
  return '#49a66a'
}
</script>
