<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import type { EChartsOption } from 'echarts'
import {
  ArrowDownRight,
  ArrowLeft,
  ArrowRight,
  ArrowUpRight,
  BookOpenCheck,
  Bot,
  CalendarDays,
  Clock3,
  Flame,
  Lightbulb,
  RefreshCw,
  Sparkles,
  Target,
  WandSparkles,
} from 'lucide-vue-next'
import PageHeader from '../components/PageHeader.vue'
import StatCard from '../components/StatCard.vue'
import EChart from '../components/EChart.vue'
import TypeBadge from '../components/TypeBadge.vue'
import {
  createWeeklyRecommendedPractice,
  fetchLatestWeeklyReport,
  fetchWeeklyReport,
  markWeeklyReportRead,
  regenerateWeeklyReport,
} from '../api/weeklyReport'
import { QUESTION_TYPE_LABELS } from '../types'
import type { QuestionType, WeeklyReport } from '../types'

const router = useRouter()
const report = ref<WeeklyReport | null>(null)
const selectedWeek = ref('')
const loading = ref(true)
const generating = ref(false)
const startingPractice = ref(false)
const error = ref('')

const currentWeekStart = computed(() => mondayOf(new Date()))
const isCurrentWeek = computed(() => selectedWeek.value === currentWeekStart.value)
const isLatestWeek = computed(() => selectedWeek.value >= currentWeekStart.value)

onMounted(async () => {
  loading.value = true
  try {
    const data = await fetchLatestWeeklyReport()
    applyReport(data)
    if (!data.read) {
      void markWeeklyReportRead(data.id).then((updated) => {
        if (report.value?.id === updated.id) report.value = updated
      })
    }
  } catch (err) {
    error.value = err instanceof Error ? err.message : '加载周报失败'
  } finally {
    loading.value = false
  }
})

function applyReport(data: WeeklyReport) {
  report.value = data
  selectedWeek.value = data.facts.weekStart
}

async function loadWeek(weekStart: string) {
  if (weekStart > currentWeekStart.value || generating.value) return
  loading.value = true
  error.value = ''
  try {
    applyReport(await fetchWeeklyReport(weekStart))
  } catch (err) {
    error.value = err instanceof Error ? err.message : '加载周报失败'
  } finally {
    loading.value = false
  }
}

function moveWeek(offset: number) {
  const base = selectedWeek.value || currentWeekStart.value
  loadWeek(shiftWeek(base, offset))
}

async function regenerate() {
  if (!selectedWeek.value || generating.value) return
  generating.value = true
  error.value = ''
  try {
    applyReport(await regenerateWeeklyReport(selectedWeek.value))
  } catch (err) {
    error.value = err instanceof Error ? err.message : '重新生成周报失败'
  } finally {
    generating.value = false
  }
}

async function startRecommendedPractice() {
  if (!report.value || startingPractice.value) return
  startingPractice.value = true
  error.value = ''
  try {
    const session = await createWeeklyRecommendedPractice(report.value.id)
    await router.push(`/practice/${session.id}`)
  } catch (err) {
    error.value = err instanceof Error ? err.message : '生成推荐练习失败'
  } finally {
    startingPractice.value = false
  }
}

function mondayOf(date: Date) {
  const copy = new Date(date.getFullYear(), date.getMonth(), date.getDate())
  const day = copy.getDay()
  copy.setDate(copy.getDate() - (day === 0 ? 6 : day - 1))
  return formatDate(copy)
}

function shiftWeek(value: string, offset: number) {
  const date = parseDate(value)
  date.setDate(date.getDate() + offset * 7)
  return formatDate(date)
}

