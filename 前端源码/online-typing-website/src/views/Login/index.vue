<template>
  <AuthLayout :features="features">
    <template #feature-icon-0>
      <svg width="22" height="22" viewBox="0 0 22 22" fill="none">
        <rect x="3" y="3" width="16" height="16" rx="3" stroke="currentColor" stroke-width="1.5"/>
        <path d="M7 8H15M7 11H12M7 14H14" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
      </svg>
    </template>
    <template #feature-icon-1>
      <svg width="22" height="22" viewBox="0 0 22 22" fill="none">
        <circle cx="11" cy="11" r="8" stroke="currentColor" stroke-width="1.5"/>
        <path d="M11 7V11L14 13" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
      </svg>
    </template>
    <template #feature-icon-2>
      <svg width="22" height="22" viewBox="0 0 22 22" fill="none">
        <path d="M3 16L8 11L12 15L19 6" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
        <path d="M15 6H19V10" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
      </svg>
    </template>

    <h2>欢迎回来</h2>
    <p class="subtitle">登录以继续使用 Define.XML 系统</p>

    <div class="form-group">
      <label>用户名</label>
      <div class="input-wrapper">
        <svg class="input-icon" width="18" height="18" viewBox="0 0 18 18" fill="none">
          <path d="M15 15.75V14.25C15 13.4544 14.6839 12.6913 14.1213 12.1287C13.5587 11.5661 12.7956 11.25 12 11.25H6C5.20435 11.25 4.44129 11.5661 3.87868 12.1287C3.31607 12.6913 3 13.4544 3 14.25V15.75" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
          <circle cx="9" cy="5.25" r="3" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
        <input
          type="text"
          v-model="loginForm.username"
          placeholder="请输入用户名"
          @keyup.enter="handleLogin"
        >
      </div>
    </div>

    <div class="form-group">
      <label>密码</label>
      <div class="input-wrapper">
        <svg class="input-icon" width="18" height="18" viewBox="0 0 18 18" fill="none">
          <rect x="3" y="8.25" width="12" height="7.5" rx="2" stroke="currentColor" stroke-width="1.5"/>
          <path d="M5.25 8.25V5.25C5.25 4.25544 5.64509 3.30161 6.34835 2.59835C7.05161 1.89509 8.00544 1.5 9 1.5C9.99456 1.5 10.9484 1.89509 11.6516 2.59835C12.3549 3.30161 12.75 4.25544 12.75 5.25V8.25" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
        <input
          type="password"
          v-model="loginForm.password"
          placeholder="请输入密码"
          @keyup.enter="handleLogin"
        >
        <button class="toggle-pwd" @click="togglePassword" type="button" tabindex="-1">
          <svg v-if="!showPassword" width="18" height="18" viewBox="0 0 18 18" fill="none">
            <path d="M1.5 9C1.5 9 4.5 3 9 3C13.5 3 16.5 9 16.5 9C16.5 9 13.5 15 9 15C4.5 15 1.5 9 1.5 9Z" stroke="currentColor" stroke-width="1.3" stroke-linecap="round"/>
            <circle cx="9" cy="9" r="2.5" stroke="currentColor" stroke-width="1.3"/>
          </svg>
          <svg v-else width="18" height="18" viewBox="0 0 18 18" fill="none">
            <path d="M2 2L16 16M7.06 7.06A2.5 2.5 0 0010.94 10.94M1.5 9C1.5 9 4.5 3 9 3C10.29 3 11.46 3.46 12.47 4.11M16.5 9C16.5 9 15.3 11.4 13 13" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </button>
      </div>
    </div>

    <button class="submit-btn" @click="handleLogin" :disabled="loading">
      <span v-if="!loading">登 录</span>
      <span v-else class="loading-dots">
        <span></span><span></span><span></span>
      </span>
    </button>

    <div class="alt-login">
      <div class="divider-line"><span>其他登录方式</span></div>
      <button class="alt-login-btn" disabled>
        <svg width="18" height="18" viewBox="0 0 18 18" fill="none">
          <path d="M12.24 10.285V14.445H15.97C14.965 16.19 13.12 17.3 11 17.3C7.577 17.3 4.8 14.523 4.8 11.1C4.8 7.677 7.577 4.9 11 4.9C12.459 4.9 13.793 5.413 14.84 6.27L16.77 4.34C15.213 2.905 13.193 2 11 2C5.865 2 1.7 6.165 1.7 11.3C1.7 16.435 5.865 20.6 11 20.6" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
        企业SSO登录
      </button>
    </div>

    <div class="form-footer">
      还没有账号？
      <router-link to="/register" class="link">联系管理员</router-link>
      开通账号
      <span class="contact-link" @mouseover="showImage = true" @mouseleave="showImage = false">
      </span>
      <img v-if="showImage" src="@/assets/admin.jpg" class="admin-image" />
    </div>
  </AuthLayout>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import axios from '@/axios'
import AuthLayout from '@/components/shared/AuthLayout.vue'

const router = useRouter()
const loading = ref(false)
const showImage = ref(false)
const showPassword = ref(false)

