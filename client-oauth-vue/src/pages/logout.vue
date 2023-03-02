<script>
import {cryptoUser} from "@/plugins/crypto-user-utils"
import axios from "axios"

//let user = await cryptoUser.getUserInfo()
//cryptoUser.getUserManager().revokeTokens()
//cryptoUser.getUserManager().signoutSilent({
  //  id_token_hint: user?.id_token
//})
await cryptoUser.signoutSilent()
//cryptoUser.removeUser()
//axios.defaults.headers.common['Authorization'] = null
//


</script>

<template>
</template>
