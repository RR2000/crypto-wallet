import {createStore} from "vuex";

export const store = createStore({
  state: {
    inUseWallet: {
      address: "Nessun account selezionato",
      balance: "Nessun account selezionato"
    }
  },
  getters: {
    inUseWallet (state) {
      return state.inUseWallet
    }
  },
  mutations: {
    cahngeUseWallet (state, wallet) {
      return state.inUseWallet = wallet
    }
  }
})
