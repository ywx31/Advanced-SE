<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  ArrowLeft,
  ArrowRight,
  BadgeCheck,
  Check,
  ClipboardPenLine,
  Play,
  RotateCcw,
  Sparkles,
  X,
} from 'lucide-vue-next'
import PageHeader from '../components/PageHeader.vue'
import EmptyState from '../components/EmptyState.vue'
import TypeBadge from '../components/TypeBadge.vue'
import {
  createWrongPractice,
  listWrongQuestions,
  setMastered,
  type WrongPracticeMode,
} from '../api/wrongBook'
import type { PageResult, WrongQuestion } from '../types'

const router = useRouter()
const data = ref<PageResult<WrongQuestion> | null>(null)
const filter = ref<'active' | 'all' | 'mastered'>('active')
const page = ref(1)
const loading = ref(true)
const error = ref('')
const updatingId = ref<number | null>(null)
const selectedIds = ref<Set<number>>(new Set())

const practiceModalOpen = ref(false)
const practiceMode = ref<WrongPracticeMode>('SIMILAR')
const practiceCount = ref(10)
const creatingPractice = ref(false)
const practiceError = ref('')

onMounted(load)

async function load() {
  loading.value = true
  error.value = ''
  try {
    const mastered = filter.value === 'all' ? null : filter.value === 'mastered'
    data.value = await listWrongQuestions(mastered, page.value, 12)
  } catch (err) {
    error.value = err instanceof Error ? err.message : '加载错题本失败'
  } finally {
    loading.value = false
  }
}

async function switchFilter(next: 'active' | 'all' | 'mastered') {
  filter.value = next
  page.value = 1
  await load()
}

async function toggleMastered(question: WrongQuestion) {
  updatingId.value = question.id
  error.value = ''
  try {
    await setMastered(question.id, !question.mastered)
    await load()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '更新错题状态失败'
  } finally {
    updatingId.value = null
  }
}

async function goPage(nextPage: number) {
  if (!data.value) return
  const maxPage = Math.max(1, Math.ceil(data.value.total / data.value.size))
  if (nextPage < 1 || nextPage > maxPage) return
  page.value = nextPage
  await load()
}

const totalPages = computed(() => data.value ? Math.max(1, Math.ceil(data.value.total / data.value.size)) : 1)
const masteredCount = computed(() => data.value?.items.filter((item) => item.mastered).length || 0)
const highFrequency = computed(() => data.value?.items.filter((item) => item.wrongCount >= 2).length || 0)
const selectedCount = computed(() => selectedIds.value.size)
const allPageSelected = computed(() =>
  Boolean(data.value?.items.length) && data.value!.items.every((item) => selectedIds.value.has(item.id)),
)
const modalTitle = computed(() =>
  practiceMode.value === 'ORIGINAL' ? '重做所选错题' : '生成同类练习',
)
const modalDescription = computed(() =>
  practiceMode.value === 'ORIGINAL'
    ? '原题会按原样出现，适合检查是否真正记住。答对后会影响原错题的连续答对次数。'
    : '系统会按所选错题的题型重新生成新题，不会直接使用原题。答对后只提升对应题型掌握度。',
)

function formatDate(value: string) {
  return new Date(value).toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}

function isSelected(id: number) {
  return selectedIds.value.has(id)
}

function toggleSelection(id: number) {
  const next = new Set(selectedIds.value)
  if (next.has(id)) next.delete(id)
  else next.add(id)
  selectedIds.value = next
}

function selectCurrentPage() {
  selectedIds.value = new Set(data.value?.items.map((item) => item.id) || [])
}

function clearSelection() {
  selectedIds.value = new Set()
}

function openPractice(mode: WrongPracticeMode, question?: WrongQuestion) {
  if (question) selectedIds.value = new Set([question.id])
  practiceMode.value = mode
  practiceCount.value = 10
  practiceError.value = ''
  practiceModalOpen.value = true
}

function closePracticeModal() {
  if (creatingPractice.value) return
  practiceModalOpen.value = false
  practiceError.value = ''
}

