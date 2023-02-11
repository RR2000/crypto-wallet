<template>
  <div>
    <template v-if="isLoggedIn">
      <button @click="performLogout">Logout</button>
    </template>
    <template v-else>
      <button @click="performLogin">Login</button>
    </template>
  </div>
</template>

<script>

import {UserManager, settings} from "../../js/oidc-settings";

const userManager = new UserManager(settings)
export default {
  data() {
    return {
      isLoggedIn: false
    }
  },
  mounted() {
    this.checkLoginStatus()
  },
  methods: {
    async checkLoginStatus() {
      const user = await userManager.getUser();
      this.isLoggedIn = !(user == null);
    },
    async performLogin() {
      try {
        await userManager.signinRedirect();
      } catch (error) {
        console.error("Error during sign-in:", error);
      }
    },
    async performLogout() {
      try {
        await userManager.signoutSilent().then(() => {
        });
      } catch (error) {
        console.error("Error during sign-out:", error);
      } finally {
        await this.checkLoginStatus()
        window.location.reload();
      }

    }
  }
}

</script>
