<template>
  <div class="sidebar-wrapper">
    <el-menu :default-active="activeMenu" router background-color="#304156" text-color="#bfcbd9"
      active-text-color="#409EFF" class="sidebar-menu">
      <div class="logo">
        <span>天气分析</span>
      </div>
      <el-menu-item index="/dashboard">
        <el-icon><HomeFilled /></el-icon>
        <span>首页仪表盘</span>
      </el-menu-item>
      <el-menu-item index="/cities">
        <el-icon><Location /></el-icon>
        <span>城市管理</span>
      </el-menu-item>
      <el-menu-item index="/weather">
        <el-icon><Cloudy /></el-icon>
        <span>天气数据</span>
      </el-menu-item>
      <el-menu-item index="/analysis">
        <el-icon><DataAnalysis /></el-icon>
        <span>数据分析</span>
      </el-menu-item>
      <el-menu-item index="/assistant">
        <el-icon><ChatDotRound /></el-icon>
        <span>天气助手</span>
      </el-menu-item>
      <el-menu-item v-if="authStore.permission === 2" index="/users">
        <el-icon><Setting /></el-icon>
        <span>用户管理</span>
      </el-menu-item>
    </el-menu>
    <a class="source-link" href="https://www.qweather.com/" target="_blank">数据来源</a>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const authStore = useAuthStore()
const activeMenu = computed(() => route.path)
</script>

<style scoped>
.sidebar-wrapper {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.sidebar-menu {
  flex: 1;
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

.source-link {
  display: block;
  height: 48px;
  line-height: 48px;
  text-align: center;
  color: #74889c;
  font-size: 13px;
  text-decoration: none;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  transition: color 0.2s;
}

.source-link:hover {
  color: #bfcbd9;
}
</style>
