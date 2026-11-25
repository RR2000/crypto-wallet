// Operazioni sul wallet che toccano le chiavi: si verifica la password sul server, si decifra nel browser,
// si modifica, si cifra di nuovo e si salva. Le chiavi private non lasciano mai il browser in chiaro; la password
// va al server solo per la verifica (su HTTPS, come in un normale login).
// Un account cifrato con un'altra password (dopo un "password dimenticata") resta bloccato ma non si perde mai.
import { changePassword, refresh, resetPassword, saveWallet, session, setPassword, verifyPassword } from './auth'
import { openAll, sealAll, slotAddress, type Slot } from './wallet'
import { canResetWithoutOld, WrongPasswordError } from './passwordHelp'
import { decryptKeystoreJson } from 'ethers'

export async function unlock(password: string): Promise<Slot[]> {
  if (!session.profile?.hasPassword) throw new Error('Prima scegli una password nella pagina Account: protegge le chiavi del wallet.')
  if (!(await verifyPassword(password))) throw new WrongPasswordError(canResetWithoutOld(session.profile))
  const slots = await openAll(session.wallet, password)
  session.locked = slots.filter((s) => !s.account).map(slotAddress)
  return slots
}

export async function editAccounts(password: string, edit: (slots: Slot[], names: string[]) => void): Promise<void> {
  const slots = await unlock(password)
  const names = [...session.wallet.names]
  edit(slots, names)
  const keystores = await sealAll(slots, password)
  const inUse = Math.min(session.wallet.inUse, Math.max(keystores.length - 1, 0))
  await saveWallet({ keystores, names, inUse })
  session.locked = slots.filter((s) => !s.account).map(slotAddress)
}

export async function renameAndSelect(names: string[], inUse: number): Promise<void> {
  await saveWallet({ ...session.wallet, names, inUse })
}

/** Cambia la password e cifra di nuovo con quella nuova gli account che si aprono (i bloccati restano come sono). */
export async function changeAccountPassword(currentPassword: string, newPassword: string): Promise<void> {
  const slots = await unlock(currentPassword)
  const keystores = await sealAll(slots.map((s) => (s.account ? { account: s.account } : s)), newPassword)
  if (!(await changePassword(currentPassword, newPassword))) throw new Error('Password attuale errata.')
  await saveWallet({ ...session.wallet, keystores })
}

/** Prima password di un account arrivato da GitHub. */
export async function chooseFirstPassword(password: string): Promise<void> {
  if (!(await setPassword(password))) throw new Error('L\'account ha già una password.')
  await refresh()
}

/** Password dimenticata, dopo il login con GitHub: password nuova, il wallet non si tocca. */
export async function resetForgottenPassword(password: string): Promise<void> {
  if (!(await resetPassword(password))) throw new Error('Per scegliere una password nuova senza la vecchia, esci e rientra con GitHub.')
  await refresh()
  session.locked = session.wallet.keystores.map((k) => slotAddress({ keystore: k }))
}

/** Riapre con una vecchia password gli account bloccati e li cifra con quella attuale. */
export async function recoverLocked(oldPassword: string, currentPassword: string): Promise<number> {
  const slots = await unlock(currentPassword)
  let recovered = 0
  for (const s of slots) {
    if (s.account) continue
    try {
      const a = await decryptKeystoreJson(JSON.stringify(s.keystore), oldPassword)
      s.account = { address: a.address, privateKey: a.privateKey }
      s.keystore = undefined
      recovered++
    } catch {
      // non e' questa la sua password: resta bloccato
    }
  }
  if (recovered === 0) throw new Error('Nessun account bloccato si apre con questa password.')
  await saveWallet({ ...session.wallet, keystores: await sealAll(slots, currentPassword) })
  session.locked = slots.filter((s) => !s.account).map(slotAddress)
  return recovered
}
