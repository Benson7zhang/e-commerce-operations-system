import api from '../index'
import type { DashboardData, StatsData } from '@/types/api'

export const getStats = () => api.get<StatsData>('/stats')
export const getDashboard = () => api.get<DashboardData>('/stats/dashboard')
export const getOrdersByStatus = (status: string, page = 1, limit = 20) =>
  api.get(`/stats/orders/${status}`, { params: { page, limit } })
