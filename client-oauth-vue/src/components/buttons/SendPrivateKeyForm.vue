<template>
  <div>
    <form @submit.prevent="submitForm">
      <input type="text" v-model="private_key" placeholder="Enter private key">
      <button type="submit">Submit</button>
    </form>
  </div>
</template>

<script setup>

import axios from "axios";
import {settings, UserManager} from "../../js/oidc-settings";

let private_key = ""

const submitForm = () => {
  new UserManager(settings).signinSilent().then(function (user) {
    axios.defaults.headers.common['Authorization'] = `${user.token_type} ${user.access_token}`;
  })
  console.log("access_token", axios.defaults.headers.common['Authorization'])
  let input = new FormData()
  input.append("private_key", "ti prego funziona")
  axios.post("http://127.0.0.1:9000/addPrivateKey",input).
  then(response => {
    console.log("Response:", response.data);
  }).catch(error => {
    console.error("Error:", error);
  });
};
</script>
<style scoped>

</style>