<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { login, session } from '../auth'
import { editAccounts, renameAndSelect, unlock } from '../actions'
import PasswordDialog from '../components/PasswordDialog.vue'
import { EXPLORER_URL, FAUCET_URL, NETWORK_NAME } from '../config'
import { addressOf, balanceOf, fromMnemonic, fromPrivateKey, newAccount } from '../wallet'

type Pending =
  | { kind: 'create' }
  | { kind: 'delete'; index: number }
  | { kind: 'show'; index: number }
  | { kind: 'privateKey' }
  | { kind: 'mnemonic' }

const busy = ref(false)
const error = ref('')
const done = ref('')
const pending = ref<Pending | null>(null)
const dialog = ref(false)
const shownKey = ref('')

const names = ref<string[]>([])
const balances = ref<Record<string, string>>({})
const privateKeyInput = ref('')
const mnemonicInput = ref('')
const mnemonicIndex = ref(0)

const accounts = computed(() => session.wallet.keystores.map((k, i) => ({ index: i, address: addressOf(k) })))
const namesChanged = computed(() => names.value.some((n, i) => n !== session.wallet.names[i]))

watch(() => session.wallet, async (w) => {
  names.value = [...w.names]
  for (const a of accounts.value) {
    if (balances.value[a.address] === undefined) {
      balances.value[a.address] = '…'
      balances.value[a.address] = await balanceOf(a.address).catch(() => 'n/d')
    }
  }
}, { immediate: true })

const dialogTitle = computed(() => {
  switch (pending.value?.kind) {
    case 'create': return 'Nuovo account'
    case 'delete': return 'Eliminare l\'account?'
    case 'show': return 'Mostrare la chiave privata?'
    default: return 'Importa account'
  }
})

function ask(p: Pending): void {
  error.value = ''
  done.value = ''
  pending.value = p
  dialog.value = true
}

async function run(label: string, job: () => Promise<void>): Promise<void> {
  busy.value = true
  error.value = ''
  done.value = ''
  try {
    await job()
    done.value = label
  } catch (e) {
    error.value = e instanceof Error ? e.message : String(e)
  } finally {
    busy.value = false
  }
}

async function onPassword(password: string): Promise<void> {
  const p = pending.value
  pending.value = null
  if (!p) return
  if (p.kind === 'show') {
    await run('', async () => {
      shownKey.value = (await unlock(password))[p.index].privateKey
    })
    return
  }
  if (p.kind === 'create') {
    await run('Account creato.', () => editAccounts(password, (list, n) => { list.push(newAccount()); n.push(`Account ${list.length}`) }))
  } else if (p.kind === 'delete') {
    await run('Account eliminato.', () => editAccounts(password, (list, n) => { list.splice(p.index, 1); n.splice(p.index, 1) }))
  } else {
    const account = p.kind === 'privateKey' ? () => fromPrivateKey(privateKeyInput.value) : () => fromMnemonic(mnemonicInput.value, mnemonicIndex.value)
    await run('Account importato.', () => editAccounts(password, (list, n) => {
      const a = account()
      if (list.some((x) => x.address === a.address)) throw new Error('Questo account è già nel wallet.')
      list.push(a)
      n.push(`Account ${list.length}`)
    }))
    privateKeyInput.value = ''
    mnemonicInput.value = ''
    mnemonicIndex.value = 0
  }
}

function saveNames(): Promise<void> {
  return run('Nomi salvati.', () => renameAndSelect(names.value.map((n, i) => n.trim() || `Account ${i + 1}`), session.wallet.inUse))
}

function select(index: number): Promise<void> {
  return run('', () => renameAndSelect(session.wallet.names, index))
}
</script>

