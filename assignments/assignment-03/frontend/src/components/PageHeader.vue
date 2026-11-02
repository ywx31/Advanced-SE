<script setup lang="ts">
defineProps<{
  eyebrow?: string
  title: string
  description?: string
}>()
</script>

<template>
  <header class="page-header">
    <div>
      <span v-if="eyebrow" class="page-eyebrow">{{ eyebrow }}</span>
      <h1>{{ title }}</h1>
      <p v-if="description">{{ description }}</p>
    </div>
    <div class="page-actions"><slot /></div>
  </header>
</template>

<style scoped>
.page-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 26px;
}
.page-eyebrow {
  display: inline-block;
  margin-bottom: 8px;
  color: #2563eb;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}
h1 {
  margin: 0;
  color: #0f172a;
  font-size: clamp(26px, 3vw, 36px);
  line-height: 1.18;
  letter-spacing: -0.035em;
}
p { margin: 8px 0 0; color: #64748b; font-size: 14px; line-height: 1.7; }
.page-actions { display: flex; flex-wrap: wrap; gap: 10px; }
@media (max-width: 700px) {
  .page-header { align-items: flex-start; flex-direction: column; }
}
</style>
