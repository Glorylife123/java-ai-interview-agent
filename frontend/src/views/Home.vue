<template>
  <div class="home-page">
    <header class="home-header">
      <h1>欢迎登录 Interview Agent</h1>
      <button class="btn-logout" @click="handleLogout">退出登录</button>
    </header>

    <main class="home-main">
      <section class="info-card">
        <p>当前 Access Token（前 50 位）：</p>
        <code class="token-preview">{{ maskedToken }}</code>
      </section>

      <section class="action-card">
        <button class="btn-primary" @click="callProtectedApi" :disabled="calling">
          {{ calling ? '请求中...' : '调用受保护接口 (/api/questions)' }}
        </button>

        <!-- 接口调用结果展示 -->
        <div v-if="apiResult !== null" class="result-box">
          <p><strong>响应数据：</strong></p>
          <pre>{{ apiResult }}</pre>
        </div>
        <p v-if="apiError" class="error-text">{{ apiError }}</p>
      </section>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth.js'
import axios from '../utils/axios.js'

const router = useRouter()
const authStore = useAuthStore()

const calling = ref(false)
const apiResult = ref(null)
const apiError = ref('')

// 显示 Token 前几位 + 掩码，便于调试又不暴露完整 Token
const maskedToken = computed(() => {
  const t = authStore.accessToken
  if (!t) return '(无)'
  return t.length > 50 ? t.slice(0, 50) + '…' : t
})

/**
 * 调用一个有代表性的受保护接口，验证 AT 是否正常工作。
 * 使用已有的 /api/questions 分页查询接口（后端需 AT 才能访问）。
 */
async function callProtectedApi() {
  calling.value = true
  apiResult.value = null
  apiError.value = ''

  try {
    const response = await axios.get('/api/questions', {
      params: { pageNum: 1, pageSize: 3 },
    })
    // 格式化展示返回数据
    apiResult.value = JSON.stringify(response.data.data || response.data, null, 2)
  } catch (err) {
    apiError.value =
      err.response?.data?.message ||
      err.message ||
      '请求失败'
  } finally {
    calling.value = false
  }
}

/**
 * 退出登录：
 * 1. 调用后端失去效当前设备 RT
 * 2. 清空本地 Token 状态
 * 3. 跳转登录页
 */
async function handleLogout() {
  try {
    await axios.post('/api/auth/logout')
  } catch {
    // 后端注销失败（如 RT 已过期），仍执行本地清空
  }
  authStore.clearToken()
  router.push('/login')
}
</script>

<style scoped>
.home-page {
  max-width: 720px;
  margin: 0 auto;
  padding: 24px;
}
.home-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}
.home-header h1 {
  margin: 0;
  font-size: 22px;
  color: #333;
}
.btn-logout {
  padding: 8px 16px;
  background: #ff4d4f;
  color: #fff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}
.btn-logout:hover {
  background: #ff7875;
}
.info-card,
.action-card {
  background: #fff;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 1px 6px rgba(0, 0, 0, 0.06);
  margin-bottom: 16px;
}
.token-preview {
  display: inline-block;
  font-size: 12px;
  background: #f6f8fa;
  padding: 6px 10px;
  border-radius: 4px;
  word-break: break-all;
}
.btn-primary {
  padding: 10px 20px;
  background: #409eff;
  color: #fff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}
.btn-primary:hover {
  background: #66b1ff;
}
.btn-primary:disabled {
  background: #a0cfff;
  cursor: not-allowed;
}
.result-box {
  margin-top: 16px;
  background: #f6f8fa;
  padding: 12px;
  border-radius: 4px;
}
.result-box pre {
  margin: 0;
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-all;
}
.error-text {
  color: #e74c3c;
  font-size: 13px;
}
</style>