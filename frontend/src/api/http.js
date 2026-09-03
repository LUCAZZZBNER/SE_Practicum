import axios from 'axios'
import { ElMessage } from 'element-plus'

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1',
  timeout: 10000,
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('access_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

http.interceptors.response.use(
  (response) => {
    const body = response.data

    if (body && typeof body === 'object' && 'code' in body) {
      if (body.code === 0 || body.code === '0' || body.code === 'success') {
        return body.data
      }

      return Promise.reject(new Error(body.msg || '请求失败'))
    }

    return body
  },
  (error) => {
    const message = error.response?.data?.msg || error.message || '网络请求失败'
    ElMessage.error(message)
    return Promise.reject(error)
  },
)

export default http
