import { defineStore } from 'pinia'
import { ref, computed, watch } from 'vue'
import { getCities, getAllCities, getAdminAllCities } from '../api/city'

export const useCitiesStore = defineStore('cities', () => {
  const myList = ref<any[]>([])
  const allList = ref<any[]>([])
  const adminAllList = ref<any[]>([])
  const filterMode = ref<'mine' | 'all'>(
    (localStorage.getItem('cityFilter') as 'mine' | 'all') || 'all'
  )

  // watch filterMode and persist to localStorage
  watch(filterMode, (val) => {
    localStorage.setItem('cityFilter', val)
  })

  const activeList = computed<any[]>(() =>
    filterMode.value === 'all' ? allList.value : myList.value
  )

  async function loadMy() {
    try {
      const res = await getCities()
      myList.value = res.data.data || []
    } catch { /* ignore */ }
  }

  async function loadAll() {
    try {
      const res = await getAllCities()
      allList.value = (res.data.data || []).map((c: string) => ({ city: c }))
    } catch { /* ignore */ }
  }

  async function loadAdminAll() {
    try {
      const res = await getAdminAllCities()
      adminAllList.value = res.data.data || []
    } catch { /* ignore */ }
  }

  async function load() {
    await Promise.all([loadMy(), loadAll()])
  }

  return { myList, allList, adminAllList, filterMode, activeList, load, loadMy, loadAll, loadAdminAll }
})
