<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import AppTopBar from '../../components/AppTopBar.vue'
import BottomNav from '../../components/BottomNav.vue'

type RecommendMode = 'ai' | 'local'

interface Preset {
  label: string
  district?: string
  businessArea?: string
  maxPrice: number
  cuisines: string[]
  tasteTags: string[]
  sceneTags: string[]
}

const router = useRouter()

const mode = ref<RecommendMode>('ai')
const aiPrompt = ref('我想在中北大学附近吃点微辣、30元以内、一个人也方便的饭')

const districts = ['尖草坪区', '小店区', '迎泽区', '杏花岭区', '万柏林区', '晋源区']
const businessAreas = ['中北大学周边', '柳巷', '万达广场', '亲贤街', '太原南站周边']
const cuisines = ['面食', '麻辣烫', '火锅', '烧烤', '快餐', '饮品', '小吃', '川菜', '黄焖鸡']
const tasteTags = ['清淡', '微辣', '中辣', '重辣', '麻辣', '咸香', '酸甜', '碳水', '甜口', '下饭']
const sceneTags = ['一人食', '学生党', '朋友聚餐', '夜宵', '约会', '快餐', '性价比', '赶时间']

const aiExamples = [
  '想吃点辣的，别太贵，最好离中北大学近',
  '今晚朋友聚餐，想吃火锅或者烧烤，人均60以内',
  '一个人吃饭，想要快一点、清淡一点',
]

const presets: Preset[] = [
  { label: '学生党面食', district: '尖草坪区', businessArea: '中北大学周边', maxPrice: 25, cuisines: ['面食'], tasteTags: ['碳水', '咸香'], sceneTags: ['学生党', '一人食'] },
  { label: '朋友聚餐火锅', district: '尖草坪区', businessArea: '中北大学周边', maxPrice: 60, cuisines: ['火锅'], tasteTags: ['麻辣'], sceneTags: ['朋友聚餐'] },
  { label: '夜宵烧烤', maxPrice: 50, cuisines: ['烧烤'], tasteTags: ['重辣', '油香'], sceneTags: ['夜宵', '朋友聚餐'] },
  { label: '快速快餐', maxPrice: 30, cuisines: ['快餐'], tasteTags: [], sceneTags: ['快餐', '赶时间'] },
  { label: '一人食麻辣烫', maxPrice: 30, cuisines: ['麻辣烫'], tasteTags: ['微辣', '麻辣'], sceneTags: ['学生党', '一人食'] },
  { label: '小吃随便吃', maxPrice: 20, cuisines: ['小吃'], tasteTags: [], sceneTags: ['一人食', '学生党'] },
]

const form = reactive({
  district: '尖草坪区',
  businessArea: '中北大学周边',
  maxPrice: 35,
  cuisines: [] as string[],
  tasteTags: [] as string[],
  sceneTags: [] as string[],
})

const showDistrict = ref(false)
const showBusinessArea = ref(false)
const showPrice = ref(false)

function toggleTag(arr: string[], tag: string) {
  const i = arr.indexOf(tag)
  if (i >= 0) arr.splice(i, 1)
  else arr.push(tag)
}

function applyPreset(p: Preset) {
  if (p.district) form.district = p.district
  if (p.businessArea) form.businessArea = p.businessArea
  form.maxPrice = p.maxPrice
  form.cuisines = [...p.cuisines]
  form.tasteTags = [...p.tasteTags]
  form.sceneTags = [...p.sceneTags]
  mode.value = 'local'
}

function submitAi() {
  const prompt = aiPrompt.value.trim() || '帮我在中北大学附近推荐一顿适合学生党的饭'
  sessionStorage.setItem('recommendationMode', 'ai')
  sessionStorage.setItem('aiRecommendationRequest', JSON.stringify({
    prompt,
    district: form.district,
    businessArea: form.businessArea,
    maxPrice: form.maxPrice,
    limit: 8,
  }))
  router.push('/recommendations')
}

function submitLocal() {
  sessionStorage.setItem('recommendationMode', 'local')
  sessionStorage.setItem('recommendationRequest', JSON.stringify({
    district: form.district,
    businessArea: form.businessArea,
    maxPrice: form.maxPrice,
    cuisines: form.cuisines,
    tasteTags: form.tasteTags,
    sceneTags: form.sceneTags,
    limit: 8,
  }))
  router.push('/recommendations')
}
</script>

