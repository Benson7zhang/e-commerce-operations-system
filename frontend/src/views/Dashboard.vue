<template>
  <div class="page-grid">
    <section class="page-header page-hero">
      <div class="page-hero-copy">
        <div class="page-kicker">运营总览</div>
        <h2>核心业务看板</h2>
        <p>聚合营收、履约、库存和售后指标，帮助快速识别当日重点事项。</p>
        <div class="page-summary">
          <el-tag>商品 {{ formatNumber(stats.products?.total) }}</el-tag>
          <el-tag>订单 {{ formatNumber(stats.orders?.total) }}</el-tag>
          <el-tag type="success">净收入 {{ formatCurrency(stats.finance?.net_revenue) }}</el-tag>
        </div>
      </div>
      <div class="page-actions">
        <el-button @click="loadStats">刷新数据</el-button>
      </div>
    </section>

    <section class="stats-grid">
      <el-card shadow="never" class="stat-card">
        <div class="stat-label">商品资产</div>
        <div class="stat-value">{{ formatNumber(stats.products?.total) }}</div>
        <div class="stat-meta">在售商品 {{ formatNumber(stats.products?.on_sale) }}</div>
        <div class="stat-trend">覆盖当前可运营商品池</div>
      </el-card>
      <el-card shadow="never" class="stat-card">
        <div class="stat-label">可用库存</div>
        <div class="stat-value accent">{{ formatNumber(stats.inventory?.available) }}</div>
        <div class="stat-meta">锁定库存 {{ formatNumber(stats.inventory?.locked) }}</div>
        <div class="stat-trend">用于判断履约弹性</div>
      </el-card>
      <el-card shadow="never" class="stat-card">
        <div class="stat-label">订单总量</div>
        <div class="stat-value">{{ formatNumber(stats.orders?.total) }}</div>
        <div class="stat-meta">已签收 {{ formatNumber(stats.orders?.received) }}</div>
        <div class="stat-trend">配送中 {{ formatNumber(stats.orders?.in_transit) }}</div>
      </el-card>
      <el-card shadow="never" class="stat-card">
        <div class="stat-label">净收入</div>
        <div class="stat-value success">{{ formatCurrency(stats.finance?.net_revenue) }}</div>
        <div class="stat-meta">累计退款 {{ formatCurrency(stats.finance?.refunded) }}</div>
        <div class="stat-trend">营收 {{ formatCurrency(stats.finance?.revenue) }}</div>
      </el-card>
    </section>

    <section class="panel-grid">
      <el-card shadow="never">
        <template #header>
          <div class="panel-header">
            <div>
              <div class="panel-title">订单状态结构</div>
              <div class="panel-subtitle">点击卡片可查看对应状态的订单样本。</div>
            </div>
          </div>
        </template>
        <div class="stats-grid tight">
          <el-card shadow="hover" class="stat-card clickable" @click="showOrderDetails('已签收')">
            <div class="stat-label">已签收</div>
            <div class="stat-value success">{{ formatNumber(stats.orders?.received) }}</div>
            <div class="stat-meta">履约完成</div>
          </el-card>
          <el-card shadow="hover" class="stat-card clickable" @click="showOrderDetails('配送中')">
            <div class="stat-label">配送中</div>
            <div class="stat-value accent">{{ formatNumber(stats.orders?.in_transit) }}</div>
            <div class="stat-meta">运输链路活跃</div>
          </el-card>
          <el-card shadow="hover" class="stat-card clickable" @click="showOrderDetails('滞留')">
            <div class="stat-label">滞留</div>
            <div class="stat-value danger">{{ formatNumber(stats.orders?.detained) }}</div>
            <div class="stat-meta">需要物流排查</div>
          </el-card>
          <el-card shadow="hover" class="stat-card clickable" @click="showOrderDetails('已退货')">
            <div class="stat-label">已退货</div>
            <div class="stat-value warning">{{ formatNumber(stats.orders?.returned) }}</div>
            <div class="stat-meta">售后已闭环</div>
          </el-card>
        </div>
      </el-card>

      <el-card shadow="never">
        <template #header>
          <div class="panel-header">
            <div>
              <div class="panel-title">今日值守摘要</div>
              <div class="panel-subtitle">把当天需要关注的待办压缩成一眼能扫完的清单。</div>
            </div>
          </div>
        </template>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="今日订单">{{ formatNumber(dashboard.today?.order_count) }} 单 / {{ formatCurrency(dashboard.today?.amount) }}</el-descriptions-item>
          <el-descriptions-item label="待发货">{{ formatNumber(dashboard.pending?.to_ship) }} 单</el-descriptions-item>
          <el-descriptions-item label="退货申请">{{ formatNumber(dashboard.pending?.return_requests) }} 条</el-descriptions-item>
          <el-descriptions-item label="低库存">{{ formatNumber(dashboard.alerts?.low_stock) }} 个 SKU</el-descriptions-item>
        </el-descriptions>
      </el-card>
    </section>

    <el-dialog v-model="showDetailModal" :title="currentStatus + '订单样本'" width="1080px" destroy-on-close>
      <el-table :data="detailOrders" stripe border style="width: 100%">
        <el-table-column prop="order_no" label="订单号" min-width="140" />
        <el-table-column prop="total_amount" label="金额" align="right" min-width="100">
          <template #default="{ row }">{{ formatCurrency(row.total_amount) }}</template>
        </el-table-column>
        <el-table-column prop="items_info" label="商品信息" min-width="160">
          <template #default="{ row }">{{ row.items_info || '-' }}</template>
        </el-table-column>
        <el-table-column prop="receiver_name" label="收货人" min-width="100" />
        <el-table-column prop="carrier" label="快递" min-width="100">
          <template #default="{ row }">{{ row.carrier || '-' }}</template>
        </el-table-column>
        <el-table-column prop="current_node" label="物流节点" min-width="120">
          <template #default="{ row }">{{ row.current_node || '-' }}</template>
        </el-table-column>
        <el-table-column prop="created_at" label="下单时间" min-width="160" />
      </el-table>
      <div v-if="detailPagination.total > 0" style="margin-top: 16px; display: flex; justify-content: flex-end;">
        <el-pagination
          v-model:current-page="detailPagination.page"
          :page-size="detailPagination.limit"
          :total="detailPagination.total"
          layout="total, prev, pager, next"
          @current-change="changeDetailPage"
        />
      </div>
      <template #footer>
        <el-button @click="closeDetailModal">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getDashboard, getOrdersByStatus, getStats } from '@/api/modules/stats'
