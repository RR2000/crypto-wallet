<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { login, session } from '../auth'
import { FAUCET_URL, NETWORK_NAME, REPO_URL, THESIS_URL } from '../config'
import { addressOf, balanceOf } from '../wallet'

const inUse = computed(() => {
  const k = session.wallet.keystores[session.wallet.inUse]
  return k ? { name: session.wallet.names[session.wallet.inUse], address: addressOf(k) } : null
})
const balance = ref('')

watch(inUse, async (a) => {
  balance.value = ''
  if (a) balance.value = await balanceOf(a.address).catch(() => 'non disponibile')
}, { immediate: true })
</script>

<template>
  <VRow>
    <VCol cols="12" md="7">
      <h1 class="text-h4 font-weight-bold mb-2">Un wallet Ethereum con solo email e password</h1>
      <p class="text-body-1 mb-4">
        Un provider OAuth2 / OpenID Connect che gestisce anche i wallet crittografici: accedi come su un sito
        qualsiasi e i tuoi account Ethereum sono già lì, su ogni dispositivo. Le chiavi private sono cifrate nel
        browser con la tua password, il server conserva solo dati cifrati.
      </p>
      <p class="text-body-2 text-medium-emphasis mb-6">
        È il progetto della mia tesi di laurea in Informatica all'Università di Torino, svolta in ALTEN.
        Rete: <strong>{{ NETWORK_NAME }}</strong>. Gli ETH qui non valgono nulla: non importare chiavi con fondi veri.
      </p>
      <div class="d-flex flex-wrap ga-3">
        <VBtn v-if="!session.profile" color="primary" variant="flat" size="large" prepend-icon="mdi-login" @click="login">
          Accedi o registrati
        </VBtn>
        <VBtn v-else color="primary" variant="flat" size="large" to="/wallet" prepend-icon="mdi-wallet-outline">
          Il mio wallet
        </VBtn>
        <VBtn :href="THESIS_URL" variant="tonal" size="large" prepend-icon="mdi-file-document-outline" rel="noopener">La tesi (PDF)</VBtn>
        <VBtn :href="REPO_URL" variant="text" size="large" prepend-icon="mdi-github" rel="noopener">Codice</VBtn>
      </div>
    </VCol>

    <VCol cols="12" md="5">
      <VCard v-if="session.profile" title="Account in uso" prepend-icon="mdi-account-circle-outline">
        <VCardText v-if="inUse">
          <div class="text-subtitle-1 font-weight-medium">{{ inUse.name }}</div>
          <div class="mono text-body-2 mb-2">{{ inUse.address }}</div>
          <div class="text-h5">{{ balance || '…' }} <span class="text-body-1">ETH</span></div>
        </VCardText>
        <VCardText v-else>
          Nessun account ancora: creane uno dalla pagina del wallet.
        </VCardText>
        <VCardActions>
          <VBtn to="/wallet">Gestisci</VBtn>
          <VBtn :href="FAUCET_URL" rel="noopener" prepend-icon="mdi-water-outline">ETH di prova</VBtn>
        </VCardActions>
      </VCard>
      <VCard v-else variant="tonal" title="Come funziona">
        <VCardText>
          <ol class="pl-4">
            <li>Accedi o registrati sul server di autenticazione (OAuth2 + PKCE).</li>
            <li>La web app riceve il tuo wallet cifrato insieme al profilo.</li>
            <li>Lo decifri nel browser con la password solo quando serve: per creare, importare o vedere una chiave.</li>
          </ol>
        </VCardText>
      </VCard>
    </VCol>
  </VRow>
</template>
