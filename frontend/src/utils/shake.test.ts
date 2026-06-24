import { describe, expect, it } from 'vitest'
import { getDeviceMotionSupport } from './shake'

describe('getDeviceMotionSupport', () => {
  it('allows local network http origin when the motion api exists', () => {
    const support = getDeviceMotionSupport({
      isSecureContext: false,
      location: {
        hostname: '192.168.1.3',
        protocol: 'http:',
      },
      DeviceMotionEvent: class DeviceMotionEvent {},
    })

    expect(support.supported).toBe(true)
    expect(support.code).toBe('SUPPORTED')
    expect(support.message).toContain('真实摇动')
  })

  it('supports device motion in secure contexts when the api exists', () => {
    const support = getDeviceMotionSupport({
      isSecureContext: true,
      location: {
        hostname: 'example.com',
        protocol: 'https:',
      },
      DeviceMotionEvent: class DeviceMotionEvent {},
    })

    expect(support.supported).toBe(true)
    expect(support.code).toBe('SUPPORTED')
  })

  it('reports missing api in secure contexts', () => {
    const support = getDeviceMotionSupport({
      isSecureContext: true,
      location: {
        hostname: 'example.com',
        protocol: 'https:',
      },
    })

    expect(support.supported).toBe(false)
    expect(support.code).toBe('NO_DEVICE_MOTION')
  })

  it('reports missing api on local network http origins when the browser hides it', () => {
    const support = getDeviceMotionSupport({
      isSecureContext: false,
      location: {
        hostname: '192.168.1.3',
        protocol: 'http:',
      },
    })

    expect(support.supported).toBe(false)
    expect(support.code).toBe('NO_DEVICE_MOTION')
    expect(support.message).toContain('当前网络环境')
  })
})
