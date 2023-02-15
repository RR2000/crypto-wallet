<script setup>
import {ref, onMounted} from 'vue';
import Web3 from 'web3';
import {userManager} from '@/plugins/oidc-settings';
import axios from 'axios';
import PasswordDialog from "@/views/oldSite/PasswordDialog.vue";

const andromedaURL = 'http://localhost:8545';
const bscURL = 'https://bsc-dataseed1.binance.org:443'
const accountPrivateKey = '0x0000000000000000000000000000000000000000000000000000000000000000';

const web3 = new Web3(new Web3.providers.HttpProvider(bscURL));

let showPrivateKeyDialog = ref(false);
let showCreateAccountDialog = ref(false);
let showImportPrivateKeyDialog = ref(false);
let newPrivateKey = ref('');
let password = ref('');
let walletTable = ref([]);
let accountNames = ref([]);

async function retrieveEncryptedWallet() {
  const user = await userManager.signinSilent();
  axios.defaults.headers.common.Authorization = `${user.token_type} ${user.access_token}`;
  let encryptedWallet = JSON.parse(user.profile.private_keys)[0]
  console.log("retrieveEncryptedWallet", encryptedWallet)
  return encryptedWallet;
}

async function retrieveNames() {
  const user = await userManager.signinSilent();
  axios.defaults.headers.common.Authorization = `${user.token_type} ${user.access_token}`;
  let names = JSON.parse(user.profile.private_keys)[1]
  console.log("retrieveNames", names)
  return names;
}

async function retrieveDecryptedWallet() {
  const encryptedWallet = await retrieveEncryptedWallet();
  console.log("retrieveDecryptedWallet enc ", encryptedWallet)
  const decryptedWallet = web3.eth.accounts.wallet.decrypt(encryptedWallet, password.value);
  console.log("retrieveDecryptedWallet dec ", decryptedWallet)
  return decryptedWallet;
}

async function updateAccountNames() {
  let encryptedWallet = await retrieveEncryptedWallet()

  const formData = new FormData();
  formData.append('private_key', JSON.stringify([encryptedWallet, accountNames.value]));
  try {
    const response = await axios.post('http://127.0.0.1:9000/updateWallet', formData);
    console.log('Private key added:', response);
  } catch (error) {
    console.error('Error adding private key:', error);
  }
  await retrieveMyWallet();
  await retrieveNames();
}

async function updateWallet(plainWallet) {
  let encryptedWallet = plainWallet.encrypt(password.value);

  const user = await userManager.signinSilent();
  axios.defaults.headers.common.Authorization = `${user.token_type} ${user.access_token}`;

  const formData = new FormData();
  formData.append('private_key', JSON.stringify([encryptedWallet, accountNames.value]))
  try {
    const response = await axios.post('http://127.0.0.1:9000/updateWallet', formData);
    console.log('Private key added:', response);
  } catch (error) {
    console.error('Error adding private key:', error);
  }
  await retrieveMyWallet();
}

// Retrieve balance of each wallet in walletTable array
async function retrieveWalletBalances() {
  await Promise.all(walletTable.value.map(async (wallet) => {
    const balance = await web3.eth.getBalance(wallet.address);
    console.log(balance);
    wallet.balance = web3.utils.fromWei(balance, 'ether');
  }));
}


async function retrieveMyWallet() {
  const encryptedWallet = await retrieveEncryptedWallet();
  accountNames.value = await retrieveNames()
  walletTable.value = []

  console.log("names", accountNames.value)

  walletTable.value = encryptedWallet.map((wallet, i) => ({
    address: wallet.address,
    balance: wallet.balance,
  }));
  await retrieveWalletBalances();
}


// Create a new account and update wallet
async function createAccount(password) {
  const myRemoteWallet = await retrieveDecryptedWallet(password);
  myRemoteWallet.add(web3.eth.accounts.create());
  await Promise.all([
    updateWallet(myRemoteWallet, password),
    retrieveEncryptedWallet(),
    retrieveWalletBalances()
  ]);
  cancelAllDialog()
}

// Add a new private key to the wallet
async function addPrivateKey(password) {
  const myRemoteWallet = await retrieveDecryptedWallet(password);
  console.log("my remote wallet: ", myRemoteWallet)
  myRemoteWallet.add(web3.eth.accounts.privateKeyToAccount(newPrivateKey.value));

  console.log("my remote wallet aggiunto: ", myRemoteWallet)
  newPrivateKey.value = ""
  await Promise.all([
    updateWallet(myRemoteWallet, password),
    retrieveEncryptedWallet(),
    retrieveWalletBalances()
  ]);
  cancelAllDialog()
}

function askCreateAccount() {
  showCreateAccountDialog.value = true;
}

function askImportPrivateKey() {
  showImportPrivateKeyDialog.value = true
}

let walletAddress

function askPrivateKey(clickedWallet) {
  walletAddress = clickedWallet
  showPrivateKeyDialog.value = true
}

function cancelAllDialog() {
  walletAddress = ''
  showPrivateKeyDialog.value = false
  showCreateAccountDialog.value = false
  showImportPrivateKeyDialog.value = false
}

// Show private key for a given wallet address
async function showPrivateKey(password) {
  const myRemoteWallet = await retrieveDecryptedWallet(password);
  let privateKey = null;
  for (let i = 0; i < myRemoteWallet.length; i++) {
    if (web3.utils.toChecksumAddress(myRemoteWallet[i].address)
      ===
      web3.utils.toChecksumAddress(walletAddress)) {
      privateKey = myRemoteWallet[i].privateKey
      break;
    }
  }
  if (privateKey) {
    alert(`Private key: ${privateKey}`);
  } else {
    alert(`No private key found for wallet address ${walletAddress}`);
  }
  cancelAllDialog()
}

onMounted(async function () {
  await retrieveMyWallet();
});

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
          <tr v-for="(item, index) in walletTable" :key="item.address">
            <td>
              <VTextField
                v-model="accountNames[index]">
              </VTextField>
            </td>
            <td>{{ item.address }}</td>
            <td class="text-center">
              {{ item.balance }}
            </td>
            <td class="text-center">
              <VBtn
                @click="askPrivateKey(item.address)"
                icon="mdi-eye"
              ></VBtn>
            </td>
          </tr>
          </tbody>
        </VTable>
        <VCol class="d-flex justify-end">
          <VBtn @click="updateAccountNames">
            Conferma modifica
          </VBtn>
          <VBtn type="createAccount" @click="askCreateAccount" class="ml-auto">
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
              <VBtn type="addPrivateKey" @click="askImportPrivateKey">
                Importa
              </VBtn>
            </VCol>
          </VRow>
        </VCardText>
      </VCard>
    </VCol>
  </VRow>
  <password-dialog
    v-model="showPrivateKeyDialog"
    title="Sei sicuro di voler mostrare la chiave privata!?"
    @confirm="showPrivateKey"
    @cancel="cancelAllDialog"
  />
  <password-dialog
    v-model="showCreateAccountDialog"
    title="Inserisci la password per aggiungere un nuovo indirizzo"
    @confirm="createAccount"
    @cancel="cancelAllDialog"
  />
  <password-dialog
    v-model="showImportPrivateKeyDialog"
    title="Inserisci la password per aggiungere un nuovo indirizzo"
    @confirm="addPrivateKey"
    @cancel="cancelAllDialog"
  />
</template>

