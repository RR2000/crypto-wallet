<script setup lang="ts">
// Chiede la password dell'account prima di un'operazione che tocca le chiavi.
import { computed, ref, watch } from 'vue'
import { session } from '../auth'
import { canResetWithoutOld, FORGOT_PASSWORD_LINK } from '../passwordHelp'

const open = defineModel<boolean>({ default: false })
defineProps<{ title: string; text?: string; danger?: boolean }>()
const emit = defineEmits<{ confirm: [password: string] }>()

const password = ref('')
const visible = ref(false)
// entrato con GitHub: se non ricorda la password puo' sceglierne una nuova senza la vecchia
const forgotLink = computed(() => canResetWithoutOld(session.profile))

watch(open, (v) => {
  if (!v) {
    password.value = ''
    visible.value = false
  }
})

function confirm(): void {
  if (!password.value) return
  const p = password.value
  open.value = false
  emit('confirm', p)
}
</script>

<template>
  <VDialog v-model="open" max-width="28rem">
    <VCard :title="title">
      <VCardText>
        <p v-if="text" class="mb-4">{{ text }}</p>
        <VTextField
          v-model="password"
          :type="visible ? 'text' : 'password'"
          :append-inner-icon="visible ? 'mdi-eye-off-outline' : 'mdi-eye-outline'"
          label="Password dell'account"
          autocomplete="current-password"
          autofocus
          @click:append-inner="visible = !visible"
          @keydown.enter="confirm"
        />
        <div v-if="forgotLink" class="text-body-2">
          <RouterLink :to="FORGOT_PASSWORD_LINK" @click="open = false">Password dimenticata?</RouterLink>
          <span class="text-medium-emphasis"> Sei entrato con GitHub: puoi sceglierne una nuova senza perdere gli account.</span>
        </div>
      </VCardText>
      <VCardActions>
        <VSpacer />
        <VBtn @click="open = false">Annulla</VBtn>
        <VBtn :color="danger ? 'error' : 'primary'" variant="flat" :disabled="!password" @click="confirm">Conferma</VBtn>
      </VCardActions>
    </VCard>
  </VDialog>
</template>
