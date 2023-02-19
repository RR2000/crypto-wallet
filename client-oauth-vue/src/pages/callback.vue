<script setup>
import {cryptoUser} from "@/plugins/crypto-user-utils"
import axios from "axios"

await cryptoUser.getUserManager().signinCallback().then(
  function (user) {
    axios.defaults.headers.common['Authorization'] = `${user.token_type} ${user.access_token}`
    window.location.href = '/'
  },
)
</script>

<template />
