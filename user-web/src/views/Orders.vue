<template>
  <div class="page orders-page">
    <el-card shadow="never">
      <template #header>
        <div class="orders-head">
          <span class="section-title">我的订单（模拟支付中心）</span>
          <el-radio-group v-model="statusFilter" size="small">
            <el-radio-button :value="''">全部</el-radio-button>
            <el-radio-button :value="'PENDING_PAY'">待支付</el-radio-button>
            <el-radio-button :value="'PAID'">已支付</el-radio-button>
            <el-radio-button :value="'CLOSED'">已关闭</el-radio-button>
            <el-radio-button :value="'REFUNDED'">已退款</el-radio-button>
          </el-radio-group>
        </div>
      </template>

      <!-- 青创集市消费（模拟下单 → 扫码支付） -->
      <div class="consume-entry">
        <el-button type="success" plain @click="consumeVisible = !consumeVisible">
          <el-icon><ShoppingCart /></el-icon> 青创集市消费（模拟下单）
        </el-button>
        <div v-if="consumeVisible" class="consume-form">
          <el-form :inline="true">
            <el-form-item label="商户">
              <el-select v-model="consume.merchantId" placeholder="选择商户" style="width:220px">
                <el-option v-for="m in merchants" :key="m.id" :label="m.merchantName" :value="m.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="金额">
              <el-input-number v-model="consume.amount" :min="1" :precision="2" style="width:130px" />
            </el-form-item>
            <el-form-item label="分类">
              <el-select v-model="consume.categoryCode" style="width:110px">
                <el-option label="餐饮" value="FOOD" />
                <el-option label="购物" value="SHOPPING" />
                <el-option label="出行" value="TRANSPORT" />
                <el-option label="娱乐" value="ENTERTAINMENT" />
                <el-option label="居住" value="HOUSING" />
                <el-option label="其他" value="OTHER" />
              </el-select>
            </el-form-item>
            <el-form-item label="商品">
              <el-input v-model="consume.subject" placeholder="如：文创笔记本×2" style="width:180px" />
            </el-form-item>
            <el-form-item>
              <el-button type="success" :loading="consuming" @click="submitConsume">下单并扫码支付</el-button>
            </el-form-item>
          </el-form>
          <div class="consume-tip">模拟下单：生成订单 → 弹收银台扫码（微信/支付宝/云闪付/工行）→ 支付成功自动展示订单</div>
        </div>
      </div>

      <el-table :data="orders" v-loading="loading" style="width:100%">
        <el-table-column prop="orderNo" label="订单号" width="200" />
        <el-table-column label="类型" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="bizTypeTag(row.bizType)" effect="plain">{{ row.bizTypeName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="subject" label="标题" min-width="160" show-overflow-tooltip />
        <el-table-column label="金额" width="110">
          <template #default="{ row }">
            <b style="color:#f56c6c">¥{{ row.amount }}</b>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="statusTag(row.status)">{{ row.statusName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="时间" width="150">
          <template #default="{ row }">{{ (row.createTime || '').slice(0, 19) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <template v-if="row.status === 'PENDING_PAY'">
              <el-button size="small" type="primary" @click="openCashier(row.orderNo)">去支付</el-button>
              <el-button size="small" plain @click="closeOrder(row)">关闭</el-button>
            </template>
            <template v-else-if="row.status === 'PAID'">
              <el-button size="small" type="warning" plain @click="handleRefund(row)">申请退款</el-button>
            </template>
            <span v-else style="color:#c0c4cc;font-size:12px">—</span>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && !orders.length" description="暂无订单" />

      <div class="pagination" v-if="total > 0">
        <el-pagination
          background
          layout="prev, pager, next"
          :total="total"
          :page-size="pageSize"
          :current-page="pageNum"
          @current-change="loadOrders"
        />
      </div>
    </el-card>

    <PayCashier v-model="cashierVisible" :order-no="cashierOrderNo" @paid="onPaid" />
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getOrders, closeOrder as apiCloseOrder, refundOrder, consumeOrder, getPayMerchants } from '@/api/pay'
import PayCashier from '@/components/PayCashier.vue'

const orders = ref([])
const total = ref(0)
const loading = ref(false)
const pageNum = ref(1)
const pageSize = 10
const statusFilter = ref('')

const cashierVisible = ref(false)
const cashierOrderNo = ref('')

// 青创集市消费
const consumeVisible = ref(false)
const merchants = ref([])
const consume = ref({ merchantId: null, amount: 88, categoryCode: 'SHOPPING', subject: '青创集市-物料采购' })
const consuming = ref(false)
const loadMerchants = async () => {
  try {
    merchants.value = await getPayMerchants()
    // 默认选中第一个商户，方便快速演示下单
    if (!consume.value.merchantId && merchants.value.length) consume.value.merchantId = merchants.value[0].id
  } catch (e) {}
}
const submitConsume = async () => {
  if (!consume.value.merchantId) { ElMessage.warning('请选择商户'); return }
  if (!consume.value.subject) { ElMessage.warning('请填写商品名称'); return }
  consuming.value = true
  try {
    const order = await consumeOrder(consume.value)
    cashierOrderNo.value = order.orderNo
    cashierVisible.value = true
  } catch (e) {} finally { consuming.value = false }
}

const loadOrders = async (page = 1) => {
  pageNum.value = page
  loading.value = true
  try {
    const res = await getOrders({ pageNum: pageNum.value, pageSize, status: statusFilter.value })
    orders.value = res.records || []
    total.value = Number(res.total || 0)
  } catch (e) {} finally { loading.value = false }
}

const openCashier = (orderNo) => { cashierOrderNo.value = orderNo; cashierVisible.value = true }
const onPaid = () => { loadOrders(pageNum.value) }

const closeOrder = async (row) => {
  try {
    await ElMessageBox.confirm(`确认关闭订单 ${row.orderNo}？`, '关闭订单', { type: 'warning' })
    await apiCloseOrder(row.orderNo)
    ElMessage.success('订单已关闭')
    loadOrders(pageNum.value)
  } catch (e) { if (e !== 'cancel') {} }
}

const handleRefund = async (row) => {
  try {
    await ElMessageBox.confirm(`确认退款 ¥${row.amount}？（模拟即时到账，原路退回钱包）`, '申请退款', { type: 'warning' })
    await refundOrder(row.orderNo, '用户主动申请退款')
    ElMessage.success('退款成功（模拟，已原路退回）')
    loadOrders(pageNum.value)
  } catch (e) { if (e !== 'cancel') {} }
}

const bizTypeTag = (t) => {
  const map = { GUARANTEE_FEE: 'warning', LOAN_REPAY: 'danger', ENTRUST_PAY: 'info', MERCHANT_CONSUME: 'success', RECHARGE: 'primary', LOAN_WITHDRAW: 'primary' }
  return map[t] || 'info'
}
const statusTag = (s) => {
  const map = { PENDING_PAY: 'warning', PAID: 'success', CLOSED: 'info', REFUNDED: 'danger' }
  return map[s] || 'info'
}

watch(statusFilter, () => loadOrders(1))
onMounted(() => { loadOrders(1); loadMerchants() })
</script>

<style scoped>
.orders-head { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 10px; }
.section-title { font-weight: 600; color: #303133; }
.pagination { display: flex; justify-content: flex-end; margin-top: 16px; }
.consume-entry { margin-bottom: 14px; }
.consume-form { background: #f5f7fa; border-radius: 8px; padding: 14px 14px 2px; margin-top: 10px; }
.consume-tip { font-size: 12px; color: #909399; margin-bottom: 10px; }
</style>
