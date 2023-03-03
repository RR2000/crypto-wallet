// @ts-ignore
import bcrypt from "bcryptjs"
import {IdTokenClaims, UserManager, UserManagerSettings} from "oidc-client-ts"
import Web3 from "web3"
import axios from "axios"

export const settings: UserManagerSettings = {
  authority: "http://localhost:9000",
  client_id: "crypto-client",
  client_secret: "secret",
  redirect_uri: window.location.origin + '/callback',
  post_logout_redirect_uri: window.location.origin + '/',
  response_type: "code",
  loadUserInfo: true,
  scope: "openid profile email wallets.private_keys",
  client_authentication: "client_secret_basic",
  includeIdTokenInSilentSignout: true,
  revokeTokensOnSignout: true,
  revokeTokenTypes: ["refresh_token", "access_token"]
}

const userManager = new UserManager(settings)

userManager.signinRedirectCallback().then(function (user) {
  axios.defaults.headers.common['Authorization'] = `${user.token_type} ${user.access_token}`;
  window.location.href = '/';
});

/*
userManager.signoutSilentCallback().then(
  function (){
    axios.defaults.headers.common['Authorization'] = ""

  }
)*/

function comparePasswordHash(inputPassword: string, hash: string) {
  return bcrypt.compareSync(inputPassword, hash)
}

function chainUrl(arg: Number) {
  if (arg === 0) {
    return 'http://localhost:8545'
  } else if (arg === 1) {
    return 'https://bsc-dataseed1.binance.org:443'
  } else if (arg === 2) {
    return 'http://127.0.0.1:8545'
  } else {
    throw("not a valid id for the address")
  }
}

export const web3 = new Web3(new Web3.providers.HttpProvider(chainUrl(0)))

class CryptoUserUtils {

  async signoutSilent() {
    try {
      await new UserManager(settings).revokeTokens(["refresh_token", "access_token"])
    } catch (e) {
      await new UserManager(settings).removeUser()
      //await new UserManager(settings).signoutSilent()
      axios.defaults.headers.common['Authorization'] = false;
      window.location.href = '/'
    }
  }

  async signinRedirect() {
    try {
      await userManager.signinRedirect()
    } catch (error) {
      console.error('Error during sign-in:', error)
    }
  }

  async signinSilent(): Promise<IdTokenClaims | undefined> {
    let user = null
    try {
      user = await userManager.signinSilent()
      if (user !== null)
        axios.defaults.headers.common['Authorization'] = `${user.token_type} ${user.access_token}`
    } catch (error) {
      console.error('Error during sign-in:', error)
    }

    if (user !== null)
      return user.profile
    else
      return undefined
  }

  async getUserInfo(): Promise<IdTokenClaims | undefined> {
    try {
      let user = await userManager.getUser()

      if (user) {
        axios.defaults.headers.common['Authorization'] = `${user.token_type} ${user.access_token}`
        return user.profile
      } else
        return undefined
    } catch (e) {
      console.error("errror while getting user info:", e)
    }
  }

  async retrieveEncryptedWallet() {
    const user = await this.getUserInfo()
    if (user) {
      console.log("Wallet encrypted: ", JSON.parse(<string>user['private_keys'])[0])
      return JSON.parse(<string>user['private_keys'])[0]
    } else {
      return []
    }
  }

  async retrieveDecryptedWallet(password: string) {
    const user: IdTokenClaims | undefined = await this.getUserInfo()
    const encryptedWallet = await this.retrieveEncryptedWallet()

    let passwordMatches: boolean = false
    if (user) {
      passwordMatches = comparePasswordHash(password, <string>user["password"])
    }
    if (!passwordMatches) {
      throw "La password non corrisponde con quella dell'account"
    }
    const decryptedWallet = web3.eth.accounts.wallet.decrypt(encryptedWallet, password)
    console.log("Wallet decrypted: ", decryptedWallet)

    return decryptedWallet
  }

  async retrieveNames() {
    const user = await this.getUserInfo()
    if (user) {
      let walletLenght = JSON.parse(<string>user["private_keys"])[0].length
      let names = JSON.parse(<string>user["private_keys"])[1]
      for (let i = 0; i < walletLenght; i++) {
        if (names[i] == null || names[i].match(/^ *$/) !== null)
          names[i] = ("Account " + (i + 1))
      }

      return names
    }

  }

  async getIndexWallet() {
    const user = await this.getUserInfo()
    if (user) {
      if (JSON.parse(<string>user['private_keys'])[2].length === 0)
        return 0
      else
        return JSON.parse(<string>user['private_keys'])[2]
    }
    return 0
  }

  index: number = 0

  async changeInUseWalletIndex(index: number) {
    this.index = index
    await this.updateRemoteWallet(await this.retrieveEncryptedWallet(), await this.retrieveNames(), undefined)
  }

  async updateRemoteWallet(wallet: any, accountNames: any, password: any) {
    let encryptedWallet
    if (password !== undefined)
      encryptedWallet = await wallet.encrypt(password)
    else
      encryptedWallet = wallet

    const formData = new FormData()
    formData.append('private_key', JSON.stringify([encryptedWallet, accountNames, this.index]))

    try {
      await axios.post('http://127.0.0.1:9000/updateWallet', formData)
      console.log('remote wallet updated: ', JSON.parse(<string>formData.get('private_key')))

      let user = await userManager.signinSilent()
      if (user)
        axios.defaults.headers.common['Authorization'] = `${user.token_type} ${user.access_token}`
    } catch (error) {
      console.error('errore while updating wallet: ', error)
    }
  }

  async changePassword(currentPassword: string, newPassword: string) {
    const user: IdTokenClaims | undefined = await this.signinSilent()
    if (user) {
      if (!comparePasswordHash(currentPassword, <string>user.password)) {
        throw ('Wrong password.')
      }
    }

    try {
      let decryptedWallet = await this.retrieveDecryptedWallet(currentPassword)
      let names = await this.retrieveNames()
      await this.updateRemoteWallet(decryptedWallet, names, newPassword)
      const salt = await bcrypt.genSalt(10)
      const hashedPassword = await bcrypt.hash(newPassword, salt)

      const formData = new FormData()
      formData.append('newPassword', hashedPassword)

      await this.signinSilent()
      await axios.post('http://127.0.0.1:9000/changePassword', formData)
    } catch (error: any) {
      if (error.response) {
        alert(`Error changing password on server : ${error.response.data.message}`)
      } else {
        alert(`Error changing password on wallet : ${error.message}`)
      }
    }
  }
}

const cryptoUser = new CryptoUserUtils()

export {
  cryptoUser,
}
