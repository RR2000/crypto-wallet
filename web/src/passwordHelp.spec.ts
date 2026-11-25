import { describe, expect, it } from 'vitest'
import { canResetWithoutOld, FORGOT_PASSWORD_LINK, offersReset, WrongPasswordError } from './passwordHelp'

describe('password rifiutata', () => {
  it('propone la password dimenticata solo a chi e\' entrato con GitHub e ha gia\' una password', () => {
    expect(canResetWithoutOld({ hasPassword: true, login: 'github' })).toBe(true)
    expect(canResetWithoutOld({ hasPassword: true, login: 'password' })).toBe(false)
    expect(canResetWithoutOld({ hasPassword: true, login: '' })).toBe(false)
    // senza password non c'e' niente da dimenticare: prima la sceglie
    expect(canResetWithoutOld({ hasPassword: false, login: 'github' })).toBe(false)
    expect(canResetWithoutOld(null)).toBe(false)
    expect(canResetWithoutOld(undefined)).toBe(false)
  })

  it('con GitHub il messaggio indica la strada e dice che le chiavi restano', () => {
    const e = new WrongPasswordError(true)
    expect(e.message).toMatch(/^La password non corrisponde a quella dell'account\./)
    expect(e.message).toContain('"Password dimenticata"')
    expect(e.message).toContain('restano nel wallet')
    expect(e.message).toContain('vecchia password')
    expect(offersReset(e)).toBe(true)
  })

  it('senza GitHub resta il messaggio di prima, senza pulsante', () => {
    const e = new WrongPasswordError(false)
    expect(e.message).toBe('La password non corrisponde a quella dell\'account.')
    expect(e).toBeInstanceOf(Error)
    expect(offersReset(e)).toBe(false)
    expect(offersReset(new Error('altro'))).toBe(false)
    expect(offersReset('stringa')).toBe(false)
  })

  it('il link porta alla card della pagina Account', () => {
    expect(FORGOT_PASSWORD_LINK).toBe('/account#password-dimenticata')
  })
})
