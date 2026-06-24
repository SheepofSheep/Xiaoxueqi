<template>
  <div class="skeleton-list" aria-hidden="true">
    <article v-for="index in count" :key="index" class="skeleton-card">
      <div class="skeleton-img shimmer"></div>
      <div class="skeleton-body">
        <div class="skeleton-line title shimmer"></div>
        <div class="skeleton-line meta shimmer"></div>
        <div class="skeleton-tags">
          <span class="shimmer"></span>
          <span class="shimmer"></span>
          <span class="shimmer short"></span>
        </div>
      </div>
    </article>
  </div>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  count?: number
}>(), {
  count: 3,
})
</script>

<style scoped>
.skeleton-list {
  display: grid;
  gap: 12px;
}

.skeleton-card {
  display: flex;
  gap: 10px;
  padding: 10px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-card);
  background: var(--color-card);
  box-shadow: var(--shadow-card);
}

.skeleton-img {
  flex-shrink: 0;
  width: 82px;
  height: 82px;
  border-radius: 10px;
}

.skeleton-body {
  min-width: 0;
  flex: 1;
  padding-top: 3px;
}

.skeleton-line {
  height: 14px;
  border-radius: 999px;
}

.skeleton-line.title {
  width: 72%;
  height: 18px;
}

.skeleton-line.meta {
  width: 58%;
  margin-top: 12px;
}

.skeleton-tags {
  display: flex;
  gap: 7px;
  margin-top: 13px;
}

.skeleton-tags span {
  width: 52px;
  height: 24px;
  border-radius: 999px;
}

.skeleton-tags .short {
  width: 38px;
}

.shimmer {
  position: relative;
  overflow: hidden;
  background: var(--color-soft);
}

.shimmer::after {
  content: "";
  position: absolute;
  inset: 0;
  transform: translateX(-100%);
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.72), transparent);
  animation: shimmer 1.2s ease-in-out infinite;
}

@keyframes shimmer {
  100% {
    transform: translateX(100%);
  }
}

@media (prefers-reduced-motion: reduce) {
  .shimmer::after {
    animation: none;
  }
}
</style>
