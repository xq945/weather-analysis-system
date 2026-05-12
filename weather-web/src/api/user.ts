import api from './index'

export function getUsers() {
  return api.get('/api/users')
}

export function updateUserStatus(id: number, status: number) {
  return api.put(`/api/users/${id}/status`, { status })
}

export function updateUserPermission(id: number, permission: number) {
  return api.put(`/api/users/${id}/permission`, { permission })
}
