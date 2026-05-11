<template>
  <div class="analysis">
    <!-- 模式切换 -->
    <div class="top-bar">
      <el-radio-group v-model="mode" @change="handleModeChange">
        <el-radio-button value="single">单城市分析</el-radio-button>
        <el-radio-button value="compare">双城市对比</el-radio-button>
      </el-radio-group>
    </div>

    <!-- 筛选区 -->
    <div class="filters">
      <template v-if="mode === 'single'">
        <span class="label">城市</span>
        <el-select v-model="city" placeholder="选择城市" style="width:180px" @change="loadSingle">
          <el-option v-for="c in cities" :key="c.city" :label="c.city" :value="c.city" />
        </el-select>
      </template>
      <template v-else>
        <span class="label">城市A</span>
        <el-select v-model="cityA" placeholder="选择城市" style="width:180px" @change="loadCompare">
          <el-option v-for="c in cities" :key="c.city" :label="c.city" :value="c.city" />
        </el-select>
        <span class="label">城市B</span>
        <el-select v-model="cityB" placeholder="选择城市" style="width:180px" @change="loadCompare">
          <el-option v-for="c in cities" :key="c.city" :label="c.city" :value="c.city"
            :disabled="c.city === cityA" />
        </el-select>
      </template>
      <span class="label">时间范围</span>
      <el-select v-model="days" style="width:120px" @change="handleDaysChange">
        <el-option :value="7" label="近7天" />
        <el-option :value="14" label="近14天" />
        <el-option :value="30" label="近30天" />
      </el-select>
    </div>

    <div v-if="!city && mode === 'single'" class="tip">
      <el-empty description="请选择城市开始分析" />
    </div>
    <div v-else-if="mode === 'compare' && (!cityA || !cityB)" class="tip">
      <el-empty description="请选择两个城市进行对比" />
    </div>

    <!-- 单城市分析 -->
    <template v-if="mode === 'single' && stats">
      <div class="stat-row">
        <div class="stat-item"><span class="stat-num">{{ stats.avgTemp ?? '-' }}°C</span><span class="stat-label">平均温度</span></div>
        <div class="stat-item"><span class="stat-num">{{ stats.maxTemp ?? '-' }}°C</span><span class="stat-label">最高温度</span></div>
        <div class="stat-item"><span class="stat-num">{{ stats.minTemp ?? '-' }}°C</span><span class="stat-label">最低温度</span></div>
        <div class="stat-item"><span class="stat-num">{{ stats.recordCount }}</span><span class="stat-label">数据条数</span></div>
      </div>
      <div class="chart-row">
        <div class="chart-box">
          <div class="chart-title">温度与体感温度趋势</div>
          <div ref="trendChart" class="chart"></div>
        </div>
        <div class="chart-box">
          <div class="chart-title">实况 vs 预报对比</div>
          <div ref="vsChart" class="chart"></div>
        </div>
      </div>
      <div class="chart-row">
        <div class="chart-box">
          <div class="chart-title">湿度变化</div>
          <div ref="humidityChart" class="chart"></div>
        </div>
        <div class="chart-box">
          <div class="chart-title">风速变化</div>
          <div ref="windChart" class="chart"></div>
        </div>
      </div>
    </template>

    <!-- 双城市对比 -->
    <template v-if="mode === 'compare' && compare">
      <div class="stat-row">
        <div class="stat-item"><span class="stat-num">{{ compare.cityA.avgTemp ?? '-' }}°C</span><span class="stat-label">{{ compare.cityA.name }} 均温</span></div>
        <div class="stat-item"><span class="stat-num">{{ compare.cityB.avgTemp ?? '-' }}°C</span><span class="stat-label">{{ compare.cityB.name }} 均温</span></div>
        <div class="stat-item">
          <span class="stat-num" :style="{ color: diffVal > 0 ? '#f56c6c' : diffVal < 0 ? '#409EFF' : '#999' }">{{ diffVal > 0 ? '+' : '' }}{{ diffVal }}°C</span>
          <span class="stat-label">温差对比</span>
        </div>
        <div class="stat-item"><span class="stat-num">{{ (compare.cityA.recordCount || 0) + (compare.cityB.recordCount || 0) }}</span><span class="stat-label">总数据量</span></div>
      </div>
      <div class="chart-row">
        <div class="chart-box">
          <div class="chart-title">温度对比</div>
          <div ref="compTempChart" class="chart"></div>
        </div>
        <div class="chart-box">
          <div class="chart-title">温度差值趋势</div>
          <div ref="compDiffChart" class="chart"></div>
        </div>
      </div>
      <div class="chart-row">
        <div class="chart-box">
          <div class="chart-title">湿度对比</div>
          <div ref="compHumidityChart" class="chart"></div>
        </div>
        <div class="chart-box">
          <div class="chart-title">风速对比</div>
          <div ref="compWindChart" class="chart"></div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick, watch } from 'vue'
