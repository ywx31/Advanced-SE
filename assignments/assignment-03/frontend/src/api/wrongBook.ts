import http from './http'
import type {
  ApiResponse,
  PageResult,
  SessionDetail,
  WrongQuestion,
} from '../types'

export type WrongPracticeMode = 'ORIGINAL' | 'SIMILAR'

export async function listWrongQuestions(
  mastered: boolean | null,
  page = 1,
  size = 12,
) {
  const { data } = await http.get<ApiResponse<PageResult<WrongQuestion>>>(
    '/wrong-book',
    { params: { mastered, page, size } },
  )
  return data.data
}

export async function setMastered(id: number, mastered: boolean) {
  const { data } = await http.patch<ApiResponse<WrongQuestion>>(
    `/wrong-book/${id}/mastered`,
    { mastered },
  )
  return data.data
}

export async function createWrongPractice(
  wrongQuestionIds: number[],
  mode: WrongPracticeMode,
  count: number,
) {
  const { data } = await http.post<ApiResponse<SessionDetail>>(
    '/wrong-book/practice',
    { wrongQuestionIds, mode, count },
  )
  return data.data
}
