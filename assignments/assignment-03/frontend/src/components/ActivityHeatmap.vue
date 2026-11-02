<script setup lang="ts">
import { computed, ref } from 'vue'
import { CalendarDays, Flame, Target } from 'lucide-vue-next'
import type { ActivityDay } from '../types'

interface HeatmapCell {
  key: string
  date: Date
  questionCount: number
  sessionCount: number
  accuracy: number
  level: number
  future: boolean
}

const props = defineProps<{ days: ActivityDay[] }>()

const pad = (value: number) => String(value).padStart(2, '0')
const toKey = (date: Date) => `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
const addDays = (date: Date, amount: number) => {
  const next = new Date(date)
  next.setDate(next.getDate() + amount)
  next.setHours(0, 0, 0, 0)
  return next
}
const parseDate = (value: string) => {
  const [year, month, day] = value.split('-').map(Number)
  return new Date(year, month - 1, day)
}
const formatDay = (value: Date) =>
  `${value.getFullYear()}年${value.getMonth() + 1}月${value.getDate()}日`

const activityMap = computed(() => {
  const map = new Map<string, ActivityDay>()
  for (const item of props.days || []) {
    map.set(item.date, item)
  }
  return map
})

const today = new Date()
today.setHours(0, 0, 0, 0)
const rangeStart = addDays(today, -364)
const gridStart = addDays(rangeStart, -rangeStart.getDay())
const gridEnd = addDays(today, 6 - today.getDay())

const cells = computed<HeatmapCell[]>(() => {
  const result: HeatmapCell[] = []
  let cursor = new Date(gridStart)
  while (cursor <= gridEnd) {
    const key = toKey(cursor)
    const item = activityMap.value.get(key)
    const count = item?.questionCount || 0
    let level = 0
    if (count >= 40) level = 4
    else if (count >= 20) level = 3
    else if (count >= 10) level = 2
    else if (count >= 1) level = 1
    result.push({
      key,
      date: new Date(cursor),
      questionCount: count,
      sessionCount: item?.sessionCount || 0,
      accuracy: Number(item?.accuracy || 0),
      level,
      future: cursor > today,
    })
    cursor = addDays(cursor, 1)
  }
  return result
})

const weekCount = computed(() => Math.ceil(cells.value.length / 7))
const weekdayLabels = ['', '一', '', '三', '', '五', '']

const monthLabels = computed(() => {
  const labels: Array<{ label: string; column: number }> = []
  let lastMonth = -1
  let lastColumn = -10
  for (let week = 0; week < weekCount.value; week += 1) {
    const weekStart = addDays(gridStart, week * 7)
    if (weekStart.getMonth() !== lastMonth) {
      if (week === 0 || week - lastColumn >= 3) {
        labels.push({ label: `${weekStart.getMonth() + 1}月`, column: week + 1 })
        lastColumn = week
      }
      lastMonth = weekStart.getMonth()
    }
  }
  return labels
})

const selected = ref<HeatmapCell | null>(null)
const totalQuestions = computed(() =>
  (props.days || []).reduce((sum, item) => sum + Number(item.questionCount || 0), 0),
)
const activeDays = computed(() => (props.days || []).filter((item) => item.questionCount > 0).length)
const bestDay = computed(() => {
  const sorted = [...(props.days || [])].sort((a, b) => b.questionCount - a.questionCount)
  return sorted[0] || null
})
const rangeLabel = computed(
  () => `${rangeStart.getFullYear()}.${pad(rangeStart.getMonth() + 1)}.${pad(rangeStart.getDate())} — ${today.getFullYear()}.${pad(today.getMonth() + 1)}.${pad(today.getDate())}`,
)
const detailText = computed(() => {
  if (!selected.value) return '悬停或点击方块，查看当天练习详情'
  if (selected.value.future) return '未来日期'
  if (!selected.value.questionCount) return `${formatDay(selected.value.date)} · 当天没有练习`
  return `${formatDay(selected.value.date)} · ${selected.value.questionCount} 道题 · 正确率 ${selected.value.accuracy.toFixed(1)}%`
})

function selectCell(cell: HeatmapCell) {
  if (!cell.future) selected.value = cell
}

function cellTitle(cell: HeatmapCell) {
  if (cell.future) return ''
  if (!cell.questionCount) return `${toKey(cell.date)}：无练习`
  return `${toKey(cell.date)}：${cell.questionCount} 题，${cell.sessionCount} 次练习，正确率 ${cell.accuracy.toFixed(1)}%`
}
</script>

<template>
  <article class="card activity-card">
    <header class="activity-header">
      <div class="activity-heading">
        <span class="activity-icon"><CalendarDays /></span>
        <div>
          <h2>练习热力图</h2>
          <p>过去一年每天完成的题目数，颜色越深，练得越多</p>
        </div>
      </div>
      <div class="activity-metrics">
        <div><Flame /><span><strong>{{ totalQuestions }}</strong>道题</span></div>
        <div><Target /><span><strong>{{ activeDays }}</strong>天有练习</span></div>
        <div class="best-day">
          最高纪录 <strong>{{ bestDay?.questionCount || 0 }}</strong> 题/天
        </div>
      </div>
    </header>

    <div class="heatmap-scroll">
      <div class="heatmap-layout">
        <div class="weekday-labels">
          <span v-for="(label, index) in weekdayLabels" :key="index">{{ label }}</span>
        </div>
        <div class="heatmap-main">
          <div
            class="month-labels"
            :style="{ gridTemplateColumns: `repeat(${weekCount}, 12px)` }"
          >
            <span
              v-for="month in monthLabels"
              :key="`${month.label}-${month.column}`"
              :style="{ gridColumnStart: month.column }"
            >{{ month.label }}</span>
          </div>
          <div
            class="heatmap-grid"
            :style="{ gridTemplateColumns: `repeat(${weekCount}, 12px)` }"
          >
            <button
              v-for="cell in cells"
              :key="cell.key"
              type="button"
              class="heat-cell"
              :class="[`level-${cell.level}`, { selected: selected?.key === cell.key, future: cell.future }]"
              :title="cellTitle(cell)"
              :aria-label="cellTitle(cell)"
              @mouseenter="selectCell(cell)"
              @focus="selectCell(cell)"
              @click="selectCell(cell)"
            />
          </div>
        </div>
      </div>
    </div>

    <footer class="activity-footer">
      <span class="activity-detail">{{ detailText }}</span>
      <div class="heat-legend">
        <span>少</span>
        <i class="heat-cell level-0" title="0 题" />
        <i class="heat-cell level-1" title="1～9 题" />
        <i class="heat-cell level-2" title="10～19 题" />
        <i class="heat-cell level-3" title="20～39 题" />
        <i class="heat-cell level-4" title="40 题以上" />
        <span>多</span>
        <small>{{ rangeLabel }}</small>
      </div>
    </footer>
  </article>
</template>

<style scoped>
.activity-card {
  margin-top: 22px;
  padding: 23px 25px 18px;
  overflow: hidden;
}
.activity-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 22px;
  margin-bottom: 20px;
}
.activity-heading { display: flex; align-items: center; gap: 13px; }
.activity-icon {
  display: grid;
  flex: 0 0 42px;
  width: 42px;
  height: 42px;
  place-items: center;
  border-radius: 13px;
  background: #ecfdf5;
  color: #059669;
}
.activity-icon svg { width: 20px; height: 20px; }
.activity-heading h2 { margin: 0; color: #1e293b; font-size: 17px; }
.activity-heading p { margin: 5px 0 0; color: #94a3b8; font-size: 11px; }
.activity-metrics { display: flex; align-items: center; gap: 20px; padding-top: 3px; }
.activity-metrics > div { display: flex; align-items: center; gap: 6px; color: #94a3b8; font-size: 9px; white-space: nowrap; }
.activity-metrics svg { width: 14px; height: 14px; color: #0d9488; }
.activity-metrics strong { margin-right: 2px; color: #334155; font-size: 14px; }
.best-day { padding-left: 18px; border-left: 1px solid #e2e8f0; }
.best-day strong { color: #059669; }
.heatmap-scroll { overflow-x: auto; padding: 2px 1px 10px; scrollbar-width: thin; }
.heatmap-layout { display: flex; width: max-content; min-width: 100%; gap: 8px; }
.weekday-labels {
  display: grid;
  flex: 0 0 16px;
  grid-template-rows: repeat(7, 12px);
  gap: 3px;
  padding-top: 24px;
  color: #94a3b8;
  font-size: 8px;
  line-height: 12px;
  text-align: right;
}
.heatmap-main { display: grid; gap: 6px; }
.month-labels {
  display: grid;
  gap: 3px;
  height: 18px;
  color: #94a3b8;
  font-size: 8px;
  line-height: 18px;
}
.month-labels span { white-space: nowrap; }
.heatmap-grid {
  display: grid;
  grid-auto-flow: column;
  grid-template-rows: repeat(7, 12px);
  grid-auto-columns: 12px;
  gap: 3px;
}
.heat-cell {
  display: block;
  width: 12px;
  height: 12px;
  padding: 0;
  border: 1px solid rgba(27, 31, 35, .06);
  border-radius: 3px;
  background: #ebedf0;
  cursor: pointer;
  transition: transform .12s ease, box-shadow .12s ease, outline-color .12s ease;
}
button.heat-cell:hover,
button.heat-cell:focus-visible,
button.heat-cell.selected {
  z-index: 2;
  outline: 1px solid #172554;
  outline-offset: 1px;
  transform: scale(1.15);
}
.heat-cell.level-1 { background: #9be9a8; }
.heat-cell.level-2 { background: #40c463; }
.heat-cell.level-3 { background: #30a14e; }
.heat-cell.level-4 { background: #216e39; }
.heat-cell.future { visibility: hidden; }
.activity-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  margin-top: 8px;
  padding-top: 14px;
  border-top: 1px solid #f1f5f9;
  color: #94a3b8;
  font-size: 9px;
}
.activity-detail { min-width: 240px; color: #64748b; font-size: 10px; }
.heat-legend { display: flex; align-items: center; gap: 4px; }
.heat-legend .heat-cell { width: 12px; height: 12px; border: 1px solid rgba(27,31,35,.06); border-radius: 3px; }
.heat-legend small { margin-left: 12px; color: #a1aec2; font-size: 8px; }
@media (max-width: 900px) {
  .activity-header { flex-direction: column; gap: 14px; }
  .activity-metrics { flex-wrap: wrap; }
  .best-day { padding-left: 0; border-left: 0; }
  .activity-footer { align-items: flex-start; flex-direction: column; gap: 10px; }
}
</style>
