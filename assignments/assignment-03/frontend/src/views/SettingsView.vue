<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import {
  Camera,
  Check,
  KeyRound,
  LockKeyhole,
  Save,
  ShieldCheck,
  UserRound,
} from 'lucide-vue-next'
import PageHeader from '../components/PageHeader.vue'
import { useAuthStore } from '../stores/auth'
import {
  changePassword,
  confirmOssAvatar,
  createAvatarPresign,
  fetchAvatarUploadConfig,
  fetchSettings,
  updateProfile,
  uploadLocalAvatar,
} from '../api/settings'
import type { AccountProfile } from '../types'

const route = useRoute()
const auth = useAuthStore()
const loading = ref(true)
const error = ref('')
const profile = ref<AccountProfile | null>(null)
const displayNameInput = ref<HTMLInputElement | null>(null)

const displayName = ref('')
const profileSaving = ref(false)
const profileMessage = ref('')
const profileError = ref('')
const avatarUploading = ref(false)
const avatarMessage = ref('')
const avatarError = ref('')

const currentPassword = ref('')
const newPassword = ref('')
const confirmPassword = ref('')
const passwordSaving = ref(false)
const passwordMessage = ref('')
const passwordError = ref('')

const initials = computed(() => profile.value?.displayName.slice(0, 1) || 'M')
const joinedAt = computed(() => {
  if (!profile.value?.createdAt) return '—'
  return new Date(profile.value.createdAt).toLocaleDateString('zh-CN', {
    year: 'numeric', month: 'long', day: 'numeric',
  })
})

onMounted(load)

async function load() {
  loading.value = true
  error.value = ''
  try {
    applyProfile(await fetchSettings())
    await nextTick()
    if (route.query.focus === 'profile') displayNameInput.value?.focus()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '加载个人资料失败'
  } finally {
    loading.value = false
  }
}

function applyProfile(next: AccountProfile) {
  profile.value = next
  displayName.value = next.displayName
  auth.updateUser({
    id: next.id,
    username: next.username,
    displayName: next.displayName,
    avatarUrl: next.avatarUrl,
  })
}

async function handleAvatarSelected(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return

  avatarMessage.value = ''
  avatarError.value = ''
  avatarUploading.value = true
  try {
    const config = await fetchAvatarUploadConfig()
    if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) {
      throw new Error('头像仅支持 JPG、PNG 或 WebP 格式')
    }
    if (file.size > config.maxSize) {
      throw new Error(`头像大小不能超过 ${Math.round(config.maxSize / 1024 / 1024)} MB`)
    }

    let updated: AccountProfile
    if (config.mode === 'OSS') {
      const ticket = await createAvatarPresign(file.name, file.type, file.size)
      const response = await fetch(ticket.uploadUrl, {
        method: ticket.method,
        headers: ticket.headers,
        body: file,
      })
      if (!response.ok) {
        throw new Error('头像上传到云端失败，请重试')
      }
      updated = await confirmOssAvatar(ticket.objectKey)
    } else {
      updated = await uploadLocalAvatar(file)
    }
    applyProfile(updated)
    avatarMessage.value = '头像已更新'
  } catch (err) {
    avatarError.value = err instanceof Error ? err.message : '头像上传失败'
  } finally {
    avatarUploading.value = false
  }
}

async function saveProfile() {
  profileMessage.value = ''
  profileError.value = ''
  if (!displayName.value.trim()) {
    profileError.value = '昵称不能为空'
    return
  }
  profileSaving.value = true
  try {
    applyProfile(await updateProfile(displayName.value.trim()))
    profileMessage.value = '个人资料已更新'
  } catch (err) {
    profileError.value = err instanceof Error ? err.message : '保存失败'
  } finally {
    profileSaving.value = false
  }
}

async function savePassword() {
  passwordMessage.value = ''
  passwordError.value = ''
  if (!currentPassword.value || !newPassword.value || !confirmPassword.value) {
    passwordError.value = '请完整填写当前密码、新密码和确认密码'
    return
  }
  if (newPassword.value !== confirmPassword.value) {
    passwordError.value = '两次输入的新密码不一致'
    return
  }
  passwordSaving.value = true
  try {
    await changePassword(currentPassword.value, newPassword.value, confirmPassword.value)
    currentPassword.value = ''
    newPassword.value = ''
    confirmPassword.value = ''
    passwordMessage.value = '密码已修改，下次登录请使用新密码'
  } catch (err) {
    passwordError.value = err instanceof Error ? err.message : '修改密码失败'
  } finally {
    passwordSaving.value = false
  }
}
</script>

