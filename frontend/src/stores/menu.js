import { defineStore } from 'pinia'
import { computed } from 'vue'
import { useAuthStore } from './auth.js'

/**
 * 菜单 Store：根据当前用户角色动态生成可见菜单。
 *
 * 每个菜单项的 roles 声明了「哪些角色可见」，菜单渲染与路由守卫共用同一份角色约定。
 * icon 为 Element Plus 图标组件名（已在 main.js 全局注册）。
 */
const ALL_MENUS = [
  // ---- 普通用户 ----
  // 管理员使用“题库管理”，不展示面向刷题的 /questions 页面。
  { index: '/questions', title: '题库', icon: 'Reading', roles: ['USER'] },
  // 答题记录：查看自己的历史作答（当前不含 AI 判分）。
  { index: '/answer-records', title: '答题记录', icon: 'Notebook', roles: ['USER'] },
  // 错题本：手动维护 + 随机错题再练（本阶段不自动加入，等接入 AI 判分后自动累积）。
  { index: '/wrong-book', title: '错题本', icon: 'WarningFilled', roles: ['USER'] },
  // 练习统计：自己的总览/分类/薄弱标签/每日趋势。
  { index: '/statistics', title: '练习统计', icon: 'DataLine', roles: ['USER'] },
  // 个人中心对两类已登录用户均开放。
  { index: '/profile', title: '个人中心', icon: 'User', roles: ['USER', 'ADMIN'] },
  // ---- 仅管理员可见 ----
  { index: '/admin/dashboard', title: '仪表盘', icon: 'Odometer', roles: ['ADMIN'] },
  { index: '/admin/questions', title: '题库管理', icon: 'Files', roles: ['ADMIN'] },
  { index: '/admin/tags', title: '标签管理', icon: 'PriceTag', roles: ['ADMIN'] },
  { index: '/admin/users', title: '用户管理', icon: 'UserFilled', roles: ['ADMIN'] },
]

export const useMenuStore = defineStore('menu', () => {
  const authStore = useAuthStore()

  // 按当前角色过滤出可见菜单，角色变化（登录/退出）时自动重算
  const menus = computed(() =>
    ALL_MENUS.filter((m) => m.roles.includes(authStore.role))
  )

  return { menus }
})
