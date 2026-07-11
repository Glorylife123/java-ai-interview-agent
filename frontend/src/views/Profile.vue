<template>
  <section class="profile-page">
    <el-row :gutter="20">
      <el-col :span="10">
        <el-card header="个人信息">
          <el-descriptions :column="1" border>
            <el-descriptions-item label="用户 ID">{{ authStore.userInfo?.id || '-' }}</el-descriptions-item>
            <el-descriptions-item label="用户名">{{ authStore.userInfo?.username || '-' }}</el-descriptions-item>
            <el-descriptions-item label="昵称">{{ authStore.userInfo?.nickname || '-' }}</el-descriptions-item>
            <el-descriptions-item label="角色"><el-tag :type="authStore.isAdmin ? 'danger' : 'success'">{{ authStore.isAdmin ? '管理员' : '普通用户' }}</el-tag></el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
      <el-col :span="14">
        <el-card header="修改密码">
          <el-alert title="密码修改成功后，当前账号的所有已登录设备会被强制下线，需要使用新密码重新登录。" type="warning" :closable="false" show-icon class="notice" />
          <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" class="password-form">
            <el-form-item label="原密码" prop="oldPassword"><el-input v-model="form.oldPassword" type="password" show-password autocomplete="current-password" /></el-form-item>
            <el-form-item label="新密码" prop="newPassword"><el-input v-model="form.newPassword" type="password" show-password autocomplete="new-password" /></el-form-item>
            <el-form-item label="确认新密码" prop="confirmPassword"><el-input v-model="form.confirmPassword" type="password" show-password autocomplete="new-password" @keyup.enter="changePassword" /></el-form-item>
            <el-form-item><el-button type="primary" :loading="saving" @click="changePassword">确认修改</el-button></el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
  </section>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from '../utils/axios.js'
import { useAuthStore } from '../stores/auth.js'

const router = useRouter()
const authStore = useAuthStore()
const formRef = ref()
const saving = ref(false)
const form = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const validateConfirm = (_rule, value, callback) => value !== form.newPassword ? callback(new Error('两次输入的新密码不一致')) : callback()
const rules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [{ required: true, message: '请输入新密码', trigger: 'blur' }, { min: 6, message: '新密码至少 6 位', trigger: 'blur' }],
  confirmPassword: [{ required: true, message: '请确认新密码', trigger: 'blur' }, { validator: validateConfirm, trigger: 'blur' }],
}
async function changePassword() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  try { await ElMessageBox.confirm('修改密码将使所有设备下线，确认继续？', '确认修改', { type: 'warning' }) } catch { return }
  saving.value = true
  try {
    await axios.post('/api/auth/change-password', { oldPassword: form.oldPassword, newPassword: form.newPassword })
    ElMessage.success('密码修改成功，请重新登录')
    authStore.clearToken()
    router.replace('/login')
  } catch (error) { ElMessage.error(error.response?.data?.message || '修改密码失败') } finally { saving.value = false }
}
</script>

<style scoped>
.profile-page { max-width: 1100px; }
.notice { margin-bottom: 22px; }
.password-form { max-width: 520px; }
</style>
