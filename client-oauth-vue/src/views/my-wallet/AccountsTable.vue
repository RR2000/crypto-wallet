<style>
.selected {
  color: rgb(145,85,253) !important;
}
</style>

<template>
  <VTable>
    <thead>
    <tr>
      <th class="text-center">In uso</th>
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
      class="text-center"
    >
      <td style="width: 0;">
        <VRadioGroup v-model="radios">
          <VRadio
            :value="index"
            @click="accountClicked(item, index)"/>
        </VRadioGroup>
      </td>
      <td class="text-center" style="width: 25%; min-width: 25%">
        <VTextField
          v-model="accountNames[index]"
        />
      </td>
      <td class="text-center"
          style="font-family: monospace; font-size: large"
          :class="{ 'selected': index === selectedWallet.index }">{{
          '0x' + item.address.toUpperCase()
        }}
      </td>
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
import {store} from "@/plugins/store";

export default {
  props: {
    radios: "",
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
      selectedWallet: store.getters.inUseWallet,
    }
  },
  methods: {
    accountClicked(item, index) {
      this.$emit('account-clicked', item, index)
      this.selectedWallet = store.getters.inUseWallet
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
