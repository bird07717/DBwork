<template>
  <section class="page login-page">
    <div class="login-hero">
      <div class="login-copy">
        <span>公交 Agent 管理端</span>
        <h1>调度数据维护与建议生成</h1>
        <p>登录后可维护线路、站点、车辆、司机、班次，并生成调度建议。</p>
        <div class="login-points">
          <strong>基础数据管理</strong>
          <strong>调度建议审核</strong>
          <strong>客流统计支撑</strong>
        </div>
      </div>

      <div class="panel login-card">
        <div class="panel-section-head">
          <div>
            <span>登录</span>
            <strong>管理员账号</strong>
          </div>
        </div>
        <el-form label-position="top" @submit.prevent>
          <el-form-item label="账号"><el-input v-model="username" /></el-form-item>
          <el-form-item label="密码"><el-input v-model="password" type="password" show-password /></el-form-item>
          <el-button type="primary" :loading="loading" @click="login">登录</el-button>
        </el-form>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { post } from '../../services/api'
import { setToken } from '../../services/auth'

const router = useRouter()
const username = ref('wjhadmin')
const password = ref('123456')
const loading = ref(false)

async function login() {
  try {
    loading.value = true
    const data = await post<{ tokenValue: string }>('/api/admin/auth/login', { username: username.value, password: password.value })
    setToken(data.tokenValue)
    ElMessage.success('登录成功')
    router.push('/admin/dashboard')
  } catch (error) {
    ElMessage.error((error as Error).message)
  } finally {
    loading.value = false
  }
}
</script>
