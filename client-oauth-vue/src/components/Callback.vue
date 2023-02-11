<script setup>
import {UserManager, settings} from "../js/oidc-settings";
import Web3 from 'web3'
import axios from "axios";

new UserManager(settings).signinCallback().then(
    function (user) {
      axios.defaults.headers.common['Authorization'] = `${user.token_type} ${user.access_token}`;
    }
)

</script>
<template>
  <div>
    <h1>
      Callback
    </h1>
  </div>
</template>
