<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import AppStateCard from '../../components/AppStateCard.vue'
import BottomNav from '../../components/BottomNav.vue'
import RestaurantCard from '../../components/RestaurantCard.vue'
import RestaurantCardSkeleton from '../../components/RestaurantCardSkeleton.vue'
import { getErrorMessage } from '../../api/client'
import { getRegeo } from '../../api/map'
import { getRestaurants } from '../../api/restaurants'
import type { Restaurant } from '../../types/api'

const router = useRouter()
const restaurants = ref<Restaurant[]>([])
const loading = ref(false)
const error = ref('')
const userLocation = reactive({ lat: 38.018, lng: 112.447 })
const locating = ref(false)
const locationText = ref('中北大学')
const gpsActive = ref(false)
const dataVersion = ref(0)
const failedImageIds = ref(new Set<number>())

const filters = reactive({
  cuisine: '',
  sortBy: 'rating' as 'rating' | 'distance' | 'price',
})

const cuisineOptions = ['全部', '面食', '麻辣烫', '火锅', '烧烤', '快餐', '饮品', '小吃']
const sortOptions: Array<{ key: 'rating' | 'distance' | 'price'; label: string }> = [
  { key: 'rating', label: '评分' },
  { key: 'distance', label: '距离' },
  { key: 'price', label: '人均' },
]

const displayLocation = computed(() => locating.value ? '定位中' : locationText.value)

const sortedRestaurants = computed(() => {
  void dataVersion.value
  let list = [...restaurants.value]
  if (filters.cuisine) {
    list = list.filter(r => r.cuisine === filters.cuisine || r.cuisine?.includes(filters.cuisine))
  }
  if (filters.sortBy === 'rating') list.sort((a, b) => b.rating - a.rating)
  else if (filters.sortBy === 'price') list.sort((a, b) => a.averagePrice - b.averagePrice)
  else list.sort((a, b) => getDist(a) - getDist(b))
  return list.slice(0, 6)
})

const featuredRestaurant = computed(() => sortedRestaurants.value[0])
const listRestaurants = computed(() => sortedRestaurants.value.slice(1, 6))

function isValidCoord(lat: number, lng: number) {
  if (Math.abs(lat) > 90 || Math.abs(lng) > 180) return false
  return !(lat < 35 || lat > 41 || lng < 110 || lng > 115)
}

function getDist(r: Restaurant) {
  if (!r.latitude || !r.longitude) return Infinity
  if (!isValidCoord(r.latitude, r.longitude)) return Infinity
  const R = 6371000
  const dLat = (r.latitude - userLocation.lat) * Math.PI / 180
  const dLng = (r.longitude - userLocation.lng) * Math.PI / 180
  const a = Math.sin(dLat / 2) ** 2
    + Math.cos(userLocation.lat * Math.PI / 180)
    * Math.cos(r.latitude * Math.PI / 180)
    * Math.sin(dLng / 2) ** 2
  return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
}

function formatDist(m: number) {
  if (!isFinite(m)) return ''
  if (m < 1000) return Math.round(m) + 'm'
  return (m / 1000).toFixed(1) + 'km'
}

function selectCuisine(cuisine: string) {
  filters.cuisine = cuisine === '全部' ? '' : (filters.cuisine === cuisine ? '' : cuisine)
}

function go(path: string) {
  router.push(path)
}

function hasUsableImage(restaurant: Restaurant) {
  return !!restaurant.coverImage && !failedImageIds.value.has(restaurant.id)
}

function markImageFailed(restaurantId: number) {
  const next = new Set(failedImageIds.value)
  next.add(restaurantId)
  failedImageIds.value = next
}

async function locateUser() {
  locating.value = true
  if ('geolocation' in navigator) {
    navigator.geolocation.getCurrentPosition(
      async (pos) => {
        const lat = pos.coords.latitude
        const lng = pos.coords.longitude
        if (!isValidCoord(lat, lng)) {
          locationText.value = '中北大学'
          gpsActive.value = false
          locating.value = false
          return
        }
        userLocation.lat = lat
        userLocation.lng = lng
        gpsActive.value = true
        dataVersion.value++
        try {
          const geo = await getRegeo(lng, lat)
          locationText.value = geo.township || geo.district || '太原'
        } catch {
          locationText.value = '已定位'
        }
        locating.value = false
      },
      () => {
        gpsActive.value = false
        locating.value = false
        dataVersion.value++
      },
      { timeout: 5000, enableHighAccuracy: true },
    )
  } else {
    gpsActive.value = false
    locating.value = false
    dataVersion.value++
  }
}

