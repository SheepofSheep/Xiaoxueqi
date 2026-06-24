<script setup lang="ts">
import { computed, reactive } from 'vue'
import { useRouter } from 'vue-router'
import AppTopBar from '../../components/AppTopBar.vue'
import BottomNav from '../../components/BottomNav.vue'
import type { RecommendationRequest } from '../../types/api'

const router = useRouter()

const form = reactive({
  spicy: '微辣',
  budget: 35,
  cuisine: '面食',
  priority: '性价比',
  scene: '一人食',
  tryNew: '稳妥',
})

const questions = [
  { key: 'spicy', title: '今天想要什么口味', options: ['清淡', '微辣', '重辣', '咸香', '酸甜'] },
  { key: 'budget', title: '预算上限', options: [20, 35, 50, 80] },
  { key: 'cuisine', title: '更想吃哪类', options: ['面食', '麻辣烫', '火锅', '烧烤', '快餐', '饮品'] },
  { key: 'priority', title: '最看重什么', options: ['性价比', '评分高', '学生党', '赶时间'] },
  { key: 'scene', title: '当前场景', options: ['一人食', '朋友聚餐', '夜宵', '赶时间', '打卡'] },
  { key: 'tryNew', title: '尝鲜程度', options: ['稳妥', '打卡'] },
] as const

const profileName = computed(() => {
  if (form.scene === '夜宵') return '夜宵雷达型'
  if (form.priority === '性价比') return '学生省钱型'
  if (form.tryNew === '打卡') return '探索尝鲜型'
  return '稳妥好吃型'
})

const summary = computed(() => [
  form.spicy,
  `¥${form.budget}以内`,
  form.cuisine,
  form.priority,
  form.scene,
].join(' · '))

function selectValue(key: keyof typeof form, value: string | number) {
  if (key === 'budget' && typeof value === 'number') {
    form.budget = value
    return
  }
  if (key !== 'budget' && typeof value === 'string') {
    form[key] = value
  }
}

function isSelected(key: keyof typeof form, value: string | number) {
  return form[key] === value
}

function submitMirror() {
  const prioritySceneTags = ['性价比', '学生党', '赶时间'].includes(form.priority) ? [form.priority] : []
  const tryNewSceneTags = form.tryNew === '打卡' ? ['打卡'] : []
  const tasteTags = [form.spicy].filter(Boolean)
  const sceneTags = Array.from(new Set([form.scene, ...prioritySceneTags, ...tryNewSceneTags].filter(Boolean)))
  const payload: RecommendationRequest = {
    district: '尖草坪区',
    businessArea: '中北大学周边',
    maxPrice: form.budget,
    cuisines: [form.cuisine],
    tasteTags,
    sceneTags,
    limit: 8,
  }
  sessionStorage.setItem('recommendationMode', 'local')
  sessionStorage.setItem('recommendationRequest', JSON.stringify(payload))
  router.push('/recommendations')
}
</script>

<template>
  <main class="mobile-page taste-page">
    <AppTopBar title="口味魔镜" subtitle="按今天的状态生成推荐画像" />

    <section class="mirror-hero">
      <span>你的画像</span>
      <h1>{{ profileName }}</h1>
      <p>{{ summary }}</p>
    </section>

    <section class="question-list">
      <div v-for="question in questions" :key="question.key" class="question-card">
        <strong>{{ question.title }}</strong>
        <div class="option-row">
          <button
            v-for="option in question.options"
            :key="String(option)"
            type="button"
            :class="{ active: isSelected(question.key, option) }"
            @click="selectValue(question.key, option)"
          >
            {{ typeof option === 'number' ? `¥${option}` : option }}
          </button>
        </div>
      </div>
    </section>

    <section class="submit-panel">
      <button type="button" @click="submitMirror">按画像推荐</button>
      <span>基于已收录餐厅排序，结果可解释、可继续筛选。</span>
    </section>

    <BottomNav />
  </main>
</template>

<style scoped>
.taste-page {
  background: linear-gradient(180deg, #fff8f2 0%, #fff4eb 46%, #fff9f5 100%);
}

.mirror-hero {
  min-height: 190px;
  border-radius: 30px;
  padding: 22px;
  color: #fff;
  background:
    linear-gradient(135deg, rgba(43, 22, 12, 0.96), rgba(255, 122, 46, 0.88)),
    url('/assets/taiyuan-food-hero.png') center/cover;
  box-shadow: var(--shadow-float);
}

.mirror-hero span {
  display: inline-flex;
  padding: 5px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.14);
  font-size: 12px;
  font-weight: 900;
}

.mirror-hero h1 {
  margin: 18px 0 8px;
  font-size: 31px;
  line-height: 1.14;
}

.mirror-hero p {
  margin: 0;
  color: rgba(255, 255, 255, 0.82);
  font-size: 14px;
  line-height: 1.6;
}

.question-list {
  display: grid;
  gap: 12px;
  margin-top: 18px;
}

.question-card {
  border: 1px solid rgba(93, 45, 22, 0.08);
  border-radius: 22px;
  padding: 16px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 12px 34px rgba(64, 32, 15, 0.06);
}

.question-card strong {
  display: block;
  color: var(--color-text);
  font-size: 16px;
}

.option-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.option-row button {
  min-height: 44px;
  border: 1px solid rgba(93, 45, 22, 0.1);
  border-radius: 999px;
  padding: 0 13px;
  color: #765747;
  background: #fff;
  font-weight: 800;
}

.option-row button.active {
  border-color: var(--color-primary);
  color: #fff;
  background: var(--color-primary);
}

.option-row button:active,
.submit-panel button:active {
  transform: scale(0.97);
}

.submit-panel {
  position: sticky;
  bottom: calc(74px + env(safe-area-inset-bottom));
  z-index: 3;
  margin-top: 18px;
  border: 1px solid rgba(255, 122, 46, 0.2);
  border-radius: 22px;
  padding: 12px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 16px 42px rgba(43, 22, 12, 0.14);
}

.submit-panel button {
  width: 100%;
  min-height: 46px;
  border: 0;
  border-radius: 999px;
  color: #fff;
  background: var(--color-primary);
  font-size: 16px;
  font-weight: 900;
}

.submit-panel span {
  display: block;
  margin-top: 8px;
  color: var(--color-muted);
  font-size: 12px;
  line-height: 1.5;
  text-align: center;
}

@media (max-width: 374px) {
  .mirror-hero h1 {
    font-size: 28px;
  }

  .question-card {
    padding: 14px;
  }

  .option-row button {
    padding: 0 11px;
  }
}
</style>
