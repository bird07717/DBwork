<template>
  <section class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">{{ route?.routeName || '线路详情' }}</h1>
        <p class="page-subtitle">{{ routeSummary }}</p>
      </div>
      <div class="page-actions">
        <el-button @click="$router.push('/')">返回首页</el-button>
        <el-button type="primary" @click="$router.push({ path: '/analysis', query: { routeId: route?.id } })">查看客流分析</el-button>
      </div>
    </div>

    <el-alert v-if="errorText" :title="errorText" type="error" show-icon style="margin-bottom: 12px" />
    <div class="metric-strip" v-loading="loading">
      <div class="panel metric compact">
        <div class="metric-label">站点数量</div>
        <div class="metric-value">{{ stations.length }}</div>
      </div>
      <div class="panel metric compact">
        <div class="metric-label">近 7 日客流</div>
        <div class="metric-value">{{ formatNumber(totalFlow) }}</div>
      </div>
      <div class="panel metric compact">
        <div class="metric-label">平均满载率</div>
        <div class="metric-value">{{ formatPercent(avgLoadRate) }}</div>
      </div>
      <div class="panel metric compact">
        <div class="metric-label">高客流站点</div>
        <div class="metric-value small">{{ topStation?.stationName || '-' }}</div>
      </div>
    </div>

    <div class="route-detail-layout">
      <div class="route-map-stack">
        <RouteMap :stations="stations" :selected-station-id="selectedStationId" :route-status="routeStatus" @select-station="selectStation" />
      </div>
      <aside class="panel route-station-panel">
        <div class="panel-section-head">
          <div>
            <span>站点顺序</span>
            <strong>{{ selectedStation?.stationName || '选择站点' }}</strong>
          </div>
          <el-tag :type="tagType(selectedStation?.statusLevel || 'low')" effect="dark">{{ statusText(selectedStation?.statusLevel || 'low') }}</el-tag>
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

    <div class="analytics-grid">
      <div class="panel chart-panel span-2">
        <div class="panel-section-head">
          <div>
            <span>趋势</span>
            <strong>近 7 日客流变化</strong>
          </div>
          <el-button size="small" @click="$router.push({ path: '/ai', query: { routeId: route?.id } })">生成分析</el-button>
        </div>
        <ChartBox :option="flowOption" />
      </div>
      <div class="panel">
        <div class="panel-section-head">
          <div>
            <span>站点排行</span>
            <strong>上车客流 Top 5</strong>
          </div>
        </div>
        <div class="rank-list">
          <div v-for="station in topStations" :key="station.id" class="rank-row">
            <span>{{ station.stationName }}</span>
            <strong>{{ formatNumber(station.boardingCount) }}</strong>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import ChartBox from '../components/ChartBox.vue'
import RouteMap from '../components/RouteMap.vue'
import { api, defaultRange, type RouteItem, type StationItem } from '../services/domain'

type StatusLevel = 'high' | 'normal' | 'low'

interface RouteStation extends StationItem {
  boardingCount: number
  statusLevel: StatusLevel
}

const routeApi = useRoute()
const route = ref<RouteItem>()
const stations = ref<RouteStation[]>([])
const stationFlow = ref<Record<string, unknown>[]>([])
const flow = ref<Record<string, unknown>[]>([])
const loadRates = ref<Record<string, unknown>[]>([])
const selectedStationId = ref(0)
const loading = ref(false)
const errorText = ref('')
const range = defaultRange()

const routeSummary = computed(() => (route.value ? `${route.value.startStationName} → ${route.value.endStationName}` : '线路加载中'))
const totalFlow = computed(() => flow.value.reduce((sum, item) => sum + Number(item.passengerCount || 0), 0))
const avgLoadRate = computed(() => {
  if (!loadRates.value.length) return 0
  return loadRates.value.reduce((sum, item) => sum + Math.min(100, Number(item.loadRate || 0)), 0) / loadRates.value.length
})
const routeStatus = computed<StatusLevel>(() => (avgLoadRate.value >= 85 ? 'high' : avgLoadRate.value >= 60 ? 'normal' : 'low'))
const selectedStation = computed(() => stations.value.find((station) => station.id === selectedStationId.value))
const topStations = computed(() => [...stations.value].sort((a, b) => b.boardingCount - a.boardingCount).slice(0, 5))
const topStation = computed(() => topStations.value[0])
const flowOption = computed(() => ({
  color: ['#2f8f83'],
  grid: { left: 44, right: 20, top: 28, bottom: 34, containLabel: true },
  tooltip: { trigger: 'axis' },
  xAxis: { type: 'category', data: flow.value.map((item) => item.statDate) },
  yAxis: { type: 'value' },
  series: [{ name: '客流', type: 'line', smooth: true, areaStyle: { opacity: 0.14 }, data: flow.value.map((item) => item.passengerCount) }]
}))

function selectStation(id: number) {
  selectedStationId.value = id
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
    const id = String(routeApi.params.id)
    const [routeRow, stationRows, routeFlowRows, stationFlowRows, loadRateRows] = await Promise.all([
      api.route(id),
      api.stations(id),
      api.routeFlow(Number(id), range[0], range[1]),
      api.stationFlow(Number(id), range[0], range[1], 50),
      api.loadRate(Number(id), range[0], range[1])
    ])
    route.value = routeRow
    stationFlow.value = stationFlowRows
    loadRates.value = loadRateRows
    const flowMap = new Map(stationFlowRows.map((item) => [Number(item.stationId), Number(item.boardingCount || 0)]))
    const counts = stationRows.map((station) => flowMap.get(station.id) || 0)
    stations.value = stationRows.map((station) => ({
      ...station,
      boardingCount: flowMap.get(station.id) || 0,
      statusLevel: statusByPassenger(flowMap.get(station.id) || 0, counts)
    }))
    selectedStationId.value = stations.value[0]?.id || 0
    flow.value = routeFlowRows
  } catch (error) {
    errorText.value = (error as Error).message || '线路详情加载失败，请稍后刷新。'
  } finally {
    loading.value = false
  }
})
</script>
