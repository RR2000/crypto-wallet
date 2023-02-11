<template>
  <h1>Login in page</h1>
  <button @click="performLogin">Login</button>
</template>

<script>
import {userManager} from "../../js/oidc-settings";

export default {
  name: "AnonymousUser",
  methods: {
    async performLogin() {
      try {
        await userManager.signinRedirect();
      } catch (error) {
        console.error("Error during sign-in:", error);
      }
    }
  }
}
</script>

<style scoped>

</style>