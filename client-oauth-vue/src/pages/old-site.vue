<script setup>
import { ref, onMounted } from 'vue';
import Web3 from 'web3';
import { userManager } from '@/plugins/oidc-settings';
import axios from 'axios';

const blockchainURL = 'http://localhost:8545';
const accountPrivateKey = '0x0000000000000000000000000000000000000000000000000000000000000000';

const web3 = new Web3(new Web3.providers.HttpProvider(blockchainURL));
const account = web3.eth.accounts.privateKeyToAccount(accountPrivateKey);

let newPrivateKey = '';
const myWallet = ref([]);

onMounted(async function() {
  await retriveCryptWallet();
});

async function updateWallet(plainWallet, password) {
  const encryptedWallet = plainWallet.encrypt(password);
  const user = await userManager.signinSilent();
  axios.defaults.headers.common.Authorization = `${user.token_type} ${user.access_token}`;
  const input = new FormData();
  input.append('private_key', JSON.stringify(encryptedWallet));
  try {
    const response = await axios.post('http://127.0.0.1:9000/updateWallet', input);
    console.log('Private key added:', response);
  } catch (error) {
    console.error('Error adding private key:', error);
  }
  await retriveCryptWallet();
}

async function retriveBalance() {
  await Promise.all(myWallet.value.map(async wallet => {
    const balance = await web3.eth.getBalance(wallet.address);
    console.log(balance);
    wallet.balance = web3.utils.fromWei(balance, 'ether');
  }));
}

async function retriveCryptWallet() {
  const user = await userManager.signinSilent();
  axios.defaults.headers.common.Authorization = `${user.token_type} ${user.access_token}`;
  console.log(JSON.parse(user.profile.private_keys));
  myWallet.value = JSON.parse(user.profile.private_keys).map(wallet => ({
    name: wallet.name,
    address: wallet.address,
    privateKey: wallet.privateKey,
    balance: 0,
  }));
  await retriveBalance();
  return JSON.parse(user.profile.private_keys);
}

async function retriveDecryptedWallet(password) {
  const cryptWallet = await retriveCryptWallet();
  const decryptedWallet = web3.eth.accounts.wallet.decrypt(cryptWallet, password);
  return decryptedWallet;
}

async function addPrivateKey(password) {
  password = 'pippo';
  const myRemoteWallet = await retriveDecryptedWallet(password);
  myRemoteWallet.add(web3.eth.accounts.privateKeyToAccount(newPrivateKey, true));

  myWallet.value = [];

  await Promise.all([
    updateWallet(myRemoteWallet, password),
  ]);
}

async function createAccount(password) {
  password = 'pippo';
  const myRemoteWallet = await retriveDecryptedWallet(password);
  myRemoteWallet.add(web3.eth.accounts.create());

  myWallet.value = []

  await Promise.all([
    updateWallet(myRemoteWallet, password),
    retriveCryptWallet(),
    retriveBalance()
  ])
}

  async function showPrivateKey(walletAddress) {
  const password = "pippo";
  const myRemoteWallet = await retriveDecryptedWallet(password);
  let privateKey = null;
  for (const account of myRemoteWallet) {
    if (account["address"] === walletAddress) {
      privateKey = account["privateKey"]
      break;
    }
  }
  if (privateKey) {
    alert(`Private key: ${privateKey}`);
  } else {
    alert(`No private key found for wallet address ${walletAddress}`);
  }
}


</script>
<template>
  <VRow>
    <VCol>
      <VCard title="I miei wallet">
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
          <tr v-for="item in myWallet" :key="item.address">
            <td>{{ item.name }}</td>
            <td>{{ item.address }}</td>
            <td class="text-center">
              {{ item.balance }}
            </td>
            <td class="text-center">
              <VBtn
                @click="showPrivateKey(item.address)"
                icon="mdi-eye"
              ></VBtn>
            </td>
          </tr>
          </tbody>
        </VTable>
        <VCol>
          <VBtn type="createAccount" @click="createAccount">
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
              <VBtn type="addPrivateKey" @click="addPrivateKey">
                Importa
              </VBtn>
            </VCol>
          </VRow>
        </VCardText>
      </VCard>
    </VCol>
  </VRow>
</template>
