<template>
  <div class="page-grid">
    <section class="page-header page-hero">
      <div class="page-hero-copy">
        <div class="page-kicker">售后审批</div>
        <h2>退货与退款处理台</h2>
        <p>集中处理退货审批、拒绝原因和退款完成状态，让售后链路保持清晰可追踪。</p>
        <div class="page-summary">
          <el-tag type="warning">待处理 {{ formatNumber(pendingCount) }}</el-tag>
          <el-tag>当前筛选 {{ currentLabel }}</el-tag>
          <el-tag>结果 {{ formatNumber(returns.length) }}</el-tag>
        </div>
      </div>
    </section>

    <section class="stats-grid">
      <el-card shadow="never" class="stat-card">
        <div class="stat-label">待处理申请</div>
        <div class="stat-value warning">{{ formatNumber(pendingCount) }}</div>
        <div class="stat-meta">需要人工审批</div>
      </el-card>
      <el-card shadow="never" class="stat-card">
        <div class="stat-label">已批准</div>
        <div class="stat-value accent">{{ formatNumber(approvedCount) }}</div>
        <div class="stat-meta">等待收货或退款确认</div>
      </el-card>
      <el-card shadow="never" class="stat-card">
        <div class="stat-label">已完成</div>
        <div class="stat-value success">{{ formatNumber(completedCount) }}</div>
        <div class="stat-meta">售后流程闭环</div>
      </el-card>
      <el-card shadow="never" class="stat-card">
        <div class="stat-label">退款金额</div>
        <div class="stat-value">{{ formatCurrency(totalRefundAmount) }}</div>
        <div class="stat-meta">基于当前列表统计</div>
      </el-card>
    </section>

    <el-card shadow="never" style="margin-bottom: 18px;">
      <el-radio-group v-model="filter" @change="applyFilter">
        <el-radio-button
          v-for="status in statusList"
          :key="status.value"
          :value="status.value"
        >
          {{ status.label }}
          <el-badge v-if="status.count" :value="status.count" :max="99" style="margin-left: 4px;" />
        </el-radio-button>
      </el-radio-group>
    </el-card>

    <div v-if="loading" v-loading="true" style="min-height: 220px;" class="el-loading-parent--relative" />
    <el-empty v-else-if="returns.length === 0" description="当前筛选下暂无退货申请" />

    <section v-else class="returns-grid">
      <el-card v-for="record in returns" :key="record.id" shadow="hover" class="return-card">
        <div class="return-card-top">
          <div>
            <div class="return-label">订单编号</div>
            <div class="return-order">{{ record.order_no }}</div>
          </div>
          <el-tag :type="statusTagType(record.status)">{{ record.status }}</el-tag>
        </div>

        <div class="return-amount">
          <div class="return-label">订单金额</div>
          <div class="return-amount-value">{{ formatCurrency(record.total_amount) }}</div>
        </div>

        <el-descriptions :column="1" size="small" border>
          <el-descriptions-item label="收货人">{{ record.receiver_name }}</el-descriptions-item>
          <el-descriptions-item label="申请时间">{{ record.created_at }}</el-descriptions-item>
          <el-descriptions-item label="退货原因">{{ record.reason }}</el-descriptions-item>
          <el-descriptions-item v-if="record.refund_amount !== null" label="退款金额">
            <span class="text-success">{{ formatCurrency(record.refund_amount) }}</span>
          </el-descriptions-item>
          <el-descriptions-item v-if="record.reject_reason" label="拒绝原因">
            <span class="text-danger">{{ record.reject_reason }}</span>
          </el-descriptions-item>
        </el-descriptions>

        <div class="return-actions" v-if="record.status === '待处理'">
          <el-button type="primary" @click="handleApprove(record)">批准退货</el-button>
          <el-button type="danger" @click="handleReject(record)">拒绝申请</el-button>
        </div>
        <div class="return-actions" v-else-if="record.status === '已批准'">
          <el-button type="primary" @click="handleComplete(record)">确认收货并退款</el-button>
        </div>
      </el-card>
    </section>

    <div v-if="pagination.total > 0" style="display: flex; justify-content: flex-end;">
      <el-pagination
        v-model:current-page="pagination.page"
        :page-size="pagination.limit"
        :total="pagination.total"
        layout="total, prev, pager, next"
        @current-change="changePage"
      />
    </div>

    <el-dialog v-model="showApproveModal" title="批准退货" width="500px" destroy-on-close>
      <el-descriptions :column="1" border>
        <el-descriptions-item label="订单号">{{ currentReturn?.order_no }}</el-descriptions-item>
        <el-descriptions-item label="订单总额">{{ formatCurrency(currentReturn?.total_amount) }}</el-descriptions-item>
      </el-descriptions>
      <el-form label-width="100px" style="margin-top: 18px;">
        <el-form-item label="退款金额">
          <el-input-number v-model="refundAmount" :precision="2" :min="0" :max="currentReturn?.total_amount" style="width: 100%;" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showApproveModal = false">取消</el-button>
        <el-button type="primary" @click="confirmApprove">确认批准</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showRejectModal" title="拒绝退货" width="500px" destroy-on-close>
      <el-descriptions :column="1" border>
        <el-descriptions-item label="订单号">{{ currentReturn?.order_no }}</el-descriptions-item>
      </el-descriptions>
      <el-form label-width="100px" style="margin-top: 18px;">
        <el-form-item label="拒绝原因">
          <el-input v-model="rejectReason" type="textarea" :rows="4" placeholder="请输入拒绝原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showRejectModal = false">取消</el-button>
        <el-button type="danger" @click="confirmReject">确认拒绝</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { approveReturn, completeReturn, getReturns, rejectReturn } from '@/api/modules/returns'
