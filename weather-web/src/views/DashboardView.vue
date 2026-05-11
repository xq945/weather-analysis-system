<template>
  <div class="dashboard">
    <div class="header">
      <h2>天气概览</h2>
      <el-button type="primary" @click="handleFetch" :loading="fetching">拉取最新数据</el-button>
    </div>

    <el-empty v-if="cities.length === 0" description="暂无关注城市，请先前往「城市管理」添加城市" />

    <div class="card-grid" v-if="cities.length > 0">
      <el-card v-for="c in cities" :key="c.city" class="weather-card" shadow="hover">
        <template #header>
          <span class="city-name">{{ c.city }}</span>
          <span v-if="c.obsTime" class="time">{{ c.obsTime }}</span>
        </template>
        <div v-if="c.temp !== undefined" class="card-body">
          <div class="temp-area">
            <span class="temp">{{ Math.round(c.temp) }}°</span>
            <span class="text">{{ c.weatherText }}</span>
          </div>
          <div class="details">
            <div class="detail-item"><span class="label">体感</span><span class="val">{{ Math.round(c.feelsLike) }}°</span></div>
            <div class="detail-item"><span class="label">湿度</span><span class="val">{{ Math.round(c.humidity) }}%</span></div>
          </div>
        </div>
        <div v-else class="no-data">
          暂无数据，请点击「拉取最新数据」
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getOverview, fetchWeather } from '../api/weather'

const cities = ref<any[]>([])
const fetching = ref(false)

async function loadOverview() {
  try {
    const res = await getOverview()
    cities.value = res.data.data || []
  } catch { /* ignore */ }
}

async function handleFetch() {
  fetching.value = true
  try {
    const res = await fetchWeather()
    const d = res.data.data
    ElMessage.success(`拉取完成：${d.nowFetched} 个城市`)
    await loadOverview()
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || '拉取失败')
  } finally {
    fetching.value = false
  }
}

onMounted(loadOverview)
</script>

<style scoped>
.header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.header h2 { margin: 0; }

.card-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 16px; }

.weather-card .city-name { font-size: 16px; font-weight: bold; }
.weather-card .time { float: right; font-size: 12px; color: #999; }

.card-body { display: flex; justify-content: space-between; align-items: center; }

.temp-area { display: flex; flex-direction: column; align-items: flex-start; }
.temp { font-size: 42px; font-weight: bold; color: #409EFF; line-height: 1.2; }
.text { font-size: 16px; color: #666; margin-top: 4px; }

.details { display: flex; flex-direction: column; gap: 8px; }
.detail-item { display: flex; gap: 12px; justify-content: space-between; min-width: 60px; }
.detail-item .label { color: #999; font-size: 13px; }
.detail-item .val { font-weight: bold; color: #333; }

.no-data { text-align: center; color: #999; padding: 20px 0; }
</style>
