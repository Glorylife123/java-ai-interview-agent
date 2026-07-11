import axios from 'axios'
import { useAuthStore } from '../stores/auth.js'
import router from '../router/index.js'

/**
 * Axios 实例与拦截器 —— 双令牌认证闭环的核心。
 *
 * 功能：
 * 1. 请求拦截器自动夹带 Authorization: Bearer <accessToken>
 * 2. 响应拦截器捕获 401，自动用 RT 刷新 AT，并重试失败的请求
 * 3. 并发 401 情形下仅发起一次刷新，其余排队等待；刷新成功后批量重放
 * 4. 刷新失败（RT 也过期）时清空 Token 跳转登录页
 *
 * 注意：登录（/api/auth/login）和注册（/api/auth/register）接口的 401 是凭据错误，
 *       不是 token 过期，拦截器不会触发刷新流程，直接透传原始错误。
 */

// 公开接口列表：这些接口的 401 是业务错误（如密码错误），不应触发 token 刷新
const PUBLIC_ENDPOINTS = [
  '/api/auth/login',
  '/api/auth/register',
]

// ---------- 并发刷新控制 ----------
let isRefreshing = false        // 是否正在刷新中（锁）
let failedQueue = []            // 等待刷新完成后重试的请求队列

/**
 * 处理队列：遍历所有在刷新期间排队等待的请求，用新 Token 重试。
 * @param {string|null} token 新 Token（null 表示刷新失败）
 */
function processQueue(token) {
  failedQueue.forEach(({ resolve, reject, config }) => {
    if (token) {
      // 刷新成功：用新 Token 重试该请求
      config.headers['Authorization'] = 'Bearer ' + token
      resolve(axiosInstance(config))
    } else {
      // 刷新失败：该请求连带失败
      reject(new Error('刷新失败，Token 已过期'))
    }
  })
  // 清空队列，释放内存
  failedQueue = []
}

// ---------- 创建 Axios 实例 ----------
const axiosInstance = axios.create({
  baseURL: '',           // 空字符串，由 Vite 代理处理 /api → localhost:8080
  timeout: 15000,
  withCredentials: true, // 保证 HttpOnly Cookie（refresh_token）能在跨域代理下正常发送
})

/**
 * 专用于「刷新」的裸实例：
 * - withCredentials:true —— 携带 HttpOnly 的 refresh_token Cookie（跨域部署下也不丢）
 * - 不挂任何请求拦截器 —— 绝不夹带 Authorization: Bearer AT（避免过期 AT 干扰刷新）
 * - 不挂响应拦截器 —— 刷新自身 401 直接抛出，杜绝递归刷新死循环
 */
const refreshClient = axios.create({
  baseURL: '',
  timeout: 15000,
  withCredentials: true,
})

// ---------- 请求拦截器 ----------
axiosInstance.interceptors.request.use(
  (config) => {
    // 从 Pinia Store 获取当前 Token（已从 localStorage 恢复的状态）
    const authStore = useAuthStore()
    if (authStore.accessToken) {
      config.headers['Authorization'] = 'Bearer ' + authStore.accessToken
    }
    return config
  },
  (error) => Promise.reject(error)
)

// ---------- 响应拦截器 ----------
axiosInstance.interceptors.response.use(
  // 正常响应直接返回
  (response) => response,
  async (error) => {
    const originalRequest = error.config

    // 非 401 错误，或请求本身就不存在，直接抛出
    if (!error.response || error.response.status !== 401 || !originalRequest) {
      return Promise.reject(error)
    }

    // ========== 判断是否应尝试刷新 ==========

    // 1. 登录/注册接口的 401 是凭据校验失败，不是 token 过期，直接透传
    if (PUBLIC_ENDPOINTS.includes(originalRequest.url)) {
      return Promise.reject(error)
    }

    // 2. 刷新接口本身返回 401：说明 RT 已过期，跳登录
    if (originalRequest.url === '/api/auth/refresh') {
      const authStore = useAuthStore()
      authStore.clearToken()
      router.push('/login')
      return Promise.reject(new Error('Refresh Token 已过期，请重新登录'))
    }

    // 如果正在刷新中，将当前请求加入队列，等刷新完成后重试
    if (isRefreshing) {
      return new Promise((resolve, reject) => {
        failedQueue.push({ resolve, reject, config: originalRequest })
      })
    }

    // ---------- 开始刷新 ----------
    isRefreshing = true
    const authStore = useAuthStore()

    try {
      // 调用刷新接口 —— 用 refreshClient（带 withCredentials，浏览器自动携带 HttpOnly Cookie 中的 RT）
      const response = await refreshClient.post('/api/auth/refresh')
      const newToken = response.data.data.access_token

      // 更新 Store 与 localStorage
      authStore.setToken(newToken, response.data.data.expires_in)

      // 用新 Token 重试队列中所有的请求
      processQueue(newToken)

      // 重试当前请求
      originalRequest.headers['Authorization'] = 'Bearer ' + newToken
      return axiosInstance(originalRequest)
    } catch (refreshError) {
      // 刷新失败：RT 也过期或无效，清空 Token 跳登录
      processQueue(null)          // 队列中所有请求都失败
      authStore.clearToken()
      router.push('/login')
      return Promise.reject(refreshError)
    } finally {
      isRefreshing = false
    }
  }
)

export default axiosInstance