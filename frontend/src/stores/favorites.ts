import { defineStore } from 'pinia'
import { computed, ref, watch } from 'vue'

const FAVORITE_KEY = 'taiyuan_food_favorites'
const HISTORY_KEY = 'taiyuan_food_history'

function readIds(key: string) {
  try {
    const value = JSON.parse(localStorage.getItem(key) || '[]')
    return Array.isArray(value) ? value.filter((id): id is number => Number.isInteger(id)) : []
  } catch {
    return []
  }
}

function uniqueHead(ids: number[], id: number, limit = 30) {
  return [id, ...ids.filter(item => item !== id)].slice(0, limit)
}

export const useFavoriteStore = defineStore('favorites', () => {
  const favoriteIds = ref<number[]>(readIds(FAVORITE_KEY))
  const historyIds = ref<number[]>(readIds(HISTORY_KEY))

  const favoriteCount = computed(() => favoriteIds.value.length)
  const historyCount = computed(() => historyIds.value.length)

  function isFavorite(id: number) {
    return favoriteIds.value.includes(id)
  }

  function toggleFavorite(id: number) {
    favoriteIds.value = isFavorite(id)
      ? favoriteIds.value.filter(item => item !== id)
      : uniqueHead(favoriteIds.value, id, 80)
  }

  function addHistory(id: number) {
    historyIds.value = uniqueHead(historyIds.value, id)
  }

  function clearHistory() {
    historyIds.value = []
  }

  watch(favoriteIds, ids => {
    localStorage.setItem(FAVORITE_KEY, JSON.stringify(ids))
  }, { deep: true })

  watch(historyIds, ids => {
    localStorage.setItem(HISTORY_KEY, JSON.stringify(ids))
  }, { deep: true })

  return {
    favoriteIds,
    historyIds,
    favoriteCount,
    historyCount,
    isFavorite,
    toggleFavorite,
    addHistory,
    clearHistory,
  }
})
