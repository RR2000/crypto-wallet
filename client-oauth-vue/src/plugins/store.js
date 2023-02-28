import {createStore} from "vuex";
import {cryptoUser, web3} from "@/plugins/crypto-user-utils";


//this.state.inUseWallet.address = encrypted.at(await cryptoUser.getIndexWallet()).address
//this.state.inUseWallet.balance = await web3.eth.getBalance(this.state.inUseWallet.address)

let isLogged = ref(false)
try {
  cryptoUser.getUserInfo().then(function (user) {
    isLogged.value = user !== null
  })
} catch (e) {
  isLogged.value = false
}

async function pippo() {
  let inUseWallet

  try{
    let encrypted = await cryptoUser.retrieveEncryptedWallet()
    let index = await cryptoUser.getIndexWallet()

    inUseWallet = {
      index: index,
      address: encrypted.at(index).address,
      balance: await web3.eth.getBalance(encrypted.at(index).address)
    }
  }catch (e) {
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
    inUseWallet: await pippo()
  },
  getters: {
    inUseWallet(state) {
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
