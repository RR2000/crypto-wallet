import {createStore} from "vuex";
import {cryptoUser, web3} from "@/plugins/crypto-user-utils";

let encrypted = await cryptoUser.retrieveEncryptedWallet()
let index = await cryptoUser.getIndexWallet()

//this.state.inUseWallet.address = encrypted.at(await cryptoUser.getIndexWallet()).address
//this.state.inUseWallet.balance = await web3.eth.getBalance(this.state.inUseWallet.address)

export const store = createStore({
  state: {
    inUseWallet: {
      index: index,
      address: encrypted.at(index).address,
      balance: await web3.eth.getBalance(encrypted.at(index).address)
    }
  },
  getters: {
    inUseWallet(state) {
      alert(JSON.stringify(state.inUseWallet))
      return state.inUseWallet
    }
  },
  mutations: {
    async cahngeUseWallet(state, index) {
      //buggato più di me
      let encrypted = await cryptoUser.retrieveEncryptedWallet()
      await cryptoUser.changeInUseWalletIndex(index)

      state.inUseWallet.index = await cryptoUser.getIndexWallet()
      state.inUseWallet.address = encrypted.at(await cryptoUser.getIndexWallet()).address
      state.inUseWallet.balance = await web3.eth.getBalance(state.inUseWallet.address)

      return state.inUseWallet
    }
  }
})
