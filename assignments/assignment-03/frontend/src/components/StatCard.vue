<script setup lang="ts">
defineProps<{
  label: string
  value: string | number
  hint?: string
  tone?: 'blue' | 'green' | 'amber' | 'rose'
}>()
</script>

<template>
  <article class="stat-card" :class="`tone-${tone || 'blue'}`">
    <div class="stat-icon"><slot name="icon" /></div>
    <div class="stat-content">
      <span class="stat-label">{{ label }}</span>
      <strong class="stat-value">{{ value }}</strong>
      <span v-if="hint" class="stat-hint">{{ hint }}</span>
    </div>
  </article>
</template>

<style scoped>
.stat-card {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  min-height: 132px;
  padding: 22px;
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 20px;
  background: #fff;
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.06);
}
.stat-icon {
  display: grid;
  flex: 0 0 46px;
  width: 46px;
  height: 46px;
  place-items: center;
  border-radius: 14px;
  background: #eff6ff;
  color: #2563eb;
}
.stat-icon :deep(svg) { width: 23px; height: 23px; }
.stat-content { display: grid; gap: 3px; }
.stat-label { color: #64748b; font-size: 13px; }
.stat-value { color: #0f172a; font-size: 29px; line-height: 1.25; letter-spacing: -0.03em; }
.stat-hint { color: #94a3b8; font-size: 12px; }
.tone-green .stat-icon { background: #ecfdf5; color: #059669; }
.tone-amber .stat-icon { background: #fffbeb; color: #d97706; }
.tone-rose .stat-icon { background: #fff1f2; color: #e11d48; }
</style>
