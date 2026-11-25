<script setup lang="ts">
// Chiede la password dell'account prima di un'operazione che tocca le chiavi.
import { ref, watch } from 'vue'

const open = defineModel<boolean>({ default: false })
defineProps<{ title: string; text?: string; danger?: boolean }>()
const emit = defineEmits<{ confirm: [password: string] }>()

const password = ref('')
const visible = ref(false)

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
      </VCardText>
      <VCardActions>
        <VSpacer />
        <VBtn @click="open = false">Annulla</VBtn>
        <VBtn :color="danger ? 'error' : 'primary'" variant="flat" :disabled="!password" @click="confirm">Conferma</VBtn>
      </VCardActions>
    </VCard>
  </VDialog>
</template>
