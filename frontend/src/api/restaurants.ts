import { apiClient, unwrap } from './client'
import type { PageResult, Restaurant, RestaurantImportResult, RestaurantQuery } from '../types/api'

export type RestaurantPayload = {
  name: string
  district: string
  businessArea?: string
  address?: string
  averagePrice: number
  cuisine: string
  rating?: number
  tasteTags?: string[]
  sceneTags?: string[]
  avoidTags?: string[]
  recommendedDishes?: string[]
  description?: string
  latitude?: number
  longitude?: number
  coverImage?: string
  sourceNote?: string
  isDemoData?: number
}

export function getRestaurants(params: RestaurantQuery) {
  return unwrap<PageResult<Restaurant>>(apiClient.get('/restaurants', { params }))
}

export function getRestaurantDetail(id: number | string) {
  return unwrap<Restaurant>(apiClient.get(`/restaurants/${id}`))
}

export function importRestaurantsCsv(file: File) {
  const formData = new FormData()
  formData.append('file', file)

  return unwrap<RestaurantImportResult>(
    apiClient.post('/admin/restaurants/import', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }),
  )
}

export function createRestaurant(payload: RestaurantPayload) {
  return unwrap<Restaurant>(apiClient.post('/admin/restaurants', payload))
}

export function updateRestaurant(id: number, payload: RestaurantPayload) {
  return unwrap<Restaurant>(apiClient.put(`/admin/restaurants/${id}`, payload))
}

export function disableRestaurant(id: number) {
  return unwrap<void>(apiClient.delete(`/admin/restaurants/${id}`))
}
