// Operazioni sul wallet che toccano le chiavi: si verifica la password sul server, si decifra nel browser,
// si modifica, si cifra di nuovo e si salva. La password e le chiavi private non lasciano mai il browser in chiaro
// verso il server, tranne la password per la verifica (su HTTPS, come in un normale login).
import { changePassword, saveWallet, session, verifyPassword } from './auth'
import { decryptAll, encryptAll, type Account } from './wallet'

export async function unlock(password: string): Promise<Account[]> {
  if (!(await verifyPassword(password))) throw new Error('La password non corrisponde a quella dell\'account.')
  return decryptAll(session.wallet, password)
}

export async function editAccounts(password: string, edit: (accounts: Account[], names: string[]) => void): Promise<void> {
  const accounts = await unlock(password)
  const names = [...session.wallet.names]
  edit(accounts, names)
  const keystores = await encryptAll(accounts, password)
  const inUse = Math.min(session.wallet.inUse, Math.max(keystores.length - 1, 0))
  await saveWallet({ keystores, names, inUse })
}

export async function renameAndSelect(names: string[], inUse: number): Promise<void> {
  await saveWallet({ ...session.wallet, names, inUse })
}

/** Cambia la password dell'account e cifra di nuovo il wallet con quella nuova. */
export async function changeAccountPassword(currentPassword: string, newPassword: string): Promise<void> {
  const accounts = await unlock(currentPassword)
  const keystores = await encryptAll(accounts, newPassword)
  if (!(await changePassword(currentPassword, newPassword))) throw new Error('Password attuale errata.')
  await saveWallet({ ...session.wallet, keystores })
}