import { formatCurrency, formatNumber } from '@/utils/formatters'
import type { ReturnItem } from '@/types/api'

const returns = ref<ReturnItem[]>([])
const loading = ref(true)
const filter = ref('')
const pendingCount = ref(0)
const pagination = ref({ page: 1, limit: 20, total: 0, pages: 0 })

const showApproveModal = ref(false)
const showRejectModal = ref(false)
const currentReturn = ref<ReturnItem | null>(null)
const refundAmount = ref(0)
const rejectReason = ref('')

const approvedCount = computed(() => returns.value.filter((item) => item.status === '已批准').length)
const completedCount = computed(() => returns.value.filter((item) => item.status === '已完成').length)
const totalRefundAmount = computed(() => returns.value.reduce((sum, item) => sum + Number(item.refund_amount || 0), 0))

interface StatusOption {
  label: string
  value: string
  count?: number
}

const statusList = computed<StatusOption[]>(() => [
  { label: '全部', value: '' },
  { label: '待处理', value: '待处理', count: pendingCount.value },
  { label: '已批准', value: '已批准' },
  { label: '已拒绝', value: '已拒绝' },
  { label: '已完成', value: '已完成' },
])

const currentLabel = computed(() => {
  const current = statusList.value.find((item) => item.value === filter.value)
  return current?.label || '全部'
})

function statusTagType(status: string): '' | 'success' | 'warning' | 'info' | 'danger' {
  const map: Record<string, '' | 'success' | 'warning' | 'info' | 'danger'> = {
    '待处理': 'warning',
    '已批准': 'info',
    '已拒绝': 'danger',
    '已完成': 'success',
  }
  return map[status] || ''
}

async function loadReturns() {
  loading.value = true
  try {
    const { data } = await getReturns(pagination.value.page, pagination.value.limit, filter.value)
    returns.value = data.data
    pagination.value = data.pagination
    if (!filter.value) {
      pendingCount.value = data.data.filter((item: ReturnItem) => item.status === '待处理').length
    }
  } finally {
    loading.value = false
  }
}

function changePage(page: number) {
  pagination.value.page = page
  loadReturns()
}

function applyFilter(value: string) {
  filter.value = value
  loadReturns()
}

function handleApprove(record: ReturnItem) {
  currentReturn.value = record
  refundAmount.value = record.total_amount
  showApproveModal.value = true
}

async function confirmApprove() {
  try {
    await approveReturn(currentReturn.value!.id, refundAmount.value)
    showApproveModal.value = false
    loadReturns()
    ElMessage.success('退货申请已批准')
  } catch {
    // error toast handled by interceptor
  }
}

function handleReject(record: ReturnItem) {
  currentReturn.value = record
  rejectReason.value = ''
  showRejectModal.value = true
}

async function confirmReject() {
  if (!rejectReason.value.trim()) {
    ElMessage.warning('请输入拒绝原因')
    return
  }
  try {
    await rejectReturn(currentReturn.value!.id, rejectReason.value)
    showRejectModal.value = false
    loadReturns()
    ElMessage.success('退货申请已拒绝')
  } catch {
    // error toast handled by interceptor
  }
}

async function handleComplete(record: ReturnItem) {
  try {
    await ElMessageBox.confirm(
      `确认已收到退货商品并退款 ${formatCurrency(record.refund_amount)} 吗？`,
      '确认收货并退款',
      { confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning' },
    )
    await completeReturn(record.id)
    loadReturns()
    ElMessage.success('退款已完成')
  } catch (error: unknown) {
    if (error !== 'cancel') {
      // error toast handled by interceptor
    }
  }
}

onMounted(loadReturns)
</script>

<style scoped>
.returns-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(340px, 1fr));
  gap: 16px;
}

.return-card {
  display: flex;
  flex-direction: column;
}

.return-card-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 16px;
}

.return-label {
  margin-bottom: 6px;
  color: var(--el-text-color-secondary);
  font-size: 0.8rem;
  font-weight: 700;
}

.return-order {
  font-size: 1.05rem;
  font-weight: 700;
}

.return-amount {
  padding: 14px 16px;
  margin-bottom: 16px;
  border-radius: 8px;
  border: 1px solid var(--el-border-color-lighter);
  background: var(--el-fill-color-lighter);
}

.return-amount-value {
  margin-top: 6px;
  font-size: 1.65rem;
  font-weight: 700;
}

.return-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 16px;
}
</style>
