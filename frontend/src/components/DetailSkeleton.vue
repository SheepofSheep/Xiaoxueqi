<template>
  <article class="detail-skeleton" aria-hidden="true">
    <section class="hero shimmer"></section>

    <section class="panel title-panel">
      <div class="line title shimmer"></div>
      <div class="line meta shimmer"></div>
      <div class="fact-grid">
        <span class="shimmer"></span>
        <span class="shimmer"></span>
        <span class="shimmer"></span>
      </div>
      <div class="action-row">
        <span class="shimmer"></span>
        <span class="shimmer"></span>
      </div>
    </section>

    <section class="panel">
      <div class="line section-title shimmer"></div>
      <div class="tag-row">
        <span class="shimmer"></span>
        <span class="shimmer"></span>
        <span class="shimmer short"></span>
      </div>
    </section>

    <section class="panel">
      <div class="line section-title shimmer"></div>
      <div class="info-row shimmer"></div>
      <div class="info-row shimmer"></div>
    </section>
  </article>
</template>

<style scoped>
.detail-skeleton {
  display: grid;
  gap: 16px;
  margin-top: 8px;
}

.hero {
  min-height: 286px;
  border-radius: 30px;
  box-shadow: 0 22px 56px rgba(75, 35, 14, 0.12);
}

.panel {
  padding: 16px;
  border: 1px solid rgba(93, 45, 22, 0.08);
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 14px 38px rgba(75, 35, 14, 0.06);
}

.title-panel {
  padding: 18px;
}

.line,
.fact-grid span,
.action-row span,
.tag-row span,
.info-row {
  border-radius: 999px;
}

.line.title {
  width: 68%;
  height: 24px;
}

.line.meta {
  width: 52%;
  height: 14px;
  margin-top: 12px;
}

.line.section-title {
  width: 36%;
  height: 18px;
}

.fact-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  margin-top: 18px;
}

.fact-grid span {
  height: 72px;
}

.action-row {
  display: grid;
  grid-template-columns: 1.2fr 1fr;
  gap: 10px;
  margin-top: 16px;
}

.action-row span {
  height: 44px;
}

.tag-row {
  display: flex;
  gap: 8px;
  margin-top: 14px;
}

.tag-row span {
  width: 62px;
  height: 30px;
}

.tag-row .short {
  width: 46px;
}

.info-row {
  height: 62px;
  margin-top: 12px;
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
