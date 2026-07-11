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
        <h3>参考答案</h3><article class="answer">{{ question.answer || '-' }}</article>
        <h3>答案解析</h3><article>{{ question.answerAnalysis || '-' }}</article>
      </template>
      <el-empty v-else-if="!loading" description="题目不存在或已删除" />
    </el-card>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import axios from '../utils/axios.js'
import { questionTypeLabel } from '../utils/questionType.js'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const question = ref(null)
async function load() {
  loading.value = true
  try { question.value = (await axios.get(`/api/questions/${route.params.id}`)).data.data } catch (error) { ElMessage.error(error.response?.data?.message || '题目详情加载失败') } finally { loading.value = false }
}
function difficultyText(value) { return ({ 1: '简单', 2: '中等', 3: '困难' })[value] || '未知' }
function difficultyType(value) { return ({ 1: 'success', 2: 'warning', 3: 'danger' })[value] || 'info' }
onMounted(load)
</script>

<style scoped>
.detail-card { margin-top: 18px; }
.title-row { display: flex; align-items: center; justify-content: space-between; font-size: 18px; font-weight: 600; }
h3 { margin: 26px 0 10px; font-size: 16px; }
article { white-space: pre-wrap; line-height: 1.9; color: #303133; }
.answer { padding: 14px; border-radius: 4px; background: #f0f9eb; }
.tag-list { display: flex; flex-wrap: wrap; gap: 6px; }
.empty-value { color: #909399; }
</style>
