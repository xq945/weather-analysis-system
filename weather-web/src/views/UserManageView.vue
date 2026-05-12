<template>
  <div class="user-manage">
    <el-card>
      <template #header>
        <span>用户管理</span>
      </template>

      <el-table :data="users" style="width:100%" v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" />
        <el-table-column prop="nickname" label="昵称" />
        <el-table-column label="权限" width="100">
          <template #default="{ row }">
            <el-tag :type="row.permission === 2 ? 'danger' : 'info'" size="small">
              {{ row.permission === 2 ? '管理员' : '普通用户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="注册时间" width="180" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 1"
              type="warning" size="small"
              @click="handleToggleStatus(row)"
            >
              禁用
            </el-button>
            <el-button
              v-else
              type="success" size="small"
              @click="handleToggleStatus(row)"
            >
              启用
            </el-button>
            <el-button
              type="primary" size="small"
              @click="handleTogglePermission(row)"
            >
              {{ row.permission === 2 ? '降为普通' : '升为管理' }}
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
import { getUsers, updateUserStatus, updateUserPermission } from '../api/user'

const users = ref<any[]>([])
const loading = ref(false)

async function loadUsers() {
  loading.value = true
  try {
    const res = await getUsers()
    users.value = res.data.data || []
  } catch {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}

async function handleToggleStatus(row: any) {
  const newStatus = row.status === 1 ? 0 : 1
  const action = newStatus === 0 ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(`确定${action}用户「${row.username}」？`, '确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }
  try {
    await updateUserStatus(row.id, newStatus)
    ElMessage.success(`已${action}`)
    await loadUsers()
  } catch {
    // handled by interceptor
  }
}

async function handleTogglePermission(row: any) {
  const newPerm = row.permission === 2 ? 1 : 2
  const action = newPerm === 2 ? '升级为管理员' : '降级为普通用户'
  try {
    await ElMessageBox.confirm(`确定将「${row.username}」${action}？`, '确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }
  try {
    await updateUserPermission(row.id, newPerm)
    ElMessage.success('权限已更新')
    await loadUsers()
  } catch {
    // handled by interceptor
  }
}

onMounted(loadUsers)
</script>
