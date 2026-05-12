<template>
  <div class="page-grid">
    <section class="page-header page-hero">
      <div class="page-hero-copy">
        <div class="page-kicker">订单与履约</div>
        <h2>订单履约工作台</h2>
        <p>围绕订单状态、履约节点与物流轨迹展开管理，支持模拟下单与全流程跟踪。</p>
        <div class="page-summary">
          <el-tag>订单 {{ formatNumber(pagination.total) }}</el-tag>
          <el-tag type="success">已签收 {{ formatNumber(signedCount) }}</el-tag>
          <el-tag type="primary">配送中 {{ formatNumber(transitCount) }}</el-tag>
        </div>
      </div>
      <div class="page-actions">
        <el-button type="primary" @click="showSimulate = true">模拟下单</el-button>
      </div>
    </section>

    <section class="stats-grid">
      <el-card shadow="never" class="stat-card">
        <div class="stat-label">当前页订单</div>
        <div class="stat-value">{{ formatNumber(orders.length) }}</div>
        <div class="stat-meta">便于快速抽样查看</div>
      </el-card>
      <el-card shadow="never" class="stat-card">
        <div class="stat-label">已签收</div>
        <div class="stat-value success">{{ formatNumber(signedCount) }}</div>
        <div class="stat-meta">履约完成订单</div>
      </el-card>
      <el-card shadow="never" class="stat-card">
        <div class="stat-label">配送中</div>
        <div class="stat-value accent">{{ formatNumber(transitCount) }}</div>
        <div class="stat-meta">需要重点跟踪运输节点</div>
      </el-card>
      <el-card shadow="never" class="stat-card">
        <div class="stat-label">当前页交易额</div>
        <div class="stat-value">{{ formatCurrency(pageAmount) }}</div>
        <div class="stat-meta">基于当前页订单汇总</div>
      </el-card>
    </section>

    <el-card shadow="never" style="margin-bottom: 18px;">
      <el-form :inline="true" @submit.prevent="handleSearch">
        <el-form-item>
          <el-input v-model="searchQuery" placeholder="搜索订单号、收货人或手机号" clearable @keyup.enter="handleSearch" style="width: 260px;" />
        </el-form-item>
        <el-form-item>
          <el-select v-model="statusFilter" placeholder="全部状态" clearable style="width: 140px;" @change="handleSearch">
            <el-option label="待支付" value="待支付" />
            <el-option label="已支付" value="已支付" />
            <el-option label="备货中" value="备货中" />
            <el-option label="已发货" value="已发货" />
            <el-option label="配送中" value="配送中" />
            <el-option label="已签收" value="已签收" />
            <el-option label="申请退货" value="申请退货" />
            <el-option label="退货中" value="退货中" />
            <el-option label="已退货" value="已退货" />
            <el-option label="已取消" value="已取消" />
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
            <div class="panel-title">订单列表</div>
            <div class="panel-subtitle">从列表直达订单详情，快速定位轨迹、收货信息和履约状态。</div>
          </div>
        </div>
      </template>

      <el-table :data="orders" v-loading="loading" stripe border style="width: 100%">
        <el-table-column prop="order_no" label="订单号" min-width="160">
          <template #default="{ row }">
            <el-button link type="primary" @click="viewOrder(row.order_no)">{{ row.order_no }}</el-button>
          </template>
        </el-table-column>
        <el-table-column prop="total_amount" label="金额" align="right" min-width="100">
          <template #default="{ row }">{{ formatCurrency(row.total_amount) }}</template>
        </el-table-column>
        <el-table-column prop="items_info" label="商品信息" min-width="160">
          <template #default="{ row }">{{ row.items_info || '-' }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" min-width="90">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="receiver_name" label="收货人" min-width="100" />
        <el-table-column prop="carrier" label="快递" min-width="90">
          <template #default="{ row }">{{ row.carrier || '-' }}</template>
        </el-table-column>
        <el-table-column prop="current_node" label="物流节点" min-width="120">
          <template #default="{ row }">{{ row.current_node || '-' }}</template>
        </el-table-column>
        <el-table-column prop="created_at" label="下单时间" min-width="160" />
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

    <el-dialog v-model="showSimulate" title="模拟下单" width="600px" destroy-on-close>
      <el-form :model="simForm" label-width="100px">
        <el-form-item label="选择商品">
          <el-select v-model="simForm.product_id" style="width: 100%;">
            <el-option
              v-for="product in products"
              :key="product.id"
              :label="`${product.name} / ${formatCurrency(product.unit_price)} / 库存 ${formatNumber(product.available)}`"
              :value="product.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="下单数量">
          <el-input-number v-model="simForm.quantity" :min="1" style="width: 100%;" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showSimulate = false">取消</el-button>
        <el-button type="primary" @click="handleSimulate">确认下单</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showDetail" :title="`订单详情 ${detail?.order_no}`" width="760px" destroy-on-close>
      <el-descriptions :column="1" border class="mb-4">
        <el-descriptions-item label="状态">
          <el-tag :type="statusType(detail?.status)" size="small">{{ detail?.status }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="收货人">{{ detail?.receiver_name }} {{ detail?.receiver_phone }}</el-descriptions-item>
        <el-descriptions-item label="收货地址">{{ detail?.receiver_address }}</el-descriptions-item>
        <el-descriptions-item label="快递信息">{{ detail?.carrier }} {{ detail?.tracking_no }}</el-descriptions-item>
        <el-descriptions-item label="订单金额">{{ formatCurrency(detail?.total_amount) }}</el-descriptions-item>
      </el-descriptions>

      <div class="panel-header">
        <div>
          <div class="panel-title">物流轨迹</div>
          <div class="panel-subtitle">按时间倒序查看运输节点。</div>
        </div>
      </div>
      <div class="timeline" v-if="detail?.tracks?.length">
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
    </el-dialog>

    <el-dialog v-model="showResult" title="下单成功" width="760px" destroy-on-close>
      <el-descriptions :column="1" border class="mb-4">
        <el-descriptions-item label="订单号">{{ result?.order_no }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusType(result?.status)" size="small">{{ result?.status }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="收货人">{{ result?.receiver_name }}</el-descriptions-item>
        <el-descriptions-item label="快递信息">{{ result?.carrier }} {{ result?.tracking_no }}</el-descriptions-item>
        <el-descriptions-item label="订单金额">{{ formatCurrency(result?.total_amount) }}</el-descriptions-item>
      </el-descriptions>
      <div class="panel-header">
        <div>
          <div class="panel-title">初始物流轨迹</div>
          <div class="panel-subtitle">模拟下单后自动生成的履约轨迹样本。</div>
        </div>
      </div>
      <div class="timeline">
        <div v-for="(track, index) in result?.tracks" :key="index" class="timeline-item">
          <div class="timeline-time">{{ track.time }}</div>
          <div class="timeline-content">{{ track.remark }}</div>
        </div>
      </div>
      <template #footer>
        <el-button type="primary" @click="closeResult">完成</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { getOrder, getOrders, simulateOrder } from '@/api/modules/order'
import { getProducts } from '@/api/modules/product'
import { formatCurrency, formatNumber, orderStatusType as statusType } from '@/utils/formatters'
import type { Order, Track } from '@/types/api'
import type { Product } from '@/types/product'

interface SimulateResult extends Order {
  tracks: Track[]
}

const orders = ref<Order[]>([])
const products = ref<Product[]>([])
const showSimulate = ref(false)
const showDetail = ref(false)
const showResult = ref(false)
const detail = ref<Order | null>(null)
const result = ref<SimulateResult | null>(null)
const loading = ref(false)
const simForm = ref({ product_id: 0, quantity: 1 })
const pagination = ref({
  page: 1,
  limit: 20,
  total: 0,
  pages: 0,
})

const searchQuery = ref('')
const statusFilter = ref('')

const signedCount = computed(() => orders.value.filter((item) => item.status === '已签收').length)
const transitCount = computed(() => orders.value.filter((item) => item.status === '配送中').length)
const pageAmount = computed(() => orders.value.reduce((sum, item) => sum + Number(item.total_amount || 0), 0))

async function loadOrders() {
  loading.value = true
  try {
    const response = await getOrders(
      pagination.value.page,
      pagination.value.limit,
      searchQuery.value || null,
      statusFilter.value || null,
    )
    orders.value = response.data.data
    pagination.value = response.data.pagination
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.value.page = 1
  loadOrders()
}

function clearSearch() {
  searchQuery.value = ''
  statusFilter.value = ''
  pagination.value.page = 1
  loadOrders()
}

async function loadProducts() {
  const response = await getProducts(1, 100)
  products.value = response.data.data.filter((item: Product) => item.status === '在售' && item.available > 0)
  if (products.value.length) {
    simForm.value.product_id = products.value[0].id
  }
}

async function viewOrder(orderNo: string) {
  const { data } = await getOrder(orderNo)
  detail.value = data
  showDetail.value = true
}

async function handleSimulate() {
  try {
    const { data } = await simulateOrder(simForm.value.product_id, simForm.value.quantity)
    result.value = data
    showSimulate.value = false
    showResult.value = true
  } catch {
    // error toast handled by interceptor
  }
}

function closeResult() {
  showResult.value = false
  loadOrders()
}

function changePage(page: number) {
  pagination.value.page = page
  loadOrders()
}

onMounted(() => {
  loadOrders()
  loadProducts()
})
</script>
