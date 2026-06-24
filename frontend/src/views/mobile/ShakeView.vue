<script setup lang="ts">
import { onBeforeUnmount, ref } from 'vue'
import AppInlineMessage from '../../components/AppInlineMessage.vue'
import AppTopBar from '../../components/AppTopBar.vue'
import BottomNav from '../../components/BottomNav.vue'
import RestaurantCard from '../../components/RestaurantCard.vue'
import { getErrorMessage } from '../../api/client'
import { shakeRestaurant } from '../../api/recommendations'
import type { Restaurant, ShakeClientTrigger } from '../../types/api'
import { createShakeDetector, getDeviceMotionSupport } from '../../utils/shake'

const status = ref('摇动手机，发现美味')
const error = ref('')
const loading = ref(false)
const result = ref<{ restaurant: Restaurant; matchScore: number; reason: string } | null>(null)
const detector = ref<ReturnType<typeof createShakeDetector> | null>(null)
const motionSupport = ref(getDeviceMotionSupport())
const shakeCount = ref(0)

async function triggerShake(clientTrigger: ShakeClientTrigger) {
  if (loading.value) return
  loading.value = true
  error.value = ''
  status.value = clientTrigger === 'DEVICE_MOTION' ? '正在捕捉你的选择' : '正在重新挑选'

  try {
    const data = await shakeRestaurant({
      district: '尖草坪区',
      businessArea: '中北大学周边',
      maxPrice: 35,
      excludeRestaurantIds: result.value ? [result.value.restaurant.id] : [],
      clientTrigger,
    })
    result.value = data
    shakeCount.value++
    status.value = clientTrigger === 'DEVICE_MOTION' ? '摇到了' : '已换一家'
  } catch (err) {
    error.value = getErrorMessage(err, '摇一摇失败，试试换一个')
    status.value = '暂时没摇出来'
  } finally {
    loading.value = false
  }
}

async function enableMotion() {
  error.value = ''
  motionSupport.value = getDeviceMotionSupport()

  if (!motionSupport.value.supported) {
    status.value = '当前设备不支持真实摇动'
    return
  }

  const motionEvent = window.DeviceMotionEvent as typeof DeviceMotionEvent & {
    requestPermission?: () => Promise<PermissionState>
  }

  try {
    if (typeof motionEvent.requestPermission === 'function') {
      const permission = await motionEvent.requestPermission()
      if (permission !== 'granted') {
        status.value = '传感器未授权'
        return
      }
    }
  } catch {
    status.value = '权限请求失败'
    return
  }

  detector.value?.stop()
  detector.value = createShakeDetector({
    onShake: () => triggerShake('DEVICE_MOTION'),
  })
  detector.value.start()
  status.value = '正在监听，摇晃手机试试'
}

onBeforeUnmount(() => {
  detector.value?.stop()
})
</script>

<template>
  <main class="mobile-page shake-page">
    <AppTopBar title="摇一摇" subtitle="真机摇动优先" />

    <section class="shake-hero">
      <img src="/assets/taiyuan-food-hero.png" alt="" class="shake-hero-img" />
      <div class="shake-copy">
        <span>随机推荐</span>
        <h1>把选择权交给这一摇。</h1>
        <p>摇动手机直接开选，也可以轻点按钮换一家。</p>
      </div>
    </section>

    <section class="motion-panel" :class="{ active: detector, loading }">
      <div class="motion-orbit" aria-hidden="true">
        <div class="motion-disc">
          <van-icon name="exchange" size="36" />
        </div>
      </div>

      <div class="motion-status">
        <strong>{{ status }}</strong>
        <span v-if="motionSupport.supported">监听开启后，真机摇动会直接触发推荐</span>
        <span v-else>当前浏览器未开放运动传感器</span>
      </div>

      <div class="shake-actions">
        <van-button
          v-if="!detector"
          block
          class="shake-btn"
          round
          @click="enableMotion"
        >
          开启摇一摇
        </van-button>
        <van-button
          block
          class="shake-btn-secondary"
          plain
          :loading="loading"
          @click="triggerShake('BUTTON_FALLBACK')"
        >
          换一家
        </van-button>
      </div>

      <AppInlineMessage
        v-if="error"
        class="motion-message"
        variant="error"
        :message="error"
        action-text="再试一次"
        :action-disabled="loading"
        @action="triggerShake('BUTTON_FALLBACK')"
      />
    </section>

    <section v-if="result" class="result-section">
      <div class="result-header">
        <span>第 {{ shakeCount }} 次结果</span>
        <button type="button" @click="triggerShake('BUTTON_FALLBACK')">再换</button>
      </div>
      <RestaurantCard
        :restaurant="result.restaurant"
        :match-score="result.matchScore"
        :reason="result.reason"
      />
    </section>

    <BottomNav />
  </main>
