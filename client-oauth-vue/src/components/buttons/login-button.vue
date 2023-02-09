<template>
  <div>
    <button @click="handleLogin">Login</button>
  </div>
</template>

<script setup>
import {UserManager, settings} from "../../js/oidc-settings";

const handleLogin = () => {
  new UserManager(settings).signinRedirect().then(function() {
  }).catch(function(err) {
    console.error(err);
  });
};
</script>