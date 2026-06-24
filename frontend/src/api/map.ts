import { apiClient, unwrap } from './client'
import type { NearbyQuery, NearbyResult } from '../types/api'

export function getNearbyRestaurants(params: NearbyQuery, signal?: AbortSignal) {
  return unwrap<NearbyResult>(apiClient.get('/map/nearby', { params, signal }))
}

export function getRegeo(lng: number, lat: number) {
  return unwrap<{ district: string; township: string; address: string }>(
    apiClient.get('/map/regeo', { params: { longitude: lng, latitude: lat } })
  )
}

