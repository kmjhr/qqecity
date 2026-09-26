<template>
  <el-container style="height: 100vh;">
    <el-aside width="220px" style="background: #1f3a68;">
      <div style="color:#fff;font-size:16px;font-weight:700;padding:18px 16px;border-bottom:1px solid rgba(255,255,255,.15);">
        青启e城
      </div>
      <el-menu
        :default-active="route.path"
        router
        background-color="#1f3a68"
        text-color="#c9d6ea"
        active-text-color="#ffffff"
      >
        <el-menu-item index="/home"><el-icon><HomeFilled /></el-icon><span>首页</span></el-menu-item>
        <el-menu-item index="/guarantee"><el-icon><House /></el-icon><span>安居保函</span></el-menu-item>
        <el-menu-item index="/loan"><el-icon><Wallet /></el-icon><span>青创e贷</span></el-menu-item>
        <el-menu-item index="/budget"><el-icon><PieChart /></el-icon><span>消费预算</span></el-menu-item>
        <el-menu-item index="/bookkeeping"><el-icon><DataAnalysis /></el-icon><span>经营赋能</span></el-menu-item>
        <el-menu-item index="/safety"><el-icon><Shield /></el-icon><span>金融安全</span></el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header style="display:flex;align-items:center;justify-content:space-between;border-bottom:1px solid #e5e8ee;">
        <div style="font-size:15px;color:#333;">{{ route.meta.title || '青启e城' }}</div>
        <div style="display:flex;align-items:center;gap:12px;">
          <el-badge :value="unreadCount" :hidden="unreadCount === 0">
            <el-button link @click="showMessages = true">消息</el-button>
          </el-badge>
          <span style="color:#666;">{{ userStore.userInfo?.name || '未登录' }}</span>
          <el-button link type="primary" @click="onLogout">退出</el-button>
        </div>
      </el-header>

      <el-main style="background:#f5f7fa;">
        <router-view />
      </el-main>
    </el-container>

    <el-dialog v-model="showMessages" title="消息中心" width="560px">
      <el-empty v-if="messages.length === 0" description="暂无消息" />
      <el-timeline v-else>
        <el-timeline-item
          v-for="m in messages"
          :key="m.msgId"
          :timestamp="m.sendTime"
          :type="m.readStatus === 0 ? 'primary' : 'info'"
        >
          <div @click="onRead(m)" style="cursor:pointer;">
            <span v-if="m.readStatus === 0" style="color:#d54941;font-size:12px;margin-right:6px;">未读</span>
            {{ m.msgContent }}
          </div>
        </el-timeline-item>
      </el-timeline>
    </el-dialog>
  </el-container>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useUserStore } from '../store/user'
import { messageApi } from '../api'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const showMessages = ref(false)
const messages = ref([])
const unreadCount = ref(0)

async function loadMessages() {
  messages.value = await messageApi.list()
  unreadCount.value = messages.value.filter(m => m.readStatus === 0).length
}

async function onRead(m) {
  if (m.readStatus === 0) {
    await messageApi.read(m.msgId)
    m.readStatus = 1
    unreadCount.value = messages.value.filter(x => x.readStatus === 0).length
  }
}

function onLogout() {
  ElMessageBox.confirm('确定退出登录？', '提示', { type: 'warning' }).then(() => {
    userStore.logout()
    router.push('/login')
  })
}

onMounted(() => {
  if (!userStore.userInfo) {
    userStore.fetchMe().catch(() => {})
  }
  loadMessages()
})
</script>
