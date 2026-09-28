<template>
  <el-dialog
    :model-value="modelValue"
    :title="title"
    width="460px"
    :close-on-click-modal="false"
    @update:model-value="$emit('update:modelValue', $event)"
  >
    <div v-loading="loading" class="cashier">
      <template v-if="order">
        <!-- ========== 支付成功面板 ========== -->
        <div v-if="paidOrder" class="paid-panel">
          <div class="paid-icon">
            <el-icon :size="52"><CircleCheckFilled /></el-icon>
          </div>
          <div class="paid-title">支付成功</div>
          <div class="paid-amount">¥{{ paidOrder.amount }}</div>
          <div class="paid-info">
            <div class="paid-row"><span>订单号</span><b>{{ paidOrder.orderNo }}</b></div>
            <div class="paid-row"><span>商品</span><b>{{ paidOrder.subject }}</b></div>
            <div class="paid-row" v-if="paidOrder.merchantName"><span>收款商户</span><b>{{ paidOrder.merchantName }}</b></div>
            <div class="paid-row"><span>支付方式</span><b>{{ paidOrder.payMethodName || '扫码支付' }}</b></div>
            <div class="paid-row"><span>支付时间</span><b>{{ (paidOrder.payTime || '').slice(0, 19) }}</b></div>
            <div class="paid-row"><span>订单状态</span><el-tag size="small" type="success">已支付</el-tag></div>
          </div>
          <div class="paid-actions">
            <el-button type="primary" @click="goOrders">查看订单详情</el-button>
            <el-button plain @click="closeDialog">关闭</el-button>
          </div>
          <div class="cashier-tip">演示系统：模拟扫码支付成功，不产生真实资金往来</div>
        </div>

        <!-- ========== 扫码支付区 ========== -->
        <template v-else>
          <div class="cashier-order">
            <div class="cashier-subject">{{ order.subject }}</div>
            <div class="cashier-meta">
              <span>订单号：{{ order.orderNo }}</span>
              <span v-if="order.merchantName">收款商户：{{ order.merchantName }}</span>
            </div>
            <div class="cashier-total">
              <span class="cashier-total-label">需支付（元）</span>
              <span class="cashier-total-num">¥{{ order.amount }}</span>
            </div>
            <div class="cashier-expire" v-if="countdown !== null">
              <el-icon><Clock /></el-icon>
              <span>二维码有效期 {{ countdownText }}，超时订单自动关闭</span>
            </div>
          </div>

          <div class="qr-channels">
            <div
              v-for="ch in channels" :key="ch.key"
              class="qr-channel" :class="{ active: channel.key === ch.key }"
              :style="{ borderColor: channel.key === ch.key ? ch.color : 'transparent' }"
              @click="switchChannel(ch)"
            >
              <span class="qr-channel-dot" :style="{ background: ch.color }"></span>
              <span>{{ ch.name }}</span>
            </div>
          </div>

          <div class="qr-box" :style="{ borderColor: channel.color }">
            <div class="qr-svg" v-html="qrSvg"></div>
            <div class="qr-mask" v-if="paying">
              <div class="qr-mask-spin"></div>
              <div>正在等待扫码支付…</div>
            </div>
          </div>
          <div class="qr-tip">{{ channel.tip }} · 识别「青启e城」收款码</div>
          <div class="qr-note">模拟二维码 · 演示点击下方按钮即完成支付</div>
          <el-button
            type="primary"
            size="large"
            class="qr-pay-btn"
            :style="{ background: channel.color, borderColor: channel.color }"
            :loading="paying"
            @click="submitQrPay"
          >
            模拟扫码支付完成
          </el-button>
        </template>
      </template>
    </div>
  </el-dialog>
</template>

<script setup>
import { ref, watch, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getOrder, payOrder } from '@/api/pay'

const props = defineProps({
  modelValue: Boolean,
  orderNo: String
})
const emit = defineEmits(['update:modelValue', 'paid'])

