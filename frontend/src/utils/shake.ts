export interface ShakeDetectorOptions {
  threshold?: number
  cooldownMs?: number
  onShake: () => void
}

export type MotionSupportCode = 'SUPPORTED' | 'NO_WINDOW' | 'NO_DEVICE_MOTION'

export interface MotionRuntime {
  isSecureContext?: boolean
  location?: {
    hostname?: string
    protocol?: string
  }
  DeviceMotionEvent?: unknown
}

export interface MotionSupportResult {
  supported: boolean
  code: MotionSupportCode
  message: string
}

export function getDeviceMotionSupport(
  runtime: MotionRuntime | undefined = typeof window === 'undefined' ? undefined : window,
): MotionSupportResult {
  if (!runtime) {
    return {
      supported: false,
      code: 'NO_WINDOW',
      message: '当前运行环境不是浏览器，无法使用真实摇动。',
    }
  }

  const protocol = runtime.location?.protocol ?? ''
  const hostname = runtime.location?.hostname ?? ''
  const localNetworkHttp = protocol === 'http:' && hostname !== 'localhost' && hostname !== '127.0.0.1'
  const trustedOrigin = runtime.isSecureContext === true
    || protocol === 'https:'
    || hostname === 'localhost'
    || hostname === '127.0.0.1'
    || hostname === '[::1]'
    || hostname === '::1'

  if (!runtime.DeviceMotionEvent) {
    return {
      supported: false,
      code: 'NO_DEVICE_MOTION',
      message: localNetworkHttp
        ? '当前网络环境可继续访问，但浏览器没有开放运动传感器。请换 Android Chrome/Edge，或在浏览器设置中允许传感器。'
        : '当前浏览器没有开放运动传感器，请换 Android Chrome/Edge 或 iOS Safari 使用。',
    }
  }

  return {
    supported: true,
    code: 'SUPPORTED',
    message: trustedOrigin ? '当前环境支持真实摇动。' : '当前环境支持真实摇动。',
  }
}

export function supportsDeviceMotion() {
  return getDeviceMotionSupport().supported
}

export function createShakeDetector(options: ShakeDetectorOptions) {
  const threshold = options.threshold ?? 18
  const cooldownMs = options.cooldownMs ?? 2000
  let lastValue = 0
  let lastTriggerAt = 0

  function handleMotion(event: DeviceMotionEvent) {
    const acceleration = event.accelerationIncludingGravity
    if (!acceleration) return

    const x = acceleration.x ?? 0
    const y = acceleration.y ?? 0
    const z = acceleration.z ?? 0
    const value = x + y + z
    const delta = Math.abs(value - lastValue)
    const now = Date.now()

    lastValue = value

    if (delta > threshold && now - lastTriggerAt > cooldownMs) {
      lastTriggerAt = now
      options.onShake()
    }
  }

  return {
    start() {
      window.addEventListener('devicemotion', handleMotion)
    },
    stop() {
      window.removeEventListener('devicemotion', handleMotion)
    },
  }
}
