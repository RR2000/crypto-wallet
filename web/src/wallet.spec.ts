import { describe, expect, it } from 'vitest'
import { addressOf, decryptAll, encryptAll, fromMnemonic, fromPrivateKey, newAccount, parseWallet, serializeWallet } from './wallet'

// frase di prova pubblica (Hardhat/Foundry): il primo account e' noto
const TEST_PHRASE = 'test test test test test test test test test test test junk'
const TEST_ADDRESS = '0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266'

describe('wallet', () => {
  it('legge il formato salvato e quello vuoto di un utente nuovo', () => {
    expect(parseWallet('[[],[],[]]')).toEqual({ keystores: [], names: [], inUse: 0 })
    expect(parseWallet(null)).toEqual({ keystores: [], names: [], inUse: 0 })
    const w = parseWallet(JSON.stringify([[{ address: 'f39fd6e51aad88f6f4ce6ab8827279cfffb92266' }], [''], 3]))
    expect(w.names).toEqual(['Account 1'])
    expect(w.inUse).toBe(0)
    expect(addressOf(w.keystores[0])).toBe(TEST_ADDRESS)
  })

  it('ricava gli account standard dalla frase mnemonica', () => {
    expect(fromMnemonic(TEST_PHRASE, 0).address).toBe(TEST_ADDRESS)
    expect(fromMnemonic(`  ${TEST_PHRASE.toUpperCase()} `, 1).address).toBe('0x70997970C51812dc3A010C7d01b50e0d17dc79C8')
  })

  it('cifra e decifra con la password, e rifiuta quella sbagliata', async () => {
    const a = newAccount()
    const b = fromPrivateKey(fromMnemonic(TEST_PHRASE).privateKey.slice(2))
    const keystores = await encryptAll([a, b], 'password-giusta')
    const stored = parseWallet(serializeWallet({ keystores, names: ['uno', 'due'], inUse: 1 }))
    expect(stored.inUse).toBe(1)
    expect(JSON.stringify(stored)).not.toContain(a.privateKey.slice(2))
    expect(await decryptAll(stored, 'password-giusta')).toEqual([a, b])
    await expect(decryptAll(stored, 'password-sbagliata')).rejects.toThrow()
  }, 30_000)
})
