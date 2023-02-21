<script setup>
import upgradeBannerDark from '@/assets/images/pro/upgrade-banner-dark.png'
import upgradeBannerLight from '@/assets/images/pro/upgrade-banner-light.png'
import logo from '@/assets/logo.svg?raw'
import {
  VerticalNavLink,
  VerticalNavSectionTitle,
} from '@layouts'
import {useTheme} from 'vuetify'
import {cryptoUser} from "@/plugins/crypto-user-utils";

const vuetifyTheme = useTheme()
const upgradeBanner = computed(() => {
  return vuetifyTheme.global.name.value === 'light' ? upgradeBannerLight : upgradeBannerDark
})
let isLogged = ref(false)
try {
  cryptoUser.getUserInfo().then(function (user) {
    isLogged.value = user !== null
  })
}catch (e) {
  isLogged.value = false
}
</script>

<template>
  <!-- 👉 Nav header -->
  <div class="nav-header">
    <RouterLink
      to="/"
      class="app-logo d-flex align-center gap-x-3 app-title-wrapper"
    >
      <!-- ℹ️ You can also use img tag or VImg here -->
      <div v-html="logo"/>

      <Transition name="vertical-nav-app-title">
        <h1 class="font-weight-semibold leading-normal text-xl text-uppercase">
          Materio
        </h1>
      </Transition>
    </RouterLink>
  </div>

  <!-- 👉 Nav items -->
  <ul>
    <VerticalNavSectionTitle :item="{ heading: 'Pages' }"/>
    <VerticalNavLink
      :item="{
        title: 'Home Page',
        to: 'index',
        icon: { icon: 'mdi-home-outline' }
      }"
    />
    <VerticalNavLink
      v-if="isLogged"
      :item="{
        title: 'Il mio wallet',
        to: 'my-wallet',
        icon: { icon: 'mdi-wallet-outline' }
      }"
    />
    <VerticalNavLink
      v-if="isLogged"
      :item="{
        title: 'Impostazioni Account',
        to: 'account-settings',
        icon: { icon: 'mdi-account-cog-outline' }
      }"
    />
    <!-- 👉 Pages -->
    <VerticalNavLink
      v-if="!isLogged"
      :item="{
        title: 'Login',
        to: 'login',
        icon: { icon: 'mdi-login' }
      }"
    />
  </ul>
</template>

<style lang="scss">
// .v-navigation-drawer {
//   height: 100%;

//   .v-navigation-drawer__content {
//     display: flex;
//     flex-direction: column;

//     > ul {
//       flex-grow: 1;
//     }
//   }
// }

.upgrade-banner {
  margin-top: auto;
  // position: absolute;
  // bottom: 13px;
  // left: 50%;
  // transform: translateX(-50%);
}
</style>
