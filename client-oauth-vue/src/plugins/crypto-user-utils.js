import bcrypt from "bcryptjs"
import {UserManager} from "oidc-client-ts"
import Web3 from "web3"
import axios from "axios"

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

export let userManagerInstance = null

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
    let user = await this.getInstance().signinSilent()
    axios.defaults.headers.common = {'Authorization': `${user.token_type} ${user.access_token}`}
  }

  async getUserInfo() {
    await this.refresh()
    const user = await this.getInstance().getUser()

    return user ? user.profile : null
  }
}

const userManager = new UserManagerSingleton(settings)

function comparePasswordHash(inputPassword, hash) {
  return bcrypt.compareSync(inputPassword, hash)
}

function chainUrl(arg){
  if (arg === 0)
    return 'http://localhost:8545'
  else if(arg === 1)
    return 'https://bsc-dataseed1.binance.org:443'
}

const web3 = new Web3(new Web3.providers.HttpProvider(chainUrl(1)))

class CryptoUserUtils {

  async getUserManager(){
    return userManager.getInstance()
  }

  async signinRedirect(){
    try {
      await userManager.getInstance().signinRedirect()
    } catch (error) {
      console.error("Error during sign-in:", error)
    }
  }

  async getUserInfo(){
    return userManager.getUserInfo()
  }

  async retrieveEncryptedWallet() {
    await userManager.refresh()
    const user = await userManager.getUserInfo()

    return JSON.parse(user["private_keys"])[0]
  }

  async retrieveDecryptedWallet(password) {
    const encryptedWallet = await this.retrieveEncryptedWallet()
    const user = await userManager.getUserInfo()

    const passwordMatches = comparePasswordHash(password, user.password)
    if (!passwordMatches) {
      throw "La password non corrisponde con quella dell'account"
    }
    const decryptedWallet = web3.eth.accounts.wallet.decrypt(encryptedWallet, password)
    console.log("retrieveDecryptedWallet dec ", decryptedWallet)

    return decryptedWallet
  }

  async retrieveNames() {
    const user = await userManager.getUserInfo()
    let walletLenght = JSON.parse(user["private_keys"])[0].length
    let names = JSON.parse(user["private_keys"])[1]
    for (let i = 0; i < walletLenght; i++) {
      if (names[i] == null || names[i].match(/^ *$/) !== null)
        names[i] = ("Account " + (i + 1))
    }
    console.log("retrieveNames", names, walletLenght)

    return names
  }

  async getBalance(nWallet){
    const encryptedWallet = await cryptoUserUtils.retrieveEncryptedWallet()

    const balance = await web3.eth.getBalance(encryptedWallet[nWallet].address)
    const balanceInEther = web3.utils.fromWei(balance, 'ether')

    return parseFloat(balanceInEther).toFixed(15) + " ETH"
  }

  async updateRemoteWallet(plainWallet, password) {
    let encryptedWallet = await plainWallet.encrypt(password)
    let accountNames = await this.retrieveNames()

    const formData = new FormData()
    formData.append('private_key', JSON.stringify([encryptedWallet, accountNames]))

    await userManager.refresh()
    try {
      const response = await axios.post('http://127.0.0.1:9000/updateWallet', formData)
      console.log('Private key added:', response)
    } catch (error) {
      console.error('Error adding private key:', error)
    }
  }

  async changePassword(currentPassword, newPassword) {
    const user = await userManager.getUserInfo()
    if (!comparePasswordHash(currentPassword, user.password)) {
      throw ('Wrong password.')
    }

    try {
      let decryptedWallet = await this.retrieveDecryptedWallet(currentPassword)
      await this.updateRemoteWallet(decryptedWallet, newPassword)

      const salt = await bcrypt.genSalt(10)
      const hashedPassword = await bcrypt.hash(newPassword, salt)

      const formData = new FormData()
      formData.append('newPassword', hashedPassword)

      await userManager.refresh()
      await axios.post('http://127.0.0.1:9000/changePassword', formData)
    } catch (error) {
      if (error.response) {
        alert(`Error changing password: ${error.response.data.message}`)
      } else {
        alert(`Error changing password: ${error.message}`)
      }
    }
  }
}

const cryptoUserUtils = new CryptoUserUtils()

export {
  cryptoUserUtils,
}
