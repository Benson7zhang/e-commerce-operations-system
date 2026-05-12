import { ref, computed } from 'vue'
import type { Pagination } from '@/types/product'

export function usePagination(fetchFn: (page: number, limit: number) => Promise<void>) {
  const pagination = ref<Pagination>({ page: 1, limit: 20, total: 0, pages: 0 })
  const loading = ref(false)

  const pageNumbers = computed(() => {
    const current = pagination.value.page
    const total = pagination.value.pages
    const pages: (number | string)[] = []
    if (total <= 7) {
      for (let i = 1; i <= total; i++) pages.push(i)
      return pages
    }
    pages.push(1)
    if (current > 4) pages.push('...')
    for (let i = Math.max(2, current - 2); i <= Math.min(total - 1, current + 2); i++) pages.push(i)
    if (current < total - 3) pages.push('...')
    pages.push(total)
    return pages
  })

  async function loadData() {
    loading.value = true
    try {
      await fetchFn(pagination.value.page, pagination.value.limit)
    } finally {
      loading.value = false
    }
  }

  function changePage(page: number) {
    pagination.value.page = page
    loadData()
  }

  return { pagination, loading, pageNumbers, loadData, changePage }
}
