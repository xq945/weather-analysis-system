import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login as loginApi, register as registerApi, getMe } from '../api/auth'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || '')
  const nickname = ref(localStorage.getItem('nickname') || '')

  async function login(username: string, password: string) {
    const res = await loginApi(username, password)
    const data = res.data.data
    token.value = data.token
    nickname.value = data.nickname
    localStorage.setItem('token', data.token)
    localStorage.setItem('nickname', data.nickname)
  }

  async function register(username: string, password: string, nick: string) {
    const res = await registerApi(username, password, nick)
    const data = res.data.data
    token.value = data.token
    nickname.value = data.nickname
    localStorage.setItem('token', data.token)
    localStorage.setItem('nickname', data.nickname)
  }

  function logout() {
    token.value = ''
    nickname.value = ''
    localStorage.removeItem('token')
    localStorage.removeItem('nickname')
  }

  function isLoggedIn() {
    return !!token.value
  }

  return { token, nickname, login, register, logout, isLoggedIn }
})
