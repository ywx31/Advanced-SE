<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowRight, Divide, Equal, Plus, Sparkles } from 'lucide-vue-next'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const mode = ref<'login' | 'register'>('login')
const username = ref('')
const password = ref('')
const displayName = ref('')
const error = ref('')
const submitting = ref(false)

const isRegister = computed(() => mode.value === 'register')
const title = computed(() => (isRegister.value ? '创建学习档案' : '欢迎回来'))
const subtitle = computed(() =>
  isRegister.value
    ? '记录每一次练习，看见自己的进步。'
    : '继续今天的数学练习吧。',
)

async function submit() {
  error.value = ''
  if (!username.value || !password.value || (isRegister.value && !displayName.value)) {
    error.value = '请完整填写表单'
    return
  }
  submitting.value = true
  try {
    if (isRegister.value) {
      await auth.register(username.value, password.value, displayName.value)
    } else {
      await auth.login(username.value, password.value)
    }
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/dashboard'
    await router.push(redirect)
  } catch (err) {
    error.value = err instanceof Error ? err.message : '操作失败，请稍后重试'
  } finally {
    submitting.value = false
  }
}

async function useDemo() {
  username.value = 'demo'
  password.value = 'demo123'
  mode.value = 'login'
  await submit()
}

function switchMode() {
  mode.value = isRegister.value ? 'login' : 'register'
  error.value = ''
}
</script>

<template>
  <main class="login-page">
    <section class="brand-panel">
      <div class="brand-line">
        <div class="logo-box"><Sparkles /></div>
        <span>MathMate</span>
      </div>

      <div class="hero-copy">
        <span class="eyebrow">SMART MATH PRACTICE</span>
        <h1>让每一道题，<br /><em>都看得见成长。</em></h1>
        <p>智能生成表内乘除、表外乘除和混合运算。自动判题、错题沉淀、趋势分析，让练习更有方向。</p>
      </div>

      <div class="floating-board" aria-hidden="true">
        <div class="math-card card-one"><span>8 × 7</span><strong>= 56</strong></div>
        <div class="math-card card-two"><span>72 ÷ 9</span><strong>= 8</strong></div>
        <div class="math-card card-three"><span>45 + 28</span><strong>= 73</strong></div>
        <div class="symbol symbol-plus"><Plus /></div>
        <div class="symbol symbol-divide"><Divide /></div>
        <div class="symbol symbol-equal"><Equal /></div>
      </div>

      <div class="brand-foot">
        <span>自动出题</span><i />
        <span>即时反馈</span><i />
        <span>学习分析</span>
      </div>
    </section>

    <section class="form-panel">
      <div class="form-card">
        <div class="mobile-logo"><Sparkles /><span>MathMate</span></div>
        <div class="form-heading">
          <span class="mini-label">{{ isRegister ? 'REGISTER' : 'SIGN IN' }}</span>
          <h2>{{ title }}</h2>
          <p>{{ subtitle }}</p>
        </div>

        <form @submit.prevent="submit">
          <div v-if="isRegister" class="field">
            <label for="displayName">你的昵称</label>
            <input id="displayName" v-model.trim="displayName" class="input" placeholder="比如：小明" autocomplete="name" />
          </div>
          <div class="field">
            <label for="username">用户名</label>
            <input id="username" v-model.trim="username" class="input" placeholder="3-30 位字母、数字或下划线" autocomplete="username" />
          </div>
          <div class="field">
            <label for="password">密码</label>
            <input id="password" v-model="password" class="input" type="password" placeholder="至少 6 位" autocomplete="current-password" />
          </div>

          <div v-if="error" class="alert">{{ error }}</div>

          <button class="btn btn-primary btn-lg submit-button" :disabled="submitting">
            {{ submitting ? '请稍候…' : isRegister ? '创建账号' : '进入学习空间' }}
            <ArrowRight v-if="!submitting" />
          </button>
        </form>

        <button class="demo-button" type="button" :disabled="submitting" @click="useDemo">
          <span class="demo-icon">D</span>
          <span><strong>体验演示账号</strong><small>demo / demo123，内含学习数据</small></span>
          <ArrowRight />
        </button>

        <p class="switch-line">
          {{ isRegister ? '已经有账号？' : '第一次使用？' }}
          <button type="button" @click="switchMode">{{ isRegister ? '直接登录' : '创建账号' }}</button>
        </p>
      </div>
    </section>
  </main>
</template>

