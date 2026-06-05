<template>
  <section class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">后台首页</h1>
        <p class="page-subtitle">基础数据规模和今日系统状态</p>
      </div>
      <el-button @click="logout">退出登录</el-button>
    </div>
    <el-alert v-if="errorText" :title="errorText" type="error" show-icon style="margin-bottom: 12px" />
    <div class="metric-strip admin-metrics" v-loading="loading">
      <div v-for="item in metrics" :key="item.label" class="panel metric compact">
        <div class="metric-label">{{ item.label }}</div>
        <div class="metric-value">{{ item.value }}</div>
      </div>
    </div>

    <div class="admin-dashboard-grid">
      <div class="panel">
        <div class="panel-section-head">
          <div>
            <span>操作</span>
            <strong>快捷入口</strong>
          </div>
        </div>
        <div class="quick-entry-grid">
          <button @click="$router.push('/admin/data')">
            <strong>基础数据管理</strong>
            <span>维护线路、站点、车辆、司机和班次</span>
          </button>
          <button @click="$router.push('/admin/dispatch')">
            <strong>调度建议管理</strong>
            <span>生成并查看规则/AI 调度建议</span>
          </button>
          <button @click="$router.push('/analysis')">
            <strong>客流分析</strong>
            <span>查看游客端统计图表和站点排行</span>
          </button>
        </div>
      </div>
      <div class="panel">
        <div class="panel-section-head">
          <div>
            <span>状态</span>
            <strong>今日概览</strong>
          </div>
          <el-tag type="success">可演示</el-tag>
        </div>
        <div class="admin-health-list">
          <div>
            <span>乘车记录</span>
            <strong>{{ summary.rideCount || 0 }}</strong>
          </div>
          <div>
            <span>可调度班次</span>
            <strong>{{ summary.scheduleCount || 0 }}</strong>
          </div>
          <div>
            <span>基础数据</span>
            <strong>{{ baseDataCount }}</strong>
          </div>
        </div>
        <p class="muted">当前数据库可支撑游客分析、线路地图展示和 AI 建议生成。</p>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { get } from '../../services/api'
import { clearToken } from '../../services/auth'

const router = useRouter()
const summary = ref<Record<string, number>>({})
const loading = ref(false)
const errorText = ref('')
const labels: Record<string, string> = {
  routeCount: '线路',
  stationCount: '站点',
  vehicleCount: '车辆',
  driverCount: '司机',
  scheduleCount: '班次',
  rideCount: '乘车记录'
}

const metrics = computed(() => Object.entries(labels).map(([key, label]) => ({ label, value: summary.value[key] || 0 })))
const baseDataCount = computed(() => (summary.value.routeCount || 0) + (summary.value.stationCount || 0) + (summary.value.vehicleCount || 0) + (summary.value.driverCount || 0))

function logout() {
  clearToken()
  router.push('/admin/login')
}

async function load() {
  try {
    loading.value = true
    errorText.value = ''
    summary.value = await get<Record<string, number>>('/api/admin/dashboard/summary')
  } catch (error) {
    errorText.value = (error as Error).message || '后台首页数据加载失败，请稍后刷新。'
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