const router = useRouter()
const loading = ref(false)
const order = ref(null)
const paidOrder = ref(null)
const paying = ref(false)

// 扫码渠道（模拟微信/支付宝/云闪付/工行）
const channels = [
  { key: 'QR_WECHAT', name: '微信支付', color: '#07c160', tip: '打开微信「扫一扫」' },
  { key: 'QR_ALIPAY', name: '支付宝', color: '#1677ff', tip: '打开支付宝「扫一扫」' },
  { key: 'QR_UNIONPAY', name: '云闪付', color: '#e60012', tip: '打开云闪付/银行App「扫一扫」' },
  { key: 'QR_ICBC', name: '工商银行', color: '#d71920', tip: '打开工行手机银行「扫一扫」' }
]
const channel = ref(channels[0])
const qrSeed = ref(Date.now())

// 倒计时
const countdown = ref(null)
let timer = null
const countdownText = computed(() => {
  if (countdown.value === null) return ''
  const m = Math.floor(countdown.value / 60)
  const s = countdown.value % 60
  return `${m}:${String(s).padStart(2, '0')}`
})

// ---------- 模拟二维码（SVG 伪二维码，无外部依赖） ----------
const qrSvg = computed(() => {
  const size = 25
  let cells = []
  const rand = (i) => {
    const x = Math.sin(qrSeed.value * 0.001 + i * 12.9898) * 43758.5453
    return (x - Math.floor(x)) > 0.5
  }
  for (let r = 0; r < size; r++) {
    for (let c = 0; c < size; c++) {
      const inFinder = (fr, fc) => r >= fr && r < fr + 7 && c >= fc && c < fc + 7
      const finder = inFinder(0, 0) || inFinder(0, size - 7) || inFinder(size - 7, 0)
      let fill = '#ffffff'
      if (finder) {
        const lr = r - (inFinder(0, 0) ? 0 : inFinder(0, size - 7) ? 0 : size - 7)
        const lc = c - (inFinder(0, 0) ? 0 : inFinder(0, size - 7) ? size - 7 : 0)
        const core = lr >= 2 && lr <= 4 && lc >= 2 && lc <= 4
        const ring = lr === 0 || lr === 6 || lc === 0 || lc === 6
        fill = (core || ring) ? '#1f2d3d' : '#ffffff'
      } else if (rand(r * size + c)) {
        fill = '#1f2d3d'
      }
      cells.push(`<rect x="${c}" y="${r}" width="1" height="1" fill="${fill}"/>`)
    }
  }
  return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${size} ${size}" shape-rendering="crispEdges">${cells.join('')}<rect x="8.5" y="8.5" width="8" height="8" fill="#ffffff"/><text x="12.5" y="14.5" font-size="2.2" text-anchor="middle" fill="${channel.value.color}" font-weight="700" font-family="sans-serif">青启e城</text></svg>`
})

const switchChannel = (ch) => {
  channel.value = ch
  qrSeed.value = Date.now()
}

// ---------- 加载订单 ----------
const loadOrder = async () => {
  if (!props.modelValue || !props.orderNo) return
  loading.value = true
  paidOrder.value = null
  channel.value = channels[0]
  qrSeed.value = Date.now()
  try {
    order.value = await getOrder(props.orderNo)
    if (order.value.status === 'PAID') paidOrder.value = order.value
    if (order.value.expireTime) {
      const exp = new Date(order.value.expireTime.replace(' ', 'T')).getTime()
      const tick = () => {
        countdown.value = Math.max(0, Math.floor((exp - Date.now()) / 1000))
        if (countdown.value === 0 && order.value.status === 'PENDING_PAY') {
          ElMessage.warning('订单已超时关闭，请重新下单')
          emit('update:modelValue', false)
        }
      }
      clearInterval(timer)
      tick()
      timer = setInterval(tick, 1000)
    }
  } catch (e) {} finally { loading.value = false }
}

