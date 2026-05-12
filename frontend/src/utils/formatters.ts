const currencyFormatter = new Intl.NumberFormat('zh-CN', {
  style: 'currency',
  currency: 'CNY',
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
})

const numberFormatter = new Intl.NumberFormat('zh-CN', {
  maximumFractionDigits: 0,
})

export function formatCurrency(value: number | string | null | undefined): string {
  return currencyFormatter.format(Number(value || 0))
}

export function formatNumber(value: number | string | null | undefined): string {
  return numberFormatter.format(Number(value || 0))
}

export type TagType = '' | 'success' | 'warning' | 'info' | 'danger'

const orderStatusMap: Record<string, TagType> = {
  '已签收': 'success',
  '配送中': 'info',
  '滞留': 'danger',
  '已退货': 'warning',
  '申请退货': 'warning',
  '已取消': '',
}

const channelStatusMap: Record<string, TagType> = {
  PAID: 'info',
  SHIPPED: '',
  COMPLETED: 'success',
  CANCELLED: 'warning',
  REFUNDED: 'danger',
  PENDING: 'warning',
}

export function orderStatusType(status: string | undefined): TagType {
  return orderStatusMap[status || ''] || ''
}

export function channelStatusType(status: string | undefined): TagType {
  return channelStatusMap[status || ''] || ''
}

export function channelStatusLabel(status: string | undefined): string {
  const map: Record<string, string> = {
    PENDING: '待付款',
    PAID: '已付款',
    SHIPPED: '已发货',
    COMPLETED: '已完成',
    CANCELLED: '已取消',
    REFUNDED: '已退款',
  }
  return map[status || ''] || status || ''
}
