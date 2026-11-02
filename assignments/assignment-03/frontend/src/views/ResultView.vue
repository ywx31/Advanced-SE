<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ArrowLeft,
  CheckCircle2,
  Clock3,
  Download,
  RotateCcw,
  Target,
  XCircle,
} from 'lucide-vue-next'
import html2canvas from 'html2canvas'
import { jsPDF } from 'jspdf'
import TypeBadge from '../components/TypeBadge.vue'
import { getSession } from '../api/practice'
import type { SessionDetail } from '../types'
import { QUESTION_TYPE_LABELS } from '../types'

const route = useRoute()
const router = useRouter()
const session = ref<SessionDetail | null>(null)
const loading = ref(true)
const error = ref('')
const filter = ref<'all' | 'wrong'>('all')
const pdfWithAnswers = ref(true)
const exporting = ref(false)
const pdfSheet = ref<HTMLElement | null>(null)

onMounted(load)

async function load() {
  loading.value = true
  try {
    const detail = await getSession(Number(route.params.id))
    if (detail.status !== 'SUBMITTED') {
      await router.replace(`/practice/${detail.id}`)
      return
    }
    session.value = detail
  } catch (err) {
    error.value = err instanceof Error ? err.message : '加载结果失败'
  } finally {
    loading.value = false
  }
}

const wrongQuestions = computed(() => session.value?.questions.filter((item) => !item.correct) || [])
const visibleQuestions = computed(() =>
  filter.value === 'wrong' ? wrongQuestions.value : session.value?.questions || [],
)
const durationText = computed(() => {
  const seconds = session.value?.durationSeconds || 0
  const minutes = Math.floor(seconds / 60)
  const rest = seconds % 60
  return minutes ? `${minutes} 分 ${rest} 秒` : `${rest} 秒`
})
const scoreMessage = computed(() => {
  const accuracy = Number(session.value?.accuracy || 0)
  if (accuracy >= 95) return { title: '非常出色！', text: '掌握得很扎实，可以挑战更高难度了。' }
  if (accuracy >= 80) return { title: '表现不错！', text: '基础已经比较稳固，再关注一下错题即可。' }
  if (accuracy >= 60) return { title: '继续加油！', text: '建议回顾错题，再进行一次同类练习。' }
  return { title: '别着急，慢慢来', text: '先复习错题本，把每一步运算弄明白。' }
})

