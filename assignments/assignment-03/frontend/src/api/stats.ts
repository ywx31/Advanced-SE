import http from './http'
import type { ApiResponse, DashboardData } from '../types'

export async function fetchDashboard() {
  const { data } = await http.get<ApiResponse<DashboardData>>('/stats/dashboard')
  return data.data
}
