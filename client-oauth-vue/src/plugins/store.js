import {createStore} from "vuex";
import {cryptoUser, web3} from "@/plugins/crypto-user-utils";


//this.state.inUseWallet.address = encrypted.at(await cryptoUser.getIndexWallet()).address
//this.state.inUseWallet.balance = await web3.eth.getBalance(this.state.inUseWallet.address)

let isLogged = ref(false)
let user = await cryptoUser.getUserInfo()
isLogged.value = (user !== null)

async function retriveWalletInUse() {
  let inUseWallet
  if (isLogged.value === true) {
    try {
      let encrypted = await cryptoUser.retrieveEncryptedWallet()
      let index = await cryptoUser.getIndexWallet()
      const balance = await web3.eth.getBalance(encrypted.at(index).address)
      const balanceInEther = web3.utils.fromWei(balance, 'ether')
      const balanceString = parseFloat(balanceInEther).toFixed(15)

      inUseWallet = {
        index: index,
        address: encrypted.at(index).address,
        balance: balanceString
      }
    } catch (e) {
      console.error(e)
    }
  }else {
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
    async cahngeUseWallet(state, index) {
      //buggato più di me
      try {
        let encrypted = await cryptoUser.retrieveEncryptedWallet()
        await cryptoUser.changeInUseWalletIndex(index)

        state.inUseWallet.index = await cryptoUser.getIndexWallet()
        state.inUseWallet.address = encrypted.at(await cryptoUser.getIndexWallet()).address

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
