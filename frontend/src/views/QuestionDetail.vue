<template>
  <section>
    <el-page-header content="题目详情" @back="router.back()" />
    <el-card v-loading="loading" class="detail-card">
      <template #header><div class="title-row"><span>{{ question?.title || '题目详情' }}</span><el-tag :type="difficultyType(question?.difficulty)">{{ difficultyText(question?.difficulty) }}</el-tag></div></template>
      <el-descriptions v-if="question" :column="3" border>
        <el-descriptions-item label="类型">{{ questionTypeLabel(question.questionType) }}</el-descriptions-item>
        <el-descriptions-item label="来源">{{ question.source || '-' }}</el-descriptions-item>
        <el-descriptions-item label="浏览次数">{{ question.viewCount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="提交次数">{{ question.submitCount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="正确次数">{{ question.correctCount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="标签" :span="3">
          <div v-if="question.tags?.length" class="tag-list">
            <el-tag v-for="tag in question.tags" :key="tag.id" size="small" effect="plain">{{ tag.name }}</el-tag>
          </div>
          <span v-else class="empty-value">无标签</span>
        </el-descriptions-item>
      </el-descriptions>
      <template v-if="question">
        <h3>题目内容</h3><article>{{ question.content || '-' }}</article>

        <!-- 我的作答：写下答案后提交，提交成功再展示参考答案与解析 -->
        <h3>我的作答</h3>
        <el-input
          v-model="userAnswer"
          type="textarea"
          :rows="6"
          :disabled="submitted"
          maxlength="5000"
          show-word-limit
          placeholder="在此输入你的作答……"
        />
        <div class="submit-bar">
          <span class="timer">已用时 {{ elapsedText }}</span>
          <div class="submit-actions">
            <el-button v-if="!submitted && !revealed" link @click="revealed = true">不提交，直接查看答案</el-button>
            <el-button type="primary" :loading="submitting" :disabled="submitted" @click="submit">
              {{ submitted ? '已提交' : '提交答案' }}
            </el-button>
          </div>
        </div>
        <el-alert
          v-if="submitted"
          class="submit-tip"
          type="success"
          :closable="false"
          show-icon
          title="已保存你的作答"
          :description="`提交时间：${formatTime(result?.submitTime)}。本次暂不进行 AI 判分，正误与得分为「待AI评估」。`"
        />

        <!-- 参考答案与解析：提交后或主动点击查看后展示 -->
        <template v-if="revealed || submitted">
          <h3>参考答案</h3><article class="answer">{{ question.answer || '-' }}</article>
          <h3>答案解析</h3><article>{{ question.answerAnalysis || '-' }}</article>
        </template>
        <el-empty v-else class="answer-hidden" description="提交作答后可查看参考答案与解析" :image-size="70" />
      </template>
      <el-empty v-else-if="!loading" description="题目不存在或已删除" />
    </el-card>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import axios from '../utils/axios.js'
import { questionTypeLabel } from '../utils/questionType.js'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const question = ref(null)

// 作答相关状态
const userAnswer = ref('')
const submitting = ref(false)
const submitted = ref(false)
const revealed = ref(false)
const result = ref(null)

// 计时：进入页面即开始，提交时上报累计秒数
let startedAt = Date.now()
const elapsed = ref(0)
let timer = null
const elapsedText = computed(() => {
  const m = Math.floor(elapsed.value / 60)
  const s = elapsed.value % 60
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
})

async function load() {
  loading.value = true
  try {
    question.value = (await axios.get(`/api/questions/${route.params.id}`)).data.data
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '题目详情加载失败')
  } finally {
    loading.value = false
  }
}

async function submit() {
  if (!userAnswer.value.trim()) {
    ElMessage.warning('请先输入你的作答')
    return
  }
  submitting.value = true
  try {
    const payload = {
      questionId: Number(route.params.id),
      userAnswer: userAnswer.value.trim(),
      timeCostSeconds: elapsed.value,
    }
    result.value = (await axios.post('/api/answer/submit', payload)).data.data
    submitted.value = true
    revealed.value = true
    if (timer) { clearInterval(timer); timer = null }
    ElMessage.success('提交成功')
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '提交失败')
  } finally {
    submitting.value = false
  }
}

function formatTime(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 19)
}
function difficultyText(value) { return ({ 1: '简单', 2: '中等', 3: '困难' })[value] || '未知' }
function difficultyType(value) { return ({ 1: 'success', 2: 'warning', 3: 'danger' })[value] || 'info' }

onMounted(() => {
  load()
  startedAt = Date.now()
  timer = setInterval(() => { elapsed.value = Math.floor((Date.now() - startedAt) / 1000) }, 1000)
})
</script>

<style scoped>
.detail-card { margin-top: 18px; }
.title-row { display: flex; align-items: center; justify-content: space-between; font-size: 18px; font-weight: 600; }
h3 { margin: 26px 0 10px; font-size: 16px; }
article { white-space: pre-wrap; line-height: 1.9; color: #303133; }
.answer { padding: 14px; border-radius: 4px; background: #f0f9eb; }
.tag-list { display: flex; flex-wrap: wrap; gap: 6px; }
.empty-value { color: #909399; }
.submit-bar { display: flex; align-items: center; justify-content: space-between; margin-top: 12px; }
.submit-actions { display: flex; align-items: center; gap: 8px; }
.timer { color: #909399; font-size: 13px; }
.submit-tip { margin-top: 14px; }
.answer-hidden { margin-top: 20px; }
</style>
