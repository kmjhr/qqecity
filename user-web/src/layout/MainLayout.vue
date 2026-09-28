<template>
  <el-container class="app-shell">
    <!-- 左侧常驻导航栏（主场景 + 智能中台分组） -->
    <el-aside :width="asideWidth" class="app-aside">
      <div class="aside-logo" @click="$router.push('/home')">
        <div class="logo-badge">青</div>
        <span class="logo-text">青启e城</span>
        <el-tag size="small" type="warning" effect="plain" class="demo-tag">演示</el-tag>
      </div>

      <el-menu
        :default-active="$route.path"
        router
        class="aside-menu"
        background-color="transparent"
        text-color="#c0c4cc"
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
        <el-menu-item index="/bookkeeping">
          <el-icon><Notebook /></el-icon>
          <span>经营赋能</span>
        </el-menu-item>
        <el-menu-item index="/budget">
          <el-icon><PieChart /></el-icon>
          <span>预算消费</span>
        </el-menu-item>
        <el-menu-item index="/safety">
          <el-icon><Lock /></el-icon>
          <span>金融安全</span>
        </el-menu-item>
        <el-menu-item index="/orders">
          <el-icon><List /></el-icon>
          <span>我的订单</span>
        </el-menu-item>

        <el-sub-menu index="intel" class="intel-group">
          <template #title>
            <el-icon><Cpu /></el-icon>
            <span>智能中台</span>
          </template>
          <el-menu-item index="/policy">
            <el-icon><Document /></el-icon>
            <span>政策匹配</span>
          </el-menu-item>
          <el-menu-item index="/credit-profile">
            <el-icon><DataLine /></el-icon>
            <span>信用画像</span>
          </el-menu-item>
          <el-menu-item index="/chat">
            <el-icon><ChatLineSquare /></el-icon>
            <span>智能对话</span>
          </el-menu-item>
          <el-menu-item index="/risk">
            <el-icon><Warning /></el-icon>
            <span>风险预警</span>
          </el-menu-item>
          <el-menu-item index="/teaching">
            <el-icon><Reading /></el-icon>
            <span>反诈教学</span>
          </el-menu-item>
        </el-sub-menu>
      </el-menu>

      <div class="aside-footer">
        <div class="aside-footer-item"><el-icon><Monitor /></el-icon> 演示系统 · 银行能力模拟</div>
      </div>
    </el-aside>

    <!-- 右侧主区：顶栏 + 内容 + 页脚 -->
    <el-container class="app-main-wrap" :class="{ 'with-panel': uiStore.panelOpen || uiStore.chatOpen }">
      <el-header class="app-header">
        <div class="header-left">
          <div class="page-title">{{ currentTitle }}</div>
        </div>
        <div class="header-right">
          <!-- 智能对话：点击滑出对话侧栏（消息中心同款，可同时使用系统） -->
          <el-tooltip content="智能对话" placement="bottom">
            <el-button circle class="chat-drawer-btn" @click="uiStore.openChat()">
              <el-icon :size="18"><ChatLineSquare /></el-icon>
            </el-button>
          </el-tooltip>
          <!-- 消息铃铛：点击滑出消息侧栏 -->
          <el-badge :value="uiStore.unreadCount" :max="99" :hidden="uiStore.unreadCount === 0"
            class="bell-badge" :class="{ 'has-new': uiStore.unreadCount > 0 }">
            <el-button circle class="bell-btn" :class="{ 'has-new': uiStore.unreadCount > 0 }"
              @click="uiStore.togglePanel()" title="消息中心">
              <el-icon :size="18"><Bell /></el-icon>
            </el-button>
          </el-badge>
          <!-- 用户区 -->
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
      </el-header>

      <el-main class="app-main">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>

      <el-footer class="app-footer">
        <p>© 2024 青启e城 · 演示系统</p>
      </el-footer>
    </el-container>

    <!-- 消息侧栏（消息 + 对话，可同时使用系统） -->
    <MessagePanel />
    <!-- 智能对话侧栏（消息中心同款） -->
    <ChatDrawer />
  </el-container>
</template>

<script setup>
import { computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, ElNotification } from 'element-plus'
import {
  HomeFilled, Wallet, Money, PieChart, Lock, Notebook, List, Cpu,
  Document, DataLine, ChatLineSquare, Warning, Reading, Bell,
  User, SwitchButton, ArrowDown, Monitor
} from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import { useUiStore } from '@/store/ui'
import { getUnreadCount } from '@/api/message'
import MessagePanel from '@/components/MessagePanel.vue'
import ChatDrawer from '@/components/ChatDrawer.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const uiStore = useUiStore()

const asideWidth = '218px'

const currentTitle = computed(() => route.meta.title || '')

