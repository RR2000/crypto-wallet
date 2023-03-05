import {createStore} from "vuex";
import {cryptoUser, web3} from "@/plugins/crypto-user-utils";
import {EncryptedKeystoreV3Json} from "web3-core";

let isLogged = ref(false)
let user = await cryptoUser.getUserInfo()
isLogged.value = (user !== undefined)

async function retriveWalletInUse() {
  let inUseWallet
  if (isLogged.value === true) {
    try {
      let encrypted: EncryptedKeystoreV3Json[] = await cryptoUser.retrieveEncryptedWallet()
      let index: number = await cryptoUser.getIndexWallet()

      let thisAccount: EncryptedKeystoreV3Json = <EncryptedKeystoreV3Json>encrypted.at(index)

      const balance = await web3.eth.getBalance(thisAccount.address)
      const balanceInEther = web3.utils.fromWei(balance, 'ether')
      const balanceString = parseFloat(balanceInEther).toFixed(15)

      inUseWallet = {
        index: index,
        address: thisAccount.address,
        balance: balanceString
      }

    } catch (e) {
      console.error(e)
    }
  } else {
    inUseWallet = {
      index: 0,
      address: "",
      balance: ""
    }
  }
  return inUseWallet
}

export const store = createStore({
  state: {
    inUseWallet: await retriveWalletInUse()
  },
  getters: {
    inUseWallet(state) {
      return state.inUseWallet
    }
  },
  mutations: {
    async changeUseWallet(state, index) {
      if (state.inUseWallet === undefined)
        throw "inUseWallet undefined"

      try {
        let encrypted = await cryptoUser.retrieveEncryptedWallet()
        await cryptoUser.changeInUseWalletIndex(index)

        state.inUseWallet.index = await cryptoUser.getIndexWallet()

        let encAccount: EncryptedKeystoreV3Json = <EncryptedKeystoreV3Json>encrypted.at(state.inUseWallet.index)
        state.inUseWallet.address = encAccount.address

        const balance = await web3.eth.getBalance(state.inUseWallet.address)
        const balanceInEther = web3.utils.fromWei(balance, 'ether')
        state.inUseWallet.balance = parseFloat(balanceInEther).toFixed(15)
      } catch (e) {
        console.error("error while changing wallet account: ", e)
      }
      return state.inUseWallet
    }
  }
})
