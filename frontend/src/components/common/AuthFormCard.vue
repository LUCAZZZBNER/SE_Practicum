<script setup>
defineProps({
  title: {
    type: String,
    required: true,
  },
})
</script>

<template>
  <main class="auth-page">
    <el-card shadow="never" class="auth-form-card">
      <h1>{{ title }}</h1>
      <slot />
    </el-card>
  </main>
</template>
