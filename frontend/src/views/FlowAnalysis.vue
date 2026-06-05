<template>
  <section class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">客流分析</h1>
        <p class="page-subtitle">按线路查看日趋势、站点排行和时段对比</p>
      </div>
      <div class="page-actions">
        <el-button @click="$router.push(`/routes/${routeId}`)">线路详情</el-button>
        <el-button type="primary" @click="$router.push({ path: '/ai', query: { routeId } })">生成分析</el-button>
      </div>
    </div>

    <el-alert v-if="errorText" :title="errorText" type="error" show-icon style="margin-bottom: 12px" />
    <div class="analysis-toolbar panel">
      <div>
        <span>线路</span>
        <el-select v-model="routeId" placeholder="选择线路" @change="load">
          <el-option v-for="route in routes" :key="route.id" :label="route.routeName" :value="route.id" />
        </el-select>
      </div>
      <div>
        <span>日期范围</span>
        <el-date-picker v-model="range" type="daterange" value-format="YYYY-MM-DD" start-placeholder="开始日期" end-placeholder="结束日期" @change="load" />
      </div>
      <el-button type="primary" @click="load">刷新分析</el-button>
    </div>

    <div class="metric-strip" v-loading="loading">
      <div class="panel metric compact">
        <div class="metric-label">区间总客流</div>
        <div class="metric-value">{{ formatNumber(totalFlow) }}</div>
      </div>
      <div class="panel metric compact">
        <div class="metric-label">最高站点</div>
        <div class="metric-value small">{{ topStation?.stationName || '-' }}</div>
      </div>
      <div class="panel metric compact">
        <div class="metric-label">高峰时段</div>
        <div class="metric-value small">{{ busiestPeriod?.periodName || '-' }}</div>
      </div>
      <div class="panel metric compact">
        <div class="metric-label">平均满载率</div>
        <div class="metric-value">{{ formatPercent(avgPeakLoadRate) }}</div>
      </div>
    </div>

    <div class="analytics-grid">
      <div class="panel chart-panel span-2">
        <div class="panel-section-head">
          <div>
            <span>趋势</span>
            <strong>日客流时间序列</strong>
          </div>
        </div>
        <ChartBox :option="routeFlowOption" />
      </div>
      <div class="panel chart-panel">
        <div class="panel-section-head">
          <div>
            <span>站点</span>
            <strong>上车客流排行</strong>
          </div>
        </div>
        <ChartBox :option="stationOption" />
      </div>
      <div class="panel chart-panel">
        <div class="panel-section-head">
          <div>
            <span>时段</span>
            <strong>高峰压力对比</strong>
          </div>
        </div>
        <ChartBox :option="peakOption" />
      </div>
      <div class="panel span-2">
        <div class="panel-section-head">
          <div>
            <span>压力分布</span>
            <strong>时段运行状态</strong>
          </div>
        </div>
        <div class="period-status-grid">
          <div v-for="period in peakAnalysis" :key="String(period.periodType)" class="period-status-card" :class="`status-${periodLevel(period)}`">
            <span>{{ period.periodName }}</span>
            <strong>{{ formatNumber(Number(period.avgPassengerPerSchedule || 0)) }}</strong>
            <em>平均满载率 {{ formatPercent(clampPercent(Number(period.avgLoadRate || 0))) }}</em>
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
import { api, defaultRange, type RouteItem } from '../services/domain'

const routes = ref<RouteItem[]>([])
const currentRoute = useRoute()
const routeId = ref(1)
const range = ref<[string, string]>(defaultRange())
const routeFlow = ref<Record<string, unknown>[]>([])
const stationFlow = ref<Record<string, unknown>[]>([])
const peakAnalysis = ref<Record<string, unknown>[]>([])
const loading = ref(false)
const errorText = ref('')

