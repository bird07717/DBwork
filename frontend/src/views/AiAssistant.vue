<template>
  <section class="page ai-page">
    <div class="page-header ai-hero">
      <div>
        <span class="command-kicker">AI Dispatch Copilot</span>
        <h1 class="page-title">AI 调度决策报告</h1>
        <p class="page-subtitle">把线路、日期、客流统计转化为可执行的风险提示和调度建议。</p>
      </div>
    </div>

    <el-alert v-if="errorText" :title="errorText" type="error" show-icon style="margin-bottom: 12px" />
    <div class="ai-workspace">
      <section class="panel ai-control-panel" v-loading="routeLoading">
        <div class="panel-section-head">
          <div>
            <span>分析输入</span>
            <strong>调度分析条件</strong>
          </div>
        </div>
        <div class="ai-form">
          <el-select v-model="routeId" placeholder="选择线路">
            <el-option label="全部线路" :value="0" />
            <el-option v-for="route in routes" :key="route.id" :label="route.routeName" :value="route.id" />
          </el-select>
          <el-select v-model="model" placeholder="选择模型">
            <el-option label="DeepSeek V4 Flash" value="deepseek-v4-flash" />
            <el-option label="DeepSeek V4 Pro" value="deepseek-v4-pro" />
          </el-select>
          <el-date-picker v-model="range" type="daterange" value-format="YYYY-MM-DD" start-placeholder="开始日期" end-placeholder="结束日期" :clearable="false" />
          <el-input v-model="question" type="textarea" :rows="5" placeholder="请输入想分析的客流或调度问题" />
          <el-button type="primary" :loading="loading" @click="submit">生成调度报告</el-button>
        </div>
        <div class="quick-questions">
          <el-button v-for="item in examples" :key="item" size="small" @click="question = item">{{ item }}</el-button>
        </div>
      </section>

      <section class="ai-report-panel ai-decision-panel">
        <div class="ai-report-head">
          <div>
            <span>智能分析助手</span>
            <strong>{{ reportTitle }}</strong>
          </div>
          <el-tag :type="analysisMode.type">{{ analysisMode.label }}</el-tag>
        </div>

        <div v-if="analysis" class="ai-report-grid decision-report-grid">
          <div v-if="analysis?.fallback" class="ai-mode-note">
            当前结果由本地统计规则生成，后续接入大模型后可扩展为自然语言推理分析。
          </div>

          <div class="decision-summary-card">
            <span>报告摘要</span>
            <strong>{{ analysis.summary || '已生成客流分析报告' }}</strong>
            <p>{{ selectedRouteName }} · {{ range[0] }} 至 {{ range[1] }}</p>
          </div>

          <div class="decision-columns">
            <div class="ai-report-block">
              <span>关键发现</span>
              <ul>
                <li v-for="item in findings" :key="item">{{ item }}</li>
              </ul>
            </div>
            <div class="ai-report-block risk-block">
              <span>风险提示</span>
              <div class="risk-list">
                <div v-for="risk in riskItems" :key="risk.title" class="risk-item" :class="`risk-${risk.level}`">
                  <strong>{{ risk.title }}</strong>
                  <p>{{ risk.detail }}</p>
                </div>
              </div>
            </div>
          </div>

          <div class="ai-report-block accent dispatch-suggestion-card">
            <div>
              <span>调度建议</span>
              <strong>{{ suggestionLevel.label }}</strong>
            </div>
            <p>{{ analysis.suggestion || '暂无建议' }}</p>
          </div>

          <div class="ai-report-actions">
            <el-button :disabled="routeId <= 0" @click="$router.push(`/routes/${routeId}`)">查看线路详情</el-button>
            <el-button type="primary" :disabled="routeId <= 0" @click="$router.push({ path: '/analysis', query: { routeId } })">打开客流看板</el-button>
          </div>
        </div>
        <div v-else class="ai-empty-state">
          <strong>选择线路和时间范围后生成调度报告</strong>
          <p>系统会结合客流趋势、站点排行、满载率和问题意图输出摘要、关键发现、风险提示和建议。</p>
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
const model = ref('deepseek-v4-flash')
const range = ref<[string, string]>(defaultRange())
const question = ref('最近一周哪条线路客流最高？')
const loading = ref(false)
const routeLoading = ref(false)
const errorText = ref('')
const analysis = ref<any>()

const selectedRouteName = computed(() => routes.value.find((route) => route.id === routeId.value)?.routeName || '全部线路')
const reportTitle = computed(() => analysis.value?.summary || '等待生成分析报告')
const findings = computed<string[]>(() => {
  const items = analysis.value?.keyFindings || []
  return items.length ? items : ['暂无关键发现，请调整线路、日期范围或问题后重新生成。']
})
const analysisMode = computed(() => {
  if (!analysis.value) return { label: '待分析', type: 'info' as const }
  return analysis.value.fallback ? { label: '本地规则分析', type: 'warning' as const } : { label: 'AI 分析', type: 'success' as const }
})
const suggestionLevel = computed(() => {
  const text = `${analysis.value?.summary || ''}${analysis.value?.suggestion || ''}${findings.value.join('')}`
  if (/高|拥挤|告警|加密|加班|满载|风险/.test(text)) return { label: '高优先级建议', level: 'high' }
  if (/关注|优化|调整|观察|偏高/.test(text)) return { label: '中优先级建议', level: 'normal' }
  return { label: '常规优化建议', level: 'low' }
})
const riskItems = computed(() => {
  const text = findings.value.join('；')
  const risks = [
    {
      title: suggestionLevel.value.level === 'high' ? '高客流风险' : suggestionLevel.value.level === 'normal' ? '关注客流波动' : '运行平稳',
      detail: text,
      level: suggestionLevel.value.level
    },
    {
      title: '调度执行建议',
      detail: analysis.value?.suggestion || '保持常规班次，持续观察高峰时段和热点站点。',
      level: suggestionLevel.value.level
    }
  ]
  return risks
})
const examples = [
  '最近一周哪条线路客流最高？',
  '1路公交早高峰是否拥挤？',
  '哪个站点上车人数最多？',
  '正常时段车辆利用率是否偏低？'
]

async function submit() {
  if (!range.value?.length) {
    ElMessage.warning('请选择分析日期范围')
    return
  }
  try {
    loading.value = true
    const [startDate, endDate] = range.value
    const data = await api.analyze({ question: question.value, model: model.value, routeId: routeId.value, startDate, endDate })
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
