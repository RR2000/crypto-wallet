// Configurazione della web app: il server di autenticazione sta su /auth dello stesso origin,
// la rete Ethereum si sceglie alla build (default: Sepolia, la rete di prova pubblica).
const ORIGIN = typeof window === 'undefined' ? 'http://127.0.0.1:5173' : window.location.origin
export const AUTH_URL = `${ORIGIN}/auth`
export const CLIENT_ID = 'wallet-web'

export const RPC_URL: string = import.meta.env.VITE_RPC_URL ?? 'https://ethereum-sepolia-rpc.publicnode.com'
export const NETWORK_NAME: string = import.meta.env.VITE_NETWORK_NAME ?? 'Sepolia (rete di prova)'
export const EXPLORER_URL: string = import.meta.env.VITE_EXPLORER_URL ?? 'https://sepolia.etherscan.io/address/'
export const FAUCET_URL: string = import.meta.env.VITE_FAUCET_URL ?? 'https://sepolia-faucet.pk910.de/'

export const REPO_URL = 'https://github.com/RR2000/oauth2-blockchain'
export const THESIS_URL = 'https://github.com/RR2000/RR2000/blob/main/Rondinella-Raoul-Thesis.pdf'
