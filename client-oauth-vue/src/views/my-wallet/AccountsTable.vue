<template>
  <VTable>
    <thead>
      <tr>
        <th class="text-center">Nome</th>
        <th class="text-center">Chiave pubblica</th>
        <th class="text-center">Bilancio</th>
        <th class="text-center">Mostra chiave privata</th>
        <th class="text-center">Elimina</th>
      </tr>
    </thead>
    <tbody>
      <tr
        v-for="(item, index) in walletTable"
        :key="item.address"
      >
        <td class="text-center">
          <VTextField
            v-model="accountNames[index]"
          />
        </td>
        <td class="text-center"
            style="font-family: monospace; font-size: large"
            :class="{ 'selected': item === selectedWallet }"
            @click="accountClicked(item)">{{ '0x' + item.address.toUpperCase()
          }}</td>
        <td class="text-center">{{ item.balance }}</td>
        <td class="text-center">
          <VBtn
            icon="mdi-eye"
            @click="askPrivateKey(item.address)"
          />
        </td>
        <td class="text-center">
          <VBtn
            icon="mdi-bin"
            color="error"
            @click="askDeletePrivateKey(index)"
          />
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
    askDeletePrivateKey(index) {
      this.$emit('ask-delete-private-key', index)
    },
  },
}
</script>
