<script setup lang="ts">
import { ref } from 'vue'
import { login, session } from '../auth'
import { changeAccountPassword } from '../actions'

const current = ref('')
const next = ref('')
const confirm = ref('')
const visible = ref(false)
const busy = ref(false)
const error = ref('')
const done = ref('')

async function change(): Promise<void> {
  error.value = ''
  done.value = ''
  if (next.value.length < 8) return void (error.value = 'La nuova password deve avere almeno 8 caratteri.')
  if (next.value !== confirm.value) return void (error.value = 'Le due password nuove non coincidono.')
  busy.value = true
  try {
    await changeAccountPassword(current.value, next.value)
    current.value = next.value = confirm.value = ''
    done.value = 'Password cambiata: il wallet ora è cifrato con quella nuova.'
  } catch (e) {
    error.value = e instanceof Error ? e.message : String(e)
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <div v-if="!session.ready" class="text-center py-16"><VProgressCircular indeterminate color="primary" /></div>

  <VCard v-else-if="!session.profile" title="Account" class="mx-auto" max-width="32rem">
    <VCardText>Accedi per vedere il tuo account.</VCardText>
    <VCardActions><VBtn color="primary" variant="flat" @click="login">Accedi</VBtn></VCardActions>
  </VCard>

  <VRow v-else>
    <VCol cols="12" md="5">
      <VCard title="Profilo" prepend-icon="mdi-account-outline">
        <VCardText>
          <VTextField :model-value="session.profile.firstName" label="Nome" readonly />
          <VTextField :model-value="session.profile.lastName" label="Cognome" readonly />
          <VTextField :model-value="session.profile.email" label="Email" readonly />
        </VCardText>
      </VCard>
    </VCol>
    <VCol cols="12" md="7">
      <VCard title="Cambia password" prepend-icon="mdi-lock-outline">
        <VCardText>
          <p class="text-body-2 text-medium-emphasis mb-4">
            La password protegge anche il wallet: cambiandola, gli account vengono cifrati di nuovo nel browser.
          </p>
          <VAlert v-if="error" type="error" :text="error" class="mb-4" />
          <VAlert v-if="done" type="success" :text="done" class="mb-4" />
          <VTextField v-model="current" :type="visible ? 'text' : 'password'" label="Password attuale" autocomplete="current-password" />
          <VTextField v-model="next" :type="visible ? 'text' : 'password'" label="Nuova password" autocomplete="new-password" />
          <VTextField v-model="confirm" :type="visible ? 'text' : 'password'" label="Ripeti la nuova password" autocomplete="new-password" />
          <VCheckbox v-model="visible" label="Mostra le password" hide-details density="compact" />
        </VCardText>
        <VCardActions>
          <VBtn color="primary" variant="flat" :loading="busy" :disabled="!current || !next || !confirm" @click="change">Cambia password</VBtn>
        </VCardActions>
      </VCard>
    </VCol>
  </VRow>
</template>
