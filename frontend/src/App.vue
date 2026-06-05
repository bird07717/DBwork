<template>
  <el-config-provider>
    <div class="shell">
      <aside class="nav">
        <RouterLink class="brand" to="/">
          <span class="brand-mark" aria-hidden="true">
            <span class="brand-route-line"></span>
            <span class="brand-route-dot top"></span>
            <span class="brand-route-dot bottom"></span>
          </span>
          <span>
            公交客流调度平台
            <small>Bus Flow Dispatch</small>
          </span>
        </RouterLink>
        <nav class="nav-links">
          <div class="nav-section">游客端</div>
          <RouterLink to="/"><span>地图态势</span><em>Map</em></RouterLink>
          <RouterLink to="/analysis"><span>客流分析</span><em>Flow</em></RouterLink>
          <RouterLink to="/ai"><span>智能分析</span><em>AI</em></RouterLink>
          <div class="nav-section">管理后台</div>
          <RouterLink to="/admin/dashboard"><span>后台首页</span><em>Admin</em></RouterLink>
          <RouterLink to="/admin/data"><span>基础数据</span><em>Data</em></RouterLink>
          <RouterLink to="/admin/dispatch"><span>调度建议</span><em>Dispatch</em></RouterLink>
        </nav>
      </aside>
      <main class="main">
        <el-alert v-if="runtimeError" :title="runtimeError" type="error" show-icon :closable="false" />
        <RouterView v-else />
      </main>
    </div>
  </el-config-provider>
</template>

<script setup lang="ts">
import { onErrorCaptured, ref, watch } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const runtimeError = ref('')

onErrorCaptured((error) => {
  runtimeError.value = (error as Error).message || '页面运行异常，请刷新或返回其他页面。'
  return false
})

watch(
  () => route.fullPath,
  () => {
    runtimeError.value = ''
  }
)
</script>
