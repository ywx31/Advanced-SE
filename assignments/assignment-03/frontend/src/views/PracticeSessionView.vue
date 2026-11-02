<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ArrowLeft,
  ArrowRight,
  CheckCircle2,
  Clock3,
  Flag,
  Keyboard,
  ListChecks,
  Send,
  X,
} from 'lucide-vue-next'
import TypeBadge from '../components/TypeBadge.vue'
import { getSession, submitSession } from '../api/practice'
import type { AnswerItem, SessionDetail } from '../types'

const route = useRoute()
const router = useRouter()
const session = ref<SessionDetail | null>(null)
const answers = ref<Record<number, string>>({})
const currentIndex = ref(0)
const loading = ref(true)
const submitting = ref(false)
const error = ref('')
const confirmSubmit = ref(false)
const answerInput = ref<HTMLInputElement | null>(null)
const elapsed = ref(0)
let timer: number | undefined

onMounted(load)
onBeforeUnmount(() => {
  if (timer) window.clearInterval(timer)
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    const detail = await getSession(Number(route.params.id))
    if (detail.status === 'SUBMITTED') {
      await router.replace(`/result/${detail.id}`)
      return
    }
    session.value = detail
    startTimer()
    await nextTick()
    focusInput()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '加载练习失败'
  } finally {
    loading.value = false
  }
}

function startTimer() {
  if (!session.value) return
  const start = new Date(session.value.startedAt).getTime()
  const update = () => {
    elapsed.value = Math.max(0, Math.floor((Date.now() - start) / 1000))
  }
  update()
  timer = window.setInterval(update, 1000)
}

const currentQuestion = computed(() => session.value?.questions[currentIndex.value] || null)
const answeredCount = computed(() =>
  Object.values(answers.value).filter((value) => value !== '' && value !== undefined).length,
)
const progress = computed(() =>
  session.value ? (answeredCount.value / session.value.totalCount) * 100 : 0,
)
const timeText = computed(() => {
  const minutes = Math.floor(elapsed.value / 60).toString().padStart(2, '0')
  const seconds = (elapsed.value % 60).toString().padStart(2, '0')
  return `${minutes}:${seconds}`
})
const isLast = computed(() => session.value ? currentIndex.value === session.value.questions.length - 1 : false)
const questionFontSize = computed(() => {
  const length = currentQuestion.value?.expression.length || 0
  if (length >= 32) return 'clamp(24px, 3vw, 38px)'
  if (length >= 24) return 'clamp(27px, 4vw, 46px)'
  if (length >= 18) return 'clamp(31px, 5vw, 54px)'
  return 'clamp(36px, 6vw, 62px)'
})

const currentValue = computed({
  get: () => currentQuestion.value ? answers.value[currentQuestion.value.id] ?? '' : '',
  set: (value: string) => {
    if (!currentQuestion.value) return
    answers.value[currentQuestion.value.id] = value.replace(/[^\d-]/g, '')
  },
})

function goTo(index: number) {
  if (!session.value || index < 0 || index >= session.value.questions.length) return
  currentIndex.value = index
  nextTick(focusInput)
}

function next() {
  if (!session.value) return
  if (isLast.value) {
    confirmSubmit.value = true
    return
  }
  goTo(currentIndex.value + 1)
}

function previous() {
  goTo(currentIndex.value - 1)
}

function focusInput() {
  answerInput.value?.focus()
}

function isAnswered(questionId: number) {
  const value = answers.value[questionId]
  return value !== undefined && value !== ''
}