function formatDate(date: Date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function parseDate(value: string) {
  const [year, month, day] = value.split('-').map(Number)
  return new Date(year, month - 1, day)
}

function shortDate(value: string) {
  const date = parseDate(value)
  return `${date.getMonth() + 1}月${date.getDate()}日`
}

function weekRangeLabel() {
  if (!report.value) return ''
  return `${shortDate(report.value.facts.weekStart)} — ${shortDate(report.value.facts.weekEnd)}`
}

function deltaText(delta: number) {
  if (delta > 0) return `较上周 +${Number(delta).toFixed(1)} 个百分点`
  if (delta < 0) return `较上周 ${Number(delta).toFixed(1)} 个百分点`
  return '与上周持平'
}

function quotaText(type: QuestionType) {
  return report.value?.facts.recommendation.typeQuotas?.[type] || 0
}

const dailyOption = computed<EChartsOption>(() => ({
  animationDuration: 700,
  tooltip: {
    trigger: 'axis',
    backgroundColor: '#172554',
    borderWidth: 0,
    textStyle: { color: '#fff', fontSize: 12 },
  },
  legend: {
    top: 0,
    right: 0,
    itemWidth: 9,
    itemHeight: 9,
    textStyle: { color: '#94a3b8', fontSize: 10 },
  },
  grid: { left: 10, right: 8, top: 38, bottom: 8, containLabel: true },
  xAxis: {
    type: 'category',
    data: report.value?.facts.daily.map((item) => item.date) || [],
    axisLine: { lineStyle: { color: '#e2e8f0' } },
    axisTick: { show: false },
    axisLabel: { color: '#94a3b8', fontSize: 10 },
  },
  yAxis: [
    {
      type: 'value',
      name: '题量',
      nameTextStyle: { color: '#94a3b8', fontSize: 10 },
      minInterval: 1,
      axisLabel: { color: '#94a3b8', fontSize: 10 },
      splitLine: { lineStyle: { color: '#f1f5f9' } },
    },
    {
      type: 'value',
      min: 0,
      max: 100,
      axisLabel: { color: '#94a3b8', fontSize: 10, formatter: '{value}%' },
      splitLine: { show: false },
    },
  ],
  series: [
    {
      name: '完成题量',
      type: 'bar',
      barWidth: 18,
      data: report.value?.facts.daily.map((item) => item.questionCount) || [],
      itemStyle: { color: '#93c5fd', borderRadius: [7, 7, 0, 0] },
    },
    {
      name: '正确率',
      type: 'line',
      yAxisIndex: 1,
      smooth: 0.35,
      symbol: 'circle',
      symbolSize: 7,
      data: report.value?.facts.daily.map((item) => Number(item.accuracy)) || [],
      lineStyle: { width: 3, color: '#0d9488' },
      itemStyle: { color: '#fff', borderColor: '#0d9488', borderWidth: 2 },
    },
  ],
}))

const weakTypeOption = computed<EChartsOption>(() => {
  const rows = report.value?.facts.weakTypes || []
  return {
    animationDuration: 700,
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      backgroundColor: '#172554',
      borderWidth: 0,
      textStyle: { color: '#fff', fontSize: 12 },
    },
    grid: { left: 8, right: 42, top: 8, bottom: 8, containLabel: true },
    xAxis: {
      type: 'value',
      min: 0,
      max: 100,
      axisLabel: { color: '#94a3b8', fontSize: 10, formatter: '{value}%' },
      splitLine: { lineStyle: { color: '#f1f5f9' } },
    },
    yAxis: {
      type: 'category',
      data: rows.map((item) => item.label),
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: '#475569', fontSize: 11 },
    },
    series: [{
      type: 'bar',
      barWidth: 14,
      data: rows.map((item) => ({
        value: Number(item.accuracy),
        itemStyle: {
          color: item.accuracy >= 80 ? '#0d9488' : item.accuracy >= 60 ? '#f59e0b' : '#e11d48',
          borderRadius: [0, 7, 7, 0],
        },
      })),
      label: { show: true, position: 'right', color: '#64748b', fontSize: 10, formatter: '{c}%' },
    }],
  }
})
</script>

