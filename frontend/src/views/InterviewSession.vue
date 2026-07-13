<template>
  <section v-loading="loading">
    <el-page-header :content="session ? session.title : '模拟面试'" @back="router.push('/interview')">
      <template #extra>
        <!-- 中途或结束后均可查看已答每一题的作答与评分反馈 -->
        <el-button v-if="session && session.status !== 'CREATED'" link type="primary" @click="goReport">
          查看我的答题情况
        </el-button>
      </template>
    </el-page-header>

    <el-card v-if="session" class="interview-card">
      <template #header>
        <div class="title-row">
          <div class="meta">
            <el-tag :type="difficultyType(session.difficulty)" size="small">{{ session.difficulty }}</el-tag>
            <span class="position">{{ session.position }}</span>
          </div>
          <span v-if="question" class="progress">第 {{ (question.sortOrder || 0) + 1 }} / {{ question.totalCount }} 题</span>
        </div>
      </template>

      <el-progress
        v-if="question"
        :percentage="progressPercent"
        :stroke-width="10"
        :show-text="false"
        class="progress-bar"
      />

      <!-- 当前题目与作答 -->
      <template v-if="question">
        <h3>题目</h3>
        <article class="question-content">{{ question.questionContent || '-' }}</article>

        <h3>我的作答</h3>
        <el-input
          v-model="userAnswer"
          type="textarea"
          :rows="6"
          :disabled="answered"
          maxlength="5000"
          show-word-limit
          placeholder="在此输入你的回答……"
        />
        <div class="submit-bar">
          <span class="timer">已用时 {{ elapsedText }}</span>
          <el-button
            v-if="!answered"
            type="primary"
            :loading="submitting"
            @click="submit"
          >提交回答</el-button>
        </div>
      </template>

      <!-- 本题即时反馈 -->
      <div v-if="feedback" class="feedback">
        <div class="feedback-head">
          <span class="score">{{ feedback.score }}<small> / 100</small></span>
          <el-tag :type="levelType(feedback.level)" size="large">{{ feedback.level }}</el-tag>
        </div>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="命中得分点">
            <div v-if="feedback.matchedPoints?.length" class="point-list">
              <el-tag v-for="(p, i) in feedback.matchedPoints" :key="i" type="success" effect="plain">{{ p }}</el-tag>
            </div>
            <span v-else class="empty-value">无</span>
          </el-descriptions-item>
          <el-descriptions-item label="缺失得分点">
            <div v-if="feedback.missingPoints?.length" class="point-list">
              <el-tag v-for="(p, i) in feedback.missingPoints" :key="i" type="danger" effect="plain">{{ p }}</el-tag>
            </div>
            <span v-else class="empty-value">无</span>
          </el-descriptions-item>
          <el-descriptions-item label="改进建议">{{ feedback.suggestion || '-' }}</el-descriptions-item>
        </el-descriptions>

        <div class="next-bar">
          <el-button v-if="!feedback.isFinished" type="primary" @click="goNext">下一题</el-button>
          <template v-else>
            <el-alert type="success" :closable="false" show-icon title="面试已完成！" class="finished-tip" />
            <el-button type="success" @click="goReport">查看面试报告</el-button>
          </template>
        </div>
      </div>

      <el-empty v-else-if="!question && !loading" description="暂无可作答的题目" />
    </el-card>
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import axios from '../utils/axios.js'

const route = useRoute()
const router = useRouter()
const sessionId = Number(route.params.id)

const loading = ref(false)
const submitting = ref(false)
const session = ref(null)
const question = ref(null)
const userAnswer = ref('')
const answered = ref(false)
const feedback = ref(null)

// 计时：每题独立计时，出下一题时重置
let startedAt = Date.now()
const elapsed = ref(0)
let timer = null
const elapsedText = computed(() => {
  const m = Math.floor(elapsed.value / 60)
  const s = elapsed.value % 60
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
})

const progressPercent = computed(() => {
  if (!question.value?.totalCount) return 0
  return Math.round(((question.value.sortOrder || 0) + 1) * 100 / question.value.totalCount)
})

