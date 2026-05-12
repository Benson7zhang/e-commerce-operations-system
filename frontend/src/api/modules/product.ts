import api from '../index'
import type { Product } from '@/types/product'
import type { PaginatedResponse, Warehouse } from '@/types/api'

export const getProducts = (page = 1, limit = 20, search?: string | null, typeFilter?: string | null, status?: string | null) => {
  const params: Record<string, string> = { page: String(page), limit: String(limit) }
  if (search) params.search = search
  if (typeFilter) params.type_filter = typeFilter
  if (status) params.status = status
  return api.get<PaginatedResponse<Product>>('/products', { params })
}

export const getProductTypes = () => api.get<string[]>('/products/types')
export const createProduct = (data: Record<string, any>) => api.post('/products', data)
export const updateProduct = (id: number, data: Record<string, any>) => api.put(`/products/${id}`, data)
export const toggleProduct = (id: number) => api.put(`/products/${id}/toggle`)
export const deleteProduct = (id: number) => api.delete(`/products/${id}`)
export const getInventory = (id: number) => api.get(`/products/${id}/inventory`)
export const updateInventory = (id: number, warehouseId: number, quantity: number) =>
  api.put(`/products/${id}/inventory?warehouse_id=${warehouseId}&quantity=${quantity}`)
export const getWarehouses = () => api.get<Warehouse[]>('/warehouses')
