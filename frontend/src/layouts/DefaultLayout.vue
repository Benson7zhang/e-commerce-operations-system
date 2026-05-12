<template>
  <div class="app-shell">
    <aside class="sidebar">
      <div class="brand-block">
        <div class="brand-kicker">E-Mall Operations</div>
        <div class="brand-title">电商运营中台</div>
        <div class="brand-subtitle">商品、履约、仓储、售后统一视图</div>
      </div>

      <div class="sidebar-section">
        <div class="sidebar-label">导航</div>
        <el-menu
          :default-active="route.path"
          :router="true"
          class="sidebar-menu"
          background-color="transparent"
          text-color="rgba(237, 243, 251, 0.72)"
          active-text-color="#ffffff"
        >
          <el-menu-item
            v-for="item in navItems"
            :key="item.path"
            :index="item.path"
            class="nav-item-custom"
          >
            <el-icon :size="20"><component :is="item.icon" /></el-icon>
            <template #title>
              <div class="nav-copy">
                <span class="nav-title">{{ item.label }}</span>
                <span class="nav-description">{{ item.description }}</span>
              </div>
            </template>
          </el-menu-item>
        </el-menu>
      </div>

      <div class="sidebar-section sidebar-footnote">
        <div class="sidebar-label">当前后端</div>
        <div class="environment-card">
          <div class="environment-title">Java Microservices</div>
          <div class="environment-meta">Gateway 9000 / Frontend 5173</div>
        </div>
      </div>
    </aside>

    <main id="main-panel" class="main-content">
      <div class="topbar">
        <div>
          <div class="topbar-title">{{ currentSection.title }}</div>
          <div class="topbar-subtitle">{{ currentSection.subtitle }}</div>
        </div>
        <div class="topbar-meta">
          <el-tag>开发环境</el-tag>
          <el-tag type="primary">Spring Cloud Gateway</el-tag>
        </div>
      </div>

      <div class="content-shell">
        <router-view />
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

const navItems = [
  { path: '/dashboard', label: '统计面板', description: '核心指标与异常信号', icon: 'DataAnalysis' },
  { path: '/products', label: '商品管理', description: 'SKU、价格与库存策略', icon: 'ShoppingBag' },
  { path: '/orders', label: '订单管理', description: '履约进度与客户订单', icon: 'Document' },
  { path: '/logistics', label: '物流查询', description: '轨迹节点与运输状态', icon: 'Van' },
  { path: '/returns', label: '退货管理', description: '售后审批与退款处理', icon: 'RefreshLeft' },
  { path: '/channels', label: '渠道订单', description: '多平台订单聚合管理', icon: 'Connection' },
]

const sectionMap: Record<string, { title: string; subtitle: string }> = {
  '/dashboard': { title: '运营总览', subtitle: '聚合指标、履约状态和业务风险信号。' },
  '/products': { title: '商品中心', subtitle: '围绕商品生命周期、价格和库存可用性展开管理。' },
  '/orders': { title: '订单履约', subtitle: '追踪订单状态、物流链路和模拟下单结果。' },
  '/logistics': { title: '物流跟踪', subtitle: '快速查询轨迹节点并定位异常运输订单。' },
  '/returns': { title: '售后处理', subtitle: '集中处理退货审批、退款和售后进度。' },
  '/channels': { title: '渠道订单', subtitle: '聚合多平台销售订单，统一管理外部渠道履约。' },
}

const currentSection = computed(() => sectionMap[route.path] || sectionMap['/dashboard'])
</script>

<style scoped>
.sidebar-menu {
  border-right: none;
}

.sidebar-menu .el-menu-item {
  height: auto;
  min-height: 56px;
  padding: 10px 12px !important;
  margin-bottom: 6px;
  border-radius: 8px;
  line-height: normal;
}

.sidebar-menu .el-menu-item:hover {
  background: rgba(255, 255, 255, 0.06) !important;
}

.sidebar-menu .el-menu-item.is-active {
  background: rgba(31, 99, 224, 0.16) !important;
  box-shadow: inset 0 0 0 1px rgba(99, 153, 247, 0.2);
}

.nav-copy {
  display: grid;
  gap: 2px;
  margin-left: 12px;
}

.nav-title {
  font-size: 0.93rem;
  font-weight: 600;
}

.nav-description {
  font-size: 0.78rem;
  color: rgba(237, 243, 251, 0.56);
}
</style>
