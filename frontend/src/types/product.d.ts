export interface Product {
  id: number
  sku: string
  name: string
  type: string
  unit_price: number
  cost_price: number
  profit: number
  stock: number
  locked: number
  available: number
  status: string
}

export interface ProductForm {
  sku: string
  name: string
  type: string
  unit_price: number
  cost_price: number
  stock: number
  warehouse_id: number
}

export interface Pagination {
  page: number
  limit: number
  total: number
  pages: number
}
