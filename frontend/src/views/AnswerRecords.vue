<template>
  <section>
    <el-card shadow="never">
      <template #header><div class="page-header"><span>答题记录</span><el-button :loading="loading" @click="load">刷新</el-button></div></template>

      <el-table v-loading="loading" :data="records" stripe>
        <el-table-column prop="questionTitle" label="题目" min-width="240" show-overflow-tooltip>
          <template #default="{ row }">
            <el-button type="primary" link @click="goQuestion(row)">{{ row.questionTitle || `题目 #${row.questionId}` }}</el-button>
          </template>
        </el-table-column>
        <el-table-column prop="userAnswer" label="我的作答" min-width="260" show-overflow-tooltip />
        <el-table-column label="评估状态" width="130" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.isCorrect === null || row.isCorrect === undefined" type="info" effect="plain">待AI评估</el-tag>
            <el-tag v-else-if="row.isCorrect === 1" type="success">正确</el-tag>
            <el-tag v-else type="danger">错误</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="得分" width="90" align="center"><template #default="{ row }">{{ row.score ?? '-' }}</template></el-table-column>
        <el-table-column label="耗时" width="100" align="center"><template #default="{ row }">{{ formatCost(row.timeCostSeconds) }}</template></el-table-column>
        <el-table-column label="提交时间" width="180"><template #default="{ row }">{{ formatTime(row.createdAt) }}</template></el-table-column>
      </el-table>
      <el-empty v-if="!loading && records.length === 0" description="暂无答题记录" />
      <Pagination v-model:page="pageNum" v-model:size="pageSize" :total="total" @change="load" />
    </el-card>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import axios from '../utils/axios.js'
import Pagination from '../components/Pagination.vue'

const router = useRouter()
const loading = ref(false)
const records = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

async function load() {
  loading.value = true
  try {
    const { data } = await axios.get('/api/answer/records', { params: { pageNum: pageNum.value, pageSize: pageSize.value } })
    const page = data.data || {}
    records.value = page.records || []
    total.value = Number(page.total || 0)
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '答题记录加载失败')
  } finally {
    loading.value = false
  }
}

function goQuestion(row) {
  if (row.questionId) router.push(`/questions/${row.questionId}`)
}
function formatCost(seconds) {
  if (seconds === null || seconds === undefined) return '-'
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  return m > 0 ? `${m}分${s}秒` : `${s}秒`
}
function formatTime(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 19)
}

onMounted(load)
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; font-size: 18px; font-weight: 600; }
</style>
