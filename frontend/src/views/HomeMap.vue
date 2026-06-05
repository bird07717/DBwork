<template>
  <section class="page home-map-page">
    <el-alert v-if="errorText" :title="errorText" type="error" show-icon style="margin-bottom: 12px" />
    <div class="home-map-stage" v-loading="loading">
      <div class="home-map-main">
        <RouteMap
          class="home-route-map"
          :stations="stations"
          :selected-station-id="selectedStationId"
          :route-status="selectedRouteStatus"
          @select-station="selectStation"
        />

        <div class="home-map-overlay">
          <div class="overlay-kicker">
            <span class="status-dot" :class="`status-${systemStatus.level}`"></span>
            {{ systemStatus.label }}
          </div>
          <h1>公交客流调度态势</h1>
          <p>{{ routeSummary }}</p>

          <div class="overview-metrics">
            <div>
              <span>今日总客流</span>
              <strong>{{ formatNumber(totalFlowToday) }}</strong>
              <em>{{ operationDate }}</em>
            </div>
            <div>
              <span>最高满载率</span>
              <strong>{{ formatPercent(highestLoadRate) }}</strong>
              <em>{{ selectedRouteStatusLabel }}</em>
            </div>
          </div>

          <div class="alert-summary">
            <div class="section-title">告警摘要</div>
            <button
              v-for="alert in alertItems"
              :key="alert.id"
              class="alert-item"
              :class="`status-${alert.level}`"
              @click="alert.stationId ? selectStation(alert.stationId) : undefined"
            >
              <span>{{ alert.title }}</span>
              <strong>{{ alert.value }}</strong>
            </button>
            <div v-if="!alertItems.length" class="quiet-state">暂无高客流告警，线路运行平稳</div>
          </div>
        </div>
      </div>

      <aside class="route-status-panel">
        <div class="panel-head">
          <div>
            <span>线路状态</span>
            <strong>{{ selectedRoute?.routeName || '-' }}</strong>
          </div>
          <el-button size="small" @click="$router.push({ path: '/analysis', query: { routeId: selectedRouteId } })">分析</el-button>
        </div>

        <div class="route-select-box">
          <span>选择线路</span>
          <el-select v-model="selectedRouteId" size="small" @change="selectRoute">
            <el-option v-for="route in routes" :key="route.id" :label="route.routeName" :value="route.id" />
          </el-select>
        </div>

        <div class="route-tabs">
          <button
            v-for="route in routes"
            :key="route.id"
            class="route-tab"
            :class="[{ active: route.id === selectedRouteId }, `status-${routeStats[route.id]?.level || 'low'}`]"
            @click="selectRoute(route.id)"
          >
            <span>{{ route.routeName }}</span>
            <strong>{{ formatPercent(routeStats[route.id]?.maxLoadRate || 0) }}</strong>
          </button>
        </div>

        <div class="selected-station-card" v-if="selectedStation">
          <div>
            <span class="station-order">#{{ selectedStation.stationOrder }}</span>
            <strong>{{ selectedStation.stationName }}</strong>
          </div>
          <el-tag :type="tagType(selectedStation.statusLevel)" effect="dark">{{ statusText(selectedStation.statusLevel) }}</el-tag>
          <p>近 7 日上车 {{ formatNumber(selectedStation.boardingCount) }} 人次</p>
          <div class="station-card-actions">
            <el-button size="small" @click="$router.push(`/routes/${selectedRouteId}`)">线路详情</el-button>
            <el-button size="small" type="primary" @click="$router.push({ path: '/analysis', query: { routeId: selectedRouteId } })">客流分析</el-button>
          </div>
        </div>

        <div class="station-list">
          <button
            v-for="station in stations"
            :key="station.id"
            class="station-row"
            :class="[{ active: station.id === selectedStationId }, `status-${station.statusLevel}`]"
            @click="selectStation(station.id)"
          >
            <span class="station-marker">{{ station.stationOrder }}</span>
            <span class="station-name">{{ station.stationName }}</span>
            <strong>{{ formatNumber(station.boardingCount) }}</strong>
          </button>
        </div>
      </aside>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import RouteMap from '../components/RouteMap.vue'
import { api, defaultRange, type RouteItem, type StationItem } from '../services/domain'

type StatusLevel = 'high' | 'normal' | 'low'

interface HomeStation extends StationItem {
  boardingCount: number
  statusLevel: StatusLevel
}

interface RouteStat {
  passengerCount: number
  maxLoadRate: number
  level: StatusLevel
}

interface AlertItem {
  id: string
  title: string
  value: string
  level: StatusLevel
  stationId?: number
}

const routes = ref<RouteItem[]>([])
const stations = ref<HomeStation[]>([])
const routeStats = ref<Record<number, RouteStat>>({})
const selectedRouteId = ref(0)
const selectedStationId = ref(0)
const loading = ref(false)
const errorText = ref('')
const range = defaultRange()
const operationDate = range[1]

