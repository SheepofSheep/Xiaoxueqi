<template>
  <section :class="['app-state-card', variant]" role="status">
    <div class="state-icon" aria-hidden="true">
      <van-icon :name="iconName" size="28" />
    </div>
    <strong>{{ title }}</strong>
    <p>{{ description }}</p>
    <button v-if="actionText" type="button" @click="$emit('action')">
      {{ actionText }}
    </button>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{
  title: string
  description: string
  actionText?: string
  icon?: string
  variant?: 'empty' | 'error'
}>(), {
  actionText: '',
  icon: '',
  variant: 'empty',
})

defineEmits<{
  action: []
}>()

const iconName = computed(() => props.icon || (props.variant === 'error' ? 'warning-o' : 'search'))
</script>

<style scoped>
.app-state-card {
  min-height: 188px;
  padding: 22px 18px;
  border: 1px solid var(--color-border);
  border-radius: 14px;
  display: grid;
  justify-items: center;
  align-content: center;
  gap: 10px;
  text-align: center;
  background: var(--color-card);
  box-shadow: var(--shadow-card);
}

.state-icon {
  width: 64px;
  height: 64px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  color: var(--color-primary);
  background: var(--color-primary-light);
}

.app-state-card.error .state-icon {
  color: var(--color-danger);
  background: #fff2ee;
}

.app-state-card strong {
  color: var(--color-text);
  font-size: 17px;
  line-height: 1.35;
}

.app-state-card p {
  max-width: 260px;
  margin: 0;
  color: var(--color-muted);
  font-size: 13px;
  line-height: 1.5;
}

.app-state-card button {
  min-width: 128px;
  min-height: 44px;
  margin-top: 6px;
  border: 0;
  border-radius: 12px;
  color: #fff;
  background: var(--color-primary);
  font: inherit;
  font-weight: 900;
  cursor: pointer;
}

.app-state-card.error button {
  background: var(--color-danger);
}

.app-state-card button:active {
  transform: scale(0.97);
}
</style>
