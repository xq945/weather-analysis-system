<template>
  <div class="weather-page">
    <div class="toolbar">
      <div class="toolbar-left">
        <el-select v-model="selectedCity" placeholder="选择城市" size="large" style="width:200px">
          <el-option v-for="c in citiesStore.activeList" :key="c.city" :label="c.city" :value="c.city" />
        </el-select>
        <el-button
          type="success" size="large"
          @click="handleFetchCity"
          :loading="fetchingCity"
          :disabled="!selectedCity"
        >
          拉取当前城市
        </el-button>
      </div>
      <div class="toolbar-right">
        <el-button type="primary" size="large" @click="handleFetch" :loading="fetching">
          拉取全部城市
        </el-button>
        <span v-if="lastFetch" class="last-fetch">上次拉取：{{ lastFetch }}</span>
      </div>
    </div>

    <div v-if="!selectedCity">
      <el-empty description="请选择城市后查看天气" />
    </div>

    <template v-if="selectedCity">
      <!-- 实时天气卡片 -->
      <el-card class="now-card" v-if="nowData">
        <template #header>
          <span>{{ selectedCity }} · 实时天气</span>
        </template>
        <el-row :gutter="16">
          <el-col :span="6">
            <div class="metric"><span class="value large">{{ nowData.temp }}°C</span><span class="label">温度</span></div>
          </el-col>
          <el-col :span="6">
            <div class="metric"><span class="value large">{{ nowData.feelsLike }}°C</span><span class="label">体感温度</span></div>
          </el-col>
          <el-col :span="6">
            <div class="metric"><span class="value">{{ nowData.humidity }}%</span><span class="label">湿度</span></div>
          </el-col>
          <el-col :span="6">
            <div class="metric"><span class="value">{{ nowData.windSpeed }} km/h</span><span class="label">风速</span></div>
          </el-col>
        </el-row>
        <el-row :gutter="16" style="margin-top:16px">
          <el-col :span="6">
            <div class="metric"><span class="value">{{ nowData.pressure }} hPa</span><span class="label">气压</span></div>
          </el-col>
          <el-col :span="6">
            <div class="metric"><span class="value">{{ nowData.visibility }} km</span><span class="label">能见度</span></div>
          </el-col>
          <el-col :span="12">
            <div class="metric"><span class="value">{{ nowData.weatherText }}</span><span class="label">天气状况</span></div>
          </el-col>
        </el-row>
        <div class="obs-time">观测时间：{{ nowData.obsTime }}</div>
      </el-card>

      <!-- 7天预报 -->
      <el-card class="forecast-card" v-if="forecastData.length > 0">
        <template #header>
          <span>{{ selectedCity }} · 7天预报</span>
        </template>
        <el-table :data="forecastData" stripe>
          <el-table-column prop="forecastDate" label="日期" width="120" />
          <el-table-column prop="tempMax" label="最高温" width="100">
            <template #default="{ row }">{{ row.tempMax }}°C</template>
          </el-table-column>
          <el-table-column prop="tempMin" label="最低温" width="100">
            <template #default="{ row }">{{ row.tempMin }}°C</template>
          </el-table-column>
          <el-table-column prop="weatherTextDay" label="白天天气" width="120" />
          <el-table-column prop="weatherTextNight" label="夜间天气" width="120" />
          <el-table-column prop="humidity" label="湿度" width="80">
            <template #default="{ row }">{{ row.humidity }}%</template>
          </el-table-column>
          <el-table-column prop="windSpeed" label="风速" width="100">
            <template #default="{ row }">{{ row.windSpeed }} km/h</template>
          </el-table-column>
        </el-table>
      </el-card>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useCitiesStore } from '../stores/cities'
import { fetchWeather, fetchCityWeather, getWeatherNow, getForecast } from '../api/weather'

const citiesStore = useCitiesStore()
const selectedCity = ref('')
const fetching = ref(false)
const fetchingCity = ref(false)
const lastFetch = ref('')

const nowData = ref<any>(null)
const forecastData = ref<any[]>([])

onMounted(async () => {
  if (citiesStore.allList.length === 0) {
    await citiesStore.load()
  }
})

watch(selectedCity, async (city) => {
  if (!city) {
    nowData.value = null
    forecastData.value = []
    return
  }
  try {
    const [nowRes, fcRes] = await Promise.all([
      getWeatherNow(city),
      getForecast(city)
    ])
    nowData.value = nowRes.data.data
    forecastData.value = fcRes.data.data || []
  } catch { /* ignore */ }
})

async function handleFetch() {
  fetching.value = true
  try {
    const res = await fetchWeather()
    const d = res.data.data
    ElMessage.success(`拉取完成：${d.nowFetched} 个城市实时天气，${d.forecastFetched} 条预报`)
    lastFetch.value = new Date().toLocaleString()
    if (d.errors && d.errors.length > 0) {
      d.errors.forEach((e: string) => ElMessage.warning(e))
    }
    if (selectedCity.value) {
      const [nowRes, fcRes] = await Promise.all([
        getWeatherNow(selectedCity.value),
        getForecast(selectedCity.value)
      ])
      nowData.value = nowRes.data.data
      forecastData.value = fcRes.data.data || []
    }
  } catch { /* ignore */ }
  finally { fetching.value = false }
}

async function handleFetchCity() {
  if (!selectedCity.value) return
  fetchingCity.value = true
  try {
    const res = await fetchCityWeather(selectedCity.value)
    const d = res.data.data
    ElMessage.success(`${selectedCity.value} 拉取完成：${d.nowFetched} 条实时，${d.forecastFetched} 条预报`)
    lastFetch.value = new Date().toLocaleString()
    if (d.errors && d.errors.length > 0) {
      d.errors.forEach((e: string) => ElMessage.warning(e))
    }
    // 刷新当前城市数据
    const [nowRes, fcRes] = await Promise.all([
      getWeatherNow(selectedCity.value),
      getForecast(selectedCity.value)
    ])
    nowData.value = nowRes.data.data
    forecastData.value = fcRes.data.data || []
  } catch { /* ignore */ }
  finally { fetchingCity.value = false }
}
</script>

<style scoped>
.weather-page { display: flex; flex-direction: column; gap: 20px; }

.toolbar { display: flex; align-items: center; justify-content: space-between; }
.toolbar-left { display: flex; align-items: center; gap: 12px; }
.toolbar-right { display: flex; align-items: center; gap: 12px; }

.last-fetch { color: #999; font-size: 13px; }

.now-card { margin-top: 0; }

.metric { display: flex; flex-direction: column; align-items: center; padding: 12px 0; }
.metric .value { font-size: 18px; font-weight: bold; color: #333; }
.metric .value.large { font-size: 28px; color: #409EFF; }
.metric .label { margin-top: 4px; font-size: 13px; color: #999; }

.obs-time { margin-top: 12px; text-align: right; color: #999; font-size: 13px; }

.forecast-card { margin-top: 0; }
</style>
