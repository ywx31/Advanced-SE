import http from './http'
import type { AccountProfile, ApiResponse } from '../types'

export type AvatarStorageMode = 'LOCAL' | 'OSS'

export interface AvatarUploadConfig {
  mode: AvatarStorageMode
  maxSize: number
}

export interface AvatarUploadTicket {
  mode: 'OSS'
  uploadUrl: string
  method: 'PUT'
  headers: Record<string, string>
  objectKey: string
  publicUrl: string
  expiresAtEpochSeconds: number
}

export async function fetchSettings() {
  const { data } = await http.get<ApiResponse<AccountProfile>>('/settings')
  return data.data
}

export async function updateProfile(displayName: string) {
  const { data } = await http.put<ApiResponse<AccountProfile>>('/settings/profile', {
    displayName,
  })
  return data.data
}

export async function changePassword(
  currentPassword: string,
  newPassword: string,
  confirmPassword: string,
) {
  await http.put<ApiResponse<null>>('/settings/password', {
    currentPassword,
    newPassword,
    confirmPassword,
  })
}

export async function fetchAvatarUploadConfig() {
  const { data } = await http.get<ApiResponse<AvatarUploadConfig>>(
    '/settings/avatar/config',
  )
  return data.data
}

export async function createAvatarPresign(
  fileName: string,
  contentType: string,
  size: number,
) {
  const { data } = await http.post<ApiResponse<AvatarUploadTicket>>(
    '/settings/avatar/presign',
    { fileName, contentType, size },
  )
  return data.data
}

export async function confirmOssAvatar(objectKey: string) {
  const { data } = await http.post<ApiResponse<AccountProfile>>(
    '/settings/avatar/confirm',
    { objectKey },
  )
  return data.data
}

export async function uploadLocalAvatar(file: File) {
  const form = new FormData()
  form.append('file', file)
  const { data } = await http.post<ApiResponse<AccountProfile>>(
    '/settings/avatar/local',
    form,
  )
  return data.data
}
