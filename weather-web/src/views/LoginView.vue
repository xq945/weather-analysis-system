<template>
  <div class="login-body">
    <div class="login-hero">
      <div class="hero-overlay">
        <h1 class="hero-title">天气数据分析系统</h1>
      </div>
    </div>
    <div class="login-panel">
      <div class="login-card">
        <h2 class="card-title">欢迎使用</h2>

        <el-tabs v-model="activeTab" class="tabs">
          <el-tab-pane label="登录" name="login">
            <el-form :model="loginForm" :rules="loginRules" ref="loginFormRef" @keyup.enter="handleLogin" size="small">
              <el-form-item prop="username">
                <el-input v-model="loginForm.username" placeholder="请输入用户名">
                  <template #prefix>
                    <svg class="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                      <circle cx="12" cy="8" r="4"/><path d="M4 20c0-4 3.6-7 8-7s8 3 8 7"/>
                    </svg>
                  </template>
                </el-input>
              </el-form-item>
              <el-form-item prop="password">
                <el-input v-model="loginForm.password" type="password" placeholder="请输入密码" show-password>
                  <template #prefix>
                    <svg class="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                      <rect x="3" y="11" width="18" height="10" rx="2"/><circle cx="12" cy="16" r="1"/><path d="M12 14v2"/>
                    </svg>
                  </template>
                </el-input>
              </el-form-item>
              <div class="form-extra">
                <el-checkbox v-model="rememberMe" size="small">记住密码</el-checkbox>
                <span class="forgot-link" @click="handleForgot">忘记密码？</span>
              </div>
              <el-form-item>
                <el-button type="primary" class="submit-btn" @click="handleLogin" :loading="loading" size="small">
                  {{ loading ? '登录中...' : '登 录' }}
                </el-button>
              </el-form-item>
            </el-form>
          </el-tab-pane>

          <el-tab-pane label="注册" name="register">
            <el-form :model="regForm" :rules="regRules" ref="regFormRef" @keyup.enter="handleRegister" size="small">
              <el-form-item prop="username">
                <el-input v-model="regForm.username" placeholder="请输入用户名（2-50个字符）">
                  <template #prefix>
                    <svg class="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                      <circle cx="12" cy="8" r="4"/><path d="M4 20c0-4 3.6-7 8-7s8 3 8 7"/>
                    </svg>
                  </template>
                </el-input>
              </el-form-item>
              <el-form-item prop="nickname">
                <el-input v-model="regForm.nickname" placeholder="昵称（选填）">
                  <template #prefix>
                    <svg class="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                      <circle cx="12" cy="8" r="4"/><path d="M12 12c-4 0-7 2.7-7 6h14c0-3.3-3-6-7-6z"/><rect x="8" y="2" width="8" height="3" rx="1"/>
                    </svg>
                  </template>
                </el-input>
              </el-form-item>
              <el-form-item prop="password">
                <el-input v-model="regForm.password" type="password" placeholder="请输入密码（至少4个字符）" show-password>
                  <template #prefix>
                    <svg class="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                      <rect x="3" y="11" width="18" height="10" rx="2"/><circle cx="12" cy="16" r="1"/><path d="M12 14v2"/>
                    </svg>
                  </template>
                </el-input>
              </el-form-item>
              <el-form-item prop="confirmPassword">
                <el-input v-model="regForm.confirmPassword" type="password" placeholder="请确认密码" show-password>
                  <template #prefix>
                    <svg class="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                      <rect x="3" y="11" width="18" height="10" rx="2"/><circle cx="12" cy="16" r="1"/><path d="M12 14v2"/>
                    </svg>
                  </template>
                </el-input>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" class="submit-btn register-btn" @click="handleRegister" :loading="loading" size="small">
                  {{ loading ? '注册中...' : '注 册' }}
                </el-button>
              </el-form-item>
            </el-form>
          </el-tab-pane>
        </el-tabs>

        <div class="card-footer">
          <span class="version">v1.0.0</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const activeTab = ref('login')
const loading = ref(false)
const rememberMe = ref(false)

const loginForm = reactive({ username: '', password: '' })
const loginRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}
const loginFormRef = ref()

