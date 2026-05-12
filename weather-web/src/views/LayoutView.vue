<template>
  <div class="layout">
    <div class="sidebar">
      <AppSidebar />
    </div>
    <div class="main">
      <div class="header">
        <div class="header-brand">
          <svg class="header-icon" viewBox="0 0 32 32" fill="none">
            <ellipse cx="18" cy="10" rx="6" ry="5" fill="#FFD93D"/>
            <circle cx="18" cy="9" r="8" fill="#FFD93D" opacity="0.25"/>
            <path d="M4 22a4 3 0 0 1 8 0H4z" fill="#90B4CE"/>
            <path d="M18 24a5 3.5 0 0 1 10 0H18z" fill="#90B4CE"/>
          </svg>
          <span class="header-title">天气数据分析系统</span>
        </div>
        <div class="header-right">
          <span class="filter-label">城市</span>
          <el-select v-model="citiesStore.filterMode" size="small" style="width:140px">
            <el-option value="all" label="所有城市" />
            <el-option value="mine" label="我的关注" />
          </el-select>
          <span class="nickname">{{ authStore.nickname }}</span>
          <el-button type="danger" size="small" @click="handleLogout">退出</el-button>
        </div>
      </div>
      <div class="content">
        <router-view />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useCitiesStore } from '../stores/cities'
import AppSidebar from '../components/AppSidebar.vue'

const router = useRouter()
const authStore = useAuthStore()
const citiesStore = useCitiesStore()

onMounted(() => {
  citiesStore.load()
})

function handleLogout() {
  authStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.layout {
  display: flex;
  position: fixed;
  inset: 0;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

/* ===== 侧边栏 ===== */
.sidebar {
  width: 220px;
  background: linear-gradient(180deg, #2c3e50 0%, #304156 100%);
  flex-shrink: 0;
}

/* ===== 主区域 ===== */
.main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* ===== 顶栏 ===== */
.header {
  height: 64px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  flex-shrink: 0;
  z-index: 10;
}

.header-brand {
  display: flex;
  align-items: center;
  gap: 10px;
}

.header-icon {
  width: 30px;
  height: 30px;
  flex-shrink: 0;
}

.header-title {
  font-size: 17px;
  font-weight: 700;
  color: #2c3e50;
  letter-spacing: 0.5px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 14px;
}

.filter-label {
  color: #909399;
  font-size: 13px;
}

.nickname {
  color: #606266;
  font-size: 14px;
}

/* ===== 内容区 ===== */
.content {
  flex: 1;
  padding: 24px;
  background: #f0f2f5;
  overflow-y: auto;
}

/* ===== 响应式 ===== */
@media (max-width: 768px) {
  .sidebar {
    width: 64px;
  }

  .header {
    padding: 0 16px;
  }

  .header-title {
    font-size: 15px;
  }

  .content {
    padding: 16px;
  }
}
</style>
