import api from './index'

export function getOverview() {
  return api.get('/api/weather/overview')
}

export function fetchWeather() {
  return api.post('/api/weather/fetch')
}

export function getWeatherNow(city: string) {
  return api.get('/api/weather/now', { params: { city } })
}

export function getForecast(city: string) {
  return api.get('/api/weather/forecast', { params: { city } })
}

export function getHistory(city: string, days: number = 7) {
  return api.get('/api/weather/history', { params: { city, days } })
}

export function getStatistics(city: string, days: number = 7) {
  return api.get('/api/weather/statistics', { params: { city, days } })
}

export function getCompare(cityA: string, cityB: string, days: number = 7) {
  return api.get('/api/weather/compare', { params: { cityA, cityB, days } })
}
