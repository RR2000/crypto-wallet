<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { features, login, logout, session } from '../auth'
import { changeAccountPassword, chooseFirstPassword, recoverLocked, resetForgottenPassword } from '../actions'
import { FORGOT_PASSWORD_LINK, offersReset } from '../passwordHelp'

const MIN = 8
const visible = ref(false)
const busy = ref(false)
const error = ref('')
const forgotOffer = ref(false)
const done = ref('')
// un modulo per card: i campi non si mescolano
type Fields = { current: string; next: string; confirm: string; old: string }
const empty = (): Fields => ({ current: '', next: '', confirm: '', old: '' })
const first = reactive(empty())
const changing = reactive(empty())
const forgot = reactive(empty())
const old = reactive(empty())

const fromGitHub = computed(() => session.profile?.login === 'github')
// password dimenticata via email: solo se il server puo' mandare posta
const byEmail = ref(false)
onMounted(async () => { byEmail.value = (await features()).passwordReset })
const type = computed(() => (visible.value ? 'text' : 'password'))

async function run(f: Fields, job: () => Promise<string>, checkNew = true): Promise<void> {
  error.value = ''
  forgotOffer.value = false
  done.value = ''
  if (checkNew && f.next.length < MIN) return void (error.value = `La nuova password deve avere almeno ${MIN} caratteri.`)
  if (checkNew && f.next !== f.confirm) return void (error.value = 'Le due password nuove non coincidono.')
  busy.value = true
  try {
    done.value = await job()
    Object.assign(f, empty())
  } catch (e) {
    error.value = e instanceof Error ? e.message : String(e)
    forgotOffer.value = offersReset(e)
  } finally {
    busy.value = false
  }
}

const choose = () => run(first, async () => { await chooseFirstPassword(first.next); return 'Password scelta: ora puoi creare e importare account.' })
const change = () => run(changing, async () => { await changeAccountPassword(changing.current, changing.next); return 'Password cambiata: il wallet ora è cifrato con quella nuova.' })
const reset = () => run(forgot, async () => {
  await resetForgottenPassword(forgot.next)
  return session.wallet.keystores.length
    ? 'Password nuova impostata. Gli account di prima restano nel wallet, bloccati: si riaprono qui sotto con la vecchia password.'
    : 'Password nuova impostata.'
})
const recover = () => run(old, async () => {
  const n = await recoverLocked(old.old, old.current)
  return n === 1 ? 'Un account sbloccato.' : `${n} account sbloccati.`
}, false)
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
          <p v-if="fromGitHub" class="text-body-2 text-medium-emphasis">Sei entrato con GitHub.</p>
        </VCardText>
      </VCard>
    </VCol>

    <VCol cols="12" md="7" class="d-flex flex-column ga-6">
      <VAlert v-if="error" type="error" closable @click:close="error = ''">
        {{ error }}
        <div v-if="forgotOffer" class="mt-3">
          <VBtn :to="FORGOT_PASSWORD_LINK" variant="outlined" prepend-icon="mdi-lock-question">Password dimenticata</VBtn>
        </div>
      </VAlert>
      <VAlert v-if="done" type="success" :text="done" closable @click:close="done = ''" />

      <!-- account arrivato da GitHub, ancora senza password -->
      <VCard v-if="!session.profile.hasPassword" title="Scegli una password" prepend-icon="mdi-lock-plus-outline">
        <VCardText>
          <p class="text-body-2 text-medium-emphasis mb-4">
            La password cifra le chiavi del wallet nel browser: il server non le vede mai. Serve anche per entrare
            senza GitHub.
          </p>
          <VTextField v-model="first.next" :type="type" label="Password" autocomplete="new-password" />
          <VTextField v-model="first.confirm" :type="type" label="Ripeti la password" autocomplete="new-password" />
          <VCheckbox v-model="visible" label="Mostra le password" hide-details density="compact" />
        </VCardText>
        <VCardActions>
          <VBtn color="primary" variant="flat" :loading="busy" :disabled="!first.next || !first.confirm" @click="choose">Scegli la password</VBtn>
        </VCardActions>
      </VCard>

      <template v-else>
        <VCard title="Cambia password" prepend-icon="mdi-lock-outline">
          <VCardText>
            <p class="text-body-2 text-medium-emphasis mb-4">
              La password protegge anche il wallet: cambiandola, gli account vengono cifrati di nuovo nel browser.
            </p>
            <VTextField v-model="changing.current" :type="type" label="Password attuale" autocomplete="current-password" />
            <VTextField v-model="changing.next" :type="type" label="Nuova password" autocomplete="new-password" />
            <VTextField v-model="changing.confirm" :type="type" label="Ripeti la nuova password" autocomplete="new-password" />
            <VCheckbox v-model="visible" label="Mostra le password" hide-details density="compact" />
          </VCardText>
          <VCardActions>
            <VBtn color="primary" variant="flat" :loading="busy" :disabled="!changing.current || !changing.next || !changing.confirm" @click="change">Cambia password</VBtn>
          </VCardActions>
        </VCard>

        <VCard id="password-dimenticata" title="Password dimenticata" prepend-icon="mdi-lock-question" class="scroll-target">
          <VCardText v-if="fromGitHub">
            <p class="text-body-2 text-medium-emphasis mb-4">
              Sei entrato con GitHub, quindi puoi scegliere una password nuova senza la vecchia. Non si cancella niente:
              gli account cifrati con la vecchia password restano nel wallet, bloccati, e si riaprono qui sotto se
              ti torna in mente.
            </p>
            <VTextField v-model="forgot.next" :type="type" label="Nuova password" autocomplete="new-password" />
            <VTextField v-model="forgot.confirm" :type="type" label="Ripeti la nuova password" autocomplete="new-password" />
          </VCardText>
          <VCardText v-else class="text-body-2">
            <p v-if="byEmail" class="mb-2">
              Esci e, nella pagina di accesso, scegli <strong>Forgot your password?</strong>: ti arriva per email un
              link per scegliere una password nuova. Non si cancella niente: gli account cifrati con la vecchia password
              restano nel wallet, bloccati, e si riaprono qui sotto se ti torna in mente.
            </p>
            {{ byEmail ? 'Oppure esci' : 'Esci' }} e rientra con <strong>Accedi con GitHub</strong>, con un account GitHub che ha la stessa email
            (verificata): da qui potrai scegliere una password nuova, senza perdere gli account.
          </VCardText>
          <VCardActions>
            <VBtn v-if="fromGitHub" color="primary" variant="flat" :loading="busy" :disabled="!forgot.next || !forgot.confirm" @click="reset">Imposta la nuova password</VBtn>
            <VBtn v-else prepend-icon="mdi-logout" @click="logout">Esci</VBtn>
          </VCardActions>
        </VCard>

        <VCard v-if="session.wallet.keystores.length" title="Sblocca account di prima" prepend-icon="mdi-lock-open-variant-outline">
          <VCardText>
            <p class="text-body-2 text-medium-emphasis mb-4">
              Account cifrati con una password vecchia: con quella si riaprono e vengono cifrati con la password attuale.
            </p>
            <VTextField v-model="old.old" :type="type" label="Password vecchia" autocomplete="off" />
            <VTextField v-model="old.current" :type="type" label="Password attuale" autocomplete="current-password" />
          </VCardText>
          <VCardActions>
            <VBtn :loading="busy" :disabled="!old.old || !old.current" @click="recover">Sblocca</VBtn>
          </VCardActions>
        </VCard>
      </template>
    </VCol>
  </VRow>
</template>
