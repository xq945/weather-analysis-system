import api from './index'

export function generateReport(city: string, date: string, reportType: number = 1) {
  return api.post('/api/admin/report/generate', { city, date, reportType })
}

export function getReportList(city?: string, date?: string) {
  return api.get('/api/report/list', { params: { city, date } })
}

export function getReportDetail(reportId: string) {
  return api.get(`/api/report/${reportId}`)
}

export function deleteReport(reportId: string) {
  return api.delete(`/api/admin/report/${reportId}`)
}

export function resyncReport(id: number) {
  return api.post(`/api/admin/report/sync/${id}`)
}
