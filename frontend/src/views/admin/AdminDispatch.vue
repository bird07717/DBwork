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
          <el-date-picker v-model="range" type="daterange" value-format="YYYY-MM-DD" start-placeholder="开始日期" end-placeholder="结束日期" />
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
          <h2>{{ result.adviceContent }}</h2>
          <p class="muted">{{ result.aiSummary }}</p>
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
        <el-table-column prop="routeName" label="线路" />
        <el-table-column prop="period_type" label="时段" />
        <el-table-column prop="passenger_count" label="客流" />
        <el-table-column prop="avg_load_rate" label="满载率" />
        <el-table-column prop="advice_level" label="级别" />
        <el-table-column prop="create_time" label="生成时间" />
      </el-table>
    </div>
  </section>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { get, post } from '../../services/api'
import { api, defaultRange, type PageResult, type RouteItem } from '../../services/domain'

const routes = ref<RouteItem[]>([])
const range = ref<[string, string]>(defaultRange())
const form = reactive({ routeId: 1, periodType: 'morning_peak' })
const loading = ref(false)
const listLoading = ref(false)
const errorText = ref('')
const result = ref<any>()
const records = ref<Record<string, unknown>[]>([])

async function generate() {
  try {
    loading.value = true
    const [startDate, endDate] = range.value
    result.value = await post('/api/admin/dispatch-advice/generate', { ...form, startDate, endDate })
    await loadList()
  } catch (error) {
    ElMessage.error((error as Error).message)
  } finally {
    loading.value = false
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
  if (level.includes('高')) return 'danger'
  if (level.includes('中') || level.includes('关注')) return 'warning'
  return 'success'
}

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