watch(() => [props.modelValue, props.orderNo], loadOrder)
onMounted(loadOrder)
onUnmounted(() => clearInterval(timer))

const title = computed(() => (paidOrder.value ? '支付结果' : '收银台（模拟支付）'))

// ---------- 扫码支付（模拟外部渠道） ----------
const submitQrPay = async () => {
  paying.value = true
  try {
    const res = await payOrder(props.orderNo, { payMethod: channel.value.key })
    paidOrder.value = res
    ElMessage.success('支付成功（模拟扫码）')
    emit('paid', res)
  } catch (e) {} finally { paying.value = false }
}

const goOrders = () => {
  emit('update:modelValue', false)
  router.push('/orders')
}
const closeDialog = () => emit('update:modelValue', false)
</script>

<style scoped>
.cashier-order {
  padding: 14px;
  background: #f5f7fa;
  border-radius: 8px;
  margin-bottom: 12px;
}
.cashier-subject { font-size: 15px; font-weight: 600; color: #303133; }
.cashier-meta { font-size: 12px; color: #909399; margin: 6px 0; display: flex; flex-direction: column; gap: 2px; }
.cashier-total { display: flex; align-items: baseline; justify-content: space-between; margin-top: 8px; }
.cashier-total-label { font-size: 13px; color: #606266; }
.cashier-total-num { font-size: 30px; font-weight: 700; color: #f56c6c; }
.cashier-expire { margin-top: 8px; font-size: 12px; color: #e6a23c; display: flex; align-items: center; gap: 4px; }

/* 扫码区 */
.qr-channels { display: flex; gap: 6px; justify-content: center; margin-bottom: 12px; flex-wrap: wrap; }
.qr-channel {
  display: flex; align-items: center; gap: 5px;
  padding: 6px 10px; border-radius: 6px; border: 1.5px solid transparent;
  font-size: 13px; color: #303133; cursor: pointer; background: #f5f7fa;
}
.qr-channel.active { background: #fff; font-weight: 600; }
.qr-channel-dot { width: 10px; height: 10px; border-radius: 3px; display: inline-block; }
.qr-box {
  position: relative;
  width: 210px; height: 210px; margin: 0 auto;
  border: 2px solid; border-radius: 10px; padding: 8px; background: #fff;
}
.qr-svg { width: 100%; height: 100%; }
.qr-svg :deep(svg) { width: 100%; height: 100%; }
.qr-mask {
  position: absolute; inset: 0; border-radius: 8px;
  background: rgba(255,255,255,0.92);
  display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 8px;
  font-size: 13px; color: #606266;
}
.qr-mask-spin { width: 28px; height: 28px; border: 3px solid #e4e7ed; border-top-color: #409eff; border-radius: 50%; animation: spin 1s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
.qr-tip { margin-top: 10px; font-size: 13px; color: #606266; text-align: center; }
.qr-note { font-size: 11px; color: #c0c4cc; margin: 4px 0 12px; text-align: center; }
.qr-pay-btn { width: 100%; margin-top: 4px; }

/* 成功面板 */
.paid-panel { text-align: center; padding: 8px 0 4px; }
.paid-icon { color: #67c23a; margin-bottom: 6px; }
.paid-title { font-size: 18px; font-weight: 700; color: #303133; }
.paid-amount { font-size: 34px; font-weight: 700; color: #303133; margin: 8px 0 14px; }
.paid-info {
  text-align: left; background: #f5f7fa; border-radius: 8px; padding: 14px 16px;
  display: flex; flex-direction: column; gap: 8px;
}
.paid-row { display: flex; justify-content: space-between; align-items: center; font-size: 13px; color: #606266; }
.paid-row b { color: #303133; font-weight: 600; max-width: 240px; word-break: break-all; }
.paid-actions { margin-top: 18px; display: flex; gap: 10px; justify-content: center; }
.cashier-tip { margin-top: 10px; font-size: 12px; color: #c0c4cc; text-align: center; }
</style>
