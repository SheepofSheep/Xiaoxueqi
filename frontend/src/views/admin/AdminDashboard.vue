<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getRestaurants } from '../../api/restaurants'

const total = ref(0)
const demoCount = ref(0)
const cuisineCount = ref(0)

onMounted(async () => {
  try {
    const result = await getRestaurants({ page: 1, pageSize: 500 })
    total.value = result.total
    demoCount.value = result.list.filter(r => r.isDemoData).length
    cuisineCount.value = new Set(result.list.map(r => r.cuisine)).size
  } catch { /* ignore */ }
})
</script>

<template>
  <div class="dashboard">
    <h1>仪表盘</h1>
    <div class="stat-grid">
      <div class="stat-card">
        <span class="stat-num">{{ total }}</span>
        <span class="stat-label">餐厅总数</span>
      </div>
      <div class="stat-card">
        <span class="stat-num">{{ cuisineCount }}</span>
        <span class="stat-label">菜系种类</span>
      </div>
      <div class="stat-card">
        <span class="stat-num">{{ demoCount }}</span>
        <span class="stat-label">样本数据</span>
      </div>
      <div class="stat-card">
        <span class="stat-num">0</span>
        <span class="stat-label">AI 调用次数</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.dashboard {
  padding: 24px;
}

h1 {
  margin: 0 0 20px;
  font-size: 22px;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 16px;
}

.stat-card {
  padding: 20px;
  border-radius: 16px;
  background: #fff;
  border: 1px solid #F1E6DC;
  box-shadow: 0 4px 16px rgba(0,0,0,0.04);
}

.stat-num {
  display: block;
  font-size: 32px;
  font-weight: 700;
  color: #FF7A2E;
}

.stat-label {
  display: block;
  margin-top: 4px;
  font-size: 13px;
  color: #8E8E93;
}
</style>
