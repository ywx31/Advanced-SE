import http from './http'
import type { ApiResponse, AuthResponse, UserProfile } from '../types'

export async function login(username: string, password: string) {
  const { data } = await http.post<ApiResponse<AuthResponse>>('/auth/login', {
    username,
    password,
  })
  return data.data
}

export async function register(
  username: string,
  password: string,
  displayName: string,
) {
  const { data } = await http.post<ApiResponse<AuthResponse>>('/auth/register', {
    username,
    password,
    displayName,
  })
  return data.data
}

export async function fetchMe() {
  const { data } = await http.get<ApiResponse<UserProfile>>('/auth/me')
  return data.data
}
