<template>
  <section>
    <div class="page-title"><div><h2>仪表盘</h2><p>系统数据概览</p></div><el-button :icon="Refresh" :loading="loading" @click="loadStats">刷新数据</el-button></div>

    <el-row :gutter="20">
      <el-col v-for="item in statsCards" :key="item.label" :span="8">
        <el-card v-loading="loading" shadow="hover" class="stat-card">
          <div class="stat-content">
            <el-icon :size="34" :color="item.color"><component :is="item.icon" /></el-icon>
            <div><div class="stat-value">{{ item.value }}</div><div class="stat-label">{{ item.label }}</div></div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="notice-card" header="说明">
      <el-alert title="当前后端尚未提供 GET /api/admin/stats，本页面已通过现有的用户、题目、标签列表接口实时汇总统计数据。后续接入专用统计接口时，只需替换 loadStats()。" type="info" :closable="false" show-icon />
    </el-card>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import axios from '../../utils/axios.js'

const loading = ref(false)
const stats = reactive({ users: 0, questions: 0, tags: 0 })
const statsCards = computed(() => [
  { label: '用户总数', value: stats.users, icon: 'UserFilled', color: '#409eff' },
  { label: '题目总数', value: stats.questions, icon: 'Files', color: '#67c23a' },
  { label: '标签总数', value: stats.tags, icon: 'PriceTag', color: '#e6a23c' },
])

/**
 * TODO：后端实现 GET /api/admin/stats 后，可改为一次请求。
 * 现阶段严格复用真实后端接口：三个请求并行，以分页 total / 标签数组 length 汇总。
 */
async function loadStats() {
  loading.value = true
  try {
    const [usersResponse, questionsResponse, tagsResponse] = await Promise.all([
      axios.get('/api/users', { params: { pageNum: 1, pageSize: 1 } }),
      axios.get('/api/questions', { params: { pageNum: 1, pageSize: 1 } }),
      axios.get('/api/tags'),
    ])
    stats.users = Number(usersResponse.data.data?.total || 0)
    stats.questions = Number(questionsResponse.data.data?.total || 0)
    stats.tags = (tagsResponse.data.data || []).length
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '统计数据加载失败')
  } finally {
    loading.value = false
  }
}
onMounted(loadStats)
</script>

<style scoped>
.page-title { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; }
h2 { margin: 0; font-size: 22px; }
p { margin: 8px 0 0; color: #909399; font-size: 14px; }
.stat-card { min-height: 130px; }
.stat-content { display: flex; align-items: center; gap: 20px; height: 82px; }
.stat-value { font-size: 30px; font-weight: 600; line-height: 1; }
.stat-label { margin-top: 10px; color: #909399; }
.notice-card { margin-top: 20px; }
</style>
