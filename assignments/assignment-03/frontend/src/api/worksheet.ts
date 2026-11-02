import http from './http'
import type { ApiResponse, PracticeConfig, WorksheetView } from '../types'

export async function createWorksheet(config: PracticeConfig) {
  const { data } = await http.post<ApiResponse<WorksheetView>>('/practice/worksheets', config)
  return data.data
}
