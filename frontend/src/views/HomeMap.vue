<template>
  <section class="page home-map-page">
    <div class="home-command-header">
      <div>
        <div class="command-kicker">Transit Command Center</div>
        <h1>地图驱动的公交调度数据平台</h1>
        <p>以线路地图为主视角，联动客流、满载率、站点告警和调度分析入口。</p>
      </div>
      <div class="command-meta">
        <span>统计区间</span>
        <strong>{{ rangeLabel }}</strong>
        <el-date-picker
          v-model="range"
          type="daterange"
          value-format="YYYY-MM-DD"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          :clearable="false"
          size="small"
          @change="reloadByRange"
        />
      </div>
    </div>

    <el-alert v-if="errorText" :title="errorText" type="error" show-icon style="margin-bottom: 12px" />

    <div class="operation-rail" v-loading="loading">
      <div v-for="card in operationCards" :key="card.label" class="operation-card" :class="`tone-${card.tone}`">
        <span>{{ card.label }}</span>
        <strong>{{ card.value }}</strong>
        <em>{{ card.note }}</em>
      </div>
    </div>

    <div class="home-map-stage" v-loading="loading">
      <div class="home-map-main">
        <RouteMap
          class="home-route-map"
          :stations="stations"
          :selected-station-id="selectedStationId"
          :route-status="selectedRouteStatus"
          @select-station="selectStation"
        />

        <div class="home-map-overlay" :class="{ collapsed: overlayCollapsed }">
          <button
            class="map-overlay-handle"
            type="button"
            :aria-label="overlayCollapsed ? '展开态势卡片' : '收起态势卡片'"
            :title="overlayCollapsed ? '展开态势卡片' : '收起态势卡片'"
            :aria-expanded="!overlayCollapsed"
            @click="overlayCollapsed = !overlayCollapsed"
          >
            <span aria-hidden="true">{{ overlayCollapsed ? '‹' : '›' }}</span>
          </button>

          <div class="overlay-kicker">
            <span class="status-dot" :class="`status-${selectedRouteStatus}`"></span>
            {{ selectedRouteStatusLabel }} · 当前线路
          </div>
          <h1>公交客流调度态势</h1>
          <p>{{ routeSummary }}</p>

          <div class="overview-metrics">
            <div>
              <span>当前线路客流</span>
              <strong>{{ formatNumber(selectedRouteStat.passengerCount) }}</strong>
              <em>{{ rangeLabel }}</em>
            </div>
            <div>
              <span>线路平均满载</span>
              <strong>{{ formatPercent(selectedRouteStat.avgLoadRate) }}</strong>
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

          <div class="overlay-actions">
            <el-button size="small" type="primary" @click="$router.push({ path: '/analysis', query: { routeId: selectedRouteId } })">打开分析看板</el-button>
            <el-button size="small" @click="$router.push({ path: '/ai', query: { routeId: selectedRouteId } })">生成 AI 建议</el-button>
          </div>
        </div>
      </div>

      <aside class="route-status-panel" v-loading="routeLoading">
        <div class="panel-head">
          <div>
            <span>线路状态</span>
            <strong>{{ selectedRoute?.routeName || '-' }}</strong>
          </div>
          <el-tag :type="tagType(selectedRouteStatus)" effect="dark">{{ selectedRouteStatusLabel }}</el-tag>
        </div>

        <div class="route-select-box">
          <span>选择线路</span>
          <el-select v-model="selectedRouteId" size="small" @change="selectRoute">
            <el-option v-for="route in routes" :key="route.id" :label="route.routeName" :value="route.id" />
          </el-select>
        </div>

        <div class="route-snapshot">
          <div>
            <span>区间客流</span>
            <strong>{{ formatNumber(selectedRouteStat.passengerCount) }}</strong>
          </div>
          <div>
            <span>峰值满载</span>
            <strong>{{ formatPercent(selectedRouteStat.peakLoadRate) }}</strong>
          </div>
          <div>
            <span>高客流站点</span>
            <strong>{{ highStationCount }}</strong>
          </div>
        </div>

        <div class="route-tabs">
          <button
            v-for="route in routeRiskItems"
            :key="route.id"
            class="route-tab"
            :class="[{ active: route.id === selectedRouteId }, `status-${route.level}`]"
            @click="selectRoute(route.id)"
          >
            <span>{{ route.routeName }}</span>
            <strong>{{ formatPercent(route.avgLoadRate) }}</strong>
          </button>
        </div>

        <div class="selected-station-card" v-if="selectedStation">
          <div>
            <span class="station-order">#{{ selectedStation.stationOrder }}</span>
            <strong>{{ selectedStation.stationName }}</strong>
          </div>
          <el-tag :type="tagType(selectedStation.statusLevel)" effect="dark">{{ statusText(selectedStation.statusLevel) }}</el-tag>
          <p>近 7 日上车 {{ formatNumber(selectedStation.boardingCount) }} 人次，当前处于{{ statusText(selectedStation.statusLevel) }}状态。</p>
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
type OperationTone = 'green' | 'yellow' | 'red' | 'blue'

