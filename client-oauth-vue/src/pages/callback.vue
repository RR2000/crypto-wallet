<script setup>
import {cryptoUserUtils} from "@/plugins/crypto-user-utils"
import axios from "axios"

cryptoUserUtils.setCallback(
  function (user) {
    axios.defaults.headers.common['Authorization'] = `${user.token_type} ${user.access_token}`
    window.location.href = '/'
  },
)
</script>

<template />