<template>
  <div v-if="!session.ready" class="text-center py-16"><VProgressCircular indeterminate color="primary" /></div>

  <VCard v-else-if="!session.profile" title="Il mio wallet" class="mx-auto" max-width="32rem">
    <VCardText>Accedi per vedere e gestire i tuoi account.</VCardText>
    <VCardActions><VBtn color="primary" variant="flat" @click="login">Accedi</VBtn></VCardActions>
  </VCard>

  <template v-else>
    <div class="d-flex flex-wrap align-center ga-2 mb-4">
      <h1 class="text-h5 font-weight-bold mr-auto">Il mio wallet</h1>
      <VChip prepend-icon="mdi-lan" variant="tonal">{{ NETWORK_NAME }}</VChip>
      <VBtn :href="FAUCET_URL" rel="noopener" variant="text" prepend-icon="mdi-water-outline">ETH di prova</VBtn>
    </div>

    <VAlert v-if="error" type="error" :text="error" closable class="mb-4" @click:close="error = ''" />
    <VAlert v-if="done" type="success" :text="done" closable class="mb-4" @click:close="done = ''" />

    <VCard class="mb-6">
      <VCardText v-if="accounts.length === 0" class="text-body-1">
        Il wallet è vuoto: crea un account nuovo o importane uno qui sotto.
      </VCardText>
      <VList v-else lines="three" class="py-0">
        <template v-for="a in accounts" :key="a.address">
          <VDivider v-if="a.index > 0" />
          <VListItem class="py-3">
            <template #prepend>
              <VBtn
                :icon="a.index === session.wallet.inUse ? 'mdi-radiobox-marked' : 'mdi-radiobox-blank'"
                :color="a.index === session.wallet.inUse ? 'primary' : undefined"
                variant="text"
                :aria-label="`Usa ${names[a.index]}`"
                :disabled="busy"
                @click="select(a.index)"
              />
            </template>
            <VTextField v-model="names[a.index]" label="Nome" hide-details density="compact" class="mb-2" style="max-width: 20rem" />
            <div class="mono text-body-2">
              <a :href="EXPLORER_URL + a.address" rel="noopener" class="text-medium-emphasis">{{ a.address }}</a>
            </div>
            <div class="text-subtitle-1 font-weight-medium">{{ balances[a.address] ?? '…' }} ETH</div>
            <template #append>
              <div class="d-flex flex-column flex-sm-row">
                <VBtn icon="mdi-eye-outline" variant="text" aria-label="Mostra chiave privata" :disabled="busy" @click="ask({ kind: 'show', index: a.index })" />
                <VBtn icon="mdi-delete-outline" variant="text" color="error" aria-label="Elimina account" :disabled="busy" @click="ask({ kind: 'delete', index: a.index })" />
              </div>
            </template>
          </VListItem>
        </template>
      </VList>
      <VDivider />
      <VCardActions class="flex-wrap ga-2 pa-4">
        <VBtn color="primary" variant="flat" prepend-icon="mdi-plus" :loading="busy" @click="ask({ kind: 'create' })">Nuovo account</VBtn>
        <VBtn v-if="namesChanged" prepend-icon="mdi-content-save-outline" :disabled="busy" @click="saveNames">Salva i nomi</VBtn>
      </VCardActions>
    </VCard>

    <VRow>
      <VCol cols="12" md="6">
        <VCard title="Importa una chiave privata" prepend-icon="mdi-key-outline">
          <VCardText>
            <VTextField v-model="privateKeyInput" label="Chiave privata (0x…)" autocomplete="off" class="mono" />
          </VCardText>
          <VCardActions>
            <VBtn :disabled="!privateKeyInput || busy" @click="ask({ kind: 'privateKey' })">Importa</VBtn>
          </VCardActions>
        </VCard>
      </VCol>
      <VCol cols="12" md="6">
        <VCard title="Importa da frase mnemonica" prepend-icon="mdi-text-box-outline">
          <VCardText>
            <VTextarea v-model="mnemonicInput" label="Frase di 12 o 24 parole" rows="2" auto-grow variant="outlined" autocomplete="off" />
            <VTextField v-model.number="mnemonicIndex" label="Numero dell'account (0 = il primo)" type="number" min="0" />
          </VCardText>
          <VCardActions>
            <VBtn :disabled="!mnemonicInput || busy" @click="ask({ kind: 'mnemonic' })">Importa</VBtn>
          </VCardActions>
        </VCard>
      </VCol>
    </VRow>

    <PasswordDialog
      v-model="dialog"
      :title="dialogTitle"
      :danger="pending?.kind === 'delete' || pending?.kind === 'show'"
      :text="pending?.kind === 'show' ? 'Chi ha la chiave privata controlla l\'account: non mostrarla davanti ad altri.' : undefined"
      @confirm="onPassword"
    />

    <VDialog :model-value="!!shownKey" max-width="36rem" @update:model-value="shownKey = ''">
      <VCard title="Chiave privata">
        <VCardText class="mono">{{ shownKey }}</VCardText>
        <VCardActions><VSpacer /><VBtn @click="shownKey = ''">Chiudi</VBtn></VCardActions>
      </VCard>
    </VDialog>

    <VOverlay :model-value="busy" class="align-center justify-center" persistent>
      <VProgressCircular indeterminate color="primary" size="56" />
    </VOverlay>
  </template>
</template>
