import api from '../index'
import type { PaginatedResponse } from '@/types/api'

export interface ChannelOrder {
  id: number
  channel_code: string
  platform_order_id: string
  unified_status: string
  platform_status: string
  total_amount: number
  buyer_name: string
  items_summary: string
  carrier: string
  tracking_no: string
  order_time: string
  synced_at: string
}

export interface ChannelAdapter {
  channel_code: string
  channel_name: string
  connected: boolean
}

export const getChannelOrders = (page = 1, limit = 20, channelCode?: string | null, status?: string | null) => {
  const params: Record<string, string> = { page: String(page), limit: String(limit) }
  if (channelCode) params.channelCode = channelCode
  if (status) params.status = status
  return api.get<PaginatedResponse<ChannelOrder>>('/channels/orders', { params })
}

export const getChannelOrder = (id: number) => api.get<ChannelOrder>(`/channels/orders/${id}`)

export const getChannelAdapters = () => api.get<ChannelAdapter[]>('/channels/adapters')

export const syncAllChannels = () => api.post('/channels/sync')

export const syncChannel = (channelCode: string) => api.post(`/channels/sync/${channelCode}`)

export const testChannelConnection = (channelCode: string) =>
  api.get<{ channel: string; channel_name: string; connected: boolean }>(`/channels/test/${channelCode}`)