<template>
  <div class="weekly-page">
    <PageHeader
      eyebrow="WEEKLY LEARNING REPORT"
      title="把这一周的进步，看得更清楚"
      description="确定性数据负责统计与推荐，DeepSeek 只负责把事实写成更容易理解的总结。"
    >
      <div class="week-switcher">
        <button class="icon-button" title="上一周" @click="moveWeek(-1)"><ArrowLeft /></button>
        <span>{{ weekRangeLabel() || '正在读取周报' }}</span>
        <button class="icon-button" title="下一周" :disabled="isLatestWeek" @click="moveWeek(1)"><ArrowRight /></button>
      </div>
      <button class="btn btn-ghost" :disabled="generating || !selectedWeek" @click="regenerate">
        <RefreshCw :class="{ spin: generating }" /> {{ generating ? '生成中…' : '重新生成' }}
      </button>
    </PageHeader>

    <div v-if="error" class="alert">{{ error }}</div>
    <div v-if="loading || generating" class="loading-panel"><div class="loader" /></div>

    <template v-else-if="report">
      <section class="report-hero">
        <div class="hero-copy">
          <div class="hero-meta">
            <span class="week-pill"><CalendarDays /> {{ isCurrentWeek ? '本周进行中' : '历史周报' }}</span>
            <span class="source-pill" :class="{ ai: report.narrative.source === 'AGENT' }">
              <Bot v-if="report.narrative.source === 'AGENT'" />
              <Sparkles v-else />
              {{ report.narrative.source === 'AGENT' ? 'DeepSeek 生成' : '确定性模板' }}
            </span>
            <span v-if="report.narrative.model" class="model-name">{{ report.narrative.model }}</span>
          </div>
          <h2>{{ report.facts.overview.questionCount > 0 ? '这一周，每一次练习都在留下刻度。' : '新的一周，从第一道题开始。' }}</h2>
          <p class="hero-summary">{{ report.narrative.summary }}</p>
          <div class="narrative-columns">
            <div>
              <span class="narrative-kicker">本周亮点</span>
              <ul>
                <li v-for="item in report.narrative.highlights" :key="item">{{ item }}</li>
              </ul>
            </div>
            <div>
              <span class="narrative-kicker">下一步建议</span>
              <ul>
                <li v-for="item in report.narrative.suggestions" :key="item">{{ item }}</li>
              </ul>
            </div>
          </div>
        </div>
        <div class="hero-score">
          <span class="score-ring" :style="{ '--score': `${Number(report.facts.overview.accuracy)}%` }">
            <strong>{{ Number(report.facts.overview.accuracy).toFixed(1) }}%</strong>
            <small>本周正确率</small>
          </span>
          <span class="score-delta" :class="{ up: report.facts.overview.accuracyDelta > 0, down: report.facts.overview.accuracyDelta < 0 }">
            <ArrowUpRight v-if="report.facts.overview.accuracyDelta > 0" />
            <ArrowDownRight v-else-if="report.facts.overview.accuracyDelta < 0" />
            <Target v-else />
            {{ deltaText(report.facts.overview.accuracyDelta) }}
          </span>
        </div>
      </section>

      <section class="stats-grid weekly-stats">
        <StatCard label="本周完成" :value="`${report.facts.overview.questionCount} 题`" hint="按已提交练习统计" tone="blue">
          <template #icon><BookOpenCheck /></template>
        </StatCard>
        <StatCard label="练习次数" :value="`${report.facts.overview.sessionCount} 次`" hint="每次认真完成都算数" tone="green">
          <template #icon><Target /></template>
        </StatCard>
        <StatCard label="学习时长" :value="`${report.facts.overview.studyMinutes} 分钟`" hint="根据答题计时累计" tone="amber">
          <template #icon><Clock3 /></template>
        </StatCard>
        <StatCard label="连续练习" :value="`${report.facts.streakDays} 天`" hint="久久为功" tone="rose">
          <template #icon><Flame /></template>
        </StatCard>
      </section>

      <section class="section-grid two-column report-charts">
        <article class="card">
          <div class="card-header">
            <div><h2>每日学习节奏</h2><p>柱形是题量，折线是当天正确率</p></div>
            <span class="badge badge-blue">周一至周日</span>
          </div>
          <div class="card-body"><EChart :option="dailyOption" height="280px" /></div>
        </article>

        <article class="card">
          <div class="card-header">
            <div><h2>薄弱题型</h2><p>至少完成 3 道样本才进入判断</p></div>
          </div>
          <div class="card-body">
            <EChart v-if="report.facts.weakTypes.length" :option="weakTypeOption" height="250px" />
            <div v-else class="soft-empty">
              <Sparkles />
              <strong>暂无稳定薄弱题型</strong>
              <p>继续积累各题型样本，下周会给出更准确的判断。</p>
            </div>
          </div>
        </article>
      </section>

      <section class="section-grid two-column report-lower">
        <article class="card mistakes-card">
          <div class="card-header">
            <div><h2>高频错题</h2><p>按本周规范化题目聚合排序</p></div>
            <span class="badge badge-rose">{{ report.facts.frequentMistakes.length }} 题</span>
          </div>
          <div class="card-body">
            <div v-if="report.facts.frequentMistakes.length" class="mistake-list">
              <div v-for="(item, index) in report.facts.frequentMistakes" :key="`${item.expression}-${index}`" class="mistake-row">
                <span class="mistake-rank">{{ String(index + 1).padStart(2, '0') }}</span>
                <strong>{{ item.expression }} = ?</strong>
                <TypeBadge :type="item.type" />
                <span class="mistake-count">错 {{ item.errorCount }} 次</span>
                <span class="correct-answer">答案 {{ item.correctAnswer }}</span>
              </div>
            </div>
            <div v-else class="soft-empty compact">
              <Target />
              <strong>本周没有记录到错题</strong>
              <p>继续保持，也可以适当提升题目难度。</p>
            </div>
          </div>
        </article>

        <article class="recommendation-card">
          <div class="recommendation-top">
            <span class="recommendation-icon"><WandSparkles /></span>
            <div>
              <span class="eyebrow">NEXT PRACTICE</span>
              <h2>{{ report.facts.recommendation.config.title }}</h2>
            </div>
          </div>
          <p>{{ report.facts.recommendation.reason }}</p>
          <div class="quota-list">
            <div v-for="type in report.facts.recommendation.config.types" :key="type" class="quota-row">
              <span><TypeBadge :type="type" /></span>
              <strong>{{ quotaText(type) }} 题</strong>
            </div>
          </div>
          <div class="recommendation-meta">
            <span>共 {{ report.facts.recommendation.config.count }} 题</span>
            <span>{{ report.facts.recommendation.config.operandCount }} 个数运算</span>
            <span>{{ report.facts.recommendation.config.allowParentheses ? '含括号' : '无括号' }}</span>
          </div>
          <button class="btn btn-dark recommend-button" :disabled="startingPractice" @click="startRecommendedPractice">
            <Lightbulb />
            {{ startingPractice ? '正在生成…' : '一键开始推荐练习' }}
          </button>
          <small class="generated-time">生成于 {{ report.generatedAt }}</small>
        </article>
      </section>
    </template>
  </div>
