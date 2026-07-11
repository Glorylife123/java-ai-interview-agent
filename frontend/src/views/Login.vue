<template>
  <main class="auth-page">
    <el-card class="auth-card" shadow="always">
      <template #header>
        <div class="card-title">
          <div class="brand-badge"><el-icon :size="24"><Cpu /></el-icon></div>
          <h2>AI 面试辅助 Agent</h2>
          <p class="subtitle">登录以继续你的面试练习</p>
        </div>
      </template>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="handleLogin">
        <el-form-item label="用户名" prop="username">
          <el-input v-model.trim="form.username" placeholder="请输入用户名" autocomplete="username" @keyup.enter="handleLogin" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" autocomplete="current-password" @keyup.enter="handleLogin" />
        </el-form-item>
        <el-button type="primary" class="submit-button" :loading="loading" @click="handleLogin">登录</el-button>
      </el-form>

      <p class="switch-text">还没有账号？<router-link to="/register">立即注册</router-link></p>
    </el-card>
  </main>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../stores/auth.js'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const formRef = ref()
const loading = ref(false)
const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

/**
 * 登录后由后端返回的 user.role 决定入口：ADMIN 去仪表盘，其他用户去题库。
 * redirect 仅在普通用户入口使用，管理员始终进入后台，避免被带到无权限路径。
 */
async function handleLogin() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const data = await authStore.login(form.username, form.password)
    ElMessage.success('登录成功')
    if (data.user?.role === 'ADMIN') {
      router.replace('/admin/dashboard')
    } else {
      router.replace(typeof route.query.redirect === 'string' ? route.query.redirect : '/questions')
    }
  } catch (error) {
    ElMessage.error(error.response?.data?.message || error.message || '登录失败，请检查用户名和密码')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page {
  display: flex;
  min-height: 100vh;
  align-items: center;
  justify-content: center;
  padding: 24px;
  /* 极淡的青绿光晕，落在 Apple 浅灰底上，安静不喧宾夺主 */
  background:
    radial-gradient(60% 55% at 22% 18%, rgba(20, 184, 166, 0.16) 0%, rgba(20, 184, 166, 0) 60%),
    radial-gradient(50% 50% at 85% 90%, rgba(13, 148, 136, 0.12) 0%, rgba(13, 148, 136, 0) 55%),
    #f5f5f7;
}
.auth-card { width: 400px; border-radius: 22px; box-shadow: var(--shadow-2); }
.auth-card :deep(.el-card__header) { padding: 30px 28px 8px; border-bottom: none; }
.auth-card :deep(.el-card__body) { padding: 8px 28px 28px; }
.card-title { display: flex; flex-direction: column; align-items: center; text-align: center; }
.card-title .brand-badge {
  display: flex; align-items: center; justify-content: center;
  width: 52px; height: 52px; border-radius: 15px; color: #fff;
  background: var(--brand-gradient); box-shadow: var(--shadow-accent); margin-bottom: 16px;
}
.card-title h2 { margin: 0; font-size: 22px; font-weight: 700; color: var(--ink-900); }
.card-title .subtitle { margin: 8px 0 0; font-size: 14px; font-weight: 400; color: var(--ink-400); }
.submit-button { width: 100%; margin-top: 8px; height: 44px; font-size: 15px; }
.switch-text { margin: 22px 0 0; text-align: center; color: var(--ink-400); font-size: 14px; }
.switch-text a { color: var(--brand-600); text-decoration: none; font-weight: 500; }
.switch-text a:hover { text-decoration: underline; }
</style>
