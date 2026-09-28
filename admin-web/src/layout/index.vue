<template>
  <el-container class="admin-layout">
    <!-- 侧边栏 -->
    <el-aside :width="isCollapse ? '64px' : '220px'" class="sidebar">
      <div class="logo">
        <el-icon :size="24" color="#fff"><Monitor /></el-icon>
        <span v-show="!isCollapse" class="logo-text">青启e城后台</span>
      </div>

      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapse"
        :collapse-transition="false"
        router
        background-color="#001529"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
        class="side-menu"
      >
        <template v-for="item in menuList" :key="item.path">
          <!-- 一级菜单 -->
          <el-menu-item v-if="!item.children" :index="item.path">
            <el-icon><component :is="item.icon" /></el-icon>
            <template #title>{{ item.title }}</template>
          </el-menu-item>
          <!-- 二级菜单 -->
          <el-sub-menu v-else :index="item.path">
            <template #title>
              <el-icon><component :is="item.icon" /></el-icon>
              <span>{{ item.title }}</span>
            </template>
            <el-menu-item
              v-for="child in item.children"
              :key="child.path"
              :index="child.path"
            >
              <el-icon><component :is="child.icon" /></el-icon>
              <template #title>{{ child.title }}</template>
            </el-menu-item>
          </el-sub-menu>
        </template>
      </el-menu>
    </el-aside>

    <!-- 右侧主体 -->
    <el-container>
      <!-- 顶部栏 -->
      <el-header class="header">
        <div class="header-left">
          <el-icon class="collapse-btn" :size="20" @click="toggleCollapse">
            <Fold v-if="!isCollapse" />
            <Expand v-else />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item v-for="item in breadcrumbs" :key="item">
              {{ item }}
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <div class="header-right">
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-avatar :size="32" :src="userStore.userInfo?.avatar">
                {{ userStore.username?.charAt(0) || 'A' }}
              </el-avatar>
              <span class="username">{{ userStore.username }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="home" divided>
                  <el-icon><HomeFilled /></el-icon>前往用户端
                </el-dropdown-item>
                <el-dropdown-item command="logout">
                  <el-icon><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 内容区 -->
      <el-main class="main-content">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/modules/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const isCollapse = ref(false)

function toggleCollapse() {
  isCollapse.value = !isCollapse.value
}

// 菜单配置（AI 新增模块时只需在此添加菜单项）
const menuList = [
  {
    path: '/dashboard',
    title: '数据看板',
    icon: 'DataAnalysis'
  },
  {
    path: '/business',
    title: '业务审核',
    icon: 'Checked',
    children: [
      { path: '/business/guarantee-manage', title: '保函管理', icon: 'Document' },
      { path: '/business/registration-review', title: '注册审核', icon: 'UserFilled' },
      { path: '/business/ai-review', title: 'AI 复审队列', icon: 'View' },
      { path: '/business/claim-review', title: '索赔复核队列', icon: 'Document' },
      { path: '/business/loan-review', title: '贷款审批', icon: 'Money' },
      { path: '/business/merchant-audit', title: '商户白名单', icon: 'Shop' },
      { path: '/business/entrust-review', title: '受托支付复核', icon: 'Money' },
      { path: '/business/risk-overview', title: '风险预警总览', icon: 'Warning' }
    ]
  },
  {
    path: '/content',
    title: '内容管理',
    icon: 'Notebook',
    children: [
      { path: '/content/alert-manage', title: '实时预警管理', icon: 'Bell' },
      { path: '/content/anti-fraud-manage', title: '反诈教学内容', icon: 'Reading' },
      { path: '/content/portal-manage', title: '政策门户管理', icon: 'Link' }
    ]
  },
  {
    path: '/system',
    title: '系统管理',
    icon: 'Setting',
    children: [
      { path: '/system/user', title: '用户管理', icon: 'User' }
    ]
  }
]

const activeMenu = computed(() => route.path)

const breadcrumbs = computed(() => {
  const matched = route.matched.filter(r => r.meta && r.meta.title)
  return matched.map(r => r.meta.title as string)
})

function handleCommand(cmd: string) {
  if (cmd === 'home') {
    window.open('/', '_blank')
  } else if (cmd === 'logout') {
    ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(async () => {
      await userStore.logout()
      ElMessage.success('已退出')
      router.push('/login')
    }).catch(() => {})
  }
}
</script>

<style scoped>
.admin-layout {
  height: 100vh;
}

.sidebar {
  background: #001529;
  transition: width 0.2s;
  overflow: hidden;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: #fff;
  background: #002140;
  font-weight: 600;
  font-size: 16px;
}

.logo-text {
  white-space: nowrap;
}

.side-menu {
  border-right: none;
  height: calc(100vh - 60px);
}

.header {
  background: #fff;
  border-bottom: 1px solid #e6e6e6;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  height: 60px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.collapse-btn {
  cursor: pointer;
  color: #606266;
}

.header-right {
  display: flex;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.username {
  font-size: 14px;
  color: #303133;
}

.main-content {
  background: #f0f2f5;
  padding: 20px;
  overflow-y: auto;
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
