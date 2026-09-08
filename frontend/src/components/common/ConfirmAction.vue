<script setup>
defineProps({
  title: {
    type: String,
    default: '确认执行该操作？',
  },
  type: {
    type: String,
    default: 'warning',
  },
})

const emit = defineEmits(['confirm'])
</script>

<template>
  <el-popconfirm :title="title" @confirm="emit('confirm')">
    <template #reference>
      <slot>
        <el-button :type="type">确认</el-button>
      </slot>
    </template>
  </el-popconfirm>
</template>
