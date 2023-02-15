<template>
  <v-dialog v-model="dialog" max-width="400">
    <v-card>
      <v-card-title>
        {{ title }}
      </v-card-title>
      <v-card-text>
        <v-text-field v-model="password" label="Password" type="password"></v-text-field>
      </v-card-text>
      <v-card-actions>
        <v-btn color="primary" @click="confirm" :disabled="!password">
          Conferma
        </v-btn>
        <v-btn color="red" @click="cancel">
          Annulla
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
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
    };
  },
  methods: {
    confirm() {
      this.dialog = false;
      this.$emit('confirm', this.password);
    },
    cancel() {
      this.dialog = false;
      this.$emit('cancel');
    },
  },
};
</script>
