<template>
  <section :class="['app-inline-message', variant, { actionable: actionText }]" :role="roleName">
    <van-icon :name="iconName" size="18" aria-hidden="true" />
    <span>{{ message }}</span>
    <button
      v-if="actionText"
      type="button"
      :disabled="actionDisabled"
      @click="$emit('action')"
    >
      {{ actionText }}
    </button>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{
  message: string
  variant?: 'info' | 'error'
  icon?: string
  actionText?: string
  actionDisabled?: boolean
}>(), {
  variant: 'info',
  icon: '',
  actionText: '',
  actionDisabled: false,
})

defineEmits<{
  action: []
}>()

const iconName = computed(() => props.icon || (props.variant === 'error' ? 'warning-o' : 'info-o'))
const roleName = computed(() => props.variant === 'error' ? 'alert' : 'status')
</script>

<style scoped>
.app-inline-message {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 9px;
  align-items: flex-start;
  padding: 11px 12px;
  border-radius: 18px;
  color: #7d6558;
  background: #fffaf7;
  font-size: 13px;
  line-height: 1.55;
}

.app-inline-message.actionable {
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
}

.app-inline-message .van-icon {
  flex-shrink: 0;
  margin-top: 1px;
  color: var(--color-primary);
}

.app-inline-message.error {
  color: var(--color-danger);
  background: #fff2ee;
}

.app-inline-message.error .van-icon {
  color: var(--color-danger);
}

.app-inline-message button {
  min-height: 44px;
  border: 0;
  border-radius: 999px;
  padding: 0 12px;
  color: #fff;
  background: var(--color-primary);
  font: inherit;
  font-size: 12px;
  font-weight: 900;
  cursor: pointer;
}

.app-inline-message.error button {
  background: var(--color-danger);
}

.app-inline-message button:disabled {
  opacity: 0.62;
  cursor: wait;
}

.app-inline-message button:active {
  transform: scale(0.97);
}

@media (max-width: 374px) {
  .app-inline-message.actionable {
    grid-template-columns: auto minmax(0, 1fr);
  }

  .app-inline-message.actionable button {
    grid-column: 2;
    width: max-content;
  }
}
</style>