</template>

<style scoped>
.weekly-page { max-width: 1320px; margin: 0 auto; }
.week-switcher {
  display: flex;
  min-height: 42px;
  align-items: center;
  gap: 4px;
  padding: 4px;
  border: 1px solid #dbe3ef;
  border-radius: 11px;
  background: #fff;
  color: #475569;
  font-size: 12px;
  font-weight: 700;
}
.week-switcher .icon-button { width: 31px; height: 31px; color: #64748b; }
.week-switcher .icon-button:disabled { opacity: .3; cursor: not-allowed; }
.week-switcher .icon-button svg { width: 15px; height: 15px; }
.report-hero {
  position: relative;
  overflow: hidden;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 240px;
  gap: 32px;
  margin-bottom: 18px;
  padding: 32px 36px;
  border-radius: 24px;
  background:
    radial-gradient(circle at 90% 12%, rgba(45, 212, 191, .26), transparent 25%),
    radial-gradient(circle at 15% 100%, rgba(96, 165, 250, .19), transparent 36%),
    linear-gradient(130deg, #111c3f, #172554 60%, #0f2f45);
  color: #fff;
  box-shadow: 0 20px 45px rgba(15, 23, 42, .16);
}
.hero-meta { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; }
.week-pill, .source-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  border: 1px solid rgba(255,255,255,.13);
  border-radius: 999px;
  background: rgba(255,255,255,.075);
  color: #cbd5e1;
  font-size: 10px;
  font-weight: 700;
}
.source-pill.ai { border-color: rgba(94,234,212,.26); background: rgba(20,184,166,.16); color: #99f6e4; }
.week-pill svg, .source-pill svg { width: 13px; height: 13px; }
.model-name { color: #7dd3fc; font-size: 10px; }
.hero-copy h2 { margin: 21px 0 9px; font-family: var(--serif); font-size: clamp(24px, 3vw, 34px); letter-spacing: -.03em; }
.hero-summary { max-width: 760px; margin: 0; color: #cbd5e1; font-size: 14px; line-height: 1.85; }
.narrative-columns { display: grid; grid-template-columns: 1fr 1fr; gap: 28px; margin-top: 24px; padding-top: 21px; border-top: 1px solid rgba(255,255,255,.09); }
.narrative-kicker { color: #7dd3fc; font-size: 10px; font-weight: 800; letter-spacing: .09em; }
.narrative-columns ul { display: grid; gap: 8px; margin: 10px 0 0; padding: 0; list-style: none; }
.narrative-columns li { position: relative; padding-left: 14px; color: #dbeafe; font-size: 12px; line-height: 1.65; }
.narrative-columns li::before { position: absolute; top: .68em; left: 0; width: 5px; height: 5px; border-radius: 50%; background: #2dd4bf; content: ''; }
.hero-score { display: grid; align-content: center; justify-items: center; gap: 16px; }
.score-ring {
  display: grid;
  width: 166px;
  height: 166px;
  place-items: center;
  align-content: center;
  border-radius: 50%;
  background:
    radial-gradient(circle, #172554 62%, transparent 63%),
    conic-gradient(#2dd4bf var(--score), rgba(255,255,255,.12) 0);
  box-shadow: inset 0 0 0 1px rgba(255,255,255,.08);
}
.score-ring strong { font-family: var(--serif); font-size: 37px; letter-spacing: -.04em; }
.score-ring small { margin-top: 5px; color: #94a3b8; font-size: 10px; }
.score-delta { display: inline-flex; align-items: center; gap: 5px; padding: 7px 11px; border-radius: 999px; background: rgba(255,255,255,.07); color: #cbd5e1; font-size: 10px; font-weight: 700; }
.score-delta.up { background: rgba(16,185,129,.14); color: #6ee7b7; }
.score-delta.down { background: rgba(244,63,94,.14); color: #fda4af; }
.score-delta svg { width: 13px; height: 13px; }
.weekly-stats { margin-bottom: 22px; }
.report-charts, .report-lower { align-items: stretch; }
.report-charts > .card, .report-lower > .card, .recommendation-card { min-height: 360px; }
.soft-empty { display: grid; min-height: 240px; place-content: center; justify-items: center; gap: 8px; color: #94a3b8; text-align: center; }
.soft-empty svg { width: 30px; height: 30px; color: #60a5fa; }
.soft-empty strong { color: #475569; font-size: 14px; }
.soft-empty p { max-width: 260px; margin: 0; font-size: 12px; line-height: 1.6; }
.soft-empty.compact { min-height: 220px; }
.mistake-list { display: grid; gap: 8px; }
.mistake-row { display: grid; grid-template-columns: 30px minmax(100px, 1fr) auto auto auto; align-items: center; gap: 10px; padding: 12px; border: 1px solid #eef2f7; border-radius: 12px; background: #fbfdff; }
.mistake-rank { color: #cbd5e1; font-family: var(--serif); font-size: 14px; }
.mistake-row strong { color: #172554; font-size: 13px; }
.mistake-count { color: #e11d48; font-size: 10px; font-weight: 700; }
.correct-answer { color: #64748b; font-size: 10px; }
.recommendation-card {
  position: relative;
  overflow: hidden;
  padding: 26px;
  border-radius: 20px;
  background: linear-gradient(150deg, #f0fdfa, #eff6ff 70%);
  border: 1px solid #ccfbf1;
  box-shadow: 0 12px 30px rgba(15, 23, 42, .06);
}
.recommendation-card::after { position: absolute; right: -35px; top: -35px; width: 130px; height: 130px; border-radius: 50%; background: rgba(45,212,191,.11); content: ''; }
.recommendation-top { position: relative; z-index: 1; display: flex; align-items: center; gap: 13px; }
.recommendation-icon { display: grid; width: 43px; height: 43px; place-items: center; border-radius: 13px; background: #0f766e; color: #fff; box-shadow: 0 8px 18px rgba(15,118,110,.22); }
.recommendation-icon svg { width: 21px; height: 21px; }
.eyebrow { color: #0f766e; font-size: 9px; font-weight: 800; letter-spacing: .12em; }
.recommendation-card h2 { margin: 3px 0 0; color: #134e4a; font-size: 18px; }
.recommendation-card > p { position: relative; z-index: 1; margin: 18px 0; color: #47666e; font-size: 12px; line-height: 1.75; }
.quota-list { position: relative; z-index: 1; display: grid; gap: 8px; padding: 13px; border: 1px solid rgba(13,148,136,.12); border-radius: 13px; background: rgba(255,255,255,.72); }
.quota-row { display: flex; align-items: center; justify-content: space-between; color: #0f766e; font-size: 12px; }
.recommendation-meta { position: relative; z-index: 1; display: flex; flex-wrap: wrap; gap: 8px; margin: 13px 0; color: #64748b; font-size: 10px; }
.recommendation-meta span { padding: 5px 8px; border-radius: 7px; background: rgba(255,255,255,.7); }
.recommend-button { position: relative; z-index: 1; width: 100%; }
.generated-time { position: relative; z-index: 1; display: block; margin-top: 11px; color: #94a3b8; font-size: 9px; text-align: center; }
@media (max-width: 1000px) {
  .report-hero { grid-template-columns: 1fr; }
  .hero-score { grid-auto-flow: column; justify-content: start; }
}
@media (max-width: 700px) {
  .report-hero { padding: 25px 21px; }
  .narrative-columns { grid-template-columns: 1fr; gap: 18px; }
  .hero-score { justify-content: center; }
  .score-ring { width: 138px; height: 138px; }
  .mistake-row { grid-template-columns: 25px 1fr auto; }
  .correct-answer { display: none; }
}
</style>
