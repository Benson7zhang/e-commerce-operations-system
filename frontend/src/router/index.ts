import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      component: () => import('@/layouts/DefaultLayout.vue'),
      redirect: '/dashboard',
      children: [
        { path: 'dashboard', name: 'Dashboard', component: () => import('@/views/Dashboard.vue'), meta: { title: '运营总览' } },
        { path: 'products', name: 'Products', component: () => import('@/views/Products.vue'), meta: { title: '商品中心' } },
        { path: 'orders', name: 'Orders', component: () => import('@/views/Orders.vue'), meta: { title: '订单履约' } },
        { path: 'logistics', name: 'Logistics', component: () => import('@/views/Logistics.vue'), meta: { title: '物流跟踪' } },
        { path: 'returns', name: 'Returns', component: () => import('@/views/Returns.vue'), meta: { title: '售后处理' } },
        { path: 'channels', name: 'Channels', component: () => import('@/views/ChannelOrders.vue'), meta: { title: '渠道订单' } },
      ],
    },
    { path: '/:pathMatch(.*)*', name: 'NotFound', component: () => import('@/views/NotFound.vue') },
  ],
})

router.beforeEach((to, _from, next) => {
  document.title = `${(to.meta.title as string) || 'E-Mall'} - 电商运营中台`
  next()
})

export default router
