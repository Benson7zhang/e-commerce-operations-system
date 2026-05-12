<template>
  <div>
    <el-table :data="data" v-loading="loading" stripe border style="width: 100%">
      <slot />
    </el-table>
    <div v-if="total && total > 0" style="margin-top: 16px; display: flex; justify-content: flex-end;">
      <el-pagination
        v-model:current-page="currentPage"
        :page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="$emit('page-change', $event)"
        @size-change="$emit('size-change', $event)"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  data: Record<string, unknown>[]
  loading?: boolean
  total?: number
  currentPage?: number
  pageSize?: number
}>()

defineEmits<{
  'page-change': [page: number]
  'size-change': [size: number]
}>()
</script>
