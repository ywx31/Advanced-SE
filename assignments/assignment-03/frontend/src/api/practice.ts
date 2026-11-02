import http from './http'
import type {
  ApiResponse,
  PageResult,
  PracticeConfig,
  SessionDetail,
  SessionSummary,
  AnswerItem,
} from '../types'

export async function createSession(config: PracticeConfig) {
  const { data } = await http.post<ApiResponse<SessionDetail>>(
    '/practice/sessions',
    config,
  )
  return data.data
}

export async function getSession(id: number) {
  const { data } = await http.get<ApiResponse<SessionDetail>>(
    `/practice/sessions/${id}`,
  )
  return data.data
}

export async function submitSession(id: number, answers: AnswerItem[]) {
  const { data } = await http.post<ApiResponse<SessionDetail>>(
    `/practice/sessions/${id}/submit`,
    { answers },
  )
  return data.data
}

export async function listSessions(page = 1, size = 10) {
  const { data } = await http.get<ApiResponse<PageResult<SessionSummary>>>(
    '/practice/sessions',
    { params: { page, size } },
  )
  return data.data
}
