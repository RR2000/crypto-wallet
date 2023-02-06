<template>
  <div>
    <p>Access Token: {{ accessToken }}</p>
  </div>
</template>

<script>
import { useAuth0 } from '@auth0/auth0-vue';

export default {
  setup() {
    const { accessToken } = useAuth0();

    return {
      accessToken,
    };
  },
};
</script>