<template>
  <main class="mobile-page seek-page">
    <AppTopBar title="开始觅食" subtitle="AI 推荐 / 本地算法" />

    <section class="seek-hero">
      <div class="hero-copy">
        <span class="hero-kicker">太原食探</span>
        <h1>今天想吃什么？</h1>
        <p>用一句话交给 AI，或自己筛选让本地算法排序。</p>
      </div>
      <img src="/assets/taiyuan-food-hero.png" alt="" class="hero-art" />
    </section>

    <section class="mode-grid" aria-label="推荐方式">
      <button type="button" :aria-pressed="mode === 'ai'" :class="['mode-card', { active: mode === 'ai' }]" @click="mode = 'ai'">
        <span class="mode-icon mode-ai">
          <van-icon name="search" size="18" />
        </span>
        <strong>AI 推荐</strong>
        <small>自然语言理解</small>
      </button>
      <button type="button" :aria-pressed="mode === 'local'" :class="['mode-card', { active: mode === 'local' }]" @click="mode = 'local'">
        <span class="mode-icon mode-local">
          <van-icon name="filter-o" size="18" />
        </span>
        <strong>本地算法</strong>
        <small>条件匹配排序</small>
      </button>
    </section>

    <section v-if="mode === 'ai'" class="ai-panel">
      <label class="ai-label" for="aiPrompt">直接告诉 AI 你想吃什么</label>
      <textarea
        id="aiPrompt"
        v-model="aiPrompt"
        class="ai-input"
        rows="5"
        placeholder="比如：我想吃点辣的，30元以内，离中北大学近一点"
      />
      <div class="example-row">
        <button v-for="text in aiExamples" :key="text" type="button" :aria-label="`填入示例：${text}`" @click="aiPrompt = text">
          {{ text }}
        </button>
      </div>
      <van-button block class="submit-btn" round @click="submitAi">让 AI 帮我找</van-button>
    </section>

    <template v-else>
      <section class="preset-section">
        <button
          v-for="p in presets"
          :key="p.label"
          type="button"
          class="preset-chip"
          :aria-label="`套用${p.label}条件`"
          @click="applyPreset(p)"
        >{{ p.label }}</button>
      </section>

      <section class="form-panel">
        <button type="button" class="pick-row" @click="showDistrict = true">
          <span class="pick-label">区域</span>
          <span class="pick-val">{{ form.district }}</span>
          <van-icon name="arrow" size="14" color="#8E8E93" />
        </button>
        <van-action-sheet v-model:show="showDistrict" :actions="districts.map(d => ({ name: d }))" @select="(a: any) => { form.district = a.name; showDistrict = false }" />

        <button type="button" class="pick-row" @click="showBusinessArea = true">
          <span class="pick-label">商圈</span>
          <span class="pick-val">{{ form.businessArea || '不限' }}</span>
          <van-icon name="arrow" size="14" color="#8E8E93" />
        </button>
        <van-action-sheet v-model:show="showBusinessArea" :actions="businessAreas.map(d => ({ name: d }))" @select="(a: any) => { form.businessArea = a.name; showBusinessArea = false }" />
        <button type="button" class="pick-row pick-muted" aria-label="清除商圈限制" @click="form.businessArea = ''"><span class="pick-label sr-only">商圈</span><span class="pick-val">不限商圈</span></button>

        <button type="button" class="pick-row" @click="showPrice = true">
          <span class="pick-label">预算</span>
          <span class="pick-val">¥{{ form.maxPrice }} 以内</span>
          <van-icon name="arrow" size="14" color="#8E8E93" />
        </button>
        <van-action-sheet v-model:show="showPrice" title="选择预算上限">
          <div class="sheet-grid">
            <button v-for="p in [15,20,25,30,35,40,50,60,80,999]" :key="p" type="button" :class="['grid-item', { active: form.maxPrice === p }]" @click="form.maxPrice = p; showPrice = false">
              {{ p === 999 ? '不限' : '¥' + p }}
            </button>
          </div>
        </van-action-sheet>

        <div class="pick-row pick-wrap">
          <span class="pick-label">菜系</span>
          <div class="chip-row">
            <button v-for="c in cuisines" :key="c" type="button" :aria-pressed="form.cuisines.includes(c)" :class="['chip', { active: form.cuisines.includes(c) }]" @click="toggleTag(form.cuisines, c)">{{ c }}</button>
          </div>
        </div>

        <div class="pick-row pick-wrap">
          <span class="pick-label">口味</span>
          <div class="chip-row">
            <button v-for="t in tasteTags" :key="t" type="button" :aria-pressed="form.tasteTags.includes(t)" :class="['chip', { active: form.tasteTags.includes(t) }]" @click="toggleTag(form.tasteTags, t)">{{ t }}</button>
          </div>
        </div>

        <div class="pick-row pick-wrap">
          <span class="pick-label">场景</span>
          <div class="chip-row">
            <button v-for="s in sceneTags" :key="s" type="button" :aria-pressed="form.sceneTags.includes(s)" :class="['chip', { active: form.sceneTags.includes(s) }]" @click="toggleTag(form.sceneTags, s)">{{ s }}</button>
          </div>
        </div>

        <div class="submit-row">
          <van-button block class="submit-btn" round @click="submitLocal">按本地算法推荐</van-button>
        </div>
      </section>
    </template>

    <BottomNav />
  </main>
</template>

