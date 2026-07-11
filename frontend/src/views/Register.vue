<template>
  <main class="auth-page">
    <el-card class="auth-card" shadow="always">
      <template #header>
        <div class="card-title">
          <div class="brand-badge"><el-icon :size="24"><Cpu /></el-icon></div>
          <h2>注册账号</h2>
          <p class="subtitle">创建账号，开始你的面试练习</p>
        </div>
      </template>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="用户名" prop="username"><el-input v-model.trim="form.username" autocomplete="username" placeholder="请输入用户名" /></el-form-item>
        <el-form-item label="昵称（可选）" prop="nickname"><el-input v-model.trim="form.nickname" placeholder="不填写时默认使用用户名" /></el-form-item>
        <el-form-item label="密码" prop="password"><el-input v-model="form.password" type="password" show-password autocomplete="new-password" placeholder="至少 6 位" /></el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword"><el-input v-model="form.confirmPassword" type="password" show-password autocomplete="new-password" placeholder="请再次输入密码" @keyup.enter="handleRegister" /></el-form-item>
        <el-button type="primary" class="submit-button" :loading="loading" @click="handleRegister">注册</el-button>
      </el-form>
      <p class="switch-text">已有账号？<router-link to="/login">返回登录</router-link></p>
    </el-card>
  </main>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import axios from '../utils/axios.js'

const router = useRouter()
const formRef = ref()
const loading = ref(false)
const form = reactive({ username: '', nickname: '', password: '', confirmPassword: '' })
const validateConfirmPassword = (_rule, value, callback) => {
  if (!value) callback(new Error('请再次输入密码'))
  else if (value !== form.password) callback(new Error('两次输入的密码不一致'))
  else callback()
}
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }, { min: 6, message: '密码不能少于 6 位', trigger: 'blur' }],
  confirmPassword: [{ validator: validateConfirmPassword, trigger: 'blur' }],
}

async function handleRegister() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    // 后端会忽略普通注册者提交的 role，并固定创建 USER，前端不提交 role。
    await axios.post('/api/auth/register', { username: form.username, password: form.password, nickname: form.nickname || undefined })
    ElMessage.success('注册成功，请登录')
    router.replace('/login')
  } catch (error) {
    ElMessage.error(error.response?.data?.message || error.message || '注册失败')
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
