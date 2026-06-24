<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import AppStateCard from '../../components/AppStateCard.vue'
import BottomNav from '../../components/BottomNav.vue'
import RestaurantCard from '../../components/RestaurantCard.vue'
import RestaurantCardSkeleton from '../../components/RestaurantCardSkeleton.vue'
import { getErrorMessage } from '../../api/client'
import { getRestaurants } from '../../api/restaurants'
import type { Restaurant } from '../../types/api'

type RankMode = 'student' | 'value' | 'night' | 'rating'

const restaurants = ref<Restaurant[]>([])
const loading = ref(false)
const error = ref('')
const failedImageIds = ref(new Set<number>())
const activeCuisine = ref('全部')
const activeRank = ref<RankMode>('student')

const cuisineOptions = ['全部', '面食', '麻辣烫', '火锅', '烧烤', '快餐', '饮品', '小吃']
const cuisineIcons: Record<string, string> = {
  全部: 'apps-o',
  面食: 'shop-o',
  麻辣烫: 'fire-o',
  火锅: 'hot-o',
  烧烤: 'bar-chart-o',
  快餐: 'clock-o',
  饮品: 'smile-o',
  小吃: 'location-o',
}

const rankTabs: Array<{ key: RankMode; label: string }> = [
  { key: 'student', label: '学生' },
  { key: 'value', label: '平价' },
  { key: 'night', label: '夜宵' },
  { key: 'rating', label: '高分' },
]

const baseList = computed(() => {
  if (activeCuisine.value === '全部') return restaurants.value
  return restaurants.value.filter(item => item.cuisine === activeCuisine.value || item.cuisine?.includes(activeCuisine.value))
})

const rankedList = computed(() => {
  const list = [...baseList.value]
  if (activeRank.value === 'student') return list.sort((a, b) => scoreStudent(b) - scoreStudent(a)).slice(0, 10)
  if (activeRank.value === 'value') return list.sort((a, b) => scoreValue(b) - scoreValue(a)).slice(0, 10)
  if (activeRank.value === 'night') return list.sort((a, b) => scoreNight(b) - scoreNight(a)).slice(0, 10)
  return list.sort((a, b) => b.rating - a.rating).slice(0, 10)
})

const topThree = computed(() => rankedList.value.slice(0, 3))

function scoreStudent(item: Restaurant) {
  const tags = [...(item.tasteTags || []), ...(item.sceneTags || [])].join('')
  const studentBonus = /学生|一人食|快|便宜|性价比|实惠/.test(tags) ? 1.4 : 0
  const campusBonus = /中北|大学|校园/.test(`${item.businessArea || ''}${item.address || ''}`) ? 1.2 : 0
  return item.rating * 10 - item.averagePrice * 0.12 + studentBonus + campusBonus
}

function scoreValue(item: Restaurant) {
  return item.rating * 14 - item.averagePrice * 0.32 + ((item.sceneTags || []).includes('性价比') ? 2 : 0)
}

function scoreNight(item: Restaurant) {
  const text = `${item.cuisine}${(item.tasteTags || []).join('')}${(item.sceneTags || []).join('')}`
  const bonus = /夜宵|烧烤|小吃|火锅|麻辣|聚餐/.test(text) ? 8 : 0
  return item.rating * 10 - item.averagePrice * 0.08 + bonus
}

function categoryIcon(cuisine: string) {
  return cuisineIcons[cuisine] || 'shop-o'
}

function hasUsableImage(restaurant: Restaurant) {
  return !!restaurant.coverImage && !failedImageIds.value.has(restaurant.id)
}

function markImageFailed(restaurantId: number) {
  const next = new Set(failedImageIds.value)
  next.add(restaurantId)
  failedImageIds.value = next
}

