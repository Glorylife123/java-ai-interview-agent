<template>
  <!-- 根组件仅负责路由出口；MainLayout 由受保护父路由按需提供。 -->
  <router-view />
</template>

<style>
:root {
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
  color: #303133;
  background: #f1f5f4;
}

* {
  box-sizing: border-box;
}

html,
body,
#app {
  min-width: 1200px;
  min-height: 100vh;
  margin: 0;
}
</style>