function startTimer() {
  startedAt = Date.now()
  elapsed.value = 0
  if (timer) clearInterval(timer)
  timer = setInterval(() => { elapsed.value = Math.floor((Date.now() - startedAt) / 1000) }, 1000)
}
function stopTimer() {
  if (timer) { clearInterval(timer); timer = null }
}

// 进入页面：按会话状态决定「开始」或「继续」
async function init() {
  loading.value = true
  try {
    const sessions = (await axios.get('/api/interview/sessions')).data.data || []
    session.value = sessions.find((s) => s.id === sessionId) || null
    if (!session.value) {
      ElMessage.error('面试会话不存在')
      router.push('/interview')
      return
    }
    if (session.value.status === 'FINISHED' || session.value.status === 'CANCELLED') {
      router.replace(`/interview/${sessionId}/report`)
      return
    }
    // 与 submit 同理：start / current-question 的业务错误也走 HTTP 200 + code≠0，需显式判定。
    const body = session.value.status === 'CREATED'
      ? (await axios.post(`/api/interview/session/${sessionId}/start`)).data
      : (await axios.get(`/api/interview/session/${sessionId}/current-question`)).data
    if (!body || body.code !== 0) {
      ElMessage.error(body?.message || '进入面试失败')
      router.push('/interview')
      return
    }
    question.value = body.data
    startTimer()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '进入面试失败')
    router.push('/interview')
  } finally {
    loading.value = false
  }
}

async function submit() {
  if (!userAnswer.value.trim()) {
    ElMessage.warning('请先输入你的回答')
    return
  }
  submitting.value = true
  try {
    const payload = {
      questionRecordId: question.value.questionRecordId,
      answerContent: userAnswer.value.trim(),
      durationSeconds: elapsed.value,
    }
    const body = (await axios.post(`/api/interview/session/${sessionId}/answer`, payload)).data
    // 后端约定：仅 401/403 映射为真实 HTTP 状态码，其余业务错误（400/404/409/500）仍返回
    // HTTP 200 且 data 为 null。必须按 body.code 判定成败，否则错误会被静默吞掉，
    // 表现为「点了提交没反应」。
    if (!body || body.code !== 0) {
      ElMessage.error(body?.message || '提交失败')
      return
    }
    feedback.value = body.data
    answered.value = true
    stopTimer()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '提交失败')
  } finally {
    submitting.value = false
  }
}

// 进入下一题：用反馈里返回的 nextQuestion 刷新当前题
function goNext() {
  if (!feedback.value?.nextQuestion) return
  question.value = feedback.value.nextQuestion
  userAnswer.value = ''
  answered.value = false
  feedback.value = null
  startTimer()
}

function goReport() { router.push(`/interview/${sessionId}/report`) }

function difficultyType(value) {
  return ({ 简单: 'success', 中等: 'warning', 困难: 'danger' })[value] || 'info'
}
function levelType(level) {
  return ({ 优秀: 'success', 良好: 'primary', 一般: 'warning', 较差: 'danger' })[level] || 'info'
}

onMounted(init)
onBeforeUnmount(stopTimer)
</script>

<style scoped>
.interview-card { margin-top: 18px; }
.title-row { display: flex; align-items: center; justify-content: space-between; }
.meta { display: flex; align-items: center; gap: 10px; }
.position { font-size: 16px; font-weight: 600; }
.progress { color: #909399; font-size: 14px; }
.progress-bar { margin-bottom: 8px; }
h3 { margin: 22px 0 10px; font-size: 16px; }
.question-content { white-space: pre-wrap; line-height: 1.9; color: #303133; padding: 14px; border-radius: 4px; background: #f4f4f5; }
.submit-bar { display: flex; align-items: center; justify-content: space-between; margin-top: 12px; }
.timer { color: #909399; font-size: 13px; }
.feedback { margin-top: 26px; padding-top: 20px; border-top: 1px dashed #dcdfe6; }
.feedback-head { display: flex; align-items: center; gap: 16px; margin-bottom: 16px; }
.score { font-size: 34px; font-weight: 700; color: #409eff; }
.score small { font-size: 15px; color: #909399; font-weight: 400; }
.point-list { display: flex; flex-wrap: wrap; gap: 6px; }
.empty-value { color: #909399; }
.next-bar { display: flex; align-items: center; gap: 12px; margin-top: 18px; }
.finished-tip { flex: 1; }
</style>
