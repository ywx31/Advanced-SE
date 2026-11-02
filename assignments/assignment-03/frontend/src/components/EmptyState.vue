<script setup lang="ts">
import { Sparkles } from 'lucide-vue-next'

defineProps<{
  title: string
  description?: string
}>()
</script>

<template>
  <div class="empty-state">
    <div class="empty-icon"><Sparkles /></div>
    <strong>{{ title }}</strong>
    <p v-if="description">{{ description }}</p>
    <slot />
  </div>
</template>

<style scoped>
.empty-state {
  display: grid;
  justify-items: center;
  gap: 10px;
  padding: 50px 20px;
  color: #64748b;
  text-align: center;
}
.empty-icon {
  display: grid;
  width: 58px;
  height: 58px;
  place-items: center;
  border-radius: 20px;
  background: linear-gradient(135deg, #e0f2fe, #eef2ff);
  color: #2563eb;
}
.empty-icon svg { width: 27px; height: 27px; }
.empty-state strong { color: #1e293b; font-size: 17px; }
.empty-state p { max-width: 360px; margin: 0; font-size: 14px; line-height: 1.7; }
</style>
