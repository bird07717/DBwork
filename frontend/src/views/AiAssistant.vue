<template>
  <section class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">AI 助手</h1>
        <p class="page-subtitle">基于统计数据生成客流分析和调度建议，当前使用本地规则分析能力</p>
      </div>
    </div>

    <el-alert v-if="errorText" :title="errorText" type="error" show-icon style="margin-bottom: 12px" />
    <div class="ai-workspace">
      <section class="panel ai-control-panel" v-loading="routeLoading">
        <div class="panel-section-head">
          <div>
            <span>输入</span>
            <strong>调度分析条件</strong>
          </div>
        </div>
        <div class="ai-form">
          <el-select v-model="routeId" placeholder="选择线路">
            <el-option label="全部线路" :value="0" />
            <el-option v-for="route in routes" :key="route.id" :label="route.routeName" :value="route.id" />
          </el-select>
          <el-date-picker v-model="range" type="daterange" value-format="YYYY-MM-DD" start-placeholder="开始日期" end-placeholder="结束日期" />
          <el-input v-model="question" type="textarea" :rows="5" />
          <el-button type="primary" :loading="loading" @click="submit">提交分析</el-button>
        </div>
        <div class="quick-questions">
          <el-button v-for="item in examples" :key="item" size="small" @click="question = item">{{ item }}</el-button>
        </div>
      </section>

      <section class="ai-report-panel">
        <div class="ai-report-head">
          <div>
            <span>智能分析助手</span>
            <strong>{{ analysis?.summary || '等待生成分析报告' }}</strong>
          </div>
          <el-tag :type="analysisMode.type">{{ analysisMode.label }}</el-tag>
        </div>

        <div v-if="analysis" class="ai-report-grid">
          <div v-if="analysis?.fallback" class="ai-mode-note">
            当前结果由本地统计规则生成，后续接入大模型后可扩展为自然语言推理分析。
          </div>
          <div class="ai-report-block">
            <span>关键发现</span>
            <ul>
              <li v-for="item in analysis.keyFindings || []" :key="item">{{ item }}</li>
            </ul>
          </div>
          <div class="ai-report-block accent">
            <span>调度建议</span>
            <p>{{ analysis.suggestion || '暂无建议' }}</p>
          </div>
          <div class="ai-report-actions">
            <el-button :disabled="routeId <= 0" @click="$router.push(`/routes/${routeId}`)">查看线路详情</el-button>
            <el-button type="primary" :disabled="routeId <= 0" @click="$router.push({ path: '/analysis', query: { routeId } })">查看客流分析</el-button>
          </div>
        </div>
        <div v-else class="ai-empty-state">
          <strong>选择线路和时间范围后提交问题</strong>
          <p>系统会结合客流趋势、站点排行和满载率生成可读的辅助判断。</p>
        </div>
      </section>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { api, defaultRange, type RouteItem } from '../services/domain'

const routes = ref<RouteItem[]>([])
const currentRoute = useRoute()
const routeId = ref(0)
const range = ref<[string, string]>(defaultRange())
const question = ref('最近一周哪条线路客流最高？')
const loading = ref(false)
const routeLoading = ref(false)
const errorText = ref('')
const analysis = ref<any>()
const analysisMode = computed(() => {
  if (!analysis.value) return { label: '待分析', type: 'info' as const }
  return analysis.value.fallback ? { label: '本地规则分析', type: 'warning' as const } : { label: 'AI 分析', type: 'success' as const }
})
const examples = [
  '最近一周哪条线路客流最高？',
  '1路公交早高峰是否拥挤？',
  '哪个站点上车人数最多？',
  '正常时段车辆利用率是否偏低？'
]

async function submit() {
  try {
    loading.value = true
    const [startDate, endDate] = range.value
    const data = await api.analyze({ question: question.value, routeId: routeId.value, startDate, endDate })
    analysis.value = data.analysis
  } catch (error) {
    ElMessage.error((error as Error).message)
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  try {
    routeLoading.value = true
    errorText.value = ''
    routes.value = await api.routes()
    const queryRouteId = Number(currentRoute.query.routeId || 0)
    if (routes.value.some((route) => route.id === queryRouteId)) {
      routeId.value = queryRouteId
    }
  } catch (error) {
    errorText.value = (error as Error).message || '线路列表加载失败，请稍后刷新。'
  } finally {
    routeLoading.value = false
  }
})
</script>
