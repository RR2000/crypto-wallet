<template>
  <div>
    <h1>
      <template v-if="isLoggedIn">
        <LoggedUser></LoggedUser>
      </template>
      <template v-else>
        <AnonymousUser></AnonymousUser>
      </template>
    </h1>
  </div>
</template>

<script>
import {userManager} from "../js/oidc-settings";
import LoggedUser from "./pages/LoggedUser.vue";
import AnonymousUser from "./pages/AnonymousUser.vue";
import axios from "axios";

export default {
  components: {AnonymousUser, LoggedUser},
  data() {
    return {
      isLoggedIn: false
    }
  },
  mounted() {
    this.checkLoginStatus()
  },
  updated() {
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