async function loadRestaurants() {
  loading.value = true
  error.value = ''
  try {
    const result = await getRestaurants({ page: 1, pageSize: 200 })
    restaurants.value = result.list
    failedImageIds.value = new Set()
  } catch (err) {
    error.value = getErrorMessage(err, '餐厅数据加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadRestaurants()
  locateUser()
})
</script>

<template>
  <main class="mobile-page home-page">
    <header class="home-header">
      <button class="location-button" type="button" :aria-label="`当前位置 ${displayLocation}`" @click="locateUser">
        <van-icon name="location-o" :color="gpsActive ? '#258a55' : '#e7682e'" size="16" />
        <span>{{ displayLocation }}</span>
      </button>
      <button class="search-button" type="button" aria-label="搜索" @click="go('/search')">
        <van-icon name="search" size="18" />
      </button>
    </header>

    <section class="decision-panel">
      <div>
        <span>今日</span>
        <h1>今天吃什么</h1>
      </div>
      <button type="button" class="primary-decision" @click="go('/search')">
        AI 推荐
      </button>
      <button type="button" class="secondary-decision" @click="go('/shake')">
        <van-icon name="exchange" size="17" />
        摇一摇
      </button>
    </section>

    <section class="entry-grid" aria-label="快捷入口">
      <button type="button" @click="go('/nearby')">
        <van-icon name="location-o" size="18" />
        <span>附近</span>
      </button>
      <button type="button" @click="go('/discover')">
        <van-icon name="apps-o" size="18" />
        <span>分类</span>
      </button>
      <button type="button" @click="go('/discover')">
        <van-icon name="bar-chart-o" size="18" />
        <span>榜单</span>
      </button>
    </section>

    <section class="filter-section">
      <div class="chip-scroll" aria-label="菜系筛选">
        <button
          v-for="cuisine in cuisineOptions"
          :key="cuisine"
          type="button"
          :aria-pressed="(filters.cuisine || '全部') === cuisine"
          :class="{ active: (filters.cuisine || '全部') === cuisine }"
          @click="selectCuisine(cuisine)"
        >
          {{ cuisine }}
        </button>
      </div>
      <div class="sort-row" aria-label="排序方式">
        <button
          v-for="option in sortOptions"
          :key="option.key"
          type="button"
          :aria-pressed="filters.sortBy === option.key"
          :class="{ active: filters.sortBy === option.key }"
          @click="filters.sortBy = option.key"
        >
          {{ option.label }}
        </button>
      </div>
    </section>

    <section class="result-section">
      <div class="section-head">
        <h2>可选餐厅</h2>
        <button type="button" @click="go('/discover')">更多</button>
      </div>

      <RestaurantCardSkeleton v-if="loading" :count="3" />
      <AppStateCard
        v-else-if="error"
        variant="error"
        title="餐厅数据失败"
        :description="error"
        action-text="重试"
        @action="loadRestaurants"
      />
      <AppStateCard
        v-else-if="sortedRestaurants.length === 0"
        title="暂无结果"
        description="换个分类试试"
        action-text="看榜单"
        @action="go('/discover')"
      />

      <template v-else>
        <RouterLink v-if="featuredRestaurant" class="today-card" :to="`/restaurants/${featuredRestaurant.id}`">
          <div class="today-image">
            <img
              v-if="hasUsableImage(featuredRestaurant)"
              :src="featuredRestaurant.coverImage"
              :alt="featuredRestaurant.name"
              @error="markImageFailed(featuredRestaurant.id)"
            />
            <van-icon v-else name="shop-o" size="26" />
          </div>
          <div class="today-info">
            <h3>{{ featuredRestaurant.name }}</h3>
            <p>
              <span><van-icon name="star" size="12" />{{ featuredRestaurant.rating }}</span>
              <span>¥{{ featuredRestaurant.averagePrice }}</span>
              <span v-if="formatDist(getDist(featuredRestaurant))">{{ formatDist(getDist(featuredRestaurant)) }}</span>
            </p>
            <div>
              <span>{{ featuredRestaurant.cuisine }}</span>
              <span v-for="tag in (featuredRestaurant.sceneTags || []).slice(0, 2)" :key="tag">{{ tag }}</span>
            </div>
          </div>
        </RouterLink>

        <div class="home-list">
          <RestaurantCard v-for="r in listRestaurants" :key="r.id" :restaurant="r" />
        </div>
      </template>
    </section>

    <BottomNav />
  </main>
</template>

<style scoped>
.home-page {
  background: var(--color-bg);
}

.home-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.location-button,
.search-button,
.entry-grid button,
.chip-scroll button,
.sort-row button {
  min-height: 44px;
  border: 1px solid var(--color-border);
  background: var(--color-card);
  color: var(--color-text);
}

.location-button {
  min-width: 0;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 0 12px;
  border-radius: 12px;
  font-size: 13px;
  font-weight: 700;
}

.location-button span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.search-button {
  width: 44px;
  flex: 0 0 44px;
  border-radius: 12px;
}

.decision-panel {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 12px;
  margin-top: 18px;
  padding: 16px;
  border: 1px solid var(--color-border);
  border-radius: 14px;
  background: var(--color-card);
  box-shadow: var(--shadow-card);
}

.decision-panel span {
  color: var(--color-primary);
  font-size: 12px;
  font-weight: 800;
}

.decision-panel h1 {
  margin: 4px 0 0;
  color: var(--color-text);
  font-size: 25px;
  line-height: 1.2;
}

.primary-decision,
.secondary-decision {
  width: 100%;
  min-width: 0;
  min-height: 44px;
  border-radius: 12px;
  font-weight: 800;
}

.primary-decision {
  border: 0;
  color: #fff;
  background: var(--color-primary);
}

.secondary-decision {
  grid-column: 1 / -1;
  border: 1px solid var(--color-border);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  color: var(--color-text);
  background: var(--color-soft);
}

.entry-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin-top: 12px;
}

