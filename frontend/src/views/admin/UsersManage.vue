<template>
  <section>
    <el-card shadow="never">
      <template #header><div class="card-header"><span>用户管理</span></div></template>
      <el-form :inline="true"><el-form-item label="每页数量"><el-select v-model="pageSize" style="width: 110px" @change="changeSize"><el-option v-for="size in [10, 20, 50, 100]" :key="size" :label="`${size} 条`" :value="size" /></el-select></el-form-item><el-form-item><el-button :icon="Refresh" @click="load">刷新</el-button></el-form-item></el-form>
      <!-- 真实后端 GET /api/users 目前无 keyword 参数；因此暂不伪造搜索，待后端扩展后再补。 -->
      <el-table v-loading="loading" :data="users" stripe>
        <el-table-column type="index" label="序号" width="70" :index="indexMethod" />
        <el-table-column prop="username" label="用户名" min-width="150" />
        <el-table-column prop="nickname" label="昵称" min-width="150"><template #default="{ row }">{{ row.nickname || '-' }}</template></el-table-column>
        <el-table-column label="角色" width="110"><template #default="{ row }"><el-tag :type="row.role === 'ADMIN' ? 'danger' : 'success'">{{ row.role === 'ADMIN' ? '管理员' : '用户' }}</el-tag></template></el-table-column>
        <el-table-column label="状态" width="110"><template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '正常' : '已禁用' }}</el-tag></template></el-table-column>
        <el-table-column prop="createdAt" label="注册时间" min-width="180" />
        <el-table-column label="操作" width="265" fixed="right"><template #default="{ row }"><el-button type="primary" link @click="openResetPassword(row)">重置密码</el-button><el-button :type="row.status === 1 ? 'warning' : 'success'" link @click="toggleStatus(row)">{{ row.status === 1 ? '禁用' : '启用' }}</el-button><el-button type="danger" link @click="remove(row)">删除</el-button></template></el-table-column>
      </el-table>
      <el-empty v-if="!loading && users.length === 0" description="暂无用户" />
      <Pagination v-model:page="pageNum" v-model:size="pageSize" :total="total" @change="load" />
    </el-card>

    <el-dialog v-model="resetDialogVisible" :title="`重置密码：${resetTarget?.username || ''}`" width="460px" destroy-on-close @closed="resetPasswordForm">
      <el-alert title="重置后该用户的全部 Refresh Token 会被吊销，所有已登录设备将在 Access Token 过期后无法续期。" type="warning" :closable="false" show-icon class="reset-alert" />
      <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-width="90px"><el-form-item label="新密码" prop="password"><el-input v-model="passwordForm.password" type="password" show-password autocomplete="new-password" /></el-form-item><el-form-item label="确认密码" prop="confirmPassword"><el-input v-model="passwordForm.confirmPassword" type="password" show-password autocomplete="new-password" @keyup.enter="submitResetPassword" /></el-form-item></el-form>
      <template #footer><el-button @click="resetDialogVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="submitResetPassword">确认重置</el-button></template>
    </el-dialog>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import axios from '../../utils/axios.js'
import Pagination from '../../components/Pagination.vue'
import { useAuthStore } from '../../stores/auth.js'

const authStore = useAuthStore()
const loading = ref(false); const saving = ref(false); const users = ref([]); const total = ref(0); const pageNum = ref(1); const pageSize = ref(10)
const resetDialogVisible = ref(false); const resetTarget = ref(null); const passwordFormRef = ref(); const passwordForm = reactive({ password: '', confirmPassword: '' })
const validateConfirm = (_rule, value, callback) => value !== passwordForm.password ? callback(new Error('两次输入的密码不一致')) : callback()
const passwordRules = { password: [{ required: true, message: '请输入新密码', trigger: 'blur' }, { min: 6, message: '密码至少 6 位', trigger: 'blur' }], confirmPassword: [{ required: true, message: '请确认新密码', trigger: 'blur' }, { validator: validateConfirm, trigger: 'blur' }] }
async function load() { loading.value = true; try { const page = (await axios.get('/api/users', { params: { pageNum: pageNum.value, pageSize: pageSize.value } })).data.data || {}; users.value = page.records || []; total.value = Number(page.total || 0) } catch (error) { ElMessage.error(error.response?.data?.message || '用户加载失败') } finally { loading.value = false } }
function changeSize() { pageNum.value = 1; load() }
// 序号按分页位置连续显示，与数据库主键 id 解耦，删除用户后仍然连贯。
function indexMethod(rowIndex) { return (pageNum.value - 1) * pageSize.value + rowIndex + 1 }
function openResetPassword(row) { resetTarget.value = row; resetDialogVisible.value = true }
function resetPasswordForm() { resetTarget.value = null; Object.assign(passwordForm, { password: '', confirmPassword: '' }); passwordFormRef.value?.clearValidate() }
async function submitResetPassword() {
  const valid = await passwordFormRef.value?.validate().catch(() => false); if (!valid || !resetTarget.value) return
  saving.value = true
  try {
    // 真实后端无 /api/admin/users/{id}/reset-password：更新密码后再调用 revoke，组合实现同等安全效果。
    await axios.put(`/api/users/${resetTarget.value.id}`, { passwordHash: passwordForm.password })
    await axios.post(`/api/admin/revoke/${resetTarget.value.id}`)
    ElMessage.success('密码已重置，相关会话已强制下线'); resetDialogVisible.value = false; load()
  } catch (error) { ElMessage.error(error.response?.data?.message || '重置密码失败') } finally { saving.value = false }
}
async function toggleStatus(row) {
  const nextStatus = row.status === 1 ? 0 : 1; const action = nextStatus === 1 ? '启用' : '禁用'
  try { await ElMessageBox.confirm(`确认${action}用户「${row.username}」？`, `${action}确认`, { type: 'warning' }) } catch { return }
  try { await axios.put(`/api/users/${row.id}`, { status: nextStatus }); if (nextStatus === 0) await axios.post(`/api/admin/revoke/${row.id}`); ElMessage.success(`${action}成功`); load() } catch (error) { ElMessage.error(error.response?.data?.message || `${action}失败`) }
}
async function remove(row) {
  if (row.id === authStore.userInfo?.id) { ElMessage.warning('不能删除当前登录账号'); return }
  try { await ElMessageBox.confirm(`确认删除用户「${row.username}」？此操作不可恢复。`, '删除确认', { type: 'warning', confirmButtonText: '删除' }) } catch { return }
  try { await axios.delete(`/api/users/${row.id}`); await axios.post(`/api/admin/revoke/${row.id}`); ElMessage.success('删除成功'); load() } catch (error) { ElMessage.error(error.response?.data?.message || '删除失败') }
}
onMounted(load)
</script>

<style scoped>.card-header { font-size: 18px; font-weight: 600; }.reset-alert { margin-bottom: 20px; }</style>
