<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const activeName = computed(() => {
  if (route.path === '/') return '/'
  if (route.path.startsWith('/nearby')) return '/nearby'
  if (route.path.startsWith('/favorites') || route.path.startsWith('/history')) return '/favorites'
  if (route.path.startsWith('/profile')) return '/profile'
  return '/discover'
})

function navigate(path: string) {
  if (activeName.value !== path) {
    router.push(path)
  }
}
</script>

<template>
  <van-tabbar :model-value="activeName" fixed placeholder safe-area-inset-bottom @change="navigate">
    <van-tabbar-item name="/" icon="home-o">首页</van-tabbar-item>
    <van-tabbar-item name="/nearby" icon="location-o">附近</van-tabbar-item>
    <van-tabbar-item name="/discover" icon="shop-o">发现</van-tabbar-item>
    <van-tabbar-item name="/favorites" icon="star-o">收藏</van-tabbar-item>
    <van-tabbar-item name="/profile" icon="user-o">我的</van-tabbar-item>
  </van-tabbar>
</template>
