import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import axios from '@/axios'
import router from '@/router'

export const useUserStore = defineStore('user', () => {
  const userInfo = ref(null)

  const isLoggedIn = computed(() => !!userInfo.value)
  const username = computed(() => userInfo.value?.username || '')
  const token = computed(() => userInfo.value?.token || '')

  function loadFromStorage() {
    const stored = localStorage.getItem('user')
    if (stored) {
      try {
        userInfo.value = JSON.parse(stored)
      } catch (e) {
        localStorage.removeItem('user')
      }
    }
  }

  async function login(loginData) {
    const res = await axios.post('/user/login', loginData)
    if (res.data.success) {
      userInfo.value = res.data.data
      localStorage.setItem('user', JSON.stringify(res.data.data))
      return true
    }
    throw new Error(res.data.message || '登录失败')
  }

  function logout() {
    userInfo.value = null
    localStorage.removeItem('user')
    router.push('/login')
  }

  loadFromStorage()

  return { userInfo, isLoggedIn, username, token, login, logout, loadFromStorage }
})
