// 认证状态管理：登录、注册、登出、Token 持久化
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login as loginApi, register as registerApi, getMe } from '../api/auth'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || '')
  const nickname = ref(localStorage.getItem('nickname') || '')
  const permission = ref(Number(localStorage.getItem('permission') || 1))

  /** 登录：保存 Token/昵称/权限到 Pinia 和 localStorage */
  async function login(username: string, password: string) {
    const res = await loginApi(username, password)
    const data = res.data.data
    token.value = data.token
    nickname.value = data.nickname
    permission.value = data.permission ?? 1
    localStorage.setItem('token', data.token)
    localStorage.setItem('nickname', data.nickname)
    localStorage.setItem('permission', String(data.permission ?? 1))
  }

  /** 注册并自动登录 */
  async function register(username: string, password: string, nick: string) {
    const res = await registerApi(username, password, nick)
    const data = res.data.data
    token.value = data.token
    nickname.value = data.nickname
    permission.value = data.permission ?? 1
    localStorage.setItem('token', data.token)
    localStorage.setItem('nickname', data.nickname)
    localStorage.setItem('permission', String(data.permission ?? 1))
  }

  /** 登出：清除所有登录态 */
  function logout() {
    token.value = ''
    nickname.value = ''
    permission.value = 1
    localStorage.removeItem('token')
    localStorage.removeItem('nickname')
    localStorage.removeItem('permission')
  }

  function isLoggedIn() {
    return !!token.value
  }

  return { token, nickname, permission, login, register, logout, isLoggedIn }
})
