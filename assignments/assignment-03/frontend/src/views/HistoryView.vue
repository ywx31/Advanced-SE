<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft, ArrowRight, CalendarDays, ChevronRight, ClipboardPenLine, Clock3 } from 'lucide-vue-next'
import PageHeader from '../components/PageHeader.vue'
import EmptyState from '../components/EmptyState.vue'
import TypeBadge from '../components/TypeBadge.vue'
import { listSessions } from '../api/practice'
import type { PageResult, SessionSummary } from '../types'
import { QUESTION_TYPE_LABELS } from '../types'

const router = useRouter()
const data = ref<PageResult<SessionSummary> | null>(null)
const page = ref(1)
const loading = ref(true)
const error = ref('')

onMounted(load)

async function load() {
  loading.value = true
  error.value = ''
  try {
    data.value = await listSessions(page.value, 10)
  } catch (err) {
    error.value = err instanceof Error ? err.message : '加载记录失败'
  } finally {
    loading.value = false
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
const submitted = computed(() => data.value?.items.filter((item) => item.status === 'SUBMITTED') || [])
const averageAccuracy = computed(() => {
  if (!submitted.value.length) return null
  return submitted.value.reduce((sum, item) => sum + Number(item.accuracy), 0) / submitted.value.length
})

function formatDate(value: string) {
  return new Date(value).toLocaleString('zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit',
  })
}

function duration(seconds: number) {
  if (!seconds) return '—'
  const minutes = Math.floor(seconds / 60)
  return minutes ? `${minutes}分${seconds % 60}秒` : `${seconds}秒`
}

function openSession(session: SessionSummary) {
  router.push(session.status === 'SUBMITTED' ? `/result/${session.id}` : `/practice/${session.id}`)
}
</script>

<template>
  <div>
    <PageHeader
      eyebrow="PRACTICE HISTORY"
      title="每一次练习，都有迹可循"
      description="回看完成时间、正确率和题型组合，持续校准自己的学习节奏。"
    >
      <RouterLink to="/practice/new" class="btn btn-primary"><ClipboardPenLine /> 新练习</RouterLink>
    </PageHeader>

    <div v-if="error" class="alert">{{ error }}</div>
    <div v-if="loading" class="loading-panel"><div class="loader" /></div>

    <template v-else-if="data">
      <section class="history-summary">
        <div><span>累计练习</span><strong>{{ data.total }}</strong><small>次</small></div>
        <div><span>本页正确率</span><strong>{{ averageAccuracy === null ? '—' : averageAccuracy.toFixed(1) }}</strong><small v-if="averageAccuracy !== null">%</small></div>
        <div><span>最近状态</span><strong class="status-text">{{ submitted.length ? '保持练习' : '等待开始' }}</strong></div>
      </section>

      <section class="card history-card">
        <div class="card-header">
          <div>
            <h2>练习记录</h2>
            <p>点击任一记录查看详情或继续作答</p>
          </div>
          <span class="badge badge-blue"><CalendarDays /> 共 {{ data.total }} 条</span>
        </div>
        <div class="card-body">
          <div v-if="data.items.length" class="history-list">
            <button
              v-for="session in data.items"
              :key="session.id"
              class="history-row"
              @click="openSession(session)"
            >
              <span class="date-block">
                <strong>{{ new Date(session.startedAt).getDate().toString().padStart(2, '0') }}</strong>
                <small>{{ new Date(session.startedAt).toLocaleDateString('zh-CN', { month: 'short' }) }}</small>
              </span>
              <span class="history-main">
                <span class="history-title-line">
                  <strong>{{ session.title }}</strong>
                  <span class="badge" :class="session.status === 'SUBMITTED' ? 'badge-green' : 'badge-amber'">
                    {{ session.status === 'SUBMITTED' ? '已完成' : '进行中' }}
                  </span>
                </span>
                <span class="history-meta">
                  <span><Clock3 /> {{ formatDate(session.startedAt) }}</span>
                  <span>{{ session.types.map((type) => QUESTION_TYPE_LABELS[type]).join(' · ') }}</span>
                </span>
                <span class="type-line">
                  <TypeBadge v-for="type in session.types.slice(0, 3)" :key="type" :type="type" />
                </span>
              </span>
              <span v-if="session.status === 'SUBMITTED'" class="history-score">
                <span class="mini-ring" :style="{ '--pct': `${Number(session.accuracy)}%` }">
                  <strong>{{ Number(session.accuracy).toFixed(0) }}%</strong>
                </span>
                <small>{{ session.correctCount }}/{{ session.totalCount }} · {{ duration(session.durationSeconds) }}</small>
              </span>
              <span v-else class="continue-button">继续 <ChevronRight /></span>
            </button>
          </div>
          <EmptyState v-else title="还没有练习记录" description="去生成第一套练习，开启你的学习曲线。">
            <RouterLink to="/practice/new" class="btn btn-primary btn-sm">开始练习</RouterLink>
          </EmptyState>
        </div>
      </section>

      <div v-if="totalPages > 1" class="pagination">
        <button class="btn btn-ghost btn-sm" :disabled="page === 1" @click="goPage(page - 1)"><ArrowLeft /> 上一页</button>
        <span>第 {{ page }} / {{ totalPages }} 页</span>
        <button class="btn btn-ghost btn-sm" :disabled="page === totalPages" @click="goPage(page + 1)">下一页 <ArrowRight /></button>
      </div>
    </template>
  </div>
</template>

<style scoped>
.history-summary { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 14px; margin-bottom: 20px; }
.history-summary > div { padding: 19px 22px; border: 1px solid rgba(148,163,184,.2); border-radius: 17px; background: #fff; box-shadow: 0 8px 22px rgba(15,23,42,.04); }
.history-summary span { display: block; color: #94a3b8; font-size: 10px; }
.history-summary strong { display: inline-block; margin-top: 5px; color: #172554; font-size: 25px; letter-spacing: -.03em; }
.history-summary small { margin-left: 3px; color: #94a3b8; font-size: 10px; }
.status-text { font-family: var(--serif); font-size: 20px !important; }
.history-list { display: grid; gap: 0; }
.history-row { display: flex; width: 100%; align-items: center; gap: 18px; padding: 18px 2px; border: 0; border-bottom: 1px solid #f1f5f9; background: transparent; text-align: left; cursor: pointer; transition: background .15s; }
.history-row:last-child { border-bottom: 0; }
.history-row:hover { background: #fbfdff; }
.date-block { display: grid; flex: 0 0 55px; justify-items: center; padding: 9px 7px; border-radius: 12px; background: #f8fafc; }
.date-block strong { color: #2563eb; font-size: 21px; line-height: 1; }
.date-block small { margin-top: 4px; color: #94a3b8; font-size: 9px; }
.history-main { display: grid; min-width: 0; flex: 1; gap: 7px; }
.history-title-line { display: flex; align-items: center; gap: 9px; }
.history-title-line > strong { overflow: hidden; color: #334155; font-size: 14px; text-overflow: ellipsis; white-space: nowrap; }
.history-meta { display: flex; flex-wrap: wrap; gap: 13px; color: #94a3b8; font-size: 9px; }
.history-meta span { display: flex; align-items: center; gap: 4px; }
.history-meta svg { width: 11px; height: 11px; }
.type-line { display: flex; gap: 5px; }
.history-score { display: grid; flex: 0 0 145px; justify-items: end; gap: 5px; }
.mini-ring { display: grid; width: 56px; height: 56px; place-items: center; border-radius: 50%; background: conic-gradient(#14b8a6 var(--pct), #e2e8f0 0); }
.mini-ring::before { grid-area: 1 / 1; width: 44px; height: 44px; border-radius: 50%; background: #fff; content: ''; }
.mini-ring strong { z-index: 1; grid-area: 1 / 1; color: #334155; font-size: 11px; }
.history-score > small { color: #94a3b8; font-size: 9px; }
.continue-button { display: flex; align-items: center; gap: 3px; color: #d97706; font-size: 11px; font-weight: 700; }
.continue-button svg { width: 15px; height: 15px; }
.pagination { display: flex; align-items: center; justify-content: center; gap: 15px; margin-top: 22px; color: #94a3b8; font-size: 11px; }
@media (max-width: 760px) {
  .history-summary { grid-template-columns: 1fr; }
  .history-row { align-items: flex-start; flex-wrap: wrap; }
  .history-main { width: calc(100% - 76px); }
  .history-score { margin-left: 73px; align-items: center; grid-auto-flow: column; }
  .history-score > small { max-width: 150px; }
}
</style>
