<script setup lang="ts">
import { ref } from 'vue'
import { apiClient } from '../../api/client'

const areas = [
  { name: '中北大学周边', lng: 112.447, lat: 38.018, radius: 5000, district: '尖草坪区', pages: 3 },
  { name: '柳巷', lng: 112.563, lat: 37.873, radius: 3000, district: '迎泽区', pages: 2 },
  { name: '万达广场', lng: 112.557, lat: 37.893, radius: 3000, district: '杏花岭区', pages: 2 },
  { name: '亲贤街', lng: 112.566, lat: 37.829, radius: 3000, district: '小店区', pages: 2 },
]

const loading = ref(false)
const results = ref<{ area: string; saved: number }[]>([])
const error = ref('')

async function fetchArea(area: typeof areas[0]) {
  const resp = await apiClient.post('/admin/restaurants/amap-fetch', null, {
    params: { lng: area.lng, lat: area.lat, radius: area.radius, businessArea: area.name, district: area.district, pages: area.pages },
  })
  return { area: area.name, saved: resp.data.data?.saved || 0 }
}

async function fetchAll() {
  loading.value = true
  error.value = ''
  results.value = []
  try {
    for (const area of areas) {
      const r = await fetchArea(area)
      results.value.push(r)
    }
  } catch (e: any) {
    error.value = e?.message || '拉取失败'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="fetch-page">
    <h1>高德拉取入库</h1>
    <p>从高德 Web API 搜索周边餐厅，AI 自动补全标签后写入本地数据库。</p>

    <el-card class="fetch-card">
      <el-table :data="areas" border size="small">
        <el-table-column prop="name" label="区域" width="140" />
        <el-table-column prop="district" label="行政区" width="100" />
        <el-table-column label="坐标" width="180">
          <template #default="{ row }">{{ row.lng }}, {{ row.lat }}</template>
        </el-table-column>
        <el-table-column prop="radius" label="半径(m)" width="90" />
        <el-table-column prop="pages" label="页数" width="70" />
      </el-table>

      <div class="fetch-actions">
        <el-button type="primary" size="large" :loading="loading" @click="fetchAll">
          开始拉取全部区域
        </el-button>
      </div>

      <el-alert v-if="error" :title="error" type="error" :closable="false" style="margin-top:14px" />

      <div v-if="results.length" class="results">
        <h3>拉取结果</h3>
        <div v-for="r in results" :key="r.area" class="result-row">
          <span>{{ r.area }}</span>
          <strong>+{{ r.saved }} 家</strong>
        </div>
        <el-alert
          style="margin-top:12px"
          title="拉取完成后返回餐厅管理查看数据，AI 标签已自动生成"
          type="success" :closable="false"
        />
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.fetch-page { padding: 24px; }
h1 { margin: 0 0 4px; font-size: 22px; }
p { color: #8E8E93; font-size: 14px; margin-bottom: 20px; }
.fetch-card { max-width: 680px; }
.fetch-actions { margin-top: 16px; }
.results { margin-top: 20px; }
.results h3 { margin: 0 0 10px; font-size: 16px; }
.result-row { display: flex; justify-content: space-between; padding: 8px 0; border-bottom: 1px solid #F1E6DC; }
.result-row strong { color: var(--color-primary); }
</style>
