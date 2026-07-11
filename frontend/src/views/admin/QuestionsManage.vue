<template>
  <section>
    <el-card shadow="never">
      <template #header><div class="card-header"><span>题库管理</span><el-button type="primary" :icon="Plus" @click="openCreate">新增题目</el-button></div></template>
      <el-form :inline="true" :model="filters" class="filters" @submit.prevent="search">
        <el-form-item label="关键词"><el-input v-model.trim="filters.keyword" clearable placeholder="标题或内容" @keyup.enter="search" /></el-form-item>
        <el-form-item label="难度"><el-select v-model="filters.difficulty" clearable placeholder="全部" style="width: 120px"><el-option label="简单" :value="1" /><el-option label="中等" :value="2" /><el-option label="困难" :value="3" /></el-select></el-form-item>
        <el-form-item label="题目类型"><el-select v-model="filters.questionType" clearable placeholder="全部" style="width: 130px"><el-option v-for="type in QUESTION_TYPE_OPTIONS" :key="type.value" :label="type.label" :value="type.value" /></el-select></el-form-item>
        <el-form-item label="标签"><el-select v-model="filters.tagId" clearable placeholder="全部" style="width: 140px"><el-option v-for="tag in tags" :key="tag.id" :label="tag.name" :value="tag.id" /></el-select></el-form-item>
        <el-form-item><el-button type="primary" :loading="loading" @click="search">查询</el-button><el-button @click="resetFilters">重置</el-button></el-form-item>
      </el-form>
      <el-table v-loading="loading" :data="questions" stripe>
        <el-table-column type="index" label="序号" width="70" :index="indexMethod" />
        <el-table-column prop="title" label="标题" min-width="240" show-overflow-tooltip />
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
        <el-table-column prop="source" label="来源" min-width="140" show-overflow-tooltip />
        <el-table-column label="状态" width="100"><template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '上架' : '下架' }}</el-tag></template></el-table-column>
        <el-table-column label="操作" width="150" fixed="right"><template #default="{ row }"><el-button type="primary" link @click="openEdit(row)">编辑</el-button><el-button type="danger" link @click="remove(row)">删除</el-button></template></el-table-column>
      </el-table>
      <el-empty v-if="!loading && questions.length === 0" description="暂无题目" />
      <Pagination v-model:page="pageNum" v-model:size="pageSize" :total="total" @change="loadQuestions" />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑题目' : '新增题目'" width="880px" destroy-on-close @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-row :gutter="16">
          <el-col :span="16"><el-form-item label="标题" prop="title"><el-input v-model.trim="form.title" maxlength="255" show-word-limit /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="题目类型" prop="questionType"><el-select v-model="form.questionType" placeholder="请选择题目类型" style="width: 100%"><el-option v-for="type in QUESTION_TYPE_OPTIONS" :key="type.value" :label="type.label" :value="type.value" /></el-select></el-form-item></el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8"><el-form-item label="难度" prop="difficulty"><el-select v-model="form.difficulty" style="width: 100%"><el-option label="简单" :value="1" /><el-option label="中等" :value="2" /><el-option label="困难" :value="3" /></el-select></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="来源"><el-input v-model.trim="form.source" placeholder="如：企业面试题" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="状态"><el-select v-model="form.status" style="width: 100%"><el-option label="上架" :value="1" /><el-option label="下架" :value="0" /></el-select></el-form-item></el-col>
        </el-row>
        <el-form-item label="关联标签"><el-select v-model="form.tagIds" multiple filterable clearable placeholder="选择标签；保存时将完整替换当前题目标签" style="width: 100%"><el-option v-for="tag in tags" :key="tag.id" :label="tag.name" :value="tag.id" /></el-select></el-form-item>
        <el-form-item label="题目内容" prop="content"><el-input v-model="form.content" type="textarea" :rows="5" maxlength="10000" show-word-limit /></el-form-item>
        <el-form-item label="参考答案" prop="answer"><el-input v-model="form.answer" type="textarea" :rows="4" maxlength="10000" show-word-limit /></el-form-item>
        <el-form-item label="答案解析"><el-input v-model="form.answerAnalysis" type="textarea" :rows="4" maxlength="10000" show-word-limit /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="submit">保存</el-button></template>
    </el-dialog>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import axios from '../../utils/axios.js'
import Pagination from '../../components/Pagination.vue'
import { DEFAULT_QUESTION_TYPE, normalizeQuestionType, QUESTION_TYPE_OPTIONS, questionTypeLabel } from '../../utils/questionType.js'

