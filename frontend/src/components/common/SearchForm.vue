<template>
  <el-card shadow="never" style="margin-bottom: 18px;">
    <el-form :inline="true" @submit.prevent="$emit('search')">
      <slot />
      <el-form-item>
        <el-button type="primary" @click="$emit('search')">搜索</el-button>
        <el-button @click="$emit('reset')">清除</el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>

<script setup lang="ts">
defineEmits<{
  search: []
  reset: []
}>()
</script>
