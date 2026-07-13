<template>
  <section v-loading="loading">
    <el-page-header content="面试报告" @back="router.push('/interview')" />

    <template v-if="report">
      <!-- 总分概览 -->
      <el-card class="overview-card">
        <div class="overview">
          <el-progress
            type="dashboard"
            :percentage="report.overallScore || 0"
            :color="scoreColor"
            :width="140"
          >
            <template #default="{ percentage }">
              <div class="dashboard-inner">
                <span class="dashboard-score">{{ percentage }}</span>
                <span class="dashboard-label">总体得分</span>
              </div>
            </template>
          </el-progress>
          <div class="summary">
            <h3>总结评语</h3>
            <p>{{ report.summary || '-' }}</p>
            <div class="report-meta">
              <span>生成方式：{{ report.generatorType === 'RULE' ? '规则评分' : report.generatorType }}</span>
              <span>生成时间：{{ formatTime(report.createdAt) }}</span>
            </div>
          </div>
        </div>
      </el-card>

      <!-- 维度得分 -->
      <el-card class="section-card">
        <template #header><span>各维度得分</span></template>
        <div v-if="dimensionList.length" class="dimensions">
          <div v-for="d in dimensionList" :key="d.name" class="dimension-row">
            <span class="dimension-name">{{ d.name }}</span>
            <el-progress
              :percentage="d.score"
              :color="scoreColor"
              :stroke-width="14"
              class="dimension-bar"
            />
          </div>
        </div>
        <el-empty v-else description="暂无维度数据" :image-size="60" />
      </el-card>

      <!-- 学习建议 -->
      <el-card class="section-card">
        <template #header><span>学习建议</span></template>
        <ul v-if="report.suggestions?.length" class="suggestions">
          <li v-for="(s, i) in report.suggestions" :key="i">{{ s }}</li>
        </ul>
        <el-empty v-else description="暂无建议" :image-size="60" />
      </el-card>
    </template>

    <!-- 仅当面试尚未结束时（无总体报告），用一行提示代替上方概览，但答题明细仍可看 -->
    <el-alert
      v-else-if="!loading && !report && answerMeta"
      type="warning"
      :closable="false"
      show-icon
      title="面试尚未结束，暂无总体报告"
      description="可以查看下方已答每一题的作答与评分反馈；完成全部题目后才会生成总体报告。"
      class="overview-card"
    />

    <!-- 我的答题情况（逐题回看） -->
    <el-card class="section-card">
      <template #header>
        <div class="answer-header">
          <span>我的答题情况</span>
          <span v-if="answerMeta" class="answer-meta-text">
            已答 {{ answerMeta.answeredCount }} / {{ answerMeta.totalQuestionCount }} 题
          </span>
        </div>
      </template>
      <div v-if="answerDetails.length">
        <el-collapse v-model="activeNames" accordion>
          <el-collapse-item
            v-for="(d, i) in answerDetails"
            :key="d.questionRecordId"
            :name="String(d.questionRecordId)"
          >
            <template #title>
              <div class="answer-item-title">
                <span class="answer-index">第 {{ d.sortOrder + 1 }} 题</span>
                <el-tag v-if="d.questionType" size="small" type="info" effect="plain">{{ d.questionType }}</el-tag>
                <el-tag v-if="d.difficulty" :type="difficultyType(d.difficulty)" size="small" effect="plain">{{ d.difficulty }}</el-tag>
                <el-tag v-if="!d.answered" size="small" type="warning">未作答</el-tag>
                <el-tag
                  v-else
                  :type="scoreTagType(d.score)"
                  size="small"
                >{{ d.score ?? '-' }} 分</el-tag>
              </div>
            </template>

            <div class="answer-detail">
              <div class="detail-block">
                <div class="detail-label">题目</div>
                <article class="question-content">{{ d.questionContent || '-' }}</article>
              </div>

              <div class="detail-block">
                <div class="detail-label">
                  我的作答
                  <span v-if="d.answered && d.durationSeconds != null" class="duration">耗时 {{ formatDuration(d.durationSeconds) }}</span>
                </div>
                <article v-if="d.answered" class="answer-content">{{ d.answerContent || '(空)' }}</article>
                <p v-else class="empty-text">本题未作答</p>
              </div>

              <template v-if="d.answered && d.level">
                <div class="detail-block">
                  <div class="detail-label">评分反馈</div>
                  <div class="feedback-line">
                    <span class="score-big" :class="scoreTextClass(d.score)">{{ d.score ?? '-' }}</span>
                    <span class="score-max"> / {{ d.maxScore || 100 }}</span>
                    <el-tag :type="levelType(d.level)" size="small">{{ d.level }}</el-tag>
                  </div>
                </div>

                <div v-if="d.matchedPoints?.length || d.missingPoints?.length" class="detail-block">
                  <el-descriptions :column="1" border size="small">
                    <el-descriptions-item label="命中得分点">
                      <div v-if="d.matchedPoints?.length" class="point-list">
                        <el-tag v-for="(p, j) in d.matchedPoints" :key="j" type="success" effect="plain" size="small">{{ p }}</el-tag>
                      </div>
                      <span v-else class="empty-text">无</span>
                    </el-descriptions-item>
                    <el-descriptions-item label="缺失得分点">
                      <div v-if="d.missingPoints?.length" class="point-list">
                        <el-tag v-for="(p, j) in d.missingPoints" :key="j" type="danger" effect="plain" size="small">{{ p }}</el-tag>
                      </div>
                      <span v-else class="empty-text">无</span>
                    </el-descriptions-item>
                  </el-descriptions>
                </div>

                <div v-if="d.suggestion" class="detail-block">
                  <div class="detail-label">改进建议</div>
                  <p class="suggestion-text">{{ d.suggestion }}</p>
                </div>
              </template>
              <el-alert
                v-else-if="d.answered && !d.level"
                type="info"
                :closable="false"
                :show-icon="false"
                title="本题尚未评分"
              />
            </div>
          </el-collapse-item>
        </el-collapse>
      </div>
      <el-empty v-else description="暂无答题记录" :image-size="60" />
    </el-card>

    <div class="footer-actions">
      <el-button @click="router.push('/interview')">返回列表</el-button>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import axios from '../utils/axios.js'

