<script setup>
import {ethers} from 'ethers'
import {ref, onMounted} from 'vue'
import PasswordDialog from "@/views/my-wallet/PasswordDialog.vue"
import {cryptoUser, web3} from "@/plugins/crypto-user-utils"
import AccountsTable from "@/views/my-wallet/AccountsTable.vue"
import {store} from "@/plugins/store";

//useStore().commit('setSelectedAddress', 'address')
//let address = useStore().getters.selectedAddress

//const accountPrivateKey = '0x0000000000000000000000000000000000000000000000000000000000000000'

let isLoading = ref(false)
let showPrivateKeyDialog = ref(false)
let showPrivateKeyPasswordDialog = ref(false)
let showCreateAccountDialog = ref(false)
let showDeleteAccountDialog = ref(false)
let showImportPrivateKeyDialog = ref(false)
let showImportMnemonicDialog = ref(false)
let newPrivateKey = ref('')
let privateKeyToShow = ref('')
let mnemonicPhrase = ref('')
let mnemonicIndex = ref(0)
let walletTable = ref([])
let accountNames = ref([])
let walletToDelete = ref(null)

async function updateAccountNames() {
  isLoading.value = true
  try {
    const myRemoteWallet = await cryptoUser.retrieveEncryptedWallet()
    await cryptoUser.updateRemoteWallet(myRemoteWallet, accountNames.value)
  } catch (e) {
    alert("error while updating account: " + e)
  }
  isLoading.value = false
}

async function refreshMyWalletTable() {
  isLoading.value = true
  try {
    const encryptedWallet = await cryptoUser.retrieveEncryptedWallet()
    accountNames.value = await cryptoUser.retrieveNames()

    walletTable.value = await Promise.all(encryptedWallet.map(async wallet => {

      const balance = await web3.eth.getBalance(wallet["address"])
      const balanceInEther = web3.utils.fromWei(balance, 'ether')
      const balanceString = parseFloat(balanceInEther).toFixed(15)

      return {
        address: wallet.address,
        balance: balanceString,
      }
    }))
  } catch (err) {
    alert("error while refreshing table: " + err)
  }
  isLoading.value = false
}

async function createAccount(password) {
  isLoading.value = true
  try {
    const myRemoteWallet = await cryptoUser.retrieveDecryptedWallet(password)
    myRemoteWallet.add(web3.eth.accounts.create())

    await cryptoUser.updateRemoteWallet(myRemoteWallet, accountNames.value, password)
    await refreshMyWalletTable()
    cancelAllDialog()
  } catch (e) {
    alert("error while creating account: " + e)
  }
  isLoading.value = false
}

async function deleteAccount(password) {
  isLoading.value = true
  try {
    const myRemoteWallet = await cryptoUser.retrieveDecryptedWallet(password)
    myRemoteWallet.remove(walletToDelete.value)

    accountNames.value.splice(walletToDelete.value, 1)
    await cryptoUser.updateRemoteWallet(myRemoteWallet, accountNames.value, password)

    await refreshMyWalletTable()
    cancelAllDialog()
  } catch (e) {
    alert("error while deleting account: " + e)
  }
  isLoading.value = false
}


async function addPrivateKeyFromMnemonic(password) {
  isLoading.value = true
  try {
    const wallet = ethers.Wallet.fromPhrase(mnemonicPhrase.value)
    const hdPath = `m/44'/60'/0'/0/${mnemonicIndex.value}`
    const derivedWallet = wallet.derivePath(hdPath)

    const myRemoteWallet = await cryptoUser.retrieveDecryptedWallet(password)
    myRemoteWallet.add(web3.eth.accounts.privateKeyToAccount(derivedWallet.privateKey))

    await cryptoUser.updateRemoteWallet(myRemoteWallet, accountNames.value, password)
    await refreshMyWalletTable()
    cancelAllDialog()
  } catch (e) {
    alert("error while adding from mnemonic " + e)
  }
  isLoading.value = false
}

// Add a new private key to the wallet
async function addPrivateKey(password) {
  isLoading.value = true
  try {
    const myRemoteWallet = await cryptoUser.retrieveDecryptedWallet(password)

    myRemoteWallet.add(web3.eth.accounts.privateKeyToAccount(newPrivateKey.value))

    await cryptoUser.updateRemoteWallet(myRemoteWallet, accountNames.value, password)
    await refreshMyWalletTable()
    cancelAllDialog()
  } catch (e) {
    alert("error while adding private key " + e)
  }
  isLoading.value = false
}

function askCreateAccount() {
  showCreateAccountDialog.value = true
}

function askDeleteKey(index) {
  walletToDelete.value = index
  showDeleteAccountDialog.value = true
}

function askImportPrivateKey() {
  showImportPrivateKeyDialog.value = true
}

function askMnemonic() {
  showImportMnemonicDialog.value = true
}

let walletAddress

function askPrivateKey(clickedWallet) {
  walletAddress = clickedWallet
  showPrivateKeyPasswordDialog.value = true
}

function cancelAllDialog() {
  walletAddress = ''
  mnemonicPhrase.value = ''
  newPrivateKey.value = ''
  mnemonicIndex.value = 0
  walletToDelete.value = null
  showImportMnemonicDialog.value = false
  showPrivateKeyPasswordDialog.value = false
  showCreateAccountDialog.value = false
  showImportPrivateKeyDialog.value = false
  showDeleteAccountDialog.value = false
}

