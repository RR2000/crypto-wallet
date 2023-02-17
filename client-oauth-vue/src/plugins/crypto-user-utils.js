import {userManagerSingleton} from "@/plugins/user-manager"
import bcrypt from "bcryptjs";
import axios from "axios";
import Web3 from "web3";

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
  async retrieveEncryptedWallet() {
    await userManagerSingleton.refresh();
    const user = await userManagerSingleton.getUserInfo()
    return JSON.parse(user["private_keys"])[0]
  }

  async retrieveDecryptedWallet(password) {
    const encryptedWallet = await this.retrieveEncryptedWallet()
    const user = await userManagerSingleton.getUserInfo()

    const passwordMatches = comparePasswordHash(password, user.password)
    if (!passwordMatches) {
      throw "La password non corrisponde con quella dell'account"
    }
    const decryptedWallet = web3.eth.accounts.wallet.decrypt(encryptedWallet, password)
    console.log("retrieveDecryptedWallet dec ", decryptedWallet)

    return decryptedWallet
  }

  async retrieveNames() {
    const user = await userManagerSingleton.getUserInfo()
    let walletLenght = JSON.parse(user["private_keys"])[0].length
    let names = JSON.parse(user["private_keys"])[1]
    for (let i = 0; i < walletLenght; i++) {
      if (names[i] == null || names[i].match(/^ *$/) !== null)
        names[i] = ("Account " + (i + 1))
    }
    console.log("retrieveNames", names, walletLenght)

    return names
  }

  async updateRemoteWallet(plainWallet, password) {
    let encryptedWallet = await plainWallet.encrypt(password)
    let accountNames = await this.retrieveNames()

    const formData = new FormData()
    formData.append('private_key', JSON.stringify([encryptedWallet, accountNames]))

    await userManagerSingleton.refresh()
    try {
      const response = await axios.post('http://127.0.0.1:9000/updateWallet', formData)
      console.log('Private key added:', response)
    } catch (error) {
      console.error('Error adding private key:', error)
    }
  }

  async changePassword(currentPassword, newPassword) {
    const user = await userManagerSingleton.getUserInfo()
    if (!comparePasswordHash(currentPassword, user.password)) {
      throw ('Wrong password.')
    }

    try {
      let decryptedWallet = await this.retrieveDecryptedWallet(currentPassword)
      await this.updateRemoteWallet(decryptedWallet, newPassword)

      alert('Crypto wallet password changed successfully.')

      const salt = await bcrypt.genSalt(10)
      const hashedPassword = await bcrypt.hash(newPassword, salt)

      const formData = new FormData()
      formData.append('newPassword', hashedPassword)

      await userManagerSingleton.refresh()
      await axios.post('http://127.0.0.1:9000/changePassword', formData)
      alert('Account password changed successfully.')
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
