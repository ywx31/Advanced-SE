<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import {
  BarChart3,
  BookOpenCheck,
  CalendarDays,
  ChevronRight,
  ClipboardPenLine,
  History,
  LogOut,
  Menu,
  NotebookTabs,
  Settings2,
  UserRound,
  X,
} from 'lucide-vue-next'
import { useAuthStore } from '../stores/auth'
import { fetchSettings } from '../api/settings'
import type { AccountProfile } from '../types'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const menuOpen = ref(false)
const profileOpen = ref(false)
const profileLoading = ref(false)
const profileDetail = ref<AccountProfile | null>(null)

const navItems = [
  { to: '/dashboard', label: '学习概览', icon: BarChart3 },
  { to: '/practice/new', label: '开始练习', icon: ClipboardPenLine },
  { to: '/wrong-book', label: '错题本', icon: NotebookTabs },
  { to: '/weekly-report', label: '学习周报', icon: CalendarDays },
  { to: '/history', label: '练习记录', icon: History },
]

const pageTitle = computed(() => String(route.meta.title || 'MathMate'))
const initials = computed(() => auth.user?.displayName?.slice(0, 1) || 'M')

onMounted(() => document.addEventListener('click', closeProfile))
onBeforeUnmount(() => document.removeEventListener('click', closeProfile))

function closeProfile() {
  profileOpen.value = false
}

async function toggleProfile() {
  if (profileOpen.value) {
    profileOpen.value = false
    return
  }
  profileOpen.value = true
  profileLoading.value = true
  try {
    profileDetail.value = await fetchSettings()
  } catch {
    profileDetail.value = null
  } finally {
    profileLoading.value = false
  }
}

function openSettings() {
  profileOpen.value = false
  menuOpen.value = false
  router.push({ name: 'settings', query: { focus: 'profile' } })
}

function joinedDate() {
  if (!profileDetail.value?.createdAt) return ''
  return new Date(profileDetail.value.createdAt).toLocaleDateString('zh-CN', {
    year: 'numeric', month: 'long', day: 'numeric',
  })
}

function logout() {
  profileOpen.value = false
  auth.logout()
  router.push('/login')
}
</script>

<template>
  <div class="app-shell">
    <aside class="sidebar" :class="{ open: menuOpen }">
      <div class="brand">
        <div class="brand-mark">
          <BookOpenCheck />
        </div>
        <div>
          <strong>MathMate</strong>
          <span>智能口算练习</span>
        </div>
        <button class="icon-button mobile-close" @click="menuOpen = false">
          <X />
        </button>
      </div>

      <nav class="nav-list">
        <RouterLink
          v-for="item in navItems"
          :key="item.to"
          :to="item.to"
          class="nav-item"
          @click="menuOpen = false"
        >
          <component :is="item.icon" />
          <span>{{ item.label }}</span>
        </RouterLink>
      </nav>

      <div class="sidebar-note">
        <span class="note-dot" />
        <strong>每天 10 分钟</strong>
        <p>坚持练习，让准确率稳步提升。</p>
      </div>

      <RouterLink to="/settings" class="settings-entry" @click="menuOpen = false; closeProfile()">
        <Settings2 />
        <span>设置</span>
      </RouterLink>

      <div class="user-panel-wrap">
        <Transition name="profile-pop">
          <div v-if="profileOpen" class="profile-popover" @click.stop>
            <span class="popover-kicker">个人资料</span>
            <div class="popover-user">
              <span class="avatar large">
                <img v-if="auth.user?.avatarUrl" :src="auth.user.avatarUrl" alt="" />
                <template v-else>{{ initials }}</template>
              </span>
              <div><strong>{{ auth.user?.displayName || '学习者' }}</strong><span>@{{ auth.user?.username }}</span></div>
            </div>
            <div class="popover-info">
              <span><UserRound /> MathMate 学习账号</span>
              <span v-if="profileLoading">正在读取账号信息…</span>
              <span v-else-if="joinedDate()"><CalendarDays /> 加入于 {{ joinedDate() }}</span>
            </div>
            <button class="popover-edit" @click.stop="openSettings">
              编辑个人资料
              <ChevronRight />
            </button>
          </div>
        </Transition>

        <div class="user-panel">
          <button class="user-profile-button" title="查看个人资料" @click.stop="toggleProfile">
            <span class="avatar">
              <img v-if="auth.user?.avatarUrl" :src="auth.user.avatarUrl" alt="" />
              <template v-else>{{ initials }}</template>
            </span>
            <span class="user-copy">
              <strong>{{ auth.user?.displayName || '学习者' }}</strong>
              <span>@{{ auth.user?.username }}</span>
            </span>
          </button>
          <button class="icon-button logout-button" title="退出登录" @click.stop="logout">
            <LogOut />
          </button>
        </div>
      </div>
    </aside>

    <div v-if="menuOpen" class="sidebar-overlay" @click="menuOpen = false" />

    <section class="main-area">
      <header class="topbar">
        <button class="icon-button mobile-menu" @click="menuOpen = true">
          <Menu />
        </button>
        <div class="topbar-title">{{ pageTitle }}</div>
      </header>

      <main class="main-content">
        <RouterView v-slot="{ Component }">
          <Transition name="page" mode="out-in">
            <component :is="Component" />
          </Transition>
        </RouterView>
      </main>
    </section>
  </div>
