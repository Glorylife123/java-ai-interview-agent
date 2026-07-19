<template>
  <el-card v-loading="loading" shadow="never" class="hot-card">
    <template #header>
      <div class="hot-header">
        <span>🔥 热门题目</span>
        <el-button link @click="load">刷新</el-button>
      </div>
    </template>

    <p v-if="error" class="error-text">{{ error }}</p>
    <ul v-else-if="list.length" class="hot-list">
      <li v-for="(item, index) in list" :key="item.questionId" class="hot-item" @click="goQuestion(item.questionId)">
        <span class="hot-rank" :class="rankClass(index)">{{ index + 1 }}</span>
        <span class="hot-title">{{ item.title || `题目 #${item.questionId}` }}</span>
        <span class="hot-score">热度 {{ item.viewScore }}</span>
      </li>
    </ul>
    <el-empty v-else-if="!loading" description="暂无热门题目" :image-size="60" />
  </el-card>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import axios from '../utils/axios.js'

const props = defineProps({
  limit: { type: Number, default: 10 },
})

const router = useRouter()
const list = ref([])
const loading = ref(false)
const error = ref('')

/**
 * GET /api/questions/hot?limit=..
 * 后端走 Redis 缓存（question:hot:list，TTL 5min），miss 时从浏览量 ZSet 回源。
 */
async function load() {
  loading.value = true
  error.value = ''
  try {
    const { data } = await axios.get('/api/questions/hot', { params: { limit: props.limit } })
    list.value = data.data || []
  } catch (err) {
    error.value = err.response?.data?.message || err.message || '热门题目加载失败'
    list.value = []
  } finally {
    loading.value = false
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

onMounted(load)
</script>

<style scoped>
.hot-card { margin-bottom: 16px; }
.hot-header { display: flex; justify-content: space-between; align-items: center; font-size: 16px; font-weight: 600; }
.error-text { color: #e74c3c; font-size: 13px; margin: 0; }
.hot-list { list-style: none; margin: 0; padding: 0; }
.hot-item { display: flex; align-items: center; gap: 12px; padding: 9px 8px; border-radius: 4px; cursor: pointer; font-size: 14px; }
.hot-item:hover { background: #f5f7fa; }
.hot-rank { flex: 0 0 24px; width: 24px; height: 24px; line-height: 24px; text-align: center; border-radius: 50%; background: #ebeef5; color: #606266; font-size: 12px; font-weight: 600; }
.rank-1 { background: #f56c6c; color: #fff; }
.rank-2 { background: #e6a23c; color: #fff; }
.rank-3 { background: #409eff; color: #fff; }
.hot-title { flex: 1; color: #303133; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.hot-score { flex: 0 0 auto; color: #909399; font-size: 12px; }
</style>
