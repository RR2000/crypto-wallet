<template>
  <div class="nav-bar__buttons">
    <template v-if="!isAuthenticated">
      <SignupButton/>
      <LoginButton/>
    </template>
    <template v-if="isAuthenticated">
      <LogoutButton/>
    </template>
  </div>
</template>

<script setup>
import LoginButton from "../buttons/login-button.vue";
import LogoutButton from "../buttons/logout-button.vue";
import SignupButton from "../buttons/signup-button.vue";
import {useAuth0} from "@auth0/auth0-vue";
</script>

<script>
import {useAuth0} from "@auth0/auth0-vue";

const {isAuthenticated, accessToken, user} = useAuth0();

export default {
  data() {
    return {
      isAuthenticated,
      accessToken,
      user,
    };
  },
};
</script>