</template>

<style scoped>
.app-shell { min-height: 100vh; background: var(--bg); }
.sidebar {
  position: fixed;
  inset: 0 auto 0 0;
  z-index: 30;
  display: flex;
  width: 246px;
  flex-direction: column;
  padding: 24px 18px 18px;
  background:
    radial-gradient(circle at 20% 0%, rgba(59, 130, 246, 0.24), transparent 32%),
    linear-gradient(175deg, #111c3f 0%, #172554 58%, #0f172a 100%);
  color: #fff;
}
.brand { display: flex; align-items: center; gap: 12px; padding: 0 8px 27px; }
.brand-mark {
  display: grid;
  width: 42px;
  height: 42px;
  place-items: center;
  border: 1px solid rgba(255,255,255,.13);
  border-radius: 14px;
  background: linear-gradient(145deg, #3b82f6, #14b8a6);
  box-shadow: 0 10px 22px rgba(37, 99, 235, .35);
}
.brand-mark svg { width: 22px; height: 22px; }
.brand strong { display: block; font-size: 18px; letter-spacing: .01em; }
.brand span { display: block; margin-top: 2px; color: #93a4c7; font-size: 11px; }
.nav-list { display: grid; gap: 7px; }
.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 12px;
  color: #aebbd8;
  font-size: 14px;
  font-weight: 600;
  text-decoration: none;
  transition: .2s ease;
}
.nav-item svg { width: 19px; height: 19px; }
.nav-item:hover { background: rgba(255,255,255,.06); color: #fff; }
.nav-item.router-link-active {
  background: linear-gradient(100deg, rgba(59,130,246,.28), rgba(20,184,166,.14));
  color: #fff;
  box-shadow: inset 0 0 0 1px rgba(147,197,253,.16);
}
.sidebar-note {
  margin: auto 4px 18px;
  padding: 15px;
  border: 1px solid rgba(255,255,255,.09);
  border-radius: 14px;
  background: rgba(255,255,255,.045);
}
.sidebar-note strong { display: block; font-size: 13px; }
.sidebar-note p { margin: 7px 0 0; color: #91a0c0; font-size: 11px; line-height: 1.65; }
.note-dot { display: block; width: 7px; height: 7px; margin-bottom: 9px; border-radius: 50%; background: #34d399; box-shadow: 0 0 0 4px rgba(52,211,153,.12); }
.settings-entry {
  display: flex;
  align-items: center;
  gap: 11px;
  margin: 0 4px 10px;
  padding: 10px 12px;
  border-radius: 11px;
  color: #9baac8;
  font-size: 12px;
  font-weight: 600;
  text-decoration: none;
  transition: .18s ease;
}
.settings-entry svg { width: 17px; height: 17px; }
.settings-entry:hover,
.settings-entry.router-link-active { background: rgba(255,255,255,.07); color: #fff; }
.user-panel-wrap { position: relative; }
.profile-popover {
  position: absolute;
  right: 0;
  bottom: calc(100% + 12px);
  left: 0;
  z-index: 40;
  padding: 16px;
  border: 1px solid rgba(148,163,184,.24);
  border-radius: 16px;
  background: #fff;
  color: #334155;
  box-shadow: 0 22px 55px rgba(2, 6, 23, .36);
}
.profile-popover::after { position: absolute; right: 22px; bottom: -6px; width: 11px; height: 11px; border-right: 1px solid #e2e8f0; border-bottom: 1px solid #e2e8f0; background: #fff; content: ''; transform: rotate(45deg); }
.popover-kicker { color: #2563eb; font-size: 8px; font-weight: 800; letter-spacing: .14em; }
.popover-user { display: flex; align-items: center; gap: 10px; margin: 12px 0 13px; }
.avatar.large { flex-basis: 42px; width: 42px; height: 42px; border-radius: 13px; font-size: 16px; }
.popover-user strong { display: block; color: #172554; font-family: var(--serif); font-size: 17px; }
.popover-user span:not(.avatar) { display: block; margin-top: 2px; color: #94a3b8; font-size: 9px; }
.popover-info { display: grid; gap: 7px; padding: 11px 0; border-top: 1px solid #f1f5f9; border-bottom: 1px solid #f1f5f9; }
.popover-info span { display: flex; align-items: center; gap: 6px; color: #7b8ba3; font-size: 9px; }
.popover-info svg { width: 12px; height: 12px; color: #64748b; }
.popover-edit { display: flex; width: 100%; align-items: center; justify-content: space-between; margin-top: 12px; padding: 9px 11px; border: 0; border-radius: 9px; background: #eff6ff; color: #2563eb; font-size: 10px; font-weight: 700; cursor: pointer; }
.popover-edit svg { width: 13px; height: 13px; }
.profile-pop-enter-active, .profile-pop-leave-active { transition: opacity .15s ease, transform .15s ease; }
.profile-pop-enter-from, .profile-pop-leave-to { opacity: 0; transform: translateY(5px); }
.user-panel {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 4px 4px;
  border-top: 1px solid rgba(255,255,255,.08);
}
.user-profile-button {
  display: flex;
  min-width: 0;
  flex: 1;
  align-items: center;
  gap: 10px;
  padding: 4px;
  border: 0;
  border-radius: 11px;
  background: transparent;
  color: inherit;
  text-align: left;
  cursor: pointer;
  transition: background .16s ease;
}
.user-profile-button:hover { background: rgba(255,255,255,.07); }
.avatar {
  overflow: hidden;
  display: grid;
  flex: 0 0 36px;
  width: 36px;
  height: 36px;
  place-items: center;
  border-radius: 50%;
  background: linear-gradient(145deg, #dbeafe, #99f6e4);
  color: #1e3a8a;
  font-weight: 800;
}
.avatar img { width: 100%; height: 100%; object-fit: cover; }
.user-copy { min-width: 0; flex: 1; }
.user-copy strong { display: block; overflow: hidden; font-size: 13px; text-overflow: ellipsis; white-space: nowrap; }
.user-copy span { display: block; margin-top: 2px; color: #8291b2; font-size: 10px; }
.icon-button {
  display: grid;
  width: 36px;
  height: 36px;
  place-items: center;
  border: 0;
  border-radius: 10px;
  background: transparent;
  color: inherit;
  cursor: pointer;
}
.icon-button svg { width: 19px; height: 19px; }
.logout-button { color: #94a3b8; }
.logout-button:hover { background: rgba(255,255,255,.08); color: #fff; }
.main-area { min-height: 100vh; margin-left: 246px; }
.topbar {
  position: sticky;
  top: 0;
  z-index: 20;
  display: flex;
  height: 68px;
  align-items: center;
  gap: 14px;
  padding: 0 34px;
  border-bottom: 1px solid rgba(148,163,184,.16);
  background: rgba(248,250,252,.82);
  backdrop-filter: blur(18px);
}
.topbar-title { flex: 1; color: #334155; font-size: 14px; font-weight: 700; }
.main-content { width: min(1280px, 100%); margin: 0 auto; padding: 30px 34px 50px; }
.mobile-menu, .mobile-close { display: none; }
.sidebar-overlay { display: none; }
.page-enter-active, .page-leave-active { transition: opacity .18s ease, transform .18s ease; }
.page-enter-from { opacity: 0; transform: translateY(5px); }
.page-leave-to { opacity: 0; }
@media (max-width: 900px) {
  .sidebar { transform: translateX(-100%); transition: transform .22s ease; }
  .sidebar.open { transform: translateX(0); }
  .main-area { margin-left: 0; }
  .mobile-menu, .mobile-close { display: grid; }
  .mobile-close { margin-left: auto; color: #cbd5e1; }
  .sidebar-overlay { position: fixed; inset: 0; z-index: 25; display: block; background: rgba(15,23,42,.5); backdrop-filter: blur(2px); }
  .topbar { padding: 0 18px; }
  .main-content { padding: 24px 18px 40px; }
}
</style>
