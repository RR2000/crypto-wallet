<template>
  <h1>INDEX PAGE</h1>
  <div id='app'>
    <header>
      <h1>FusionAuth Example Vue</h1>
    </header>
    <div id='container'>
      <Greet v-bind:email="email"/>
    </div>
    <Test/>
  </div>
</template>
<script setup>
import {useAuth0} from "@auth0/auth0-vue";

const {user, isAuthenticated} = useAuth0();
const {loginWithRedirect} = useAuth0();

if (!isAuthenticated) {
  loginWithRedirect({
    appState: {
      target: "/profile",
    },
  });
}

const code = user ? JSON.stringify(user.value, null, 2) : "";

</script>

<script>
//const code = user ? JSON.stringify(user.value, null, 2) : "";
import Greet from './Greeting.vue';
import Test from './components/buttons/nav-bar-buttons.vue';

export default {
  name: 'app',
  components: {
    Greet,
    Test
  },

  data() {
    return {
      email: ""
    }
  }
};
</script>
<style>
h1 {
  text-align: center;
  font-size: 40px;
  font-family: Arial, Helvetica, sans-serif;
}

#container {
  box-sizing: border-box;
  border: 5px solid gray;
  border-radius: 15%;
  width: 400px;
  height: 400px;
  margin: auto;
}
</style>
