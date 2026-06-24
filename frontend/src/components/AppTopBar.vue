<script setup lang="ts">
import { useRouter } from 'vue-router'

withDefaults(defineProps<{
  title: string
  subtitle?: string
  rightText?: string
}>(), {
  subtitle: '',
  rightText: '',
})

const router = useRouter()
</script>

<template>
  <header class="app-topbar">
    <button class="topbar-back" type="button" aria-label="返回" @click="router.back()">
      <van-icon name="arrow-left" size="20" />
    </button>
    <div class="topbar-title">
      <strong>{{ title }}</strong>
      <span v-if="subtitle">{{ subtitle }}</span>
    </div>
    <div class="topbar-right" aria-hidden="true">
      <span v-if="rightText">{{ rightText }}</span>
    </div>
  </header>
</template>

<style scoped>
.app-topbar {
  min-height: 48px;
  display: grid;
  grid-template-columns: 44px 1fr 44px;
  gap: 8px;
  align-items: center;
  margin: -2px 0 14px;
}

.topbar-back {
  width: 44px;
  height: 44px;
  border: 1px solid var(--color-border);
  border-radius: 12px;
  display: grid;
  place-items: center;
  color: var(--color-text);
  background: var(--color-card);
  box-shadow: var(--shadow-card);
  cursor: pointer;
}

.topbar-title {
  min-width: 0;
  text-align: center;
}

.topbar-title strong {
  display: block;
  color: var(--color-text);
  font-size: 17px;
  line-height: 1.25;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.topbar-title span {
  display: block;
  margin-top: 2px;
  color: var(--color-muted);
  font-size: 11px;
  line-height: 1.25;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.topbar-right {
  min-width: 44px;
  color: var(--color-primary);
  font-size: 12px;
  font-weight: 800;
  text-align: right;
}

.topbar-back:active {
  transform: scale(0.96);
}
</style>
