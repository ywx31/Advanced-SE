import http from './http'
import type { ApiResponse, SessionDetail, WeeklyReport } from '../types'

export async function fetchLatestWeeklyReport() {
  const { data } = await http.get<ApiResponse<WeeklyReport>>('/reports/weekly/latest')
  return data.data
}

export async function fetchWeeklyReport(weekStart: string) {
  const { data } = await http.get<ApiResponse<WeeklyReport>>('/reports/weekly', {
    params: { weekStart },
  })
  return data.data
}

export async function regenerateWeeklyReport(weekStart?: string) {
  const { data } = await http.post<ApiResponse<WeeklyReport>>('/reports/weekly/generate', null, {
    params: weekStart ? { weekStart } : undefined,
  })
  return data.data
}

export async function markWeeklyReportRead(id: number) {
  const { data } = await http.post<ApiResponse<WeeklyReport>>(`/reports/weekly/${id}/read`)
  return data.data
}

export async function createWeeklyRecommendedPractice(id: number) {
  const { data } = await http.post<ApiResponse<SessionDetail>>(
    `/reports/weekly/${id}/recommended-practice`,
  )
  return data.data
}