interface HomeStation extends StationItem {
  boardingCount: number
  statusLevel: StatusLevel
}

interface RouteStat {
  passengerCount: number
  avgLoadRate: number
  peakLoadRate: number
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
const overlayCollapsed = ref(true)
const loading = ref(false)
const routeLoading = ref(false)
const errorText = ref('')
const range = ref<[string, string]>(defaultRange())
const routeStationCache = ref<Record<number, HomeStation[]>>({})
let routeRequestSeq = 0

const selectedRoute = computed(() => routes.value.find((route) => route.id === selectedRouteId.value))
const selectedStation = computed(() => stations.value.find((station) => station.id === selectedStationId.value))
const selectedRouteStat = computed<RouteStat>(() => routeStats.value[selectedRouteId.value] || { passengerCount: 0, avgLoadRate: 0, peakLoadRate: 0, level: 'low' })
const rangeLabel = computed(() => `${range.value[0]} 至 ${range.value[1]}`)
const routeSummary = computed(() => {
  if (!selectedRoute.value) return '线路加载中'
  return `${selectedRoute.value.routeName} · ${selectedRoute.value.startStationName} → ${selectedRoute.value.endStationName}`
})
const totalFlowToday = computed(() => Object.values(routeStats.value).reduce((sum, row) => sum + row.passengerCount, 0))
const highestLoadRate = computed(() => Math.max(0, ...Object.values(routeStats.value).map((row) => row.avgLoadRate)))
const selectedRouteStatus = computed<StatusLevel>(() => selectedRouteStat.value.level)
const selectedRouteStatusLabel = computed(() => statusText(selectedRouteStatus.value))
const highStationCount = computed(() => stations.value.filter((station) => station.statusLevel === 'high').length)
const attentionRouteCount = computed(() => Object.values(routeStats.value).filter((item) => item.level !== 'low').length)
const topStation = computed(() => [...stations.value].sort((a, b) => b.boardingCount - a.boardingCount)[0])
const routeRiskItems = computed(() =>
  routes.value
    .map((route) => ({
      ...route,
      passengerCount: routeStats.value[route.id]?.passengerCount || 0,
      avgLoadRate: routeStats.value[route.id]?.avgLoadRate || 0,
      peakLoadRate: routeStats.value[route.id]?.peakLoadRate || 0,
      level: routeStats.value[route.id]?.level || 'low'
    }))
    .sort((a, b) => b.avgLoadRate - a.avgLoadRate)
)
const systemStatus = computed(() => {
  const level = statusByLoad(highestLoadRate.value)
  return { level, label: level === 'high' ? '高客流告警' : level === 'normal' ? '运行关注' : '运行平稳' }
})
const operationCards = computed<{ label: string; value: string; note: string; tone: OperationTone }[]>(() => [
  { label: '在线线路', value: `${routes.value.length}`, note: `${attentionRouteCount.value} 条需关注`, tone: attentionRouteCount.value ? 'yellow' : 'green' },
  { label: '区间客流', value: formatNumber(totalFlowToday.value), note: `${rangeLabel.value} 汇总`, tone: 'blue' },
  { label: '最高平均满载', value: formatPercent(highestLoadRate.value), note: systemStatus.value.label, tone: systemStatus.value.level === 'high' ? 'red' : systemStatus.value.level === 'normal' ? 'yellow' : 'green' },
  { label: '热点站点', value: topStation.value?.stationName || '-', note: topStation.value ? `${formatNumber(topStation.value.boardingCount)} 人次` : '等待加载', tone: highStationCount.value ? 'red' : 'green' }
])
const alertItems = computed<AlertItem[]>(() => {
  const items: AlertItem[] = []
  const topHighStation = stations.value.find((station) => station.statusLevel === 'high')
  if (!selectedRouteStat.value.passengerCount) {
    items.push({
      id: 'empty-data',
      title: '当前日期范围暂无客流数据',
      value: '请切换区间',
      level: 'normal'
    })
    return items
  }
  if (selectedRouteStat.value.avgLoadRate >= 85) {
    items.push({
      id: 'load-rate',
      title: '当前线路平均满载超过阈值',
      value: formatPercent(selectedRouteStat.value.avgLoadRate),
      level: 'high'
    })
  }
  if (topHighStation) {
    items.push({
      id: `station-${topHighStation.id}`,
      title: `${topHighStation.stationName} 客流偏高`,
      value: `${formatNumber(topHighStation.boardingCount)} 人次`,
      level: 'high',
      stationId: topHighStation.id
    })
  }
  if (!items.length && selectedRouteStat.value.avgLoadRate >= 60) {
    items.push({
      id: 'watch',
      title: '当前线路进入关注区间',
      value: formatPercent(selectedRouteStat.value.avgLoadRate),
      level: 'normal'
    })
  }
  return items.slice(0, 3)
})

async function selectRoute(id: number, options: { expandOverlay?: boolean } = {}) {
  if (!id) return
  if (options.expandOverlay !== false) overlayCollapsed.value = false
  if (selectedRouteId.value === id && routeStationCache.value[id]) {
    routeLoading.value = false
    stations.value = routeStationCache.value[id]
    selectedStationId.value = stations.value[0]?.id || 0
    return
  }
  const requestSeq = ++routeRequestSeq
  selectedRouteId.value = id
  const cachedStations = routeStationCache.value[id]
  if (cachedStations) {
    routeLoading.value = false
    stations.value = cachedStations
    selectedStationId.value = cachedStations[0]?.id || 0
    return
  }
  try {
    routeLoading.value = true
    errorText.value = ''
    const [stationRows, stationFlowRows, routeStat] = await Promise.all([
      api.stations(id),
      api.stationFlow(id, range.value[0], range.value[1], 50),
      ensureRouteStat(id)
    ])
    if (requestSeq !== routeRequestSeq) return
    routeStats.value = { ...routeStats.value, [id]: routeStat }
    const flowMap = new Map(stationFlowRows.map((item) => [Number(item.stationId), Number(item.boardingCount || 0)]))
    const counts = stationRows.map((station) => flowMap.get(station.id) || 0)
    const nextStations = stationRows.map((station) => ({
      ...station,
      boardingCount: flowMap.get(station.id) || 0,
      statusLevel: statusByPassenger(flowMap.get(station.id) || 0, counts)
    }))
    routeStationCache.value = { ...routeStationCache.value, [id]: nextStations }
    stations.value = nextStations
    selectedStationId.value = stations.value[0]?.id || 0
  } catch (error) {
    if (requestSeq !== routeRequestSeq) return
    errorText.value = (error as Error).message || '首页线路数据加载失败，请稍后刷新。'
  } finally {
    if (requestSeq === routeRequestSeq) routeLoading.value = false
  }
}

function selectStation(id: number) {
  selectedStationId.value = id
}

async function loadOverview(routeRows: RouteItem[]) {
  const entries = await Promise.all(
    routeRows.map(async (route) => {
      const [routeFlowRows, loadRateRows] = await Promise.all([
        api.routeFlow(route.id, range.value[0], range.value[1]),
        api.loadRate(route.id, range.value[0], range.value[1])
      ])
      return [route.id, buildRouteStat(routeFlowRows, loadRateRows)] as const
    })
  )
  routeStats.value = Object.fromEntries(entries)
}

async function ensureRouteStat(id: number) {
  const cached = routeStats.value[id]
  if (cached) return cached
  const [routeFlowRows, loadRateRows] = await Promise.all([api.routeFlow(id, range.value[0], range.value[1]), api.loadRate(id, range.value[0], range.value[1])])
  return buildRouteStat(routeFlowRows, loadRateRows)
}

function buildRouteStat(routeFlowRows: Record<string, unknown>[], loadRateRows: Record<string, unknown>[]): RouteStat {
  const passengerCount = routeFlowRows.reduce((sum, row) => sum + Number(row.passengerCount || 0), 0)
  const rates = loadRateRows.map((row) => Math.min(100, Number(row.loadRate || 0))).filter((value) => value > 0)
  const avgLoadRate = rates.length ? rates.reduce((sum, value) => sum + value, 0) / rates.length : 0
  const peakLoadRate = Math.max(0, ...rates)
  return { passengerCount, avgLoadRate, peakLoadRate, level: statusByLoad(avgLoadRate) }
}

async function reloadByRange() {
  if (!range.value?.[0] || !range.value?.[1] || !routes.value.length) return
  try {
    loading.value = true
    errorText.value = ''
    routeRequestSeq += 1
    routeStats.value = {}
    routeStationCache.value = {}
    stations.value = []
    selectedStationId.value = 0
    const routeId = selectedRouteId.value || routes.value[0]?.id || 0
    await loadOverview(routes.value)
    if (routeId) await selectRoute(routeId, { expandOverlay: false })
  } catch (error) {
    errorText.value = (error as Error).message || '首页统计区间数据加载失败，请稍后刷新。'
  } finally {
    loading.value = false
  }
}

function statusByLoad(value: number): StatusLevel {
  if (value >= 85) return 'high'
  if (value >= 60) return 'normal'
  return 'low'
}

function statusByPassenger(value: number, values: number[]): StatusLevel {
  const max = Math.max(0, ...values)
  if (!value || !max) return 'low'
  if (value >= max * 0.7) return 'high'
  if (value >= max * 0.35) return 'normal'
  return 'low'
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
      await selectRoute(routes.value[0].id, { expandOverlay: false })
    }
  } catch (error) {
    errorText.value = (error as Error).message || '首页数据加载失败，请稍后刷新。'
  } finally {
    loading.value = false
  }
})
</script>
