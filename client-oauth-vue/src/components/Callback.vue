<script>
import {UserManager, settings} from "../js/oidc-settings";
import Web3 from 'web3'
import axios from "axios";

const web3 = new Web3();

const user = await new UserManager(settings).getUser()

await new UserManager(settings).getUser().then(function (user){
  axios.defaults.headers.common['Authorization'] = `${user.token_type} ${user.access_token}`;
  alert(user.profile["private_keys"])
})

</script>
<template>
  <div>
    <h1>
      Callback
    </h1>
  </div>
</template>
