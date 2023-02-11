<template>
  <div>
    <button @click="performLogin">Login</button>
  </div>
</template>

<script setup>
import axios from 'axios';
import {UserManager, settings} from "../../js/oidc-settings";

const userManager = new UserManager(settings);

async function performLogin() {
  try {
    await userManager.signinRedirect();
  } catch (error) {
    console.error("Error during sign-in:", error);
  }
}

</script>