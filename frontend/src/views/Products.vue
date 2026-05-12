<template>
  <div class="page-grid">
    <section class="page-header page-hero">
      <div class="page-hero-copy">
        <div class="page-kicker">商品与库存</div>
        <h2>SKU 运营台</h2>
        <p>统一维护商品信息、定价结构和库存可用量，覆盖从新增商品到库存更新的完整链路。</p>
        <div class="page-summary">
          <el-tag>总商品 {{ formatNumber(pagination.total) }}</el-tag>
          <el-tag>当前页 {{ formatNumber(products.length) }}</el-tag>
          <el-tag>类型 {{ formatNumber(productTypes.length) }}</el-tag>
        </div>
      </div>
      <div class="page-actions">
        <el-button type="primary" @click="showAddModal = true">新增商品</el-button>
      </div>
    </section>

    <section class="stats-grid">
      <el-card shadow="never" class="stat-card">
        <div class="stat-label">在售商品</div>
        <div class="stat-value">{{ formatNumber(onSaleCount) }}</div>
        <div class="stat-meta">适合直接参与前台销售</div>
      </el-card>
      <el-card shadow="never" class="stat-card">
        <div class="stat-label">下架商品</div>
        <div class="stat-value warning">{{ formatNumber(offSaleCount) }}</div>
        <div class="stat-meta">待重新定价或补货</div>
      </el-card>
      <el-card shadow="never" class="stat-card">
        <div class="stat-label">低库存 SKU</div>
        <div class="stat-value danger">{{ formatNumber(lowStockCount) }}</div>
        <div class="stat-meta">可用库存少于 10</div>
      </el-card>
      <el-card shadow="never" class="stat-card">
        <div class="stat-label">平均毛利</div>
        <div class="stat-value success">{{ formatCurrency(avgProfit) }}</div>
        <div class="stat-meta">基于当前页样本计算</div>
      </el-card>
    </section>

    <el-card shadow="never" style="margin-bottom: 18px;">
      <el-form :inline="true" @submit.prevent="handleSearch">
        <el-form-item>
          <el-input v-model="searchQuery" placeholder="搜索 SKU 或商品名称" clearable @keyup.enter="handleSearch" style="width: 240px;" />
        </el-form-item>
        <el-form-item>
          <el-select v-model="typeFilter" placeholder="全部类型" clearable style="width: 140px;" @change="handleSearch">
            <el-option v-for="type in productTypes" :key="type" :label="type" :value="type" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-select v-model="statusFilter" placeholder="全部状态" clearable style="width: 140px;" @change="handleSearch">
            <el-option label="在售" value="在售" />
            <el-option label="下架" value="下架" />
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
            <div class="panel-title">商品列表</div>
            <div class="panel-subtitle">从商品信息、价格结构和库存状态三个维度快速查看当前商品池。</div>
          </div>
        </div>
      </template>

      <el-table :data="products" v-loading="loading" stripe border style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="sku" label="SKU" min-width="100" />
        <el-table-column prop="name" label="名称" min-width="140" />
        <el-table-column prop="type" label="类型" min-width="80" />
        <el-table-column prop="unit_price" label="售价" align="right" min-width="100">
          <template #default="{ row }">{{ formatCurrency(row.unit_price) }}</template>
        </el-table-column>
        <el-table-column prop="cost_price" label="成本" align="right" min-width="100">
          <template #default="{ row }">{{ formatCurrency(row.cost_price) }}</template>
        </el-table-column>
        <el-table-column prop="profit" label="毛利" align="right" min-width="100">
          <template #default="{ row }"><span class="text-success">{{ formatCurrency(row.profit) }}</span></template>
        </el-table-column>
        <el-table-column prop="available" label="可用库存" align="right" min-width="100">
          <template #default="{ row }">
            <span :class="row.available > 10 ? 'text-success' : row.available > 0 ? 'text-warning' : 'text-danger'">
              {{ formatNumber(row.available) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" min-width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === '在售' ? 'success' : 'warning'" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" min-width="200">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="openEditModal(row)">编辑</el-button>
            <el-button size="small" @click="handleToggle(row)">{{ row.status === '在售' ? '下架' : '上架' }}</el-button>
            <el-button type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
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

    <el-dialog v-model="showAddModal" title="新增商品" width="600px" destroy-on-close>
      <el-form :model="form" label-width="100px">
        <el-form-item label="SKU">
          <el-input v-model="form.sku" placeholder="例如 JAVA004" />
        </el-form-item>
        <el-form-item label="商品名称">
          <el-input v-model="form.name" placeholder="请输入商品名称" />
        </el-form-item>
        <el-form-item label="商品类型">
          <el-select v-model="form.type" style="width: 100%;">
            <el-option v-for="type in typeOptions" :key="type" :label="type" :value="type" />
          </el-select>
        </el-form-item>
        <el-form-item label="售价">
          <el-input-number v-model="form.unit_price" :precision="2" :min="0" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="成本价">
          <el-input-number v-model="form.cost_price" :precision="2" :min="0" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="初始库存">
          <el-input-number v-model="form.stock" :min="0" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="入库仓库">
          <el-select v-model="form.warehouse_id" style="width: 100%;">
            <el-option v-for="warehouse in warehouses" :key="warehouse.id" :label="`${warehouse.name} / ${warehouse.region}`" :value="warehouse.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddModal = false">取消</el-button>
        <el-button type="primary" @click="handleAdd">确认新增</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showEditModal" title="编辑商品" width="600px" destroy-on-close>
      <el-form :model="editForm" label-width="100px">
        <el-form-item label="SKU">
          <el-input v-model="editForm.sku" disabled />
        </el-form-item>
        <el-form-item label="商品名称">
          <el-input v-model="editForm.name" />
        </el-form-item>
        <el-form-item label="商品类型">
          <el-select v-model="editForm.type" style="width: 100%;">
            <el-option v-for="type in typeOptions" :key="type" :label="type" :value="type" />
          </el-select>
        </el-form-item>
        <el-form-item label="售价">
          <el-input-number v-model="editForm.unit_price" :precision="2" :min="0" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="成本价">
          <el-input-number v-model="editForm.cost_price" :precision="2" :min="0" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="预估毛利">
          <el-input :model-value="formatCurrency((editForm.unit_price || 0) - (editForm.cost_price || 0))" disabled />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditModal = false">取消</el-button>
        <el-button type="primary" @click="handleEdit">保存修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createProduct, deleteProduct, getProductTypes, getProducts, getWarehouses, toggleProduct, updateProduct } from '@/api/modules/product'
import { formatCurrency, formatNumber } from '@/utils/formatters'
import type { Product } from '@/types/product'
import type { Warehouse } from '@/types/api'

const products = ref<Product[]>([])
const productTypes = ref<string[]>([])
const warehouses = ref<Warehouse[]>([])
const showAddModal = ref(false)
const showEditModal = ref(false)
const loading = ref(false)
const pagination = ref({
  page: 1,
  limit: 20,
  total: 0,
  pages: 0,
})

const searchQuery = ref('')
const typeFilter = ref('')
const statusFilter = ref('')

const form = ref({
  sku: '',
  name: '',
  type: '通用',
  unit_price: 99,
  cost_price: 50,
  stock: 100,
  warehouse_id: 1,
})

const editForm = ref({
  id: 0,
  sku: '',
  name: '',
  type: '',
  unit_price: 0,
  cost_price: 0,
})

const typeOptions = computed(() => (productTypes.value.length ? productTypes.value : ['通用']))
const onSaleCount = computed(() => products.value.filter((item) => item.status === '在售').length)
const offSaleCount = computed(() => products.value.filter((item) => item.status !== '在售').length)
const lowStockCount = computed(() => products.value.filter((item) => item.available <= 10).length)
const avgProfit = computed(() => {
  if (!products.value.length) return 0
  const total = products.value.reduce((sum, item) => sum + Number(item.profit || 0), 0)
  return total / products.value.length
})

async function loadProducts() {
  loading.value = true
  try {
    const response = await getProducts(
      pagination.value.page,
      pagination.value.limit,
      searchQuery.value || null,
      typeFilter.value || null,
      statusFilter.value || null,
    )
    products.value = response.data.data
    pagination.value = response.data.pagination
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.value.page = 1
  loadProducts()
}

function clearSearch() {
  searchQuery.value = ''
  typeFilter.value = ''
  statusFilter.value = ''
  pagination.value.page = 1
  loadProducts()
}

async function loadOptions() {
  const [typesResponse, warehousesResponse] = await Promise.all([getProductTypes(), getWarehouses()])
  productTypes.value = typesResponse.data
  warehouses.value = warehousesResponse.data.filter((item: Warehouse) => item.status === 1)
  if (!form.value.type && productTypes.value.length) {
    form.value.type = productTypes.value[0]
  }
  if (warehouses.value.length) {
    form.value.warehouse_id = warehouses.value[0].id
  }
}

async function handleAdd() {
  try {
    await createProduct(form.value)
    showAddModal.value = false
    form.value = {
      sku: '',
      name: '',
      type: typeOptions.value[0] || '通用',
      unit_price: 99,
      cost_price: 50,
      stock: 100,
      warehouse_id: warehouses.value[0]?.id || 1,
    }
    loadProducts()
    ElMessage.success('商品添加成功')
  } catch {
    // error toast handled by interceptor
  }
}

async function handleToggle(product: Product) {
  await toggleProduct(product.id)
  loadProducts()
}

async function handleDelete(product: Product) {
  try {
    await ElMessageBox.confirm(`确认删除商品 [${product.name}] 吗？`, '确认删除', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await deleteProduct(product.id)
    loadProducts()
    ElMessage.success('删除成功')
  } catch (error: unknown) {
    if (error !== 'cancel') {
      // error toast handled by interceptor
    }
  }
}

function openEditModal(product: Product) {
  editForm.value = {
    id: product.id,
    sku: product.sku,
    name: product.name,
    type: product.type,
    unit_price: product.unit_price,
    cost_price: product.cost_price,
  }
  showEditModal.value = true
}

async function handleEdit() {
  try {
    await updateProduct(editForm.value.id, {
      name: editForm.value.name,
      type: editForm.value.type,
      unit_price: editForm.value.unit_price,
      cost_price: editForm.value.cost_price,
    })
    showEditModal.value = false
    loadProducts()
    ElMessage.success('修改成功')
  } catch {
    // error toast handled by interceptor
  }
}

function changePage(page: number) {
  pagination.value.page = page
  loadProducts()
}

onMounted(() => {
  loadProducts()
  loadOptions()
})
</script>
