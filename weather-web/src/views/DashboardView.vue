<template>
  <div class="dashboard">
    <el-card class="welcome-card">
      <h2>欢迎使用天气数据分析系统</h2>
      <p>本系统用于查看和分析天气数据，支持多城市管理和历史数据查询。</p>
    </el-card>

    <el-row :gutter="20" style="margin-top:20px">
      <el-col :span="8">
        <el-card class="stat-card">
          <div class="stat-num">{{ cityCount }}</div>
          <div class="stat-label">已关注城市</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="stat-card">
          <div class="stat-num">0</div>
          <div class="stat-label">天气记录数</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="stat-card">
          <div class="stat-num">-</div>
          <div class="stat-label">数据更新时间</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card style="margin-top:20px">
      <template #header>
        <span>快速上手</span>
      </template>
      <el-steps :active="0" align-center>
        <el-step title="添加城市" description="在「城市管理」中添加关注城市" />
        <el-step title="对接数据" description="配置和风天气 API 密钥，拉取天气数据" />
        <el-step title="查看分析" description="查看天气详情图表和数据分析" />
      </el-steps>
      <div style="text-align:center;margin-top:20px">
        <el-tag type="info">提示：请先在「城市管理」中添加关注城市，数据对接后此处将展示天气概览</el-tag>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getCities } from '../api/city'

const cityCount = ref(0)

onMounted(async () => {
  try {
    const res = await getCities()
    cityCount.value = res.data.data.length
  } catch {
    // ignore
  }
})
</script>

<style scoped>
.welcome-card h2 {
  margin: 0 0 8px;
  color: #333;
}

.welcome-card p {
  margin: 0;
  color: #666;
}

.stat-card {
  text-align: center;
}

.stat-num {
  font-size: 32px;
  font-weight: bold;
  color: #409EFF;
}

.stat-label {
  margin-top: 8px;
  color: #999;
  font-size: 14px;
}
</style>
