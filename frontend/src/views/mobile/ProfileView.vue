<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import BottomNav from '../../components/BottomNav.vue'
import { getRestaurants } from '../../api/restaurants'
import { useFavoriteStore } from '../../stores/favorites'

const router = useRouter()
const favoriteStore = useFavoriteStore()
const restaurantCount = ref(0)

const stats = computed(() => [
  { label: '收藏', value: favoriteStore.favoriteCount, path: '/favorites' },
  { label: '浏览', value: favoriteStore.historyCount, path: '/history' },
  { label: '餐厅', value: restaurantCount.value, path: '/discover' },
])

const functionItems = [
  { label: 'AI 推荐', icon: 'search', path: '/search' },
  { label: '口味魔镜', icon: 'smile-o', path: '/taste-mirror' },
  { label: '摇一摇', icon: 'exchange', path: '/shake' },
  { label: '附近', icon: 'location-o', path: '/nearby' },
  { label: '榜单', icon: 'bar-chart-o', path: '/discover' },
  { label: '收藏', icon: 'star-o', path: '/favorites' },
  { label: '浏览', icon: 'clock-o', path: '/history' },
]

function go(path: string) {
  router.push(path)
}

onMounted(async () => {
  try {
    const res = await getRestaurants({ page: 1, pageSize: 1 })
    restaurantCount.value = res.total
  } catch {
    restaurantCount.value = 0
  }
})
</script>

<template>
  <main class="mobile-page profile-page">
    <header class="profile-header">
      <div class="avatar" aria-hidden="true">
        <van-icon name="user-o" size="24" />
      </div>
      <div>
        <span>我的</span>
        <h1>太原食探</h1>
      </div>
    </header>

    <section class="stats-row" aria-label="个人数据">
      <button v-for="item in stats" :key="item.label" type="button" @click="go(item.path)">
        <strong>{{ item.value }}</strong>
        <span>{{ item.label }}</span>
      </button>
    </section>

    <section class="function-panel">
      <h2>功能</h2>
      <div class="function-grid">
        <button v-for="item in functionItems" :key="item.label" type="button" @click="go(item.path)">
          <van-icon :name="item.icon" size="19" />
          <span>{{ item.label }}</span>
        </button>
      </div>
    </section>

    <BottomNav />
  </main>
</template>

<style scoped>
.profile-page {
  background: var(--color-bg);
}

.profile-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px;
  border: 1px solid var(--color-border);
  border-radius: 14px;
  background: var(--color-card);
  box-shadow: var(--shadow-card);
}

.profile-header > div:last-child {
  min-width: 0;
}

.avatar {
  width: 52px;
  height: 52px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  flex-shrink: 0;
  color: var(--color-primary);
  background: var(--color-primary-light);
}

.profile-header span {
  color: var(--color-primary);
  font-size: 12px;
  font-weight: 800;
}

.profile-header h1 {
  margin: 4px 0 0;
  color: var(--color-text);
  font-size: 22px;
  line-height: 1.25;
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin-top: 12px;
}

.stats-row button {
  min-height: 72px;
  border: 1px solid var(--color-border);
  border-radius: 12px;
  background: var(--color-card);
  box-shadow: var(--shadow-card);
}

.stats-row strong,
.stats-row span {
  display: block;
}

.stats-row strong {
  color: var(--color-text);
  font-size: 20px;
}

.stats-row span {
  margin-top: 4px;
  color: var(--color-muted);
  font-size: 12px;
  font-weight: 800;
}

.function-panel {
  margin-top: 18px;
  padding: 14px;
  border: 1px solid var(--color-border);
  border-radius: 14px;
  background: var(--color-card);
  box-shadow: var(--shadow-card);
}

.function-panel h2 {
  margin: 0 0 10px;
  color: var(--color-text);
  font-size: 18px;
}

.function-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.function-grid button {
  min-height: 78px;
  border: 1px solid var(--color-border);
  border-radius: 12px;
  display: grid;
  place-items: center;
  align-content: center;
  gap: 6px;
  color: var(--color-text);
  background: #fff;
}

.function-grid .van-icon {
  color: var(--color-primary);
}

.function-grid span {
  max-width: 100%;
  font-size: 12px;
  font-weight: 800;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

button:active {
  transform: scale(0.98);
}

@media (max-width: 374px) {
  .function-grid {
    gap: 7px;
  }

  .function-grid button {
    min-height: 74px;
  }
}
</style>
