<template>
  <section>
    <el-card shadow="never">
      <template #header><div class="page-header"><span>题库</span><el-button @click="resetFilters">重置筛选</el-button></div></template>
      <el-form :inline="true" :model="filters" class="filters" @submit.prevent="search">
        <el-form-item label="关键词"><el-input v-model.trim="filters.keyword" clearable placeholder="题目标题或内容" @keyup.enter="search" /></el-form-item>
        <el-form-item label="难度"><el-select v-model="filters.difficulty" clearable placeholder="全部" style="width: 120px"><el-option label="简单" :value="1" /><el-option label="中等" :value="2" /><el-option label="困难" :value="3" /></el-select></el-form-item>
        <el-form-item label="题目类型"><el-select v-model="filters.questionType" clearable placeholder="全部" style="width: 130px"><el-option v-for="type in QUESTION_TYPE_OPTIONS" :key="type.value" :label="type.label" :value="type.value" /></el-select></el-form-item>
        <el-form-item label="标签"><el-select v-model="filters.tagId" clearable filterable placeholder="全部" style="width: 160px"><el-option v-for="tag in tags" :key="tag.id" :label="tag.name" :value="tag.id" /></el-select></el-form-item>
        <el-form-item><el-button type="primary" :loading="loading" @click="search">查询</el-button></el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="questions" stripe @row-click="viewDetail">
        <el-table-column prop="title" label="题目" min-width="280" show-overflow-tooltip />
        <el-table-column label="类型" width="120"><template #default="{ row }">{{ questionTypeLabel(row.questionType) }}</template></el-table-column>
        <el-table-column label="难度" width="100"><template #default="{ row }"><el-tag :type="difficultyType(row.difficulty)">{{ difficultyText(row.difficulty) }}</el-tag></template></el-table-column>
        <el-table-column label="标签" min-width="180">
          <template #default="{ row }">
            <div v-if="row.tags?.length" class="tag-list">
              <el-tag v-for="tag in row.tags" :key="tag.id" size="small" effect="plain">{{ tag.name }}</el-tag>
            </div>
            <span v-else class="empty-value">无标签</span>
          </template>
        </el-table-column>
        <el-table-column prop="source" label="来源" width="150" show-overflow-tooltip />
        <el-table-column label="统计" min-width="220">
          <template #default="{ row }">
            <span class="statistics">浏览 {{ row.viewCount || 0 }} · 提交 {{ row.submitCount || 0 }} · 正确 {{ row.correctCount || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="110" fixed="right"><template #default="{ row }"><el-button type="primary" link @click.stop="viewDetail(row)">查看详情</el-button></template></el-table-column>
      </el-table>
      <el-empty v-if="!loading && questions.length === 0" description="暂无题目" />
      <Pagination v-model:page="pageNum" v-model:size="pageSize" :total="total" @change="loadQuestions" />
    </el-card>

    <!-- 详情弹窗：无需离开题库即可查看内容、答案和解析 -->
    <el-dialog v-model="detailVisible" :title="detail?.title || '题目详情'" width="760px" destroy-on-close>
      <el-skeleton :loading="detailLoading" animated :rows="8">
        <template #default>
          <el-descriptions :column="3" border>
            <el-descriptions-item label="难度"><el-tag :type="difficultyType(detail?.difficulty)">{{ difficultyText(detail?.difficulty) }}</el-tag></el-descriptions-item>
            <el-descriptions-item label="类型">{{ questionTypeLabel(detail?.questionType) }}</el-descriptions-item>
            <el-descriptions-item label="来源">{{ detail?.source || '-' }}</el-descriptions-item>
            <el-descriptions-item label="标签" :span="3">
              <div v-if="detail?.tags?.length" class="tag-list">
                <el-tag v-for="tag in detail.tags" :key="tag.id" size="small" effect="plain">{{ tag.name }}</el-tag>
              </div>
              <span v-else class="empty-value">无标签</span>
            </el-descriptions-item>
            <el-descriptions-item label="浏览次数">{{ detail?.viewCount || 0 }}</el-descriptions-item>
            <el-descriptions-item label="提交次数">{{ detail?.submitCount || 0 }}</el-descriptions-item>
            <el-descriptions-item label="正确次数">{{ detail?.correctCount || 0 }}</el-descriptions-item>
          </el-descriptions>
          <h4>题目内容</h4><div class="detail-content">{{ detail?.content || '-' }}</div>
          <h4>参考答案</h4><div class="detail-content answer">{{ detail?.answer || '-' }}</div>
          <h4>答案解析</h4><div class="detail-content">{{ detail?.answerAnalysis || '-' }}</div>
        </template>
      </el-skeleton>
      <template #footer><el-button @click="detailVisible = false">关闭</el-button><el-button type="primary" @click="openDetailPage">在新页面查看</el-button></template>
    </el-dialog>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import axios from '../utils/axios.js'
import Pagination from '../components/Pagination.vue'
import { QUESTION_TYPE_OPTIONS, questionTypeLabel } from '../utils/questionType.js'

const router = useRouter()
const loading = ref(false)
const questions = ref([])
const tags = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const filters = reactive({ keyword: '', difficulty: undefined, questionType: undefined, tagId: undefined })
const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref(null)

async function loadQuestions() {
  loading.value = true
  try {
    const { data } = await axios.get('/api/questions', { params: { ...filters, pageNum: pageNum.value, pageSize: pageSize.value } })
    const page = data.data || {}
    questions.value = page.records || []
    total.value = Number(page.total || 0)
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '题目加载失败')
  } finally { loading.value = false }
}

async function loadTags() {
  try { tags.value = (await axios.get('/api/tags')).data.data || [] } catch { /* 标签加载失败不阻断题库查询 */ }
}
function search() { pageNum.value = 1; loadQuestions() }
function resetFilters() { Object.assign(filters, { keyword: '', difficulty: undefined, questionType: undefined, tagId: undefined }); search() }
async function viewDetail(row) {
  detailVisible.value = true
  detailLoading.value = true
  detail.value = null
  try { detail.value = (await axios.get(`/api/questions/${row.id}`)).data.data } catch (error) { ElMessage.error(error.response?.data?.message || '题目详情加载失败'); detailVisible.value = false } finally { detailLoading.value = false }
}
function openDetailPage() { if (detail.value?.id) { detailVisible.value = false; router.push(`/questions/${detail.value.id}`) } }
function difficultyText(value) { return ({ 1: '简单', 2: '中等', 3: '困难' })[value] || '未知' }
function difficultyType(value) { return ({ 1: 'success', 2: 'warning', 3: 'danger' })[value] || 'info' }
onMounted(() => { loadTags(); loadQuestions() })
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; font-size: 18px; font-weight: 600; }
.filters { margin-bottom: 4px; }
.detail-content { white-space: pre-wrap; line-height: 1.8; color: #303133; }
.answer { padding: 12px; border-radius: 4px; background: #f0f9eb; }
.tag-list { display: flex; flex-wrap: wrap; gap: 6px; }
.empty-value { color: #909399; }
.statistics { color: #606266; font-size: 13px; white-space: nowrap; }
h4 { margin: 20px 0 10px; }
</style>
