<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, ref } from 'vue'
import { useRouter } from 'vue-router'
import html2canvas from 'html2canvas'
import { jsPDF } from 'jspdf'
import {
  Braces,
  Check,
  Divide,
  Download,
  Eye,
  FileText,
  Gauge,
  Infinity as InfinityIcon,
  Info,
  Layers3,
  Play,
  RotateCcw,
  Sigma,
  Sparkles,
  X,
} from 'lucide-vue-next'
import PageHeader from '../components/PageHeader.vue'
import { createSession } from '../api/practice'
import { createWorksheet } from '../api/worksheet'
import type { PracticeConfig, QuestionType, WorksheetView } from '../types'

const router = useRouter()
const title = ref('今日数学练习')
const count = ref(20)
const selectedTypes = ref<QuestionType[]>(['TABLE_MULTIPLICATION', 'TABLE_DIVISION'])
const operandCount = ref(3)
const allowParentheses = ref(false)
const maxNumber = ref(100)
const maxFactor = ref(20)
const starting = ref(false)
const exporting = ref(false)
const printMode = ref<'blank' | 'answers'>('blank')
const worksheet = ref<WorksheetView | null>(null)
const pdfSheet = ref<HTMLElement | null>(null)
const error = ref('')
const controlNotice = ref('')
let noticeTimer: number | undefined

onBeforeUnmount(() => {
  if (noticeTimer) window.clearTimeout(noticeTimer)
})

function notifyMixedFeature() {
  controlNotice.value = '该功能仅适用于“混合运算”。请先选择题型中的“混合运算”，再调整运算数或启用括号。'
  if (noticeTimer) window.clearTimeout(noticeTimer)
  noticeTimer = window.setTimeout(() => { controlNotice.value = '' }, 3600)
}

function toggleParentheses() {
  if (!supportsParentheses.value) {
    notifyMixedFeature()
    return
  }
  allowParentheses.value = !allowParentheses.value
}

const typeOptions: Array<{
  type: QuestionType
  title: string
  description: string
  example: string
  icon: typeof X
  tone: string
}> = [
  { type: 'TABLE_MULTIPLICATION', title: '表内乘法', description: '两个 1～9 的因数，夯实乘法口诀。', example: '8 × 7', icon: X, tone: 'blue' },
  { type: 'TABLE_DIVISION', title: '表内除法', description: '除数和商均在乘法口诀范围内。', example: '56 ÷ 8', icon: Divide, tone: 'green' },
  { type: 'ADVANCED_MULTIPLICATION', title: '表外乘法', description: '至少一个因数超过 9，并严控结果范围。', example: '14 × 6', icon: Layers3, tone: 'amber' },
  { type: 'ADVANCED_DIVISION', title: '表外除法', description: '表外除数或商，答案保持整数。', example: '96 ÷ 12', icon: InfinityIcon, tone: 'rose' },
  { type: 'MIXED_OPERATIONS', title: '混合运算', description: '2～6 个数、最多 5 个运算符，可加入括号。', example: '(18 + 6) ÷ 3', icon: Sigma, tone: 'violet' },
]

const selectedCount = computed(() => selectedTypes.value.length)
const supportsParentheses = computed(() => selectedTypes.value.includes('MIXED_OPERATIONS'))
const estimatedQuestions = computed(() => {
  const perType = {
    TABLE_MULTIPLICATION: 45,
    TABLE_DIVISION: 81,
    ADVANCED_MULTIPLICATION: Math.max(0, maxFactor.value - 9) * maxFactor.value,
    ADVANCED_DIVISION: Math.max(0, maxFactor.value - 9) * maxFactor.value,
    MIXED_OPERATIONS: 1000,
  }
  return selectedTypes.value.reduce((sum, type) => sum + perType[type], 0)
})
const configValid = computed(() => selectedCount.value > 0 && count.value > 0 && count.value <= 300)

function toggleType(type: QuestionType) {
  if (selectedTypes.value.includes(type)) {
    if (selectedTypes.value.length === 1) return
    selectedTypes.value = selectedTypes.value.filter((item) => item !== type)
  } else {
    selectedTypes.value = [...selectedTypes.value, type]
  }
  if (!selectedTypes.value.includes('MIXED_OPERATIONS')) {
    allowParentheses.value = false
  }
}

