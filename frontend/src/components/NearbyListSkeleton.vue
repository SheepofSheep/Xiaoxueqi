<template>
  <section class="nearby-skeleton" aria-hidden="true">
    <article v-for="index in count" :key="index" class="near-card">
      <div class="near-img shimmer"></div>
      <div class="near-body">
        <div class="near-top">
          <span class="line title shimmer"></span>
          <span class="line distance shimmer"></span>
        </div>
        <span class="line meta shimmer"></span>
        <span class="line address shimmer"></span>
        <div class="near-foot">
          <span class="badge shimmer"></span>
          <span class="button shimmer"></span>
        </div>
      </div>
    </article>
  </section>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  count?: number
}>(), {
  count: 4,
})
</script>

<style scoped>
.nearby-skeleton {
  display: grid;
  gap: 12px;
}

.near-card {
  display: flex;
  gap: 12px;
  padding: 12px;
  border: 1px solid rgba(93, 45, 22, 0.08);
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 12px 34px rgba(64, 32, 15, 0.06);
}

.near-img {
  flex: 0 0 82px;
  width: 82px;
  height: 82px;
  border-radius: 18px;
}

.near-body {
  min-width: 0;
  flex: 1;
  padding-top: 2px;
}

.near-top {
  display: flex;
  justify-content: space-between;
  gap: 10px;
}

.line,
.badge,
.button {
  display: block;
  border-radius: 999px;
}

.title {
  width: 58%;
  height: 17px;
}

.distance {
  width: 42px;
  height: 15px;
  flex-shrink: 0;
}

.meta {
  width: 46%;
  height: 13px;
  margin-top: 11px;
}

.address {
  width: 78%;
  height: 12px;
  margin-top: 10px;
}

.near-foot {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
}

.badge {
  width: 62px;
  height: 24px;
}

.button {
  width: 56px;
  height: 36px;
  margin-left: auto;
}

.shimmer {
  position: relative;
  overflow: hidden;
  background: #fff0e5;
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
