import axios from 'axios'
import { ElMessage } from 'element-plus'
import { clearSession } from '../auth/session'
import { ApiError } from './errors'

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

      const error = new ApiError(body.msg || '请求失败', {
        status: response.status,
        code: body.code,
        data: body.data,
      })
      return Promise.reject(error)
    }

    return body
  },
  (error) => {
    if (error.response?.status === 401) clearSession()
    const message = error.response?.data?.msg || error.message || '网络请求失败'
    ElMessage.error(message)
    if (error.response?.data?.code != null) {
      return Promise.reject(new ApiError(message, {
        status: error.response.status,
        code: error.response.data.code,
        data: error.response.data.data,
      }))
    }
    return Promise.reject(error)
  },
)

export default http