async function downloadPdf(withAnswers: boolean) {
  if (!session.value || !pdfSheet.value || exporting.value) return
  exporting.value = true
  pdfWithAnswers.value = withAnswers
  await nextTick()
  const element = pdfSheet.value.cloneNode(true) as HTMLElement
  element.style.position = 'fixed'
  element.style.top = '0'
  element.style.left = '0'
  element.style.zIndex = '99998'
  element.style.pointerEvents = 'none'
  element.style.transform = 'none'
  element.style.background = '#ffffff'
  document.body.appendChild(element)
  try {
    await new Promise((resolve) => requestAnimationFrame(() => resolve(undefined)))
    const safeTitle = session.value.title.replace(/[\\/:*?"<>|]/g, '_')
    const canvas = await html2canvas(element, {
      scale: 2,
      useCORS: true,
      backgroundColor: '#ffffff',
      logging: false,
      width: element.scrollWidth,
      height: element.scrollHeight,
      windowWidth: element.scrollWidth,
      windowHeight: element.scrollHeight,
      scrollX: 0,
      scrollY: 0,
    })
    if (!canvas.width || !canvas.height) {
      throw new Error('PDF 内容渲染失败，请重试')
    }

    const pdf = new jsPDF({ unit: 'mm', format: 'a4', orientation: 'portrait' })
    const pageWidth = 210
    const pageHeight = 297
    const margin = 10
    const usableWidth = pageWidth - margin * 2
    const usableHeight = pageHeight - margin * 2
    const sliceHeight = Math.floor(canvas.width * usableHeight / usableWidth)
    let offset = 0
    let pageIndex = 0

    while (offset < canvas.height) {
      const chunkHeight = Math.min(sliceHeight, canvas.height - offset)
      const pageCanvas = document.createElement('canvas')
      pageCanvas.width = canvas.width
      pageCanvas.height = chunkHeight
      const context = pageCanvas.getContext('2d')
      if (!context) throw new Error('无法创建 PDF 画布')
      context.fillStyle = '#ffffff'
      context.fillRect(0, 0, pageCanvas.width, pageCanvas.height)
      context.drawImage(canvas, 0, offset, canvas.width, chunkHeight, 0, 0, canvas.width, chunkHeight)
      if (pageIndex > 0) pdf.addPage()
      pdf.addImage(
        pageCanvas.toDataURL('image/jpeg', 0.95),
        'JPEG',
        margin,
        margin,
        usableWidth,
        chunkHeight * usableWidth / canvas.width,
      )
      offset += chunkHeight
      pageIndex += 1
    }
    pdf.save(`${safeTitle}_${withAnswers ? '含答案' : '练习题'}.pdf`)
  } finally {
    element.remove()
    exporting.value = false
  }
}
</script>

<template>
  <div v-if="loading" class="loading-panel"><div class="loader" /></div>
  <div v-else-if="error" class="card result-error"><div class="alert">{{ error }}</div></div>

  <template v-else-if="session">
    <header class="result-header">
      <button class="back-link" @click="router.push('/history')"><ArrowLeft /> 练习记录</button>
      <div class="result-heading">
        <span>PRACTICE RESULT</span>
        <h1>{{ session.title }}</h1>
        <p>{{ new Date(session.submittedAt || session.startedAt).toLocaleString('zh-CN') }}</p>
      </div>
    </header>

    <section class="score-hero">
      <div class="score-ring" :style="{ '--score': `${Number(session.accuracy)}%` }">
        <div class="score-inner">
          <strong>{{ Number(session.accuracy).toFixed(0) }}</strong>
          <span>正确率</span>
        </div>
      </div>
      <div class="score-copy">
        <span class="score-eyebrow">SCORE SUMMARY</span>
        <h2>{{ scoreMessage.title }}</h2>
        <p>{{ scoreMessage.text }}</p>
        <div class="score-metrics">
          <div><CheckCircle2 /><span><strong>{{ session.correctCount }}</strong>答对</span></div>
          <div><XCircle /><span><strong>{{ session.totalCount - session.correctCount }}</strong>答错</span></div>
          <div><Clock3 /><span><strong>{{ durationText }}</strong>用时</span></div>
        </div>
      </div>
      <div class="hero-actions">
        <button class="btn btn-ghost" :disabled="exporting" @click="downloadPdf(false)"><Download /> 下载练习题</button>
        <button class="btn btn-dark" :disabled="exporting" @click="downloadPdf(true)"><Download /> {{ exporting ? '生成中…' : '下载含答案' }}</button>
        <RouterLink to="/practice/new" class="btn btn-primary"><RotateCcw /> 再练一组</RouterLink>
      </div>
    </section>

    <section class="review-section">
      <div class="review-toolbar">
        <div>
          <h2>逐题回顾</h2>
          <p>对照答案检查每一步运算</p>
        </div>
        <div class="filter-tabs">
          <button :class="{ active: filter === 'all' }" @click="filter = 'all'">全部 {{ session.totalCount }}</button>
          <button :class="{ active: filter === 'wrong' }" @click="filter = 'wrong'">错题 {{ wrongQuestions.length }}</button>
        </div>
      </div>

      <div class="question-review-grid">
        <article
          v-for="question in visibleQuestions"
          :key="question.id"
          class="review-card"
          :class="question.correct ? 'correct' : 'wrong'"
        >
          <div class="review-top">
            <span class="review-number">第 {{ question.number }} 题</span>
            <TypeBadge :type="question.type" />
            <span class="result-mark" :class="question.correct ? 'mark-correct' : 'mark-wrong'">
              <CheckCircle2 v-if="question.correct" />
              <XCircle v-else />
            </span>
          </div>
          <div class="review-expression serif">{{ question.expression }} = ?</div>
          <div class="answer-compare">
            <span>你的答案 <strong :class="question.correct ? 'text-green' : 'text-rose'">{{ question.userAnswer ?? '未作答' }}</strong></span>
            <span v-if="!question.correct">正确答案 <strong class="text-green">{{ question.correctAnswer }}</strong></span>
          </div>
        </article>
      </div>
      <div v-if="!visibleQuestions.length" class="all-correct">
        <CheckCircle2 />
        <strong>全部答对，太棒了！</strong>
        <p>这一组没有错题。</p>
      </div>
    </section>

    <div ref="pdfSheet" class="pdf-sheet">
      <header class="pdf-header">
        <div>
          <h1>{{ session.title }}</h1>
          <p>MathMate 智能口算练习 · {{ new Date(session.submittedAt || session.startedAt).toLocaleDateString('zh-CN') }}</p>
        </div>
        <div class="pdf-meta">
          共 {{ session.totalCount }} 题<br />
          {{ QUESTION_TYPE_LABELS[session.config.types[0]] }}{{ session.config.types.length > 1 ? ` 等 ${session.config.types.length} 类` : '' }}
        </div>
      </header>

      <div class="pdf-info">
        <span>姓名：________________</span>
        <span>日期：________________</span>
        <span v-if="pdfWithAnswers">正确率：{{ Number(session.accuracy).toFixed(1) }}%</span>
        <span v-else>用时：______ 分 ______ 秒</span>
      </div>

      <div class="pdf-questions">
        <div v-for="question in session.questions" :key="`pdf-${question.id}`" class="pdf-question">
          <span class="pdf-type">{{ QUESTION_TYPE_LABELS[question.type] }}</span>
          <span>第{{ question.number }}题：</span>
          <strong>{{ question.expression }} =</strong>
          <span class="pdf-answer">{{ pdfWithAnswers ? question.correctAnswer : '____________' }}</span>
        </div>
      </div>

      <footer class="pdf-footer">MathMate · 让每一道题都看得见成长</footer>
    </div>

    <div v-if="exporting" class="pdf-overlay">
      <div class="pdf-export-card">
        <div class="loader" />
        <strong>正在生成 PDF</strong>
        <span>排版与字体处理中，请稍候…</span>
      </div>
    </div>
  </template>
</template>

<style scoped>
.result-error { max-width: 600px; margin: 60px auto; padding: 24px; }
.result-header { display: flex; align-items: flex-start; gap: 10px; margin-bottom: 22px; }
.back-link { display: inline-flex; min-height: 36px; align-items: center; gap: 6px; padding: 0 8px; border: 0; background: none; color: #64748b; font-size: 12px; cursor: pointer; }
.back-link svg { width: 15px; height: 15px; }
.result-heading { flex: 1; text-align: center; padding-right: 80px; }
.result-heading > span { color: #2563eb; font-size: 9px; font-weight: 800; letter-spacing: .18em; }
.result-heading h1 { margin: 7px 0 4px; color: #172554; font-family: var(--serif); font-size: clamp(28px, 4vw, 38px); font-weight: 600; letter-spacing: -.03em; }
.result-heading p { margin: 0; color: #94a3b8; font-size: 10px; }
.score-hero { display: grid; grid-template-columns: 210px minmax(0, 1fr) auto; gap: 30px; align-items: center; padding: 32px; border: 1px solid rgba(148,163,184,.2); border-radius: 24px; background: radial-gradient(circle at 85% 20%, #dbeafe, transparent 28%), linear-gradient(145deg, #fff, #f8fbff); box-shadow: 0 16px 42px rgba(15,23,42,.07); }
.score-ring { display: grid; width: 175px; height: 175px; place-items: center; border-radius: 50%; background: conic-gradient(#2563eb var(--score), #e2e8f0 0); }
.score-inner { display: grid; width: 143px; height: 143px; place-items: center; align-content: center; border-radius: 50%; background: #fff; box-shadow: inset 0 0 0 1px #eef2f7; }
.score-inner strong { color: #172554; font-size: 51px; line-height: 1; letter-spacing: -.06em; }
.score-inner span { margin-top: 7px; color: #94a3b8; font-size: 10px; }
.score-eyebrow { color: #2563eb; font-size: 9px; font-weight: 800; letter-spacing: .16em; }
.score-copy h2 { margin: 9px 0 9px; color: #172554; font-family: var(--serif); font-size: 29px; font-weight: 600; }
.score-copy > p { margin: 0; color: #64748b; font-size: 13px; line-height: 1.7; }
.score-metrics { display: flex; flex-wrap: wrap; gap: 20px; margin-top: 22px; }
.score-metrics > div { display: flex; align-items: center; gap: 8px; color: #94a3b8; }
.score-metrics svg { width: 18px; height: 18px; color: #2563eb; }
.score-metrics span { display: grid; gap: 2px; font-size: 10px; }
.score-metrics strong { color: #334155; font-size: 14px; }
.hero-actions { display: grid; gap: 9px; min-width: 155px; }
.review-section { margin-top: 26px; }
.review-toolbar { display: flex; align-items: flex-end; justify-content: space-between; gap: 20px; margin-bottom: 16px; }
.review-toolbar h2 { margin: 0; color: #172554; font-size: 20px; }
.review-toolbar p { margin: 5px 0 0; color: #94a3b8; font-size: 11px; }
.filter-tabs { display: flex; padding: 4px; border-radius: 11px; background: #eef2f7; }
.filter-tabs button { padding: 8px 14px; border: 0; border-radius: 8px; background: transparent; color: #64748b; font-size: 11px; cursor: pointer; }
.filter-tabs button.active { background: #fff; color: #172554; font-weight: 700; box-shadow: 0 3px 8px rgba(15,23,42,.08); }
.question-review-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; }
.review-card { padding: 18px; border: 1px solid #e2e8f0; border-radius: 16px; background: #fff; }
.review-card.wrong { border-color: #fecdd3; background: #fffafb; }
.review-top { display: flex; align-items: center; gap: 8px; }
.review-number { color: #94a3b8; font-size: 10px; font-weight: 700; }
.result-mark { margin-left: auto; }
.result-mark svg { width: 18px; height: 18px; }
.mark-correct { color: #059669; }
.mark-wrong { color: #e11d48; }
.review-expression { margin: 21px 0; color: #172554; font-size: 26px; font-weight: 600; }
.answer-compare { display: flex; gap: 24px; color: #94a3b8; font-size: 11px; }
.answer-compare strong { margin-left: 4px; font-size: 13px; }
.all-correct { display: grid; justify-items: center; padding: 60px 20px; color: #64748b; text-align: center; }
.all-correct svg { width: 48px; height: 48px; color: #10b981; }
.all-correct strong { margin-top: 12px; color: #172554; font-size: 20px; }
.all-correct p { margin: 6px 0 0; font-size: 12px; }
.pdf-sheet { position: fixed; top: 0; left: -10000px; width: 794px; padding: 40px 44px; background: #fff; color: #1e3a8a; font-family: var(--serif); }
.pdf-header { display: flex; align-items: flex-start; justify-content: space-between; padding-bottom: 18px; border-bottom: 2px solid #2563eb; }
.pdf-header h1 { margin: 0; font-size: 27px; letter-spacing: .04em; }
.pdf-header p { margin: 7px 0 0; color: #64748b; font-size: 11px; }
.pdf-meta { color: #475569; font-size: 11px; line-height: 1.8; text-align: right; }
.pdf-info { display: flex; justify-content: space-between; margin: 20px 0 24px; color: #475569; font-size: 12px; }
.pdf-questions { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); column-gap: 26px; }
.pdf-question { padding: 10px 0; border-bottom: 1px dashed #dbeafe; color: #1e3a8a; font-size: 15px; line-height: 1.5; break-inside: avoid; }
.pdf-type { display: inline-block; margin-right: 6px; padding: 1px 5px; border-radius: 6px; background: #eff6ff; color: #2563eb; font-family: var(--sans); font-size: 8px; font-weight: 700; vertical-align: 1px; }
.pdf-question strong { font-weight: 600; }
.pdf-answer { color: #0f766e; font-weight: 700; }
.pdf-footer { margin-top: 32px; padding-top: 12px; border-top: 1px solid #dbeafe; color: #94a3b8; font-size: 9px; text-align: center; }
.pdf-overlay { position: fixed; inset: 0; z-index: 99999; display: grid; place-items: center; background: rgba(15,23,42,.62); backdrop-filter: blur(5px); }
.pdf-export-card { display: grid; justify-items: center; gap: 10px; min-width: 260px; padding: 30px; border-radius: 20px; background: #fff; box-shadow: 0 30px 80px rgba(15,23,42,.28); text-align: center; }
.pdf-export-card .loader { width: 32px; height: 32px; border: 3px solid #dbeafe; border-top-color: #2563eb; border-radius: 50%; animation: spin .8s linear infinite; }
.pdf-export-card strong { color: #172554; font-size: 15px; }
.pdf-export-card span { color: #94a3b8; font-size: 11px; }
@media (max-width: 1100px) {
  .score-hero { grid-template-columns: 175px 1fr; }
  .hero-actions { grid-column: 1 / -1; grid-template-columns: repeat(3, 1fr); }
}
@media (max-width: 700px) {
  .result-heading { padding-right: 0; }
  .score-hero { grid-template-columns: 1fr; justify-items: center; text-align: center; }
  .score-metrics { justify-content: center; }
  .hero-actions { width: 100%; grid-template-columns: 1fr; }
  .question-review-grid { grid-template-columns: 1fr; }
  .review-toolbar { align-items: flex-start; flex-direction: column; }
}
</style>
