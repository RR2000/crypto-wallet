// Il wallet dell'utente: gli account sono keystore V3 cifrati nel browser con la password dell'account,
// il server conserva solo questo JSON cifrato. Formato salvato: [keystore[], nomi[], indice in uso].
import {
  decryptKeystoreJson,
  encryptKeystoreJson,
  formatEther,
  getAddress,
  HDNodeWallet,
  JsonRpcProvider,
  Mnemonic,
  Wallet,
} from 'ethers'
import { RPC_URL } from './config'

export interface Keystore {
  address: string
  [key: string]: unknown
}

export interface StoredWallet {
  keystores: Keystore[]
  names: string[]
  inUse: number
}

export interface Account {
  address: string
  privateKey: string
}

/** scrypt piu' leggero del default di ethers: cifrare e decifrare piu' account resta veloce anche sul telefono */
const SCRYPT = { N: 1 << 14, r: 8, p: 1 }

export function parseWallet(claim: unknown): StoredWallet {
  let raw: unknown = claim
  if (typeof claim === 'string') {
    try {
      raw = JSON.parse(claim)
    } catch {
      raw = null
    }
  }
  const [keystores, names, inUse] = Array.isArray(raw) ? raw : []
  const ks = Array.isArray(keystores) ? (keystores as Keystore[]) : []
  return {
    keystores: ks,
    names: ks.map((_, i) => (Array.isArray(names) && typeof names[i] === 'string' && names[i].trim()) || `Account ${i + 1}`),
    inUse: typeof inUse === 'number' && inUse >= 0 && inUse < ks.length ? inUse : 0,
  }
}

export function serializeWallet(w: StoredWallet): string {
  return JSON.stringify([w.keystores, w.names, w.inUse])
}

export function addressOf(k: Keystore): string {
  return getAddress(k.address.startsWith('0x') ? k.address : `0x${k.address}`)
}

export async function decryptAll(w: StoredWallet, password: string): Promise<Account[]> {
  return Promise.all(
    w.keystores.map(async (k) => {
      const a = await decryptKeystoreJson(JSON.stringify(k), password)
      return { address: a.address, privateKey: a.privateKey }
    }),
  )
}

/**
 * Un posto del wallet: keystore cifrato, account in chiaro, o entrambi.
 * Solo keystore = account bloccato: cifrato con un'altra password (una vecchia), resta li' finche' non torna in mente.
 * Solo account = da cifrare al salvataggio.
 */
export interface Slot {
  keystore?: Keystore
  account?: Account
}

export function slotAddress(s: Slot): string {
  return s.account?.address ?? addressOf(s.keystore!)
}

/** Apre i keystore che si aprono con questa password; gli altri restano bloccati, mai buttati. */
export async function openAll(w: StoredWallet, password: string): Promise<Slot[]> {
  return Promise.all(
    w.keystores.map(async (keystore) => {
      try {
        const a = await decryptKeystoreJson(JSON.stringify(keystore), password)
        return { keystore, account: { address: a.address, privateKey: a.privateKey } }
      } catch {
        return { keystore }
      }
    }),
  )
}

/** Cifra solo i posti senza keystore; gli altri (anche quelli bloccati) restano com'erano. */
export async function sealAll(slots: Slot[], password: string): Promise<Keystore[]> {
  return Promise.all(slots.map(async (s) => s.keystore ?? (await encryptAll([s.account!], password))[0]))
}

export async function encryptAll(accounts: Account[], password: string): Promise<Keystore[]> {
  return Promise.all(
    accounts.map(async (a) => JSON.parse(await encryptKeystoreJson({ address: a.address, privateKey: a.privateKey }, password, { scrypt: SCRYPT })) as Keystore),
  )
}

export function newAccount(): Account {
  const w = Wallet.createRandom()
  return { address: w.address, privateKey: w.privateKey }
}

export function fromPrivateKey(key: string): Account {
  const k = key.trim()
  const w = new Wallet(k.startsWith('0x') ? k : `0x${k}`)
  return { address: w.address, privateKey: w.privateKey }
}

/** Account BIP-44 standard di Ethereum (m/44'/60'/0'/0/i) dalla frase mnemonica */
export function fromMnemonic(phrase: string, index = 0): Account {
  const w = HDNodeWallet.fromMnemonic(Mnemonic.fromPhrase(phrase.trim().toLowerCase().replace(/\s+/g, ' ')), `m/44'/60'/0'/0/${index}`)
  return { address: w.address, privateKey: w.privateKey }
}

let provider: JsonRpcProvider | undefined

export async function balanceOf(address: string): Promise<string> {
  provider ??= new JsonRpcProvider(RPC_URL, undefined, { staticNetwork: true })
  return formatEther(await provider.getBalance(address))
}
