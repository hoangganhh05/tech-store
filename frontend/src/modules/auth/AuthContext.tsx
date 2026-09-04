import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { AUTH_UNAUTHORIZED_EVENT } from '../../services/httpClient'
import { type AuthenticatedUser, type LoginResult } from '../../services/authService'
import { tokenStorage } from '../../utils/tokenStorage'
import { AuthContext } from './AuthStore'

const AUTH_USER_KEY = 'techstore.authUser'

function getStoredUser(): AuthenticatedUser | null {
  if (!tokenStorage.getAccessToken()) return null

  try {
    const value = window.localStorage.getItem(AUTH_USER_KEY)
    return value ? (JSON.parse(value) as AuthenticatedUser) : null
  } catch {
    tokenStorage.clear()
    window.localStorage.removeItem(AUTH_USER_KEY)
    return null
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthenticatedUser | null>(getStoredUser)

  const clearSession = useCallback(() => {
    tokenStorage.clear()
    window.localStorage.removeItem(AUTH_USER_KEY)
    setUser(null)
  }, [])

  const signIn = useCallback((result: LoginResult) => {
    tokenStorage.setTokens(result.accessToken, result.refreshToken)
    window.localStorage.setItem(AUTH_USER_KEY, JSON.stringify(result.user))
    setUser(result.user)
  }, [])

  useEffect(() => {
    window.addEventListener(AUTH_UNAUTHORIZED_EVENT, clearSession)
    return () => window.removeEventListener(AUTH_UNAUTHORIZED_EVENT, clearSession)
  }, [clearSession])

  const value = useMemo(() => ({
    user,
    isAuthenticated: user !== null,
    signIn,
    clearSession,
  }), [clearSession, signIn, user])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
