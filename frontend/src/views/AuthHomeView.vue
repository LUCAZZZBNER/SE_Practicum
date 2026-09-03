<template>
  <main class="auth-page">
    <section class="auth-panel">
      <div class="auth-intro">
        <h1>轻量级外卖服务平台</h1>
        <p>请选择身份进入系统。普通用户和商家使用独立注册入口，登录后进入各自功能区。</p>
      </div>

      <div class="auth-grid">
        <el-card shadow="never" class="auth-card">
          <h2>普通用户</h2>
          <p>浏览店铺、选择商品、管理购物车并查询订单。</p>
          <div class="auth-actions">
            <el-button type="primary" @click="$router.push('/login/customer')">用户登录</el-button>
            <el-button @click="$router.push('/register/customer')">用户注册</el-button>
          </div>
        </el-card>

        <el-card shadow="never" class="auth-card">
          <h2>商家</h2>
          <p>管理店铺状态、商品分类、商品信息和库存。</p>
          <div class="auth-actions">
            <el-button type="primary" @click="$router.push('/login/merchant')">商家登录</el-button>
            <el-button @click="$router.push('/register/merchant')">商家注册</el-button>
          </div>
        </el-card>
      </div>
    </section>
  </main>
</template>