import { formatCurrency, formatNumber } from '@/utils/formatters'
import type { DashboardData, StatsData, Order } from '@/types/api'

const stats = ref<StatsData>({} as StatsData)
const dashboard = ref<DashboardData>({} as DashboardData)
const showDetailModal = ref(false)
const currentStatus = ref('')
const detailOrders = ref<Order[]>([])
const detailPagination = ref({
  page: 1,
  limit: 10,
  total: 0,
  pages: 0,
})

async function loadStats() {
  try {
    const [statsResponse, dashboardResponse] = await Promise.all([getStats(), getDashboard()])
    stats.value = statsResponse.data
    dashboard.value = dashboardResponse.data
  } catch {
    // error toast handled by interceptor
  }
}

async function showOrderDetails(status: string) {
  currentStatus.value = status
  detailPagination.value.page = 1
  await loadOrderDetails()
  showDetailModal.value = true
}

function closeDetailModal() {
  showDetailModal.value = false
  currentStatus.value = ''
  detailOrders.value = []
}

async function loadOrderDetails() {
  try {
    const response = await getOrdersByStatus(currentStatus.value, detailPagination.value.page, detailPagination.value.limit)
    detailOrders.value = response.data.data
    detailPagination.value = response.data.pagination
  } catch {
    // error toast handled by interceptor
  }
}

function changeDetailPage(page: number) {
  detailPagination.value.page = page
  loadOrderDetails()
}

onMounted(loadStats)
</script>
