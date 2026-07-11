<template>
  <el-container class="layout-root">
    <!-- 顶部导航栏 -->
    <el-header class="layout-header">
      <div class="brand">
        <div class="brand-badge"><el-icon :size="18"><Cpu /></el-icon></div>
        <span class="brand-text">AI 面试辅助 Agent</span>
      </div>

      <div class="header-right">
        <span class="welcome">
          {{ authStore.userInfo?.nickname || authStore.userInfo?.username }}
        </span>
        <el-tag :type="authStore.isAdmin ? 'danger' : 'success'" size="small" effect="dark">
          {{ authStore.isAdmin ? '管理员' : '普通用户' }}
        </el-tag>
        <el-button type="primary" link @click="handleLogout">退出登录</el-button>
      </div>
    </el-header>

    <el-container>
      <!-- 侧边菜单：根据角色动态渲染 -->
      <el-aside width="210px" class="layout-aside">
        <el-menu :default-active="activeMenu" router class="side-menu">
          <el-menu-item
            v-for="item in menuStore.menus"
            :key="item.index"
            :index="item.index"
          >
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.title }}</span>
          </el-menu-item>
        </el-menu>
      </el-aside>

      <!-- 主内容区 -->
      <el-main class="layout-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useAuthStore } from '../stores/auth.js'
import { useMenuStore } from '../stores/menu.js'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const menuStore = useMenuStore()

// 高亮当前菜单：详情等子页面（如 /questions/1）归属到 /questions
const activeMenu = computed(() => {
  if (route.path.startsWith('/questions')) return '/questions'
  return route.path
})

/** 退出登录：二次确认 → 调后端注销 → 清本地 → 跳登录 */
async function handleLogout() {
  try {
    await ElMessageBox.confirm('确认退出登录？', '提示', {
      type: 'warning',
      confirmButtonText: '退出',
      cancelButtonText: '取消',
    })
  } catch {
    return // 用户取消
  }
  await authStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.layout-root {
  height: 100vh;
}
.layout-header {
  position: sticky;
  top: 0;
  z-index: 10;
  height: 62px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  /* 毛玻璃导航：半透明白 + 背景模糊 + 发丝底边 */
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: saturate(180%) blur(20px);
  -webkit-backdrop-filter: saturate(180%) blur(20px);
  border-bottom: 1px solid var(--hairline);
  color: var(--ink-900);
}
.brand {
  display: flex;
  align-items: center;
  gap: 11px;
  font-size: 17px;
  font-weight: 600;
  letter-spacing: -0.01em;
}
/* 品牌标记：青绿圆角方块，作为整个界面的签名元素 */
.brand-badge {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 9px;
  color: #fff;
  background: var(--brand-gradient);
  box-shadow: var(--shadow-accent);
}
.brand-text { color: var(--ink-900); }
.header-right {
  display: flex;
  align-items: center;
  gap: 14px;
}
.header-right :deep(.el-button.is-link) {
  color: var(--brand-600);
}
.welcome {
  font-size: 14px;
  color: var(--ink-600);
  font-weight: 500;
}
.layout-aside {
  background: transparent;
  border-right: 1px solid var(--hairline);
}
.side-menu {
  border-right: none;
  height: 100%;
  padding: 14px 12px;
  background: transparent;
}
.side-menu :deep(.el-menu-item) {
  border-radius: 10px;
  margin-bottom: 4px;
  height: 44px;
  font-size: 14.5px;
  color: var(--ink-600);
}
.side-menu :deep(.el-menu-item:hover) {
  background: rgba(13, 148, 136, 0.08);
  color: var(--brand-600);
}
.side-menu :deep(.el-menu-item.is-active) {
  background: var(--brand-500);
  color: #fff;
  font-weight: 600;
  box-shadow: var(--shadow-accent);
}
.side-menu :deep(.el-menu-item.is-active .el-icon) {
  color: #fff;
}
.layout-main {
  background: var(--app-bg);
  padding: 26px 28px;
}
</style>
