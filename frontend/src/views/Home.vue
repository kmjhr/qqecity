<template>
  <div>
    <el-card shadow="never">
      <div style="font-size:18px;font-weight:600;">欢迎，{{ userStore.userInfo?.name || '青年朋友' }}</div>
      <div style="color:#8a919c;font-size:13px;margin-top:6px;">
        青启e城面向城市新场景青年（在校大学生、应届毕业生、Z世代），提供安居、创业、消费三大场景的演示服务。
      </div>
    </el-card>

    <el-row :gutter="16" style="margin-top:16px;">
      <el-col :span="8" v-for="card in cards" :key="card.path">
        <el-card shadow="hover" style="cursor:pointer;" @click="$router.push(card.path)">
          <div style="font-size:15px;font-weight:600;color:#1f3a68;">{{ card.title }}</div>
          <div style="font-size:13px;color:#5a616b;margin-top:8px;line-height:1.6;">{{ card.desc }}</div>
          <div style="font-size:12px;color:#8a919c;margin-top:10px;">{{ card.tag }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" style="margin-top:16px;">
      <template #header>演示主链路</template>
      <el-steps :active="6" simple>
        <el-step title="注册登录" />
        <el-step title="安居保函" />
        <el-step title="B类免费预审" />
        <el-step title="设置预算" />
        <el-step title="分级提醒" />
        <el-step title="反诈甄别" />
      </el-steps>
      <div style="font-size:12px;color:#8a919c;margin-top:12px;">
        银行侧能力（放款、开函、征信）均为模拟桩实现；正式接入需金融合规评审。
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { useUserStore } from '../store/user'

const userStore = useUserStore()

const cards = [
  { path: '/guarantee', title: '安居金融风控', desc: '租房履约保函：申请 → 房东确认 → AI合同复审 → 缴费出函', tag: '押金换保函，缓解应届生现金流' },
  { path: '/loan', title: '轻创业智能授信', desc: '青创e贷：B类两步式（免费预审不查征信 → 100%受托支付）', tag: 'A类最高5万，B类1—2万（演示）' },
  { path: '/budget', title: '碎片消费治理', desc: '分类预算、MCC归类、50%/20%分级提醒、结余转储蓄', tag: '场景三：碎片化消费' },
  { path: '/bookkeeping', title: '创业经营赋能', desc: 'AI记账、现金流报表（本期为占位接口）', tag: '二期完善' },
  { path: '/safety', title: '青年金融安全', desc: '反诈教学、骗局话术甄别、借贷行为预警', tag: '场景四：金融安全' }
]
</script>
