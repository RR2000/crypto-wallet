<script setup>
import {userManager} from "@/plugins/oidc-settings"
import axios from "axios"

userManager.signinCallback().then(
  function (user) {
    axios.defaults.headers.common['Authorization'] = `${user.token_type} ${user.access_token}`
    window.location.href = '/'
  },
)
</script>
<template>

</template>
