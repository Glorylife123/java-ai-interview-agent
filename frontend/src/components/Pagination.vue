<template>
  <!--
    分页公共组件：对 el-pagination 的轻封装。
    用法：<Pagination v-model:page="pageNum" v-model:size="pageSize" :total="total" @change="load" />
  -->
  <div class="pagination-wrap">
    <el-pagination
      background
      layout="total, sizes, prev, pager, next, jumper"
      :total="total"
      :current-page="page"
      :page-size="size"
      :page-sizes="[10, 20, 50, 100]"
      @current-change="onPageChange"
      @size-change="onSizeChange"
    />
  </div>
</template>

<script setup>
const props = defineProps({
  total: { type: Number, default: 0 },
  page: { type: Number, default: 1 },
  size: { type: Number, default: 10 },
})

// 通过 v-model:page / v-model:size 双向同步，并额外抛出 change 让父组件重新拉数据
const emit = defineEmits(['update:page', 'update:size', 'change'])

function onPageChange(p) {
  emit('update:page', p)
  emit('change')
}

function onSizeChange(s) {
  emit('update:size', s)
  // 改变每页条数时回到第 1 页，避免停留在越界页码
  emit('update:page', 1)
  emit('change')
}
</script>

<style scoped>
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
