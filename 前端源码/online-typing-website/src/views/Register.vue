<template>
  <AuthLayout :features="features">
    <template #feature-icon-0>
      <svg width="22" height="22" viewBox="0 0 22 22" fill="none">
        <path d="M11 14C14.3137 14 17 11.3137 17 8C17 4.68629 14.3137 2 11 2C7.68629 2 5 4.68629 5 8C5 11.3137 7.68629 14 11 14Z" stroke="currentColor" stroke-width="1.5"/>
        <path d="M3 20C3 17.2386 6.58172 15 11 15C15.4183 15 19 17.2386 19 20" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
      </svg>
    </template>
    <template #feature-icon-1>
      <svg width="22" height="22" viewBox="0 0 22 22" fill="none">
        <rect x="3" y="3" width="16" height="16" rx="3" stroke="currentColor" stroke-width="1.5"/>
        <path d="M8 11L10 13L14 9" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
      </svg>
    </template>
    <template #feature-icon-2>
      <svg width="22" height="22" viewBox="0 0 22 22" fill="none">
        <path d="M3 16L8 11L12 15L19 6" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
        <path d="M15 6H19V10" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
      </svg>
    </template>

    <h2>创建账户</h2>
    <p class="subtitle">注册新账户以开始使用系统</p>

    <div class="form-group">
      <label>用户名</label>
      <div class="input-wrapper">
        <svg class="input-icon" width="18" height="18" viewBox="0 0 18 18" fill="none">
          <path d="M15 15.75V14.25C15 13.4544 14.6839 12.6913 14.1213 12.1287C13.5587 11.5661 12.7956 11.25 12 11.25H6C5.20435 11.25 4.44129 11.5661 3.87868 12.1287C3.31607 12.6913 3 13.4544 3 14.25V15.75" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
          <circle cx="9" cy="5.25" r="3" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
        <input
          type="text"
          v-model="registerForm.username"
          placeholder="请输入用户名"
          @keyup.enter="handleRegister"
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
          v-model="registerForm.password"
          placeholder="请输入密码"
        >
      </div>
    </div>

    <div class="form-group">
      <label>确认密码</label>
      <div class="input-wrapper">
        <svg class="input-icon" width="18" height="18" viewBox="0 0 18 18" fill="none">
          <rect x="3" y="8.25" width="12" height="7.5" rx="2" stroke="currentColor" stroke-width="1.5"/>
          <path d="M5.25 8.25V5.25C5.25 4.25544 5.64509 3.30161 6.34835 2.59835C7.05161 1.89509 8.00544 1.5 9 1.5C9.99456 1.5 10.9484 1.89509 11.6516 2.59835C12.3549 3.30161 12.75 4.25544 12.75 5.25V8.25" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
        <input
          type="password"
          v-model="registerForm.confirmPassword"
          placeholder="请确认密码"
          @keyup.enter="handleRegister"
        >
      </div>
    </div>

    <button class="submit-btn" @click="handleRegister">注 册</button>

    <div class="form-footer">
      已有账号？
      <router-link to="/login" class="link">立即登录</router-link>
    </div>
  </AuthLayout>
</template>

<script setup>
import { reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import axios from '../axios'
import AuthLayout from '@/components/shared/AuthLayout.vue'

const router = useRouter()

const features = [
  { title: '安全的账户管理', desc: '多因素认证，企业级数据安全保障' },
  { title: '多项目协同工作', desc: '团队成员在线协作，高效管理多个项目' },
  { title: '专业级数据处理', desc: '符合CDISC标准的数据转换与质控' }
]

const registerForm = reactive({
  username: '',
  password: '',
  confirmPassword: ''
})

const handleRegister = async () => {
  if (!registerForm.username || !registerForm.password || !registerForm.confirmPassword) {
    ElMessage.error('请填写完整信息')
    return
  }
  if (registerForm.password !== registerForm.confirmPassword) {
    ElMessage.error('两次输入的密码不一致')
    return
  }

  try {
    const response = await axios({
      url: '/user/register',
      method: 'post',
      data: {
        username: registerForm.username,
        password: registerForm.password
      }
    })
    if (response.data.code === 200 || response.data.code === '0') {
      ElMessage.success('注册成功')
      router.push('/login')
    } else {
      ElMessage.error(response.data.message || '注册失败')
    }
  } catch (error) {
    ElMessage.error('注册失败:' + error.message)
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
      padding: 0 14px 0 42px;
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

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 6px 20px rgba(99, 102, 241, 0.4);
  }

  &:active {
    transform: translateY(0);
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
}
</style>
