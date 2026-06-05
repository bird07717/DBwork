import { get, post } from './api'

export interface RouteItem {
  id: number
  routeCode: string
  routeName: string
  direction: string
  startStationName: string
  endStationName: string
  status: number
}

export interface StationItem {
  id: number
  stationCode: string
  stationName: string
  stationOrder: number
  longitude: number
  latitude: number
}

export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  size: number
}

export function defaultRange() {
  return ['2026-05-29', '2026-06-04'] as [string, string]
}

export const api = {
  routes: () => get<RouteItem[]>('/api/public/routes'),
  route: (id: number | string) => get<RouteItem>(`/api/public/routes/${id}`),
  stations: (id: number | string) => get<StationItem[]>(`/api/public/routes/${id}/stations`),
  routeFlow: (routeId: number, startDate: string, endDate: string) =>
    get<Record<string, unknown>[]>(`/api/public/statistics/route-flow?routeId=${routeId}&startDate=${startDate}&endDate=${endDate}`),
  stationFlow: (routeId: number, startDate: string, endDate: string, limit = 10) =>
    get<Record<string, unknown>[]>(
      `/api/public/statistics/station-flow?routeId=${routeId}&startDate=${startDate}&endDate=${endDate}&limit=${limit}`
    ),
  peakAnalysis: (routeId: number, startDate: string, endDate: string) =>
    get<Record<string, unknown>[]>(`/api/public/statistics/peak-analysis?routeId=${routeId}&startDate=${startDate}&endDate=${endDate}`),
  loadRate: (routeId: number, startDate: string, endDate: string) =>
    get<Record<string, unknown>[]>(`/api/public/statistics/load-rate?routeId=${routeId}&startDate=${startDate}&endDate=${endDate}`),
  analyze: (body: unknown) => post<Record<string, unknown>>('/api/public/ai/analyze', body)
}