function usePreset(preset: 'warmup' | 'advanced' | 'challenge') {
  error.value = ''
  if (preset === 'warmup') {
    title.value = '乘除口诀热身'
    count.value = 20
    selectedTypes.value = ['TABLE_MULTIPLICATION', 'TABLE_DIVISION']
    operandCount.value = 3
    allowParentheses.value = false
    maxFactor.value = 20
  } else if (preset === 'advanced') {
    title.value = '表外乘除进阶'
    count.value = 30
    selectedTypes.value = ['ADVANCED_MULTIPLICATION', 'ADVANCED_DIVISION']
    operandCount.value = 3
    allowParentheses.value = false
    maxFactor.value = 20
  } else {
    title.value = '综合能力挑战'
    count.value = 30
    selectedTypes.value = [
      'TABLE_MULTIPLICATION',
      'TABLE_DIVISION',
      'ADVANCED_MULTIPLICATION',
      'ADVANCED_DIVISION',
      'MIXED_OPERATIONS',
    ]
    operandCount.value = 4
    allowParentheses.value = true
    maxFactor.value = 20
  }
}

function reset() {
  title.value = '今日数学练习'
  count.value = 20
  selectedTypes.value = ['TABLE_MULTIPLICATION', 'TABLE_DIVISION']
  operandCount.value = 3
  allowParentheses.value = false
  maxNumber.value = 100
  maxFactor.value = 20
  error.value = ''
}

function buildConfig(): PracticeConfig {
  return {
    title: title.value.trim() || '数学练习',
    count: Number(count.value),
    types: selectedTypes.value,
    operandCount: Number(operandCount.value),
    allowParentheses: allowParentheses.value,
    maxNumber: Number(maxNumber.value),
    maxFactor: Number(maxFactor.value),
  }
}

async function startPractice() {
  error.value = ''
  if (!configValid.value) {
    error.value = '请至少选择一种题型，并将题目数量设为 1～300'
    return
  }
  starting.value = true
  try {
    const session = await createSession(buildConfig())
    await router.push(`/practice/${session.id}`)
  } catch (err) {
    error.value = err instanceof Error ? err.message : '生成练习失败'
  } finally {
    starting.value = false
  }
}