<style scoped>
.seek-page { background: linear-gradient(180deg, #fff7f1 0%, #fffaf7 42%, #fff7f1 100%); }

.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

.seek-hero {
  position: relative;
  min-height: 150px;
  margin: 10px 0 14px;
  padding: 18px;
  border-radius: 22px;
  overflow: hidden;
  background: linear-gradient(135deg, #2b160c 0%, #7a3418 52%, #ff7a2e 100%);
  box-shadow: var(--shadow-float);
}

.hero-copy { position: relative; z-index: 2; max-width: 58%; color: #fff; }
.hero-kicker { display: inline-flex; padding: 4px 10px; border-radius: 999px; background: rgba(255,255,255,.18); font-size: 12px; }
.hero-copy h1 { margin: 10px 0 6px; font-size: 25px; line-height: 1.16; }
.hero-copy p { margin: 0; font-size: 13px; line-height: 1.55; color: rgba(255,255,255,.82); }
.hero-art { position: absolute; right: -54px; bottom: -34px; width: 68%; height: 150%; object-fit: cover; opacity: .92; transform: rotate(-2deg); }

.mode-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; margin-bottom: 14px; }
.mode-card {
  min-height: 84px;
  border: 1px solid var(--color-border);
  border-radius: 18px;
  background: #fff;
  box-shadow: var(--shadow-card);
  display: grid;
  place-items: center;
  gap: 3px;
  color: var(--color-text);
  font: inherit;
  cursor: pointer;
}
.mode-card.active { border-color: rgba(255,122,46,.5); background: linear-gradient(180deg, #fff, #fff0e5); }
.mode-card strong { font-size: 16px; }
.mode-card small { color: var(--color-muted); font-size: 12px; }
.mode-icon {
  width: 34px;
  height: 34px;
  border-radius: 14px;
  display: grid;
  place-items: center;
  font-weight: 800;
}
.mode-ai { color: #fff; background: linear-gradient(135deg, #ff7a2e, #ff4d2e); }
.mode-local { color: #2b160c; background: #ffe7d1; }

.ai-panel,
.form-panel {
  border-radius: var(--radius-card);
  background: var(--color-card);
  box-shadow: var(--shadow-card);
  overflow: hidden;
}
.ai-panel { padding: 16px; }
.ai-label { display: block; margin-bottom: 10px; font-size: 15px; font-weight: 700; }
.ai-input {
  width: 100%;
  min-height: 112px;
  resize: vertical;
  border: 1px solid var(--color-border);
  border-radius: 16px;
  padding: 14px;
  color: var(--color-text);
  background: #fffaf7;
  font: inherit;
  line-height: 1.6;
  outline: none;
}
.ai-input:focus { border-color: var(--color-primary); box-shadow: 0 0 0 3px rgba(255,122,46,.12); }
.example-row { display: grid; gap: 8px; margin: 10px 0 14px; }
.example-row button {
  min-height: 44px;
  border: 1px solid #f3dcc9;
  border-radius: 14px;
  background: #fff7f1;
  color: #7a3418;
  text-align: left;
  padding: 8px 12px;
  font: inherit;
}

.preset-section { display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 16px; }
.preset-chip {
  min-height: 44px; border: 0; padding: 8px 14px; border-radius: 22px; background: var(--color-primary-light);
  color: var(--color-primary); font-size: 13px; font-weight: 600; cursor: pointer;
  white-space: nowrap; font-family: inherit;
}

.pick-row {
  width: 100%; min-height: 52px; display: flex; align-items: center; gap: 12px; padding: 14px 16px;
  border: 0; border-bottom: 1px solid var(--color-border); cursor: pointer;
  background: transparent; text-align: left; font-family: inherit;
}
.pick-row:last-child { border-bottom: none; }
.pick-wrap { align-items: flex-start; }
.pick-muted .pick-val { color: var(--color-muted); font-size: 13px; }
.pick-label { font-size: 14px; font-weight: 600; min-width: 48px; color: var(--color-text); }
.pick-val { flex: 1; font-size: 14px; color: var(--color-text); }

.chip-row { display: flex; flex-wrap: wrap; gap: 8px; flex: 1; }
.chip {
  min-height: 44px; border: 0; padding: 7px 12px; border-radius: 18px; font-size: 13px;
  background: #F7F7F8; color: var(--color-muted); cursor: pointer;
  transition: all 120ms; font-family: inherit;
}
.chip.active { background: var(--color-primary); color: #fff; }

.submit-row { padding: 16px; }
.submit-btn {
  height: 46px; border: none; border-radius: 24px;
  background: var(--color-primary); color: #fff; font-size: 16px; font-weight: 700;
}

.sheet-grid { display: flex; flex-wrap: wrap; gap: 10px; padding: 16px; }
.grid-item {
  min-height: 44px; border: 0; padding: 10px 18px; border-radius: 14px; font-size: 14px; font-weight: 600;
  background: #F7F7F8; cursor: pointer;
  font-family: inherit;
}
.grid-item.active { background: var(--color-primary); color: #fff; }

button:active {
  transform: scale(0.97);
}

button:focus-visible,
.ai-input:focus-visible {
  outline: 3px solid rgba(255,122,46,.32);
  outline-offset: 3px;
}

@media (max-width: 374px) {
  .hero-copy { max-width: 64%; }
  .hero-copy h1 { font-size: 23px; }
  .hero-art { right: -70px; }
}
</style>
