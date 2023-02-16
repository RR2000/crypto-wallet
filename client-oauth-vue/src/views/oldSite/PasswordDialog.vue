<template>
  <VDialog
    v-model="dialog"
    max-width="40%"
  >
    <VCard>
      <VCardTitle>
        {{ title }}
      </VCardTitle>
      <VCardText>
        <VTextField
          v-model="password"
          label="Password"
          type="password"
        />
      </VCardText>
      <VCardActions>
        <VBtn
          color="primary"
          :disabled="!password"
          @click="confirm"
        >
          Conferma
        </VBtn>
        <VBtn
          color="red"
          @click="cancel"
        >
          Annulla
        </VBtn>
      </VCardActions>
    </VCard>
  </VDialog>
</template>

<script>
export default {
  props: {
    title: {
      type: String,
      default: 'Password Dialog',
    },
  },
  data() {
    return {
      dialog: false,
      password: '',
    }
  },
  methods: {
    confirm() {
      this.dialog = false
      this.$emit('confirm', this.password)
      this.password = ''
    },
    cancel() {
      this.dialog = false
      this.$emit('cancel')
      this.password = ''
    },
  },
}
</script>
