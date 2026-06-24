<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import AppInlineMessage from '../../components/AppInlineMessage.vue'
import AppStateCard from '../../components/AppStateCard.vue'
import AppTopBar from '../../components/AppTopBar.vue'
import BottomNav from '../../components/BottomNav.vue'
import NearbyListSkeleton from '../../components/NearbyListSkeleton.vue'
import { getErrorMessage } from '../../api/client'
import { getNearbyRestaurants } from '../../api/map'
import type { NearbyRestaurant } from '../../types/api'

const loading = ref(false)
const error = ref('')
const notice = ref('')
const list = ref<NearbyRestaurant[]>([])
const failedImageIds = ref(new Set<string>())
const radius = ref(3000)
const coords = reactive({ lng: 112.447, lat: 38.018 })
const locating = ref(false)
let abortController: AbortController | null = null

const radiusOptions = [1000, 3000, 5000]
const countText = computed(() => list.value.length ? `${list.value.length} 家` : '--')

function formatDistance(m: number) {
  if (!Number.isFinite(m)) return '--'
  if (m < 1000) return Math.round(m) + 'm'
  return (m / 1000).toFixed(1) + 'km'
}

function formatRadius(m: number) {
  return m >= 1000 ? `${m / 1000}km` : `${m}m`
}

function typeName(type: string) {
  return type?.split(';')[0] || '餐饮'
}

function hasUsableImage(item: NearbyRestaurant) {
  return !!item.coverImage && !failedImageIds.value.has(item.id)
}

function markImageFailed(itemId: string) {
  const next = new Set(failedImageIds.value)
  next.add(itemId)
  failedImageIds.value = next
}

function openNav(lng?: number, lat?: number, name?: string) {
  if (!lng || !lat) return
  window.open(`https://uri.amap.com/navigation?to=${lng},${lat},${encodeURIComponent(name || '')}&mode=bus&callnative=1`, '_blank')
}

function isValidCoord(lat: number, lng: number) {
  return !(Math.abs(lat) > 90 || Math.abs(lng) > 180 || lat < 35 || lat > 41 || lng < 110 || lng > 115)
}

function locateUser() {
  locating.value = true
  if ('geolocation' in navigator) {
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        const lat = pos.coords.latitude
        const lng = pos.coords.longitude
        if (isValidCoord(lat, lng)) {
          coords.lng = lng
          coords.lat = lat
        }
        locating.value = false
        searchNearby()
      },
      () => {
        locating.value = false
        searchNearby()
      },
      { timeout: 5000, enableHighAccuracy: true },
    )
  } else {
    locating.value = false
    searchNearby()
  }
}

async function searchNearby() {
  if (abortController) abortController.abort()
  abortController = new AbortController()
  const signal = abortController.signal

  loading.value = true
  error.value = ''
  try {
    const result = await getNearbyRestaurants({
      longitude: coords.lng,
      latitude: coords.lat,
      radius: radius.value,
      keyword: '餐饮服务',
      page: 1,
      pageSize: 20,
    }, signal)
    if (!signal.aborted) {
      list.value = result.list
      failedImageIds.value = new Set()
      notice.value = result.notice
    }
  } catch (err) {
    if (!signal.aborted) {
      error.value = getErrorMessage(err, '附近搜索不可用')
    }
  } finally {
    if (!signal.aborted) loading.value = false
  }
}

function changeRadius(nextRadius: number) {
  radius.value = nextRadius
  searchNearby()
}

onMounted(locateUser)
</script>

