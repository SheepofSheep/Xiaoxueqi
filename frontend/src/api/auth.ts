import { apiClient, unwrap } from './client'

interface LoginResult {
  token: string
  role: string
  nickname: string
}

export async function loginGuest(): Promise<LoginResult> {
  return unwrap(apiClient.post('/auth/guest'))
}

export async function loginAdmin(username: string, password: string): Promise<LoginResult> {
  return unwrap(apiClient.post('/auth/login', { username, password }))
}
