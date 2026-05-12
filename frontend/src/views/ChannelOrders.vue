<template>
  <div class="page-grid">
    <section class="page-header page-hero">
      <div class="page-hero-copy">
        <div class="page-kicker">多平台聚合</div>
        <h2>渠道订单管理</h2>
        <p>聚合来自不同电商平台的销售订单，统一查看和管理。对接新平台只需实现适配器接口。</p>
        <div class="page-summary">
          <el-tag>渠道订单 {{ formatNumber(pagination.total) }}</el-tag>
          <el-tag v-for="adapter in adapters" :key="adapter.channel_code" :type="adapter.connected ? 'success' : 'danger'">
            {{ adapter.channel_name }} {{ adapter.connected ? '已连接' : '未连接' }}
          </el-tag>
        </div>
      </div>
      <div class="page-actions">
        <el-button type="primary" :loading="syncing" @click="handleSyncAll">全量同步</el-button>
      </div>
    </section>

    <section class="stats-grid">
      <el-card shadow="never" class="stat-card" v-for="adapter in adapters" :key="adapter.channel_code">
        <div class="stat-label">{{ adapter.channel_name }}</div>
        <div class="stat-value" :class="adapter.connected ? 'success' : 'danger'">
          {{ adapter.connected ? '在线' : '离线' }}
        </div>
        <div class="stat-meta">
          <el-button link type="primary" size="small" @click="handleSyncChannel(adapter.channel_code)">
            同步订单
          </el-button>
          <el-button link type="info" size="small" @click="handleTest(adapter.channel_code)">
            测试连通
          </el-button>
        </div>
      </el-card>
    </section>

    <el-card shadow="never" style="margin-bottom: 18px;">
      <el-form :inline="true" @submit.prevent="handleSearch">
        <el-form-item>
          <el-select v-model="channelFilter" placeholder="全部渠道" clearable style="width: 160px;" @change="handleSearch">
            <el-option v-for="adapter in adapters" :key="adapter.channel_code" :label="adapter.channel_name" :value="adapter.channel_code" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-select v-model="statusFilter" placeholder="全部状态" clearable style="width: 140px;" @change="handleSearch">
            <el-option label="待付款" value="PENDING" />
            <el-option label="已付款" value="PAID" />
            <el-option label="已发货" value="SHIPPED" />
            <el-option label="已完成" value="COMPLETED" />
            <el-option label="已取消" value="CANCELLED" />
            <el-option label="已退款" value="REFUNDED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="clearSearch">清除条件</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="panel-header">
          <div>
            <div class="panel-title">渠道订单列表</div>
            <div class="panel-subtitle">来自各电商平台的订单统一展示，按同步时间倒序排列。</div>
          </div>
        </div>
      </template>

      <el-table :data="orders" v-loading="loading" stripe border style="width: 100%">
        <el-table-column prop="platform_order_id" label="平台订单号" min-width="160">
          <template #default="{ row }">
            <el-button link type="primary" @click="viewOrder(row.id)">{{ row.platform_order_id }}</el-button>
          </template>
        </el-table-column>
        <el-table-column prop="channel_code" label="渠道" min-width="100">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ row.channel_code }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="total_amount" label="金额" align="right" min-width="100">
          <template #default="{ row }">{{ formatCurrency(row.total_amount) }}</template>
        </el-table-column>
        <el-table-column prop="items_summary" label="商品信息" min-width="180">
          <template #default="{ row }">{{ row.items_summary || '-' }}</template>
        </el-table-column>
        <el-table-column prop="unified_status" label="状态" min-width="90">
          <template #default="{ row }">
            <el-tag :type="statusType(row.unified_status)" size="small">{{ statusLabel(row.unified_status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="buyer_name" label="买家" min-width="100">
          <template #default="{ row }">{{ row.buyer_name || '-' }}</template>
        </el-table-column>
        <el-table-column prop="carrier" label="快递" min-width="100">
          <template #default="{ row }">{{ row.carrier || '-' }}</template>
        </el-table-column>
        <el-table-column prop="order_time" label="下单时间" min-width="160">
          <template #default="{ row }">{{ formatTime(row.order_time) }}</template>
        </el-table-column>
        <el-table-column prop="synced_at" label="同步时间" min-width="160">
          <template #default="{ row }">{{ formatTime(row.synced_at) }}</template>
        </el-table-column>
      </el-table>

      <div v-if="pagination.total > 0" style="margin-top: 16px; display: flex; justify-content: flex-end;">
        <el-pagination
          v-model:current-page="pagination.page"
          :page-size="pagination.limit"
          :total="pagination.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="changePage"
        />
      </div>
    </el-card>

    <el-dialog v-model="showDetail" :title="`渠道订单详情 #${detail?.platform_order_id}`" width="700px" destroy-on-close>
      <el-descriptions :column="1" border class="mb-4">
        <el-descriptions-item label="渠道">
          <el-tag type="info">{{ detail?.channel_code }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="平台订单号">{{ detail?.platform_order_id }}</el-descriptions-item>
        <el-descriptions-item label="统一状态">
          <el-tag :type="statusType(detail?.unified_status)" size="small">{{ statusLabel(detail?.unified_status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="平台原始状态">{{ detail?.platform_status }}</el-descriptions-item>
        <el-descriptions-item label="订单金额">{{ formatCurrency(detail?.total_amount) }}</el-descriptions-item>
        <el-descriptions-item label="买家信息">{{ detail?.buyer_name }} {{ detail?.buyer_phone }}</el-descriptions-item>
        <el-descriptions-item label="收货地址">{{ detail?.buyer_address }}</el-descriptions-item>
        <el-descriptions-item label="快递信息">{{ detail?.carrier || '-' }} {{ detail?.tracking_no || '' }}</el-descriptions-item>
        <el-descriptions-item label="商品摘要">{{ detail?.items_summary }}</el-descriptions-item>
        <el-descriptions-item label="下单时间">{{ formatTime(detail?.order_time) }}</el-descriptions-item>
        <el-descriptions-item label="同步时间">{{ formatTime(detail?.synced_at) }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getChannelOrders,
  getChannelOrder,
  getChannelAdapters,
  syncAllChannels,
  syncChannel,
  testChannelConnection,
} from '@/api/modules/channel'
import type { ChannelOrder, ChannelAdapter } from '@/api/modules/channel'
import { formatCurrency, formatNumber, channelStatusType as statusType, channelStatusLabel as statusLabel } from '@/utils/formatters'

const orders = ref<ChannelOrder[]>([])
const adapters = ref<ChannelAdapter[]>([])
const showDetail = ref(false)
const detail = ref<ChannelOrder | null>(null)
const loading = ref(false)
const syncing = ref(false)
const pagination = ref({ page: 1, limit: 20, total: 0, pages: 0 })
const channelFilter = ref('')
const statusFilter = ref('')

function formatTime(value: string | undefined): string {
  if (!value) return '-'
  try {
    return new Date(value).toLocaleString('zh-CN', {
      year: 'numeric', month: '2-digit', day: '2-digit',
      hour: '2-digit', minute: '2-digit',
    })
  } catch {
    return value
  }
}

async function loadOrders() {
  loading.value = true
  try {
    const response = await getChannelOrders(
      pagination.value.page,
      pagination.value.limit,
      channelFilter.value || null,
      statusFilter.value || null,
    )
    orders.value = response.data.data
    pagination.value = response.data.pagination
  } finally {
    loading.value = false
  }
}

async function loadAdapters() {
  try {
    const { data } = await getChannelAdapters()
    adapters.value = Array.isArray(data) ? data : []
  } catch {
    adapters.value = []
  }
}

function handleSearch() {
  pagination.value.page = 1
  loadOrders()
}

function clearSearch() {
  channelFilter.value = ''
  statusFilter.value = ''
  pagination.value.page = 1
  loadOrders()
}

function changePage(page: number) {
  pagination.value.page = page
  loadOrders()
}

async function viewOrder(id: number) {
  const { data } = await getChannelOrder(id)
  detail.value = data
  showDetail.value = true
}

async function handleSyncAll() {
  syncing.value = true
  try {
    await syncAllChannels()
    ElMessage.success('全量同步完成')
    loadOrders()
  } catch {
    // error toast handled by interceptor
  } finally {
    syncing.value = false
  }
}

async function handleSyncChannel(channelCode: string) {
  try {
    await syncChannel(channelCode)
    ElMessage.success(`${channelCode} 同步完成`)
    loadOrders()
  } catch {
    // error toast handled by interceptor
  }
}

async function handleTest(channelCode: string) {
  try {
    const { data } = await testChannelConnection(channelCode)
    if (data.connected) {
      ElMessage.success(`${data.channel_name} 连通正常`)
    } else {
      ElMessage.warning(`${data.channel_name} 连接失败`)
    }
    loadAdapters()
  } catch {
    // error toast handled by interceptor
  }
}

onMounted(() => {
  loadAdapters()
  loadOrders()
})
</script>
