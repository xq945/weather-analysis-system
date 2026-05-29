// 城市管理 API：关注/取消城市、城市列表
import api from './index'

export function getCities() {
  return api.get('/api/cities')               // 我的关注城市
}

export function getAllCities() {
  return api.get('/api/cities/all')           // 所有被关注的城市名（去重）
}

export function getAdminAllCities() {
  return api.get('/api/cities/admin/all')     // 管理员：所有人的关注记录
}

export function addCity(city: string) {
  return api.post('/api/cities', { city })
}

export function removeCity(id: number) {
  return api.delete(`/api/cities/${id}`)
}
