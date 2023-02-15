<script setup>
import {userManager} from "../js/oidc-settings";
import axios from "axios";

userManager.signinCallback().then(
    function (user) {
      axios.defaults.headers.common['Authorization'] = `${user.token_type} ${user.access_token}`;
      window.location.href = '/';
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
