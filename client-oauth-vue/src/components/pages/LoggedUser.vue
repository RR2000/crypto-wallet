<template>
  <div>
    <h1>Your personal page</h1>
    <button @click="performLogout">Logout</button>
    <div>{{ wallet }}</div>
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
  return web3.eth.accounts.wallet.decrypt(encryptedWallet, password);
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

async function addPrivateKey(privateKeyInput, password){
  let myRemoteWallet = await retrieveWallet(password)
  //TODO remove ignore length, dev only
  myRemoteWallet.add(web3.eth.accounts.privateKeyToAccount(privateKeyInput, true))
  await updateWallet(myRemoteWallet, password)
}

export default {
  name: "LoggedUser",
  async created() {
    this.password = "";
    this.wallet = "";
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
      wallet: "",
      privateKeyInput: "",
      password: "",
    };
  },
};
</script>

<style scoped>
</style>
