import api from './index'

export function getCities() {
  return api.get('/api/cities')
}

export function getAllCities() {
  return api.get('/api/cities/all')
}

export function getAdminAllCities() {
  return api.get('/api/cities/admin/all')
}

export function addCity(city: string) {
  return api.post('/api/cities', { city })
}

export function removeCity(id: number) {
  return api.delete(`/api/cities/${id}`)
}
