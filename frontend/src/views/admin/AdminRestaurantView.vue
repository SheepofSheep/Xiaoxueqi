<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createRestaurant,
  disableRestaurant,
  getRestaurants,
  updateRestaurant,
  type RestaurantPayload,
} from '../../api/restaurants'
import type { Restaurant } from '../../types/api'

const loading = ref(false)
const saving = ref(false)
const error = ref('')
const restaurants = ref<Restaurant[]>([])
const total = ref(0)
const keyword = ref('')
const page = ref(1)
const pageSize = ref(20)
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)

const form = reactive({
  name: '',
  district: '尖草坪区',
  businessArea: '中北大学周边',
  address: '',
  averagePrice: 30,
  cuisine: '面食',
  rating: 4.5,
  tasteTags: '',
  sceneTags: '',
  avoidTags: '',
  recommendedDishes: '',
  description: '',
  latitude: undefined as number | undefined,
  longitude: undefined as number | undefined,
  coverImage: '',
  sourceNote: '后台人工维护',
  isDemoData: 0,
})

function textToList(value: string) {
  return value.split(/[|,，、\s]+/).map(item => item.trim()).filter(Boolean)
}

function listToText(value?: string[]) {
  return (value || []).join('、')
}

function resetForm() {
  editingId.value = null
  Object.assign(form, {
    name: '',
    district: '尖草坪区',
    businessArea: '中北大学周边',
    address: '',
    averagePrice: 30,
    cuisine: '面食',
    rating: 4.5,
    tasteTags: '',
    sceneTags: '',
    avoidTags: '',
    recommendedDishes: '',
    description: '',
    latitude: undefined,
    longitude: undefined,
    coverImage: '',
    sourceNote: '后台人工维护',
    isDemoData: 0,
  })
}

function openCreate() {
  resetForm()
  dialogVisible.value = true
}

function openEdit(row: Restaurant) {
  editingId.value = row.id
  Object.assign(form, {
    name: row.name,
    district: row.district,
    businessArea: row.businessArea || '',
    address: row.address || '',
    averagePrice: row.averagePrice,
    cuisine: row.cuisine,
    rating: row.rating,
    tasteTags: listToText(row.tasteTags),
    sceneTags: listToText(row.sceneTags),
    avoidTags: listToText(row.avoidTags),
    recommendedDishes: listToText(row.recommendedDishes),
    description: row.description || '',
    latitude: row.latitude,
    longitude: row.longitude,
    coverImage: row.coverImage || '',
    sourceNote: row.sourceNote || '后台人工维护',
    isDemoData: row.isDemoData ? 1 : 0,
  })
  dialogVisible.value = true
}

function toPayload(): RestaurantPayload {
  return {
    name: form.name.trim(),
    district: form.district.trim(),
    businessArea: form.businessArea.trim(),
    address: form.address.trim(),
    averagePrice: Number(form.averagePrice) || 0,
    cuisine: form.cuisine.trim(),
    rating: Number(form.rating) || 0,
    tasteTags: textToList(form.tasteTags),
    sceneTags: textToList(form.sceneTags),
    avoidTags: textToList(form.avoidTags),
    recommendedDishes: textToList(form.recommendedDishes),
    description: form.description.trim(),
    latitude: form.latitude,
    longitude: form.longitude,
    coverImage: form.coverImage.trim(),
    sourceNote: form.sourceNote.trim(),
    isDemoData: form.isDemoData,
  }
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const result = await getRestaurants({ keyword: keyword.value.trim(), page: page.value, pageSize: pageSize.value })
    restaurants.value = result.list
    total.value = result.total
  } catch {
    error.value = '餐厅数据加载失败'
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 1
  load()
}

async function submitForm() {
  if (!form.name.trim() || !form.district.trim() || !form.cuisine.trim()) {
    ElMessage.warning('名称、区域和菜系必填')
    return
  }
  saving.value = true
  try {
    if (editingId.value) {
      await updateRestaurant(editingId.value, toPayload())
      ElMessage.success('餐厅已更新')
    } else {
      await createRestaurant(toPayload())
      ElMessage.success('餐厅已新增')
    }
    dialogVisible.value = false
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败')
  } finally {
    saving.value = false
  }
}

