<script setup lang="ts">
import { ref } from 'vue'
import { importRestaurantsCsv } from '../../api/restaurants'
import type { RestaurantImportResult } from '../../types/api'

const fileInputRef = ref<HTMLInputElement | null>(null)
const selectedFile = ref<File | null>(null)
const uploading = ref(false)
const errorMessage = ref('')
const result = ref<RestaurantImportResult | null>(null)

function handleFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  selectedFile.value = input.files?.[0] || null
  errorMessage.value = ''
  result.value = null
}

async function upload() {
  if (!selectedFile.value) {
    errorMessage.value = '请先选择 CSV 文件'
    return
  }
  uploading.value = true
  errorMessage.value = ''
  try {
    result.value = await importRestaurantsCsv(selectedFile.value)
    selectedFile.value = null
    if (fileInputRef.value) fileInputRef.value.value = ''
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'CSV 导入失败'
  } finally {
    uploading.value = false
  }
}
</script>

<template>
  <main class="admin-page">
    <header class="admin-header">
      <div>
        <h1>CSV 导入</h1>
        <span class="sub">太原食探 · 管理后台</span>
      </div>
      <RouterLink to="/admin/restaurants">
        <el-button>返回餐厅管理</el-button>
      </RouterLink>
    </header>

    <el-card class="admin-card">
      <el-alert
        title="CSV 模板：data/restaurants_template.csv。支持同名+同地址更新。"
        type="info"
        :closable="false"
      />

      <div class="upload-row">
        <input ref="fileInputRef" type="file" accept=".csv,text/csv" @change="handleFileChange" />
        <el-button type="primary" :loading="uploading" :disabled="!selectedFile" @click="upload">
          上传 CSV
        </el-button>
      </div>

      <p v-if="selectedFile" class="file-name">已选择：{{ selectedFile.name }}</p>

      <el-alert v-if="errorMessage" class="result-alert" :title="errorMessage" type="error" :closable="false" />

      <section v-if="result" class="result-block">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="成功行数">{{ result.successCount }}</el-descriptions-item>
          <el-descriptions-item label="失败行数">{{ result.failureCount }}</el-descriptions-item>
          <el-descriptions-item label="新增">{{ result.createdCount }}</el-descriptions-item>
          <el-descriptions-item label="更新">{{ result.updatedCount }}</el-descriptions-item>
        </el-descriptions>

        <el-table v-if="result.errors.length" class="error-table" :data="result.errors" size="small">
          <el-table-column prop="rowNumber" label="行号" width="90" />
          <el-table-column prop="message" label="错误原因" />
        </el-table>
      </section>
    </el-card>
  </main>
</template>

<style scoped>
.admin-page {
  min-height: 100vh;
  padding: 24px;
  background: #FFF7F1;
}

.admin-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

h1 {
  margin: 0;
  font-size: 22px;
  color: #1F1F1F;
}

.sub {
  font-size: 13px;
  color: #8E8E93;
}

.admin-card {
  max-width: 880px;
}

.upload-row {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-top: 18px;
}

.file-name {
  margin: 12px 0 0;
  color: #606266;
}

.result-alert,
.result-block {
  margin-top: 18px;
}

.error-table {
  margin-top: 16px;
}
</style>
