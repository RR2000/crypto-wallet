import {Log, OidcClient, UserManager} from "oidc-client-ts"

//TODO DA RIMUOVERE IN FAVORE DI user-manager.js

Log.setLogger(console)
Log.setLevel(Log.DEBUG)

export const settings = {
  authority: "http://localhost:9000",
  client_id: "crypto-client",
  client_secret: "secret",
  redirect_uri: window.location.origin + '/callback',
  post_logout_redirect_uri: window.location.origin + '/',
  response_type: "code",
  loadUserInfo: true,
  scope: "openid profile email wallets.private_keys",
  client_authentication: "client_secret_basic",
}

const userManager = new UserManager(settings)

export {
  Log,
  OidcClient,
  userManager,
}
