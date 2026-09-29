<template>
  <el-container class="main-layout">
    <!-- 顶部导航栏 -->
    <el-header class="header">
      <div class="header-inner">
        <div class="logo" @click="$router.push('/home')">
          <img src="/logo-city.png" alt="青启e城" class="logo-img" />
          <div class="logo-text">
            <span class="logo-title">青启e城</span>
            <span class="logo-subtitle">QINGQI eCity</span>
          </div>
        </div>

        <el-menu
          mode="horizontal"
          :default-active="activePath"
          router
          class="nav-menu"
          background-color="transparent"
          text-color="#475569"
          active-text-color="#0ea5e9"
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
            <span>创业经营</span>
          </el-menu-item>
          <el-menu-item index="/budget">
            <el-icon><PieChart /></el-icon>
            <span>预算消费</span>
          </el-menu-item>
          <el-menu-item index="/safety">
            <el-icon><Lock /></el-icon>
            <span>金融安全</span>
          </el-menu-item>
        </el-menu>

        <div class="user-area">
          <el-button circle text @click="openOrder" title="账单中心">
            <el-icon :size="18"><Tickets /></el-icon>
          </el-button>

          <el-badge :value="unreadCount" :max="99" class="icon-badge" type="primary">
            <el-button circle text @click="openMessage" title="消息中心">
              <el-icon :size="18"><Bell /></el-icon>
            </el-button>
          </el-badge>

          <el-dropdown @command="handleCommand" trigger="click">
            <div class="user-name">
              <el-avatar :size="34" :src="userStore.userInfo?.avatar" class="user-avatar">
                {{ userStore.username?.charAt(0) || 'U' }}
              </el-avatar>
              <div class="user-meta">
                <span class="name-text">{{ userStore.username || '用户' }}</span>
                <span class="role-text">青年用户</span>
              </div>
              <el-icon><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon>个人中心
                </el-dropdown-item>
                <el-dropdown-item command="message">
                  <el-icon><Bell /></el-icon>消息中心
                </el-dropdown-item>
                <el-dropdown-item command="orders">
                  <el-icon><Tickets /></el-icon>账单中心
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
      <div class="content-wrapper">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </div>
    </el-main>

    <!-- 页脚 -->
    <el-footer class="footer">
      <div class="footer-inner">
        <div class="footer-left">
          <img src="/logo-icon.jpg" alt="" class="footer-logo" />
          <span>青启e城 · 青年金融智能服务平台</span>
        </div>
        <div class="footer-right">
          <span>© 2026 青启e城</span>
          <span class="divider">|</span>
          <span>演示系统</span>
        </div>
      </div>
    </el-footer>

    <!-- 左下角悬浮智能中台（5 功能图标竖向：对话→侧边栏，其余→对应页面） -->
    <div class="smart-hub">
      <div class="hub-icons">
        <el-tooltip content="智能对话" placement="right">
          <div class="hub-icon" @click="openAssistant">
            <el-icon :size="18"><ChatLineSquare /></el-icon>
          </div>
        </el-tooltip>
        <el-tooltip content="政策匹配" placement="right">
          <div class="hub-icon" @click="go('/policy')">
            <el-icon :size="18"><Document /></el-icon>
          </div>
        </el-tooltip>
        <el-tooltip content="信用画像" placement="right">
          <div class="hub-icon" @click="go('/credit-profile')">
            <el-icon :size="18"><DataLine /></el-icon>
          </div>
        </el-tooltip>
        <el-tooltip content="风险预警" placement="right">
          <div class="hub-icon" @click="go('/risk')">
            <el-icon :size="18"><Warning /></el-icon>
          </div>
        </el-tooltip>
        <el-tooltip content="反诈教学" placement="right">
          <div class="hub-icon" @click="go('/teaching')">
            <el-icon :size="18"><Reading /></el-icon>
          </div>
        </el-tooltip>
      </div>
    </div>

    <!-- ==================== 消息侧边栏（点标题进入消息中心页） ==================== -->
    <el-drawer v-model="messageVisible" size="380px" class="side-drawer" :modal="false" :lock-scroll="false">
      <template #header>
        <span class="drawer-title" @click="go('/message')">消息中心 →</span>
      </template>
      <div class="drawer-toolbar">
        <span>共 {{ messageTotal }} 条 · {{ unreadCount }} 条未读</span>
        <el-button text type="primary" size="small" class="grad-white-btn" :disabled="!unreadCount" @click="handleMarkAllRead">全部已读</el-button>
      </div>
      <div v-loading="messageLoading" class="drawer-list">
        <div
          v-for="m in messageList"
          :key="m.id"
          class="drawer-item"
          :class="{ unread: !m.readFlag }"
          @click="handleReadMessage(m)"
        >
          <div class="drawer-item-title">{{ m.title }}</div>
          <div class="drawer-item-desc">{{ m.content }}</div>
          <div class="drawer-item-time">{{ (m.createTime || '').replace('T', ' ') }}</div>
        </div>
        <el-empty v-if="!messageLoading && !messageList.length" description="暂无消息" />
      </div>
    </el-drawer>

    <!-- ==================== 订单侧边栏（点标题进入账单中心页） ==================== -->
    <el-drawer v-model="orderVisible" size="380px" class="side-drawer" :modal="false" :lock-scroll="false">
      <template #header>
        <span class="drawer-title" @click="go('/orders')">账单中心 →</span>
      </template>
      <div v-loading="orderLoading" class="drawer-list">
        <div v-for="o in orderList" :key="o.orderNo" class="drawer-item">
          <div class="drawer-item-title">{{ o.subject || o.bizTypeName || o.orderNo }}</div>
          <div class="drawer-item-row">
            <el-tag size="small" :type="orderStatusTag(o.status)" effect="plain">{{ o.statusName || o.status }}</el-tag>
            <b class="order-amount">¥{{ o.amount }}</b>
          </div>
          <div class="drawer-item-time">{{ (o.createTime || '').slice(0, 19) }}</div>
        </div>
        <el-empty v-if="!orderLoading && !orderList.length" description="暂无订单" />
      </div>
    </el-drawer>

    <!-- ==================== 智能助手台侧边栏（对话 + 中台入口） ==================== -->
    <el-drawer v-model="assistantVisible" title="智能对话" size="460px" class="assistant-drawer" :modal="false" :lock-scroll="false">
      <template #header>
        <span class="drawer-title" @click="go('/chat')">智能对话 →</span>
      </template>
      <ChatPanel class="drawer-chat" />
    </el-drawer>
  </el-container>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  HomeFilled, Wallet, Money, PieChart, Lock, Notebook,
  Bell, Tickets, ChatLineSquare, Document, DataLine, Warning, Reading,
  ArrowDown, User, SwitchButton
} from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import { getUnreadCount, getMessagePage, markRead, markAllRead } from '@/api/message'
import { getOrders } from '@/api/pay'
import ChatPanel from '@/components/ChatPanel.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const activePath = computed(() => route.path)

