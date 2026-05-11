<template>
  <el-menu :default-active="activeMenu" router background-color="#304156" text-color="#bfcbd9"
    active-text-color="#409EFF" class="sidebar-menu">
    <div class="logo">
      <span>🌤 天气分析</span>
    </div>
    <el-menu-item index="/dashboard">
      <el-icon><HomeFilled /></el-icon>
      <span>首页仪表盘</span>
    </el-menu-item>
    <el-menu-item index="/cities">
      <el-icon><Location /></el-icon>
      <span>城市管理</span>
    </el-menu-item>
    <el-menu-item index="/weather" disabled>
      <el-icon><Cloudy /></el-icon>
      <span>天气数据</span>
      <el-tag size="small" style="margin-left:8px">开发中</el-tag>
    </el-menu-item>
  </el-menu>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const activeMenu = computed(() => route.path)
</script>

<style scoped>
.sidebar-menu {
  height: 100%;
  border-right: none;
}

.logo {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 18px;
  font-weight: bold;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}
</style>
