<template>
  <section class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">调度建议管理</h1>
        <p class="page-subtitle">选择线路、日期和时段生成 AI/规则调度建议</p>
      </div>
    </div>
    <div class="dispatch-layout">
      <section class="panel dispatch-form-panel" v-loading="routeLoading">
        <div class="panel-section-head">
          <div>
            <span>生成</span>
            <strong>调度建议条件</strong>
          </div>
        </div>
        <div class="dispatch-form">
          <el-select v-model="form.routeId" placeholder="线路">
            <el-option v-for="route in routes" :key="route.id" :label="route.routeName" :value="route.id" />
          </el-select>
          <el-date-picker v-model="range" type="daterange" value-format="YYYY-MM-DD" start-placeholder="开始日期" end-placeholder="结束日期" :clearable="false" />
          <el-select v-model="form.periodType" placeholder="时段">
            <el-option label="早高峰" value="morning_peak" />
            <el-option label="晚高峰" value="evening_peak" />
            <el-option label="正常时段" value="normal" />
          </el-select>
          <el-button type="primary" :loading="loading" @click="generate">生成建议</el-button>
        </div>
      </section>

      <section class="panel dispatch-result-panel">
        <div class="panel-section-head">
          <div>
            <span>结果</span>
            <strong>{{ result ? '最新建议' : '等待生成' }}</strong>
          </div>
          <el-tag :type="adviceTag(result?.adviceLevel)">{{ result?.adviceLevel || '未生成' }}</el-tag>
        </div>
        <template v-if="result">
          <div class="dispatch-advice-card" :class="`level-${adviceLevelClass(result.adviceLevel)}`">
            <span>建议内容</span>
            <h2>{{ result.adviceContent }}</h2>
            <p>{{ result.aiSummary }}</p>
          </div>
          <div class="dispatch-advice-meta">
            <div>
              <span>建议级别</span>
              <strong>{{ result.adviceLevel || '-' }}</strong>
            </div>
            <div>
              <span>生成线路</span>
              <strong>{{ selectedRouteName }}</strong>
            </div>
            <div>
              <span>分析时段</span>
              <strong>{{ periodText(form.periodType) }}</strong>
            </div>
            <div>
              <span>客流量</span>
              <strong>{{ formatNumber(Number(result.metrics?.passengerCount || 0)) }}</strong>
            </div>
            <div>
              <span>平均满载率</span>
              <strong>{{ formatPercent(Number(result.metrics?.avgLoadRate || 0)) }}</strong>
            </div>
            <div>
              <span>班次数</span>
              <strong>{{ formatNumber(Number(result.metrics?.scheduleCount || 0)) }}</strong>
            </div>
          </div>
          <div class="dispatch-evidence-grid">
            <div class="dispatch-evidence-card">
              <span>规则依据</span>
              <strong>{{ ruleBasis }}</strong>
            </div>
            <div class="dispatch-evidence-card">
              <span>AI 风险提示</span>
              <strong>{{ result.aiResult?.risk || '需结合车辆可用数、司机排班和道路情况复核。' }}</strong>
            </div>
          </div>
          <div class="dispatch-hotspot-panel">
            <div class="panel-section-head">
              <div>
                <span>站点依据</span>
                <strong>热点站点 TOP 3</strong>
              </div>
            </div>
            <div class="hotspot-list" v-loading="hotspotLoading">
              <div v-for="station in hotspotStations" :key="String(station.stationId)" class="hotspot-row">
                <span>{{ station.stationName }}</span>
                <strong>{{ formatNumber(Number(station.boardingCount || 0)) }} 人次</strong>
              </div>
              <div v-if="!hotspotStations.length" class="empty-hint">暂无站点排行数据。</div>
            </div>
          </div>
        </template>
        <div v-else class="empty-hint">选择线路、日期和时段后生成建议。</div>
      </section>
    </div>

    <div class="panel" style="margin-top: 16px">
      <div class="panel-section-head">
        <div>
          <span>历史</span>
          <strong>调度建议记录</strong>
        </div>
      </div>
      <el-alert v-if="errorText" :title="errorText" type="error" show-icon style="margin-bottom: 12px" />
      <el-table :data="records" border v-loading="listLoading" empty-text="暂无调度建议，选择条件后点击生成建议">
        <el-table-column type="expand">
          <template #default="{ row }">
            <div class="history-detail">
              <div>
                <span>建议内容</span>
                <strong>{{ row.advice_content || row.adviceContent || '-' }}</strong>
              </div>
              <div>
                <span>AI 摘要</span>
                <strong>{{ row.ai_summary || row.aiSummary || '-' }}</strong>
              </div>
              <div>
                <span>规则依据</span>
                <strong>{{ historyRuleBasis(row) }}</strong>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="routeName" label="线路" />
        <el-table-column label="日期范围" min-width="180">
          <template #default="{ row }">{{ row.start_date || row.startDate }} 至 {{ row.end_date || row.endDate }}</template>
        </el-table-column>
        <el-table-column prop="period_type" label="时段">
          <template #default="{ row }">
            <el-tag type="warning" effect="light">{{ periodText(String(row.period_type || row.periodType || '')) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="passenger_count" label="客流">
          <template #default="{ row }">{{ formatNumber(Number(row.passenger_count || 0)) }}</template>
        </el-table-column>
        <el-table-column prop="avg_load_rate" label="满载率">
          <template #default="{ row }">{{ formatPercent(Number(row.avg_load_rate || 0)) }}</template>
        </el-table-column>
        <el-table-column prop="advice_level" label="级别">
          <template #default="{ row }">
            <el-tag :type="adviceTag(String(row.advice_level || ''))" effect="dark">{{ row.advice_level || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="create_time" label="生成时间">
          <template #default="{ row }">{{ formatDateTime(row.create_time) }}</template>
        </el-table-column>
      </el-table>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { get, post } from '../../services/api'
import { api, defaultRange, type PageResult, type RouteItem } from '../../services/domain'

const routes = ref<RouteItem[]>([])
const range = ref<[string, string]>(defaultRange())
const form = reactive({ routeId: 1, periodType: 'morning_peak' })
const loading = ref(false)
const listLoading = ref(false)
const hotspotLoading = ref(false)
const errorText = ref('')
const result = ref<any>()
const records = ref<Record<string, unknown>[]>([])
const hotspotStations = ref<Record<string, unknown>[]>([])

const ruleBasis = computed(() => {
  if (!result.value) return '-'
  return buildRuleBasis(form.periodType, Number(result.value.metrics?.avgLoadRate || 0))
})

async function generate() {
  if (!range.value?.length) {
    ElMessage.warning('请选择分析日期范围')
    return
  }
  try {
    loading.value = true
    const [startDate, endDate] = range.value
    result.value = await post('/api/admin/dispatch-advice/generate', { ...form, startDate, endDate })
    await loadHotspots()
    await loadList()
  } catch (error) {
    ElMessage.error((error as Error).message)
  } finally {
    loading.value = false
  }
}

async function loadHotspots() {
  if (!form.routeId || !range.value?.length) return
  try {
    hotspotLoading.value = true
    const [startDate, endDate] = range.value
    hotspotStations.value = (await api.stationFlow(form.routeId, startDate, endDate, 3)) as Record<string, unknown>[]
  } catch {
    hotspotStations.value = []
  } finally {
    hotspotLoading.value = false
  }
}

async function loadList() {
  try {
    listLoading.value = true
    errorText.value = ''
    const data = await get<PageResult<Record<string, unknown>>>('/api/admin/dispatch-advice?page=1&size=10')
    records.value = data.records
  } catch (error) {
    records.value = []
    errorText.value = (error as Error).message
  } finally {
    listLoading.value = false
  }
}

function adviceTag(level?: string) {
  if (!level) return 'info'
  if (level.includes('高') || level === 'high') return 'danger'
  if (level.includes('中') || level.includes('关注') || level === 'medium') return 'warning'
  return 'success'
}

function adviceLevelClass(level?: string) {
  if (!level) return 'none'
  if (level.includes('高') || level === 'high') return 'high'
  if (level.includes('中') || level.includes('关注') || level === 'medium') return 'normal'
  return 'low'
}

function periodText(value: string) {
  if (value === 'morning_peak') return '早高峰'
  if (value === 'evening_peak') return '晚高峰'
  if (value === 'normal') return '正常时段'
  return value || '-'
}

function formatNumber(value = 0) {
  return Math.round(value).toLocaleString('zh-CN')
}

function formatPercent(value = 0) {
  return `${Number(value || 0).toFixed(1)}%`
}

function formatDateTime(value: unknown) {
  if (!value) return '-'
  return String(value).replace('T', ' ')
}

function buildRuleBasis(periodType: string, avgLoadRate: number) {
  const period = periodText(periodType)
  if (periodType !== 'normal' && avgLoadRate >= 85) {
    return `当前${period}平均满载率为 ${formatPercent(avgLoadRate)}，超过 85% 加车阈值，因此建议增加班次或缩短发车间隔。`
  }
  if (periodType !== 'normal' && avgLoadRate >= 60) {
    return `当前${period}平均满载率为 ${formatPercent(avgLoadRate)}，处于 60%-85% 观察区间，因此建议维持班次并持续观察热点站点。`
  }
  if (periodType === 'normal' && avgLoadRate < 30) {
    return `当前${period}平均满载率为 ${formatPercent(avgLoadRate)}，低于 30% 低利用率阈值，因此建议适当延长发车间隔或减少班次。`
  }
  return `当前${period}平均满载率为 ${formatPercent(avgLoadRate)}，未触发高风险阈值，建议保持当前调度并继续观察。`
}

function historyRuleBasis(row: Record<string, unknown>) {
  const periodType = String(row.period_type || row.periodType || '')
  const avgLoadRate = Number(row.avg_load_rate || row.avgLoadRate || 0)
  return buildRuleBasis(periodType, avgLoadRate)
}

const selectedRouteName = computed(() => routes.value.find((route) => route.id === form.routeId)?.routeName || '-')

const routeLoading = ref(false)

async function loadRoutes() {
  try {
    routeLoading.value = true
    routes.value = await api.routes()
    form.routeId = routes.value[0]?.id || 1
  } catch (error) {
    errorText.value = (error as Error).message || '线路数据加载失败，请稍后刷新。'
  } finally {
    routeLoading.value = false
  }
}

onMounted(async () => {
  await loadRoutes()
  await loadList()
})
</script>
