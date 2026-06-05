import { clearToken, getToken } from './auth'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
  timestamp: string
}

export async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const headers = new Headers(options.headers)
  if (!headers.has('Content-Type') && options.body) {
    headers.set('Content-Type', 'application/json')
  }
  const token = getToken()
  if (token) {
    headers.set('satoken', token)
  }
  const response = await fetch(`${API_BASE_URL}${path}`, { ...options, headers })
  const payload = (await response.json()) as ApiResponse<T>
  if (payload.code === 401) {
    clearToken()
    if (window.location.pathname.startsWith('/admin') && !window.location.pathname.includes('/admin/login')) {
      window.location.href = '/admin/login'
    }
  }
  if (payload.code !== 200) {
    throw new Error(payload.message || '请求失败')
  }
  return payload.data
}

export function get<T>(path: string) {
  return request<T>(path)
}

export function post<T>(path: string, body?: unknown) {
  return request<T>(path, { method: 'POST', body: body ? JSON.stringify(body) : undefined })
}

export function put<T>(path: string, body?: unknown) {
  return request<T>(path, { method: 'PUT', body: body ? JSON.stringify(body) : undefined })
}

export function del<T>(path: string) {
  return request<T>(path, { method: 'DELETE' })
}
