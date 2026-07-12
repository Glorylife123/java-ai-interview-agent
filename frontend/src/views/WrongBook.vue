<template>
  <section>
    <el-card shadow="never">
      <template #header>
        <div class="page-header">
          <span>错题本</span>
          <div class="header-actions">
            <el-select v-model="filter.mastered" placeholder="全部" clearable style="width: 120px" @change="search">
              <el-option label="未掌握" :value="0" />
              <el-option label="已掌握" :value="1" />
            </el-select>
            <el-button :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
            <el-button type="primary" :icon="MagicStick" :loading="randoming" @click="randomPractice">随机错题再练</el-button>
          </div>
        </div>
      </template>

      <el-table v-loading="loading" :data="records" stripe>
        <el-table-column label="题目" min-width="260" show-overflow-tooltip>
          <template #default="{ row }">
            <el-button type="primary" link @click="goQuestion(row)">{{ row.questionTitle || `题目 #${row.questionId}` }}</el-button>
          </template>
        </el-table-column>
        <el-table-column label="分类" width="120" align="center"><template #default="{ row }">{{ row.questionCategory || '-' }}</template></el-table-column>
        <el-table-column label="类型" width="100" align="center"><template #default="{ row }">{{ questionTypeLabel(row.questionType) }}</template></el-table-column>
        <el-table-column label="难度" width="90" align="center">
          <template #default="{ row }"><el-tag :type="difficultyType(row.questionDifficulty)" size="small">{{ difficultyText(row.questionDifficulty) }}</el-tag></template>
        </el-table-column>
        <el-table-column label="错次" width="80" align="center">
          <template #default="{ row }"><el-tag type="danger" effect="plain" size="small">{{ row.wrongCount }}</el-tag></template>
        </el-table-column>
        <el-table-column label="最近答错" width="170"><template #default="{ row }">{{ formatTime(row.lastWrongAt) }}</template></el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.mastered === 1" type="success" size="small">已掌握</el-tag>
            <el-tag v-else type="warning" size="small">未掌握</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.mastered !== 1" type="success" link size="small" @click="markMastered(row, true)">标记掌握</el-button>
            <el-button v-else type="warning" link size="small" @click="markMastered(row, false)">取消掌握</el-button>
            <el-button type="danger" link size="small" @click="remove(row)">移除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && records.length === 0" description="错题本为空" />
      <Pagination v-model:page="pageNum" v-model:size="pageSize" :total="total" @change="load" />
    </el-card>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, MagicStick } from '@element-plus/icons-vue'
import axios from '../utils/axios.js'
import Pagination from '../components/Pagination.vue'
import { questionTypeLabel } from '../utils/questionType.js'

const router = useRouter()
const loading = ref(false)
const randoming = ref(false)
const records = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const filter = reactive({ mastered: undefined })

async function load() {
  loading.value = true
  try {
    const params = { pageNum: pageNum.value, pageSize: pageSize.value }
    if (filter.mastered === 0 || filter.mastered === 1) params.mastered = filter.mastered
    const { data } = await axios.get('/api/wrong/page', { params })
    const page = data.data || {}
    records.value = page.records || []
    total.value = Number(page.total || 0)
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '错题本加载失败')
  } finally {
    loading.value = false
  }
}

function search() {
  pageNum.value = 1
  load()
}

async function markMastered(row, mastered) {
  try {
    await axios.put(`/api/wrong/${row.questionId}/mastered`, { mastered })
    ElMessage.success(mastered ? '已标记为掌握' : '已取消掌握')
    row.mastered = mastered ? 1 : 0
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '操作失败')
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm('确定将这道题移出错题本吗？移除后再次答错会重新加入。', '移除错题', { type: 'warning' })
  } catch {
    return // 用户取消
  }
  try {
    await axios.delete(`/api/wrong/${row.questionId}`)
    ElMessage.success('已移出错题本')
    const remained = total.value - 1
    total.value = remained
    // 当前页删空且不在第一页时退一页，否则原地重载
    if (records.value.length === 1 && pageNum.value > 1) {
      pageNum.value -= 1
    }
    load()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '移除失败')
  }
}

async function randomPractice() {
  randoming.value = true
  try {
    const { data } = await axios.get('/api/wrong/random')
    const q = data.data
    if (!q) {
      ElMessage.info('暂无错题，去刷点新题吧～')
      return
    }
    router.push(`/questions/${q.id}`)
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '随机错题获取失败')
  } finally {
    randoming.value = false
  }
}

function goQuestion(row) {
  if (row.questionId) router.push(`/questions/${row.questionId}`)
}

function formatTime(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 19)
}
function difficultyText(value) { return ({ 1: '简单', 2: '中等', 3: '困难' })[Number(value)] || '未知' }
function difficultyType(value) { return ({ 1: 'success', 2: 'warning', 3: 'danger' })[Number(value)] || 'info' }

onMounted(load)
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; font-size: 18px; font-weight: 600; }
.header-actions { display: flex; align-items: center; gap: 10px; }
</style>
