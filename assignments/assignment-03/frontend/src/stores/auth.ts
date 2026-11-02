import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import * as authApi from '../api/auth'
import type { UserProfile } from '../types'

const TOKEN_KEY = 'mathmate_token'
const USER_KEY = 'mathmate_user'

function readUser(): UserProfile | null {
  try {
    const raw = localStorage.getItem(USER_KEY)
    return raw ? (JSON.parse(raw) as UserProfile) : null
  } catch {
    return null
  }
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem(TOKEN_KEY) || '')
  const user = ref<UserProfile | null>(readUser())
  const busy = ref(false)
  const isLoggedIn = computed(() => Boolean(token.value))

  function saveSession(nextToken: string, nextUser: UserProfile) {
    token.value = nextToken
    user.value = nextUser
    localStorage.setItem(TOKEN_KEY, nextToken)
    localStorage.setItem(USER_KEY, JSON.stringify(nextUser))
  }

  async function login(username: string, password: string) {
    busy.value = true
    try {
      const result = await authApi.login(username, password)
      saveSession(result.token, result.user)
      return result.user
    } finally {
      busy.value = false
    }
  }

  async function register(username: string, password: string, displayName: string) {
    busy.value = true
    try {
      const result = await authApi.register(username, password, displayName)
      saveSession(result.token, result.user)
      return result.user
    } finally {
      busy.value = false
    }
  }

  async function refreshUser() {
    if (!token.value) return null
    const profile = await authApi.fetchMe()
    user.value = profile
    localStorage.setItem(USER_KEY, JSON.stringify(profile))
    return profile
  }

  function updateUser(nextUser: UserProfile) {
    user.value = nextUser
    localStorage.setItem(USER_KEY, JSON.stringify(nextUser))
  }

  function logout() {
    token.value = ''
    user.value = null
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
  }

  return { token, user, busy, isLoggedIn, login, register, refreshUser, updateUser, logout }
})