const totalFlow = computed(() => routeFlow.value.reduce((sum, item) => sum + Number(item.passengerCount || 0), 0))
const topStation = computed(() => stationFlow.value[0])
const busiestPeriod = computed(() => [...peakAnalysis.value].sort((a, b) => Number(b.avgPassengerPerSchedule || 0) - Number(a.avgPassengerPerSchedule || 0))[0])
const avgPeakLoadRate = computed(() => {
  if (!peakAnalysis.value.length) return 0
  return peakAnalysis.value.reduce((sum, item) => sum + clampPercent(Number(item.avgLoadRate || 0)), 0) / peakAnalysis.value.length
})
const routeFlowOption = computed(() => ({
  color: ['#2f8f83'],
  grid: { left: 48, right: 24, top: 36, bottom: 48, containLabel: true },
  tooltip: { trigger: 'axis' },
  xAxis: { type: 'category', data: routeFlow.value.map((item) => item.statDate) },
  yAxis: { type: 'value' },
  series: [{ name: '客流', type: 'line', smooth: true, areaStyle: { opacity: 0.14 }, data: routeFlow.value.map((item) => item.passengerCount) }]
}))

const stationOption = computed(() => ({
  grid: { left: 156, right: 24, top: 24, bottom: 24 },
  tooltip: {},
  xAxis: { type: 'value' },
  yAxis: {
    type: 'category',
    data: stationFlow.value.map((item) => item.stationName).reverse(),
    axisLabel: { width: 138, overflow: 'truncate', interval: 0 }
  },
  series: [{ name: '上车客流', type: 'bar', data: stationFlow.value.map((item) => item.boardingCount).reverse(), itemStyle: { color: '#2f8f83', borderRadius: [0, 4, 4, 0] } }]
}))

const peakOption = computed(() => ({
  grid: { left: 48, right: 24, top: 36, bottom: 42, containLabel: true },
  tooltip: {
    formatter: (items: any) => {
      const item = Array.isArray(items) ? items[0] : items
      const row = peakAnalysis.value[item.dataIndex] || {}
      return `${row.periodName}<br/>总客流：${row.passengerCount || 0}<br/>班次数：${row.scheduleCount || 0}<br/>班均客流：${row.avgPassengerPerSchedule || 0}<br/>平均满载率：${clampPercent(Number(row.avgLoadRate || 0)).toFixed(2)}%`
    }
  },
  xAxis: { type: 'category', data: peakAnalysis.value.map((item) => item.periodName) },
  yAxis: { type: 'value' },
  series: [{ name: '班均客流', type: 'bar', data: peakAnalysis.value.map((item) => item.avgPassengerPerSchedule), itemStyle: { color: '#de6b48', borderRadius: [4, 4, 0, 0] } }]
}))

async function load() {
  if (!routeId.value || !range.value) return
  try {
    loading.value = true
    errorText.value = ''
    const [start, end] = range.value
    routeFlow.value = await api.routeFlow(routeId.value, start, end)
    stationFlow.value = await api.stationFlow(routeId.value, start, end)
    peakAnalysis.value = await api.peakAnalysis(routeId.value, start, end)
  } catch (error) {
    errorText.value = (error as Error).message || '客流分析数据加载失败，请稍后刷新。'
  } finally {
    loading.value = false
  }
}

function periodLevel(period: Record<string, unknown>) {
  const loadRate = clampPercent(Number(period.avgLoadRate || 0))
  if (loadRate >= 85) return 'high'
  if (loadRate >= 60) return 'normal'
  return 'low'
}

function clampPercent(value: number) {
  return Math.max(0, Math.min(100, value))
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
    const queryRouteId = Number(currentRoute.query.routeId || 0)
    routeId.value = routes.value.some((route) => route.id === queryRouteId) ? queryRouteId : routes.value[0]?.id || 1
  } catch (error) {
    errorText.value = (error as Error).message || '线路列表加载失败，请稍后刷新。'
  } finally {
    loading.value = false
  }
  await load()
})
</script>
