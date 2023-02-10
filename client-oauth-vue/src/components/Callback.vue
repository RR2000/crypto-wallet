<script>
import {UserManager, settings} from "../js/oidc-settings";

new UserManager(settings).signinRedirectCallback().then(function(user) {
  console.log("signin response success",user.profile);
}).catch(function(err) {
  console.log(err);
});
</script>
<template>
  <div>
    <h1>
      Callback
    </h1>
  </div>
</template>