async function loadRestaurants() {
  loading.value = true
  error.value = ''
  try {
    const res = await getRestaurants({ page: 1, pageSize: 200 })
    restaurants.value = res.list
    failedImageIds.value = new Set()
  } catch (err) {
    error.value = getErrorMessage(err, '榜单加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadRestaurants)
</script>

<template>
  <main class="mobile-page discover-page">
    <header class="simple-header">
      <div>
        <span>发现</span>
        <h1>分类与榜单</h1>
      </div>
    </header>

    <section class="category-grid" aria-label="餐饮分类">
      <button
        v-for="cuisine in cuisineOptions"
        :key="cuisine"
        type="button"
        :aria-pressed="activeCuisine === cuisine"
        :class="{ active: activeCuisine === cuisine }"
        @click="activeCuisine = cuisine"
      >
        <van-icon :name="categoryIcon(cuisine)" size="18" />
        <span>{{ cuisine }}</span>
      </button>
    </section>

    <section class="rank-panel">
      <div class="section-head">
        <h2>{{ activeCuisine === '全部' ? '榜单' : activeCuisine }}</h2>
      </div>

      <div class="rank-tabs" aria-label="榜单类型">
        <button
          v-for="tab in rankTabs"
          :key="tab.key"
          type="button"
          :aria-pressed="activeRank === tab.key"
          :class="{ active: activeRank === tab.key }"
          @click="activeRank = tab.key"
        >
          {{ tab.label }}
        </button>
      </div>

      <div v-if="topThree.length" class="podium-list" aria-label="前三名">
        <RouterLink v-for="(item, index) in topThree" :key="item.id" :to="`/restaurants/${item.id}`" class="podium-card">
          <span class="rank-mark">{{ index + 1 }}</span>
          <div class="podium-image">
            <img v-if="hasUsableImage(item)" :src="item.coverImage" :alt="item.name" @error="markImageFailed(item.id)" />
            <van-icon v-else name="shop-o" size="21" />
          </div>
          <b>{{ item.name }}</b>
          <small>
            <van-icon name="star" size="11" />
            {{ item.rating }} · ¥{{ item.averagePrice }}
          </small>
        </RouterLink>
      </div>

      <RestaurantCardSkeleton v-if="loading" :count="4" />
      <AppStateCard
        v-else-if="error"
        variant="error"
        title="榜单加载失败"
        :description="error"
        action-text="重试"
        @action="loadRestaurants"
      />
      <AppStateCard
        v-else-if="rankedList.length === 0"
        title="暂无结果"
        description="切换分类试试"
        action-text="重试"
        @action="loadRestaurants"
      />
      <div v-else class="rank-list">
        <div v-for="(item, index) in rankedList" :key="item.id" class="rank-row">
          <span class="rank-number">{{ index + 1 }}</span>
          <RestaurantCard :restaurant="item" />
        </div>
      </div>
    </section>

    <BottomNav />
  </main>
</template>

<style scoped>
.discover-page {
  background: var(--color-bg);
}

.simple-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 14px;
}

.simple-header span {
  color: var(--color-primary);
  font-size: 12px;
  font-weight: 800;
}

.simple-header h1 {
  margin: 4px 0 0;
  color: var(--color-text);
  font-size: 24px;
  line-height: 1.25;
}

.category-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.category-grid button {
  min-width: 0;
  min-height: 72px;
  border: 1px solid var(--color-border);
  border-radius: 12px;
  display: grid;
  place-items: center;
  align-content: center;
  gap: 5px;
  color: var(--color-muted);
  background: var(--color-card);
  box-shadow: var(--shadow-card);
}

.category-grid button.active {
  border-color: rgba(231, 104, 46, 0.3);
  color: var(--color-primary-strong);
  background: var(--color-primary-light);
}

.category-grid span {
  max-width: 100%;
  font-size: 12px;
  font-weight: 800;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rank-panel {
  margin-top: 18px;
}

.section-head {
  margin-bottom: 10px;
}

.section-head h2 {
  margin: 0;
  color: var(--color-text);
  font-size: 18px;
}

.rank-tabs {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
}

.rank-tabs button {
  min-height: 44px;
  border: 1px solid var(--color-border);
  border-radius: 10px;
  color: var(--color-muted);
  background: var(--color-card);
  font-weight: 800;
}

.rank-tabs button.active {
  border-color: rgba(231, 104, 46, 0.3);
  color: var(--color-primary-strong);
  background: var(--color-primary-light);
}

.podium-list {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  margin: 12px 0;
}

.podium-card {
  min-width: 0;
  padding: 8px;
  border: 1px solid var(--color-border);
  border-radius: 12px;
  background: var(--color-card);
  box-shadow: var(--shadow-card);
}

.rank-mark {
  width: 24px;
  height: 24px;
  border-radius: 8px;
  display: grid;
  place-items: center;
  color: #fff;
  background: var(--color-primary);
  font-size: 12px;
  font-weight: 900;
}

.podium-image {
  height: 68px;
  margin: 7px 0;
  border-radius: 10px;
  display: grid;
  place-items: center;
  overflow: hidden;
  color: var(--color-primary);
  background: var(--color-soft);
}

.podium-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.podium-card b {
  display: -webkit-box;
  color: var(--color-text);
  font-size: 12px;
  line-height: 1.35;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.podium-card small {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  max-width: 100%;
  margin-top: 5px;
  color: var(--color-star);
  font-size: 11px;
  font-weight: 800;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rank-list {
  display: grid;
  gap: 10px;
  margin-top: 12px;
}

.rank-row {
  display: grid;
  grid-template-columns: 24px minmax(0, 1fr);
  gap: 8px;
  align-items: center;
}

.rank-number {
  width: 24px;
  height: 24px;
  border-radius: 8px;
  display: grid;
  place-items: center;
  color: var(--color-muted);
  background: var(--color-soft);
  font-size: 12px;
  font-weight: 900;
}

button:active,
.podium-card:active {
  transform: scale(0.98);
}

@media (max-width: 374px) {
  .podium-list {
    gap: 6px;
  }

  .podium-card {
    padding: 7px;
  }
}

@media (min-width: 390px) {
  .category-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}
</style>