async function confirmPractice() {
  creatingPractice.value = true
  practiceError.value = ''
  try {
    const session = await createWrongPractice([...selectedIds.value], practiceMode.value, practiceCount.value)
    practiceModalOpen.value = false
    await router.push(`/practice/${session.id}`)
  } catch (err) {
    practiceError.value = err instanceof Error ? err.message : '生成练习失败'
  } finally {
    creatingPractice.value = false
  }
}
</script>

<template>
  <div>
    <PageHeader
      eyebrow="MISTAKE NOTEBOOK"
      title="错题不是失败，是线索"
      description="选择错题后可以重做原题，或按题型生成新的同类练习。"
    >
      <button class="btn btn-ghost" @click="openPractice('ORIGINAL')"><RotateCcw /> 重做原题</button>
      <button class="btn btn-primary" @click="openPractice('SIMILAR')"><Sparkles /> 同类练习</button>
    </PageHeader>

    <div v-if="error" class="alert">{{ error }}</div>

    <section class="mistake-banner">
      <div class="banner-copy">
        <span class="banner-kicker">TURN MISTAKES INTO PROGRESS</span>
        <h2>把错题复习变成<br /><em>下一次的稳定得分。</em></h2>
        <p>原题重做检验记忆，同类练习检验方法。未选择错题时，默认使用全部待攻克错题。</p>
      </div>
      <div class="banner-stats">
        <div><strong>{{ data?.total ?? '—' }}</strong><span>{{ filter === 'active' ? '待攻克' : filter === 'mastered' ? '已掌握' : '全部错题' }}</span></div>
        <div><strong>{{ highFrequency }}</strong><span>高频错题</span></div>
        <div><strong>{{ masteredCount }}</strong><span>本页已掌握</span></div>
      </div>
      <div class="banner-orb">×</div>
    </section>

    <section class="wrong-section">
      <div class="wrong-toolbar">
        <div class="filter-tabs">
          <button :class="{ active: filter === 'active' }" @click="switchFilter('active')">待攻克</button>
          <button :class="{ active: filter === 'all' }" @click="switchFilter('all')">全部</button>
          <button :class="{ active: filter === 'mastered' }" @click="switchFilter('mastered')">已掌握</button>
        </div>
        <div class="selection-tools">
          <span>已选 <strong>{{ selectedCount }}</strong> 道</span>
          <button v-if="data?.items.length && !allPageSelected" @click="selectCurrentPage">全选本页</button>
          <button v-if="selectedCount" @click="clearSelection">清空</button>
        </div>
      </div>

      <div v-if="loading" class="loading-panel"><div class="loader" /></div>
      <template v-else-if="data">
        <div v-if="data.items.length" class="wrong-grid">
          <article v-for="question in data.items" :key="question.id" class="wrong-card" :class="{ mastered: question.mastered, selected: isSelected(question.id) }">
            <div class="wrong-card-top">
              <button class="select-control" :class="{ active: isSelected(question.id) }" :title="isSelected(question.id) ? '取消选择' : '选择这道错题'" @click="toggleSelection(question.id)">
                <Check v-if="isSelected(question.id)" />
              </button>
              <span class="status-dot" />
              <span class="wrong-count">错 {{ question.wrongCount }} 次</span>
              <span v-if="question.correctStreak" class="streak">连续答对 {{ question.correctStreak }} 次</span>
              <span class="type-tag"><TypeBadge :type="question.questionType" /></span>
              <span v-if="question.mastered" class="badge badge-green"><BadgeCheck /> 已掌握</span>
            </div>

            <div class="wrong-expression serif">{{ question.expression }} = ?</div>

            <div class="answer-panels">
              <div class="answer-panel wrong-answer"><span>上次答案</span><strong>{{ question.lastWrongAnswer ?? '未作答' }}</strong></div>
              <div class="answer-panel right-answer"><span>正确答案</span><strong>{{ question.correctAnswer }}</strong></div>
            </div>

            <div class="wrong-footer">
              <span>最近练习：{{ formatDate(question.lastPracticedAt) }}</span>
              <div class="card-actions">
                <button class="similar-button" @click="openPractice('SIMILAR', question)"><Sparkles /> 同类练习</button>
                <button class="master-button" :disabled="updatingId === question.id" @click="toggleMastered(question)">
                  <Check v-if="!question.mastered" />
                  <RotateCcw v-else />
                  {{ updatingId === question.id ? '更新中…' : question.mastered ? '重新加入待攻克' : '标记已掌握' }}
                </button>
              </div>
            </div>
          </article>
        </div>

        <EmptyState v-else title="这里暂时没有错题" description="完成练习后，答错的题会自动收录到这里。">
          <RouterLink to="/practice/new" class="btn btn-primary btn-sm"><ClipboardPenLine /> 去做一组练习</RouterLink>
        </EmptyState>

        <div v-if="totalPages > 1" class="pagination">
          <button class="btn btn-ghost btn-sm" :disabled="page === 1" @click="goPage(page - 1)"><ArrowLeft /> 上一页</button>
          <span>第 {{ page }} / {{ totalPages }} 页</span>
          <button class="btn btn-ghost btn-sm" :disabled="page === totalPages" @click="goPage(page + 1)">下一页 <ArrowRight /></button>
        </div>
      </template>
    </section>

    <Transition name="modal">
      <div v-if="practiceModalOpen" class="modal-backdrop" @click.self="closePracticeModal">
        <div class="practice-modal">
          <button class="modal-close" @click="closePracticeModal"><X /></button>
          <span class="modal-icon"><Play /></span>
          <h2>{{ modalTitle }}</h2>
          <p class="modal-description">{{ modalDescription }}</p>

          <div class="mode-switch">
            <button :class="{ active: practiceMode === 'ORIGINAL' }" @click="practiceMode = 'ORIGINAL'">重做原题</button>
            <button :class="{ active: practiceMode === 'SIMILAR' }" @click="practiceMode = 'SIMILAR'">同类练习</button>
          </div>

          <div class="practice-scope">
            <span>参与错题</span>
            <strong>{{ selectedCount ? `已选 ${selectedCount} 道` : '全部待攻克错题' }}</strong>
          </div>

          <div v-if="practiceMode === 'SIMILAR'" class="count-choice">
            <span>生成题目数量</span>
            <div>
              <button v-for="value in [5, 10, 20]" :key="value" :class="{ active: practiceCount === value }" @click="practiceCount = value">{{ value }} 题</button>
            </div>
            <small>系统会按所选错题的题型比例分配题量，并排除原题。</small>
          </div>
          <div v-else class="original-note">原题会按原样出现，实际题量等于参与的错题数量。</div>

          <div v-if="practiceError" class="alert">{{ practiceError }}</div>
          <div class="modal-actions">
            <button class="btn btn-ghost" :disabled="creatingPractice" @click="closePracticeModal">取消</button>
            <button class="btn btn-primary" :disabled="creatingPractice" @click="confirmPractice">
              {{ creatingPractice ? '生成中…' : practiceMode === 'ORIGINAL' ? '开始重做' : '生成练习' }}
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
.mistake-banner { position: relative; overflow: hidden; display: grid; grid-template-columns: minmax(0, 1fr) auto; gap: 30px; align-items: center; min-height: 200px; padding: 30px 34px; border-radius: 22px; background: radial-gradient(circle at 90% 10%, rgba(94,234,212,.22), transparent 30%), linear-gradient(135deg, #172554, #0f172a); color: #fff; box-shadow: 0 16px 36px rgba(15,23,42,.14); }
.banner-kicker { color: #5eead4; font-size: 9px; font-weight: 800; letter-spacing: .18em; }
.banner-copy h2 { margin: 13px 0 9px; font-family: var(--serif); font-size: 27px; font-weight: 600; line-height: 1.45; }
.banner-copy h2 em { color: #5eead4; font-style: normal; }
.banner-copy p { margin: 0; color: #aab8d1; font-size: 11px; line-height: 1.7; }
.banner-stats { position: relative; z-index: 1; display: flex; gap: 12px; }
.banner-stats > div { min-width: 90px; padding: 13px; border: 1px solid rgba(255,255,255,.1); border-radius: 13px; background: rgba(255,255,255,.06); text-align: center; }
.banner-stats strong { display: block; color: #fff; font-size: 23px; }
.banner-stats span { display: block; margin-top: 4px; color: #9daccc; font-size: 9px; }
.banner-orb { position: absolute; right: 410px; bottom: -65px; color: rgba(255,255,255,.04); font-family: var(--serif); font-size: 180px; }
.wrong-section { margin-top: 23px; }
.wrong-toolbar { display: flex; align-items: center; justify-content: space-between; gap: 15px; margin-bottom: 14px; }
.filter-tabs { display: flex; padding: 4px; border-radius: 11px; background: #e9eef5; }
.filter-tabs button { padding: 8px 16px; border: 0; border-radius: 8px; background: transparent; color: #64748b; font-size: 11px; cursor: pointer; }
.filter-tabs button.active { background: #fff; color: #172554; font-weight: 700; box-shadow: 0 3px 8px rgba(15,23,42,.08); }
.selection-tools { display: flex; align-items: center; gap: 10px; color: #94a3b8; font-size: 10px; }
.selection-tools strong { color: #2563eb; font-size: 13px; }
.selection-tools button { padding: 5px 8px; border: 0; border-radius: 7px; background: #eff6ff; color: #2563eb; font-size: 9px; font-weight: 700; cursor: pointer; }
.wrong-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px; }
.wrong-card { padding: 20px; border: 1px solid #e2e8f0; border-radius: 18px; background: #fff; box-shadow: 0 9px 24px rgba(15,23,42,.045); transition: border-color .16s ease, background .16s ease; }
.wrong-card.selected { border-color: #93c5fd; background: #f8fbff; box-shadow: inset 0 0 0 1px rgba(37,99,235,.08), 0 9px 24px rgba(15,23,42,.045); }
.wrong-card.mastered { background: #fbfefd; }
.wrong-card-top { display: flex; min-height: 22px; align-items: center; gap: 7px; }
.select-control { display: grid; flex: 0 0 19px; width: 19px; height: 19px; place-items: center; padding: 0; border: 1px solid #cbd5e1; border-radius: 50%; background: #fff; color: #fff; cursor: pointer; }
.select-control.active { border-color: #2563eb; background: #2563eb; }
.select-control svg { width: 11px; height: 11px; }
.status-dot { width: 7px; height: 7px; border-radius: 50%; background: #e11d48; box-shadow: 0 0 0 4px #fff1f2; }
.mastered .status-dot { background: #10b981; box-shadow: 0 0 0 4px #ecfdf5; }
.wrong-count { color: #e11d48; font-size: 10px; font-weight: 700; }
.streak { color: #94a3b8; font-size: 9px; }
.type-tag { margin-left: auto; }
.wrong-card-top .badge { margin-left: 0; }
.wrong-expression { margin: 27px 0 21px; color: #172554; font-family: var(--serif); font-size: clamp(22px, 2.2vw, 30px); font-weight: 600; letter-spacing: .01em; overflow-wrap: anywhere; }
.answer-panels { display: grid; grid-template-columns: 1fr 1fr; gap: 9px; }
.answer-panel { padding: 11px 13px; border-radius: 11px; }
.answer-panel span { display: block; font-size: 9px; }
.answer-panel strong { display: block; margin-top: 4px; font-size: 17px; }
.wrong-answer { background: #fff1f2; color: #be123c; }
.wrong-answer span { color: #fb7185; }
.right-answer { background: #ecfdf5; color: #047857; }
.right-answer span { color: #34d399; }
.wrong-footer { display: flex; align-items: center; justify-content: space-between; gap: 10px; margin-top: 17px; color: #a1aec2; font-size: 9px; }
.card-actions { display: flex; gap: 6px; }
.master-button, .similar-button { display: inline-flex; align-items: center; gap: 4px; padding: 7px 9px; border: 0; border-radius: 8px; font-size: 9px; font-weight: 700; cursor: pointer; }
.master-button { background: #f8fafc; color: #64748b; }
.master-button:hover { background: #eff6ff; color: #2563eb; }
.similar-button { background: #eff6ff; color: #2563eb; }
.similar-button:hover { background: #dbeafe; }
.master-button svg, .similar-button svg { width: 12px; height: 12px; }
.pagination { display: flex; align-items: center; justify-content: center; gap: 15px; margin-top: 22px; color: #94a3b8; font-size: 11px; }
.modal-backdrop { position: fixed; inset: 0; z-index: 80; display: grid; place-items: center; padding: 20px; background: rgba(15,23,42,.58); backdrop-filter: blur(5px); }
.practice-modal { position: relative; width: min(470px, 100%); padding: 30px; border-radius: 22px; background: #fff; box-shadow: 0 30px 80px rgba(15,23,42,.28); }
.modal-close { position: absolute; top: 14px; right: 14px; display: grid; width: 32px; height: 32px; place-items: center; border: 0; border-radius: 9px; background: #f8fafc; color: #94a3b8; cursor: pointer; }
.modal-close svg { width: 17px; height: 17px; }
.modal-icon { display: grid; width: 52px; height: 52px; place-items: center; border-radius: 16px; background: #eff6ff; color: #2563eb; }
.modal-icon svg { width: 24px; height: 24px; }
.practice-modal h2 { margin: 16px 0 8px; color: #172554; font-family: var(--serif); font-size: 24px; }
.modal-description { margin: 0 0 18px; color: #64748b; font-size: 11px; line-height: 1.7; }
.mode-switch { display: grid; grid-template-columns: 1fr 1fr; gap: 6px; padding: 4px; border-radius: 11px; background: #f1f5f9; }
.mode-switch button { padding: 9px; border: 0; border-radius: 8px; background: transparent; color: #64748b; font-size: 11px; font-weight: 700; cursor: pointer; }
.mode-switch button.active { background: #fff; color: #172554; box-shadow: 0 3px 8px rgba(15,23,42,.08); }
.practice-scope { display: flex; align-items: center; justify-content: space-between; margin-top: 16px; padding: 12px 14px; border-radius: 10px; background: #f8fafc; color: #94a3b8; font-size: 10px; }
.practice-scope strong { color: #334155; }
.count-choice { display: grid; gap: 9px; margin-top: 18px; }
.count-choice > span { color: #334155; font-size: 11px; font-weight: 700; }
.count-choice > div { display: flex; gap: 8px; }
.count-choice button { flex: 1; padding: 9px; border: 1px solid #e2e8f0; border-radius: 9px; background: #fff; color: #64748b; font-size: 10px; cursor: pointer; }
.count-choice button.active { border-color: #93c5fd; background: #eff6ff; color: #2563eb; font-weight: 700; }
.count-choice small, .original-note { color: #94a3b8; font-size: 9px; line-height: 1.6; }
.original-note { margin-top: 16px; padding: 12px; border-radius: 10px; background: #fffbeb; color: #a16207; }
.modal-actions { display: grid; grid-template-columns: 1fr 1fr; gap: 9px; margin-top: 22px; }
.modal-enter-active, .modal-leave-active { transition: opacity .17s ease; }
.modal-enter-active .practice-modal, .modal-leave-active .practice-modal { transition: transform .17s ease; }
.modal-enter-from, .modal-leave-to { opacity: 0; }
.modal-enter-from .practice-modal, .modal-leave-to .practice-modal { transform: translateY(8px) scale(.98); }
@media (max-width: 950px) {
  .mistake-banner { grid-template-columns: 1fr; }
  .banner-stats { justify-content: flex-start; }
  .banner-orb { right: -10px; }
}
@media (max-width: 760px) {
  .wrong-grid { grid-template-columns: 1fr; }
  .banner-stats { display: grid; grid-template-columns: repeat(3, 1fr); }
  .banner-stats > div { min-width: 0; }
  .wrong-toolbar { align-items: flex-start; flex-direction: column; }
  .wrong-footer { align-items: flex-start; flex-direction: column; }
  .card-actions { width: 100%; }
  .similar-button, .master-button { flex: 1; justify-content: center; }
}
</style>