// ---- 消息侧边栏 ----
const messageVisible = ref(false)
const messageList = ref([])
const messageTotal = ref(0)
const messageLoading = ref(false)
const unreadCount = ref(0)

// ---- 订单侧边栏 ----
const orderVisible = ref(false)
const orderList = ref([])
const orderLoading = ref(false)

// ---- 智能助手台 ----
const assistantVisible = ref(false)

async function loadUnread() {
  try {
    const res = await getUnreadCount()
    const c = typeof res === 'object' && res !== null ? (res.unreadCount ?? res.count ?? res.unread ?? res.total ?? 0) : (res ?? 0)
    unreadCount.value = Number(c) || 0
  } catch (e) {}
}

async function openMessage() {
  orderVisible.value = false
  assistantVisible.value = false
  messageVisible.value = true
  messageLoading.value = true
  try {
    const res = await getMessagePage({ pageNum: 1, pageSize: 8 })
    messageList.value = res?.records || []
    messageTotal.value = Number(res?.total || 0)
  } catch (e) {} finally {
    messageLoading.value = false
  }
}

async function handleReadMessage(m) {
  if (!m.readFlag) {
    m.readFlag = true
    if (unreadCount.value > 0) unreadCount.value -= 1
    try { await markRead(m.id) } catch (e) {}
  }
}

async function handleMarkAllRead() {
  try {
    await markAllRead()
    messageList.value.forEach(m => { m.readFlag = true })
    unreadCount.value = 0
    ElMessage.success('已全部标记为已读')
  } catch (e) {
    ElMessage.error('操作失败，请重试')
  }
}

async function openOrder() {
  messageVisible.value = false
  assistantVisible.value = false
  orderVisible.value = true
  orderLoading.value = true
  try {
    const res = await getOrders({ pageNum: 1, pageSize: 10 })
    orderList.value = res?.records || []
  } catch (e) {} finally {
    orderLoading.value = false
  }
}

function orderStatusTag(s) {
  const map = { PENDING_PAY: 'warning', PAID: 'success', CLOSED: 'info', REFUNDED: 'danger' }
  return map[s] || 'info'
}

// ---- 智能助手台 ----
function openAssistant() {
  messageVisible.value = false
  orderVisible.value = false
  assistantVisible.value = true
}

function go(path) {
  router.push(path)
}

function handleCommand(command) {
  if (command === 'message') {
    router.push('/message')
  } else if (command === 'orders') {
    router.push('/orders')
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

loadUnread()
</script>

<style scoped>
.main-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--qq-bg);
}

