<template>
  <div class="page-grid">
    <section class="page-header page-hero">
      <div class="page-hero-copy">
        <div class="page-kicker">物流追踪</div>
        <h2>运输节点查询台</h2>
        <p>按订单号快速定位物流轨迹，同时保留最近订单入口，支持从订单到物流的联动查询。</p>
        <div class="page-summary">
          <el-tag>最近订单 {{ formatNumber(pagination.total) }}</el-tag>
          <el-tag v-if="detail" type="primary">当前订单 {{ detail.order_no }}</el-tag>
        </div>
      </div>
    </section>

    <section class="panel-grid">
      <el-card shadow="never">
        <template #header>
          <div class="panel-header">
            <div>
              <div class="panel-title">物流检索</div>
              <div class="panel-subtitle">输入订单号即可直达对应的履约轨迹。</div>
            </div>
          </div>
        </template>
        <el-form :inline="true" @submit.prevent="search">
          <el-form-item style="flex: 1;">
            <el-input v-model="orderNo" placeholder="输入订单号，例如 JAVA-ORDER-001" clearable @keyup.enter="search" style="width: 100%;" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="search">查询订单</el-button>
          </el-form-item>
        </el-form>
        <el-alert v-if="searchError" :title="searchError" type="error" show-icon :closable="false" style="margin-top: 12px;" />

        <div v-if="!detail" style="margin-top: 18px;">
          <div class="panel-header">
            <div>
              <div class="panel-title">最近订单</div>
              <div class="panel-subtitle">适合没有具体订单号时快速抽样查看。</div>
            </div>
          </div>

          <el-table :data="recentOrders" v-loading="loading" stripe border style="width: 100%">
            <el-table-column prop="order_no" label="订单号" min-width="140" />
            <el-table-column prop="status" label="状态" min-width="90">
              <template #default="{ row }">
                <el-tag :type="statusType(row.status)" size="small">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="receiver_name" label="收货人" min-width="100" />
            <el-table-column label="操作" width="120">
              <template #default="{ row }">
                <el-button type="primary" link @click="searchByOrder(row.order_no)">查看轨迹</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div v-if="pagination.total > 0" style="margin-top: 16px; display: flex; justify-content: flex-end;">
            <el-pagination
              v-model:current-page="pagination.page"
              :page-size="pagination.limit"
              :total="pagination.total"
              layout="total, prev, pager, next"
              @current-change="changePage"
            />
          </div>
        </div>
      </el-card>

      <el-card shadow="never" v-if="detail">
        <template #header>
          <div class="panel-header">
            <div>
              <div class="panel-title">运输详情</div>
              <div class="panel-subtitle">展示当前订单的物流状态、金额和收货信息。</div>
            </div>
            <el-button @click="clearDetail">返回列表</el-button>
          </div>
        </template>

        <div class="stats-grid tight">
          <el-card shadow="never" class="stat-card">
            <div class="stat-label">订单号</div>
            <div class="stat-value" style="font-size: 1.2rem;">{{ detail.order_no }}</div>
            <div class="stat-meta">用于串联订单与物流</div>
          </el-card>
          <el-card shadow="never" class="stat-card">
            <div class="stat-label">状态</div>
            <div style="margin-top: 12px;">
              <el-tag :type="statusType(detail.status)">{{ detail.status }}</el-tag>
            </div>
            <div class="stat-meta">当前履约进度</div>
          </el-card>
          <el-card shadow="never" class="stat-card">
            <div class="stat-label">快递单号</div>
            <div class="stat-value" style="font-size: 1.2rem;">{{ detail.tracking_no || '-' }}</div>
            <div class="stat-meta">{{ detail.carrier || '待分配' }}</div>
          </el-card>
          <el-card shadow="never" class="stat-card">
            <div class="stat-label">订单金额</div>
            <div class="stat-value success">{{ formatCurrency(detail.total_amount) }}</div>
            <div class="stat-meta">用于售后和履约对账</div>
          </el-card>
        </div>

        <div style="margin-top: 18px;">
          <el-card shadow="never" style="margin-bottom: 18px;">
            <template #header>
              <div class="panel-title">收货信息</div>
            </template>
            <el-descriptions :column="1" border>
              <el-descriptions-item label="收货人">{{ detail.receiver_name }} {{ detail.receiver_phone }}</el-descriptions-item>
              <el-descriptions-item label="收货地址">{{ detail.receiver_address }}</el-descriptions-item>
            </el-descriptions>
          </el-card>

          <el-card shadow="never">
            <template #header>
              <div class="panel-header">
                <div>
                  <div class="panel-title">物流轨迹</div>
                  <div class="panel-subtitle">按运输节点展示当前订单的履约进度。</div>
                </div>
              </div>
            </template>
            <div class="timeline" v-if="detail.tracks?.length">
              <div
                v-for="(track, index) in detail.tracks"
                :key="index"
                class="timeline-item"
                :class="track.status === '滞留' ? 'danger' : track.status === '已到达' ? 'success' : ''"
              >
                <div class="timeline-time">{{ track.time }}</div>
                <div class="timeline-content">{{ track.remark }}</div>
              </div>
            </div>
            <el-empty v-else description="暂无轨迹信息" :image-size="80" />
          </el-card>
        </div>
      </el-card>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getOrder, getOrders } from '@/api/modules/order'
import { formatCurrency, formatNumber, orderStatusType as statusType } from '@/utils/formatters'
import type { Order } from '@/types/api'

const orderNo = ref('')
const detail = ref<Order | null>(null)
const recentOrders = ref<Order[]>([])
const searchError = ref('')
const loading = ref(false)
const pagination = ref({
  page: 1,
  limit: 10,
  total: 0,
  pages: 0,
})

async function search() {
  if (!orderNo.value.trim()) return
  try {
    searchError.value = ''
    const { data } = await getOrder(orderNo.value.trim())
    detail.value = data
  } catch {
    searchError.value = '未找到对应订单，请确认订单号是否正确。'
  }
}

function searchByOrder(value: string) {
  orderNo.value = value
  search()
}

async function loadRecentOrders() {
  loading.value = true
  try {
    const response = await getOrders(pagination.value.page, pagination.value.limit)
    recentOrders.value = response.data.data
    pagination.value = response.data.pagination
  } catch {
    // error toast handled by interceptor
  } finally {
    loading.value = false
  }
}

function clearDetail() {
  detail.value = null
  searchError.value = ''
}

function changePage(page: number) {
  pagination.value.page = page
  loadRecentOrders()
}

onMounted(loadRecentOrders)
</script>
