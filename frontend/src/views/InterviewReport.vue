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

      <div class="footer-actions">
        <el-button @click="router.push('/interview')">返回列表</el-button>
      </div>
    </template>

    <el-empty v-else-if="!loading" description="报告不存在，请先完成面试" />
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

async function load() {
  loading.value = true
  try {
    report.value = (await axios.get(`/api/interview/session/${sessionId}/report`)).data.data
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '报告加载失败')
  } finally {
    loading.value = false
  }
}

function formatTime(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 19)
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
.footer-actions { margin-top: 20px; text-align: center; }
</style>
