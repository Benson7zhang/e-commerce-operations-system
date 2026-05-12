import api from '../index'
import type { ReturnItem } from '@/types/api'

export const getReturns = (page = 1, limit = 20, status?: string | null) =>
  api.get('/returns', { params: { page, limit, ...(status ? { status } : {}) } })
export const getReturnDetail = (id: number) => api.get(`/returns/${id}`)
export const approveReturn = (id: number, refundAmount: number) =>
  api.put(`/returns/${id}/approve`, { refund_amount: refundAmount })
export const rejectReturn = (id: number, reason: string) =>
  api.put(`/returns/${id}/reject`, { reject_reason: reason })
export const completeReturn = (id: number) => api.put(`/returns/${id}/complete`)
