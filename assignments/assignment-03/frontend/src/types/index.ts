export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}

export interface PageResult<T> {
  items: T[]
  total: number
  page: number
  size: number
}

export interface UserProfile {
  id: number
  username: string
  displayName: string
  avatarUrl?: string | null
}

export interface AccountProfile {
  id: number
  username: string
  displayName: string
  avatarUrl?: string | null
  createdAt: string
}


export interface AuthResponse {
  token: string
  user: UserProfile
}

export type QuestionType =
  | 'TABLE_MULTIPLICATION'
  | 'TABLE_DIVISION'
  | 'ADVANCED_MULTIPLICATION'
  | 'ADVANCED_DIVISION'
  | 'MIXED_OPERATIONS'

export interface PracticeConfig {
  title: string
  count: number
  types: QuestionType[]
  operandCount: number
  allowParentheses: boolean
  maxNumber: number
  maxFactor: number
}

export interface QuestionView {
  id: number
  number: number
  expression: string
  type: QuestionType
  userAnswer: number | null
  correctAnswer: number | null
  correct: boolean | null
}

export interface SessionSummary {
  id: number
  title: string
  totalCount: number
  correctCount: number
  accuracy: number
  durationSeconds: number
  status: 'IN_PROGRESS' | 'SUBMITTED'
  startedAt: string
  submittedAt: string | null
  types: QuestionType[]
}

export interface SessionDetail {
  id: number
  title: string
  config: PracticeConfig
  totalCount: number
  correctCount: number
  accuracy: number
  durationSeconds: number
  status: 'IN_PROGRESS' | 'SUBMITTED'
  startedAt: string
  submittedAt: string | null
  questions: QuestionView[]
}

export interface AnswerItem {
  questionId: number
  answer: number | null
}

export interface OverviewStats {
  totalSessions: number
  totalQuestions: number
  correctQuestions: number
  averageAccuracy: number
  totalSeconds: number
  activeWrongQuestions: number
}

export interface TrendPoint {
  sessionId: number
  label: string
  accuracy: number
  correctCount: number
  totalCount: number
}

export interface TypeAccuracy {
  type: QuestionType
  label: string
  total: number
  correct: number
  accuracy: number
}

export interface DailyPoint {
  date: string
  sessionCount: number
  questionCount: number
  accuracy: number
}

export interface ActivityDay {
  date: string
  sessionCount: number
  questionCount: number
  accuracy: number
}

export interface DashboardData {
  overview: OverviewStats
  trend: TrendPoint[]
  typeAccuracy: TypeAccuracy[]
  daily: DailyPoint[]
  activity: ActivityDay[]
  recentSessions: SessionSummary[]
  streakDays: number
}

export interface WrongQuestion {
  id: number
  expression: string
  questionType: QuestionType
  correctAnswer: number
  lastWrongAnswer: number | null
  wrongCount: number
  correctStreak: number
  mastered: boolean
  lastPracticedAt: string
}

export const QUESTION_TYPE_LABELS: Record<QuestionType, string> = {
  TABLE_MULTIPLICATION: '表内乘法',
  TABLE_DIVISION: '表内除法',
  ADVANCED_MULTIPLICATION: '表外乘法',
  ADVANCED_DIVISION: '表外除法',
  MIXED_OPERATIONS: '混合运算',
}

export interface WeeklyOverview {
  sessionCount: number
  questionCount: number
  correctCount: number
  accuracy: number
  previousAccuracy: number
  accuracyDelta: number
  studyMinutes: number
}

export interface WeeklyTypeStat {
  type: QuestionType
  label: string
  total: number
  correct: number
  accuracy: number
}

export interface WeeklyMistake {
  expression: string
  type: QuestionType
  typeLabel: string
  errorCount: number
  correctAnswer: number
}

export interface WeeklyDailyPoint {
  date: string
  questionCount: number
  accuracy: number
}

export interface RecommendedPractice {
  config: PracticeConfig
  typeQuotas: Partial<Record<QuestionType, number>>
  reason: string
}

export interface WeeklyFacts {
  weekStart: string
  weekEnd: string
  overview: WeeklyOverview
  weakTypes: WeeklyTypeStat[]
  frequentMistakes: WeeklyMistake[]
  daily: WeeklyDailyPoint[]
  streakDays: number
  recommendation: RecommendedPractice
}

export interface WeeklyNarrative {
  summary: string
  highlights: string[]
  suggestions: string[]
  source: 'AGENT' | 'TEMPLATE'
  model: string | null
  moderationStatus: string
}

export interface WeeklyReport {
  id: number
  facts: WeeklyFacts
  narrative: WeeklyNarrative
  generatedAt: string
  read: boolean
}

export interface WorksheetQuestion {
  number: number
  expression: string
  type: QuestionType
  answer: number
}

export interface WorksheetView {
  title: string
  config: PracticeConfig
  generatedAt: string
  totalCount: number
  questions: WorksheetQuestion[]
}