const loading = ref(false); const saving = ref(false); const questions = ref([]); const tags = ref([]); const total = ref(0); const pageNum = ref(1); const pageSize = ref(10)
const filters = reactive({ keyword: '', difficulty: undefined, questionType: undefined, tagId: undefined })
const dialogVisible = ref(false); const editingId = ref(null); const formRef = ref()
const newForm = () => ({ title: '', content: '', questionType: DEFAULT_QUESTION_TYPE, difficulty: 1, answer: '', answerAnalysis: '', source: '', status: 1, tagIds: [] })
const form = reactive(newForm())
const rules = { title: [{ required: true, message: '请输入题目标题', trigger: 'blur' }], content: [{ required: true, message: '请输入题目内容', trigger: 'blur' }], questionType: [{ required: true, message: '请选择题目类型', trigger: 'change' }], difficulty: [{ required: true, message: '请选择难度', trigger: 'change' }], answer: [{ required: true, message: '请输入参考答案', trigger: 'blur' }] }
async function loadQuestions() {
  loading.value = true
  try { const page = (await axios.get('/api/questions', { params: { ...filters, pageNum: pageNum.value, pageSize: pageSize.value } })).data.data || {}; questions.value = page.records || []; total.value = Number(page.total || 0) } catch (error) { ElMessage.error(error.response?.data?.message || '题目加载失败') } finally { loading.value = false }
}
async function loadTags() { try { tags.value = (await axios.get('/api/tags')).data.data || [] } catch { ElMessage.warning('标签加载失败，保存题目时将无法绑定标签') } }
function search() { pageNum.value = 1; loadQuestions() }
function resetFilters() { Object.assign(filters, { keyword: '', difficulty: undefined, questionType: undefined, tagId: undefined }); search() }
function resetForm() { editingId.value = null; Object.assign(form, newForm()); formRef.value?.clearValidate() }
function openCreate() { resetForm(); dialogVisible.value = true }
async function openEdit(row) {
  try {
    // 题目详情已直接携带 tags，无需额外请求 /api/questions/{id}/tags。
    const detail = (await axios.get(`/api/questions/${row.id}`)).data.data || row
    editingId.value = row.id
    Object.assign(form, {
      ...newForm(),
      ...detail,
      questionType: normalizeQuestionType(detail.questionType),
      tagIds: (detail.tags || []).map((tag) => tag.id),
    })
    dialogVisible.value = true
  } catch (error) { ElMessage.error(error.response?.data?.message || '题目详情加载失败') }
}
async function submit() {
  const valid = await formRef.value?.validate().catch(() => false); if (!valid) return
  saving.value = true
  try {
    // 后端新增/编辑请求已支持 tagIds：一次事务内整体替换，可正确移除被取消的标签。
    const { tags: _responseTags, ...editableFields } = form
    const payload = { ...editableFields, tagIds: [...form.tagIds] }
    if (editingId.value) await axios.put(`/api/questions/${editingId.value}`, payload)
    else await axios.post('/api/questions', payload)
    ElMessage.success('保存成功'); dialogVisible.value = false; loadQuestions()
  } catch (error) { ElMessage.error(error.response?.data?.message || '保存失败') } finally { saving.value = false }
}
async function remove(row) {
  try { await ElMessageBox.confirm(`确认删除题目「${row.title}」？`, '删除确认', { type: 'warning', confirmButtonText: '删除' }) } catch { return }
  try { await axios.delete(`/api/questions/${row.id}`); ElMessage.success('删除成功'); loadQuestions() } catch (error) { ElMessage.error(error.response?.data?.message || '删除失败') }
}
// 表格序号按分页位置连续显示（1、2、3…），与数据库主键 id 解耦，
// 因此删除题目导致 id 出现空洞时，前端序号仍然连贯。
function indexMethod(rowIndex) { return (pageNum.value - 1) * pageSize.value + rowIndex + 1 }
function difficultyText(value) { return ({ 1: '简单', 2: '中等', 3: '困难' })[value] || '未知' }
function difficultyType(value) { return ({ 1: 'success', 2: 'warning', 3: 'danger' })[value] || 'info' }
onMounted(() => { loadTags(); loadQuestions() })
</script>

<style scoped>
.card-header { display: flex; align-items: center; justify-content: space-between; font-size: 18px; font-weight: 600; }
.filters { margin-bottom: 4px; }
.tag-list { display: flex; flex-wrap: wrap; gap: 6px; }
.empty-value { color: #909399; }
</style>
