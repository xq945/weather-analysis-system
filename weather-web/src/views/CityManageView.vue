<template>
  <div class="city-manage">
    <el-card>
      <template #header>
        <span>城市管理</span>
      </template>

      <div class="add-row">
        <el-input v-model="cityInput" placeholder="输入城市名，如：北京" size="large" style="width:300px"
          @keyup.enter="handleAdd" />
        <el-button type="primary" size="large" @click="handleAdd" :loading="adding">
          添加关注
        </el-button>
      </div>

      <el-table :data="cities" style="width:100%;margin-top:20px" v-loading="loading" empty-text="暂无关注城市，请在上方添加">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="city" label="城市名" />
        <el-table-column prop="createdAt" label="添加时间" />
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button type="danger" size="small" @click="handleRemove(row)">
              取消关注
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getCities, addCity, removeCity } from '../api/city'

const cityInput = ref('')
const cities = ref<any[]>([])
const loading = ref(false)
const adding = ref(false)

async function loadCities() {
  loading.value = true
  try {
    const res = await getCities()
    cities.value = res.data.data
  } catch {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}

async function handleAdd() {
  const city = cityInput.value.trim()
  if (!city) {
    ElMessage.warning('请输入城市名')
    return
  }
  adding.value = true
  try {
    const res = await addCity(city)
    const d = res.data.data
    ElMessage.success(`「${d.city}」添加成功，Location ID: ${d.cityCode}`)
    cityInput.value = ''
    await loadCities()
  } catch (err: any) {
    const msg = err?.response?.data?.message || '添加失败'
    ElMessage.error(msg)
  } finally {
    adding.value = false
  }
}

async function handleRemove(row: any) {
  try {
    await ElMessageBox.confirm(`确定取消关注「${row.city}」？`, '确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  try {
    await removeCity(row.id)
    ElMessage.success('已取消关注')
    await loadCities()
  } catch {
    // handled by interceptor
  }
}

onMounted(loadCities)
</script>

<style scoped>
.add-row {
  display: flex;
  gap: 12px;
  align-items: center;
}
</style>
