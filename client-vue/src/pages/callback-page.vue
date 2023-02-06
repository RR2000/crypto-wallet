<template>
  <h1>CALLBACK PAGE</h1>
  <div class="page-layout">
    <NavBar />
    <MobileNavBar />
    <div class="page-layout__content">
      <slot />
    </div>
  </div>
</template>

<script setup>
import NavBar from "../components/buttons/nav-bar-buttons.vue";
</script>