import api from '../index'
import type { Warehouse } from '@/types/api'

export const getWarehouses = () => api.get<Warehouse[]>('/warehouses')
