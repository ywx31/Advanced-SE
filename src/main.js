import html2canvas from 'html2canvas'
import { jsPDF } from 'jspdf'
import { generateQuestions } from './generator.js'
import './style.css'

const titleInput = document.querySelector('#sheet-title')
const countInput = document.querySelector('#question-count')
const rangeInput = document.querySelector('#question-range')
const countLabel = document.querySelector('#count-label')
const previewButton = document.querySelector('#preview-button')
const pdfButton = document.querySelector('#pdf-button')
const emptyState = document.querySelector('#empty-state')
const previewContent = document.querySelector('#preview-content')
const previewTitle = document.querySelector('#preview-title')
const previewSummary = document.querySelector('#preview-summary')
const previewMode = document.querySelector('#preview-mode')
const previewList = document.querySelector('#preview-list')
const paperTitle = document.querySelector('#paper-title')
const paperMeta = document.querySelector('#paper-meta')
const paperMode = document.querySelector('#paper-mode')
const pdfSheet = document.querySelector('#pdf-sheet')
const pdfTitle = document.querySelector('#pdf-title')
const pdfMeta = document.querySelector('#pdf-meta')
const pdfMode = document.querySelector('#pdf-mode')
const pdfList = document.querySelector('#pdf-list')
const overlay = document.querySelector('#export-overlay')
const exportMessage = document.querySelector('#export-message')
const toast = document.querySelector('#toast')

let printMode = 'blank'
let questions = []
let generatedCount = 0
let generatedTitle = ''
let toastTimer

function getTitle() {
  return titleInput.value.trim() || '四则运算练习卷'
}

function getCount() {
  const value = Number(countInput.value)
  return Number.isInteger(value) ? value : NaN
}

function syncCount(source) {
  const value = Math.min(300, Math.max(1, Math.round(Number(source.value) || 1)))
  countInput.value = String(value)
  rangeInput.value = String(value)
  countLabel.textContent = `${value} 题`
}

function setPrintMode(mode) {
  printMode = mode
  document.querySelectorAll('.print-option').forEach((button) => {
    const selected = button.dataset.mode === mode
    button.classList.toggle('selected', selected)
    button.setAttribute('aria-checked', String(selected))
  })
  updateModeLabels()
}

function updateModeLabels() {
  const blank = printMode === 'blank'
  previewMode.textContent = blank ? '练习题' : '含答案'
  paperMode.textContent = `打印方式：${blank ? '练习题' : '含答案'}`
  pdfMode.textContent = paperMode.textContent
}

function showToast(message, tone = 'success') {
  window.clearTimeout(toastTimer)
  toast.textContent = message
  toast.className = `toast visible ${tone}`
  toastTimer = window.setTimeout(() => {
    toast.className = 'toast'
  }, 2800)
}

function questionLine(question, includeAnswer) {
  const answer = includeAnswer ? String(question.answer) : '________________'
  return `第${question.number}道题：${question.expression} 结果：${answer}`
}

function renderList(target, includeAnswer, compact = false) {
  const fragment = document.createDocumentFragment()
  for (const question of questions) {
    const line = document.createElement('div')
    line.className = compact ? 'question-line' : 'pdf-line'
    line.textContent = questionLine(question, includeAnswer)
    fragment.appendChild(line)
  }
  target.replaceChildren(fragment)
}

function renderPreview() {
  const title = getTitle()
  previewTitle.textContent = title
  previewSummary.textContent = `共 ${questions.length} 道不重复题目 · ${printMode === 'answers' ? '每题包含答案' : '答案位置留空'} · 预览可滚动，PDF 包含全部题目`
  paperTitle.textContent = title
  paperMeta.textContent = `共 ${questions.length} 道题 · 每题 3 个数、2 个运算符`
  updateModeLabels()
  renderList(previewList, printMode === 'answers', true)
  emptyState.classList.add('hidden')
  previewContent.classList.remove('hidden')
}

function ensureQuestions(force = false) {
  const count = getCount()
  if (!Number.isInteger(count) || count < 1 || count > 300) {
    throw new Error('题目数量必须是 1～300 的整数')
  }
  if (force || questions.length !== count || generatedTitle !== getTitle()) {
    questions = generateQuestions(count)
    generatedCount = count
    generatedTitle = getTitle()
  }
  return questions
}

async function generatePreview(showMessage = true) {
  const started = performance.now()
  ensureQuestions(true)
  renderPreview()
  if (showMessage) {
    const elapsed = Math.round(performance.now() - started)
    showToast(`已生成 ${generatedCount} 道不重复题目（${elapsed} ms）`)
  }
  await new Promise((resolve) => requestAnimationFrame(resolve))
}

async function downloadPdf() {
  if (pdfButton.disabled) return
  try {
    if (!questions.length || getCount() !== generatedCount || getTitle() !== generatedTitle) {
      await generatePreview(false)
    }
    pdfButton.disabled = true
    overlay.classList.remove('hidden')
    exportMessage.textContent = printMode === 'answers'
      ? '正在排版含答案练习卷，请稍候…'
      : '正在排版空白练习卷，请稍候…'

    const title = getTitle()
    const safeTitle = title.replace(/[\\/:*?"<>|]/g, '_')
    pdfTitle.textContent = title
    pdfMeta.textContent = `共 ${questions.length} 道题 · 每题 3 个数、2 个运算符`
    updateModeLabels()
    renderList(pdfList, printMode === 'answers')

    await new Promise((resolve) => requestAnimationFrame(() => requestAnimationFrame(resolve)))

    const canvas = await html2canvas(pdfSheet, {
      scale: 2,
      useCORS: true,
      backgroundColor: '#ffffff',
      logging: false,
      width: pdfSheet.scrollWidth,
      height: pdfSheet.scrollHeight,
      windowWidth: pdfSheet.scrollWidth,
      windowHeight: pdfSheet.scrollHeight,
      scrollX: 0,
      scrollY: 0,
    })
    if (!canvas.width || !canvas.height) throw new Error('PDF 内容渲染失败')

    const pdf = new jsPDF({ unit: 'mm', format: 'a4', orientation: 'portrait' })
    const pageWidth = 210
    const pageHeight = 297
    const margin = 9
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

    pdf.save(`${safeTitle}_${questions.length}题_${printMode === 'answers' ? '含答案' : '练习题'}.pdf`)
    showToast('PDF 已生成并开始下载')
  } catch (error) {
    showToast(error instanceof Error ? error.message : '生成 PDF 失败', 'error')
  } finally {
    pdfButton.disabled = false
    overlay.classList.add('hidden')
  }
}

countInput.addEventListener('input', () => syncCount(countInput))
rangeInput.addEventListener('input', () => syncCount(rangeInput))
titleInput.addEventListener('input', () => {
  if (questions.length) {
    previewTitle.textContent = getTitle()
    paperTitle.textContent = getTitle()
  }
})
document.querySelectorAll('.print-option').forEach((button) => {
  button.addEventListener('click', () => {
    setPrintMode(button.dataset.mode)
    if (questions.length) renderPreview()
  })
})
previewButton.addEventListener('click', () => {
  generatePreview().catch((error) => {
    showToast(error instanceof Error ? error.message : '生成预览失败', 'error')
  })
})
pdfButton.addEventListener('click', downloadPdf)

syncCount(countInput)
updateModeLabels()
