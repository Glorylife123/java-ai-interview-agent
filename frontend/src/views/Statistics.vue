<template>
  <section>
    <div class="page-title">
      <div><h2>练习统计</h2><p>我的刷题数据总览</p></div>
      <el-button :icon="Refresh" :loading="loading" @click="loadAll">刷新</el-button>
    </div>

    <!-- ========== 总览卡片 ========== -->
    <el-row :gutter="20">
      <el-col v-for="item in overviewCards" :key="item.label" :xs="12" :sm="12" :md="8" :lg="4">
        <el-card v-loading="loading" shadow="hover" class="stat-card">
          <div class="stat-content">
            <el-icon :size="34" :color="item.color"><component :is="item.icon" /></el-icon>
            <div>
              <div class="stat-value">{{ item.value }}</div>
              <div class="stat-label">{{ item.label }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="block-row">
      <!-- ========== 分类统计 ========== -->
      <el-col :xs="24" :lg="12">
        <el-card v-loading="loading" shadow="never" class="block-card">
          <template #header><div class="block-header"><el-icon><PieChart /></el-icon><span>分类统计</span></div></template>
          <el-empty v-if="!loading && categories.length === 0" description="暂无分类数据" :image-size="80" />
          <div v-else class="bar-list">
            <div v-for="c in categories" :key="c.category" class="bar-item">
              <div class="bar-meta">
                <span class="bar-name" :title="c.category">{{ c.category }}</span>
                <span class="bar-rate" :class="rateClass(c.correctRate)">{{ formatRate(c.correctRate) }}</span>
              </div>
              <div class="bar-track">
                <div class="bar-fill correct" :style="{ width: correctWidth(c) + '%' }"></div>
                <div class="bar-fill wrong" :style="{ width: wrongWidth(c) + '%' }"></div>
              </div>
              <div class="bar-detail">
                答题 {{ c.answeredCount }} · 正确 {{ c.correctCount }} · 错误 {{ c.wrongCount }}
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- ========== 薄弱标签 ========== -->
      <el-col :xs="24" :lg="12">
        <el-card v-loading="loading" shadow="never" class="block-card">
          <template #header>
            <div class="block-header">
              <el-icon><Warning /></el-icon><span>薄弱标签</span>
              <el-select v-model="weakLimit" size="small" class="weak-limit" @change="loadWeakTags">
                <el-option v-for="n in [5, 10, 20]" :key="n" :label="`前 ${n} 个`" :value="n" />
              </el-select>
            </div>
          </template>
          <el-empty v-if="!loading && weakTags.length === 0" description="暂无薄弱标签" :image-size="80" />
          <el-table v-else :data="weakTags" stripe>
            <el-table-column type="index" label="#" width="50" align="center" />
            <el-table-column prop="tagName" label="标签" min-width="140" show-overflow-tooltip />
            <el-table-column prop="wrongCount" label="错误次数" width="100" align="center">
              <template #default="{ row }"><el-tag type="danger" effect="plain">{{ row.wrongCount }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="totalCount" label="答题总数" width="100" align="center" />
            <el-table-column label="错误率" width="180" align="center">
              <template #default="{ row }">
                <el-progress :percentage="wrongRate(row)" :stroke-width="10" :color="progressColor(row)" />
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <!-- ========== 每日练习趋势 ========== -->
    <el-card v-loading="loading" shadow="never" class="block-card">
      <template #header>
        <div class="block-header">
          <el-icon><TrendCharts /></el-icon><span>每日练习趋势</span>
          <el-select v-model="dailyDays" size="small" class="weak-limit" @change="loadDaily">
            <el-option v-for="n in [7, 14, 30, 90]" :key="n" :label="`最近 ${n} 天`" :value="n" />
          </el-select>
        </div>
      </template>
      <el-empty v-if="!loading && daily.length === 0" description="最近无练习记录" :image-size="80" />
      <div v-else class="trend-chart">
        <div class="trend-bars">
          <div v-for="d in daily" :key="d.date" class="trend-col" :title="`${d.date}：${d.count} 题，正确率 ${formatRate(d.correctRate)}`">
            <div class="trend-value">{{ d.count }}</div>
            <div class="trend-bar" :style="{ height: trendHeight(d.count) + 'px' }"></div>
            <div class="trend-date">{{ shortDate(d.date) }}</div>
            <div class="trend-rate" :class="rateClass(d.correctRate)">{{ formatRate(d.correctRate) }}</div>
          </div>
        </div>
      </div>
    </el-card>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, PieChart, Warning, TrendCharts } from '@element-plus/icons-vue'
import axios from '../utils/axios.js'

const loading = ref(false)

// 总览统计
const overview = ref({ totalAnswered: 0, correctCount: 0, wrongCount: 0, correctRate: 0, wrongBookCount: 0, avgScore: 0 })
const overviewCards = computed(() => [
  { label: '总答题数', value: overview.value.totalAnswered, icon: 'Edit', color: '#409eff' },
  { label: '正确数', value: overview.value.correctCount, icon: 'CircleCheck', color: '#67c23a' },
  { label: '错误数', value: overview.value.wrongCount, icon: 'CircleClose', color: '#f56c6c' },
  { label: '正确率', value: formatRate(overview.value.correctRate), icon: 'DataAnalysis', color: '#e6a23c' },
  { label: '错题本', value: overview.value.wrongBookCount, icon: 'Notebook', color: '#9254de' },
  { label: '平均分', value: Number(overview.value.avgScore || 0).toFixed(1), icon: 'Trophy', color: '#13c2c2' },
])