.header {
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(12px);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  padding: 0;
  height: 72px;
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-inner {
  max-width: 1280px;
  margin: 0 auto;
  height: 100%;
  display: flex;
  align-items: center;
  padding: 0 24px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  margin-right: 40px;
  transition: opacity 0.2s;
}

.logo:hover { opacity: 0.85; }

.logo-img {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  object-fit: contain;
  background: transparent;
  transform: translateY(-3px);
}

.logo-text {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
}

.logo-title {
  font-size: 18px;
  font-weight: 700;
  background: linear-gradient(135deg, #0ea5e9 0%, #10b981 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.logo-subtitle {
  font-size: 10px;
  color: var(--qq-text-muted);
  letter-spacing: 0.5px;
  font-weight: 500;
}

.nav-menu {
  flex: 1;
  border-bottom: none;
  height: 72px;
}

.nav-menu :deep(.el-menu-item) {
  height: 72px;
  line-height: 72px;
  font-size: 14px;
  font-weight: 500;
  padding: 0 16px;
}

.nav-menu :deep(.el-menu-item .el-icon) {
  margin-right: 6px;
  font-size: 18px;
}

.user-area {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 10px;
}

.icon-badge :deep(.el-badge__content) {
  top: 6px;
  right: 6px;
}

.user-name {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  padding: 6px 10px 6px 6px;
  border-radius: 999px;
  transition: background 0.2s;
}

.user-name:hover { background: #f1f5f9; }

.user-avatar {
  border: 2px solid #e0f2fe;
  font-size: 14px;
  font-weight: 600;
}

.user-meta {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
}

.name-text {
  font-size: 14px;
  font-weight: 600;
  color: var(--qq-text);
}

.role-text {
  font-size: 11px;
  color: var(--qq-text-muted);
}

.main-content {
  flex: 1;
  padding: 24px;
}

.content-wrapper {
  max-width: 1280px;
  margin: 0 auto;
  min-height: calc(100vh - 168px);
}

.footer {
  background: #fff;
  border-top: 1px solid var(--qq-border);
  padding: 0;
  height: 64px;
}

.footer-inner {
  max-width: 1280px;
  margin: 0 auto;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  color: var(--qq-text-secondary);
  font-size: 13px;
}

.footer-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.footer-logo {
  width: 24px;
  height: 24px;
  border-radius: 6px;
  object-fit: cover;
}

.footer-right { display: flex; align-items: center; gap: 12px; }
.divider { color: var(--qq-border); }

.fade-enter-active,
.fade-leave-active { transition: opacity 0.25s ease; }
.fade-enter-from,
.fade-leave-to { opacity: 0; }

/* ---------- 悬浮智能中台（5 功能图标竖向） ---------- */
.smart-hub {
  position: fixed;
  left: 24px;
  bottom: 24px;
  z-index: 90;
  background: linear-gradient(135deg, #0ea5e9, #10b981);
  border-radius: 999px;
  padding: 10px 8px;
  box-shadow: 0 8px 24px rgba(14, 165, 233, 0.35);
  color: #fff;
}

.hub-icons {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.hub-icon {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.18);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: background 0.15s, transform 0.15s;
}

.hub-icon:hover {
  background: rgba(255, 255, 255, 0.34);
  transform: translateY(-2px);
}

/* ---------- 侧边栏通用 ---------- */
/* 抽屉打开时主页面可操作：外层全屏容器不拦截点击，仅抽屉面板可交互 */
:deep(.el-modal-drawer) {
  pointer-events: none;
}
:deep(.el-modal-drawer .el-drawer) {
  pointer-events: auto;
}
.side-drawer :deep(.el-drawer__body) {
  padding: 12px 16px;
}

.drawer-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  cursor: pointer;
}

.drawer-title:hover {
  color: #0ea5e9;
}

.drawer-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  font-size: 12px;
  color: var(--qq-text-secondary);
}

.drawer-list {
  min-height: 200px;
  max-height: calc(100vh - 140px);
  overflow-y: auto;
}

.drawer-item {
  padding: 12px;
  border: 1px solid var(--qq-border);
  border-radius: 10px;
  margin-bottom: 10px;
  cursor: pointer;
  transition: background 0.15s;
}

.drawer-item:hover { background: #f8fafc; }
.drawer-item.unread { background: #f0f9ff; border-color: #bae6fd; }

.drawer-item-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.drawer-item-desc {
  font-size: 12px;
  color: #606266;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.drawer-item-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 4px 0;
}

.order-amount { color: #f56c6c; font-size: 14px; }

.drawer-item-time {
  font-size: 11px;
  color: #909399;
  margin-top: 4px;
}

/* ---------- 智能助手台抽屉 ---------- */
.assistant-drawer :deep(.el-drawer__body) {
  display: flex;
  flex-direction: column;
  padding: 16px 20px;
}

.drawer-chat {
  flex: 1;
  min-height: 0;
}

@media (max-width: 1024px) {
  .nav-menu :deep(.el-menu-item) {
    padding: 0 10px;
    font-size: 13px;
  }

  .logo { margin-right: 20px; }
}
</style>
