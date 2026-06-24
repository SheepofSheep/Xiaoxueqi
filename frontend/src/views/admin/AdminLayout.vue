<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../../stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const menuItems = [
  { path: '/admin', label: '仪表盘', icon: 'Odometer' },
  { path: '/admin/restaurants', label: '餐厅管理', icon: 'Food' },
  { path: '/admin/import', label: 'CSV 导入', icon: 'Upload' },
  { path: '/admin/amap-fetch', label: '高德拉取', icon: 'Location' },
]

function logout() {
  auth.clearAuth()
  router.push('/admin/login')
}
</script>

<template>
  <div class="admin-layout">
    <aside class="sidebar">
      <div class="sidebar-brand">
        <span class="brand-icon">食</span>
        <span class="brand-text">太原食探</span>
      </div>
      <nav class="sidebar-nav">
        <RouterLink
          v-for="item in menuItems"
          :key="item.path"
          :to="item.path"
          :class="['nav-item', { active: route.path === item.path }]"
        >
          {{ item.label }}
        </RouterLink>
      </nav>
      <div class="sidebar-footer">
        <span class="user-info">{{ auth.nickname }}</span>
        <el-button text size="small" @click="logout">退出</el-button>
      </div>
    </aside>
    <main class="admin-main">
      <RouterView />
    </main>
  </div>
</template>

<style scoped>
.admin-layout {
  display: flex;
  min-height: 100vh;
  background: #FFF7F1;
}

.sidebar {
  width: 220px;
  background: #fff;
  border-right: 1px solid #F1E6DC;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.sidebar-brand {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 20px 16px;
  border-bottom: 1px solid #F1E6DC;
}

.brand-icon {
  width: 34px;
  height: 34px;
  display: grid;
  place-items: center;
  border-radius: 13px;
  background: linear-gradient(135deg, #ff7a2e, #ff4d2e);
  color: #fff;
  font-size: 17px;
  font-weight: 900;
}
.brand-text { font-size: 17px; font-weight: 700; color: #1F1F1F; }

.sidebar-nav {
  flex: 1;
  padding: 12px 8px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.nav-item {
  display: block;
  padding: 10px 14px;
  border-radius: 10px;
  font-size: 14px;
  color: #1F1F1F;
  text-decoration: none;
  transition: background 120ms;
}
.nav-item:hover { background: #FFF7F1; }
.nav-item.active {
  background: #FFE7D1;
  color: #FF7A2E;
  font-weight: 600;
}

.sidebar-footer {
  padding: 14px 16px;
  border-top: 1px solid #F1E6DC;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.user-info { font-size: 13px; color: #8E8E93; }

.admin-main {
  flex: 1;
  overflow-y: auto;
}
</style>