async function handleDisable(row: Restaurant) {
  try {
    await ElMessageBox.confirm(`确认下架「${row.name}」？下架后用户端不再展示。`, '下架餐厅', {
      type: 'warning',
      confirmButtonText: '确认下架',
      cancelButtonText: '取消',
    })
    await disableRestaurant(row.id)
    ElMessage.success('已下架')
    await load()
  } catch {
    // 用户取消时不提示。
  }
}

onMounted(load)
</script>

<template>
  <main class="admin-page">
    <header class="admin-header">
      <div>
        <h1>餐厅管理</h1>
        <span class="sub">太原食探 · 管理后台</span>
      </div>
      <div class="header-actions">
        <RouterLink to="/admin/import">
          <el-button>CSV 导入</el-button>
        </RouterLink>
        <el-button type="primary" @click="openCreate">新增餐厅</el-button>
      </div>
    </header>

    <el-card class="admin-card">
      <div class="toolbar">
        <el-input
          v-model="keyword"
          clearable
          placeholder="搜索餐厅、地址、标签"
          style="max-width: 320px"
          @keyup.enter="search"
          @clear="search"
        />
        <el-button type="primary" :loading="loading" @click="search">搜索</el-button>
        <span class="total-text">共 {{ total }} 家上架餐厅</span>
      </div>

      <el-alert v-if="error" :title="error" type="error" :closable="false" style="margin-bottom:16px" />
      <el-table v-loading="loading" :data="restaurants" border stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" label="名称" min-width="180" />
        <el-table-column prop="district" label="区域" width="120" />
        <el-table-column prop="businessArea" label="商圈" width="150" />
        <el-table-column prop="cuisine" label="菜系" width="100" />
        <el-table-column prop="averagePrice" label="人均" width="90" />
        <el-table-column prop="rating" label="评分" width="80" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '上架' : '下架' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDisable(row)">下架</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="page"
        v-model:page-size="pageSize"
        class="pagination"
        background
        layout="total, sizes, prev, pager, next"
        :total="total"
        :page-sizes="[10, 20, 50, 80]"
        @current-change="load"
        @size-change="search"
      />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑餐厅' : '新增餐厅'" width="760px">
      <el-form label-width="92px" class="restaurant-form">
        <el-row :gutter="14">
          <el-col :span="12">
            <el-form-item label="名称">
              <el-input v-model="form.name" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="菜系">
              <el-input v-model="form.cuisine" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="区域">
              <el-input v-model="form.district" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="商圈">
              <el-input v-model="form.businessArea" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="人均">
              <el-input-number v-model="form.averagePrice" :min="0" :precision="0" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="评分">
              <el-input-number v-model="form.rating" :min="0" :max="5" :step="0.1" :precision="1" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="地址">
              <el-input v-model="form.address" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="纬度">
              <el-input-number v-model="form.latitude" :precision="6" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="经度">
              <el-input-number v-model="form.longitude" :precision="6" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="图片">
              <el-input v-model="form.coverImage" placeholder="coverImage URL，可为空" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="口味标签">
              <el-input v-model="form.tasteTags" placeholder="用空格、逗号或顿号分隔" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="场景标签">
              <el-input v-model="form.sceneTags" placeholder="如 一人食、学生常去、夜宵" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="忌口标签">
              <el-input v-model="form.avoidTags" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="推荐菜">
              <el-input v-model="form.recommendedDishes" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="简介">
              <el-input v-model="form.description" type="textarea" :rows="3" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="来源说明">
              <el-input v-model="form.sourceNote" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>
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
  gap: 16px;
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

.header-actions,
.toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
}

.admin-card {
  max-width: 1180px;
}

.toolbar {
  margin-bottom: 16px;
}

.total-text {
  color: #8E8E93;
  font-size: 13px;
}

.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}

.restaurant-form :deep(.el-input-number) {
  width: 100%;
}
</style>