import * as echarts from 'echarts'
import { getCities } from '../api/city'
import { getStatistics, getCompare } from '../api/weather'

const mode = ref<'single' | 'compare'>('single')
const cities = ref<any[]>([])
const city = ref('')
const cityA = ref('')
const cityB = ref('')
const days = ref(7)
const stats = ref<any>(null)
const compare = ref<any>(null)

const trendChart = ref<HTMLDivElement>()
const vsChart = ref<HTMLDivElement>()
const humidityChart = ref<HTMLDivElement>()
const windChart = ref<HTMLDivElement>()
const compTempChart = ref<HTMLDivElement>()
const compDiffChart = ref<HTMLDivElement>()
const compHumidityChart = ref<HTMLDivElement>()
const compWindChart = ref<HTMLDivElement>()

const diffVal = computed(() => {
  if (!compare.value) return 0
  const a = compare.value.cityA.avgTemp
  const b = compare.value.cityB.avgTemp
  if (a == null || b == null) return 0
  return Math.round((a - b) * 10) / 10
})

onMounted(async () => {
  const res = await getCities()
  cities.value = res.data.data || []
})

function handleModeChange() { stats.value = null; compare.value = null }
function handleDaysChange() { mode.value === 'single' ? loadSingle() : loadCompare() }

async function loadSingle() {
  if (!city.value) return
  const res = await getStatistics(city.value, days.value)
  stats.value = res.data.data
  await nextTick()
  renderSingleCharts()
}

async function loadCompare() {
  if (!cityA.value || !cityB.value) return
  const res = await getCompare(cityA.value, cityB.value, days.value)
  compare.value = res.data.data
  await nextTick()
  renderCompareCharts()
}

function renderSingleCharts() {
  if (!stats.value) return
  const data = stats.value
  const times = (data.trend || []).map((d: any) => d.time)
  const fmt = (v: any) => v != null ? Number(Number(v).toFixed(1)) : null

  renderChart(trendChart.value!, {
    tooltip: { trigger: 'axis' },
    legend: { data: ['温度', '体感温度'], bottom: 0 },
    grid: { left: 50, right: 20, top: 10, bottom: 30 },
    xAxis: { type: 'category', data: times, axisLabel: { rotate: 45, fontSize: 10 } },
    yAxis: { type: 'value', name: '°C' },
    series: [
      { name: '温度', type: 'line', data: (data.trend || []).map((d: any) => fmt(d.temp)), smooth: true, symbol: 'none' },
      { name: '体感温度', type: 'line', data: (data.trend || []).map((d: any) => fmt(d.feelsLike)), smooth: true, symbol: 'none' }
    ]
  })

  const daily = data.dailySummary || []
  const forecast = data.forecast || []
  renderChart(vsChart.value!, {
    tooltip: { trigger: 'axis' },
    legend: { data: ['实际最高', '实际最低', '预报最高', '预报最低'], bottom: 0 },
    grid: { left: 50, right: 20, top: 10, bottom: 30 },
    xAxis: { type: 'category', data: daily.map((d: any) => d.date), axisLabel: { rotate: 45, fontSize: 10 } },
    yAxis: { type: 'value', name: '°C' },
    series: [
      { name: '实际最高', type: 'line', data: daily.map((d: any) => d.maxTemp), symbol: 'circle', symbolSize: 6 },
      { name: '实际最低', type: 'line', data: daily.map((d: any) => d.minTemp), symbol: 'circle', symbolSize: 6 },
      { name: '预报最高', type: 'line', data: forecast.slice(0, daily.length).map((f: any) => f.tempMax), lineStyle: { type: 'dashed' }, symbol: 'none' },
      { name: '预报最低', type: 'line', data: forecast.slice(0, daily.length).map((f: any) => f.tempMin), lineStyle: { type: 'dashed' }, symbol: 'none' }
    ]
  })

  renderChart(humidityChart.value!, {
    tooltip: { trigger: 'axis' },
    grid: { left: 50, right: 20, top: 10, bottom: 30 },
    xAxis: { type: 'category', data: times, axisLabel: { rotate: 45, fontSize: 10 } },
    yAxis: { type: 'value', name: '%' },
    series: [{ type: 'line', data: (data.trend || []).map((d: any) => fmt(d.humidity)), smooth: true, symbol: 'none', color: '#67c23a', areaStyle: { color: 'rgba(103,194,58,0.1)' } }]
  })

  renderChart(windChart.value!, {
    tooltip: { trigger: 'axis' },
    grid: { left: 50, right: 20, top: 10, bottom: 30 },
    xAxis: { type: 'category', data: times, axisLabel: { rotate: 45, fontSize: 10 } },
    yAxis: { type: 'value', name: 'km/h' },
    series: [{ type: 'bar', data: (data.trend || []).map((d: any) => fmt(d.windSpeed)), color: '#e6a23c' }]
  })
}

