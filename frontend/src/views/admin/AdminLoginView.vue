<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { loginAdmin } from '../../api/auth'
import { useAuthStore } from '../../stores/auth'

const router = useRouter()
const auth = useAuthStore()

const username = ref('admin')
const password = ref('')
const loading = ref(false)
const error = ref('')

async function doLogin() {
  loading.value = true
  error.value = ''
  try {
    const result = await loginAdmin(username.value, password.value)
    auth.setAuth(result)
    router.push('/admin')
  } catch {
    error.value = '登录失败，请检查用户名和密码'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="login-page">
    <div class="login-card">
      <h1>太原食探 · 管理后台</h1>
      <p>请使用管理员账号登录</p>
      <div class="login-form">
        <input v-model="username" type="text" placeholder="用户名" class="field" @keyup.enter="doLogin" />
        <input v-model="password" type="password" placeholder="密码" class="field" @keyup.enter="doLogin" />
        <button class="login-btn" :disabled="loading" @click="doLogin">
          {{ loading ? '登录中…' : '登录' }}
        </button>
        <p v-if="error" class="err">{{ error }}</p>
      </div>
    </div>
  </main>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #FFF7F1, #FFE7D1);
}

.login-card {
  width: 360px;
  padding: 32px;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 8px 32px rgba(0,0,0,0.08);
  text-align: center;
}

h1 { margin: 0 0 4px; font-size: 20px; }
p { margin: 0 0 20px; font-size: 14px; color: #8E8E93; }

.login-form { display: grid; gap: 12px; }

.field {
  height: 44px;
  padding: 0 14px;
  border: 1px solid #F1E6DC;
  border-radius: 12px;
  font-size: 15px;
  outline: none;
}
.field:focus { border-color: #FF7A2E; }

.login-btn {
  height: 44px;
  border: none;
  border-radius: 24px;
  background: #FF7A2E;
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
}
.login-btn:disabled { opacity: 0.6; }

.err { margin: 8px 0 0; color: #FF4D2E; font-size: 13px; }
</style>
