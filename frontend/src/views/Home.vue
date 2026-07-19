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

      <!-- 热门题目：后端基于浏览量 ZSet + 5min 缓存，缓存 miss 自动回源 -->
      <section class="action-card">
        <div class="card-title">🔥 热门题目</div>
        <p v-if="hotError" class="error-text">{{ hotError }}</p>
        <ul v-else-if="hotQuestions.length" class="hot-list">
          <li v-for="(item, index) in hotQuestions" :key="item.questionId" class="hot-item" @click="goQuestion(item.questionId)">
            <span class="hot-rank" :class="rankClass(index)">{{ index + 1 }}</span>
            <span class="hot-title">{{ item.title || `题目 #${item.questionId}` }}</span>
            <span class="hot-score">热度 {{ item.viewScore }}</span>
          </li>
        </ul>
        <p v-else-if="hotLoading" class="muted">加载中…</p>
        <p v-else class="muted">暂无热门题目</p>
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

// 热门题目列表
const hotQuestions = ref([])
const hotLoading = ref(false)
const hotError = ref('')

/**
 * 加载热门题目：GET /api/questions/hot?limit=10。
 * 后端走 Redis 缓存（question:hot:list，TTL 5min），miss 时从浏览量 ZSet 回源。
 */
async function loadHotQuestions() {
  hotLoading.value = true
  hotError.value = ''
  try {
    const { data } = await axios.get('/api/questions/hot', { params: { limit: 10 } })
    hotQuestions.value = data.data || []
  } catch (err) {
    hotError.value = err.response?.data?.message || err.message || '热门题目加载失败'
    hotQuestions.value = []
  } finally {
    hotLoading.value = false
  }
}

function goQuestion(id) {
  if (id) router.push(`/questions/${id}`)
}

function rankClass(index) {
  if (index === 0) return 'rank-1'
  if (index === 1) return 'rank-2'
  if (index === 2) return 'rank-3'
  return ''
}

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

// 进入首页即加载一次热门题目（后端 AT 受保护接口）
onMounted(loadHotQuestions)
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
.card-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 12px;
  color: #333;
}
.muted {
  color: #909399;
  font-size: 13px;
  margin: 0;
}
.hot-list {
  list-style: none;
  margin: 0;
  padding: 0;
}
.hot-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 8px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}
.hot-item:hover {
  background: #f5f7fa;
}
.hot-rank {
  flex: 0 0 24px;
  width: 24px;
  height: 24px;
  line-height: 24px;
  text-align: center;
  border-radius: 50%;
  background: #ebeef5;
  color: #606266;
  font-size: 12px;
  font-weight: 600;
}
.rank-1 { background: #f56c6c; color: #fff; }
.rank-2 { background: #e6a23c; color: #fff; }
.rank-3 { background: #409eff; color: #fff; }
.hot-title {
  flex: 1;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.hot-score {
  flex: 0 0 auto;
  color: #909399;
  font-size: 12px;
}
</style>