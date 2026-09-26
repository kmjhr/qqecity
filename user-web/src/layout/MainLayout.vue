<template>
  <el-container class="main-layout">
    <!-- 顶部导航栏 -->
    <el-header class="header">
      <div class="header-inner">
        <div class="logo" @click="$router.push('/home')">
          <el-icon :size="24" color="#409EFF"><OfficeBuilding /></el-icon>
          <span class="logo-text">青启e城</span>
        </div>

        <el-menu
          mode="horizontal"
          :default-active="$route.path"
          router
          class="nav-menu"
          background-color="transparent"
          text-color="#333"
          active-text-color="#409EFF"
        >
          <el-menu-item index="/home">
            <el-icon><HomeFilled /></el-icon>
            <span>首页</span>
          </el-menu-item>
          <el-menu-item index="/guarantee">
            <el-icon><Wallet /></el-icon>
            <span>安居保函</span>
          </el-menu-item>
          <el-menu-item index="/loan">
            <el-icon><Money /></el-icon>
            <span>青创e贷</span>
          </el-menu-item>
          <el-menu-item index="/budget">
            <el-icon><PieChart /></el-icon>
            <span>预算消费</span>
          </el-menu-item>
          <el-menu-item index="/safety">
            <el-icon><Shield /></el-icon>
            <span>金融安全</span>
          </el-menu-item>
        </el-menu>

        <div class="user-area">
          <el-dropdown @command="handleCommand">
            <span class="user-name">
              <el-avatar :size="28" :src="userStore.userInfo?.avatar">
                {{ userStore.username?.charAt(0) || 'U' }}
              </el-avatar>
              <span class="name-text">{{ userStore.username || '用户' }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="message">
                  <el-icon><Bell /></el-icon>消息中心
                </el-dropdown-item>
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon>个人中心
                </el-dropdown-item>
                <el-dropdown-item command="logout" divided>
                  <el-icon><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </el-header>

    <!-- 主内容区 -->
    <el-main class="main-content">
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </el-main>

    <!-- 页脚 -->
    <el-footer class="footer">
      <p>© 2024 青启e城 · 演示系统</p>
    </el-footer>
  </el-container>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()

function handleCommand(command) {
  if (command === 'message') {
    router.push('/message')
  } else if (command === 'profile') {
    router.push('/profile')
  } else if (command === 'logout') {
    ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(async () => {
      await userStore.logout()
      ElMessage.success('已退出登录')
      router.push('/login')
    }).catch(() => {})
  }
}
</script>

<style scoped>
.main-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.header {
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  padding: 0;
  height: 64px;
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-inner {
  max-width: 1200px;
  margin: 0 auto;
  height: 100%;
  display: flex;
  align-items: center;
  padding: 0 24px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  margin-right: 40px;
}

.logo-text {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.nav-menu {
  flex: 1;
  border-bottom: none;
}

.user-area {
  margin-left: auto;
}

.user-name {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  color: #606266;
}

.name-text {
  font-size: 14px;
}

.main-content {
  flex: 1;
  background: #f5f7fa;
  padding: 24px;
}

.footer {
  background: #fff;
  text-align: center;
  color: #909399;
  font-size: 13px;
  border-top: 1px solid #ebeef5;
  height: 50px;
  line-height: 50px;
  padding: 0;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
