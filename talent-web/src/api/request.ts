import axios from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResult } from './types'

/**
 * axios 实例。
 *
 * baseURL 写 /api，开发环境下由 vite.config.ts 里的 proxy 转发到网关 9090，
 * 所以前端代码里只写 '/employee/page'，不用关心后端地址，也没有跨域问题。
 *
 * 后端统一返回 { code, message, data }，这里统一拆包：
 * 成功直接把 data 交给调用方，失败弹提示并抛错，页面里不用每处都判断 code。
 */
const instance = axios.create({
  baseURL: '/api',
  timeout: 15000,
})

instance.interceptors.response.use(
  (response) => {
    const body = response.data as ApiResult<unknown>
    if (body && typeof body.code === 'number') {
      if (body.code === 200) {
        return body.data as never
      }
      ElMessage.error(body.message || '请求失败')
      return Promise.reject(new Error(body.message || '请求失败'))
    }
    return response.data
  },
  (error) => {
    const message = error?.response
      ? `请求失败（HTTP ${error.response.status}）`
      : '连不上后端服务，确认网关 9090 和 Nacos 已启动'
    ElMessage.error(message)
    return Promise.reject(error)
  },
)

export function get<T>(url: string, params?: Record<string, unknown>): Promise<T> {
  return instance.get(url, { params }) as unknown as Promise<T>
}

export function post<T>(url: string, data?: unknown): Promise<T> {
  return instance.post(url, data) as unknown as Promise<T>
}

export function put<T>(url: string, data?: unknown): Promise<T> {
  return instance.put(url, data) as unknown as Promise<T>
}

export function del<T>(url: string): Promise<T> {
  return instance.delete(url) as unknown as Promise<T>
}