async function showPrivateKey(password) {
  isLoading.value = true
  try {
    const myRemoteWallet = await cryptoUser.retrieveDecryptedWallet(password)
    for (let i = 0; i < myRemoteWallet.length; i++) {
      if (
        web3.utils.toChecksumAddress(myRemoteWallet[i].address) ===
        web3.utils.toChecksumAddress(walletAddress)
      ) {
        privateKeyToShow.value = myRemoteWallet[i].privateKey
        showPrivateKeyDialog.value = true
        break
      }
    }
  } catch (e) {
    alert(e)
  }
  showPrivateKeyPasswordDialog.value = false
  isLoading.value = false
}

onMounted(async function () {
  await refreshMyWalletTable()
})

const numberRule = val => {
  if (val === '') {
    return 'Campo obbligatorio'
  } else if (val < 0) {
    return 'Il numero deve essere positivo!'
  } else if (!Number.isInteger(val)) {
    return 'Il numero deve essere intero!'
  }

  return true
}

async function onClickedAccount(item, index) {
  await store.commit("changeUseWallet", index)
}

//const selectedAddress = walletTable.value[selectedWallet].address;
</script>

<template>
  <VRow>
    <VCol>
      <VCard title="Il mio wallet">
        <AccountsTable
          :wallet-table="walletTable"
          :account-names="accountNames"
          @ask-private-key="askPrivateKey"
          @ask-delete-private-key="askDeleteKey"
          @update-account-names="updateAccountNames"
          @account-clicked="onClickedAccount"
        />
        <VCol class="d-flex justify-end">
          <VBtn @click="updateAccountNames">
            Conferma modifica
          </VBtn>
          <VBtn
            type="createAccount"
            class="ml-auto"
            @click="askCreateAccount"
          >
            Genera nuovo account
          </VBtn>
        </VCol>
      </VCard>
    </VCol>
  </VRow>
  <VRow>
    <VCol pa="6">
      <VCard title="Importa chiave privata">
        <VCardText>
          <VRow>
            <VCol>
              <VTextField
                v-model="newPrivateKey"
                label="Chiave privata"
                placeholder="0x0000000000000000000000000000000000000000000000000000000000000000"
              />
            </VCol>
          </VRow>
          <VRow>
            <VCol>
              <VBtn
                type="addPrivateKey"
                @click="askImportPrivateKey"
              >
                Importa
              </VBtn>
            </VCol>
          </VRow>
        </VCardText>
      </VCard>
    </VCol>
  </VRow>
  <VRow>
    <VCol pa="6">
      <VCard title="Importa da frase mnemonica">
        <VCardText>
          <VRow>
            <VCol>
              <VTextField
                v-model="mnemonicPhrase"
                label="Chiave mnemonica"
                placeholder="soda pledge remain avocado hurdle sword invest orphan soda hawk cannon balance"
              />
            </VCol>
          </VRow>
          <VRow v-if="false">
            <VCol>
              <VTextField
                v-model="mnemonicIndex"
                :rules="[numberRule]"
                label="Numero dell'account"
                type="number"
              />
            </VCol>
          </VRow>
          <VRow>
            <VCol>
              <VBtn
                type="addMnemonic"
                @click="askMnemonic"
              >
                Importa
              </VBtn>
            </VCol>
          </VRow>
        </VCardText>
      </VCard>
    </VCol>
  </VRow>
  <v-dialog v-model="isLoading" persistent>
    <v-card color="primary">
      <v-card-text class="text-center">
        <v-progress-circular indeterminate="true" color="white"></v-progress-circular>
        <span class="ml-2 white--text">Caricamento in corso...</span>
      </v-card-text>
    </v-card>
  </v-dialog>
  <PasswordDialog
    v-model="showPrivateKeyPasswordDialog"
    title="Sei sicuro di voler mostrare la chiave privata!?"
    @confirm="showPrivateKey"
    @cancel="cancelAllDialog"
  />
  <PasswordDialog
    v-model="showCreateAccountDialog"
    title="Inserisci la password per aggiungere un nuovo indirizzo"
    @confirm="createAccount"
    @cancel="cancelAllDialog"
  />
  <PasswordDialog
    v-model="showDeleteAccountDialog"
    title="Sicuro di voler eliminare l'account?"
    @confirm="deleteAccount"
    @cancel="cancelAllDialog"
  />
  <PasswordDialog
    v-model="showImportPrivateKeyDialog"
    title="Inserisci la password per aggiungere un nuovo indirizzo"
    @confirm="addPrivateKey"
    @cancel="cancelAllDialog"
  />
  <PasswordDialog
    v-model="showImportMnemonicDialog"
    title="Inserisci la password per aggiungere un nuovo indirizzo"
    @confirm="addPrivateKeyFromMnemonic"
    @cancel="cancelAllDialog"
  />
  <template>
    <VDialog
      v-model="showPrivateKeyDialog"
      persistent
      max-width="50%"
    >
      <VCard>
        <VCardTitle>
          <span class="headline">Private Key</span>
        </VCardTitle>
        <VCardText>
          <span>{{ privateKeyToShow }}</span>
        </VCardText>
        <VCardActions>
          <VBtn
            color="primary"
            text
            @click="showPrivateKeyDialog = false; privateKeyToShow=''"
          >
            Close
          </VBtn>
        </VCardActions>
      </VCard>
    </VDialog>
  </template>
</template>