<template>
  <div>
    <PageHeader
      eyebrow="ACCOUNT SETTINGS"
      title="设置"
      description="修改个人资料和登录密码。修改昵称无需验证，修改密码需要验证当前密码。"
    />

    <div v-if="error" class="alert">{{ error }}</div>
    <div v-if="loading" class="loading-panel"><div class="loader" /></div>

    <template v-else-if="profile">
      <section class="profile-summary">
        <label class="profile-avatar" title="点击上传头像">
          <img v-if="profile.avatarUrl" :src="profile.avatarUrl" alt="用户头像" />
          <span v-else>{{ initials }}</span>
          <span class="avatar-upload-icon"><Camera /></span>
          <input type="file" accept="image/jpeg,image/png,image/webp" :disabled="avatarUploading" @change="handleAvatarSelected" />
        </label>
        <div class="profile-copy">
          <span class="profile-kicker">YOUR ACCOUNT</span>
          <h2>{{ profile.displayName }}</h2>
          <p>@{{ profile.username }} · 加入于 {{ joinedAt }}</p>
          <small class="avatar-tip">{{ avatarUploading ? '头像上传中…' : '点击头像可上传本地图片' }}</small>
          <p v-if="avatarError" class="avatar-message avatar-error">{{ avatarError }}</p>
          <p v-if="avatarMessage" class="avatar-message avatar-success"><Check /> {{ avatarMessage }}</p>
        </div>
        <div class="security-note">
          <ShieldCheck />
          <span><strong>账号安全</strong><small>只有修改密码时才需要验证当前密码</small></span>
        </div>
      </section>

      <div class="settings-columns">
        <section class="card settings-card">
          <div class="setting-card-head">
            <span class="setting-icon blue"><UserRound /></span>
            <div><h2>个人资料</h2><p>用户名固定不变，昵称可以直接修改</p></div>
          </div>
          <form class="setting-form" @submit.prevent="saveProfile">
            <div class="field">
              <label for="displayName">昵称</label>
              <input
                id="displayName"
                ref="displayNameInput"
                v-model.trim="displayName"
                class="input"
                maxlength="30"
                placeholder="请输入昵称"
                autocomplete="nickname"
              />
            </div>
            <div class="field">
              <label for="username">用户名</label>
              <input id="username" class="input readonly-input" :value="profile.username" disabled />
              <span class="field-hint">用户名用于登录，当前版本暂不支持修改</span>
            </div>
            <div class="profile-tip">
              <Check />
              <span>修改昵称不会影响登录账号和学习记录</span>
            </div>
            <p v-if="profileError" class="form-message error-message">{{ profileError }}</p>
            <p v-if="profileMessage" class="form-message success-message"><Check /> {{ profileMessage }}</p>
            <button class="btn btn-primary setting-save" :disabled="profileSaving">
              <Save /> {{ profileSaving ? '保存中…' : '保存个人资料' }}
            </button>
          </form>
        </section>

        <section class="card settings-card">
          <div class="setting-card-head">
            <span class="setting-icon amber"><KeyRound /></span>
            <div><h2>账号安全</h2><p>修改登录密码，必须验证当前密码</p></div>
          </div>
          <form class="setting-form" @submit.prevent="savePassword">
            <div class="field">
              <label for="currentPassword">当前密码</label>
              <div class="input-with-icon">
                <LockKeyhole />
                <input id="currentPassword" v-model="currentPassword" class="input" type="password" autocomplete="current-password" placeholder="请输入当前密码" />
              </div>
            </div>
            <div class="field">
              <label for="newPassword">新密码</label>
              <input id="newPassword" v-model="newPassword" class="input" type="password" autocomplete="new-password" placeholder="6-50 位新密码" />
            </div>
            <div class="field">
              <label for="confirmPassword">确认新密码</label>
              <input id="confirmPassword" v-model="confirmPassword" class="input" type="password" autocomplete="new-password" placeholder="再次输入新密码" />
            </div>
            <p v-if="passwordError" class="form-message error-message">{{ passwordError }}</p>
            <p v-if="passwordMessage" class="form-message success-message"><Check /> {{ passwordMessage }}</p>
            <button class="btn btn-dark setting-save" :disabled="passwordSaving">
              <KeyRound /> {{ passwordSaving ? '修改中…' : '修改密码' }}
            </button>
          </form>
        </section>
      </div>
    </template>
  </div>
