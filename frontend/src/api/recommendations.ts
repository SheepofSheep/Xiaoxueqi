import { apiClient, unwrap } from './client'
import type {
  RecommendationRequest,
  RecommendationResult,
  AiRecommendationRequest,
  AiRecommendationResult,
  ShakeRequest,
  ShakeResult,
} from '../types/api'

export function recommendRestaurants(payload: RecommendationRequest) {
  return unwrap<RecommendationResult>(apiClient.post('/recommendations', payload))
}

export function aiRecommendRestaurants(payload: AiRecommendationRequest) {
  return unwrap<AiRecommendationResult>(apiClient.post('/recommendations/ai', payload))
}

export function shakeRestaurant(payload: ShakeRequest) {
  return unwrap<ShakeResult>(apiClient.post('/recommendations/shake', payload))
}
