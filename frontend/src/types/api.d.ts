export interface ApiResponse<T = any> {
  data: T
}

export interface PaginatedResponse<T> {
  data: T[]
  pagination: {
    page: number
    limit: number
    total: number
    pages: number
  }
}

export interface Warehouse {
  id: number
  name: string
  region: string
  address: string
  status: number
}

export interface Order {
  id: number
  order_no: string
  total_amount: number
  status: string
  receiver_name: string
  receiver_phone: string
  receiver_address: string
  tracking_no: string
  carrier: string
  current_node: string
  items_info: string
  created_at: string
  tracks?: Track[]
}

export interface Track {
  time: string
  remark: string
  status: string
}

export interface ReturnItem {
  id: number
  order_id: number
  order_no: string
  receiver_name: string
  total_amount: number
  reason: string
  status: string
  refund_amount: number | null
  reject_reason: string | null
  created_at: string
}

export interface DashboardData {
  today: { order_count: number; amount: number }
  pending: { to_ship: number; return_requests: number }
  alerts: { low_stock: number }
}

export interface StatsData {
  products: { total: number; on_sale: number }
  orders: Record<string, number>
  channel_orders: { total: number; total_amount: number; paid: number; shipped: number; completed: number }
  inventory: { total: number; locked: number; available: number; stock_value: number }
  finance: { revenue: number; refunded: number; net_revenue: number }
}