const regForm = reactive({ username: '', nickname: '', password: '', confirmPassword: '' })
const validateConfirm = (_rule: any, value: string, callback: any) => {
  if (value !== regForm.password) {
    callback(new Error('两次密码不一致'))
  } else {
    callback()
  }
}
const regRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 50, message: '用户名长度2-50个字符', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 4, message: '密码至少4个字符', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' },
  ],
}
const regFormRef = ref()

async function handleLogin() {
  const valid = await loginFormRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await authStore.login(loginForm.username, loginForm.password)
    if (rememberMe.value) {
      localStorage.setItem('saved_username', loginForm.username)
    } else {
      localStorage.removeItem('saved_username')
    }
    ElMessage.success('登录成功')
    router.push('/dashboard')
  } catch {
    // error handled by interceptor
  } finally {
    loading.value = false
  }
}

async function handleRegister() {
  const valid = await regFormRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await authStore.register(regForm.username, regForm.password, regForm.nickname || regForm.username)
    ElMessage.success('注册成功，已自动登录')
    router.push('/dashboard')
  } catch {
    // error handled by interceptor
  } finally {
    loading.value = false
  }
}

function handleForgot() {
  ElMessage.info('请联系管理员重置密码')
}
</script>

<style scoped>
.login-body {
  display: flex;
  position: fixed;
  inset: 0;
  overflow: hidden;
}

/* ===== 左侧插画 ===== */
.login-hero {
  flex: 0 0 60%;
  height: 100%;
  background: url('/background.png') center / cover no-repeat;
  position: relative;
}

.hero-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.25);
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding-top: 20vh;
}

.hero-title {
  text-align: center;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-size: 36px;
  font-weight: 700;
  margin: 0;
  color: #fff;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
  letter-spacing: 2px;
}

/* ===== 右侧登录面板 ===== */
.login-panel {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1.5rem;
  background-color: #f8fafc;
  overflow-y: auto;
}

.login-card {
  width: 400px;
  max-width: 100%;
  padding: 28px 32px 20px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06), 0 8px 24px rgba(0, 0, 0, 0.04);
}

.card-title {
  text-align: center;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-size: 18px;
  font-weight: 600;
  color: #2c3e50;
  margin: 0 0 16px;
}

.tabs :deep(.el-tabs__header) {
  margin-bottom: 12px;
}

.tabs :deep(.el-form-item) {
  margin-bottom: 14px;
}

.tabs :deep(.el-tabs__nav-wrap::after) {
  height: 1px;
  background: #ebeef5;
}

.tabs :deep(.el-tabs__item) {
  font-size: 14px;
  color: #909399;
  padding: 0 16px;
  height: 36px;
  line-height: 36px;
  transition: color 0.2s;
}

.tabs :deep(.el-tabs__item.is-active) {
  color: #409eff;
  font-weight: 600;
}

.tabs :deep(.el-tabs__active-bar) {
  height: 2px;
  border-radius: 2px;
  background: #409eff;
}

.input-icon {
  width: 16px;
  height: 16px;
  color: #c0c4cc;
  transition: color 0.2s;
}

:deep(.el-input.is-focus) .input-icon {
  color: #409eff;
}

.form-extra {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  font-size: 12px;
}

.forgot-link {
  color: #909399;
  cursor: pointer;
  transition: color 0.2s;
}

.forgot-link:hover {
  color: #409eff;
}

.submit-btn {
  width: 100%;
  height: 36px;
  font-size: 14px;
  letter-spacing: 3px;
  border-radius: 6px;
  transition: all 0.2s;
}

.submit-btn:not(.is-loading):hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.35);
}

.submit-btn:not(.is-loading):active {
  transform: translateY(0);
}

.register-btn {
  --el-button-bg-color: #67c23a;
  --el-button-border-color: #67c23a;
  --el-button-hover-bg-color: #7bcb4a;
  --el-button-hover-border-color: #7bcb4a;
}

.register-btn:not(.is-loading):hover {
  box-shadow: 0 4px 12px rgba(103, 194, 58, 0.35);
}

.card-footer {
  text-align: center;
  margin-top: 8px;
  font-size: 11px;
  color: #c0c4cc;
}

.version {
  color: #c0c4cc;
}

/* ===== 响应式 ===== */
@media (max-width: 768px) {
  .login-hero {
    display: none;
  }

  .login-panel {
    padding: 1rem;
  }

  .login-card {
    padding: 28px 24px 20px;
  }

  .hero-title {
    font-size: 24px;
  }
}
</style>