const features = [
  { title: '表单设计', desc: '灵活的CRF表单设计，支持多种字段类型' },
  { title: '智能识别', desc: 'AI驱动的OCR识别，自动提取关键Spec数据' },
  { title: '数据分析', desc: '强大的数据导出与统计分析功能' }
]

const loginForm = reactive({
  username: '',
  password: ''
})

const togglePassword = () => {
  showPassword.value = !showPassword.value
  const pwdInput = document.querySelector('input[type="password"], input.pwd-visible')
  if (pwdInput) {
    pwdInput.type = showPassword.value ? 'text' : 'password'
  }
}

const handleLogin = async () => {
  if (!loginForm.username) {
    ElMessage.warning('请输入用户名')
    return
  }
  if (!loginForm.password) {
    ElMessage.warning('请输入密码')
    return
  }

  loading.value = true
  try {
    const res = await axios.post('/user/login', {
      username: loginForm.username,
      password: loginForm.password
    })

    if (res.data.success) {
      localStorage.setItem('user', JSON.stringify(res.data.data))
      ElMessage.success('登录成功')
      router.push('/')
    } else {
      ElMessage.error(res.data.message || '用户名或密码错误')
    }
  } catch (err) {
    ElMessage.error('服务器连接失败，请稍后重试')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="less">
.form-group {
  margin-bottom: 22px;

  label {
    display: block;
    font-size: 13px;
    font-weight: 600;
    color: var(--saas-text-primary);
    margin-bottom: 8px;
  }

  .input-wrapper {
    position: relative;

    .input-icon {
      position: absolute;
      left: 14px;
      top: 50%;
      transform: translateY(-50%);
      color: var(--saas-text-tertiary);
      pointer-events: none;
    }

    input {
      width: 100%;
      height: 48px;
      padding: 0 44px 0 42px;
      border: 1.5px solid var(--saas-border);
      border-radius: 12px;
      font-size: 14px;
      font-family: inherit;
      color: var(--saas-text-primary);
      background: var(--saas-bg-card);
      outline: none;
      transition: all 0.2s ease;

      &::placeholder {
        color: var(--saas-text-tertiary);
      }

      &:focus {
        border-color: var(--saas-primary);
        box-shadow: 0 0 0 3px var(--saas-primary-bg);
      }
    }

    .toggle-pwd {
      position: absolute;
      right: 12px;
      top: 50%;
      transform: translateY(-50%);
      background: none;
      border: none;
      color: var(--saas-text-tertiary);
      cursor: pointer;
      padding: 4px;
      display: flex;
      align-items: center;
      justify-content: center;
      transition: color 0.2s;

      &:hover {
        color: var(--saas-text-secondary);
      }
    }
  }
}

.submit-btn {
  width: 100%;
  height: 48px;
  border: none;
  border-radius: 12px;
  background: linear-gradient(135deg, var(--saas-primary), var(--saas-primary-dark));
  color: var(--saas-text-inverse);
  font-size: 16px;
  font-weight: 600;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.25s ease;
  margin-top: 4px;

  &:hover:not(:disabled) {
    transform: translateY(-1px);
    box-shadow: 0 6px 20px rgba(99, 102, 241, 0.4);
  }

  &:active:not(:disabled) {
    transform: translateY(0);
  }

  &:disabled {
    opacity: 0.7;
    cursor: not-allowed;
  }
}

.loading-dots {
  display: inline-flex;
  gap: 4px;

  span {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: var(--saas-text-inverse);
    animation: blink 1.4s infinite both;

    &:nth-child(2) { animation-delay: 0.2s; }
    &:nth-child(3) { animation-delay: 0.4s; }
  }
}

@keyframes blink {
  0%, 80%, 100% { opacity: 0.3; }
  40% { opacity: 1; }
}

.alt-login {
  margin-top: 28px;

  .divider-line {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 16px;

    &::before,
    &::after {
      content: '';
      flex: 1;
      height: 1px;
      background: var(--saas-border);
    }

    span {
      font-size: 12px;
      color: var(--saas-text-tertiary);
      white-space: nowrap;
    }
  }

  .alt-login-btn {
    width: 100%;
    height: 44px;
    border: 1.5px solid var(--saas-border);
    border-radius: 12px;
    background: var(--saas-bg-card);
    color: var(--saas-text-secondary);
    font-size: 14px;
    font-family: inherit;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    transition: all 0.2s ease;

    &:hover:not(:disabled) {
      border-color: var(--saas-primary-lighter);
      color: var(--saas-primary);
      background: var(--saas-primary-bg);
    }

    &:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }
  }
}

.form-footer {
  text-align: center;
  margin-top: 24px;
  font-size: 13px;
  color: var(--saas-text-secondary);

  .link {
    color: var(--saas-primary);
    font-weight: 500;
    text-decoration: none;

    &:hover {
      text-decoration: underline;
    }
  }

  .contact-link {
    color: var(--saas-primary);
    cursor: pointer;
    font-weight: 500;
  }
}

.admin-image {
  position: absolute;
  display: block;
  width: 240px;
  height: auto;
  border-radius: var(--saas-radius-lg);
  box-shadow: var(--saas-shadow-xl);
  z-index: 999;
  margin-top: -80px;
  margin-left: 200px;
}
</style>
