// Cosa dire quando la password dell'account viene rifiutata. Chi e' entrato con GitHub puo' sceglierne una nuova
// senza la vecchia (pagina Account, card "Password dimenticata"): glielo si propone subito, invece di lasciarlo li'.

/** Dove porta il link "Password dimenticata": la card della pagina Account. */
export const FORGOT_PASSWORD_LINK = '/account#password-dimenticata'

/** true se l'account ha una password e si puo' sceglierne una nuova senza la vecchia (login con GitHub). */
export function canResetWithoutOld(profile: { hasPassword: boolean; login: string } | null | undefined): boolean {
  return !!profile && profile.hasPassword && profile.login === 'github'
}

/** Password rifiutata; `offerReset` dice se proporre la strada della password dimenticata. */
export class WrongPasswordError extends Error {
  constructor(readonly offerReset: boolean) {
    super(offerReset
      ? 'La password non corrisponde a quella dell\'account. Se non la ricordi, sei entrato con GitHub: puoi '
        + 'sceglierne una nuova in Account, alla voce "Password dimenticata". Gli account che hai restano nel wallet '
        + 'e si sbloccano con la vecchia password, se ti torna in mente.'
      : 'La password non corrisponde a quella dell\'account.')
    this.name = 'WrongPasswordError'
  }
}

/** true se l'errore e' una password rifiutata per cui conviene mostrare il pulsante "Password dimenticata". */
export function offersReset(e: unknown): boolean {
  return e instanceof WrongPasswordError && e.offerReset
}
