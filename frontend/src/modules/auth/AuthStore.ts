import { createContext } from 'react'
import type { AuthenticatedUser, LoginResult } from '../../services/authService'

export type AuthContextValue = {
  user: AuthenticatedUser | null
  isAuthenticated: boolean
  signIn: (result: LoginResult) => void
  clearSession: () => void
}

export const AuthContext = createContext<AuthContextValue | undefined>(undefined)