// 分类统计 + 薄弱标签 + 每日趋势
const categories = ref([])
const weakTags = ref([])
const daily = ref([])
const weakLimit = ref(5)
const dailyDays = ref(30)

// 每日趋势最大题数（用于柱高比例归一化）
const dailyMaxCount = computed(() => daily.value.reduce((m, d) => Math.max(m, Number(d.count) || 0), 1))

async function loadOverview() {
  try {
    const { data } = await axios.get('/api/stat/overview')
    overview.value = data.data || overview.value
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '总览统计加载失败')
  }
}

async function loadCategories() {
  try {
    const { data } = await axios.get('/api/stat/category')
    categories.value = data.data || []
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '分类统计加载失败')
  }
}

async function loadWeakTags() {
  try {
    const { data } = await axios.get('/api/stat/weak-tags', { params: { limit: weakLimit.value } })
    weakTags.value = data.data || []
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '薄弱标签加载失败')
  }
}

async function loadDaily() {
  try {
    const { data } = await axios.get('/api/stat/daily', { params: { days: dailyDays.value } })
    daily.value = data.data || []
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '每日趋势加载失败')
  }
}

async function loadAll() {
  loading.value = true
  await Promise.all([loadOverview(), loadCategories(), loadWeakTags(), loadDaily()])
  loading.value = false
}

// ---------- 格式化与辅助 ----------
function formatRate(v) {
  const n = Number(v || 0)
  // 后端已保留一位小数（BigDecimal），这里仅兜底展示
  return `${n.toFixed(1)}%`
}
function rateClass(rate) {
  const n = Number(rate || 0)
  if (n >= 80) return 'rate-good'
  if (n >= 60) return 'rate-mid'
  return 'rate-bad'
}
// 分类条：正确段 + 错误段拼接，分母为 0 时无填充
function correctWidth(c) {
  const total = Number(c.answeredCount) || 0
  return total === 0 ? 0 : (Number(c.correctCount) / total) * 100
}
function wrongWidth(c) {
  const total = Number(c.answeredCount) || 0
  return total === 0 ? 0 : (Number(c.wrongCount) / total) * 100
}
// 薄弱标签错误率
function wrongRate(row) {
  const total = Number(row.totalCount) || 0
  return total === 0 ? 0 : Math.round((Number(row.wrongCount) / total) * 100)
}
function progressColor(row) {
  const r = wrongRate(row)
  return r >= 70 ? '#f56c6c' : r >= 40 ? '#e6a23c' : '#67c23a'
}
// 每日趋势柱高（10px 下限，最高 120px）
function trendHeight(count) {
  const n = Number(count) || 0
  return 10 + (n / (dailyMaxCount.value || 1)) * 110
}
function shortDate(date) {
  // yyyy-MM-dd -> MM-dd
  if (!date) return ''
  const parts = String(date).split('-')
  return parts.length >= 3 ? `${parts[1]}-${parts[2]}` : date
}

onMounted(loadAll)
</script>

<style scoped>
.page-title { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; }
h2 { margin: 0; font-size: 22px; }
p { margin: 8px 0 0; color: #909399; font-size: 14px; }

.stat-card { min-height: 110px; margin-bottom: 16px; }
.stat-content { display: flex; align-items: center; gap: 16px; height: 64px; }
.stat-value { font-size: 26px; font-weight: 600; line-height: 1; }
.stat-label { margin-top: 8px; color: #909399; font-size: 13px; }

.block-row { margin-bottom: 0; }
.block-card { margin-bottom: 20px; }
.block-header { display: flex; align-items: center; gap: 8px; font-size: 16px; font-weight: 600; }
.weak-limit { width: 120px; margin-left: auto; }

/* 分类条 */
.bar-list { display: flex; flex-direction: column; gap: 18px; }
.bar-meta { display: flex; justify-content: space-between; align-items: center; font-size: 13px; }
.bar-name { font-weight: 500; max-width: 60%; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.bar-rate { font-weight: 600; }
.bar-track { display: flex; height: 14px; margin-top: 6px; border-radius: 7px; overflow: hidden; background: #f0f2f5; }
.bar-fill { transition: width .3s ease; }
.bar-fill.correct { background: #67c23a; }
.bar-fill.wrong { background: #f56c6c; }
.bar-detail { margin-top: 6px; color: #909399; font-size: 12px; }

.rate-good { color: #67c23a; }
.rate-mid { color: #e6a23c; }
.rate-bad { color: #f56c6c; }

/* 每日趋势 */
.trend-chart { overflow-x: auto; }
.trend-bars { display: flex; align-items: flex-end; gap: 10px; min-width: 100%; padding-top: 8px; }
.trend-col { display: flex; flex-direction: column; align-items: center; flex: 1 0 60px; min-width: 60px; }
.trend-value { font-size: 12px; color: #606266; }
.trend-bar { width: 60%; min-height: 10px; margin: 6px 0; border-radius: 4px 4px 0 0; background: linear-gradient(180deg, #409eff 0%, #79bbff 100%); }
.trend-date { font-size: 11px; color: #909399; }
.trend-rate { margin-top: 2px; font-size: 11px; font-weight: 600; }
</style>
