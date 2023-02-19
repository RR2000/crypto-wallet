<script setup>
import {ethers} from 'ethers'
import {ref, onMounted} from 'vue'
import Web3 from 'web3'
import axios from 'axios'
import PasswordDialog from "@/views/my-wallet/PasswordDialog.vue"
import {cryptoUserUtils} from "@/plugins/crypto-user-utils"
import AccountsTable from "@/views/my-wallet/AccountsTable.vue"

function chainUrl(arg) {
  if (arg === 0)
    return 'http://localhost:8545'
  else if (arg === 1)
    return 'https://bsc-dataseed1.binance.org:443'
}

//useStore().commit('setSelectedAddress', 'address')
//let address = useStore().getters.selectedAddress

//alert(address)
//const accountPrivateKey = '0x0000000000000000000000000000000000000000000000000000000000000000'

const web3 = new Web3(new Web3.providers.HttpProvider(chainUrl(1)))

let showPrivateKeyDialog = ref(false)
let showPrivateKeyPasswordDialog = ref(false)
let showCreateAccountDialog = ref(false)
let showImportPrivateKeyDialog = ref(false)
let showImportMnemonicDialog = ref(false)
let selectedWallet = ref(null)
let newPrivateKey = ref('')
let privateKeyToShow = ref('')
let mnemonicPhrase = ref('')
let mnemonicIndex = ref(0)
let walletTable = ref([])
let accountNames = ref([])

async function updateAccountNames() {
  let encryptedWallet = await cryptoUserUtils.retrieveEncryptedWallet()

  const formData = new FormData()
  formData.append('private_key', JSON.stringify([encryptedWallet, accountNames.value]))
  try {
    const response = await axios.post('http://127.0.0.1:9000/updateWallet', formData)
    console.log('Private key added:', response)
  } catch (error) {
    console.error('Error adding private key:', error)
  }
  await refreshMyWalletTable()
  await cryptoUserUtils.retrieveNames()
}

async function refreshMyWalletTable() {
  const encryptedWallet = await cryptoUserUtils.retrieveEncryptedWallet()
  accountNames.value = await cryptoUserUtils.retrieveNames()
  walletTable.value = await Promise.all(encryptedWallet.map(async wallet => {
    const balance = await web3.eth.getBalance(wallet["address"])
    const balanceInEther = web3.utils.fromWei(balance, 'ether')
    
    return {
      address: wallet.address,
      balance: parseFloat(balanceInEther).toFixed(15),
    }
  }))
}

// Create a new account and update wallet
async function createAccount(password) {
  const myRemoteWallet = await cryptoUserUtils.retrieveDecryptedWallet(password)
  myRemoteWallet.add(web3.eth.accounts.create())

  await cryptoUserUtils.updateRemoteWallet(myRemoteWallet, password)
  await refreshMyWalletTable()
  cancelAllDialog()
}


async function addPrivateKeyFromMnemonic(password) {
  const wallet = ethers.Wallet.fromPhrase(mnemonicPhrase.value)
  const hdPath = `m/44'/60'/0'/0/${mnemonicIndex.value}`
  const derivedWallet = wallet.derivePath(hdPath)

  const myRemoteWallet = await cryptoUserUtils.retrieveDecryptedWallet(password)
  myRemoteWallet.add(web3.eth.accounts.privateKeyToAccount(derivedWallet.privateKey))

  await cryptoUserUtils.updateRemoteWallet(myRemoteWallet, password)
  await refreshMyWalletTable()
  cancelAllDialog()
}

// Add a new private key to the wallet
async function addPrivateKey(password) {
  const myRemoteWallet = await cryptoUserUtils.retrieveDecryptedWallet(password)

  myRemoteWallet.add(web3.eth.accounts.privateKeyToAccount(newPrivateKey.value))

  await cryptoUserUtils.updateRemoteWallet(myRemoteWallet, password)
  await refreshMyWalletTable()
  cancelAllDialog()
}

function askCreateAccount() {
  showCreateAccountDialog.value = true
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
  showImportMnemonicDialog.value = false
  showPrivateKeyPasswordDialog.value = false
  showCreateAccountDialog.value = false
  showImportPrivateKeyDialog.value = false
}

async function showPrivateKey(password) {
  const myRemoteWallet = await cryptoUserUtils.retrieveDecryptedWallet(password)
  for (let i = 0; i < myRemoteWallet.length; i++) {
    if (
      web3.utils.toChecksumAddress(myRemoteWallet[i].address) ===
      web3.utils.toChecksumAddress(walletAddress)
    ) {
      privateKeyToShow.value = myRemoteWallet[i].privateKey
      break
    }
  }
  cancelAllDialog()
  showPrivateKeyDialog.value = true
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

function onClickedAccount(account){
  alert(account)
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
          <VRow>
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

