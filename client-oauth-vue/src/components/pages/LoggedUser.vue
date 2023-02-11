<template>
  <div>
    <h1>Your personal page</h1>
    <button @click="performLogout">Logout</button>
    <div v-if="myAddresses.length > 0">
      <h2>Public Keys:</h2>
      <ul>
        <li v-for="address in myAddresses" :key="address">{{ address }}</li>
      </ul>
    </div>
    <div>
      <input type="text" v-model="privateKeyInput" placeholder="Enter private key"><br>
      <input type="password" v-model="password" placeholder="Enter password">
      <button @click.prevent="retrieveWallet(password)">Retrieve Wallet</button>
      <button @click.prevent="addPrivateKey(privateKeyInput, password)">Submit</button>
    </div>
  </div>
</template>

<script>
import Web3 from "web3";
import { userManager } from "../../js/oidc-settings";
import axios from "axios";

const web3 = new Web3();

async function retrieveWallet(password) {
  const user = await userManager.signinSilent();
  axios.defaults.headers.common["Authorization"] = `${user.token_type} ${user.access_token}`;
  const encryptedWallet = JSON.parse(user.profile["private_keys"]);
  const decryptedWallet = web3.eth.accounts.wallet.decrypt(encryptedWallet, password);

  for (let i = 0; i < decryptedWallet.length; i++) {
    this.myAddresses.push(decryptedWallet[i].address);
  }

  return decryptedWallet;
}

async function updateWallet(plainWallet, password) {
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
}

async function addPrivateKey(privateKeyInput, password) {
  const myRemoteWallet = await retrieveWallet(password);
  // TODO remove ignore length, dev only
  myRemoteWallet.add(web3.eth.accounts.privateKeyToAccount(privateKeyInput, true));
  await updateWallet(myRemoteWallet, password);
}

export default {
  name: "LoggedUser",
  created() {
    this.password = "";
    this.myAddresses = [];
  },
  methods: {
    retrieveWallet,
    addPrivateKey,
    async performLogout() {
      try {
        await userManager.signoutSilent();
      } catch (error) {
        console.error("Error during sign-out:", error);
      } finally {
        window.location.reload();
      }
    },
  },
  data() {
    return {
      privateKeyInput: "",
      password: "",
      myAddresses: [],
    };
  },
};
</script>

<style scoped>
</style>
