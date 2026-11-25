<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { completeLogin, session } from '../auth'

const router = useRouter()
const error = ref('')

onMounted(async () => {
  try {
    await completeLogin()
    session.ready = true
    await router.replace('/wallet')
  } catch (e) {
    session.ready = true
    error.value = e instanceof Error ? e.message : String(e)
  }
})
</script>

<template>
  <div class="text-center py-16">
    <template v-if="error">
      <VAlert type="error" :text="`Accesso non riuscito: ${error}`" class="mb-4 text-left" />
      <VBtn to="/">Torna alla home</VBtn>
    </template>
    <VProgressCircular v-else indeterminate color="primary" />
  </div>
</template>