</template>

<style scoped>
.profile-summary { display: flex; align-items: center; gap: 17px; margin-bottom: 20px; padding: 22px 25px; border: 1px solid rgba(148,163,184,.2); border-radius: 20px; background: radial-gradient(circle at 88% 15%, #dbeafe, transparent 26%), #fff; box-shadow: 0 12px 30px rgba(15,23,42,.055); }
.profile-avatar { position: relative; overflow: hidden; display: grid; flex: 0 0 62px; width: 62px; height: 62px; place-items: center; border-radius: 18px; background: linear-gradient(145deg, #dbeafe, #99f6e4); color: #1e3a8a; font-size: 24px; font-weight: 800; cursor: pointer; }
.profile-avatar > img { width: 100%; height: 100%; object-fit: cover; }
.profile-avatar > input { display: none; }
.avatar-upload-icon { position: absolute; right: 3px; bottom: 3px; display: grid; width: 20px; height: 20px; place-items: center; border: 2px solid #fff; border-radius: 50%; background: #2563eb; color: #fff; }
.avatar-upload-icon svg { width: 10px; height: 10px; }
.profile-copy { flex: 1; }
.profile-kicker { color: #2563eb; font-size: 9px; font-weight: 800; letter-spacing: .16em; }
.profile-copy h2 { margin: 5px 0 3px; color: #172554; font-family: var(--serif); font-size: 25px; }
.profile-copy p { margin: 0; color: #94a3b8; font-size: 11px; }
.avatar-tip { display: block; margin-top: 6px; color: #2563eb; font-size: 9px; }
.avatar-message { display: flex; align-items: center; gap: 4px; margin-top: 6px !important; font-size: 9px !important; }
.avatar-message svg { width: 12px; height: 12px; }
.avatar-error { color: #be123c !important; }
.avatar-success { color: #047857 !important; }
.security-note { display: flex; align-items: center; gap: 10px; padding: 11px 14px; border-radius: 12px; background: #ecfdf5; color: #047857; }
.security-note svg { width: 20px; height: 20px; }
.security-note strong { display: block; font-size: 11px; }
.security-note small { display: block; margin-top: 3px; color: #5d8d80; font-size: 9px; }
.settings-columns { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 18px; }
.settings-card { padding: 23px; }
.setting-card-head { display: flex; align-items: center; gap: 12px; margin-bottom: 20px; }
.setting-icon { display: grid; flex: 0 0 41px; width: 41px; height: 41px; place-items: center; border-radius: 12px; }
.setting-icon svg { width: 19px; height: 19px; }
.setting-icon.blue { background: #eff6ff; color: #2563eb; }
.setting-icon.amber { background: #fffbeb; color: #d97706; }
.setting-card-head h2 { margin: 0; color: #1e293b; font-size: 16px; }
.setting-card-head p { margin: 4px 0 0; color: #94a3b8; font-size: 10px; }
.setting-form { display: grid; gap: 15px; }
.readonly-input { background: #f8fafc; color: #94a3b8; }
.profile-tip { display: flex; align-items: center; gap: 7px; padding: 10px 12px; border-radius: 10px; background: #f8fafc; color: #64748b; font-size: 10px; line-height: 1.5; }
.profile-tip svg { width: 14px; height: 14px; color: #059669; }
.input-with-icon { position: relative; }
.input-with-icon svg { position: absolute; top: 50%; left: 13px; z-index: 1; width: 15px; height: 15px; color: #94a3b8; transform: translateY(-50%); }
.input-with-icon .input { padding-left: 38px; }
.setting-save { width: 100%; margin-top: 3px; }
.form-message { display: flex; align-items: center; gap: 5px; margin: -3px 0 0; padding: 9px 11px; border-radius: 9px; font-size: 10px; line-height: 1.5; }
.form-message svg { width: 13px; height: 13px; }
.error-message { background: #fff1f2; color: #be123c; }
.success-message { background: #ecfdf5; color: #047857; }
@media (max-width: 900px) {
  .profile-summary { align-items: flex-start; flex-wrap: wrap; }
  .security-note { width: 100%; }
  .settings-columns { grid-template-columns: 1fr; }
}
</style>
