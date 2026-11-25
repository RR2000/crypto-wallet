<script setup lang="ts">
import { useTheme } from 'vuetify'
import { login, logout, session } from './auth'
import { REPO_URL } from './config'

const theme = useTheme()
function toggleTheme(): void {
  theme.global.name.value = theme.global.current.value.dark ? 'light' : 'dark'
}
</script>

<template>
  <VApp>
    <VAppBar flat border="b" density="comfortable">
      <VAppBarTitle>
        <RouterLink to="/" class="brand">
          <VIcon icon="mdi-ethereum" color="primary" class="mr-1" />Crypto Wallet
        </RouterLink>
      </VAppBarTitle>
      <template v-if="session.profile">
        <VBtn to="/wallet" prepend-icon="mdi-wallet-outline" class="d-none d-sm-flex">Il mio wallet</VBtn>
        <VBtn to="/wallet" icon="mdi-wallet-outline" class="d-sm-none" aria-label="Il mio wallet" />
        <VBtn to="/account" prepend-icon="mdi-account-cog-outline" class="d-none d-sm-flex">Account</VBtn>
        <VBtn to="/account" icon="mdi-account-cog-outline" class="d-sm-none" aria-label="Account" />
      </template>
      <VBtn
        :icon="theme.global.current.value.dark ? 'mdi-weather-sunny' : 'mdi-weather-night'"
        aria-label="Tema chiaro o scuro"
        @click="toggleTheme"
      />
      <VBtn v-if="session.profile" icon="mdi-logout" aria-label="Esci" @click="logout" />
      <VBtn v-else-if="session.ready" color="primary" variant="flat" class="mr-2" @click="login">Accedi</VBtn>
    </VAppBar>

    <VMain>
      <VContainer class="page">
        <RouterView />
      </VContainer>
    </VMain>

    <VFooter class="justify-center text-medium-emphasis text-caption">
      Progetto di tesi, Università di Torino ·&nbsp;<a :href="REPO_URL" rel="noopener">codice su GitHub</a>
    </VFooter>
  </VApp>
</template>

<style>
.brand {
  color: inherit;
  text-decoration: none;
  font-weight: 600;
}
.page {
  max-width: 64rem;
}
/* le card raggiunte con un #ancora non finiscono sotto la barra in alto */
.scroll-target {
  scroll-margin-top: 5rem;
}
.mono {
  font-family: ui-monospace, 'Cascadia Mono', Menlo, monospace;
  overflow-wrap: anywhere;
}
</style>
