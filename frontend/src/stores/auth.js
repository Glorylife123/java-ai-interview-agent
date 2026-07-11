import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import axios from '../utils/axios.js'

/**
 * 认证状态 Store。
 *
 * 管理 accessToken 与 userInfo（含 role），并同步 localStorage 防止刷新丢失。
 * 注意：refresh_token 由后端通过 HttpOnly Cookie 自动管理，前端无需存储。
 *
 * 后端角色取值为大写 'ADMIN' / 'USER'（务必与后端保持一致，切勿写成小写）。
 */
const TOKEN_KEY = 'accessToken'
const USER_KEY = 'userInfo'

export const useAuthStore = defineStore('auth', () => {
  // ---------- 状态 ----------
  const accessToken = ref('')
  const expiresIn = ref(0) // 有效期（秒），仅用于展示
  // 登录用户信息：{ id, username, nickname, role, status }（不含密码哈希）
  const userInfo = ref(null)

  // ---------- 计算属性 ----------
  const isLoggedIn = computed(() => !!accessToken.value)
  const role = computed(() => userInfo.value?.role || '')
  // 权限判断的唯一入口，其余组件一律复用它，避免各处硬编码字符串
  const isAdmin = computed(() => role.value === 'ADMIN')

  // ---------- 初始化：从 localStorage 恢复 ----------
  function initFromStorage() {
    const token = localStorage.getItem(TOKEN_KEY)
    if (token) accessToken.value = token
    const rawUser = localStorage.getItem(USER_KEY)
    if (rawUser) {
      try {
        userInfo.value = JSON.parse(rawUser)
      } catch {
        userInfo.value = null
      }
    }
  }

  // ---------- Actions ----------
  function setToken(token, expIn) {
    accessToken.value = token
    expiresIn.value = expIn || 0
    localStorage.setItem(TOKEN_KEY, token)
  }

  function setUserInfo(info) {
    userInfo.value = info || null
    if (info) localStorage.setItem(USER_KEY, JSON.stringify(info))
    else localStorage.removeItem(USER_KEY)
  }

  function clearToken() {
    accessToken.value = ''
    expiresIn.value = 0
    userInfo.value = null
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
  }

  /**
   * 登录：调用后端登录接口，成功后保存 token 与用户信息。
   * 后端响应结构：{ code, message, data: { access_token, expires_in, user } }
   * @returns {Promise<object>} 响应体 data 部分（含 user，供调用方按 role 决定跳转）
   */
  async function login(username, password) {
    const response = await axios.post('/api/auth/login', { username, password })
    const data = response.data.data
    setToken(data.access_token, data.expires_in)
    setUserInfo(data.user)
    return data
  }

  /**
   * 退出登录：调用后端注销接口（失效当前设备 RT），再清空本地状态。
   */
  async function logout() {
    try {
      await axios.post('/api/auth/logout')
    } catch {
      // 后端注销失败（如 RT 已过期），仍执行本地清空
    }
    clearToken()
  }

  // 初始化时立即恢复一次
  initFromStorage()

  return {
    accessToken, expiresIn, userInfo,
    isLoggedIn, role, isAdmin,
    setToken, setUserInfo, clearToken, initFromStorage,
    login, logout,
  }
})
