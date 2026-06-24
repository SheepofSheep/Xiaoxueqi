<script setup lang="ts">
import { ref } from 'vue'
import type { Restaurant } from '../types/api'
import { useFavoriteStore } from '../stores/favorites'

defineProps<{
  restaurant: Restaurant
  matchScore?: number
  reason?: string
}>()

const favoriteStore = useFavoriteStore()
const failedImageIds = ref(new Set<number>())

function hasUsableImage(restaurant: Restaurant) {
  return !!restaurant.coverImage && !failedImageIds.value.has(restaurant.id)
}

function markImageFailed(restaurantId: number) {
  const next = new Set(failedImageIds.value)
  next.add(restaurantId)
  failedImageIds.value = next
}
</script>

<template>
  <article class="resto-card">
    <div class="card-img-wrap">
      <img
        v-if="hasUsableImage(restaurant)"
        :src="restaurant.coverImage"
        class="card-img"
        :alt="restaurant.name"
        @error="markImageFailed(restaurant.id)"
      />
      <span v-else class="card-img-placeholder" aria-hidden="true">
        <van-icon name="shop-o" size="24" />
      </span>
    </div>
    <div class="card-body">
      <div class="card-top">
        <h3 class="card-name">{{ restaurant.name }}</h3>
        <button
          type="button"
          :class="['favorite-button', { active: favoriteStore.isFavorite(restaurant.id) }]"
          :aria-label="favoriteStore.isFavorite(restaurant.id) ? '取消收藏' : '收藏餐厅'"
          :aria-pressed="favoriteStore.isFavorite(restaurant.id)"
          @click="favoriteStore.toggleFavorite(restaurant.id)"
        >
          <van-icon :name="favoriteStore.isFavorite(restaurant.id) ? 'star' : 'star-o'" size="15" />
        </button>
        <span v-if="matchScore !== undefined" class="match-badge">{{ Math.round(matchScore) }}%</span>
      </div>
      <div class="card-meta">
        <span class="meta-star">
          <van-icon name="star" size="12" />
          {{ restaurant.rating }}
        </span>
        <span class="meta-price">¥{{ restaurant.averagePrice }}</span>
        <span class="meta-area">{{ restaurant.businessArea || restaurant.district }}</span>
      </div>
      <div class="card-tags">
        <span class="tag tag-cuisine">{{ restaurant.cuisine }}</span>
        <span v-for="tag in (restaurant.tasteTags || []).slice(0, 3)" :key="tag" class="tag tag-taste">{{ tag }}</span>
        <span v-for="tag in (restaurant.sceneTags || []).slice(0, 2)" :key="tag" class="tag tag-scene">{{ tag }}</span>
      </div>
      <p v-if="reason" class="card-reason">{{ reason }}</p>
      <p v-else-if="(restaurant.recommendedDishes || []).length" class="card-dishes">
        {{ (restaurant.recommendedDishes || []).slice(0, 2).join(' / ') }}
      </p>
    </div>
    <RouterLink :to="`/restaurants/${restaurant.id}`" class="card-hit" :aria-label="`查看${restaurant.name}`" />
  </article>
</template>

<style scoped>
.resto-card {
  position: relative;
  display: flex;
  width: 100%;
  min-width: 0;
  gap: 10px;
  padding: 10px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-card);
  background: var(--color-card);
  box-shadow: var(--shadow-card);
  overflow: hidden;
}

.card-img-wrap {
  flex-shrink: 0;
  width: 82px;
  height: 82px;
  border-radius: 10px;
  background: var(--color-soft);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.card-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.card-img-placeholder {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: grid;
  place-items: center;
  background: var(--color-card);
  color: var(--color-primary);
  box-shadow: inset 0 0 0 1px var(--color-border);
}

.card-body {
  flex: 1;
  min-width: 0;
}

.card-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 6px;
  min-width: 0;
}

.card-name {
  flex: 1;
  min-width: 0;
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  line-height: 1.3;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.match-badge {
  flex-shrink: 0;
  padding: 3px 8px;
  border-radius: 8px;
  background: var(--color-primary-light);
  color: var(--color-primary-strong);
  font-size: 12px;
  font-weight: 700;
}

.favorite-button {
  position: relative;
  z-index: 2;
  width: 36px;
  height: 36px;
  flex-shrink: 0;
  border: 1px solid var(--color-border);
  border-radius: 10px;
  display: grid;
  place-items: center;
  color: var(--color-muted);
  background: #fff;
}

.favorite-button.active {
  color: var(--color-primary);
  background: #f8e2d4;
}

.card-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 10px;
  margin-top: 6px;
  font-size: 12px;
  color: var(--color-muted);
}

.meta-star {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  color: var(--color-star);
  font-weight: 700;
}
.meta-price { color: var(--color-primary); font-weight: 600; }
.meta-area {
  min-width: 0;
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
  margin-top: 7px;
  max-height: 26px;
  overflow: hidden;
}

.tag {
  max-width: 78px;
  padding: 3px 7px;
  border-radius: var(--radius-tag);
  font-size: 11px;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tag-cuisine {
  background: var(--color-primary-light);
  color: var(--color-primary-strong);
  font-weight: 500;
}

.tag-taste {
  background: #F7F7F8;
  color: var(--color-muted);
}

.tag-scene {
  background: #FFF7F1;
  color: var(--color-brand-dark);
}

.card-reason {
  margin: 8px 0 0;
  font-size: 12px;
  line-height: 1.45;
  color: var(--color-muted);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-dishes {
  margin: 6px 0 0;
  font-size: 12px;
  color: var(--color-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-hit {
  position: absolute;
  inset: 0;
  z-index: 1;
}

.favorite-button,
.card-img-wrap {
  position: relative;
  z-index: 2;
}

@media (max-width: 374px) {
  .resto-card {
    gap: 9px;
    padding: 9px;
  }

  .card-img-wrap {
    width: 70px;
    height: 70px;
  }

  .tag {
    max-width: 68px;
  }
}
</style>
