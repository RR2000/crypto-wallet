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
          :type="isPasswordVisible ? 'text' : 'password'"
          :append-inner-icon="isPasswordVisible ? 'mdi-eye-off-outline' : 'mdi-eye-outline'"
          label="Password"
          @click:append-inner="isPasswordVisible = !isPasswordVisible"
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
      isPasswordVisible: false
    }
  },
  methods: {
    confirm() {
      this.dialog = false
      this.isPasswordVisible = false
      this.$emit('confirm', this.password)
      this.password = ''
    },
    cancel() {
      this.dialog = false
      this.isPasswordVisible = false
      this.$emit('cancel')
      this.password = ''
    },
  },
}
</script>
