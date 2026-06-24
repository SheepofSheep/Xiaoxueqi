<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppStateCard from '../../components/AppStateCard.vue'
import BottomNav from '../../components/BottomNav.vue'
import RestaurantCard from '../../components/RestaurantCard.vue'
import RestaurantCardSkeleton from '../../components/RestaurantCardSkeleton.vue'
import { getErrorMessage } from '../../api/client'
import { getRestaurants } from '../../api/restaurants'
import { useFavoriteStore } from '../../stores/favorites'
import type { Restaurant } from '../../types/api'

const route = useRoute()
const router = useRouter()
const favoriteStore = useFavoriteStore()
const restaurants = ref<Restaurant[]>([])
const loading = ref(false)
const error = ref('')

const isHistory = computed(() => route.path === '/history')
const title = computed(() => isHistory.value ? '浏览记录' : '收藏')
const activeIds = computed(() => isHistory.value ? favoriteStore.historyIds : favoriteStore.favoriteIds)

const visibleRestaurants = computed(() => {
  const map = new Map(restaurants.value.map(item => [item.id, item]))
  return activeIds.value.map(id => map.get(id)).filter((item): item is Restaurant => !!item)
})

async function loadRestaurants() {
  if (!activeIds.value.length) {
    restaurants.value = []
    error.value = ''
    return
  }
  loading.value = true
  error.value = ''
  try {
    const res = await getRestaurants({ page: 1, pageSize: 200 })
    restaurants.value = res.list
  } catch (err) {
    error.value = getErrorMessage(err, '列表加载失败')
  } finally {
    loading.value = false
  }
}

function goDiscover() {
  router.push('/discover')
}

onMounted(loadRestaurants)
watch(activeIds, loadRestaurants)
</script>

<template>
  <main class="mobile-page collection-page">
    <header class="simple-header">
      <div>
        <span>{{ isHistory ? '历史' : '已保存' }}</span>
        <h1>{{ title }}</h1>
      </div>
    </header>

    <section class="collection-tabs" aria-label="收藏和历史">
      <button type="button" :aria-pressed="!isHistory" :class="{ active: !isHistory }" @click="router.push('/favorites')">
        <strong>{{ favoriteStore.favoriteCount }}</strong>
        <span>收藏</span>
      </button>
      <button type="button" :aria-pressed="isHistory" :class="{ active: isHistory }" @click="router.push('/history')">
        <strong>{{ favoriteStore.historyCount }}</strong>
        <span>浏览</span>
      </button>
    </section>

    <section v-if="isHistory && favoriteStore.historyCount" class="tool-row">
      <button type="button" @click="favoriteStore.clearHistory">清空</button>
    </section>

    <RestaurantCardSkeleton v-if="loading" class="collection-skeleton" :count="3" />
    <AppStateCard
      v-else-if="error"
      variant="error"
      title="列表加载失败"
      :description="error"
      action-text="重试"
      @action="loadRestaurants"
    />
    <AppStateCard
      v-else-if="visibleRestaurants.length === 0"
      :title="isHistory ? '暂无浏览' : '暂无收藏'"
      description="先去发现餐厅"
      action-text="发现"
      @action="goDiscover"
    />
    <section v-else class="collection-list">
      <RestaurantCard v-for="item in visibleRestaurants" :key="item.id" :restaurant="item" />
    </section>

    <BottomNav />
  </main>
</template>

<style scoped>
.collection-page {
  background: var(--color-bg);
}

.simple-header {
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

.collection-tabs {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.collection-tabs button {
  min-height: 72px;
  border: 1px solid var(--color-border);
  border-radius: 12px;
  display: grid;
  align-content: center;
  gap: 3px;
  background: var(--color-card);
  box-shadow: var(--shadow-card);
}

.collection-tabs strong {
  color: var(--color-text);
  font-size: 20px;
}

.collection-tabs span {
  color: var(--color-muted);
  font-size: 12px;
  font-weight: 800;
}

.collection-tabs button.active {
  border-color: rgba(231, 104, 46, 0.3);
  background: var(--color-primary-light);
}

.tool-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 8px;
}

.tool-row button {
  min-height: 44px;
  border: 0;
  color: var(--color-primary);
  background: transparent;
  font-weight: 800;
}

.collection-list {
  display: grid;
  min-width: 0;
  gap: 10px;
  margin-top: 14px;
}

.state,
.collection-skeleton {
  margin: 18px auto;
}

button:active {
  transform: scale(0.98);
}
</style>
