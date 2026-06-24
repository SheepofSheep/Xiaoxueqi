import { registerSW } from 'virtual:pwa-register'

type ToastAction = {
  label: string
  handler: () => void
}

let toastTimer: number | undefined

function showPwaToast(message: string, action?: ToastAction) {
  const old = document.querySelector('.pwa-toast')
  old?.remove()

  const toast = document.createElement('div')
  toast.className = 'pwa-toast'
  toast.setAttribute('role', 'status')
  toast.setAttribute('aria-live', 'polite')

  const text = document.createElement('span')
  text.textContent = message
  toast.appendChild(text)

  if (action) {
    const button = document.createElement('button')
    button.type = 'button'
    button.textContent = action.label
    button.addEventListener('click', () => {
      action.handler()
      toast.remove()
    })
    toast.appendChild(button)
  }

  document.body.appendChild(toast)

  window.clearTimeout(toastTimer)
  toastTimer = window.setTimeout(() => {
    toast.remove()
  }, action ? 8000 : 3600)
}

const updateServiceWorker = registerSW({
  immediate: true,
  onNeedRefresh() {
    showPwaToast('太原食探有新版本', {
      label: '刷新',
      handler: () => updateServiceWorker(true),
    })
  },
  onOfflineReady() {
    showPwaToast('已可离线打开应用，最新餐厅和地图需联网')
  },
  onRegisterError(error) {
    console.warn('PWA 注册失败', error)
  },
})

window.addEventListener('offline', () => {
  showPwaToast('当前离线：已保留页面壳，推荐和地图需恢复网络')
})

window.addEventListener('online', () => {
  showPwaToast('网络已恢复，可以继续查看最新推荐')
})
