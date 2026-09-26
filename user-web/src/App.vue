<template>
  <router-view />
</template>

<script setup>
// 用户前端根组件 - 只渲染路由视图
// 具体布局由 MainLayout.vue 承载
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body, #app {
  width: 100%;
  height: 100%;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}
</style>
