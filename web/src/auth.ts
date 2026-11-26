// Login OpenID Connect con il server di autenticazione (Authorization Code + PKCE, client pubblico)
// e chiamate all'API del wallet con l'access token.
import { UserManager, type User } from 'oidc-client-ts'
import { reactive } from 'vue'
import { AUTH_URL, CLIENT_ID } from './config'
import { parseWallet, serializeWallet, type StoredWallet } from './wallet'

const manager = new UserManager({
  authority: AUTH_URL,
  client_id: CLIENT_ID,
  redirect_uri: `${window.location.origin}/callback`,
  post_logout_redirect_uri: `${window.location.origin}/`,
  response_type: 'code',
  scope: 'openid profile email wallet',
  loadUserInfo: true,
  automaticSilentRenew: false,
})

export interface Profile {
  email: string
  firstName: string
  lastName: string
  /** false per un account arrivato da GitHub che non ha ancora scelto la password (che cifra il wallet) */
  hasPassword: boolean
  /** come si e' entrati: "github" permette di scegliere una password nuova senza la vecchia */
  login: string
}

export const session = reactive({
  ready: false,
  profile: null as Profile | null,
  wallet: parseWallet(null) as StoredWallet,
  /** indirizzi che l'ultima password usata non apre: cifrati con una password vecchia */
  locked: [] as string[],
})

let user: User | null = null

function apply(claims: Record<string, unknown>): void {
  session.profile = {
    email: String(claims.email ?? claims.sub ?? ''),
    firstName: String(claims.given_name ?? ''),
    lastName: String(claims.family_name ?? ''),
    hasPassword: claims.has_password !== false,
    // e' un claim del token: userinfo non lo ripete
    login: String(user?.profile.login ?? claims.login ?? ''),
  }
  session.wallet = parseWallet(claims.wallet)
}

/** All'avvio: riprende la sessione se c'e' un token ancora valido. */
export async function restore(): Promise<void> {
  try {
    user = await manager.getUser()
    if (user && !user.expired) await refresh()
    else user = null
  } catch {
    user = null
  }
  session.ready = true
}

export async function login(): Promise<void> {
  await manager.signinRedirect()
}

export async function completeLogin(): Promise<void> {
  user = await manager.signinRedirectCallback()
  apply(user.profile)
}

export async function logout(): Promise<void> {
  session.profile = null
  try {
    await manager.signoutRedirect()
  } catch {
    await manager.removeUser()
    window.location.href = '/'
  }
}

async function call(path: string, init: RequestInit = {}): Promise<Response> {
  if (!user || user.expired) {
    await login()
    throw new Error('Sessione scaduta: accedi di nuovo.')
  }
  return fetch(`${AUTH_URL}${path}`, {
    ...init,
    headers: { ...(init.headers ?? {}), Authorization: `Bearer ${user.access_token}` },
  })
}

function form(data: Record<string, string>): RequestInit {
  return { method: 'POST', body: new URLSearchParams(data) }
}

/** Rilegge profilo e wallet dal server (userinfo), dopo ogni modifica. */
export async function refresh(): Promise<void> {
  const r = await call('/userinfo')
  if (!r.ok) throw new Error(`userinfo: ${r.status}`)
  apply(await r.json())
}

export async function verifyPassword(password: string): Promise<boolean> {
  const r = await call('/api/password/verify', form({ password }))
  return r.status === 204
}

export async function saveWallet(w: StoredWallet): Promise<void> {
  const r = await call('/api/wallet', form({ wallet: serializeWallet(w) }))
  if (!r.ok) throw new Error(`salvataggio del wallet non riuscito (${r.status})`)
  await refresh()
}

export async function changePassword(currentPassword: string, newPassword: string): Promise<boolean> {
  const r = await call('/api/password', form({ currentPassword, newPassword }))
  return r.status === 204
}

export async function setPassword(newPassword: string): Promise<boolean> {
  const r = await call('/api/password/set', form({ newPassword }))
  return r.status === 204
}

/** Cosa offre il server: "Accedi con GitHub" e la password dimenticata via email (link nella pagina di accesso). */
export async function features(): Promise<{ github: boolean; passwordReset: boolean }> {
  try {
    const r = await fetch(`${AUTH_URL}/features`)
    if (r.ok) return await r.json()
  } catch {
    // server non raggiungibile: non si propone niente
  }
  return { github: false, passwordReset: false }
}

export async function resetPassword(newPassword: string): Promise<boolean> {
  const r = await call('/api/password/reset', form({ newPassword }))
  return r.status === 204
}
