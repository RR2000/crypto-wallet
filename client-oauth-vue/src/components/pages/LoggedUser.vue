<template>
  <div>
    <h1>Il Mio Wallet</h1>
    <button @click="performLogout">Logout</button>
    <div>
      <input type="password" v-model="passwordInput" placeholder="Inserisci password">
      <button @click.prevent="retrieveWallet(this)">Mostra i miei wallet</button>
    </div>
    <div v-if="myAddresses.length > 0">
      <h2>I miei indirizzi:</h2>
      <ul>
        <li v-for="address in myAddresses" :key="address">{{ address }}</li>
      </ul>
    </div>
    <div>
      <input type="password" v-model="passwordInput" placeholder="Inserisci password">
      <input type="text" v-model="privateKeyInput" placeholder="Inserisci qui chiave privata">
      <button @click.prevent="addPrivateKey(this)">Aggiungi chiave privata</button>
    </div>
    <div>
      <input type="password" v-model="passwordInput" placeholder="Inserisci password">
      <button @click.prevent="createNewAccount(this)">Aggiungi un nuovo indirizzo casuale</button>
    </div>
  </div>
</template>
<script>
import Web3 from "web3";
import {userManager} from "../../js/oidc-settings";
import axios from "axios";

const web3 = new Web3();

const updateWallet = async (plainWallet, password) => {
  const encryptedWallet = plainWallet.encrypt(password);
  const user = await userManager.signinSilent();
  axios.defaults.headers.common["Authorization"] = `${user.token_type} ${user.access_token}`;
  const input = new FormData();
  input.append("private_key", JSON.stringify(encryptedWallet));
  try {
    const response = await axios.post("http://127.0.0.1:9000/updateWallet", input);
    console.log("Private key added:", response);
  } catch (error) {
    console.error("Error adding private key:", error);
  }
};

const createNewAccount = async (instance, password) => {
  const myRemoteWallet = await retrieveWallet(instance, password);
  myRemoteWallet.add(web3.eth.accounts.create());
  await updateWallet(myRemoteWallet, password);
  await retrieveWallet(instance, password)
};

const performLogout = async () => {
  try {
    await userManager.signoutSilent();
  } catch (error) {
    console.error("Error during sign-out:", error);
  } finally {
    window.location.reload();
  }
};

const addPrivateKey = async (instance, privateKeyInput, password) => {
  const myRemoteWallet = await retrieveWallet(instance, password);
  myRemoteWallet.add(web3.eth.accounts.privateKeyToAccount(privateKeyInput, true));
  await updateWallet(myRemoteWallet, password);
  await retrieveWallet(instance, password)
};

const retrieveWallet = async (instance, password) => {
  instance.myAddresses = []
  const user = await userManager.signinSilent();
  axios.defaults.headers.common["Authorization"] = `${user.token_type} ${user.access_token}`;
  const encryptedWallet = JSON.parse(user.profile["private_keys"]);
  let decryptedWallet;
  try {
    decryptedWallet = web3.eth.accounts.wallet.decrypt(encryptedWallet, password);
  } catch (error) {
    alert("Mmm... La tua password mi sembra errata!");
  }
  for (let i = 0; i < decryptedWallet.length; i++) {
    instance.myAddresses.push(decryptedWallet[i].address);
  }
  return decryptedWallet;
};


export default {
  data() {
    return {
      privateKeyInput: "",
      passwordInput: "",
      myAddresses: []
    }
  },
  methods: {
    retrieveWallet: (self) => retrieveWallet(self, self.passwordInput),
    addPrivateKey: (self) => addPrivateKey(self, self.privateKeyInput, self.passwordInput),
    createNewAccount: (self) => createNewAccount(self, self.passwordInput),
    performLogout,
  }
}
</script>
<style scoped>

</style>
