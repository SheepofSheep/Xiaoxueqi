export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

export interface PageResult<T> {
  list: T[]
  page: number
  pageSize: number
  total: number
}

export interface Restaurant {
  id: number
  name: string
  district: string
  businessArea?: string
  address?: string
  averagePrice: number
  cuisine: string
  rating: number
  tasteTags: string[]
  sceneTags: string[]
  avoidTags: string[]
  recommendedDishes: string[]
  description?: string
  latitude?: number
  longitude?: number
  coverImage?: string
  source?: string
  sourceNote?: string
  isDemoData: boolean
  status: number
  createdAt?: string
  updatedAt?: string
}

export interface RestaurantQuery {
  keyword?: string
  district?: string
  businessArea?: string
  cuisine?: string
  maxPrice?: number
  tasteTag?: string
  sceneTag?: string
  page?: number
  pageSize?: number
}

export interface CsvImportError {
  rowNumber: number
  message: string
}

export interface RestaurantImportResult {
  successCount: number
  createdCount: number
  updatedCount: number
  failureCount: number
  errors: CsvImportError[]
}

export interface RecommendationRequest {
  district?: string
  businessArea?: string
  maxPrice?: number
  cuisines?: string[]
  tasteTags?: string[]
  sceneTags?: string[]
  avoidTags?: string[]
  peopleCount?: number
  limit?: number
  excludeRestaurantIds?: number[]
}

export interface AiRecommendationRequest {
  prompt: string
  district?: string
  businessArea?: string
  maxPrice?: number
  limit?: number
}

export interface AiFoodIntent {
  district?: string
  businessArea?: string
  maxPrice?: number
  cuisines: string[]
  tasteTags: string[]
  sceneTags: string[]
  avoidTags: string[]
  summary: string
  aiUsed: boolean
  aiFallback: boolean
  aiModel?: string
  aiLatencyMs?: number
}

export interface RecommendationItem {
  restaurant: Restaurant
  matchScore: number
  matchedTags: string[]
  reason: string
  aiUsed: boolean
  aiFallback: boolean
  aiModel?: string
  aiLatencyMs?: number
}

export interface RecommendationResult {
  items: RecommendationItem[]
  relaxed: boolean
  relaxedRules: string[]
  localReasonUsed: boolean
  aiUsed: boolean
  aiFallback?: boolean
}

export interface AiRecommendationResult {
  recommendation: RecommendationResult
  interpretedRequest: RecommendationRequest
  intent: AiFoodIntent
}

export type ShakeClientTrigger = 'DEVICE_MOTION' | 'BUTTON_FALLBACK'

export interface ShakeRequest {
  district?: string
  businessArea?: string
  maxPrice?: number
  avoidTags?: string[]
  excludeRestaurantIds?: number[]
  clientTrigger: ShakeClientTrigger
}

export interface ShakeResult {
  restaurant: Restaurant
  matchScore: number
  reason: string
  clientTrigger: ShakeClientTrigger
}

export interface NearbyQuery {
  longitude: number
  latitude: number
  radius?: number
  keyword?: string
  page?: number
  pageSize?: number
}

export interface NearbyRestaurant {
  id: string
  name: string
  address: string
  distance: number
  type: string
  longitude?: number
  latitude?: number
  source: 'AMAP_REALTIME'
  canRecommend: false
  coverImage?: string
  rating?: number
}

export interface NearbyResult extends PageResult<NearbyRestaurant> {
  notice: string
}
