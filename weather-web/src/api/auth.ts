import api from './index'

export function login(username: string, password: string) {
  return api.post('/api/auth/login', { username, password })
}

export function register(username: string, password: string, nickname: string) {
  return api.post('/api/auth/register', { username, password, nickname })
}

export function getMe() {
  return api.get('/api/auth/me')
}
