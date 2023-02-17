import {UserManager} from "oidc-client-ts"
import axios from "axios";

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

let userManagerInstance = null

class UserManagerSingleton {
  constructor(settings) {
    if (!userManagerInstance) {
      userManagerInstance = new UserManager(settings)
    }
  }

  getInstance() {
    return userManagerInstance
  }

  async refresh() {
    await this.getInstance().signinSilent().then(function (user) {
      axios.defaults.headers.common = {'Authorization': `${user.token_type} ${user.access_token}`}
    })
  }

  async getUserInfo() {
    await this.refresh()
    const user = await this.getInstance().getUser()
    return user ? user.profile : null
  }
}

const userManagerSingleton = new UserManagerSingleton(settings)

export {
  userManagerSingleton,
}