async function doSubmit() {
  if (!session.value) return
  submitting.value = true
  error.value = ''
  try {
    const payload: AnswerItem[] = session.value.questions.map((question) => {
      const raw = answers.value[question.id]
      return {
        questionId: question.id,
        answer: raw === undefined || raw === '' ? null : Number(raw),
      }
    })
    await submitSession(session.value.id, payload)
    confirmSubmit.value = false
    await router.push(`/result/${session.value.id}`)
  } catch (err) {
    error.value = err instanceof Error ? err.message : '提交失败'
    confirmSubmit.value = false
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div v-if="loading" class="loading-panel"><div class="loader" /></div>
  <div v-else-if="error && !session" class="card error-card">
    <div class="alert">{{ error }}</div>
    <button class="btn btn-ghost" @click="router.push('/practice/new')"><ArrowLeft /> 返回配置</button>
  </div>

  <template v-else-if="session && currentQuestion">
    <header class="practice-top">
      <button class="back-link" @click="router.push('/practice/new')"><ArrowLeft /> 退出练习</button>
      <div class="practice-title">
        <span>{{ session.title }}</span>
        <small>{{ session.config.count }} 道题 · {{ session.config.types.length }} 类题型<template v-if="session.config.types.includes('MIXED_OPERATIONS')"> · 混合运算 {{ session.config.operandCount }} 个数</template></small>
      </div>
      <div class="timer"><Clock3 /> {{ timeText }}</div>
    </header>

    <div class="progress-row">
      <div class="progress-track"><div class="progress-fill" :style="{ width: `${progress}%` }" /></div>
      <span>已答 {{ answeredCount }} / {{ session.totalCount }}</span>
    </div>

    <div class="practice-layout">
      <main class="question-stage">
        <div class="question-meta">
          <span class="question-index">第 {{ currentIndex + 1 }} 题</span>
          <TypeBadge :type="currentQuestion.type" />
        </div>

        <div class="question-display serif" :style="{ fontSize: questionFontSize }">{{ currentQuestion.expression }}</div>
        <div class="answer-zone">
          <span class="equal-sign">=</span>
          <input
            ref="answerInput"
            v-model="currentValue"
            class="answer-input serif"
            type="text"
            inputmode="numeric"
            autocomplete="off"
            placeholder="?"
            @keydown.enter.prevent="next"
          />
        </div>
        <p class="keyboard-hint"><Keyboard /> 输入答案后按 Enter 进入下一题</p>

        <div class="question-actions">
          <button class="btn btn-ghost" :disabled="currentIndex === 0" @click="previous"><ArrowLeft /> 上一题</button>
          <button v-if="!isLast" class="btn btn-primary" @click="next">下一题 <ArrowRight /></button>
          <button v-else class="btn btn-dark" @click="confirmSubmit = true"><Send /> 提交练习</button>
        </div>
      </main>

      <aside class="navigator-card">
        <div class="navigator-head">
          <div>
            <h3><ListChecks /> 答题卡</h3>
            <p>点击题号快速跳转</p>
          </div>
          <span>{{ answeredCount }}/{{ session.totalCount }}</span>
        </div>
        <div class="question-grid">
          <button
            v-for="(question, index) in session.questions"
            :key="question.id"
            class="question-dot"
            :class="{ current: index === currentIndex, answered: isAnswered(question.id) }"
            @click="goTo(index)"
          >
            {{ index + 1 }}
          </button>
        </div>
        <div class="legend">
          <span><i class="dot current-dot" /> 当前</span>
          <span><i class="dot answered-dot" /> 已答</span>
          <span><i class="dot empty-dot" /> 未答</span>
        </div>
        <button class="btn btn-dark submit-all" @click="confirmSubmit = true"><Flag /> 检查并提交</button>
      </aside>
    </div>

    <div v-if="confirmSubmit" class="modal-backdrop" @click.self="confirmSubmit = false">
      <div class="confirm-modal">
        <button class="modal-close" @click="confirmSubmit = false"><X /></button>
        <span class="modal-icon"><CheckCircle2 /></span>
        <h2>确认提交练习？</h2>
        <p v-if="answeredCount < session.totalCount">
          还有 <strong>{{ session.totalCount - answeredCount }}</strong> 道题未作答，未答题将计为错误。
        </p>
        <p v-else>全部题目已作答，提交后会立即生成成绩和错题记录。</p>
        <div v-if="error" class="alert">{{ error }}</div>
        <div class="modal-actions">
          <button class="btn btn-ghost" @click="confirmSubmit = false">继续检查</button>
          <button class="btn btn-primary" :disabled="submitting" @click="doSubmit">
            {{ submitting ? '正在提交…' : '确认提交' }}
          </button>
        </div>
      </div>
    </div>
  </template>
</template>

<style scoped>
.error-card { display: grid; max-width: 500px; gap: 20px; margin: 60px auto; padding: 25px; }
.practice-top { display: flex; align-items: center; gap: 18px; margin-bottom: 18px; }
.back-link { display: inline-flex; align-items: center; gap: 6px; border: 0; background: none; color: #64748b; font-size: 12px; cursor: pointer; }
.back-link svg { width: 15px; height: 15px; }
.practice-title { flex: 1; text-align: center; }
.practice-title span { display: block; color: #1e293b; font-family: var(--serif); font-size: 20px; font-weight: 600; }
.practice-title small { display: block; margin-top: 3px; color: #94a3b8; font-size: 10px; }
.timer { display: flex; align-items: center; gap: 6px; padding: 8px 12px; border-radius: 10px; background: #fff; color: #475569; font-size: 12px; font-weight: 700; box-shadow: 0 6px 16px rgba(15,23,42,.06); }
.timer svg { width: 15px; height: 15px; color: #2563eb; }
.progress-row { display: flex; align-items: center; gap: 14px; margin-bottom: 22px; }
.progress-row .progress-track { flex: 1; }
.progress-row > span { min-width: 80px; color: #94a3b8; font-size: 10px; text-align: right; }
.practice-layout { display: grid; grid-template-columns: minmax(0, 1fr) 290px; gap: 20px; align-items: start; }
.question-stage { position: relative; min-height: 530px; padding: 50px clamp(28px, 7vw, 88px); border: 1px solid rgba(148,163,184,.2); border-radius: 24px; background: radial-gradient(circle at 90% 10%, #eff6ff, transparent 26%), #fff; box-shadow: 0 18px 50px rgba(15,23,42,.07); text-align: center; }
.question-meta { display: flex; align-items: center; justify-content: center; gap: 10px; }
.question-index { color: #94a3b8; font-size: 11px; font-weight: 700; letter-spacing: .08em; }
.question-display { margin: 75px 0 40px; color: #172554; font-size: clamp(36px, 6vw, 62px); font-weight: 600; letter-spacing: .02em; overflow-wrap: anywhere; }
.answer-zone { display: flex; align-items: center; justify-content: center; gap: 18px; }
.equal-sign { color: #94a3b8; font-family: var(--serif); font-size: 33px; }
.answer-input { width: min(220px, 50vw); padding: 5px 10px 9px; border: 0; border-bottom: 3px solid #93c5fd; outline: none; background: transparent; color: #2563eb; font-size: 56px; font-weight: 600; text-align: center; }
.answer-input::placeholder { color: #bfdbfe; }
.answer-input:focus { border-color: #2563eb; }
.keyboard-hint { display: flex; align-items: center; justify-content: center; gap: 6px; margin: 16px 0 0; color: #a1aec2; font-size: 10px; }
.keyboard-hint svg { width: 13px; height: 13px; }
.question-actions { display: flex; justify-content: center; gap: 12px; margin-top: 68px; }
.navigator-card { padding: 22px; border: 1px solid rgba(148,163,184,.2); border-radius: 20px; background: #fff; box-shadow: 0 12px 30px rgba(15,23,42,.055); }
.navigator-head { display: flex; align-items: flex-start; justify-content: space-between; }
.navigator-head h3 { display: flex; align-items: center; gap: 7px; margin: 0; color: #334155; font-size: 14px; }
.navigator-head h3 svg { width: 16px; height: 16px; color: #2563eb; }
.navigator-head p { margin: 5px 0 0; color: #94a3b8; font-size: 10px; }
.navigator-head > span { padding: 5px 8px; border-radius: 8px; background: #eff6ff; color: #2563eb; font-size: 10px; font-weight: 700; }
.question-grid { display: grid; grid-template-columns: repeat(5, 1fr); gap: 8px; max-height: 300px; margin: 20px 0; overflow-y: auto; }
.question-dot { display: grid; aspect-ratio: 1; place-items: center; border: 1px solid #e2e8f0; border-radius: 9px; background: #fff; color: #94a3b8; font-size: 11px; cursor: pointer; }
.question-dot.answered { border-color: #bfdbfe; background: #eff6ff; color: #2563eb; font-weight: 700; }
.question-dot.current { border-color: #172554; background: #172554; color: #fff; box-shadow: 0 4px 10px rgba(23,37,84,.2); }
.legend { display: flex; flex-wrap: wrap; gap: 13px; margin-bottom: 18px; color: #94a3b8; font-size: 9px; }
.legend span { display: flex; align-items: center; gap: 4px; }
.dot { display: inline-block; width: 8px; height: 8px; border-radius: 50%; }
.current-dot { background: #172554; }
.answered-dot { background: #93c5fd; }
.empty-dot { border: 1px solid #cbd5e1; background: #fff; }
.submit-all { width: 100%; }
.modal-backdrop { position: fixed; inset: 0; z-index: 60; display: grid; place-items: center; padding: 20px; background: rgba(15,23,42,.56); backdrop-filter: blur(5px); }
.confirm-modal { position: relative; width: min(440px, 100%); padding: 32px; border-radius: 22px; background: #fff; box-shadow: 0 30px 80px rgba(15,23,42,.28); text-align: center; }
.modal-close { position: absolute; top: 14px; right: 14px; display: grid; width: 32px; height: 32px; place-items: center; border: 0; border-radius: 9px; background: #f8fafc; color: #94a3b8; cursor: pointer; }
.modal-close svg { width: 17px; height: 17px; }
.modal-icon { display: grid; width: 58px; height: 58px; margin: 0 auto 18px; place-items: center; border-radius: 18px; background: #ecfdf5; color: #059669; }
.modal-icon svg { width: 28px; height: 28px; }
.confirm-modal h2 { margin: 0; color: #172554; font-family: var(--serif); font-size: 24px; }
.confirm-modal p { margin: 13px 0 22px; color: #64748b; font-size: 13px; line-height: 1.7; }
.modal-actions { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; margin-top: 20px; }
@media (max-width: 900px) {
  .practice-layout { grid-template-columns: 1fr; }
  .navigator-card { order: -1; }
  .question-grid { grid-template-columns: repeat(10, 1fr); max-height: 120px; }
  .question-dot { aspect-ratio: auto; height: 30px; }
  .legend, .submit-all { display: none; }
  .question-stage { min-height: 480px; }
}
@media (max-width: 600px) {
  .practice-title small, .keyboard-hint { display: none; }
  .practice-title span { font-size: 16px; }
  .question-stage { padding: 36px 20px; }
  .question-display { margin: 60px 0 35px; font-size: 38px; }
  .question-actions { margin-top: 55px; }
  .question-grid { grid-template-columns: repeat(8, 1fr); }
  .modal-actions { grid-template-columns: 1fr; }
}
</style>
