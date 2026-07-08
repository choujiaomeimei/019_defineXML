import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from './router'

const service = axios.create({
  timeout: 30000
})

const exportService = axios.create({
  timeout: 120000
})

function attachToken(config) {
  const userStr = localStorage.getItem('user')
  if (userStr) {
    try {
      const user = JSON.parse(userStr)
      if (user.token) {
        config.headers['Authorization'] = `Bearer ${user.token}`
      }
    } catch (e) { /* ignore parse errors */ }
  }
  return config
}

function handleUnauthorized(error) {
  if (error.response?.status === 401) {
    localStorage.removeItem('user')
    ElMessage.error('登录已过期，请重新登录')
    router.push('/login')
  }
  return Promise.reject(error)
}

service.interceptors.request.use(
  config => {
    config = attachToken(config)
    if (config.method === 'post' || config.method === 'put') {
      config.headers['Content-Type'] = config.headers['Content-Type'] || 'application/json;charset=UTF-8'
    }
    return config
  },
  error => Promise.reject(error)
)

service.interceptors.response.use(
  response => response,
  error => {
    console.error('请求错误:', error.config?.url, error.message)
    if (error.response?.status === 401) {
      return handleUnauthorized(error)
    }
    ElMessage.error(error.response?.data?.message || '请求失败')
    return Promise.reject(error)
  }
)

exportService.interceptors.request.use(
  config => attachToken(config),
  error => Promise.reject(error)
)

exportService.interceptors.response.use(
  response => response,
  error => {
    if (error.response?.status === 401) {
      return handleUnauthorized(error)
    }
    ElMessage.error(error.response?.data?.message || '导出失败')
    return Promise.reject(error)
  }
)

export default service
export { exportService }
