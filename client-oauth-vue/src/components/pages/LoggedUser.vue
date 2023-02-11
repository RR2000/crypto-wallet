<template>
  <h1>Your personal page</h1>
  <button @click="performLogout">Logout</button>
  {{ wallet }}
  <div>
    <form @submit.prevent="submitForm">
      <input type="text" v-model="privateKeyInput" placeholder="Enter private key">
      <button type="submit">Submit</button>
    </form>
  </div>
</template>
<script>
import Web3 from "web3";
import { userManager } from "../../js/oidc-settings";
import axios from "axios";

const web3 = new Web3();

async function retriveWallet(password){
  const user = await userManager.signinSilent();
  axios.defaults.headers.common["Authorization"] = `${user.token_type} ${user.access_token}`;
  console.log("userm", user.profile["private_keys"])
  const encryptedWallet = JSON.parse(user.profile["private_keys"])
  return web3.eth.accounts.wallet.decrypt(encryptedWallet, password)
}

async function updateWallet(plainWallet, password) {
  let encryptedWallet = plainWallet.encrypt(password)
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

async function cose(){
  let vuoto = web3.eth.accounts.wallet.create();
  let password = "pwd"

  await updateWallet(vuoto, password)

  let retrievedVuoto = await retriveWallet(password)

  let aggiungoUno = retrievedVuoto.add(web3.eth.accounts.create())

  await updateWallet(aggiungoUno, password)
}


export default {
  name: "LoggedUser",
  async created() {
    this.wallet = await cose();
  },
  methods: {
    async submitForm() {
      let self = this;
      await userManager
          .signinSilent()
          .then(async (user) => {
            axios.defaults.headers.common["Authorization"] = `${user.token_type} ${user.access_token}`;
            const input = new FormData();
            input.append("private_key", self.privateKeyInput);
            await axios
                .post("http://127.0.0.1:9000/addPrivateKey", input)
                .then(async (response) => {
                  self.wallet = await getUserWallet();
                  console.log("WALLET UPDATED: ", self.wallet);
                })
                .catch((error) => {
                  console.error("Error:", error);
                });
          })
          .catch((error) => {
            console.error("Error submitting form:", error);
          });
    },
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
    };
  },
};
</script>

<style scoped>
</style>
