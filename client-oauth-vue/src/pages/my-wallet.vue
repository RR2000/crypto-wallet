<script setup>
import {ethers} from 'ethers'
import bcrypt from 'bcryptjs'
import {ref, onMounted} from 'vue'
import Web3 from 'web3'
import axios from 'axios'
import PasswordDialog from "@/views/my-wallet/PasswordDialog.vue"
import {userManagerSingleton} from "@/plugins/user-manager";

function chainUrl(arg){
  if (arg === 0)
    return 'http://localhost:8545'
  else if(arg === 1)
    return 'https://bsc-dataseed1.binance.org:443'
}

//const accountPrivateKey = '0x0000000000000000000000000000000000000000000000000000000000000000'

const web3 = new Web3(new Web3.providers.HttpProvider(chainUrl(1)))

let showPrivateKeyDialog = ref(false)
let showPrivateKeyPasswordDialog = ref(false)
let showCreateAccountDialog = ref(false)
let showImportPrivateKeyDialog = ref(false)
let showImportMnemonicDialog = ref(false)
let newPrivateKey = ref('')
let privateKeyToShow = ref('')
let mnemonicPhrase = ref('')
let mnemonicIndex = ref(0)
let walletTable = ref([])
let accountNames = ref([])

async function retrieveEncryptedWallet() {
  await userManagerSingleton.refresh();
  const user = await userManagerSingleton.getUserInfo()
  return JSON.parse(user.private_keys)[0]
}

async function retrieveNames() {
  const user = await userManagerSingleton.getUserInfo()
  let walletLenght = JSON.parse(user.private_keys)[0].length
  let names = JSON.parse(user.private_keys)[1]
  for(let i = 0; i < walletLenght; i++){
    if(names[i] == null || names[i].match(/^ *$/) !== null)
      names[i] = ("Account " + (i + 1))
  }
  console.log("retrieveNames", names, walletLenght)

  return names
}

function checkPassword(inputPassword, hash) {
  return bcrypt.compareSync(inputPassword, hash)
}

async function retrieveDecryptedWallet(password) {
  const encryptedWallet = await retrieveEncryptedWallet()
  const user = await userManagerSingleton.getUserInfo()

  const passwordMatches = checkPassword(password, user.password)
  if (!passwordMatches) {
    throw "La password non corrisponde con quella dell'account"
  }
  const decryptedWallet = web3.eth.accounts.wallet.decrypt(encryptedWallet, password)
  console.log("retrieveDecryptedWallet dec ", decryptedWallet)

  return decryptedWallet
}

async function updateAccountNames() {
  let encryptedWallet = await retrieveEncryptedWallet()

  const formData = new FormData()
  formData.append('private_key', JSON.stringify([encryptedWallet, accountNames.value]))
  try {
    const response = await axios.post('http://127.0.0.1:9000/updateWallet', formData)
    console.log('Private key added:', response)
  } catch (error) {
    console.error('Error adding private key:', error)
  }
  await retrieveMyWallet()
  await retrieveNames()
}

async function updateWallet(plainWallet, password) {
  let encryptedWallet = plainWallet.encrypt(password)

  const formData = new FormData()
  formData.append('private_key', JSON.stringify([encryptedWallet, accountNames.value]))
  try {
    const response = await axios.post('http://127.0.0.1:9000/updateWallet', formData)
    console.log('Private key added:', response)
  } catch (error) {
    console.error('Error adding private key:', error)
  }
  await retrieveMyWallet()
}

// Retrieve balance of each wallet in walletTable array
async function retrieveWalletBalances() {
  await Promise.all(walletTable.value.map(async wallet => {
    const balance = await web3.eth.getBalance(wallet["address"])
    console.log(balance)
    wallet.balance = web3.utils.fromWei(balance, 'ether')
    wallet.balance = parseFloat(wallet.balance).toFixed(15)
  }))
}


async function retrieveMyWallet() {
  const encryptedWallet = await retrieveEncryptedWallet()
  accountNames.value = await retrieveNames()
  walletTable.value = []

  console.log("names", accountNames.value)

  walletTable.value = encryptedWallet.map(wallet => ({
    address: wallet.address,
    balance: wallet.balance,
  }))
  await retrieveWalletBalances()
}


// Create a new account and update wallet
async function createAccount(password) {
  const myRemoteWallet = await retrieveDecryptedWallet(password)
  myRemoteWallet.add(web3.eth.accounts.create())
  await Promise.all([
    updateWallet(myRemoteWallet, password),
    retrieveEncryptedWallet(),
    retrieveWalletBalances(),
  ])
  cancelAllDialog()
}


async function addPrivateKeyFromMnemonic(password) {
  const wallet = ethers.Wallet.fromPhrase(mnemonicPhrase.value)
  const hdPath = `m/44'/60'/0'/0/${mnemonicIndex.value}`
  const derivedWallet = wallet.derivePath(hdPath)

  console.log("derivedWallet", derivedWallet)

  const myRemoteWallet = await retrieveDecryptedWallet(password)
  myRemoteWallet.add(web3.eth.accounts.privateKeyToAccount(derivedWallet.privateKey))

  console.log("my remote wallet aggiunto: ", myRemoteWallet)
  newPrivateKey.value = ""
  cancelAllDialog()
  await Promise.all([
    updateWallet(myRemoteWallet, password),
    retrieveEncryptedWallet(),
    retrieveWalletBalances(),
  ])
}

// Add a new private key to the wallet
async function addPrivateKey(password) {
  const myRemoteWallet = await retrieveDecryptedWallet(password)
  console.log("my remote wallet: ", myRemoteWallet)
  myRemoteWallet.add(web3.eth.accounts.privateKeyToAccount(newPrivateKey.value))

  console.log("my remote wallet aggiunto: ", myRemoteWallet)
  newPrivateKey.value = ""
  await Promise.all([
    updateWallet(myRemoteWallet, password),
    retrieveEncryptedWallet(),
    retrieveWalletBalances(),
  ])
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
  const myRemoteWallet = await retrieveDecryptedWallet(password)
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
  await retrieveMyWallet()
})

const numberRule = val => {
  if (val === ''){
    return 'Campo obbligatorio'
  } else if (val < 0) {
    return 'Il numero deve essere positivo!'
  } else if (!Number.isInteger(val)) {
    return 'Il numero deve essere intero!'
  }

  return true
}
</script>

<template>
  <VRow>
    <VCol>
      <VCard title="Il mio wallet">
        <VTable>
          <thead>
            <tr>
              <th class="text-uppercase">
                Nome
              </th>
              <th class="text-uppercase">
                Chiave pubblica
              </th>
              <th class="text-center text-uppercase">
                Bilancio
              </th>
              <th class="text-center text-uppercase">
                Mostra chiave privata
              </th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="(item, index) in walletTable"
              :key="item.address"
            >
              <td>
                <VTextField
                  v-model="accountNames[index]"
                />
              </td>
              <td style="font-family: monospace; font-size: 1em">
                {{ "0x" + item.address.toUpperCase() }}
              </td>
              <td class="text-center">
                {{ item.balance }}
              </td>
              <td class="text-center">
                <VBtn
                  icon="mdi-eye"
                  @click="askPrivateKey(item.address)"
                />
              </td>
            </tr>
          </tbody>
        </VTable>
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

