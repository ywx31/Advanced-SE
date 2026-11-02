<script setup lang="ts">
import { computed } from 'vue'
import { QUESTION_TYPE_LABELS } from '../types'
import type { QuestionType } from '../types'

const props = defineProps<{ type: QuestionType }>()
const label = computed(() => QUESTION_TYPE_LABELS[props.type] || props.type)
</script>

<template>
  <span class="type-badge" :class="`type-${type.toLowerCase()}`">{{ label }}</span>
</template>

<style scoped>
.type-badge {
  display: inline-flex;
  align-items: center;
  padding: 4px 8px;
  border-radius: 999px;
  background: #eff6ff;
  color: #2563eb;
  font-size: 9px;
  font-weight: 800;
  white-space: nowrap;
}
.type-table_division { background: #ecfdf5; color: #047857; }
.type-advanced_multiplication { background: #fffbeb; color: #b45309; }
.type-advanced_division { background: #fff1f2; color: #be123c; }
.type-mixed_operations { background: #f5f3ff; color: #6d28d9; }
</style>
