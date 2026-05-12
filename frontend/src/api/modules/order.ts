import api from '../index'
import type { Order, PaginatedResponse } from '@/types/api'

export const getOrders = (page = 1, limit = 20, search?: string | null, status?: string | null) => {
  const params: Record<string, string> = { page: String(page), limit: String(limit) }
  if (search) params.search = search
  if (status) params.status = status
  return api.get<PaginatedResponse<Order>>('/orders', { params })
}

export const getOrder = (orderNo: string) => api.get<Order>(`/orders/${orderNo}`)
export const simulateOrder = (productId: number, quantity: number) =>
  api.post('/orders/simulate', { product_id: productId, quantity })
