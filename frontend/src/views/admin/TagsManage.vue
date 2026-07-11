<template>
  <section>
    <el-card shadow="never">
      <template #header><div class="card-header"><span>标签管理</span><el-button type="primary" :icon="Plus" @click="openCreate">新增标签</el-button></div></template>
      <el-table v-loading="loading" :data="tags" stripe>
        <el-table-column type="index" label="序号" width="70" />
        <el-table-column prop="name" label="标签名称" min-width="220" />
        <el-table-column prop="category" label="分类" min-width="180"><template #default="{ row }">{{ row.category || '-' }}</template></el-table-column>
        <el-table-column prop="createdAt" label="创建时间" min-width="180" />
        <el-table-column label="操作" width="160" fixed="right"><template #default="{ row }"><el-button type="primary" link @click="openEdit(row)">编辑</el-button><el-button type="danger" link @click="remove(row)">删除</el-button></template></el-table-column>
      </el-table>
      <el-empty v-if="!loading && tags.length === 0" description="暂无标签" />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑标签' : '新增标签'" width="460px" destroy-on-close @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="名称" prop="name"><el-input v-model.trim="form.name" maxlength="50" show-word-limit placeholder="例如：Java 基础" /></el-form-item>
        <el-form-item label="分类" prop="category"><el-input v-model.trim="form.category" maxlength="50" show-word-limit placeholder="例如：后端开发（可选）" /></el-form-item>
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

const loading = ref(false)
const saving = ref(false)
const tags = ref([])
const dialogVisible = ref(false)
const editingId = ref(null)
const formRef = ref()
const form = reactive({ name: '', category: '' })
const rules = { name: [{ required: true, message: '请输入标签名称', trigger: 'blur' }] }
async function load() { loading.value = true; try { tags.value = (await axios.get('/api/tags')).data.data || [] } catch (error) { ElMessage.error(error.response?.data?.message || '标签加载失败') } finally { loading.value = false } }
function resetForm() { editingId.value = null; Object.assign(form, { name: '', category: '' }); formRef.value?.clearValidate() }
function openCreate() { resetForm(); dialogVisible.value = true }
function openEdit(row) { editingId.value = row.id; Object.assign(form, { name: row.name || '', category: row.category || '' }); dialogVisible.value = true }
async function submit() {
  const valid = await formRef.value?.validate().catch(() => false); if (!valid) return
  saving.value = true
  try {
    if (editingId.value) await axios.put(`/api/tags/${editingId.value}`, form)
    else await axios.post('/api/tags', form)
    ElMessage.success('保存成功'); dialogVisible.value = false; load()
  } catch (error) { ElMessage.error(error.response?.data?.message || '保存失败') } finally { saving.value = false }
}
async function remove(row) {
  try { await ElMessageBox.confirm(`确认删除标签「${row.name}」？`, '删除确认', { type: 'warning' }) } catch { return }
  try { await axios.delete(`/api/tags/${row.id}`); ElMessage.success('删除成功'); load() } catch (error) { ElMessage.error(error.response?.data?.message || '删除失败') }
}
onMounted(load)
</script>

<style scoped>.card-header { display: flex; align-items: center; justify-content: space-between; font-size: 18px; font-weight: 600; }</style>