</template>

<style scoped>
.shake-page {
  background: linear-gradient(180deg, #fff8f2 0%, #fff2e8 54%, #fff8f2 100%);
}

.shake-hero {
  position: relative;
  min-height: 236px;
  margin-top: 10px;
  border-radius: 30px;
  overflow: hidden;
  background: #2b160c;
  box-shadow: 0 22px 56px rgba(75, 35, 14, 0.18);
}

.shake-hero::after {
  content: "";
  position: absolute;
  inset: 0;
  background:
    linear-gradient(90deg, rgba(43, 22, 12, 0.95), rgba(43, 22, 12, 0.44) 62%, rgba(43, 22, 12, 0.12)),
    linear-gradient(180deg, transparent, rgba(43, 22, 12, 0.36));
}

.shake-hero-img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  opacity: 0.86;
}

.shake-copy {
  position: relative;
  z-index: 1;
  width: 68%;
  padding: 26px 22px;
  color: #fff;
}

.shake-copy span {
  display: inline-flex;
  padding: 5px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.14);
  font-size: 12px;
  font-weight: 800;
}

.shake-copy h1 {
  margin: 16px 0 10px;
  font-size: 29px;
  line-height: 1.12;
}

.shake-copy p {
  margin: 0;
  color: rgba(255, 255, 255, 0.82);
  font-size: 14px;
  line-height: 1.6;
}

.motion-panel {
  margin-top: 18px;
  padding: 22px 18px 18px;
  border: 1px solid rgba(93, 45, 22, 0.08);
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 18px 48px rgba(75, 35, 14, 0.1);
}

.motion-orbit {
  width: 164px;
  height: 164px;
  margin: 4px auto 18px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  background:
    radial-gradient(circle, rgba(255, 122, 46, 0.18) 0 34%, transparent 35%),
    conic-gradient(from 140deg, rgba(255, 122, 46, 0.08), rgba(255, 122, 46, 0.32), rgba(255, 122, 46, 0.08));
}

.motion-disc {
  width: 104px;
  height: 104px;
  display: grid;
  place-items: center;
  border-radius: 34px;
  background: linear-gradient(135deg, #ff7a2e, #ff4d2e);
  color: #fff;
  box-shadow: 0 18px 38px rgba(255, 103, 46, 0.32);
}

.motion-panel.loading .motion-disc {
  animation: shake-pulse 420ms ease-in-out;
}

.motion-panel.active .motion-orbit {
  box-shadow: inset 0 0 0 1px rgba(255, 122, 46, 0.18);
}

@keyframes shake-pulse {
  0%, 100% { transform: rotate(0) scale(1); }
  25% { transform: rotate(-8deg) scale(1.03); }
  75% { transform: rotate(8deg) scale(1.03); }
}

.motion-status {
  text-align: center;
}

.motion-status strong {
  display: block;
  color: var(--color-text);
  font-size: 20px;
}

.motion-status span {
  display: block;
  margin-top: 6px;
  color: var(--color-muted);
  font-size: 13px;
  line-height: 1.55;
}

.shake-actions {
  display: grid;
  gap: 10px;
  margin-top: 20px;
}

.shake-btn,
.shake-btn-secondary {
  height: 48px;
  border-radius: 24px;
  font-size: 16px;
  font-weight: 800;
}

.shake-btn {
  border: none;
  background: var(--color-primary);
  color: #fff;
}

.shake-btn-secondary {
  border-color: rgba(255, 122, 46, 0.38);
  color: var(--color-primary);
  background: #fffaf7;
}

.motion-message {
  margin-top: 14px;
}

.result-section {
  margin-top: 20px;
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.result-header span {
  color: var(--color-muted);
  font-size: 13px;
  font-weight: 700;
}

.result-header button {
  min-height: 44px;
  border: 0;
  border-radius: 999px;
  padding: 0 13px;
  color: var(--color-primary);
  background: var(--color-primary-light);
  font: inherit;
  font-size: 13px;
  font-weight: 800;
}

button:active {
  transform: scale(0.97);
}

@media (prefers-reduced-motion: reduce) {
  .motion-panel.loading .motion-disc {
    animation: none;
  }
}

</style>
