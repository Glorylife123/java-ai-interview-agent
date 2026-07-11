import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth.js'

/**
 * 客户端解码 JWT payload（仅读取，不验证签名）。
 * 用于路由守卫快速判断 token 是否过期，避免带着过期 token 跳转到受保护页面。
 */
function decodeJwtPayload(token) {
  try {
    const payloadBase64 = token.split('.')[1]
    const decoded = atob(payloadBase64.replace(/-/g, '+').replace(/_/g, '/'))
    return JSON.parse(decoded)
  } catch {
    return null
  }
}

/**
 * 检查 localStorage 中的 accessToken 是否仍有效（未过期）。
 * 若过期则清空本地状态并返回 false。
 */
function isValidToken() {
  const token = localStorage.getItem('accessToken')
  if (!token) return false

  const payload = decodeJwtPayload(token)
  if (!payload || !payload.exp) return false

  const now = Math.floor(Date.now() / 1000)
  if (payload.exp < now) {
    const authStore = useAuthStore()
    authStore.clearToken()
    return false
  }
  return true
}

/**
 * 按角色确定受保护页面的安全落点。
 * 管理员只进入后台仪表盘，普通用户进入刷题题库；所有权限拒绝和根路径跳转复用它，
 * 防止管理员被 /questions 的 USER 权限守卫反复重定向而形成循环。
 */
function getDefaultAuthenticatedPath(authStore) {
  return authStore.role === 'ADMIN' ? '/admin/dashboard' : '/questions'
}

/**
 * 路由表。
 *
 * 布局策略：登录/注册是独立整屏页面；其余业务页面都作为 MainLayout 的子路由，
 * 由 MainLayout 提供顶栏 + 侧边菜单，子页面经 <router-view /> 渲染。
 *
 * meta 约定：
 * - requiresAuth: true    需要登录
 * - roles: ['ADMIN']      需要指定角色（缺省表示任意已登录角色均可）
 */
const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/Register.vue'),
  },
  {
    path: '/',
    component: () => import('../layouts/MainLayout.vue'),
    // 根路径按当前角色跳转：ADMIN → 仪表盘，USER → 题库。
    redirect: () => getDefaultAuthenticatedPath(useAuthStore()),
    children: [
      // ---- 普通用户 ----
      {
        path: 'questions',
        name: 'Questions',
        component: () => import('../views/Questions.vue'),
        meta: { requiresAuth: true, roles: ['USER'], title: '题库' },
      },
      {
        path: 'questions/:id',
        name: 'QuestionDetail',
        component: () => import('../views/QuestionDetail.vue'),
        meta: { requiresAuth: true, roles: ['USER'], title: '题目详情' },
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('../views/Profile.vue'),
        meta: { requiresAuth: true, title: '个人中心' },
      },
      // ---- 管理员后台 ----
      {
        path: 'admin/dashboard',
        name: 'Dashboard',
        component: () => import('../views/admin/Dashboard.vue'),
        meta: { requiresAuth: true, roles: ['ADMIN'], title: '仪表盘' },
      },
      {
        path: 'admin/questions',
        name: 'QuestionsManage',
        component: () => import('../views/admin/QuestionsManage.vue'),
        meta: { requiresAuth: true, roles: ['ADMIN'], title: '题库管理' },
      },
      {
        path: 'admin/tags',
        name: 'TagsManage',
        component: () => import('../views/admin/TagsManage.vue'),
        meta: { requiresAuth: true, roles: ['ADMIN'], title: '标签管理' },
      },
      {
        path: 'admin/users',
        name: 'UsersManage',
        component: () => import('../views/admin/UsersManage.vue'),
        meta: { requiresAuth: true, roles: ['ADMIN'], title: '用户管理' },
      },
    ],
  },
  // 兜底先回根路径，再由根路由按角色选择题库或仪表盘。
  { path: '/:pathMatch(.*)*', redirect: '/' },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// ---------- 全局前置守卫 ----------
router.beforeEach((to) => {
  const authStore = useAuthStore()

  // 1. 登录/注册页公开放行（避免已登录/未登录都能正常访问，防止死循环）
  if (to.path === '/login' || to.path === '/register') {
    return true
  }

  // 2. 不需要认证的页面直接放行
  if (!to.meta.requiresAuth) {
    return true
  }

  // 3. 校验 token 是否存在且未过期；无效则清理并跳登录
  if (!isValidToken()) {
    authStore.clearToken()
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  // 4. 确保 Pinia 状态已从 localStorage 恢复（刷新页面后 Pinia 会丢失）
  if (!authStore.accessToken || !authStore.userInfo) {
    authStore.initFromStorage()
  }

  // 5. 角色校验：目标路由声明了 roles 且当前角色不在其中 → 回到按角色计算的安全落点。
  // 例如 ADMIN 直接输入 /questions 会被带回 /admin/dashboard，而不是形成 /questions 重定向循环。
  const roles = to.meta.roles
  if (roles && roles.length > 0 && !roles.includes(authStore.role)) {
    return getDefaultAuthenticatedPath(authStore)
  }

  return true
})

export default router