const route = useRoute()
const router = useRouter()
const sessionId = Number(route.params.id)

const loading = ref(false)
const report = ref(null)
const answerMeta = ref(null)   // 整场元信息（含总数、已答数）
const answerDetails = ref([])  // 逐题明细
const activeNames = ref('')    // el-collapse 当前展开项

// dimensionScores 后端返回 Map<String,Integer>，转成数组便于渲染
const dimensionList = computed(() => {
  const scores = report.value?.dimensionScores || {}
  return Object.entries(scores).map(([name, score]) => ({ name, score: score ?? 0 }))
})

function scoreColor(percentage) {
  if (percentage >= 80) return '#67c23a'
  if (percentage >= 60) return '#e6a23c'
  return '#f56c6c'
}

function scoreTagType(score) {
  if (score == null) return 'info'
  if (score >= 80) return 'success'
  if (score >= 60) return 'warning'
  return 'danger'
}

function scoreTextClass(score) {
  if (score == null) return 'score-low'
  if (score >= 80) return 'score-high'
  if (score >= 60) return 'score-mid'
  return 'score-low'
}

function levelType(level) {
  return ({ 优秀: 'success', 良好: 'primary', 一般: 'warning', 较差: 'danger' })[level] || 'info'
}

function difficultyType(value) {
  return ({ 简单: 'success', 中等: 'warning', 困难: 'danger' })[value] || 'info'
}

function formatTime(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 19)
}

function formatDuration(seconds) {
  const s = Number(seconds) || 0
  const m = Math.floor(s / 60)
  const r = s % 60
  return `${String(m).padStart(2, '0')}:${String(r).padStart(2, '0')}`
}

// 后端业务错误走 HTTP 200 + code≠0，必须按 body.code 判定，否则错误被静默吞掉。
function unwrap(body, failMsg) {
  if (!body || body.code !== 0) {
    return null
  }
  return body.data
}

async function load() {
  loading.value = true
  try {
    // 报告仅在面试结束后存在；中途面试会拿到 404 业务码，属正常情况，不弹错。
    const reportBody = (await axios.get(`/api/interview/session/${sessionId}/report`)).data
    report.value = unwrap(reportBody)

    // 答题明细在 IN_PROGRESS 即可看；失败才提示。
    const answerBody = (await axios.get(`/api/interview/session/${sessionId}/answers`)).data
    const answerData = unwrap(answerBody, '答题情况加载失败')
    if (answerData) {
      answerMeta.value = answerData
      answerDetails.value = answerData.details || []
      // 默认展开第一题，便于直接查看
      if (answerDetails.value.length) {
        activeNames.value = String(answerDetails.value[0].questionRecordId)
      }
    } else {
      ElMessage.error(answerBody?.message || '答题情况加载失败')
    }
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '页面加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.overview-card { margin-top: 18px; }
.overview { display: flex; align-items: center; gap: 36px; }
.dashboard-inner { display: flex; flex-direction: column; align-items: center; }
.dashboard-score { font-size: 30px; font-weight: 700; color: #303133; }
.dashboard-label { font-size: 13px; color: #909399; margin-top: 2px; }
.summary { flex: 1; }
.summary h3 { margin: 0 0 10px; font-size: 16px; }
.summary p { line-height: 1.9; color: #303133; margin: 0 0 14px; }
.report-meta { display: flex; gap: 20px; color: #909399; font-size: 13px; }
.section-card { margin-top: 18px; }
.dimensions { display: flex; flex-direction: column; gap: 16px; }
.dimension-row { display: flex; align-items: center; gap: 14px; }
.dimension-name { width: 120px; flex-shrink: 0; color: #303133; }
.dimension-bar { flex: 1; }
.suggestions { margin: 0; padding-left: 20px; line-height: 2; color: #303133; }

.answer-header { display: flex; align-items: center; justify-content: space-between; }
.answer-meta-text { color: #909399; font-size: 13px; }
.answer-item-title { display: flex; align-items: center; gap: 8px; }
.answer-index { font-weight: 600; color: #303133; }

.answer-detail { padding: 4px 0 8px; }
.detail-block { margin-bottom: 16px; }
.detail-block:last-child { margin-bottom: 0; }
.detail-label { font-size: 13px; color: #909399; margin-bottom: 6px; }
.duration { margin-left: 10px; font-size: 12px; color: #c0c4cc; }
.question-content { white-space: pre-wrap; line-height: 1.9; color: #303133; padding: 12px; border-radius: 4px; background: #f4f4f5; }
.answer-content { white-space: pre-wrap; line-height: 1.9; color: #303133; padding: 12px; border-radius: 4px; background: #ecf5ff; }
.empty-text { color: #909399; }
.point-list { display: flex; flex-wrap: wrap; gap: 6px; }
.feedback-line { display: flex; align-items: center; gap: 12px; }
.score-big { font-size: 26px; font-weight: 700; }
.score-max { font-size: 14px; color: #909399; }
.score-high { color: #67c23a; }
.score-mid { color: #e6a23c; }
.score-low { color: #f56c6c; }
.suggestion-text { margin: 0; line-height: 1.8; color: #303133; }

.footer-actions { margin-top: 20px; text-align: center; }
</style>
