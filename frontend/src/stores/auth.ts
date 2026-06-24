import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

interface LoginResult {
  token: string
  role: string
  nickname: string
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('auth_token') || '')
  const role = ref(localStorage.getItem('auth_role') || '')
  const nickname = ref(localStorage.getItem('auth_nickname') || '')

  const isAdmin = computed(() => role.value === 'ADMIN')
  const isLoggedIn = computed(() => !!token.value)

  function setAuth(data: LoginResult) {
    token.value = data.token
    role.value = data.role
    nickname.value = data.nickname
    localStorage.setItem('auth_token', data.token)
    localStorage.setItem('auth_role', data.role)
    localStorage.setItem('auth_nickname', data.nickname)
  }

  function clearAuth() {
    token.value = ''
    role.value = ''
    nickname.value = ''
    localStorage.removeItem('auth_token')
    localStorage.removeItem('auth_role')
    localStorage.removeItem('auth_nickname')
  }

  return { token, role, nickname, isAdmin, isLoggedIn, setAuth, clearAuth }
})
