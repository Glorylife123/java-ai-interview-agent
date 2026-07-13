<template>
  <section>
    <el-card shadow="never">
      <template #header>
        <div class="page-header">
          <span>模拟面试</span>
          <div class="header-actions">
            <el-button :loading="loading" @click="load">刷新</el-button>
            <el-button type="primary" :icon="Plus" @click="openCreate">发起面试</el-button>
          </div>
        </div>
      </template>

      <el-table v-loading="loading" :data="sessions" stripe>
        <el-table-column prop="title" label="面试" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">{{ row.title || `${row.position}模拟面试` }}</template>
        </el-table-column>
        <el-table-column prop="position" label="岗位" width="160" show-overflow-tooltip />
        <el-table-column label="难度" width="100" align="center">
          <template #default="{ row }"><el-tag :type="difficultyType(row.difficulty)" size="small">{{ row.difficulty }}</el-tag></template>
        </el-table-column>
        <el-table-column label="进度" width="110" align="center">
          <template #default="{ row }">{{ row.currentQuestionIndex || 0 }} / {{ row.totalQuestionCount || 0 }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }"><el-tag :type="statusType(row.status)" effect="plain">{{ statusText(row.status) }}</el-tag></template>
        </el-table-column>
        <el-table-column label="开始时间" width="180"><template #default="{ row }">{{ formatTime(row.startedAt) }}</template></el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'CREATED'" type="primary" link @click="goSession(row)">开始</el-button>
            <el-button v-else-if="row.status === 'IN_PROGRESS'" type="primary" link @click="goSession(row)">继续</el-button>
            <el-button v-if="row.status === 'IN_PROGRESS' || row.status === 'FINISHED'" type="success" link @click="goReport(row)">答题情况</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && sessions.length === 0" description="还没有面试记录，点击「发起面试」开始吧" />
    </el-card>

    <!-- 发起面试 -->
    <el-dialog v-model="createVisible" title="发起模拟面试" width="480px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="岗位" prop="position">
          <el-input v-model.trim="form.position" maxlength="50" show-word-limit placeholder="如：Java后端实习生" />
        </el-form-item>
        <el-form-item label="难度" prop="difficulty">
          <el-select v-model="form.difficulty" placeholder="请选择难度" style="width: 100%">
            <el-option label="简单" value="简单" />
            <el-option label="中等" value="中等" />
            <el-option label="困难" value="困难" />
          </el-select>
        </el-form-item>
        <el-form-item label="题目数量" prop="totalQuestionCount">
          <el-input-number v-model="form.totalQuestionCount" :min="1" :max="20" />
          <span class="hint">按难度从题库随机抽题，需保证题库有足够题目</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="submitCreate">开始面试</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import axios from '../utils/axios.js'

const router = useRouter()
const loading = ref(false)
const sessions = ref([])

const createVisible = ref(false)
const creating = ref(false)
const formRef = ref()
const form = reactive({ position: '', difficulty: '中等', totalQuestionCount: 5 })
const rules = {
  position: [{ required: true, message: '请输入岗位', trigger: 'blur' }],
  difficulty: [{ required: true, message: '请选择难度', trigger: 'change' }],
  totalQuestionCount: [{ required: true, message: '请设置题目数量', trigger: 'change' }],
}

async function load() {
  loading.value = true
  try {
    sessions.value = (await axios.get('/api/interview/sessions')).data.data || []
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '面试记录加载失败')
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.assign(form, { position: '', difficulty: '中等', totalQuestionCount: 5 })
  formRef.value?.clearValidate()
  createVisible.value = true
}

async function submitCreate() {
  if (!(await formRef.value?.validate().catch(() => false))) return
  creating.value = true
  try {
    const session = (await axios.post('/api/interview/session', { ...form })).data.data
    ElMessage.success('面试已创建，即将开始')
    createVisible.value = false
    router.push(`/interview/${session.id}`)
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '创建失败')
  } finally {
    creating.value = false
  }
}

function goSession(row) { router.push(`/interview/${row.id}`) }
function goReport(row) { router.push(`/interview/${row.id}/report`) }

function statusText(status) {
  return ({ CREATED: '待开始', IN_PROGRESS: '进行中', FINISHED: '已完成', CANCELLED: '已取消' })[status] || status
}
function statusType(status) {
  return ({ CREATED: 'info', IN_PROGRESS: 'warning', FINISHED: 'success', CANCELLED: 'info' })[status] || 'info'
}
function difficultyType(value) {
  return ({ 简单: 'success', 中等: 'warning', 困难: 'danger' })[value] || 'info'
}
function formatTime(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 19)
}

onMounted(load)
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; font-size: 18px; font-weight: 600; }
.header-actions { display: flex; gap: 8px; }
.hint { margin-left: 12px; color: #909399; font-size: 12px; }
</style>