onMounted(() => {
  refreshUnread()
  // 新消息轮询：每 15 秒拉取一次未读数；发现新增 → 全局通知提醒（不打断当前操作）
  pollTimer = setInterval(async () => {
    const prev = uiStore.unreadCount
    try {
      const count = await getUnreadCount()
      uiStore.setUnread(count || 0)
      if ((count || 0) > prev && prev >= 0 && !uiStore.panelOpen) {
        showNewMsgNotify(count)
      }
    } catch (e) { /* 忽略 */ }
  }, 15000)
})

onBeforeUnmount(() => {
  if (pollTimer) clearInterval(pollTimer)
})

let pollTimer = null

function showNewMsgNotify(count) {
  ElNotification({
    title: `收到 ${count} 条新消息`,
    message: '点击查看消息中心；侧栏消息/对话可同时使用，不打断当前操作',
    type: 'info',
    duration: 5000,
    position: 'bottom-right',
    onClick: () => uiStore.openPanel('msg')
  })
}

async function refreshUnread() {
  try {
    const count = await getUnreadCount()
    uiStore.setUnread(count || 0)
  } catch (e) { /* 忽略 */ }
}

function handleCommand(command) {
  if (command === 'message') {
    uiStore.openPanel('msg')
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
.app-shell {
  min-height: 100vh;
}

/* ===== 左侧导航 ===== */
.app-aside {
  background: linear-gradient(180deg, #1f2d3d 0%, #24344a 100%);
  display: flex;
  flex-direction: column;
  color: #fff;
  position: sticky;
  top: 0;
  height: 100vh;
  overflow-y: auto;
  flex-shrink: 0;
}

.aside-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 18px 16px;
  cursor: pointer;
}

.logo-badge {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  background: linear-gradient(135deg, #409EFF, #67c23a);
  color: #fff;
  font-size: 18px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

.logo-text {
  font-size: 17px;
  font-weight: 600;
  color: #fff;
}

.demo-tag {
  margin-left: auto;
}

.aside-menu {
  border-right: none;
  flex: 1;
  padding: 4px 8px;
}

.aside-menu :deep(.el-menu-item),
.aside-menu :deep(.el-sub-menu__title) {
  height: 44px;
  line-height: 44px;
  border-radius: 8px;
  margin-bottom: 2px;
}

.aside-menu :deep(.el-menu-item.is-active) {
  background: rgba(64, 158, 255, 0.16);
  font-weight: 600;
}

.aside-menu :deep(.el-sub-menu .el-menu-item) {
  background: transparent;
  height: 40px;
  line-height: 40px;
  padding-left: 46px !important;
}

.aside-menu :deep(.el-sub-menu .el-menu-item:hover) {
  background: rgba(255, 255, 255, 0.06);
}

.aside-menu :deep(.el-sub-menu .el-menu-item.is-active) {
  background: rgba(64, 158, 255, 0.2);
}

.aside-menu :deep(.el-menu-item:hover),
.aside-menu :deep(.el-sub-menu__title:hover) {
  background: rgba(255, 255, 255, 0.06);
}

.menu-group-label {
  padding: 14px 16px 6px;
  font-size: 12px;
  color: #7a8ba3;
  letter-spacing: 1px;
  user-select: none;
}

.intel-group {
  margin-top: 10px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  padding-top: 4px;
}

.aside-footer {
  padding: 14px 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  font-size: 12px;
  color: #7a8ba3;
}

.aside-footer-item {
  display: flex;
  align-items: center;
  gap: 6px;
}

/* ===== 右侧主区 ===== */
.app-main-wrap {
  min-width: 0;
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  transition: margin-right 0.3s ease;
}

/* 消息侧栏打开时主区让位，不遮挡内容（可同时使用系统） */
.app-main-wrap.with-panel {
  margin-right: 400px;
}

.app-header {
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  padding: 0 24px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-title {
  font-size: 17px;
  font-weight: 600;
  color: #303133;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 18px;
}

.bell-badge {
  display: flex;
  align-items: center;
}

.bell-btn,
.chat-drawer-btn {
  border: none;
  background: #f5f7fa;
  color: #606266;
}

.bell-btn:hover,
.chat-drawer-btn:hover {
  background: #ecf5ff;
  color: #409eff;
}

.bell-badge.has-new .bell-btn,
.bell-btn.has-new {
  background: #fef0f0;
  color: #f56c6c;
  animation: bell-ring 0.9s ease 2;
}

@keyframes bell-ring {
  0%, 100% { transform: rotate(0); }
  20% { transform: rotate(14deg); }
  40% { transform: rotate(-11deg); }
  60% { transform: rotate(7deg); }
  80% { transform: rotate(-4deg); }
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

.app-main {
  flex: 1;
  background: #f5f7fa;
  padding: 24px;
  min-width: 0;
}

.app-footer {
  background: #fff;
  text-align: center;
  color: #909399;
  font-size: 13px;
  border-top: 1px solid #ebeef5;
  height: 48px;
  line-height: 48px;
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
