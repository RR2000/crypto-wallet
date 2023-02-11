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
import {userManager} from "../../js/oidc-settings";
import axios from "axios";

async function getUserWallet() {
  let download = ""
  await userManager.signinSilent().then(function (user) {
    download = user.profile["private_keys"]
  })
  return download
}

export default {
  name: "LoggedUser",
  async created() {
    this.wallet = await getUserWallet()
  },
  methods: {
    async submitForm() {
      let self = this
      await userManager.signinSilent().then(async function (user) {
        axios.defaults.headers.common['Authorization'] = `${user.token_type} ${user.access_token}`;
        console.log("access_token", axios.defaults.headers.common['Authorization'])
        let input = new FormData()
        input.append("private_key", self.privateKeyInput)
        axios.post("http://127.0.0.1:9000/addPrivateKey", input).then(async response => {
          self.wallet = await getUserWallet()
          console.log("WALLET UPDATED: ", self.wallet);
        }).catch(error => {
          console.error("Error:", error);
        })
      })
    },
    async performLogout() {
      try {
        await userManager.signoutSilent()
      } catch (error) {
        console.error("Error during sign-out:", error);
      } finally {
        //await this.checkLoginStatus()
        window.location.reload();
      }
    }
  },
  data() {
    return {
      wallet: "",
      privateKeyInput: ""
    }
  }
}
</script>
<style scoped>

</style>
