<template>
  <div>
    <button @click="handleLogin">Login</button>
  </div>
</template>

<script setup>
import {useAuth0} from "@auth0/auth0-vue";

const { loginWithRedirect } = useAuth0();

const handleLogin = () => {
  loginWithRedirect();
  const { user } = useAuth0();
  const code = user ? JSON.stringify(user.value, null, 2) : "";
  alert(code)
};
</script>