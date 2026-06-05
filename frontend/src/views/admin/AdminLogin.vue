<template>
  <section class="page login-page admin-login-page">
    <div class="login-hero admin-login-hero">
      <div class="login-copy admin-login-copy">
        <span class="command-kicker">Transit Admin Console</span>
        <h1>公交调度数据平台管理端</h1>
        <p>面向课程演示和运营管理场景，集中维护线路、站点、车辆、司机、班次与调度建议。</p>
        <div class="login-points admin-login-points">
          <strong>基础数据管理</strong>
          <strong>调度建议审核</strong>
          <strong>客流统计支撑</strong>
        </div>
        <div class="login-preview-card">
          <div>
            <span>访问方式</span>
            <strong>管理员登录</strong>
          </div>
          <div>
            <span>平台状态</span>
            <strong>本地联调就绪</strong>
          </div>
        </div>
      </div>

      <div class="panel login-card admin-login-card">
        <div class="panel-section-head">
          <div>
            <span>登录</span>
            <strong>管理员账号</strong>
          </div>
          <el-tag type="success">Admin</el-tag>
        </div>
        <el-form label-position="top" @submit.prevent>
          <el-form-item label="账号"><el-input v-model="username" /></el-form-item>
          <el-form-item label="密码"><el-input v-model="password" type="password" show-password /></el-form-item>
          <el-button type="primary" :loading="loading" @click="login">进入调度控制台</el-button>
        </el-form>
        <p class="login-tip">登录后可进入后台首页、基础数据管理和调度建议管理。</p>
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
const username = ref('')
const password = ref('')
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