function renderCompareCharts() {
  if (!compare.value) return
  const c = compare.value
  const aTrend = c.cityA.trend || []
  const bTrend = c.cityB.trend || []
  const times = aTrend.map((d: any) => d.time)
  const fmt = (v: any) => v != null ? Number(Number(v).toFixed(1)) : null

  renderChart(compTempChart.value!, {
    tooltip: { trigger: 'axis' },
    legend: { data: [c.cityA.name, c.cityB.name], bottom: 0 },
    grid: { left: 50, right: 20, top: 10, bottom: 30 },
    xAxis: { type: 'category', data: times, axisLabel: { rotate: 45, fontSize: 10 } },
    yAxis: { type: 'value', name: '°C' },
    series: [
      { name: c.cityA.name, type: 'line', data: aTrend.map((d: any) => fmt(d.temp)), smooth: true, symbol: 'none' },
      { name: c.cityB.name, type: 'line', data: bTrend.map((d: any) => fmt(d.temp)), smooth: true, symbol: 'none' }
    ]
  })

  const diff = c.diff || []
  renderChart(compDiffChart.value!, {
    tooltip: { trigger: 'axis' },
    grid: { left: 50, right: 20, top: 10, bottom: 30 },
    xAxis: { type: 'category', data: diff.map((d: any) => d.time), axisLabel: { rotate: 45, fontSize: 10 } },
    yAxis: { type: 'value', name: '°C' },
    series: [{
      type: 'line', data: diff.map((d: any) => d.tempDiff), smooth: true, symbol: 'none',
      areaStyle: { color: 'rgba(64,158,255,0.2)' },
      lineStyle: { color: '#409EFF' }
    }]
  })

  renderChart(compHumidityChart.value!, {
    tooltip: { trigger: 'axis' },
    legend: { data: [c.cityA.name, c.cityB.name], bottom: 0 },
    grid: { left: 50, right: 20, top: 10, bottom: 30 },
    xAxis: { type: 'category', data: times, axisLabel: { rotate: 45, fontSize: 10 } },
    yAxis: { type: 'value', name: '%' },
    series: [
      { name: c.cityA.name, type: 'line', data: aTrend.map((d: any) => fmt(d.humidity)), smooth: true, symbol: 'none' },
      { name: c.cityB.name, type: 'line', data: bTrend.map((d: any) => fmt(d.humidity)), smooth: true, symbol: 'none' }
    ]
  })

  renderChart(compWindChart.value!, {
    tooltip: { trigger: 'axis' },
    legend: { data: [c.cityA.name, c.cityB.name], bottom: 0 },
    grid: { left: 50, right: 20, top: 10, bottom: 30 },
    xAxis: { type: 'category', data: times, axisLabel: { rotate: 45, fontSize: 10 } },
    yAxis: { type: 'value', name: 'km/h' },
    series: [
      { name: c.cityA.name, type: 'bar', data: aTrend.map((d: any) => fmt(d.windSpeed)), barGap: '10%', itemStyle: { color: '#409EFF' } },
      { name: c.cityB.name, type: 'bar', data: bTrend.map((d: any) => fmt(d.windSpeed)), itemStyle: { color: '#e6a23c' } }
    ]
  })
}

function renderChart(dom: HTMLDivElement, option: any) {
  const instance = echarts.getInstanceByDom(dom)
  if (instance) instance.dispose()
  echarts.init(dom).setOption(option)
}
</script>

<style scoped>
.analysis { display: flex; flex-direction: column; gap: 16px; }

.top-bar { display: flex; align-items: center; }

.filters { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.filters .label { font-weight: bold; color: #333; }

.tip { padding: 60px 0; }

.stat-row { display: flex; gap: 16px; flex-wrap: wrap; }
.stat-item {
  flex: 1; min-width: 140px; background: #fff; border-radius: 8px; padding: 20px;
  text-align: center; box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}
.stat-num { display: block; font-size: 28px; font-weight: bold; color: #409EFF; }
.stat-label { display: block; margin-top: 6px; font-size: 13px; color: #999; }

.chart-row { display: flex; gap: 16px; }
.chart-box { flex: 1; background: #fff; border-radius: 8px; padding: 16px; box-shadow: 0 2px 8px rgba(0,0,0,0.06); }
.chart-title { font-size: 14px; font-weight: bold; color: #333; margin-bottom: 8px; }
.chart { width: 100%; height: 260px; }
</style>