const selectedRoute = computed(() => routes.value.find((route) => route.id === selectedRouteId.value))
const selectedStation = computed(() => stations.value.find((station) => station.id === selectedStationId.value))
const routeSummary = computed(() => {
  if (!selectedRoute.value) return '线路加载中'
  return `${selectedRoute.value.routeName} · ${selectedRoute.value.startStationName} → ${selectedRoute.value.endStationName}`
})
const totalFlowToday = computed(() => Object.values(routeStats.value).reduce((sum, row) => sum + row.passengerCount, 0))
const highestLoadRate = computed(() => Math.max(0, ...Object.values(routeStats.value).map((row) => row.maxLoadRate)))
const selectedRouteStatus = computed<StatusLevel>(() => routeStats.value[selectedRouteId.value]?.level || 'low')
const selectedRouteStatusLabel = computed(() => statusText(selectedRouteStatus.value))
const systemStatus = computed(() => {
  const level = statusByLoad(highestLoadRate.value)
  return { level, label: level === 'high' ? '高客流告警' : level === 'normal' ? '运行关注' : '运行平稳' }
})
const alertItems = computed<AlertItem[]>(() => {
  const items: AlertItem[] = []
  const topStation = stations.value.find((station) => station.statusLevel === 'high')
  if (highestLoadRate.value >= 85) {
    items.push({
      id: 'load-rate',
      title: '最高满载率超过阈值',
      value: formatPercent(highestLoadRate.value),
      level: 'high'
    })
  }
  if (topStation) {
    items.push({
      id: `station-${topStation.id}`,
      title: `${topStation.stationName} 客流偏高`,
      value: `${formatNumber(topStation.boardingCount)} 人次`,
      level: 'high',
      stationId: topStation.id
    })
  }
  if (!items.length && highestLoadRate.value >= 60) {
    items.push({
      id: 'watch',
      title: '部分班次接近关注区间',
      value: formatPercent(highestLoadRate.value),
      level: 'normal'
    })
  }
  return items.slice(0, 3)
})

async function selectRoute(id: number) {
  try {
    loading.value = true
    errorText.value = ''
    selectedRouteId.value = id
    const [stationRows, routeFlowRows, stationFlowRows, loadRateRows] = await Promise.all([
      api.stations(id),
      api.routeFlow(id, range[0], range[1]),
      api.stationFlow(id, range[0], range[1], 50),
      api.loadRate(id, range[0], range[1])
    ])
    routeStats.value = { ...routeStats.value, [id]: buildRouteStat(routeFlowRows, loadRateRows) }
    const flowMap = new Map(stationFlowRows.map((item) => [Number(item.stationId), Number(item.boardingCount || 0)]))
    const counts = stationRows.map((station) => flowMap.get(station.id) || 0)
    stations.value = stationRows.map((station) => ({
      ...station,
      boardingCount: flowMap.get(station.id) || 0,
      statusLevel: statusByPassenger(flowMap.get(station.id) || 0, counts)
    }))
    selectedStationId.value = stations.value[0]?.id || 0
  } catch (error) {
    errorText.value = (error as Error).message || '首页线路数据加载失败，请稍后刷新。'
  } finally {
    loading.value = false
  }
}

function selectStation(id: number) {
  selectedStationId.value = id
}

async function loadOverview(routeRows: RouteItem[]) {
  const entries = await Promise.all(
    routeRows.map(async (route) => {
      const [routeFlowRows, loadRateRows] = await Promise.all([
        api.routeFlow(route.id, range[0], range[1]),
        api.loadRate(route.id, range[0], range[1])
      ])
      return [route.id, buildRouteStat(routeFlowRows, loadRateRows)] as const
    })
  )
  routeStats.value = Object.fromEntries(entries)
}

function buildRouteStat(routeFlowRows: Record<string, unknown>[], loadRateRows: Record<string, unknown>[]): RouteStat {
  const passengerCount = routeFlowRows
    .filter((row) => String(row.statDate) === operationDate)
    .reduce((sum, row) => sum + Number(row.passengerCount || 0), 0)
  const maxLoadRate = Math.max(0, ...loadRateRows.map((row) => Math.min(100, Number(row.loadRate || 0))))
  return { passengerCount, maxLoadRate, level: statusByLoad(maxLoadRate) }
}

function statusByLoad(value: number): StatusLevel {
  if (value >= 85) return 'high'
  if (value >= 60) return 'normal'
  return 'low'
}

function statusByPassenger(value: number, values: number[]): StatusLevel {
  const counts = values.filter((count) => count > 0).sort((a, b) => a - b)
  if (!value || !counts.length) return 'low'
  const low = counts[Math.floor((counts.length - 1) * 0.33)] || 0
  const high = counts[Math.floor((counts.length - 1) * 0.67)] || 0
  if (high <= low) return 'normal'
  if (value >= high) return 'high'
  if (value <= low) return 'low'
  return 'normal'
}

function statusText(level: StatusLevel) {
  return level === 'high' ? '高客流' : level === 'normal' ? '关注' : '平稳'
}

function tagType(level: StatusLevel) {
  return level === 'high' ? 'danger' : level === 'normal' ? 'warning' : 'success'
}

function formatNumber(value = 0) {
  return Math.round(value).toLocaleString('zh-CN')
}

function formatPercent(value = 0) {
  return `${Number(value || 0).toFixed(1)}%`
}

onMounted(async () => {
  try {
    loading.value = true
    errorText.value = ''
    routes.value = await api.routes()
    if (routes.value[0]) {
      await loadOverview(routes.value)
      await selectRoute(routes.value[0].id)
    }
  } catch (error) {
    errorText.value = (error as Error).message || '首页数据加载失败，请稍后刷新。'
  } finally {
    loading.value = false
  }
})
</script>