<template>
  <main class="mobile-page nearby-page">
    <AppTopBar title="附近" subtitle="距离筛选" />

    <section class="nearby-summary">
      <div>
        <span>搜索半径</span>
        <strong>{{ formatRadius(radius) }}</strong>
      </div>
      <div>
        <span>结果</span>
        <strong>{{ countText }}</strong>
      </div>
      <button type="button" :disabled="locating" @click="locateUser">
        <van-icon name="aim" size="17" />
        {{ locating ? '定位中' : '定位' }}
      </button>
    </section>

    <section class="radius-row" aria-label="搜索半径">
      <button
        v-for="r in radiusOptions"
        :key="r"
        type="button"
        :aria-pressed="radius === r"
        :class="{ active: radius === r }"
        @click="changeRadius(r)"
      >
        {{ formatRadius(r) }}
      </button>
    </section>

    <AppInlineMessage v-if="notice" class="nearby-notice" :message="notice" />

    <NearbyListSkeleton v-if="loading" :count="4" />
    <AppStateCard
      v-else-if="error"
      variant="error"
      title="附近搜索失败"
      :description="error"
      action-text="重试"
      @action="searchNearby"
    />
    <AppStateCard
      v-else-if="!list.length"
      title="暂无附近结果"
      description="扩大距离试试"
      action-text="5km"
      @action="changeRadius(5000)"
    />

    <section v-else class="near-list">
      <article v-for="item in list" :key="item.id" class="near-card">
        <div class="near-img-wrap">
          <img
            v-if="hasUsableImage(item)"
            :src="item.coverImage"
            class="near-img"
            :alt="item.name"
            @error="markImageFailed(item.id)"
          />
          <span v-else class="near-img-placeholder" aria-hidden="true">
            <van-icon name="shop-o" size="22" />
          </span>
        </div>
        <div class="near-body">
          <div class="near-top">
            <h3>{{ item.name }}</h3>
            <span class="dist">{{ formatDistance(item.distance) }}</span>
          </div>
          <div class="near-meta">
            <span v-if="item.rating" class="r-star">
              <van-icon name="star" size="12" />
              {{ item.rating }}
            </span>
            <span>{{ typeName(item.type) }}</span>
          </div>
          <p>{{ item.address }}</p>
        </div>
        <button
          class="go-btn"
          type="button"
          :disabled="!item.longitude"
          :aria-label="`导航到${item.name}`"
          @click="openNav(item.longitude, item.latitude, item.name)"
        >
          <van-icon name="guide-o" size="17" />
        </button>
      </article>
    </section>

    <BottomNav />
  </main>
</template>

<style scoped>
.nearby-page {
  background: var(--color-bg);
}

.nearby-summary {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  align-items: stretch;
}

.nearby-summary > div,
.nearby-summary button {
  min-width: 0;
  min-height: 64px;
  border: 1px solid var(--color-border);
  border-radius: 12px;
  background: var(--color-card);
  box-shadow: var(--shadow-card);
}

.nearby-summary > div {
  padding: 10px 12px;
}

.nearby-summary span,
.nearby-summary strong {
  display: block;
}

.nearby-summary span {
  color: var(--color-muted);
  font-size: 12px;
}

.nearby-summary strong {
  margin-top: 5px;
  color: var(--color-text);
  font-size: 17px;
}

.nearby-summary button {
  grid-column: 1 / -1;
  min-width: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  color: var(--color-primary);
  font-weight: 800;
}

.radius-row {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  margin: 12px 0;
}

.radius-row button {
  min-height: 44px;
  border: 1px solid var(--color-border);
  border-radius: 10px;
  color: var(--color-muted);
  background: var(--color-card);
  font-weight: 800;
}

.radius-row button.active {
  border-color: rgba(231, 104, 46, 0.3);
  color: var(--color-primary-strong);
  background: var(--color-primary-light);
}

.nearby-notice {
  margin-bottom: 12px;
}

.near-list {
  display: grid;
  gap: 10px;
}

.near-card {
  min-width: 0;
  display: grid;
  grid-template-columns: 74px minmax(0, 1fr) 40px;
  gap: 10px;
  align-items: center;
  padding: 10px;
  border: 1px solid var(--color-border);
  border-radius: 12px;
  background: var(--color-card);
  box-shadow: var(--shadow-card);
}

.near-img-wrap {
  width: 74px;
  height: 74px;
  border-radius: 10px;
  display: grid;
  place-items: center;
  overflow: hidden;
  color: var(--color-primary);
  background: var(--color-soft);
}

.near-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.near-img-placeholder {
  width: 40px;
  height: 40px;
  display: grid;
  place-items: center;
  border-radius: 10px;
  background: var(--color-card);
  box-shadow: inset 0 0 0 1px var(--color-border);
}

.near-body {
  min-width: 0;
}

.near-top {
  display: flex;
  gap: 8px;
  align-items: flex-start;
}

.near-top h3 {
  min-width: 0;
  flex: 1;
  margin: 0;
  color: var(--color-text);
  font-size: 15px;
  line-height: 1.35;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dist {
  flex: 0 0 auto;
  color: var(--color-primary);
  font-size: 12px;
  font-weight: 800;
}

.near-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 5px;
  color: var(--color-muted);
  font-size: 12px;
}

.r-star {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  color: var(--color-star);
  font-weight: 800;
}

.near-body p {
  margin: 5px 0 0;
  color: var(--color-muted);
  font-size: 12px;
  line-height: 1.45;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.go-btn {
  width: 40px;
  height: 40px;
  border: 1px solid var(--color-border);
  border-radius: 10px;
  display: grid;
  place-items: center;
  color: var(--color-primary);
  background: var(--color-card);
}

.go-btn:disabled {
  opacity: 0.4;
}

button:active {
  transform: scale(0.97);
}

@media (max-width: 374px) {
  .near-card {
    grid-template-columns: 66px minmax(0, 1fr) 40px;
    gap: 8px;
  }

  .near-img-wrap {
    width: 66px;
    height: 66px;
  }
}
</style>