<style scoped>
.login-page { display: grid; min-height: 100vh; grid-template-columns: minmax(0, 1.08fr) minmax(430px, .92fr); background: #f8fafc; }
.brand-panel { position: relative; overflow: hidden; display: flex; min-height: 100vh; flex-direction: column; padding: 46px clamp(38px, 6vw, 82px) 42px; background: radial-gradient(circle at 85% 15%, rgba(45,212,191,.22), transparent 26%), radial-gradient(circle at 20% 80%, rgba(96,165,250,.22), transparent 30%), linear-gradient(145deg, #0f172a, #172554 56%, #0b2444); color: #fff; }
.brand-panel::after { position: absolute; right: -100px; bottom: -180px; width: 430px; height: 430px; border: 1px solid rgba(255,255,255,.08); border-radius: 50%; box-shadow: 0 0 0 80px rgba(255,255,255,.025), 0 0 0 160px rgba(255,255,255,.015); content: ''; }
.brand-line { display: flex; align-items: center; gap: 12px; font-size: 19px; font-weight: 800; letter-spacing: .02em; }
.logo-box { display: grid; width: 41px; height: 41px; place-items: center; border: 1px solid rgba(255,255,255,.16); border-radius: 13px; background: linear-gradient(145deg, #3b82f6, #14b8a6); }
.logo-box svg { width: 21px; height: 21px; }
.hero-copy { position: relative; z-index: 2; max-width: 650px; margin-top: clamp(70px, 14vh, 145px); }
.eyebrow { color: #7dd3fc; font-size: 11px; font-weight: 800; letter-spacing: .2em; }
h1 { margin: 20px 0 23px; font-family: var(--serif); font-size: clamp(40px, 5.2vw, 68px); font-weight: 600; line-height: 1.22; letter-spacing: -.04em; }
h1 em { color: #5eead4; font-style: normal; }
.hero-copy p { max-width: 530px; margin: 0; color: #aab8d1; font-size: 15px; line-height: 1.9; }
.floating-board { position: relative; z-index: 2; height: 180px; margin-top: 25px; }
.math-card { position: absolute; display: flex; align-items: center; gap: 9px; padding: 12px 16px; border: 1px solid rgba(255,255,255,.14); border-radius: 14px; background: rgba(255,255,255,.075); box-shadow: 0 16px 40px rgba(0,0,0,.18); backdrop-filter: blur(10px); font-family: var(--serif); }
.math-card span { color: #dbeafe; }
.math-card strong { color: #5eead4; }
.card-one { top: 28px; left: 0; transform: rotate(-4deg); }
.card-two { top: 55px; left: 225px; transform: rotate(4deg); }
.card-three { top: 115px; left: 55px; transform: rotate(2deg); opacity: .9; }
.symbol { position: absolute; display: grid; place-items: center; border-radius: 50%; background: rgba(255,255,255,.06); color: #93c5fd; }
.symbol svg { width: 20px; height: 20px; }
.symbol-plus { top: 9px; left: 310px; width: 47px; height: 47px; }
.symbol-divide { top: 101px; left: 440px; width: 38px; height: 38px; color: #5eead4; }
.symbol-equal { top: 4px; left: 500px; width: 34px; height: 34px; }
.brand-foot { display: flex; align-items: center; gap: 14px; margin-top: auto; color: #7787a7; font-size: 11px; letter-spacing: .04em; }
.brand-foot i { width: 3px; height: 3px; border-radius: 50%; background: #41516f; }
.form-panel { display: grid; place-items: center; padding: 42px clamp(28px, 5vw, 78px); background: #f8fafc; }
.form-card { width: min(100%, 420px); }
.mobile-logo { display: none; }
.form-heading { margin-bottom: 32px; }
.mini-label { color: #2563eb; font-size: 10px; font-weight: 800; letter-spacing: .18em; }
.form-heading h2 { margin: 10px 0 8px; color: #0f172a; font-family: var(--serif); font-size: 34px; font-weight: 600; letter-spacing: -.035em; }
.form-heading p { margin: 0; color: #64748b; font-size: 14px; }
form { display: grid; gap: 17px; }
.submit-button { width: 100%; margin-top: 6px; }
.demo-button { display: flex; width: 100%; align-items: center; gap: 12px; margin-top: 18px; padding: 13px 14px; border: 1px dashed #bfdbfe; border-radius: 13px; background: #eff6ff; color: #1e3a8a; text-align: left; cursor: pointer; }
.demo-icon { display: grid; flex: 0 0 35px; width: 35px; height: 35px; place-items: center; border-radius: 10px; background: #dbeafe; font-weight: 800; }
.demo-button span:nth-child(2) { flex: 1; }
.demo-button strong { display: block; font-size: 12px; }
.demo-button small { display: block; margin-top: 3px; color: #64748b; font-size: 10px; }
.demo-button svg { width: 16px; height: 16px; }
.switch-line { margin: 25px 0 0; color: #94a3b8; font-size: 12px; text-align: center; }
.switch-line button { border: 0; background: none; color: #2563eb; font-weight: 700; cursor: pointer; }
@media (max-width: 900px) {
  .login-page { display: block; }
  .brand-panel { display: none; }
  .form-panel { min-height: 100vh; padding: 26px; background: radial-gradient(circle at 100% 0%, #dbeafe, transparent 34%), #f8fafc; }
  .mobile-logo { display: flex; align-items: center; gap: 9px; margin-bottom: 60px; color: #172554; font-weight: 800; }
  .mobile-logo svg { width: 22px; height: 22px; color: #2563eb; }
}
</style>
