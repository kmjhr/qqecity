<template>
  <div class="page orders-page">
    <el-card shadow="never">
      <template #header>
        <div class="orders-head">
          <span class="section-title">账单中心（模拟支付中心）</span>
          <el-radio-group v-model="statusFilter" size="small">
            <el-radio-button :value="''">全部</el-radio-button>
            <el-radio-button :value="'PENDING_PAY'">待支付</el-radio-button>
            <el-radio-button :value="'PAID'">已支付</el-radio-button>
            <el-radio-button :value="'CLOSED'">已关闭</el-radio-button>
            <el-radio-button :value="'REFUNDED'">已退款</el-radio-button>
          </el-radio-group>
        </div>
      </template>

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
              <span style="color:#c0c4cc;font-size:12px">已支付（不可退款）</span>
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
import { getOrders, closeOrder as apiCloseOrder } from '@/api/pay'
import PayCashier from '@/components/PayCashier.vue'

const orders = ref([])
const total = ref(0)
const loading = ref(false)
const pageNum = ref(1)
const pageSize = 10
const statusFilter = ref('')

const cashierVisible = ref(false)
const cashierOrderNo = ref('')

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

const bizTypeTag = (t) => {
  const map = { GUARANTEE_FEE: 'warning', LOAN_REPAY: 'danger', ENTRUST_PAY: 'info', MERCHANT_CONSUME: 'success', RECHARGE: 'primary', LOAN_WITHDRAW: 'primary' }
  return map[t] || 'info'
}
const statusTag = (s) => {
  const map = { PENDING_PAY: 'warning', PAID: 'success', CLOSED: 'info', REFUNDED: 'danger' }
  return map[s] || 'info'
}

watch(statusFilter, () => loadOrders(1))
onMounted(() => { loadOrders(1) })
</script>

<style scoped>
.orders-head { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 10px; }
.section-title { font-weight: 600; color: #303133; }
.pagination { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
