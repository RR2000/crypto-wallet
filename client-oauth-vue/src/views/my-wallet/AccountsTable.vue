<template>
  <VTable>
    <thead>
      <tr>
        <th>Nome</th>
        <th>Chiave pubblica</th>
        <th>Bilancio</th>
        <th>Mostra chiave privata</th>
      </tr>
    </thead>
    <tbody>
      <tr
        v-for="(item, index) in walletTable"
        :key="item.address"
        :class="{ 'selected': item === selectedWallet }"
        @click="accountClicked(item)"
      >
        <td>
          <VTextField
            v-model="accountNames[index]"
            class="account-name"
          />
        </td>
        <td>{{ '0x' + item.address.toUpperCase() }}</td>
        <td>{{ item.balance }}</td>
        <td>
          <VBtn
            icon
            class="show-private-key"
            @click="askPrivateKey(item.address)"
          >
            <VIcon>mdi-eye</VIcon>
          </VBtn>
        </td>
      </tr>
    </tbody>
  </VTable>
</template>

<script>
export default {
  props: {
    walletTable: {
      type: Array,
      required: true,
    },
    accountNames: {
      type: Array,
      required: true,
    },
  },
  data() {
    return {
      selectedWallet: null,
    }
  },
  methods: {
    accountClicked(item) {
      this.selectedWallet = item
      this.$emit('account-clicked', item)
    },
    updateAccountNames() {
      this.$emit('update-account-names', this.accountNames)
    },
    askPrivateKey(address) {
      this.$emit('ask-private-key', address)
    },
  },
}
</script>