.entry-grid button {
  border-radius: 12px;
  display: grid;
  place-items: center;
  gap: 5px;
  padding: 10px 6px;
  font-size: 13px;
  font-weight: 800;
}

.entry-grid .van-icon {
  color: var(--color-primary);
}

.filter-section {
  margin-top: 18px;
}

.chip-scroll {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  padding-bottom: 2px;
  scrollbar-width: none;
}

.chip-scroll::-webkit-scrollbar {
  display: none;
}

.chip-scroll button {
  flex: 0 0 auto;
  padding: 0 14px;
  border-radius: 12px;
  color: var(--color-muted);
  font-size: 13px;
  font-weight: 700;
}

.sort-row {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  margin-top: 10px;
}

.sort-row button {
  border-radius: 10px;
  color: var(--color-muted);
  font-size: 13px;
  font-weight: 700;
}

.chip-scroll button.active,
.sort-row button.active {
  border-color: rgba(231, 104, 46, 0.3);
  color: var(--color-primary-strong);
  background: var(--color-primary-light);
}

.result-section {
  margin-top: 18px;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.section-head h2 {
  margin: 0;
  color: var(--color-text);
  font-size: 18px;
}

.section-head button {
  min-height: 44px;
  border: 0;
  color: var(--color-primary);
  background: transparent;
  font-weight: 800;
}

.today-card {
  min-width: 0;
  display: flex;
  gap: 12px;
  padding: 10px;
  border: 1px solid var(--color-border);
  border-radius: 14px;
  background: var(--color-card);
  box-shadow: var(--shadow-card);
}

.today-image {
  width: 104px;
  height: 104px;
  flex: 0 0 104px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  overflow: hidden;
  color: var(--color-primary);
  background: var(--color-soft);
}

.today-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.today-info {
  min-width: 0;
  flex: 1;
  align-self: center;
}

.today-info h3 {
  margin: 0;
  color: var(--color-text);
  font-size: 17px;
  line-height: 1.3;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.today-info p {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 8px 0 0;
  color: var(--color-muted);
  font-size: 12px;
}

.today-info p span:first-child {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  color: var(--color-star);
  font-weight: 800;
}

.today-info div {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
  max-height: 25px;
  margin-top: 9px;
  overflow: hidden;
}

.today-info div span {
  max-width: 76px;
  padding: 3px 7px;
  border-radius: 8px;
  color: var(--color-primary-strong);
  background: var(--color-primary-light);
  font-size: 11px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.home-list {
  display: grid;
  gap: 10px;
  margin-top: 10px;
}

button {
  font-family: inherit;
}

button:active,
.today-card:active {
  transform: scale(0.985);
}

@media (max-width: 374px) {
  .entry-grid,
  .sort-row {
    gap: 7px;
  }

  .today-image {
    width: 88px;
    height: 88px;
    flex-basis: 88px;
  }

  .decision-panel h1 {
    font-size: 23px;
  }
}
</style>