async function exportWorksheetPdf() {
  error.value = ''
  if (!configValid.value) {
    error.value = '请至少选择一种题型，并将题目数量设为 1～300'
    return
  }
  if (exporting.value) return
  exporting.value = true
  let pdfElement: HTMLElement | null = null
  try {
    worksheet.value = await createWorksheet(buildConfig())
    await nextTick()
    if (!pdfSheet.value) throw new Error('PDF 模板尚未准备好，请重试')
    pdfElement = pdfSheet.value.cloneNode(true) as HTMLElement
    pdfElement.style.position = 'fixed'
    pdfElement.style.top = '0'
    pdfElement.style.left = '0'
    pdfElement.style.zIndex = '99998'
    pdfElement.style.pointerEvents = 'none'
    pdfElement.style.transform = 'none'
    pdfElement.style.background = '#ffffff'
    document.body.appendChild(pdfElement)
    await new Promise((resolve) => requestAnimationFrame(() => resolve(undefined)))

    const canvas = await html2canvas(pdfElement, {
      scale: 2,
      useCORS: true,
      backgroundColor: '#ffffff',
      logging: false,
      width: pdfElement.scrollWidth,
      height: pdfElement.scrollHeight,
      windowWidth: pdfElement.scrollWidth,
      windowHeight: pdfElement.scrollHeight,
      scrollX: 0,
      scrollY: 0,
    })
    if (!canvas.width || !canvas.height) throw new Error('PDF 内容渲染失败，请重试')

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
    const safeTitle = worksheet.value.title.replace(/[\/:*?"<>|]/g, '_')
    pdf.save(`${safeTitle}_${printMode.value === 'answers' ? '含答案' : '练习题'}.pdf`)
  } catch (err) {
    error.value = err instanceof Error ? err.message : '生成 PDF 失败'
  } finally {
    pdfElement?.remove()
    exporting.value = false
  }
}
</script>

<template>
  <div>
    <PageHeader
      eyebrow="SMART GENERATOR"
      title="定制一组刚刚好的练习"
      description="选择题型、数量和范围。系统会自动控制整数结果、中间过程与重复题目。"
    >
      <button class="btn btn-ghost" @click="reset"><RotateCcw /> 恢复默认</button>
    </PageHeader>

    <div class="preset-row">
      <button class="preset" @click="usePreset('warmup')">
        <span class="preset-icon blue"><Gauge /></span>
        <span><strong>快速热身</strong><small>20 题 · 表内乘除</small></span>
      </button>
      <button class="preset" @click="usePreset('advanced')">
        <span class="preset-icon amber"><Layers3 /></span>
        <span><strong>乘除进阶</strong><small>30 题 · 表外乘除</small></span>
      </button>
      <button class="preset" @click="usePreset('challenge')">
        <span class="preset-icon violet"><Sparkles /></span>
        <span><strong>综合挑战</strong><small>30 题 · 五类混合</small></span>
      </button>
    </div>

    <div class="setup-layout">
      <div class="setup-main">
        <section class="card">
          <div class="card-header">
            <div>
              <h2>选择题型</h2>
              <p>可以同时选择多类题型，系统会均匀抽取</p>
            </div>
            <span class="badge badge-blue">已选 {{ selectedCount }} 类</span>
          </div>
          <div class="card-body type-grid">
            <button
              v-for="option in typeOptions"
              :key="option.type"
              class="type-option"
              :class="[`tone-${option.tone}`, { selected: selectedTypes.includes(option.type) }]"
              @click="toggleType(option.type)"
            >
              <span class="type-icon"><component :is="option.icon" /></span>
              <span class="type-copy">
                <strong>{{ option.title }}</strong>
                <small>{{ option.description }}</small>
                <span class="example">{{ option.example }}</span>
              </span>
              <span class="check-dot"><Check v-if="selectedTypes.includes(option.type)" /></span>
            </button>
          </div>
        </section>

        <section class="card settings-card">
          <div class="card-header">
            <div>
              <h2>练习参数</h2>
              <p>数值越小，越适合基础巩固</p>
            </div>
          </div>
          <div class="card-body settings-grid">
            <div class="field">
              <label for="title">练习名称</label>
              <input id="title" v-model="title" class="input" maxlength="100" placeholder="给这次练习起个名字" />
            </div>

            <div class="field">
              <label for="count">题目数量</label>
              <div class="number-input-wrap">
                <input id="count" v-model.number="count" class="input" type="number" min="1" max="300" />
                <span>题</span>
              </div>
              <span class="field-hint">支持 1～300 题，作业要求可一次生成 300 道</span>
            </div>

            <div class="field">
              <label for="maxNumber">运算数值上限：{{ maxNumber }}</label>
              <input id="maxNumber" v-model.number="maxNumber" class="range" type="range" min="10" max="100" step="10" />
              <span class="field-hint">操作数与结果仍会控制在合理范围内</span>
            </div>

            <div class="field">
              <label for="maxFactor">乘法因子上限：{{ maxFactor }}</label>
              <input id="maxFactor" v-model.number="maxFactor" class="range" type="range" min="9" max="30" step="1" />
              <span class="field-hint">表外题至少有一个因数大于 9</span>
            </div>

            <div class="field operand-count-field" :class="{ disabled: !supportsParentheses }" @click="!supportsParentheses && notifyMixedFeature()">
              <label for="operandCount">混合运算数：{{ operandCount }} 个</label>
              <input
                id="operandCount"
                v-model.number="operandCount"
                class="range"
                type="range"
                min="2"
                max="6"
                step="1"
                :disabled="!supportsParentheses"
              />
              <span class="field-hint">
                {{ supportsParentheses ? `包含 ${operandCount - 1} 个运算符，最多 6 个数、5 个运算符` : '选择题“混合运算”后可调整' }}
              </span>
            </div>

            <div class="switch-row" :class="{ disabled: !supportsParentheses }">
              <span class="switch-icon"><Braces /></span>
              <span class="switch-copy">
                <strong>允许带括号</strong>
                <small>{{ supportsParentheses ? '用于混合运算，训练运算顺序' : '选择题“混合运算”后可启用' }}</small>
              </span>
              <button
                class="toggle"
                :class="{ on: allowParentheses }"
                :disabled="!supportsParentheses"
                type="button"
                @click="allowParentheses = !allowParentheses"
              >
                <span />
              </button>
            </div>
          </div>
          <div class="print-settings">
            <div class="print-heading">
              <div>
                <strong>打印方式</strong>
                <span>选择 PDF 中是否提前显示答案</span>
              </div>
              <FileText />
            </div>
            <div class="print-options">
              <button class="print-option" :class="{ selected: printMode === 'blank' }" type="button" @click="printMode = 'blank'">
                <span class="print-option-icon"><FileText /></span>
                <span><strong>练习题</strong><small>答案位置留空，适合打印作答</small></span>
                <span class="radio-dot" />
              </button>
              <button class="print-option" :class="{ selected: printMode === 'answers' }" type="button" @click="printMode = 'answers'">
                <span class="print-option-icon"><Eye /></span>
                <span><strong>含答案</strong><small>每题结果直接显示，适合核对</small></span>
                <span class="radio-dot" />
              </button>
            </div>
          </div>
        </section>
      </div>

      <aside class="summary-card">
        <span class="summary-kicker">YOUR WORKSHEET</span>
        <h2>{{ title || '数学练习' }}</h2>
        <div class="summary-count">
          <strong>{{ count }}</strong>
          <span>道题</span>
        </div>

        <div class="summary-list">
          <div><span>题型</span><strong>{{ selectedCount }} 类</strong></div>
          <div><span>混合运算数</span><strong>{{ supportsParentheses ? `${operandCount} 个` : '单步运算' }}</strong></div>
          <div><span>运算符</span><strong>{{ supportsParentheses ? `${operandCount - 1} 个` : '1 个' }}</strong></div>
          <div><span>括号</span><strong>{{ allowParentheses ? '允许' : '不使用' }}</strong></div>
          <div><span>数值上限</span><strong>{{ maxNumber }}</strong></div>
          <div><span>因子上限</span><strong>{{ maxFactor }}</strong></div>
          <div><span>打印方式</span><strong>{{ printMode === 'answers' ? '含答案' : '练习题' }}</strong></div>
        </div>

        <div class="quality-note">
          <span><Check /> 自动去重</span>
          <span><Check /> 整数中间结果</span>
          <span><Check /> 自动判题</span>
        </div>

        <div v-if="estimatedQuestions < count" class="alert">
          当前选择的不重复题量可能不足 {{ count }} 道，请增加题型或调整参数。
        </div>
        <div v-if="error" class="alert">{{ error }}</div>

        <div class="worksheet-actions">
          <button class="btn btn-primary btn-lg start-button" :disabled="exporting || starting || !configValid" @click="exportWorksheetPdf">
            <Download />
            {{ exporting ? '正在生成 PDF…' : '生成 PDF' }}
          </button>
          <button class="btn btn-ghost btn-lg start-button" :disabled="starting || exporting || !configValid" @click="startPractice">
            <Play v-if="!starting" />
            {{ starting ? '正在生成…' : '在线答题' }}
          </button>
        </div>
        <p class="summary-foot">PDF 导出不会写入学习记录；在线答题提交后会计入统计。</p>
      </aside>
    </div>

    <div v-if="worksheet" ref="pdfSheet" class="worksheet-pdf-sheet">
      <header class="worksheet-pdf-header">
        <div>
          <span class="worksheet-pdf-kicker">MATHMATE · 四则运算练习卷</span>
          <h1>{{ worksheet.title }}</h1>
          <p>第 {{ worksheet.questions[0]?.number }}～{{ worksheet.questions[worksheet.questions.length - 1]?.number }} 题 · 共 {{ worksheet.totalCount }} 道</p>
        </div>
        <div class="worksheet-pdf-meta">
          <span>打印方式：{{ printMode === 'answers' ? '含答案' : '练习题' }}</span>
          <span>姓名：________________</span>
          <span>日期：________________</span>
        </div>
      </header>
      <div class="worksheet-pdf-guide">
        <strong>要求：</strong>每题包含三个数、两个运算符；结果与中间过程均为 0～100 的整数。
      </div>
      <div class="worksheet-pdf-grid">
        <div v-for="question in worksheet.questions" :key="question.number" class="worksheet-pdf-line">
          第{{ question.number }}道题：{{ question.expression }} 结果：{{ printMode === 'answers' ? question.answer : '________________' }}
        </div>
      </div>
      <footer class="worksheet-pdf-footer">MathMate · 自动去重 · 整数中间结果 · 结果范围 0～100</footer>
    </div>

    <div v-if="exporting" class="pdf-overlay">
      <div class="pdf-export-card">
        <div class="loader" />
        <strong>正在生成 PDF</strong>
        <span>{{ printMode === 'answers' ? '正在排版含答案练习卷…' : '正在排版空白练习卷…' }}</span>
      </div>
    </div>

    <Transition name="notice">
      <div v-if="controlNotice" class="feature-notice">
        <Info />
        <span>{{ controlNotice }}</span>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
.preset-row { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 12px; margin-bottom: 20px; }
.preset { display: flex; align-items: center; gap: 12px; padding: 14px 16px; border: 1px solid #e2e8f0; border-radius: 14px; background: #fff; text-align: left; cursor: pointer; transition: .18s ease; }
.preset:hover { border-color: #bfdbfe; box-shadow: 0 9px 22px rgba(15,23,42,.07); transform: translateY(-1px); }
.preset-icon { display: grid; width: 39px; height: 39px; place-items: center; border-radius: 11px; }
.preset-icon svg { width: 18px; height: 18px; }
.preset-icon.blue { background: #eff6ff; color: #2563eb; }
.preset-icon.amber { background: #fffbeb; color: #d97706; }
.preset-icon.violet { background: #f5f3ff; color: #7c3aed; }
.preset strong { display: block; color: #334155; font-size: 12px; }
.preset small { display: block; margin-top: 3px; color: #94a3b8; font-size: 10px; }
.setup-layout { display: grid; grid-template-columns: minmax(0, 1fr) 330px; gap: 20px; align-items: start; }
.setup-main { display: grid; gap: 20px; }
.type-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; }
.type-option { position: relative; display: flex; gap: 13px; min-height: 130px; padding: 17px; border: 1px solid #e5ebf3; border-radius: 15px; background: #fff; text-align: left; cursor: pointer; transition: .18s ease; }
.type-option:hover { border-color: #cbd5e1; transform: translateY(-1px); }
.type-option.selected { border-color: #93c5fd; background: #f8fbff; box-shadow: inset 0 0 0 1px rgba(37,99,235,.1); }
.type-icon { display: grid; flex: 0 0 42px; width: 42px; height: 42px; place-items: center; border-radius: 12px; background: #eff6ff; color: #2563eb; }
.type-icon svg { width: 19px; height: 19px; }
.type-copy { display: flex; min-width: 0; flex: 1; flex-direction: column; }
.type-copy strong { color: #1e293b; font-size: 14px; }
.type-copy small { margin-top: 6px; color: #7b8ba3; font-size: 11px; line-height: 1.55; }
.example { margin-top: auto; padding-top: 12px; color: #94a3b8; font-family: var(--serif); font-size: 12px; }
.check-dot { position: absolute; top: 14px; right: 14px; display: grid; width: 19px; height: 19px; place-items: center; border: 1px solid #dbe3ef; border-radius: 50%; color: #fff; }
.check-dot svg { width: 12px; height: 12px; }
.selected .check-dot { border-color: #2563eb; background: #2563eb; }
.tone-green .type-icon { background: #ecfdf5; color: #059669; }
.tone-amber .type-icon { background: #fffbeb; color: #d97706; }
.tone-rose .type-icon { background: #fff1f2; color: #e11d48; }
.tone-violet .type-icon { background: #f5f3ff; color: #7c3aed; }
.settings-card { overflow: hidden; }
.settings-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 22px 28px; }
.number-input-wrap { position: relative; }
.number-input-wrap .input { padding-right: 42px; }
.number-input-wrap span { position: absolute; top: 50%; right: 14px; color: #94a3b8; font-size: 12px; transform: translateY(-50%); }
.range { width: 100%; height: 6px; margin: 9px 0 5px; accent-color: #2563eb; cursor: pointer; }
.operand-count-field.disabled { opacity: .58; cursor: pointer; }
.operand-count-field.disabled .range { pointer-events: none; }
.toggle.locked { opacity: .72; cursor: pointer; }
.feature-notice { position: fixed; right: 28px; bottom: 28px; z-index: 80; display: flex; align-items: flex-start; gap: 9px; max-width: 390px; padding: 13px 15px; border: 1px solid #bfdbfe; border-radius: 13px; background: #eff6ff; color: #1e3a8a; box-shadow: 0 16px 38px rgba(15,23,42,.16); font-size: 11px; line-height: 1.65; }
.feature-notice svg { flex: 0 0 16px; width: 16px; height: 16px; margin-top: 1px; color: #2563eb; }
.notice-enter-active, .notice-leave-active { transition: opacity .18s ease, transform .18s ease; }
.notice-enter-from, .notice-leave-to { opacity: 0; transform: translateY(6px); }
.switch-row { display: flex; grid-column: 1 / -1; align-items: center; gap: 13px; padding: 15px; border: 1px solid #e5ebf3; border-radius: 14px; background: #fbfcfe; }
.switch-row.disabled { opacity: .58; }
.switch-icon { display: grid; width: 39px; height: 39px; place-items: center; border-radius: 11px; background: #f1f5f9; color: #64748b; }
.switch-icon svg { width: 18px; height: 18px; }
.switch-copy { flex: 1; }
.switch-copy strong { display: block; color: #334155; font-size: 13px; }
.switch-copy small { display: block; margin-top: 4px; color: #94a3b8; font-size: 10px; }
.toggle { width: 45px; height: 25px; padding: 3px; border: 0; border-radius: 999px; background: #cbd5e1; cursor: pointer; transition: .18s; }
.toggle span { display: block; width: 19px; height: 19px; border-radius: 50%; background: #fff; box-shadow: 0 2px 5px rgba(15,23,42,.2); transition: .18s; }
.toggle.on { background: #2563eb; }
.toggle.on span { transform: translateX(20px); }
.summary-card { position: sticky; top: 92px; padding: 25px; border: 1px solid rgba(148,163,184,.2); border-radius: 20px; background: #fff; box-shadow: 0 15px 35px rgba(15,23,42,.08); }
.summary-kicker { color: #2563eb; font-size: 9px; font-weight: 800; letter-spacing: .18em; }
.summary-card h2 { margin: 12px 0 19px; color: #172554; font-family: var(--serif); font-size: 21px; font-weight: 600; }
.summary-count { display: flex; align-items: baseline; gap: 7px; padding-bottom: 20px; border-bottom: 1px solid #eef2f7; }
.summary-count strong { color: #0f172a; font-size: 50px; line-height: 1; letter-spacing: -.05em; }
.summary-count span { color: #64748b; font-size: 13px; }
.summary-list { display: grid; gap: 12px; padding: 18px 0; }
.summary-list div { display: flex; justify-content: space-between; color: #94a3b8; font-size: 12px; }
.summary-list strong { color: #475569; font-weight: 700; }
.quality-note { display: grid; gap: 7px; margin: 2px 0 18px; padding: 13px; border-radius: 12px; background: #f8fafc; }
.quality-note span { display: flex; align-items: center; gap: 7px; color: #64748b; font-size: 10px; }
.quality-note svg { width: 13px; height: 13px; color: #059669; }
.start-button { width: 100%; margin-top: 12px; }
.summary-foot { margin: 10px 0 0; color: #a1aec2; font-size: 9px; text-align: center; }
.print-settings { margin: 0 24px 24px; padding-top: 20px; border-top: 1px solid #eef2f7; }
.print-heading { display: flex; align-items: center; justify-content: space-between; margin-bottom: 13px; }
.print-heading strong { display: block; color: #334155; font-size: 13px; }
.print-heading span { display: block; margin-top: 4px; color: #94a3b8; font-size: 10px; }
.print-heading > svg { width: 19px; height: 19px; color: #2563eb; }
.print-options { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px; }
.print-option { display: flex; align-items: center; gap: 11px; padding: 13px; border: 1px solid #e2e8f0; border-radius: 13px; background: #fff; text-align: left; cursor: pointer; transition: .18s ease; }
.print-option:hover { border-color: #bfdbfe; }
.print-option.selected { border-color: #60a5fa; background: #f8fbff; box-shadow: inset 0 0 0 1px rgba(37,99,235,.08); }
.print-option-icon { display: grid; flex: 0 0 36px; width: 36px; height: 36px; place-items: center; border-radius: 10px; background: #eff6ff; color: #2563eb; }
.print-option-icon svg { width: 17px; height: 17px; }
.print-option > span:nth-child(2) { display: grid; flex: 1; gap: 4px; }
.print-option strong { color: #334155; font-size: 12px; }
.print-option small { color: #94a3b8; font-size: 9px; line-height: 1.5; }
.radio-dot { width: 14px; height: 14px; border: 1px solid #cbd5e1; border-radius: 50%; }
.print-option.selected .radio-dot { border: 4px solid #2563eb; }
.worksheet-actions { display: grid; gap: 9px; }
.worksheet-pdf-sheet { position: fixed; top: 0; left: -10000px; width: 794px; padding: 36px 40px; background: #fff; color: #172554; font-family: var(--serif); }
.worksheet-pdf-header { display: flex; align-items: flex-start; justify-content: space-between; gap: 30px; padding-bottom: 16px; border-bottom: 2px solid #2563eb; }
.worksheet-pdf-kicker { color: #2563eb; font-family: var(--sans); font-size: 9px; font-weight: 800; letter-spacing: .16em; }
.worksheet-pdf-header h1 { margin: 8px 0 5px; font-size: 27px; }
.worksheet-pdf-header p { margin: 0; color: #64748b; font-size: 11px; }
.worksheet-pdf-meta { display: grid; gap: 7px; min-width: 210px; color: #475569; font-size: 11px; text-align: right; }
.worksheet-pdf-guide { margin: 16px 0 13px; padding: 9px 12px; border-radius: 8px; background: #eff6ff; color: #475569; font-family: var(--sans); font-size: 9px; line-height: 1.6; }
.worksheet-pdf-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); column-gap: 25px; }
.worksheet-pdf-line { padding: 8px 0; border-bottom: 1px dashed #dbeafe; color: #172554; font-size: 12px; line-height: 1.5; break-inside: avoid; }
.worksheet-pdf-footer { margin-top: 22px; padding-top: 10px; border-top: 1px solid #dbeafe; color: #94a3b8; font-family: var(--sans); font-size: 8px; text-align: center; }
.pdf-overlay { position: fixed; inset: 0; z-index: 99999; display: grid; place-items: center; background: rgba(15,23,42,.62); backdrop-filter: blur(5px); }
.pdf-export-card { display: grid; justify-items: center; gap: 10px; min-width: 260px; padding: 30px; border-radius: 20px; background: #fff; box-shadow: 0 30px 80px rgba(15,23,42,.28); text-align: center; }
.pdf-export-card .loader { width: 32px; height: 32px; border: 3px solid #dbeafe; border-top-color: #2563eb; border-radius: 50%; animation: spin .8s linear infinite; }
.pdf-export-card strong { color: #172554; font-size: 15px; }
.pdf-export-card span { color: #94a3b8; font-size: 11px; }
@media (max-width: 1080px) {
  .setup-layout { grid-template-columns: 1fr; }
  .summary-card { position: static; }
}
@media (max-width: 780px) {
  .preset-row, .type-grid, .settings-grid, .print-options { grid-template-columns: 1fr; }
  .switch-row { grid-column: auto; }
}
</style>
