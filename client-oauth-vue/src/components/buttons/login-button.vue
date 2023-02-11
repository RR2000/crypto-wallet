<template>
  <div>
    <button @click="performLogin">Login</button>
  </div>
</template>

<script setup>
import axios from 'axios';
import {UserManager, settings} from "../../js/oidc-settings";

async function performLogin() {
  await new UserManager(settings).signinRedirect()

}

</script>