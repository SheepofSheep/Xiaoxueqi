<script setup lang="ts">
import { onMounted, ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import AppTopBar from '../../components/AppTopBar.vue'
import AppStateCard from '../../components/AppStateCard.vue'
import BottomNav from '../../components/BottomNav.vue'
import RestaurantCard from '../../components/RestaurantCard.vue'
import RestaurantCardSkeleton from '../../components/RestaurantCardSkeleton.vue'
import { getErrorMessage } from '../../api/client'
import { aiRecommendRestaurants, recommendRestaurants } from '../../api/recommendations'
import type { AiFoodIntent, RecommendationItem, RecommendationRequest, RecommendationResult } from '../../types/api'

type RecommendMode = 'ai' | 'local'

const router = useRouter()
const loading = ref(false)
const error = ref('')
const items = ref<RecommendationItem[]>([])
const result = ref<RecommendationResult | null>(null)
const intent = ref<AiFoodIntent | null>(null)
const mode = ref<RecommendMode>('local')
const localRequest = ref<RecommendationRequest | null>(null)
const interpretedRequest = ref<RecommendationRequest | null>(null)

const pageTitle = computed(() => mode.value === 'ai' ? 'AI 推荐结果' : '本地算法推荐')
const modeLabel = computed(() => mode.value === 'ai' ? 'AI 已理解' : '本地排序')
const heroTitle = computed(() => items.value.length ? `找到 ${items.value.length} 家可选` : '正在整理推荐')
const heroSubtitle = computed(() => mode.value === 'ai'
  ? '先把你的描述解析成可控条件，再交给本地餐厅库排序。'
  : '按区域、预算、菜系、口味和场景完成可解释匹配。')

const topScore = computed(() => {
  const score = Math.max(0, ...items.value.map(item => item.matchScore || 0))
  return score ? `${score}%` : '--'
})

const requestTags = computed(() => {
  if (intent.value) {
    return [
      intent.value.businessArea,
      intent.value.maxPrice ? `¥${intent.value.maxPrice}以内` : '',
      ...intent.value.cuisines,
      ...intent.value.tasteTags,
      ...intent.value.sceneTags,
      ...intent.value.avoidTags.map(t => `避开${t}`),
    ].filter(Boolean)
  }
  const req = localRequest.value
  if (!req) return []
  return [
    req.businessArea,
    req.maxPrice ? `¥${req.maxPrice}以内` : '',
    ...(req.cuisines || []),
    ...(req.tasteTags || []),
    ...(req.sceneTags || []),
    ...(req.avoidTags || []).map(t => `避开${t}`),
  ].filter(Boolean)
})

const aiSummary = computed(() => {
  if (mode.value !== 'ai') return null
  const aiItems = items.value.filter(i => i.aiUsed)
  return {
    model: intent.value?.aiModel || aiItems[0]?.aiModel || '结构化条件',
    used: aiItems.length,
    total: items.value.length,
    latency: Math.max(intent.value?.aiLatencyMs || 0, ...aiItems.map(i => i.aiLatencyMs || 0)),
  }
})

function parseSaved<T>(key: string, fallback: T): T {
  const saved = sessionStorage.getItem(key)
  if (!saved) return fallback
  try {
    return JSON.parse(saved) as T
  } catch {
    return fallback
  }
}

function defaultLocalRequest(): RecommendationRequest {
  return {
    district: '尖草坪区',
    businessArea: '中北大学周边',
    maxPrice: 35,
    limit: 8,
  }
}

async function loadRecommendations(excludeCurrent = false) {
  loading.value = true
  error.value = ''
  try {
    const excludeRestaurantIds = excludeCurrent ? items.value.map(item => item.restaurant.id) : []
    mode.value = sessionStorage.getItem('recommendationMode') === 'ai' ? 'ai' : 'local'

    if (mode.value === 'ai' && !excludeCurrent) {
      const payload = parseSaved('aiRecommendationRequest', {
        prompt: '帮我在中北大学附近推荐一顿适合学生党的饭',
        district: '尖草坪区',
        businessArea: '中北大学周边',
        maxPrice: 35,
        limit: 8,
      })
      const res = await aiRecommendRestaurants(payload)
      items.value = res.recommendation.items
      result.value = res.recommendation
      intent.value = res.intent
      interpretedRequest.value = res.interpretedRequest
      localRequest.value = res.interpretedRequest
      return
    }

    const base = mode.value === 'ai'
      ? interpretedRequest.value || defaultLocalRequest()
      : parseSaved<RecommendationRequest>('recommendationRequest', defaultLocalRequest())
    const payload: RecommendationRequest = {
      ...base,
      limit: base.limit || 8,
      excludeRestaurantIds,
    }
    localRequest.value = payload
    const res = await recommendRestaurants(payload)
    items.value = res.items
    result.value = res
  } catch (err) {
    error.value = getErrorMessage(err, '推荐失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

function goSearch() {
  router.push('/search')
}

onMounted(() => loadRecommendations(false))
</script>

<template>
  <main class="mobile-page recommendation-page">
    <AppTopBar :title="pageTitle" subtitle="结果可继续换一批" />

    <section class="result-hero">
      <div class="hero-copy">
        <span class="hero-badge">{{ modeLabel }}</span>
        <h1>{{ heroTitle }}</h1>
        <p>{{ heroSubtitle }}</p>
      </div>
      <div class="hero-stats" aria-label="推荐概览">
        <div>
          <strong>{{ items.length || '--' }}</strong>
          <span>候选</span>
        </div>
        <div>
          <strong>{{ topScore }}</strong>
          <span>最高匹配</span>
        </div>
      </div>
    </section>

    <section v-if="requestTags.length || aiSummary || result?.relaxed" class="insight-card">
      <div class="insight-head">
        <strong>{{ intent?.summary || '当前推荐条件' }}</strong>
        <span v-if="result?.relaxed">已放宽</span>
      </div>
      <div v-if="requestTags.length" class="tag-cloud">
        <span v-for="tag in requestTags" :key="tag">{{ tag }}</span>
      </div>
      <div v-if="aiSummary" class="ai-line">
        <span>{{ aiSummary.model }}</span>
        <span>{{ aiSummary.used }}/{{ aiSummary.total }} 条 AI 润色</span>
        <span v-if="aiSummary.latency">{{ aiSummary.latency }}ms</span>
      </div>
      <p v-if="result?.relaxedRules?.length" class="relaxed-note">
        {{ result.relaxedRules.join('、') }}
      </p>
    </section>

    <section class="action-row" aria-label="结果操作">
      <button type="button" class="primary-action" aria-label="换一批推荐结果" :disabled="loading" @click="loadRecommendations(true)">
        {{ loading ? '正在整理' : '换一批' }}
      </button>
      <button type="button" class="secondary-action" aria-label="重新筛选推荐条件" @click="goSearch">重新筛选</button>
    </section>

    <section v-if="loading" class="state-card">
      <strong>正在从餐厅库里挑选</strong>
      <span>会优先看预算、距离、标签和适合场景。</span>
      <RestaurantCardSkeleton class="recommendation-skeleton" :count="2" />
    </section>

    <AppStateCard
      v-else-if="error"
      variant="error"
      title="推荐结果暂时没整理好"
      :description="error"
      action-text="重试"
      @action="loadRecommendations(false)"
    />

    <AppStateCard
      v-else-if="items.length === 0"
      title="暂时没有合适结果"
      description="可以放宽预算、区域或口味条件再试一次。"
      action-text="调整条件"
      @action="goSearch"
    />

    <section v-else class="result-list">
      <RestaurantCard
        v-for="item in items"
        :key="item.restaurant.id"
        :restaurant="item.restaurant"
        :match-score="item.matchScore"
        :reason="item.reason"
      />
    </section>

    <BottomNav />
  </main>
</template>

<style scoped>
.recommendation-page {
  background: linear-gradient(180deg, #fff8f2 0%, #fff4eb 44%, #fff9f5 100%);
}

.result-hero {
  min-height: 196px;
  padding: 20px;
  border-radius: 30px;
  color: #fff;
  background:
    linear-gradient(135deg, rgba(43, 22, 12, 0.96), rgba(117, 52, 23, 0.92) 58%, rgba(255, 122, 46, 0.9)),
    #2b160c;
  box-shadow: var(--shadow-float);
}

.hero-copy {
  max-width: 82%;
}

.hero-badge {
  display: inline-flex;
  padding: 5px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.14);
  font-size: 12px;
  font-weight: 800;
}

.hero-copy h1 {
  margin: 18px 0 8px;
  font-size: 30px;
  line-height: 1.12;
}

.hero-copy p {
  margin: 0;
  color: rgba(255, 255, 255, 0.82);
  font-size: 14px;
  line-height: 1.65;
}

.hero-stats {
  display: flex;
  gap: 10px;
  margin-top: 18px;
}

.hero-stats div {
  min-width: 86px;
  padding: 10px 12px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.12);
}

.hero-stats strong,
.hero-stats span {
  display: block;
}

.hero-stats strong {
  font-size: 18px;
}

.hero-stats span {
  margin-top: 2px;
  color: rgba(255, 255, 255, 0.72);
  font-size: 12px;
}

.insight-card,
.state-card {
  margin-top: 16px;
  border: 1px solid rgba(93, 45, 22, 0.08);
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: var(--shadow-card);
}

.insight-card {
  padding: 16px;
}

.insight-head {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  justify-content: space-between;
}

.insight-head strong {
  color: var(--color-text);
  font-size: 15px;
  line-height: 1.45;
}

.insight-head span {
  flex-shrink: 0;
  padding: 4px 8px;
  border-radius: 999px;
  background: #fff3ea;
  color: var(--color-primary);
  font-size: 11px;
  font-weight: 800;
}

.tag-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
  margin-top: 12px;
}

.tag-cloud span {
  padding: 6px 10px;
  border-radius: 999px;
  background: var(--color-primary-light);
  color: #9b431f;
  font-size: 12px;
  font-weight: 700;
}

.ai-line {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 12px;
  color: var(--color-muted);
  font-size: 12px;
}

.relaxed-note {
  margin: 12px 0 0;
  color: var(--color-muted);
  font-size: 12px;
  line-height: 1.6;
}

.action-row {
  display: grid;
  grid-template-columns: 1fr 0.82fr;
  gap: 10px;
  margin: 16px 0;
}

.primary-action,
.secondary-action {
  min-height: 46px;
  border-radius: 999px;
  font: inherit;
  font-weight: 800;
  cursor: pointer;
}

.primary-action {
  border: 0;
  background: linear-gradient(135deg, #ff7a2e, #ff4d2e);
  color: #fff;
  box-shadow: 0 14px 30px rgba(255, 103, 46, 0.26);
}

.primary-action:disabled {
  opacity: 0.68;
  cursor: wait;
}

.secondary-action {
  border: 1px solid rgba(255, 122, 46, 0.28);
  background: #fff;
  color: var(--color-primary);
}

.result-list {
  display: grid;
  gap: 12px;
}

.state-card {
  min-height: 190px;
  padding: 24px 18px;
  display: grid;
  place-items: center;
  align-content: center;
  gap: 10px;
  text-align: center;
}

.recommendation-skeleton {
  width: 100%;
  margin-top: 8px;
  text-align: left;
}

.state-card strong {
  color: var(--color-text);
  font-size: 17px;
}

.state-card span {
  max-width: 250px;
  color: var(--color-muted);
  font-size: 13px;
  line-height: 1.6;
}

button:active {
  transform: scale(0.97);
}

@media (max-width: 374px) {
  .hero-copy h1 {
    font-size: 27px;
  }

  .action-row {
    grid-template-columns: 1fr;
  }
}
</style>
