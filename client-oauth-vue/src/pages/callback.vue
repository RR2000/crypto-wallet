<script setup>
import {userManagerInstance} from "@/plugins/crypto-user-utils"
import axios from "axios"

userManagerInstance.signinCallback().then(
  function (user) {
    axios.defaults.headers.common['Authorization'] = `${user.token_type} ${user.access_token}`
    window.location.href = '/'
  },
)
</script>

<template />
