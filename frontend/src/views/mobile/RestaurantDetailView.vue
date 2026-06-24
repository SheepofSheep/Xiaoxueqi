<script setup lang="ts">
import { onMounted, ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppTopBar from '../../components/AppTopBar.vue'
import AppStateCard from '../../components/AppStateCard.vue'
import BottomNav from '../../components/BottomNav.vue'
import DetailSkeleton from '../../components/DetailSkeleton.vue'
import { getErrorMessage } from '../../api/client'
import { getRestaurantDetail } from '../../api/restaurants'
import { useFavoriteStore } from '../../stores/favorites'
import type { Restaurant } from '../../types/api'

const route = useRoute()
const router = useRouter()
const favoriteStore = useFavoriteStore()
const restaurant = ref<Restaurant | null>(null)
const error = ref('')
const imageFailed = ref(false)

const tasteTags = computed(() => restaurant.value?.tasteTags || [])
const sceneTags = computed(() => restaurant.value?.sceneTags || [])
const dishes = computed(() => restaurant.value?.recommendedDishes || [])
const isFavorite = computed(() => restaurant.value ? favoriteStore.isFavorite(restaurant.value.id) : false)

function toggleFavorite() {
  if (restaurant.value) {
    favoriteStore.toggleFavorite(restaurant.value.id)
  }
}

function openNavigation() {
  const target = restaurant.value
  if (!target?.longitude || !target.latitude) return
  window.open(
    `https://uri.amap.com/navigation?to=${target.longitude},${target.latitude},${encodeURIComponent(target.name)}&mode=bus&callnative=1`,
    '_blank',
  )
}

onMounted(async () => {
  try {
    restaurant.value = await getRestaurantDetail(String(route.params.id))
    imageFailed.value = false
    favoriteStore.addHistory(restaurant.value.id)
  } catch (err) {
    error.value = getErrorMessage(err, '餐厅详情加载失败')
  }
})
</script>

<template>
  <main class="mobile-page detail-shell">
    <AppTopBar title="餐厅详情" subtitle="到店前先看清楚" />
    <AppStateCard
      v-if="error"
      variant="error"
      title="餐厅详情暂时打不开"
      :description="error"
      action-text="回到发现"
      @action="router.push('/discover')"
    />

    <article v-else-if="restaurant" class="detail-page">
      <section class="detail-hero">
        <img
          v-if="restaurant.coverImage && !imageFailed"
          :src="restaurant.coverImage"
          class="hero-img"
          :alt="restaurant.name"
          @error="imageFailed = true"
        />
        <span v-else class="hero-placeholder" aria-hidden="true">
          <van-icon name="shop-o" size="52" />
        </span>
        <div class="hero-shade"></div>
        <div class="hero-badges">
          <span>{{ restaurant.cuisine }}</span>
          <span :class="restaurant.status === 1 ? 'open' : 'closed'">{{ restaurant.status === 1 ? '营业中' : '休息中' }}</span>
        </div>
      </section>

      <section class="title-panel">
        <h1>{{ restaurant.name }}</h1>
        <p>{{ restaurant.businessArea || restaurant.district }} · 人均 ¥{{ restaurant.averagePrice }}</p>

        <div class="fact-grid">
          <div>
            <strong>{{ restaurant.rating }}</strong>
            <span>评分</span>
          </div>
          <div>
            <strong>¥{{ restaurant.averagePrice }}</strong>
            <span>人均</span>
          </div>
          <div>
            <strong>{{ restaurant.district }}</strong>
            <span>区域</span>
          </div>
        </div>

        <div class="detail-actions" aria-label="餐厅操作">
          <button type="button" class="detail-action primary" :disabled="!restaurant.longitude || !restaurant.latitude" @click="openNavigation">
            <van-icon name="guide-o" size="17" />
            <span>导航</span>
          </button>
          <button type="button" :class="['detail-action', { active: isFavorite }]" :aria-pressed="isFavorite" @click="toggleFavorite">
            <van-icon :name="isFavorite ? 'star' : 'star-o'" size="17" />
            <span>{{ isFavorite ? '已收藏' : '收藏' }}</span>
          </button>
        </div>
      </section>

      <section v-if="tasteTags.length || sceneTags.length" class="detail-section">
        <div class="section-head">
          <span>适合这样吃</span>
        </div>
        <div class="detail-tags">
          <span v-for="tag in tasteTags" :key="tag" class="taste">{{ tag }}</span>
          <span v-for="tag in sceneTags" :key="tag" class="scene">{{ tag }}</span>
        </div>
      </section>

      <section class="detail-section">
        <div class="section-head">
          <span>到店信息</span>
        </div>
        <div class="info-list">
          <div class="info-row">
            <van-icon name="location-o" size="18" />
            <div>
              <strong>地址</strong>
              <p>{{ restaurant.address || '待补充' }}</p>
            </div>
          </div>
          <div class="info-row">
            <van-icon name="shop-o" size="18" />
            <div>
              <strong>来源</strong>
              <p>{{ restaurant.sourceNote || '太原食探餐厅库' }}</p>
            </div>
          </div>
        </div>
      </section>

      <section v-if="dishes.length" class="detail-section">
        <div class="section-head">
          <span>可以先看这些</span>
        </div>
        <div class="dish-list">
          <span v-for="dish in dishes" :key="dish">{{ dish }}</span>
        </div>
      </section>

      <section v-if="restaurant.description" class="detail-section">
        <div class="section-head">
          <span>简介</span>
        </div>
        <p class="description">{{ restaurant.description }}</p>
      </section>

      <section class="notice-card">
        <strong>信息说明</strong>
        <p>餐厅信息会持续更新，营业时间、价格和菜品请以实际到店为准。</p>
      </section>
    </article>

    <DetailSkeleton v-else />

    <BottomNav />
  </main>
</template>

<style scoped>
.detail-shell {
  background: linear-gradient(180deg, #fff8f2 0%, #fff4eb 48%, #fff8f2 100%);
}

.detail-page {
  display: grid;
  gap: 16px;
}

.detail-hero {
  position: relative;
  min-height: 286px;
  margin-top: 8px;
  border-radius: 30px;
  overflow: hidden;
  background:
    radial-gradient(circle at 30% 20%, rgba(255, 255, 255, 0.88), transparent 34%),
    linear-gradient(135deg, #ffe3cd, #fff8f2);
  box-shadow: 0 22px 56px rgba(75, 35, 14, 0.16);
}

.hero-img {
  width: 100%;
  height: 286px;
  object-fit: cover;
}

.hero-placeholder {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  color: var(--color-primary);
  font-size: 54px;
  font-weight: 900;
}

.hero-shade {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, transparent 42%, rgba(43, 22, 12, 0.56));
}

.hero-badges {
  position: absolute;
  left: 14px;
  right: 14px;
  bottom: 14px;
  display: flex;
  justify-content: space-between;
  gap: 10px;
}

.hero-badges span {
  min-height: 34px;
  display: inline-flex;
  align-items: center;
  padding: 0 12px;
  border-radius: 999px;
  color: #fff;
  background: rgba(43, 22, 12, 0.68);
  font-size: 13px;
  font-weight: 800;
}

.hero-badges .open {
  background: rgba(34, 197, 94, 0.9);
}

.hero-badges .closed {
  background: rgba(142, 142, 147, 0.9);
}

.title-panel,
.detail-section,
.notice-card {
  border: 1px solid rgba(93, 45, 22, 0.08);
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 14px 38px rgba(75, 35, 14, 0.08);
}

.title-panel {
  padding: 18px;
}

.title-panel h1 {
  margin: 0;
  color: var(--color-text);
  font-size: 25px;
  line-height: 1.24;
}

.title-panel p {
  margin: 8px 0 0;
  color: var(--color-muted);
  font-size: 14px;
}

.fact-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  margin-top: 18px;
}

.fact-grid div {
  min-height: 72px;
  border-radius: 18px;
  background: #fff7f1;
  display: grid;
  align-content: center;
  justify-items: center;
  padding: 10px 6px;
  text-align: center;
}

.fact-grid strong {
  max-width: 100%;
  color: var(--color-text);
  font-size: 17px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.fact-grid span {
  margin-top: 3px;
  color: var(--color-muted);
  font-size: 12px;
}

.detail-actions {
  display: grid;
  grid-template-columns: 1.2fr 1fr;
  gap: 10px;
  margin-top: 16px;
}

.detail-action {
  min-height: 44px;
  border: 1px solid rgba(255, 122, 46, 0.24);
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  color: var(--color-primary);
  background: #fff7f1;
  font-size: 14px;
  font-weight: 800;
}

.detail-action.primary {
  border: 0;
  color: #fff;
  background: var(--color-primary);
}

.detail-action.active {
  background: var(--color-primary-light);
}

.detail-action:disabled {
  opacity: 0.55;
}

.detail-section {
  padding: 16px;
}

.section-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.section-head span {
  color: var(--color-text);
  font-size: 17px;
  font-weight: 800;
}

.detail-tags,
.dish-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.detail-tags span,
.dish-list span {
  padding: 7px 10px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 700;
}

.detail-tags .taste {
  background: var(--color-primary-light);
  color: var(--color-primary);
}

.detail-tags .scene,
.dish-list span {
  background: #fff7f1;
  color: var(--color-brand-dark);
}

.info-list {
  display: grid;
  gap: 12px;
}

.info-row {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  padding: 13px;
  border-radius: 18px;
  background: #fff9f5;
}

.info-row .van-icon {
  width: 34px;
  height: 34px;
  display: grid;
  place-items: center;
  flex-shrink: 0;
  border-radius: 13px;
  color: var(--color-primary);
  background: var(--color-primary-light);
}

.info-row strong {
  display: block;
  color: var(--color-text);
  font-size: 14px;
}

.info-row p,
.description,
.notice-card p {
  margin: 5px 0 0;
  color: var(--color-muted);
  font-size: 13px;
  line-height: 1.65;
}

.notice-card {
  padding: 16px;
}

.notice-card strong {
  color: var(--color-text);
  font-size: 15px;
}

.notice-card .van-tag {
  margin-top: 10px;
}
</style>
