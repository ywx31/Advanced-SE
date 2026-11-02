<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import type { EChartsOption } from 'echarts'
import {
  ArrowUpRight,
  BookOpenCheck,
  ClipboardPenLine,
  Flame,
  Target,
  Trophy,
} from 'lucide-vue-next'
import PageHeader from '../components/PageHeader.vue'
import StatCard from '../components/StatCard.vue'
import EChart from '../components/EChart.vue'
import EmptyState from '../components/EmptyState.vue'
import ActivityHeatmap from '../components/ActivityHeatmap.vue'
import { fetchDashboard } from '../api/stats'
import type { DashboardData } from '../types'

const data = ref<DashboardData | null>(null)
const loading = ref(true)
const error = ref('')

onMounted(load)

async function load() {
  loading.value = true
  error.value = ''
  try {
    data.value = await fetchDashboard()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '加载失败'
  } finally {
    loading.value = false
  }
}

const trendOption = computed<EChartsOption>(() => ({
  animationDuration: 700,
  tooltip: {
    trigger: 'axis',
    backgroundColor: '#172554',
    borderWidth: 0,
    textStyle: { color: '#fff', fontSize: 12 },
    formatter: (params: unknown) => {
      const first = Array.isArray(params) ? params[0] as { axisValue: string; value: number } : null
      return first ? `${first.axisValue}<br/>正确率 <b>${first.value}%</b>` : ''
    },
  },
  grid: { left: 12, right: 18, top: 20, bottom: 15, containLabel: true },
  xAxis: {
    type: 'category',
    boundaryGap: false,
    data: data.value?.trend.map((item) => item.label) || [],
    axisLine: { lineStyle: { color: '#e2e8f0' } },
    axisTick: { show: false },
    axisLabel: { color: '#94a3b8', fontSize: 10 },
  },
  yAxis: {
    type: 'value',
    min: 0,
    max: 100,
    interval: 25,
    axisLabel: { color: '#94a3b8', fontSize: 10, formatter: '{value}%' },
    splitLine: { lineStyle: { color: '#f1f5f9' } },
  },
  series: [{
    type: 'line',
    smooth: 0.35,
    symbol: 'circle',
    symbolSize: 7,
    data: data.value?.trend.map((item) => Number(item.accuracy)) || [],
    lineStyle: { width: 3, color: '#2563eb' },
    itemStyle: { color: '#fff', borderColor: '#2563eb', borderWidth: 2 },
    areaStyle: {
      color: {
        type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
        colorStops: [
          { offset: 0, color: 'rgba(37,99,235,.22)' },
          { offset: 1, color: 'rgba(37,99,235,0)' },
        ],
      },
    },
  }],
}))

const typeOption = computed<EChartsOption>(() => {
  const rows = data.value?.typeAccuracy || []
  return {
    animationDuration: 700,
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      backgroundColor: '#172554',
      borderWidth: 0,
      textStyle: { color: '#fff', fontSize: 12 },
      formatter: (params: unknown) => {
        const first = Array.isArray(params) ? params[0] as { name: string; value: number } : null
        return first ? `${first.name}<br/>正确率 <b>${first.value}%</b>` : ''
      },
    },
    grid: { left: 6, right: 28, top: 10, bottom: 10, containLabel: true },
    xAxis: {
      type: 'value', min: 0, max: 100,
      axisLabel: { color: '#94a3b8', fontSize: 10, formatter: '{value}%' },
      splitLine: { lineStyle: { color: '#f1f5f9' } },
    },
    yAxis: {
      type: 'category',
      data: rows.map((row) => row.label),
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: '#475569', fontSize: 11 },
    },
    series: [{
      type: 'bar',
      data: rows.map((row) => ({ value: Number(row.accuracy), itemStyle: { color: row.accuracy >= 80 ? '#0d9488' : row.accuracy >= 60 ? '#f59e0b' : '#e11d48', borderRadius: [0, 6, 6, 0] } })),
      barWidth: 13,
      label: { show: true, position: 'right', color: '#64748b', fontSize: 10, formatter: '{c}%' },
    }],
  }
})

const overview = computed(() => data.value?.overview)
</script>

<template>
  <div>
    <PageHeader
      eyebrow="LEARNING OVERVIEW"
      title="今天也来练几道吧"
      description="从准确率、题型掌握和错题变化中，找到下一步最值得练习的方向。"
    >
      <RouterLink to="/practice/new" class="btn btn-primary">
        <ClipboardPenLine /> 生成新练习
      </RouterLink>
    </PageHeader>

    <div v-if="error" class="alert">{{ error }}</div>
    <div v-if="loading" class="loading-panel"><div class="loader" /></div>

    <template v-else-if="data && overview">
      <section class="stats-grid">
        <StatCard label="平均正确率" :value="`${Number(overview.averageAccuracy || 0).toFixed(1)}%`" hint="按累计答题数计算" tone="blue">
          <template #icon><Target /></template>
        </StatCard>
        <StatCard label="累计答题" :value="overview.totalQuestions || 0" hint="每道题都算一次成长" tone="green">
          <template #icon><BookOpenCheck /></template>
        </StatCard>
        <StatCard label="连续练习" :value="`${data.streakDays} 天`" hint="今天完成练习即可延续" tone="amber">
          <template #icon><Flame /></template>
        </StatCard>
        <StatCard label="待攻克错题" :value="overview.activeWrongQuestions || 0" hint="建议优先复习高频错题" tone="rose">
          <template #icon><Trophy /></template>
        </StatCard>
      </section>

      <ActivityHeatmap :days="data.activity" />

      <section class="section-grid two-column dashboard-grid">
        <article class="card">
          <div class="card-header">
            <div>
              <h2>正确率趋势</h2>
              <p>最近 {{ data.trend.length }} 次练习表现</p>
            </div>
            <span class="badge badge-blue"><ArrowUpRight /> 持续观察</span>
          </div>
          <div class="card-body">
            <EChart v-if="data.trend.length" :option="trendOption" height="290px" />
            <EmptyState v-else title="还没有趋势数据" description="完成第一次练习后，这里会展示你的正确率变化。">
              <RouterLink to="/practice/new" class="btn btn-soft btn-sm">开始第一次练习</RouterLink>
            </EmptyState>
          </div>
        </article>

        <article class="card">
          <div class="card-header">
            <div>
              <h2>题型掌握度</h2>
              <p>正确率较低的题型排在前面</p>
            </div>
          </div>
          <div class="card-body">
            <EChart v-if="data.typeAccuracy.length" :option="typeOption" height="290px" />
            <EmptyState v-else title="暂无题型分析" description="多完成几种题型后，就能看到掌握情况。" />
          </div>
        </article>
      </section>

    </template>
  </div>
</template>

<style scoped>
.dashboard-grid { margin-top: 22px; grid-template-columns: minmax(0, 1.35fr) minmax(320px, .9fr); }
@media (max-width: 1100px) {
  .dashboard-grid { grid-template-columns: 1fr; }
}
</style>
